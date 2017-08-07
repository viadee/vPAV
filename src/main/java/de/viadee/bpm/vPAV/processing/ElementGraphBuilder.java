/**
 * Copyright � 2017, viadee Unternehmensberatung GmbH All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met: 1. Redistributions of source code must retain the above copyright notice, this list of
 * conditions and the following disclaimer. 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation and/or other materials provided with the
 * distribution. 3. All advertising materials mentioning features or use of this software must display the following
 * acknowledgement: This product includes software developed by the viadee Unternehmensberatung GmbH. 4. Neither the
 * name of the viadee Unternehmensberatung GmbH nor the names of its contributors may be used to endorse or promote
 * products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY <COPYRIGHT HOLDER> ''AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO
 * EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package de.viadee.bpm.vPAV.processing;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.Activity;
import org.camunda.bpm.model.bpmn.instance.BaseElement;
import org.camunda.bpm.model.bpmn.instance.BoundaryEvent;
import org.camunda.bpm.model.bpmn.instance.CallActivity;
import org.camunda.bpm.model.bpmn.instance.EndEvent;
import org.camunda.bpm.model.bpmn.instance.Event;
import org.camunda.bpm.model.bpmn.instance.ExtensionElements;
import org.camunda.bpm.model.bpmn.instance.FlowElement;
import org.camunda.bpm.model.bpmn.instance.Message;
import org.camunda.bpm.model.bpmn.instance.MessageEventDefinition;
import org.camunda.bpm.model.bpmn.instance.ParallelGateway;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.bpmn.instance.SequenceFlow;
import org.camunda.bpm.model.bpmn.instance.StartEvent;
import org.camunda.bpm.model.bpmn.instance.SubProcess;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaIn;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaOut;

import de.viadee.bpm.vPAV.RuntimeConfig;
import de.viadee.bpm.vPAV.processing.model.data.AnomalyContainer;
import de.viadee.bpm.vPAV.processing.model.data.BpmnElement;
import de.viadee.bpm.vPAV.processing.model.data.ElementChapter;
import de.viadee.bpm.vPAV.processing.model.data.KnownElementFieldType;
import de.viadee.bpm.vPAV.processing.model.data.ProcessVariable;
import de.viadee.bpm.vPAV.processing.model.data.VariableOperation;
import de.viadee.bpm.vPAV.processing.model.graph.Edge;
import de.viadee.bpm.vPAV.processing.model.graph.Graph;
import de.viadee.bpm.vPAV.processing.model.graph.IGraph;
import de.viadee.bpm.vPAV.processing.model.graph.Path;

/**
 * creates data flow graph based on a bpmn model
 *
 */
public class ElementGraphBuilder {

    private Map<String, BpmnElement> elementMap = new HashMap<String, BpmnElement>();

    private Map<String, String> processIdToPathMap;

    private Map<String, String> decisionRefToPathMap;

    private Map<String, Collection<String>> messageIdToVariables;

    private Map<String, Collection<String>> processIdToVariables;

    public ElementGraphBuilder() {
    }

    public ElementGraphBuilder(final Map<String, String> decisionRefToPathMap,
            final Map<String, String> processIdToPathMap, final Map<String, Collection<String>> messageIdToVariables,
            final Map<String, Collection<String>> processIdToVariables) {
        this.decisionRefToPathMap = decisionRefToPathMap;
        this.processIdToPathMap = processIdToPathMap;
        this.messageIdToVariables = messageIdToVariables;
        this.processIdToVariables = processIdToVariables;
    }

    /**
     * create data flow graphs for a model
     *
     * @param modelInstance
     * @param processdefinition
     * @param cl
     * @return
     */
    public Collection<IGraph> createProcessGraph(final BpmnModelInstance modelInstance,
            final String processdefinition, final Collection<String> calledElementHierarchy) {

        final Collection<IGraph> graphCollection = new ArrayList<IGraph>();

        final Collection<Process> processes = modelInstance.getModelElementsByType(Process.class);
        for (final Process process : processes) {
            final IGraph graph = new Graph(process.getId());
            final Collection<FlowElement> elements = process.getFlowElements();
            final Collection<SequenceFlow> flows = new ArrayList<SequenceFlow>();
            final Collection<BoundaryEvent> boundaryEvents = new ArrayList<BoundaryEvent>();
            final Collection<SubProcess> subProcesses = new ArrayList<SubProcess>();
            final Collection<CallActivity> callActivities = new ArrayList<CallActivity>();

            for (final FlowElement element : elements) {
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
                    addElementsSubprocess(subProcesses, flows, boundaryEvents, graph, subprocess,
                            processdefinition);
                }
                // initialize element
                final BpmnElement node = new BpmnElement(processdefinition, element);
                // examine process variables and save it with access operation
                final Map<String, ProcessVariable> variables = new ProcessVariableReader(decisionRefToPathMap)
                        .getVariablesFromElement(node);
                // examine process variables for element and set it
                node.setProcessVariables(variables);
                // mention element
                elementMap.put(element.getId(), node);
                if (element.getElementType().getBaseType().getBaseType().getTypeName().equals("event")) {
                    // add variables for message event (set by outer class)
                    addProcessVariablesForMessageName(element, node);
                }
                if (element.getElementType().getTypeName().equals("startEvent")) {
                    // add process variables for start event, which set by call startProcessInstanceByKey
                    final String processId = node.getBaseElement().getParentElement().getAttributeValue("id");
                    addProcessVariablesByStartForProcessId(node, processId);

                    graph.addStartNode(node);
                }
                if (element.getElementType().getTypeName().equals("endEvent")) {
                    graph.addEndNode(node);
                }
                // save process elements as a node
                graph.addVertex(node);
            }
            // add edges into the graph
            addEdges(processdefinition, graph, flows, boundaryEvents, subProcesses);

            // resolve call activities and integrate called processes
            for (final CallActivity callActivity : callActivities) {
                integrateCallActivityFlow(processdefinition, modelInstance, callActivity, graph,
                        calledElementHierarchy);
            }

            graphCollection.add(graph);
        }

        return graphCollection;
    }

    /**
     * add process variables on start event for a specific process id
     *
     * @param node
     * @param processId
     */
    private void addProcessVariablesByStartForProcessId(final BpmnElement node,
            final String processId) {
        if (processIdToVariables != null && processId != null) {
            final Collection<String> outerVariables = processIdToVariables.get(processId);
            // add variables
            if (outerVariables != null) {
                for (final String varName : outerVariables) {
                    node.setProcessVariable(varName,
                            new ProcessVariable(varName, node, ElementChapter.OutstandingVariable,
                                    KnownElementFieldType.Class, null, VariableOperation.WRITE, ""));
                }
            }
        }
    }

    /**
     * add process variables on event for a specific message name
     *
     * @param element
     * @param node
     */
    private void addProcessVariablesForMessageName(final FlowElement element,
            final BpmnElement node) {
        if (messageIdToVariables != null) {
            if (element instanceof Event) {
                final Event event = (Event) element;
                final Collection<MessageEventDefinition> messageEventDefinitions = event
                        .getChildElementsByType(MessageEventDefinition.class);
                if (messageEventDefinitions != null) {
                    for (MessageEventDefinition eventDef : messageEventDefinitions) {
                        final Message message = eventDef.getMessage();
                        final String messageName = message.getName();
                        final Collection<String> outerVariables = messageIdToVariables.get(messageName);
                        if (outerVariables != null) {
                            for (final String varName : outerVariables) {
                                node.setProcessVariable(varName,
                                        new ProcessVariable(varName, node, ElementChapter.OutstandingVariable,
                                                KnownElementFieldType.Class, null, VariableOperation.WRITE, ""));
                            }
                        }
                    }
                }
            }
        }
    }

    public BpmnElement getElement(final String id) {
        return elementMap.get(id);
    }

    /**
     * create invalid paths for data flow anomalies
     *
     * @param graphCollection
     * @return
     */
    public Map<AnomalyContainer, List<Path>> createInvalidPaths(
            final Collection<IGraph> graphCollection) {
        final Map<AnomalyContainer, List<Path>> invalidPathMap = new HashMap<AnomalyContainer, List<Path>>();

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
                    invalidPathMap.put(anomaly, new ArrayList<Path>(paths));
                }
            }
        }

        return invalidPathMap;
    }

    /**
     * add edges to data flow graph
     *
     * @param processdefinition
     * @param graph
     * @param flows
     * @param boundaryEvents
     * @param subProcesses
     */
    private void addEdges(final String processdefinition, final IGraph graph,
            final Collection<SequenceFlow> flows, final Collection<BoundaryEvent> boundaryEvents,
            final Collection<SubProcess> subProcesses) {
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
            final Collection<StartEvent> startEvents = subProcess
                    .getChildElementsByType(StartEvent.class);
            final Collection<EndEvent> endEvents = subProcess.getChildElementsByType(EndEvent.class);
            if (startEvents != null && startEvents.size() > 0 && endEvents != null
                    && endEvents.size() > 0) {
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
     * add elements from subprocess to data flow graph
     *
     * @param subProcesses
     * @param flows
     * @param graph
     * @param process
     * @param processdefinitionPath
     * @param cl
     */
    private void addElementsSubprocess(final Collection<SubProcess> subProcesses,
            final Collection<SequenceFlow> flows, final Collection<BoundaryEvent> events,
            final IGraph graph, final SubProcess process, final String processdefinitionPath) {
        subProcesses.add(process);
        final Collection<FlowElement> subElements = process.getFlowElements();
        for (final FlowElement subElement : subElements) {
            if (subElement instanceof SubProcess) {
                final SubProcess subProcess = (SubProcess) subElement;
                addElementsSubprocess(subProcesses, flows, events, graph, subProcess, processdefinitionPath);
            } else if (subElement instanceof SequenceFlow) {
                final SequenceFlow flow = (SequenceFlow) subElement;
                flows.add(flow);
            } else if (subElement instanceof BoundaryEvent) {
                final BoundaryEvent boundaryEvent = (BoundaryEvent) subElement;
                events.add(boundaryEvent);
            }
            // add elements of the sub process as nodes
            final BpmnElement node = new BpmnElement(processdefinitionPath, subElement);
            // determine process variables with operations
            final Map<String, ProcessVariable> variables = new ProcessVariableReader(decisionRefToPathMap)
                    .getVariablesFromElement(node);
            // set process variables for the node
            node.setProcessVariables(variables);
            // mention the element
            elementMap.put(subElement.getId(), node);
            // add element as node
            graph.addVertex(node);
        }
    }

    /**
     * integrate a called activity into data flow graph
     *
     * @param processdefinition
     * @param modelInstance
     * @param callActivity
     * @param graph
     * @param classLoader
     * @param calledElementHierarchy
     */
    private void integrateCallActivityFlow(final String processdefinition,
            final BpmnModelInstance modelInstance, final CallActivity callActivity, final IGraph graph,
            final Collection<String> calledElementHierarchy) {

        final String calledElement = callActivity.getCalledElement();

        // check call hierarchy to avoid deadlocks
        if (calledElementHierarchy.contains(calledElement)) {
            throw new RuntimeException("call activity hierarchy causes a deadlock (see "
                    + processdefinition + ", " + callActivity.getId() + "). please avoid loops.");
        }
        calledElementHierarchy.add(calledElement);

        // integrate only, if file locations for process ids are known
        if (processIdToPathMap != null && processIdToPathMap.get(calledElement) != null) {

            // 1) read in- and output variables from call activity
            final Collection<String> inVariables = new ArrayList<String>();
            final Collection<String> outVariables = new ArrayList<String>();
            readCallActivityDataInterfaces(callActivity, inVariables, outVariables);

            // 2) add parallel gateways before and after the call activity in the main data flow
            // They are necessary for connecting the sub process with the main flow
            final List<BpmnElement> parallelGateways = addParallelGatewaysBeforeAndAfterCallActivityInMainDataFlow(
                    modelInstance, callActivity, graph);
            final BpmnElement parallelGateway1 = parallelGateways.get(0);
            final BpmnElement parallelGateway2 = parallelGateways.get(1);

            // get file path of the called process
            final String callActivityPath = processIdToPathMap.get(calledElement);
            if (callActivityPath != null) {
                // 3) load process and transform it into a data flow graph
                final Collection<IGraph> subgraphs = createSubDataFlowsFromCallActivity(
                        RuntimeConfig.getInstance().getClassLoader(),
                        calledElementHierarchy, callActivityPath);

                for (final IGraph subgraph : subgraphs) {
                    // look only on the called process!
                    if (subgraph.getProcessId().equals(calledElement)) {
                        // 4) connect sub data flow with the main data flow
                        connectParallelGatewaysWithSubDataFlow(graph, inVariables, outVariables,
                                parallelGateway1, parallelGateway2, subgraph);
                    }
                }
            }
        }
    }

    /**
     * add parallel gateways before and after a call activity
     *
     * there are needed to connect the called process with the main flow
     *
     * @param modelInstance
     * @param callActivity
     * @param graph
     * @return parallel gateway elements
     */
    private List<BpmnElement> addParallelGatewaysBeforeAndAfterCallActivityInMainDataFlow(
            final BpmnModelInstance modelInstance, final CallActivity callActivity, final IGraph graph) {

        final ParallelGateway element1 = modelInstance.newInstance(ParallelGateway.class);
        element1.setAttributeValue("id", "_gw_in", true);

        final ParallelGateway element2 = modelInstance.newInstance(ParallelGateway.class);
        element2.setAttributeValue("id", "_gw_out", true);

        final List<BpmnElement> elements = new ArrayList<BpmnElement>();
        final BpmnElement parallelGateway1 = new BpmnElement(null, element1);
        final BpmnElement parallelGateway2 = new BpmnElement(null, element2);
        elements.add(parallelGateway1);
        elements.add(parallelGateway2);

        graph.addVertex(parallelGateway1);
        graph.addVertex(parallelGateway2);

        connectParallelGatewaysWithMainDataFlow(callActivity, graph, parallelGateway1,
                parallelGateway2);

        return elements;
    }

    /**
     * connect the parallel gateways in the data flow before and after the call activity
     *
     * @param graph
     * @param inVariables
     * @param outVariables
     * @param parallelGateway1
     * @param parallelGateway2
     * @param subgraph
     */
    private void connectParallelGatewaysWithSubDataFlow(final IGraph graph,
            final Collection<String> inVariables, final Collection<String> outVariables,
            final BpmnElement parallelGateway1, final BpmnElement parallelGateway2,
            final IGraph subgraph) {

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
                final BpmnElement from = edge.from;
                final BpmnElement to = edge.to;
                // add edge the the main data flow
                graph.addEdge(from, to, 100);
            }
        }

        // get start and end nodes of the sub data flow and connect parallel gateways in the main flow
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
     * read and transform process definition into data flows
     *
     * @param classLoader
     * @param calledElementHierarchy
     * @param callActivityPath
     * @return
     */
    private Collection<IGraph> createSubDataFlowsFromCallActivity(final ClassLoader classLoader,
            final Collection<String> calledElementHierarchy, final String callActivityPath) {
        // read called process
        final BpmnModelInstance submodel = Bpmn.readModelFromFile(new File(callActivityPath));

        // transform process into data flow
        final ElementGraphBuilder graphBuilder = new ElementGraphBuilder(decisionRefToPathMap,
                processIdToPathMap, messageIdToVariables, processIdToVariables);
        final Collection<IGraph> subgraphs = graphBuilder.createProcessGraph(submodel, callActivityPath,
                calledElementHierarchy);
        return subgraphs;
    }

    /**
     * integrate parallel gateways into the main data flow before and after the call activity
     *
     * @param callActivity
     * @param graph
     * @param parallelGateway1
     * @param parallelGateway2
     */
    private void connectParallelGatewaysWithMainDataFlow(final CallActivity callActivity,
            final IGraph graph, final BpmnElement parallelGateway1, final BpmnElement parallelGateway2) {

        // read incoming and outgoing sequence flows of the call activity
        final SequenceFlow incomingSequenceFlow = callActivity.getIncoming().iterator().next();
        final SequenceFlow outgoingSequenceFlow = callActivity.getOutgoing().iterator().next();

        // remove edges
        graph.removeEdge(elementMap.get(incomingSequenceFlow.getId()),
                elementMap.get(callActivity.getId()));
        graph.removeEdge(elementMap.get(callActivity.getId()),
                elementMap.get(outgoingSequenceFlow.getId()));

        // link parallel gateways with the existing data flow
        graph.addEdge(elementMap.get(incomingSequenceFlow.getId()), parallelGateway1, 100);
        graph.addEdge(parallelGateway2, elementMap.get(outgoingSequenceFlow.getId()), 100);
        graph.addEdge(parallelGateway1, elementMap.get(callActivity.getId()), 100);
        graph.addEdge(elementMap.get(callActivity.getId()), parallelGateway2, 100);
    }

    /**
     * read in- und output variables for a call activity
     *
     * @param callActivity
     * @param inVariables
     * @param outVariables
     */
    private void readCallActivityDataInterfaces(final CallActivity callActivity,
            final Collection<String> inVariables, final Collection<String> outVariables) {

        final ExtensionElements extensionElements = callActivity.getExtensionElements();
        if (extensionElements != null) {
            final List<CamundaIn> inputAssociations = extensionElements.getElementsQuery()
                    .filterByType(CamundaIn.class).list();
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
