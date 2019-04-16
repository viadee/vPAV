/**
 * BSD 3-Clause License
 *
 * Copyright © 2019, viadee Unternehmensberatung AG
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * * Neither the name of the copyright holder nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package de.viadee.bpm.vPAV.processing;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import de.viadee.bpm.vPAV.BpmnScanner;
import de.viadee.bpm.vPAV.FileScanner;
import de.viadee.bpm.vPAV.config.model.Rule;
import de.viadee.bpm.vPAV.constants.BpmnConstants;
import de.viadee.bpm.vPAV.processing.code.flow.FlowGraph;
import de.viadee.bpm.vPAV.processing.model.data.AnomalyContainer;
import de.viadee.bpm.vPAV.processing.model.data.BpmnElement;
import de.viadee.bpm.vPAV.processing.model.data.ProcessVariableOperation;
import de.viadee.bpm.vPAV.processing.model.graph.Edge;
import de.viadee.bpm.vPAV.processing.model.graph.Graph;
import de.viadee.bpm.vPAV.processing.model.graph.IGraph;
import de.viadee.bpm.vPAV.processing.model.graph.Path;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.*;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaIn;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaOut;

import java.io.File;
import java.util.*;

/**
 * Creates data flow graph based on a bpmn model
 *
 */
public class ElementGraphBuilder {

	private Map<String, BpmnElement> elementMap = new HashMap<>();

	private Map<String, String> processIdToPathMap;

	private Map<String, String> decisionRefToPathMap;

	private Map<String, Collection<String>> messageIdToVariables;

	private Map<String, Collection<String>> processIdToVariables;

	private BpmnScanner bpmnScanner;

	private Rule rule;

	public ElementGraphBuilder(BpmnScanner bpmnScanner) {
		this.bpmnScanner = bpmnScanner;
	}

	public ElementGraphBuilder(BpmnScanner bpmnScanner, final Rule rule) {
		this.bpmnScanner = bpmnScanner;
		this.rule = rule;
	}

	public ElementGraphBuilder(final Map<String, String> decisionRefToPathMap,
			final Map<String, String> processIdToPathMap, final Map<String, Collection<String>> messageIdToVariables,
			final Map<String, Collection<String>> processIdToVariables, final Rule rule, BpmnScanner bpmnScanner) {
		this.decisionRefToPathMap = decisionRefToPathMap;
		this.processIdToPathMap = processIdToPathMap;
		this.messageIdToVariables = messageIdToVariables;
		this.processIdToVariables = processIdToVariables;
		this.bpmnScanner = bpmnScanner;
		this.rule = rule;
	}

	public ElementGraphBuilder(final Map<String, String> decisionRefToPathMap,
			final Map<String, String> processIdToPathMap, final Map<String, Collection<String>> messageIdToVariables,
			final Map<String, Collection<String>> processIdToVariables, BpmnScanner bpmnScanner) {
		this.decisionRefToPathMap = decisionRefToPathMap;
		this.processIdToPathMap = processIdToPathMap;
		this.messageIdToVariables = messageIdToVariables;
		this.processIdToVariables = processIdToVariables;
		this.bpmnScanner = bpmnScanner;
	}

	public ElementGraphBuilder(final Map<String, String> decisionRefToPathMap,
			final Map<String, String> processIdToPathMap, BpmnScanner bpmnScanner) {
		this.decisionRefToPathMap = decisionRefToPathMap;
		this.processIdToPathMap = processIdToPathMap;
		this.bpmnScanner = bpmnScanner;
	}

	/**
	 * Create data flow graphs for a model
	 *
	 * @param context
	 *            JavaReaderContext (static vs. regex)
	 * @param fileScanner
	 *            FileScanner
	 * @param modelInstance
	 *            BpmnModelInstance
	 * @param processDefinition
	 *            processDefinition
	 * @param calledElementHierarchy
	 *            calledElementHierarchy
	 * @param scanner
	 *            OuterProcessVariablesScanner
	 * @return graphCollection returns graphCollection
	 */
	public Collection<IGraph> createProcessGraph(final JavaReaderContext context, final FileScanner fileScanner,
			final BpmnModelInstance modelInstance, final String processDefinition,
			final Collection<String> calledElementHierarchy, final ProcessVariablesScanner scanner) {

		final Collection<IGraph> graphCollection = new ArrayList<>();

		final Collection<Process> processes = modelInstance.getModelElementsByType(Process.class);
		for (final Process process : processes) {
			final IGraph graph = new Graph(process.getId());
			final Collection<FlowElement> elements = process.getFlowElements();
			final Collection<SequenceFlow> flows = new ArrayList<>();
			final Collection<BoundaryEvent> boundaryEvents = new ArrayList<>();
			final Collection<SubProcess> subProcesses = new ArrayList<>();
			final Collection<CallActivity> callActivities = new ArrayList<>();

			for (final FlowElement element : elements) {
				final FlowGraph flowGraph = new FlowGraph();
				if (element instanceof SequenceFlow) {
					// mention sequence flows
					final SequenceFlow flow = (SequenceFlow) element;
					flows.add(flow);
				} else if (element instanceof BoundaryEvent) {
					// mention boundary events
					final BoundaryEvent event = (BoundaryEvent) element;
					boundaryEvents.add(event);
				} else if (element instanceof CallActivity) {
					// mention call activities
					final CallActivity callActivity = (CallActivity) element;
					callActivities.add(callActivity);
				} else if (element instanceof SubProcess) {
					final SubProcess subprocess = (SubProcess) element;
					addElementsSubprocess(context, fileScanner, subProcesses, flows, boundaryEvents, graph, subprocess,
							processDefinition, flowGraph);
				}

				// Ordered map to hold operations in correct order
				final ListMultimap<String, ProcessVariableOperation> variables = ArrayListMultimap.create();

				// initialize element
				final BpmnElement node = new BpmnElement(processDefinition, element);

				// retrieve initial variable operation (should be WRITE)
				if (element.getElementType().getTypeName().equals(BpmnConstants.START_EVENT)) {
					final ArrayList<String> messageRefs = bpmnScanner.getMessageRefs(element.getId());
					String messageName = "";
					if (messageRefs.size() == 1) {
						messageName = bpmnScanner.getMessageName(messageRefs.get(0));
					}
					// add process variables for start event, which set by call
					// startProcessInstanceByKey

					for (EntryPoint ep : scanner.getEntryPoints()) {
						if (ep.getMessageName().equals(messageName)) {
							variables.putAll(
									checkInitialVariableOperations(ep, context, scanner, node, processDefinition));
						}
					}
					graph.addStartNode(node);
				}

				if (element.getElementType().getTypeName().equals(BpmnConstants.RECEIVE_TASK)) {
					final ArrayList<String> messageRefs = bpmnScanner.getMessageRefs(element.getId());
					String messageName = "";
					if (messageRefs.size() == 1) {
						messageName = bpmnScanner.getMessageName(messageRefs.get(0));
					}
					// add process variables for receive task, which set by call
					// startProcessInstanceByKey

					for (EntryPoint ep : scanner.getIntermediateEntryPoints()) {
						if (ep.getMessageName().equals(messageName)) {
							variables.putAll(
									checkInitialVariableOperations(ep, context, scanner, node, processDefinition));
						}
					}
				}

				// examine process variables and save it with access operation
				final ProcessVariableReader reader = new ProcessVariableReader(decisionRefToPathMap, rule, bpmnScanner);
				variables.putAll(reader.getVariablesFromElement(context, fileScanner, node, flowGraph));
				// examine process variables for element and set it
				node.setProcessVariables(variables);

				// mention element
				elementMap.put(element.getId(), node);

				if (element.getElementType().getTypeName().equals(BpmnConstants.END_EVENT)) {
					graph.addEndNode(node);
				}
				// save process elements as a node
				graph.addVertex(node);
			}
			// add edges into the graph
			addEdges(graph, flows, boundaryEvents, subProcesses);

			// resolve call activities and integrate called processes
			for (final CallActivity callActivity : callActivities) {
				integrateCallActivityFlow(context, fileScanner, processDefinition, modelInstance, callActivity, graph,
						calledElementHierarchy, scanner);
			}

			graphCollection.add(graph);
		}

		return graphCollection;
	}

	/**
	 *
	 * Checks for initial variable operations (esp. initializations of variables)
	 *
	 * @param entryPoint
	 *            Current entryPoint (most likely rest controller classes)
	 * @param jvc
	 *            JavaReaderContext
	 * @param scanner
	 *            OuterProcessVariableScanner
	 * @param element
	 *            Current BPMN element
	 * @param resourceFilePath
	 *            Current BPMN location
	 * @return initial operations
	 */
	private ListMultimap<String, ProcessVariableOperation> checkInitialVariableOperations(final EntryPoint entryPoint,
			final JavaReaderContext jvc, final ProcessVariablesScanner scanner, final BpmnElement element,
			final String resourceFilePath) {
		return jvc.readClass(entryPoint.getClassName(), scanner, element, resourceFilePath, entryPoint);

	}

	public BpmnElement getElement(final String id) {
		return elementMap.get(id);
	}

	/**
	 * Create invalid paths for data flow anomalies
	 *
	 * @param graphCollection
	 *            IGraph
	 * @return invalidPathMap returns invalidPathMap
	 */
	public Map<AnomalyContainer, List<Path>> createInvalidPaths(final Collection<IGraph> graphCollection) {
		final Map<AnomalyContainer, List<Path>> invalidPathMap = new HashMap<>();

		for (final IGraph g : graphCollection) {
			// add data flow information to graph
			g.setAnomalyInformation(g.getStartNodes().iterator().next());
			// get nodes with data anomalies
			final Map<BpmnElement, List<AnomalyContainer>> anomalies = g.getNodesWithAnomalies();

			for (final BpmnElement element : anomalies.keySet()) {
				for (AnomalyContainer anomaly : anomalies.get(element)) {
					// create paths for data flow anomalies
					final List<Path> paths = g.getAllInvalidPaths(element, anomaly);
					for (final Path path : paths) {
						// reverse order for a better readability
						Collections.reverse(path.getElements());
					}
					invalidPathMap.put(anomaly, new ArrayList<>(paths));
				}
			}
		}

		return invalidPathMap;
	}

	/**
	 * Add edges to data flow graph
	 *
	 * @param graph
	 *            IGraph
	 * @param flows
	 *            Collection of SequenceFlows
	 * @param boundaryEvents
	 *            Collection of BoundaryEvents
	 * @param subProcesses
	 *            Collection of SubProcesses
	 */
	private void addEdges(final IGraph graph, final Collection<SequenceFlow> flows,
			final Collection<BoundaryEvent> boundaryEvents, final Collection<SubProcess> subProcesses) {
		for (final SequenceFlow flow : flows) {
			final BpmnElement flowElement = elementMap.get(flow.getId());
			final BpmnElement srcElement = elementMap.get(flow.getSource().getId());
			final BpmnElement destElement = elementMap.get(flow.getTarget().getId());

			graph.addEdge(srcElement, flowElement, 100);
			graph.addEdge(flowElement, destElement, 100);
		}
		for (final BoundaryEvent event : boundaryEvents) {
			final BpmnElement dstElement = elementMap.get(event.getId());
			final Activity source = event.getAttachedTo();
			final BpmnElement srcElement = elementMap.get(source.getId());
			graph.addEdge(srcElement, dstElement, 100);
		}
		for (final SubProcess subProcess : subProcesses) {
			final BpmnElement subprocessElement = elementMap.get(subProcess.getId());
			// integration of a subprocess in data flow graph
			// inner elements will be directly connected into the graph
			final Collection<StartEvent> startEvents = subProcess.getChildElementsByType(StartEvent.class);
			final Collection<EndEvent> endEvents = subProcess.getChildElementsByType(EndEvent.class);
			if (startEvents != null && startEvents.size() > 0 && endEvents != null && endEvents.size() > 0) {
				final Collection<SequenceFlow> incomingFlows = subProcess.getIncoming();
				for (final SequenceFlow incomingFlow : incomingFlows) {
					final BpmnElement srcElement = elementMap.get(incomingFlow.getId());
					for (final StartEvent startEvent : startEvents) {
						final BpmnElement dstElement = elementMap.get(startEvent.getId());
						graph.addEdge(srcElement, dstElement, 100);
						graph.removeEdge(srcElement, subprocessElement);
					}
				}
				final Collection<SequenceFlow> outgoingFlows = subProcess.getOutgoing();
				for (final EndEvent endEvent : endEvents) {
					final BpmnElement srcElement = elementMap.get(endEvent.getId());
					for (final SequenceFlow outgoingFlow : outgoingFlows) {
						final BpmnElement dstElement = elementMap.get(outgoingFlow.getId());
						graph.addEdge(srcElement, dstElement, 100);
						graph.removeEdge(subprocessElement, dstElement);
					}
				}
			}
		}
	}

	/**
	 * Add elements from subprocess to data flow graph
	 *
	 * @param context
	 *            JavaReaderContext
	 * @param fileScanner
	 *            FileScanner
	 * @param subProcesses
	 *            Collection of SubProcesses
	 * @param flows
	 *            Collection of SequenceFlows
	 * @param events
	 *            Collection of BoundaryEvents
	 * @param graph
	 *            Current Graph
	 * @param process
	 *            Current Process
	 * @param processDefinition
	 *            Current Path to process
	 */
	private void addElementsSubprocess(final JavaReaderContext context, final FileScanner fileScanner,
			final Collection<SubProcess> subProcesses, final Collection<SequenceFlow> flows,
			final Collection<BoundaryEvent> events, final IGraph graph, final SubProcess process,
			final String processDefinition, final FlowGraph cg) {
		subProcesses.add(process);
		final Collection<FlowElement> subElements = process.getFlowElements();
		for (final FlowElement subElement : subElements) {
			if (subElement instanceof SubProcess) {
				final SubProcess subProcess = (SubProcess) subElement;
				addElementsSubprocess(context, fileScanner, subProcesses, flows, events, graph, subProcess,
						processDefinition, cg);
			} else if (subElement instanceof SequenceFlow) {
				final SequenceFlow flow = (SequenceFlow) subElement;
				flows.add(flow);
			} else if (subElement instanceof BoundaryEvent) {
				final BoundaryEvent boundaryEvent = (BoundaryEvent) subElement;
				events.add(boundaryEvent);
			}
			// add elements of the sub process as nodes
			final BpmnElement node = new BpmnElement(processDefinition, subElement);
			// determine process variables with operations
			final ListMultimap<String, ProcessVariableOperation> variables = ArrayListMultimap.create();
			variables.putAll(new ProcessVariableReader(decisionRefToPathMap, rule, bpmnScanner)
					.getVariablesFromElement(context, fileScanner, node, cg));
			// set process variables for the node
			node.setProcessVariables(variables);
			// mention the element
			elementMap.put(subElement.getId(), node);
			// add element as node
			graph.addVertex(node);
		}
	}

	/**
	 * Integrate a called activity into data flow graph
	 *
	 * @param context
	 *            JavaReaderContext
	 * @param fileScanner
	 *            FileScanner
	 * @param processdefinition
	 *            Current Path to process
	 * @param modelInstance
	 *            BpmnModelInstance
	 * @param callActivity
	 *            CallActivity
	 * @param graph
	 *            Current Graph
	 * @param calledElementHierarchy
	 *            Collection of Element Hierarchy
	 * @param scanner
	 *            OuterProcessVariableScanner
	 */
	private void integrateCallActivityFlow(final JavaReaderContext context, final FileScanner fileScanner,
			final String processdefinition, final BpmnModelInstance modelInstance, final CallActivity callActivity,
			final IGraph graph, final Collection<String> calledElementHierarchy,
			final ProcessVariablesScanner scanner) {

		final String calledElement = callActivity.getCalledElement();

		// check call hierarchy to avoid deadlocks
		if (calledElementHierarchy.contains(calledElement)) {
			throw new RuntimeException("call activity hierarchy causes a deadlock (see " + processdefinition + ", "
					+ callActivity.getId() + "). please avoid loops.");
		}
		calledElementHierarchy.add(calledElement);

		// integrate only, if file locations for process ids are known
		if (processIdToPathMap != null && processIdToPathMap.get(calledElement) != null) {

			// 1) read in- and output variables from call activity
			final Collection<String> inVariables = new ArrayList<>();
			final Collection<String> outVariables = new ArrayList<>();
			readCallActivityDataInterfaces(callActivity, inVariables, outVariables);

			// 2) add parallel gateways before and after the call activity in the main data
			// flow
			// They are necessary for connecting the sub process with the main flow
			final List<BpmnElement> parallelGateways = addParallelGatewaysBeforeAndAfterCallActivityInMainDataFlow(
					modelInstance, callActivity, graph);
			final BpmnElement parallelGateway1 = parallelGateways.get(0);
			final BpmnElement parallelGateway2 = parallelGateways.get(1);

			// get file path of the called process
			final String callActivityPath = processIdToPathMap.get(calledElement);
			if (callActivityPath != null) {
				// 3) load process and transform it into a data flow graph
				final Collection<IGraph> subgraphs = createSubDataFlowsFromCallActivity(context, fileScanner,
						calledElementHierarchy, callActivityPath, scanner);

				for (final IGraph subgraph : subgraphs) {
					// look only on the called process!
					if (subgraph.getProcessId().equals(calledElement)) {
						// 4) connect sub data flow with the main data flow
						connectParallelGatewaysWithSubDataFlow(graph, inVariables, outVariables, parallelGateway1,
								parallelGateway2, subgraph);
					}
				}
			}
		}
	}

	/**
	 * Add parallel gateways before and after a call activity. They are needed to
	 * connect the called process with the main flow
	 *
	 * @param modelInstance
	 *            BpmnModelInstance
	 * @param callActivity
	 *            CallActivity
	 * @param graph
	 *            Current Graph
	 * @return parallel gateway elements
	 */
	private List<BpmnElement> addParallelGatewaysBeforeAndAfterCallActivityInMainDataFlow(
			final BpmnModelInstance modelInstance, final CallActivity callActivity, final IGraph graph) {

		final ParallelGateway element1 = modelInstance.newInstance(ParallelGateway.class);
		element1.setAttributeValue(BpmnConstants.ATTR_ID, "_gw_in", true);

		final ParallelGateway element2 = modelInstance.newInstance(ParallelGateway.class);
		element2.setAttributeValue(BpmnConstants.ATTR_ID, "_gw_out", true);

		final List<BpmnElement> elements = new ArrayList<>();
		final BpmnElement parallelGateway1 = new BpmnElement(null, element1);
		final BpmnElement parallelGateway2 = new BpmnElement(null, element2);
		elements.add(parallelGateway1);
		elements.add(parallelGateway2);

		graph.addVertex(parallelGateway1);
		graph.addVertex(parallelGateway2);

		connectParallelGatewaysWithMainDataFlow(callActivity, graph, parallelGateway1, parallelGateway2);

		return elements;
	}

	/**
	 * Connect the parallel gateways in the data flow before and after the call
	 * activity
	 *
	 * @param graph
	 *            Current Graph
	 * @param inVariables
	 *            Collection of ingoing variables
	 * @param outVariables
	 *            Collection of outgoing variables
	 * @param parallelGateway1
	 *            First parallel gateway (BpmnElement)
	 * @param parallelGateway2
	 *            Second parallel gateway (BpmnElement)
	 * @param subgraph
	 *            Subgraph
	 */
	private void connectParallelGatewaysWithSubDataFlow(final IGraph graph, final Collection<String> inVariables,
			final Collection<String> outVariables, final BpmnElement parallelGateway1,
			final BpmnElement parallelGateway2, final IGraph subgraph) {

		// read nodes of the sub data flow
		final Collection<BpmnElement> vertices = subgraph.getVertices();
		for (final BpmnElement vertex : vertices) {
			// add _ before the element id to avoid name clashes
			final BaseElement baseElement = vertex.getBaseElement();
			baseElement.setId("_" + baseElement.getId());
			// add node to the main data flow
			graph.addVertex(vertex);
		}
		// read edges of the sub data flow
		final Collection<List<Edge>> edges = subgraph.getEdges();
		for (final List<Edge> list : edges) {
			for (final Edge edge : list) {
				final BpmnElement from = edge.getFrom();
				final BpmnElement to = edge.getTo();
				// add edge the the main data flow
				graph.addEdge(from, to, 100);
			}
		}

		// get start and end nodes of the sub data flow and connect parallel gateways in
		// the main flow
		// with it
		final Collection<BpmnElement> startNodes = subgraph.getStartNodes();
		for (final BpmnElement startNode : startNodes) {
			// set variables from in interface of the call activity
			startNode.setInCa(inVariables);
			graph.addEdge(parallelGateway1, startNode, 100);
		}
		final Collection<BpmnElement> endNodes = subgraph.getEndNodes();
		for (final BpmnElement endNode : endNodes) {
			// set variables from out interface of the call activity
			endNode.setOutCa(outVariables);
			graph.addEdge(endNode, parallelGateway2, 100);
		}
	}

	/**
	 * Read and transform process definition into data flows
	 *
	 * @param context
	 *            JavaReaderContext
	 * @param fileScanner
	 *            FileScanner
	 * @param calledElementHierarchy
	 *            Collection of Element Hierarchy
	 * @param callActivityPath
	 *            CallActivityPath
	 * @param scanner
	 *            OuterProcessVariableScanner
	 * @return Collection of IGraphs (subgraphs)
	 */
	private Collection<IGraph> createSubDataFlowsFromCallActivity(final JavaReaderContext context,
			final FileScanner fileScanner, final Collection<String> calledElementHierarchy,
			final String callActivityPath, final ProcessVariablesScanner scanner) {
		// read called process
		final BpmnModelInstance subModel = Bpmn.readModelFromFile(new File(callActivityPath));

		// transform process into data flow
		final ElementGraphBuilder graphBuilder = new ElementGraphBuilder(decisionRefToPathMap, processIdToPathMap,
				messageIdToVariables, processIdToVariables, rule, bpmnScanner);
		return graphBuilder.createProcessGraph(context, fileScanner, subModel, callActivityPath, calledElementHierarchy,
				scanner);
	}

	/**
	 * Integrate parallel gateways into the main data flow before and after the call
	 * activity
	 *
	 * @param callActivity
	 *            CallActivity
	 * @param graph
	 *            Current Graph
	 * @param parallelGateway1
	 *            First parallel gateway (BpmnElement)
	 * @param parallelGateway2
	 *            Second parallel gateway (BpmnElement)
	 */
	private void connectParallelGatewaysWithMainDataFlow(final CallActivity callActivity, final IGraph graph,
			final BpmnElement parallelGateway1, final BpmnElement parallelGateway2) {

		// read incoming and outgoing sequence flows of the call activity
		final SequenceFlow incomingSequenceFlow = callActivity.getIncoming().iterator().next();
		final SequenceFlow outgoingSequenceFlow = callActivity.getOutgoing().iterator().next();

		// remove edges
		graph.removeEdge(elementMap.get(incomingSequenceFlow.getId()), elementMap.get(callActivity.getId()));
		graph.removeEdge(elementMap.get(callActivity.getId()), elementMap.get(outgoingSequenceFlow.getId()));

		// link parallel gateways with the existing data flow
		graph.addEdge(elementMap.get(incomingSequenceFlow.getId()), parallelGateway1, 100);
		graph.addEdge(parallelGateway2, elementMap.get(outgoingSequenceFlow.getId()), 100);
		graph.addEdge(parallelGateway1, elementMap.get(callActivity.getId()), 100);
		graph.addEdge(elementMap.get(callActivity.getId()), parallelGateway2, 100);
	}

	/**
	 * Read in- and output variables for a call activity
	 *
	 * @param callActivity
	 *            CallActivity
	 * @param inVariables
	 *            Collection of ingoing variables
	 * @param outVariables
	 *            Collection of outgoing variables
	 */
	private void readCallActivityDataInterfaces(final CallActivity callActivity, final Collection<String> inVariables,
			final Collection<String> outVariables) {

		final ExtensionElements extensionElements = callActivity.getExtensionElements();
		if (extensionElements != null) {
			final List<CamundaIn> inputAssociations = extensionElements.getElementsQuery().filterByType(CamundaIn.class)
					.list();
			for (final CamundaIn inputAssociation : inputAssociations) {
				final String source = inputAssociation.getCamundaSource();
				if (source != null && !source.isEmpty()) {
					inVariables.add(source);
				}
			}
			final List<CamundaOut> outputAssociations = extensionElements.getElementsQuery()
					.filterByType(CamundaOut.class).list();
			for (final CamundaOut outputAssociation : outputAssociations) {
				final String target = outputAssociation.getCamundaTarget();
				if (target != null && !target.isEmpty()) {
					outVariables.add(target);
				}
			}
		}
	}
}
