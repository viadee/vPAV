/*
 * BSD 3-Clause License
 *
 * Copyright © 2020, viadee Unternehmensberatung AG
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
import de.viadee.bpm.vPAV.FileScanner;
import de.viadee.bpm.vPAV.RuntimeConfig;
import de.viadee.bpm.vPAV.config.model.Rule;
import de.viadee.bpm.vPAV.config.reader.XmlVariablesReader;
import de.viadee.bpm.vPAV.constants.BpmnConstants;
import de.viadee.bpm.vPAV.constants.CamundaMethodServices;
import de.viadee.bpm.vPAV.processing.code.flow.*;
import de.viadee.bpm.vPAV.processing.model.data.*;
import de.viadee.bpm.vPAV.processing.model.graph.Edge;
import de.viadee.bpm.vPAV.processing.model.graph.Graph;
import de.viadee.bpm.vPAV.processing.model.graph.Path;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.bpmn.instance.*;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.util.*;

import static de.viadee.bpm.vPAV.constants.CamundaMethodServices.CORRELATE_MESSAGE;
import static de.viadee.bpm.vPAV.processing.ProcessVariableReader.addNodeAndGetNewPredecessor;

/**
 * Creates data flow graph based on a bpmn model
 */
public class ElementGraphBuilder {

    private final Map<String, BpmnElement> elementMap = new HashMap<>();

    private Map<String, String> processIdToPathMap = new HashMap<>();

    private Map<String, BpmnModelInstance> processIdToModelInstance = new HashMap<>();

    private Map<String, String> decisionRefToPathMap;

    private Map<String, Collection<String>> messageIdToVariables;

    private Map<String, Collection<String>> processIdToVariables;

    private final Map<BpmnElement, BpmnElement> splittedSubprocesses = new HashMap<>();

    private Rule rule;

    public ElementGraphBuilder() {

    }

    public ElementGraphBuilder(final Rule rule) {
        this.rule = rule;
    }

    public ElementGraphBuilder(final Map<String, String> decisionRefToPathMap,
            final Map<String, String> processIdToPathMap, final Map<String, Collection<String>> messageIdToVariables,
            final Map<String, Collection<String>> processIdToVariables, final Rule rule) {
        this.decisionRefToPathMap = decisionRefToPathMap;
        this.processIdToPathMap = processIdToPathMap;
        this.messageIdToVariables = messageIdToVariables;
        this.processIdToVariables = processIdToVariables;
        this.rule = rule;
    }

    public ElementGraphBuilder(final Map<String, String> decisionRefToPathMap,
            final Map<String, String> processIdToPathMap, final Map<String, Collection<String>> messageIdToVariables,
            final Map<String, Collection<String>> processIdToVariables) {
        this.decisionRefToPathMap = decisionRefToPathMap;
        this.processIdToPathMap = processIdToPathMap;
        this.messageIdToVariables = messageIdToVariables;
        this.processIdToVariables = processIdToVariables;
    }

    public ElementGraphBuilder(final Map<String, String> decisionRefToPathMap,
            final Map<String, String> processIdToPathMap) {
        this.decisionRefToPathMap = decisionRefToPathMap;
        this.processIdToPathMap = processIdToPathMap;
    }

    public ElementGraphBuilder(Map<String, BpmnModelInstance> processIdToModelInstance) {
        this.processIdToModelInstance = processIdToModelInstance;
    }

    /**
     * Create data flow graphs for a model
     *
     * @param fileScanner            FileScanner
     * @param modelInstance          BpmnModelInstance
     * @param processDefinition      processDefinition
     * @param calledElementHierarchy calledElementHierarchy
     * @param scanner                OuterProcessVariablesScanner
     * @param flowAnalysis           FlowAnalysis
     * @return graphCollection returns graphCollection
     */
    public Collection<Graph> createProcessGraph(final FileScanner fileScanner, final BpmnModelInstance modelInstance,
            final String processDefinition, final Collection<String> calledElementHierarchy,
            final EntryPointScanner scanner, final FlowAnalysis flowAnalysis) {

        final Collection<Graph> graphCollection = new ArrayList<>();

        final Collection<Process> processes = modelInstance.getModelElementsByType(Process.class);

        HashMap<String, ListMultimap<String, ProcessVariableOperation>> userVariables = new HashMap<>();
        try {
            // Use first process as default process
            userVariables = (new XmlVariablesReader()).read(RuntimeConfig.getInstance().getUserVariablesFilePath(),
                    processes.iterator().next().getId());
        } catch (JAXBException e) {
            e.printStackTrace();
        }

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

                // Add element to suitable collection
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
                } else if (element instanceof StartEvent) {
                    if (userVariables.containsKey("StartEvent")) {
                        // Join with variables without a defined creation Point
                        if (!userVariables.containsKey(element.getId())) {
                            userVariables.put(element.getId(), ArrayListMultimap.create());
                        }
                        userVariables.get(element.getId()).putAll(userVariables.get("StartEvent"));
                    }
                }

                createVariablesOfFlowElement(scanner, graph, node, userVariables.get(element.getId()),
                        processes.size());

                // mention element
                elementMap.put(element.getId(), node);

                if (element.getElementType().getTypeName().equals(BpmnConstants.END_EVENT)) {
                    graph.addEndNode(node);
                }
                // save process elements as a node
                graph.addVertex(node);

                if (element instanceof SubProcess) {
                    final SubProcess subprocess = (SubProcess) element;
                    addElementsSubprocess(subProcesses, flows, boundaryEvents, graph, subprocess, node,
                            processDefinition, flowAnalysis);
                }
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
     * Creates the variables map of a flow element
     *
     * @param scanner OuterProcessVariablesScanner
     * @param graph   Graph
     */
    private void createVariablesOfFlowElement(final EntryPointScanner scanner, final Graph graph,
            final BpmnElement bpmnElement,
            final ListMultimap<String, ProcessVariableOperation> userVariables, int numProcesses) {
        final FlowElement element = (FlowElement) bpmnElement.getBaseElement();
        BasicNode[] predecessor = new BasicNode[1];

        // Add user defined variables
        if (userVariables != null) {
            BasicNode userVarNode = new BasicNode(bpmnElement, ElementChapter.UserDefined,
                    KnownElementFieldType.UserDefined);

            for (Map.Entry<String, ProcessVariableOperation> var : userVariables.entries()) {
                var.getValue().initializeOperation(bpmnElement);
                userVarNode.addOperation(var.getValue());
            }

            bpmnElement.getControlFlowGraph().addNode(userVarNode);
            predecessor[0] = userVarNode;
        }

        // retrieve initial variable operation (should be WRITE)
        if (element instanceof StartEvent) {
            ArrayList<String> messageNames = new ArrayList<>();
            for (EventDefinition ed : ((StartEvent) element).getEventDefinitions()) {
                if (ed instanceof MessageEventDefinition) {
                    messageNames.add(((MessageEventDefinition) ed).getMessage().getName());
                }
            }
            String messageName = "";
            if (messageNames.size() == 1) {
                messageName = messageNames.get(0);
            }
            // add process variables for start event, which set e.g. by call startProcessInstanceByKey
            for (EntryPoint ep : scanner.getEntryPoints()) {
                if (isEntryPointApplicable(ep, graph, messageName, numProcesses)) {
                    BasicNode initVarNode = new BasicNode(bpmnElement, ElementChapter.ProcessStart,
                            KnownElementFieldType.ProcessStart);
                    String scopeId = element.getScope().getAttributeValue(BpmnConstants.ATTR_ID);

                    for (String var : ep.getProcessVariables()) {
                        ProcessVariableOperation pvo = new ProcessVariableOperation(var, VariableOperation.WRITE,
                                scopeId);
                        initVarNode.addOperation(pvo);
                    }

                    bpmnElement.getControlFlowGraph().addNode(initVarNode);
                    predecessor[0] = addNodeAndGetNewPredecessor(initVarNode, bpmnElement.getControlFlowGraph(),
                            predecessor[0]);
                }
            }
            graph.addStartNode(bpmnElement);
        } else if (element instanceof ReceiveTask || element instanceof IntermediateCatchEvent) {
            String messageName = "";
            if (element instanceof ReceiveTask) {
                if (((ReceiveTask) element).getMessage() != null) {
                    messageName = ((ReceiveTask) element).getMessage().getName();
                }
            } else {
                for (EventDefinition ed : ((IntermediateCatchEvent) element).getEventDefinitions()) {
                    if (ed instanceof MessageEventDefinition) {
                        messageName = ((MessageEventDefinition) ed).getMessage().getName();
                        break;
                    }
                }
            }

            if (!messageName.equals("")) {
                for (EntryPoint ep : scanner.getEntryPoints()) {
                    if (ep.getEntryPointName().equals(CORRELATE_MESSAGE) && ep
                            .getMessageName().equals(messageName)) {
                        BasicNode initVarNode = new BasicNode(bpmnElement, ElementChapter.Message,
                                KnownElementFieldType.Message);
                        String scopeId = element.getScope().getAttributeValue(BpmnConstants.ATTR_ID);

                        for (String var : ep.getProcessVariables()) {
                            ProcessVariableOperation pvo = new ProcessVariableOperation(var, VariableOperation.WRITE,
                                    scopeId);
                            initVarNode.addOperation(pvo);
                        }

                        bpmnElement.getControlFlowGraph().addNode(initVarNode);
                        predecessor[0] = addNodeAndGetNewPredecessor(initVarNode, bpmnElement.getControlFlowGraph(),
                                predecessor[0]);
                    }
                }
            }
        }

        // examine process variables and save it with access operation
        final ProcessVariableReader reader = new ProcessVariableReader(decisionRefToPathMap, rule);
        reader.getVariablesFromElement(bpmnElement, predecessor);
    }

    public BpmnElement getElement(final String id) {
        return elementMap.get(id);
    }

    /**
     * Create invalid paths for data flow anomalies
     *
     * @param graphCollection IGraph
     * @return invalidPathMap returns invalidPathMap
     */
    public Map<AnomalyContainer, List<Path>> createInvalidPaths(final Collection<Graph> graphCollection) {
        final Map<AnomalyContainer, List<Path>> invalidPathMap = new HashMap<>();

        for (final Graph g : graphCollection) {
            // get nodes with data anomalies
            final Map<BpmnElement, List<AnomalyContainer>> anomalies = g.getNodesWithAnomalies();

            for (final Map.Entry<BpmnElement, List<AnomalyContainer>> element : anomalies.entrySet()) {
                for (AnomalyContainer anomaly : element.getValue()) {
                    // create paths for data flow anomalies
                    final List<Path> paths = g.getAllInvalidPaths(element.getKey(), anomaly);
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
     * @param graph          IGraph
     * @param flows          Collection of SequenceFlows
     * @param boundaryEvents Collection of BoundaryEvents
     * @param subProcesses   Collection of SubProcesses
     */
    private void addEdges(final Graph graph, final Collection<SequenceFlow> flows,
            final Collection<BoundaryEvent> boundaryEvents, final Collection<SubProcess> subProcesses) {
        for (final SequenceFlow flow : flows) {
            final BpmnElement flowElement = elementMap.get(flow.getId());
            BpmnElement srcElement = elementMap.get(flow.getSource().getId());
            final BpmnElement destElement = elementMap.get(flow.getTarget().getId());

            // Check if src element is part of a splitted subprocess in order to use after element instead
            if (splittedSubprocesses.get(srcElement) != null) {
                srcElement = splittedSubprocesses.get(srcElement);
            }

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
            addEdgesOfSubprocess(graph, subProcess);
        }
    }

    /**
     * Add edges of a subprocess to data flow graph
     *
     * @param graph      IGraph
     * @param subProcess Subprocess
     */
    private void addEdgesOfSubprocess(final Graph graph, final SubProcess subProcess) {
        final BpmnElement subprocessElement = elementMap.get(subProcess.getId());
        // integration of a subprocess in data flow graph
        // inner elements will be directly connected into the graph
        final Collection<StartEvent> startEvents = subProcess.getChildElementsByType(StartEvent.class);
        final Collection<EndEvent> endEvents = subProcess.getChildElementsByType(EndEvent.class);

        // Check if subprocess itself has nodes
        boolean useElementItself = false;
        boolean isBefore = false;
        boolean bothSides = splittedSubprocesses.containsKey(subprocessElement);
        if (subprocessElement.getControlFlowGraph().getNodes().size() > 0) {
            useElementItself = true;
            ElementChapter chapter = subprocessElement.getControlFlowGraph().firstNode().getElementChapter();
            isBefore = !(chapter.equals(ElementChapter.OutputImplementation)
                    || chapter.equals(ElementChapter.ExecutionListenerEnd) || chapter
                    .equals(ElementChapter.OutputData));
        }

        if (startEvents != null && !startEvents.isEmpty() && endEvents != null && !endEvents.isEmpty()) {
            if (useElementItself && (bothSides || isBefore)) {
                for (final StartEvent startEvent : startEvents) {
                    final BpmnElement dstElement = elementMap.get(startEvent.getId());
                    graph.addEdge(subprocessElement, dstElement, 100);
                    if (bothSides) {
                        graph.removeEdge(subprocessElement, splittedSubprocesses.get(subprocessElement));
                    } else {
                        graph.removeEdge(subprocessElement, graph.getAdjacencyListSuccessor(subprocessElement).get(0));
                    }
                }
            } else {
                final Collection<SequenceFlow> incomingFlows = subProcess.getIncoming();
                for (final SequenceFlow incomingFlow : incomingFlows) {
                    final BpmnElement srcElement = elementMap.get(incomingFlow.getId());
                    for (final StartEvent startEvent : startEvents) {
                        final BpmnElement dstElement = elementMap.get(startEvent.getId());
                        graph.addEdge(srcElement, dstElement, 100);
                        graph.removeEdge(srcElement, subprocessElement);
                        srcElement.removeSuccessor(subprocessElement.getGraphId());
                        srcElement.addSuccessor(dstElement);
                    }
                }
            }

            if (useElementItself && (bothSides || !isBefore)) {
                for (final EndEvent endEvent : endEvents) {
                    final BpmnElement srcElement = elementMap.get(endEvent.getId());
                    if (bothSides) {
                        graph.addEdge(srcElement, splittedSubprocesses.get(subprocessElement), 100);
                    } else {
                        graph.addEdge(srcElement, subprocessElement, 100);
                    }
                }
            } else {
                final Collection<SequenceFlow> outgoingFlows = subProcess.getOutgoing();
                for (final EndEvent endEvent : endEvents) {
                    final BpmnElement srcElement = elementMap.get(endEvent.getId());
                    for (final SequenceFlow outgoingFlow : outgoingFlows) {
                        final BpmnElement dstElement = elementMap.get(outgoingFlow.getId());
                        graph.addEdge(srcElement, dstElement, 100);
                        graph.removeEdge(subprocessElement, dstElement);
                        dstElement.removePredecessor(subprocessElement.getGraphId());
                        dstElement.addPredecessor(srcElement);
                    }
                }
            }
        }
    }

    /**
     * Add elements from subprocess to data flow graph
     *
     * @param subProcesses      Collection of SubProcesses
     * @param flows             Collection of SequenceFlows
     * @param events            Collection of BoundaryEvents
     * @param graph             Current Graph
     * @param process           Current Process
     * @param processDefinition Current Path to process
     * @param flowAnalysis      FlowAnalysis
     */
    private void addElementsSubprocess(final Collection<SubProcess> subProcesses,
            final Collection<SequenceFlow> flows, final Collection<BoundaryEvent> events, final Graph graph,
            final SubProcess process, final BpmnElement element, final String processDefinition,
            final FlowAnalysis flowAnalysis) {
        subProcesses.add(process);
        BpmnElement secondElement = null;

        if (splitSubprocessElement(element)) {
            secondElement = (BpmnElement) element.getSuccessors().iterator().next();
            element.removeSuccessor(secondElement.getId());
            elementMap.put(secondElement.getGraphId(), secondElement);
            graph.addVertex(secondElement);
            graph.addEdge(element, secondElement, 100);
            splittedSubprocesses.put(element, secondElement);
        }

        final Collection<FlowElement> subElements = process.getFlowElements();
        for (final FlowElement subElement : subElements) {
            // add elements of the sub process as nodes
            final BpmnElement node = new BpmnElement(processDefinition, subElement, new ControlFlowGraph(),
                    flowAnalysis);
            new ProcessVariableReader(decisionRefToPathMap, rule)
                    .getVariablesFromElement(node, new BasicNode[1]);
            // mention the element
            elementMap.put(subElement.getId(), node);
            // add element as node
            graph.addVertex(node);

            if (subElement instanceof SubProcess) {
                final SubProcess subProcess = (SubProcess) subElement;
                addElementsSubprocess(subProcesses, flows, events, graph, subProcess, node, processDefinition,
                        flowAnalysis);
            } else if (subElement instanceof SequenceFlow) {
                final SequenceFlow flow = (SequenceFlow) subElement;
                flows.add(flow);
            } else if (subElement instanceof BoundaryEvent) {
                final BoundaryEvent boundaryEvent = (BoundaryEvent) subElement;
                events.add(boundaryEvent);
            }
        }
    }

    private boolean splitSubprocessElement(final BpmnElement element) {
        if (element.getControlFlowGraph().getNodes().isEmpty()) {
            return false;
        }

        BasicNode lastNodeBefore = null;
        BasicNode firstNodeAfter = null;
        ElementChapter firstNodeChapter = element.getControlFlowGraph().firstNode().getElementChapter();
        ElementChapter lastNodeChapter = element.getControlFlowGraph().lastNode().getElementChapter();
        boolean hasFirstHalf = !(firstNodeChapter.equals(ElementChapter.OutputImplementation)
                || firstNodeChapter.equals(ElementChapter.ExecutionListenerEnd) || firstNodeChapter
                .equals(ElementChapter.OutputData));
        boolean hasSecondHalf = lastNodeChapter.equals(ElementChapter.OutputImplementation)
                || lastNodeChapter.equals(ElementChapter.ExecutionListenerEnd)
                || lastNodeChapter.equals(ElementChapter.OutputData);

        if (!(hasFirstHalf && hasSecondHalf)) {
            return false;
        }

        // Should have only one predecessor
        ArrayList<BasicNode> nodesAfter = new ArrayList<>();
        BasicNode curSuccessor = element.getControlFlowGraph().lastNode();
        nodesAfter.add(curSuccessor);
        AnalysisElement predecessor = element.getControlFlowGraph().lastNode().getPredecessors().iterator().next();
        while (predecessor != null) {
            ElementChapter chapter = ((BasicNode) predecessor).getElementChapter();
            if (!(chapter.equals(ElementChapter.OutputImplementation)
                    || chapter.equals(ElementChapter.ExecutionListenerEnd) || chapter
                    .equals(ElementChapter.OutputData))) {
                lastNodeBefore = (BasicNode) predecessor;
                firstNodeAfter = curSuccessor;
                break;
            }
            curSuccessor = (BasicNode) predecessor;
            nodesAfter.add(curSuccessor);
            predecessor = curSuccessor.getPredecessors().iterator().next();
        }
        if (lastNodeBefore != null && firstNodeAfter != null) {
            BpmnElement secondElement = new BpmnElement(element.getProcessDefinition(), element.getBaseElement(),
                    new ControlFlowGraph(), element.getFlowAnalysis(), element.getId() + "_after");
            // Change edges
            element.addSuccessor(secondElement);

            // Split nodes
            nodesAfter.forEach(secondElement.getControlFlowGraph()::addNodeWithoutNewId);
            nodesAfter.forEach(element.getControlFlowGraph()::removeNode);
            firstNodeAfter.clearPredecessors();
            lastNodeBefore.clearSuccessors();

            return true;
        }
        return false;
    }

    /**
     * Integrate a called activity into data flow graph
     *
     * @param fileScanner            FileScanner
     * @param processDefinition      Current Path to process
     * @param element                CallActivity
     * @param graph                  Current Graph
     * @param calledElementHierarchy Collection of Element Hierarchy
     * @param scanner                ProcessVariableScanner
     * @param flowAnalysis           FlowAnalysis
     */
    private void integrateCallActivityFlow(final FileScanner fileScanner, final String processDefinition,
            final BpmnElement element, final FlowElement activity, final Graph graph,
            final Collection<String> calledElementHierarchy, final EntryPointScanner scanner,
            final FlowAnalysis flowAnalysis) {

        Collection<Graph> subGraphs = null;
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
                subGraphs = createSubDataFlowsFromCallActivity(fileScanner,
                        calledElementHierarchy, callActivityPath, scanner, flowAnalysis);
            }
        } else if (processIdToModelInstance.containsKey(calledElement)) {
            // get model of the called process
            BpmnModelInstance calledProcessModel = processIdToModelInstance.get(calledElement);
            // load process and transform it into a data flow graph
            subGraphs = createSubDataFlowsFromCallActivity(fileScanner,
                    calledElementHierarchy, calledProcessModel, scanner, flowAnalysis);
        }

        if (subGraphs != null) {
            for (final Graph subGraph : subGraphs) {
                // look only on the called process!
                if (subGraph.getProcessId().equals(calledElement)) {
                    // connect sub data flow with the main data flow
                    connectGraphs(graph, subGraph, element);
                }
            }
        }
    }

    /**
     * Connect graph with sub graph
     *
     * @param graph    Current Graph
     * @param subGraph Sub Graph
     */
    private void connectGraphs(final Graph graph, final Graph subGraph, final BpmnElement callActivity) {
        // read nodes of the sub data flow
        final Collection<BpmnElement> vertices = subGraph.getVertices();
        for (final BpmnElement vertex : vertices) {
            // add _ before the element id to avoid name clashes
            vertex.setGraphId("_" + vertex.getGraphId());
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

        // connect end node of sub graph
        final Collection<BpmnElement> endNodes = subGraph.getEndNodes();
        for (final BpmnElement endNode : endNodes) {
            for (final BpmnElement succ : graph.getAdjacencyListSuccessor(callActivity)) {
                graph.addEdge(endNode, succ, 100);
            }
        }

        // get start nodes of the sub data flow and connect
        final Collection<BpmnElement> startNodes = subGraph.getStartNodes();
        for (final BpmnElement startNode : startNodes) {
            // set variables from in interface of the call activity
            graph.addEdge(callActivity, startNode, 100);
        }
    }

    /**
     * Read and transform process definition into data flows
     *
     * @param fileScanner            FileScanner
     * @param calledElementHierarchy Collection of Element Hierarchy
     * @param callActivityPath       CallActivityPath
     * @param scanner                ProcessVariableScanner
     * @param flowAnalysis           FlowAnalysis
     * @return Collection of IGraphs (subgraphs)
     */
    private Collection<Graph> createSubDataFlowsFromCallActivity(final FileScanner fileScanner,
            final Collection<String> calledElementHierarchy, final String callActivityPath,
            final EntryPointScanner scanner, final FlowAnalysis flowAnalysis) {
        // read called process
        final BpmnModelInstance subModel = Bpmn
                .readModelFromFile(new File(RuntimeConfig.getInstance().getBasepath() + callActivityPath));

        // transform process into data flow
        final ElementGraphBuilder graphBuilder = new ElementGraphBuilder(decisionRefToPathMap, processIdToPathMap,
                messageIdToVariables, processIdToVariables, rule);
        return graphBuilder.createProcessGraph(fileScanner, subModel, callActivityPath, calledElementHierarchy, scanner,
                flowAnalysis);
    }

    // Used for testing
    private Collection<Graph> createSubDataFlowsFromCallActivity(final FileScanner fileScanner,
            final Collection<String> calledElementHierarchy, final BpmnModelInstance subModel,
            final EntryPointScanner scanner, final FlowAnalysis flowAnalysis) {
        // transform process into data flow
        final ElementGraphBuilder graphBuilder = new ElementGraphBuilder(decisionRefToPathMap, processIdToPathMap,
                messageIdToVariables, processIdToVariables, rule);
        return graphBuilder
                .createProcessGraph(fileScanner, subModel, subModel.getModel().getModelName(), calledElementHierarchy,
                        scanner,
                        flowAnalysis);
    }

    public boolean isEntryPointApplicable(EntryPoint ep, Graph graph, String messageName, int numProcesses) {
        if (ep.getProcessVariables().isEmpty()) {
            return false;
        }

        // Key matches process id
        if (graph.getProcessId().equals(ep.getProcessDefinitionKey())) {
            return true;
        }

        // Process id is used and we have only one process
        if (ep.getEntryPointName().equals(CamundaMethodServices.START_PROCESS_INSTANCE_BY_ID) && numProcesses == 1) {
            return true;
        }

        // Process is started by message and message names must be equal
        if ((ep.getEntryPointName().equals(CamundaMethodServices.START_PROCESS_INSTANCE_BY_MESSAGE) || ep
                .getEntryPointName().equals(CORRELATE_MESSAGE)) && ep
                .getMessageName().equals(messageName)) {
            return true;
        }

        // Message names must match and only one process is allowed because we cannot match process ids to processes
        return ep.getEntryPointName().equals(CamundaMethodServices.START_PROCESS_INSTANCE_BY_MESSAGE_AND_PROCESS_DEF)
                && ep
                .getMessageName().equals(messageName) && numProcesses == 1;
    }
}
