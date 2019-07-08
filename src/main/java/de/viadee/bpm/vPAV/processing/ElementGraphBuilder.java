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
import de.viadee.bpm.vPAV.constants.ConfigConstants;
import de.viadee.bpm.vPAV.processing.code.flow.BpmnElement;
import de.viadee.bpm.vPAV.processing.code.flow.ControlFlowGraph;
import de.viadee.bpm.vPAV.processing.code.flow.FlowAnalysis;
import de.viadee.bpm.vPAV.processing.model.data.AnomalyContainer;
import de.viadee.bpm.vPAV.processing.model.data.ProcessVariableOperation;
import de.viadee.bpm.vPAV.processing.model.graph.Edge;
import de.viadee.bpm.vPAV.processing.model.graph.Graph;
import de.viadee.bpm.vPAV.processing.model.graph.Path;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.bpmn.instance.*;

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
	 * @param flowAnalysis
	 *            FlowAnalysis
	 * @return graphCollection returns graphCollection
	 */
	public Collection<Graph> createProcessGraph(final FileScanner fileScanner, final BpmnModelInstance modelInstance,
			final String processDefinition, final Collection<String> calledElementHierarchy,
			final ProcessVariablesScanner scanner, final FlowAnalysis flowAnalysis) {

		final Collection<Graph> graphCollection = new ArrayList<>();

		final Collection<Process> processes = modelInstance.getModelElementsByType(Process.class);
		for (final Process process : processes) {
			final Graph graph = new Graph(process.getId());
			final Collection<FlowElement> elements = process.getFlowElements();
			final Collection<SequenceFlow> flows = new ArrayList<>();
			final Collection<BoundaryEvent> boundaryEvents = new ArrayList<>();
			final Collection<SubProcess> subProcesses = new ArrayList<>();
			final HashMap<BpmnElement, FlowElement> callActivities = new HashMap<>();

			for (final FlowElement element : elements) {

				final ControlFlowGraph controlFlowGraph = new ControlFlowGraph();
				// initialize element
				final BpmnElement node = new BpmnElement(processDefinition, element, controlFlowGraph, flowAnalysis);

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
					callActivities.put(node, element);
				} else if (element instanceof SubProcess) {
					final SubProcess subprocess = (SubProcess) element;
					addElementsSubprocess(fileScanner, subProcesses, flows, boundaryEvents, graph, subprocess,
							processDefinition, controlFlowGraph, flowAnalysis);
				}

				// Ordered map to hold operations in correct order
				final ListMultimap<String, ProcessVariableOperation> variables = ArrayListMultimap.create();

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
							variables.putAll(checkInitialVariableOperations(ep, node, processDefinition));
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
							variables.putAll(checkInitialVariableOperations(ep, node, processDefinition));
						}
					}
				}

				// examine process variables and save it with access operation
				final ProcessVariableReader reader = new ProcessVariableReader(decisionRefToPathMap, rule, bpmnScanner);
				variables.putAll(reader.getVariablesFromElement(fileScanner, node, controlFlowGraph));
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
			for (Map.Entry<BpmnElement, FlowElement> entry : callActivities.entrySet()) {
				integrateCallActivityFlow(fileScanner, processDefinition, entry.getKey(), entry.getValue(), graph,
						calledElementHierarchy, scanner, flowAnalysis);
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
	 * @param element
	 *            Current BPMN element
	 * @param resourceFilePath
	 *            Current BPMN location
	 * @return initial operations
	 */
	private ListMultimap<String, ProcessVariableOperation> checkInitialVariableOperations(final EntryPoint entryPoint,
			final BpmnElement element, final String resourceFilePath) {
		return new JavaReaderStatic().getVariablesFromClass(entryPoint.getClassName(), element, resourceFilePath,
				entryPoint);
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
	public Map<AnomalyContainer, List<Path>> createInvalidPaths(final Collection<Graph> graphCollection) {
		final Map<AnomalyContainer, List<Path>> invalidPathMap = new HashMap<>();

		for (final Graph g : graphCollection) {
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
	private void addEdges(final Graph graph, final Collection<SequenceFlow> flows,
			final Collection<BoundaryEvent> boundaryEvents, final Collection<SubProcess> subProcesses) {
		for (final SequenceFlow flow : flows) {
			final BpmnElement flowElement = elementMap.get(flow.getId());
			final BpmnElement srcElement = elementMap.get(flow.getSource().getId());
			final BpmnElement destElement = elementMap.get(flow.getTarget().getId());

			flowElement.addPredecessor(srcElement);
			flowElement.addSuccessor(destElement);

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
	 * @param controlFlowGraph
	 *            ControlFlowGraph
	 * @param flowAnalysis
	 *            FlowAnalysis
	 */
	private void addElementsSubprocess(final FileScanner fileScanner, final Collection<SubProcess> subProcesses,
			final Collection<SequenceFlow> flows, final Collection<BoundaryEvent> events, final Graph graph,
			final SubProcess process, final String processDefinition, final ControlFlowGraph controlFlowGraph,
			final FlowAnalysis flowAnalysis) {
		subProcesses.add(process);
		final Collection<FlowElement> subElements = process.getFlowElements();
		for (final FlowElement subElement : subElements) {
			if (subElement instanceof SubProcess) {
				final SubProcess subProcess = (SubProcess) subElement;
				addElementsSubprocess(fileScanner, subProcesses, flows, events, graph, subProcess, processDefinition,
						controlFlowGraph, flowAnalysis);
			} else if (subElement instanceof SequenceFlow) {
				final SequenceFlow flow = (SequenceFlow) subElement;
				flows.add(flow);
			} else if (subElement instanceof BoundaryEvent) {
				final BoundaryEvent boundaryEvent = (BoundaryEvent) subElement;
				events.add(boundaryEvent);
			}
			// add elements of the sub process as nodes
			final BpmnElement node = new BpmnElement(processDefinition, subElement, controlFlowGraph, flowAnalysis);
			// determine process variables with operations
			final ListMultimap<String, ProcessVariableOperation> variables = ArrayListMultimap.create();
			variables.putAll(new ProcessVariableReader(decisionRefToPathMap, rule, bpmnScanner)
					.getVariablesFromElement(fileScanner, node, controlFlowGraph));
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
	 * @param fileScanner
	 *            FileScanner
	 * @param processDefinition
	 *            Current Path to process
	 * @param element
	 *            CallActivity
	 * @param graph
	 *            Current Graph
	 * @param calledElementHierarchy
	 *            Collection of Element Hierarchy
	 * @param scanner
	 *            ProcessVariableScanner
	 * @param flowAnalysis
	 *            FlowAnalysis
	 */
	private void integrateCallActivityFlow(final FileScanner fileScanner, final String processDefinition,
			final BpmnElement element, final FlowElement activity, final Graph graph,
			final Collection<String> calledElementHierarchy, final ProcessVariablesScanner scanner,
			final FlowAnalysis flowAnalysis) {

		final CallActivity callActivity = (CallActivity) activity;
		final String calledElement = callActivity.getCalledElement();

		// check call hierarchy to avoid deadlocks
		if (calledElementHierarchy.contains(calledElement)) {
			throw new RuntimeException("call activity hierarchy causes a deadlock (see " + processDefinition + ", "
					+ callActivity.getId() + "). please avoid loops.");
		}
		calledElementHierarchy.add(calledElement);

		// integrate only, if file locations for process ids are known
		if (processIdToPathMap != null && processIdToPathMap.get(calledElement) != null) {

			// get file path of the called process
			final String callActivityPath = processIdToPathMap.get(calledElement);
			if (callActivityPath != null) {
				// load process and transform it into a data flow graph
				final Collection<Graph> subGraphs = createSubDataFlowsFromCallActivity(fileScanner,
						calledElementHierarchy, callActivityPath, scanner, flowAnalysis);

				for (final Graph subGraph : subGraphs) {
					// look only on the called process!
					if (subGraph.getProcessId().equals(calledElement)) {
						// connect sub data flow with the main data flow
						connectGraphs(graph, subGraph, element);
					}
				}
			}
		}
	}

	/**
	 * Connect graph with sub graph
	 *
	 * @param graph
	 *            Current Graph
	 * @param subGraph
	 *            Sub Graph
	 */
	private void connectGraphs(final Graph graph, final Graph subGraph, final BpmnElement callActivity) {
		// read nodes of the sub data flow
		final Collection<BpmnElement> vertices = subGraph.getVertices();
		for (final BpmnElement vertex : vertices) {
			// add _ before the element id to avoid name clashes
			final BaseElement baseElement = vertex.getBaseElement();
			baseElement.setId("_" + baseElement.getId());
			// add node to the main data flow
			graph.addVertex(vertex);
		}

		// read edges of the sub data flow
		final Collection<List<Edge>> subGraphEdges = subGraph.getEdges();
		for (final List<Edge> list : subGraphEdges) {
			for (final Edge edge : list) {
				final BpmnElement from = edge.getFrom();
				final BpmnElement to = edge.getTo();
				// add edge the the main data flow
				graph.addEdge(from, to, 100);
			}
		}

		// get start nodes of the sub data flow and connect
		final Collection<BpmnElement> startNodes = subGraph.getStartNodes();
		for (final BpmnElement startNode : startNodes) {
			// set variables from in interface of the call activity
			graph.addEdge(callActivity, startNode, 100);
		}

		final Collection<BpmnElement> endNodes = subGraph.getEndNodes();
		for (final BpmnElement endNode : endNodes) {
			for (final BpmnElement succ : graph.getAdjacencyListSuccessor(callActivity)) {
				graph.addEdge(endNode, succ, 100);
			}
		}
	}

	/**
	 * Read and transform process definition into data flows
	 *
	 * @param fileScanner
	 *            FileScanner
	 * @param calledElementHierarchy
	 *            Collection of Element Hierarchy
	 * @param callActivityPath
	 *            CallActivityPath
	 * @param scanner
	 *            ProcessVariableScanner
	 * @param flowAnalysis
	 *            FlowAnalysis
	 * @return Collection of IGraphs (subgraphs)
	 */
	private Collection<Graph> createSubDataFlowsFromCallActivity(final FileScanner fileScanner,
			final Collection<String> calledElementHierarchy, final String callActivityPath,
			final ProcessVariablesScanner scanner, final FlowAnalysis flowAnalysis) {
		// read called process
		final BpmnModelInstance subModel = Bpmn.readModelFromFile(new File(ConfigConstants.getInstance().getBasepath() + callActivityPath));

		// transform process into data flow
		final ElementGraphBuilder graphBuilder = new ElementGraphBuilder(decisionRefToPathMap, processIdToPathMap,
				messageIdToVariables, processIdToVariables, rule, bpmnScanner);
		return graphBuilder.createProcessGraph(fileScanner, subModel, callActivityPath, calledElementHierarchy, scanner,
				flowAnalysis);
	}
}
