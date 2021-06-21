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
package de.viadee.bpm.vPAV.processing.code.flow;

import de.viadee.bpm.vPAV.constants.BpmnConstants;
import de.viadee.bpm.vPAV.processing.model.data.*;
import de.viadee.bpm.vPAV.processing.model.graph.Graph;
import org.camunda.bpm.model.bpmn.impl.BpmnModelConstants;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.bpmn.instance.*;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;

import java.util.*;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static de.viadee.bpm.vPAV.processing.model.data.VariableOperation.*;

public class FlowAnalysis {

    public static final Logger LOGGER = Logger.getLogger(FlowAnalysis.class.getName());

    private final LinkedHashMap<String, AnalysisElement> nodes;

    private int operationCounter = 0;

    public FlowAnalysis() {
        this.nodes = new LinkedHashMap<>();
    }

    private final HashMap<String, AnalysisElement> nodesBeforeCallActivities = new HashMap<>();

    private final HashSet<String> callActivitiesInMapping = new HashSet<>();

    private final HashSet<String> callActivitiesOutMapping = new HashSet<>();

    /**
     * Given a collection of graphs, this method is the sole entrance to the
     * analysis of the graphs. First process model and control flow graph of
     * delegates are embedded to create a single graph. Then the graph is analyzed
     * conservatively by following the Reaching Definition algorithm. For
     * discovering data flow anomalies inside a single block, a sequential check on
     * unit basis is performed. Lastly, anomalies are extracted and appended to the
     * parent element (for visualization)
     *
     * @param graphCollection Collection of graphs
     */
    public void analyze(final Collection<Graph> graphCollection) {
        for (Graph graph : graphCollection) {
            embedControlFlowGraph(graph);
            computeReachingDefinitions();
            computeLineByLine();
            extractAnomalies();
        }
    }

    /**
     * Embeds the control flow graphs of bpmn elements into the process model
     *
     * @param graph Given Graph
     */
    private void embedControlFlowGraph(final Graph graph) {
        // Add all elements on bpmn level
        graph.getVertexInfo().keySet().forEach(element -> {
            element.clearPredecessors();
            graph.getAdjacencyListPredecessor(element)
                    .forEach(element::addPredecessor);
            graph.getAdjacencyListSuccessor(element)
                    .forEach(element::addSuccessor);
            this.nodes.put(element.getGraphId(), element);
        });

        // Add all nodes on source code level and correct the pointers
        final LinkedHashMap<String, AnalysisElement> temp = new LinkedHashMap<>(nodes);
        final LinkedHashMap<String, AnalysisElement> cfgNodes = new LinkedHashMap<>();
        final ArrayList<String> ids = new ArrayList<>();
        temp.values().forEach(analysisElement -> {
            boolean hasNodesBefore = !(analysisElement.getBaseElement() instanceof CallActivity);

            if (analysisElement.getControlFlowGraph().hasNodes()) {
                BasicNode firstNode = analysisElement.getControlFlowGraph().firstNode();
                BasicNode lastNode = analysisElement.getControlFlowGraph().lastNode();
                boolean hasNodesAfter;

                if (analysisElement.getBaseElement() instanceof CallActivity) {
                    // Split nodes in "before" and "after" nodes.
                    BasicNode lastNodeBefore = null;
                    BasicNode firstNodeAfter = null;
                    BasicNode predecessor = null;
                    ElementChapter chapter;
                    chapter = firstNode.getElementChapter();
                    boolean isFirstHalf = !(chapter.equals(ElementChapter.OUTPUT_IMPLEMENTATION)
                            || chapter.equals(ElementChapter.EXECUTION_LISTENER_END) || chapter
                            .equals(ElementChapter.OUTPUT_DATA));

                    // Set predecessor and successor relationships between nodes
                    for (BasicNode curNode : analysisElement.getControlFlowGraph().getNodes().values()) {
                        if (predecessor != null) {
                            chapter = curNode.getElementChapter();
                            if ((chapter.equals(ElementChapter.OUTPUT_IMPLEMENTATION)
                                    || chapter.equals(ElementChapter.EXECUTION_LISTENER_END)
                                    || chapter.equals(ElementChapter.OUTPUT_DATA)) && isFirstHalf) {
                                // Split in before and after nodes
                                isFirstHalf = false;
                                lastNodeBefore = predecessor;
                                firstNodeAfter = curNode;
                                // Clear connection between before and after nodes
                                firstNodeAfter.clearPredecessors();
                            }
                        }
                        predecessor = curNode;
                    }

                    hasNodesBefore = isFirstHalf || lastNodeBefore != null;
                    hasNodesAfter = !isFirstHalf;

                    if (hasNodesBefore && !hasNodesAfter) {
                        lastNodeBefore = lastNode;
                        for (AnalysisElement succ : analysisElement.getSuccessors()) {
                            if (succ.getBaseElement() instanceof SequenceFlow) {
                                nodesBeforeCallActivities.put(succ.getGraphId(), lastNodeBefore);
                                break;
                            }
                        }
                    } else if (!hasNodesBefore && hasNodesAfter) {
                        firstNodeAfter = firstNode;
                        nodesBeforeCallActivities
                                .put(firstNodeAfter.getGraphId(), analysisElement.getPredecessors().get(0));
                    } else {
                        nodesBeforeCallActivities.put(firstNodeAfter.getGraphId(), lastNodeBefore);
                    }

                    for (AnalysisElement succ : analysisElement.getSuccessors()) {
                        if (succ.getBaseElement() instanceof StartEvent) {
                            if (hasNodesBefore) {
                                // Replace call activity by "before" nodes

                                // Predecessor of child start event is last node before
                                succ.clearPredecessors();
                                succ.addPredecessor(lastNodeBefore);
                                lastNodeBefore.clearSuccessors();
                                lastNodeBefore.addSuccessor(succ);

                                // Replace incoming element connections with first node
                                firstNode.clearPredecessors();
                                for (AnalysisElement preds : analysisElement.getPredecessors()) {
                                    preds.removeSuccessor(analysisElement.getGraphId());
                                    preds.addSuccessor(firstNode);
                                    firstNode.addPredecessor(preds);
                                }

                            } else {
                                // Build direct connections between predecessors of call activity and child
                                // start event
                                succ.clearPredecessors();
                                analysisElement.getPredecessors().forEach(preds -> {
                                    preds.removeSuccessor(analysisElement.getGraphId());
                                    preds.addSuccessor(succ);
                                    succ.addPredecessor(preds);
                                });
                            }

                        } else if (succ.getBaseElement() instanceof SequenceFlow) {
                            AnalysisElement endEvent = succ;
                            // Find end event of subprocess
                            for (AnalysisElement nestedPreds : succ.getPredecessors()) {
                                if (nestedPreds.getBaseElement() instanceof EndEvent) {
                                    endEvent = nestedPreds;
                                }
                            }

                            if (endEvent.equals(succ)) {
                                // End event was not found
                                LOGGER.severe("End event in child process was not found.");
                                return;
                            }

                            succ.clearPredecessors();
                            endEvent.clearSuccessors();

                            if (hasNodesAfter) {
                                endEvent.addSuccessor(firstNodeAfter);
                                firstNodeAfter.addPredecessor(endEvent);
                                succ.addPredecessor(lastNode);
                                lastNode.addSuccessor(succ);
                            } else {
                                succ.addPredecessor(endEvent);
                                endEvent.addSuccessor(succ);
                            }

                        }

                    }

                } else {
                    // Replace element with first block
                    for (AnalysisElement pred : analysisElement.getPredecessors()) {
                        pred.removeSuccessor(analysisElement.getId());
                        pred.addSuccessor(firstNode);
                        firstNode.addPredecessor(pred);
                    }

                    // Replace element with last block
                    for (AnalysisElement succ : analysisElement.getSuccessors()) {
                        succ.removePredecessor(analysisElement.getId());
                        succ.addPredecessor(lastNode);
                        lastNode.addSuccessor(succ);
                    }
                }

                final LinkedHashMap<String, ProcessVariableOperation> initialVariables = new LinkedHashMap<>();

                analysisElement.getOperations().values().forEach(operation -> {
                    if (operation.getFieldType().equals(KnownElementFieldType.Initial)) {
                        initialVariables.put(operation.getId(), operation);
                    }
                });

                // If we have initial operations, we cant have input mapping (restriction of
                // start event)
                // Set initial operations as input for the first block and later remove bpmn
                // element
                if (!initialVariables.isEmpty() && hasNodesBefore) {
                    firstNode.addDefined(initialVariables);
                }

                // Remember id of elements to be removed
                analysisElement.getControlFlowGraph().getNodes().values()
                        .forEach(node -> cfgNodes.put(node.getGraphId(), node));
                ids.add(analysisElement.getGraphId());
            } else {
                if (analysisElement.getBaseElement() instanceof CallActivity) {
                    analysisElement.getSuccessors().forEach(succ -> {
                        if (succ.getBaseElement() instanceof StartEvent) {
                            succ.clearPredecessors();
                            LinkedHashMap<String, AnalysisElement> preds = new LinkedHashMap<>(
                                    analysisElement.getPredecessors().stream()
                                            .collect(Collectors
                                                    .toMap(AnalysisElement::getGraphId, Function.identity())));
                            succ.setPredecessors(preds);
                            preds.forEach((key, value) -> {
                                value.removeSuccessor(analysisElement.getId());
                                value.addSuccessor(succ);
                            });
                        } else if (succ.getBaseElement() instanceof SequenceFlow) {
                            // Find end event
                            AnalysisElement endEvent = null;
                            for (AnalysisElement nestedPred : succ.getPredecessors()) {
                                if (nestedPred.getBaseElement() instanceof EndEvent) {
                                    endEvent = nestedPred;
                                }
                            }

                            final LinkedHashMap<String, ProcessVariableOperation> camundaOutput = new LinkedHashMap<>();

                            for (ProcessVariableOperation operation : analysisElement.getOperations().values()) {
                                if (endEvent != null
                                        && operation.getFieldType().equals(KnownElementFieldType.CamundaOut)) {
                                    if (operation.getOperation().equals(READ)) {
                                        endEvent.getOperations().put(operation.getId(), operation);
                                    } else {
                                        endEvent.getDefined().put(operation.getId(), operation);
                                    }

                                } else if (operation.getFieldType().equals(KnownElementFieldType.OutputParameter)) {
                                    camundaOutput.put(operation.getId(), operation);
                                }
                            }

                            succ.addDefined(camundaOutput);
                            succ.getPredecessors().forEach(pred -> {
                                if (pred.getBaseElement() instanceof CallActivity) {
                                    succ.removePredecessor(pred.getId());
                                }
                            });
                            nodesBeforeCallActivities.put(succ.getGraphId(), analysisElement.getPredecessors().get(0));
                        }
                    });
                    ids.add(analysisElement.getGraphId());
                }
                // In case we have start event that maps a message to a method
                final LinkedHashMap<String, ProcessVariableOperation> initialOperations = new LinkedHashMap<>();
                analysisElement.getOperations().values().forEach(operation -> {
                    if (operation.getFieldType().equals(KnownElementFieldType.Initial)) {
                        initialOperations.put(operation.getId(), operation);
                    }
                });
                analysisElement.addDefined(initialOperations);
            }
            embedCallActivities(analysisElement, hasNodesBefore);
        });

        temp.putAll(cfgNodes);
        nodes.putAll(temp);
        ids.forEach(nodes::remove);
    }

    /**
     * Embeds call activities
     *
     * @param analysisElement Current element
     */
    private void embedCallActivities(AnalysisElement analysisElement, boolean hasNodesBefore) {
        final LinkedHashMap<String, ProcessVariableOperation> camundaIn = new LinkedHashMap<>();
        final ArrayList<ProcessVariableOperation> operationList = new ArrayList<>();
        if (analysisElement.getBaseElement() instanceof CallActivity) {
            analysisElement.getSuccessors().forEach(succ -> {
                if (succ.getBaseElement() instanceof StartEvent) {
                    analysisElement.getOperations().values().forEach(operation -> {
                        if (operation.getFieldType().equals(KnownElementFieldType.CamundaIn)) {
                            if (operation.getOperation().equals(VariableOperation.READ)) {
                                succ.getOperations().put(operation.getId(), operation);
                            } else {
                                succ.getDefined().put(operation.getId(), operation);
                            }
                            operationList.add(operation);
                        } else if (operation.getFieldType().equals(KnownElementFieldType.InputParameter)) {
                            camundaIn.put(operation.getId(), operation);
                            operationList.add(operation);
                        }
                    });
                    // Add input parameters only as defined if Call Activity has no nodes that are
                    // executed before the start event
                    if (!hasNodesBefore) {
                        succ.addDefined(camundaIn);
                    }
                } else if (succ.getBaseElement() instanceof SequenceFlow) {
                    analysisElement.removeSuccessor(succ.getGraphId());
                } else {
                    analysisElement.removeSuccessor(succ.getGraphId());
                    succ.removePredecessor(analysisElement.getGraphId());
                }
            });
        }

        // Remove operation from base sets, because we moved them to In/Out
        operationList.forEach(analysisElement::removeOperation);

        // Clear wrong predecessors in case of call activities
        if (analysisElement.getBaseElement() instanceof StartEvent) {
            analysisElement.getPredecessors().forEach(pred -> {
                if (pred.getBaseElement() instanceof EndEvent) {
                    analysisElement.removePredecessor(pred.getGraphId());
                }
            });
        }

        // Clear wrong successors in case of call activities
        if (analysisElement.getBaseElement() instanceof EndEvent) {
            analysisElement.getSuccessors().forEach(succ -> {
                if (succ.getBaseElement() instanceof StartEvent) {
                    analysisElement.removeSuccessor(succ.getGraphId());
                }
            });
        }
    }

    /**
     * Uses the approach from ALSU07 (Reaching Definitions) to compute data flow
     * anomalies across the embedded CFG
     */
    private void computeReachingDefinitions() {
        boolean change = true;
        while (change) {
            change = false;
            for (AnalysisElement analysisElement : nodes.values()) {
                // Calculate in-sets (intersection of predecessors)
                final LinkedHashMap<String, ProcessVariableOperation> inUsed = new LinkedHashMap<>();
                final LinkedHashMap<String, ProcessVariableOperation> inUnused = new LinkedHashMap<>();
                final Set<ProcessVariableOperation> inUsedT = new HashSet<>(inUsed.values());
                final Set<ProcessVariableOperation> inUnusedT = new HashSet<>(inUnused.values());
                final List<AnalysisElement> predecessors = analysisElement.getPredecessors();

                // If more than one predecessor, take intersection of operations (conservatism)
                if (predecessors.size() > 1) {
                    for (int i = 0; i < predecessors.size(); i++) {
                        if (i == 0) {
                            inUsedT.addAll(predecessors.get(i).getOutUsed().values());
                            inUnusedT.addAll(predecessors.get(i).getOutUnused().values());
                        } else {
                            inUsedT.retainAll(predecessors.get(i).getOutUsed().values());
                            inUnusedT.retainAll(predecessors.get(i).getOutUnused().values());
                        }
                    }
                    inUsedT.forEach(pvo -> inUsed.put(pvo.getId(), pvo));
                    inUnusedT.forEach(pvo -> inUnused.put(pvo.getId(), pvo));
                    // Else take union to propagate operations
                } else {
                    for (AnalysisElement pred : predecessors) {
                        Map<String, ProcessVariableOperation>[] inSets = filterInputVariables(pred,
                                analysisElement);
                        inUsed.putAll(inSets[0]);
                        inUnused.putAll(inSets[1]);
                    }
                }

                analysisElement.setInUsed(inUsed);
                analysisElement.setInUnused(inUnused);

                // Get old values before calculating new values and later check for changes
                final LinkedHashMap<String, ProcessVariableOperation> oldOutUnused = analysisElement.getOutUnused();
                final LinkedHashMap<String, ProcessVariableOperation> oldOutUsed = analysisElement.getOutUsed();

                // Calculate out-sets for used definitions (transfer functions)
                final LinkedHashMap<String, ProcessVariableOperation> inUsedTemp = new LinkedHashMap<>(
                        analysisElement.getInUsed());
                final LinkedHashMap<String, ProcessVariableOperation> internalUnion = new LinkedHashMap<>(
                        analysisElement.getInUnused());
                final LinkedHashMap<String, ProcessVariableOperation> tempUsed = new LinkedHashMap<>(
                        analysisElement.getUsed());

                Optional<Map.Entry<String, ProcessVariableOperation>> oldOperation;
                // Variables are overwritten if new operation
                for (Map.Entry<String, ProcessVariableOperation> operation : analysisElement.getDefined().entrySet()) {
                    oldOperation = internalUnion.entrySet().stream()
                            .filter(entry -> entry.getValue().getName().equals(operation.getValue().getName()))
                            .findFirst();
                    // Remove old operation from input set
                    oldOperation.ifPresent(stringProcessVariableOperationEntry -> internalUnion
                            .remove(stringProcessVariableOperationEntry.getKey()));
                    internalUnion.put(operation.getKey(), operation.getValue());

                    // Add operation to used set if variable is defined again although it was used
                    // before
                    oldOperation = inUsed.entrySet().stream()
                            .filter(entry -> entry.getValue().getName().equals(operation.getValue().getName()))
                            .findFirst();
                    oldOperation.ifPresent(o -> tempUsed.put(o.getKey(), o.getValue()));
                }

                final LinkedHashMap<String, ProcessVariableOperation> internalIntersection = new LinkedHashMap<>(
                        getIntersection(internalUnion, analysisElement.getUsed()));
                inUsedTemp.putAll(internalIntersection);
                final LinkedHashMap<String, ProcessVariableOperation> outUsed = new LinkedHashMap<>(
                        getSetDifference(inUsedTemp, analysisElement.getKilled()));

                // Calculate out-sets for unused definitions (transfer functions)
                final LinkedHashMap<String, ProcessVariableOperation> tempKillSet = new LinkedHashMap<>();
                tempKillSet.putAll(analysisElement.getKilled());
                tempKillSet.putAll(tempUsed);

                final LinkedHashMap<String, ProcessVariableOperation> outUnused = new LinkedHashMap<>(
                        getSetDifference(internalUnion, tempKillSet));

                analysisElement.setOutUsed(outUsed);
                analysisElement.setOutUnused(outUnused);

                if (!oldOutUnused.equals(outUnused) || !oldOutUsed.equals(outUsed)) {
                    change = true;
                }
            }
        }
    }

    private Map<String, ProcessVariableOperation>[] filterInputVariables(AnalysisElement predecessor,
            AnalysisElement analysisElement) {
        String scopePredecessor = predecessor.getBaseElement().getScope().getAttributeValue(BpmnConstants.ATTR_ID);
        String scopeElement = analysisElement.getBaseElement().getScope().getAttributeValue(BpmnConstants.ATTR_ID);
        LinkedHashMap<String, ProcessVariableOperation> tempInUsed = new LinkedHashMap<>(predecessor.getOutUsed());
        LinkedHashMap<String, ProcessVariableOperation> tempInUnused = new LinkedHashMap<>(predecessor.getOutUnused());

        // Subprocess or call activity
        if (!isChildOrSiblingOfScope(analysisElement.getBaseElement(), scopePredecessor) &&
                !predecessor.getBaseElement().getScope().getElementType().getTypeName().equals("subProcess")) {
            // Check for local variables in element like input parameters
            filterLocalVariables(predecessor, tempInUnused, tempInUsed);

            // Change scope of variables so that they are continued to be passed
            if (isCallActivityMappingInAll(analysisElement)) {
                copyOperationsWithCallActivityScope(tempInUnused, tempInUsed,
                        ((Process) analysisElement.getBaseElement().getParentElement()).getId());

            } else if (isCallActivityMappingOutAll(predecessor)) {
                copyOperationsWithCallActivityScope(tempInUnused, tempInUsed,
                        ((Process) analysisElement.getBaseElement().getParentElement()).getId());
            } else {
                filterVariablesWithoutScope(predecessor.getOutUnused(), tempInUnused, Arrays.asList(scopeElement,
                        analysisElement.getParentElement().getGraphId()));
                filterVariablesWithoutScope(predecessor.getOutUsed(), tempInUsed, Arrays.asList(scopeElement,
                        analysisElement.getParentElement().getGraphId()));

                if (nodesBeforeCallActivities.containsKey(analysisElement.getGraphId())) {
                    AnalysisElement predecessorCallActivity = nodesBeforeCallActivities
                            .get(analysisElement.getGraphId());
                    // Pass Input parameters forward that are present before and after the call activity
                    predecessorCallActivity.getOutUnused().forEach((key, value) -> {
                        if (value.getScopeId().equals(analysisElement.getParentElement().getGraphId()) || value
                                .getScopeId()
                                .equals(scopeElement)) {
                            tempInUnused.put(key, value);
                        }
                    });
                    predecessorCallActivity.getOutUsed().forEach((key, value) -> {
                        if (value.getScopeId().equals(analysisElement.getParentElement().getGraphId()) || value
                                .getScopeId()
                                .equals(scopeElement)) {
                            tempInUnused.put(key, value);
                        }
                    });
                }

                // Call Activity
                if (predecessor.getBaseElement() instanceof EndEvent && analysisElement instanceof BasicNode
                        && ((BasicNode) analysisElement).getElementChapter()
                        .equals(ElementChapter.OUTPUT_DATA)) {
                    predecessor.getOutUnused().forEach(tempInUnused::put);
                    predecessor.getOutUsed().forEach(tempInUsed::put);
                }
            }

        } else if (!predecessor.getParentElement().getGraphId()
                .equals(analysisElement.getParentElement().getGraphId())) {
            // Check for local variables in element like input parameters
            filterLocalVariables(predecessor, tempInUnused, tempInUsed);

        } else if (predecessor instanceof BasicNode && ((BasicNode) predecessor).getElementChapter()
                .equals(ElementChapter.OUTPUT_DATA)) {
            filterVariablesWithoutScope(predecessor.getOutUnused(), tempInUnused, Arrays.asList(scopeElement,
                    analysisElement.getParentElement().getGraphId()));
            filterVariablesWithoutScope(predecessor.getOutUsed(), tempInUsed, Arrays.asList(scopeElement,
                    analysisElement.getParentElement().getGraphId()));
        }

        return new LinkedHashMap[] { tempInUsed, tempInUnused };
    }

    private void copyOperationsWithCallActivityScope(LinkedHashMap<String, ProcessVariableOperation> tempInUnused,
            LinkedHashMap<String, ProcessVariableOperation> tempInUsed,
            String newScope) {
        LinkedHashMap<String, ProcessVariableOperation> tempInUsedNew = new LinkedHashMap<>();
        LinkedHashMap<String, ProcessVariableOperation> tempInUnusedNew = new LinkedHashMap<>();

        // Copy variables so that they keep the scope of the process
        tempInUnused.forEach((key, value) -> tempInUnusedNew.put(key, new ProcessVariableOperation(value, newScope)));
        tempInUsed.forEach((key, value) -> tempInUsedNew.put(key, new ProcessVariableOperation(value, newScope)));

        // Update input hash maps
        tempInUnused.clear();
        tempInUsed.clear();
        tempInUnusedNew.forEach(tempInUnused::put);
        tempInUsedNew.forEach(tempInUsed::put);
    }

    private boolean isCallActivityMappingInAll(AnalysisElement element) {
        ModelElementInstance parentElement = element.getBaseElement().getParentElement();
        return (parentElement instanceof Process) && callActivitiesInMapping
                .contains(((Process) parentElement).getId());
    }

    private boolean isCallActivityMappingOutAll(AnalysisElement predecessor) {
        // Check if call activity has a variable mapping of all
        ModelElementInstance parentElement = predecessor.getBaseElement().getParentElement();
        return (parentElement instanceof Process) && callActivitiesOutMapping
                .contains(((Process) parentElement).getId());
    }

    private void filterLocalVariables(AnalysisElement predecessor,
            LinkedHashMap<String, ProcessVariableOperation> tempInUnused,
            LinkedHashMap<String, ProcessVariableOperation> tempInUsed) {

        // Check for local variables in element like input parameters
        filterVariablesWithScope(predecessor.getOutUnused(), tempInUnused,
                Collections.singletonList(predecessor.getParentElement().getGraphId()));
        filterVariablesWithScope(predecessor.getOutUsed(), tempInUsed,
                Collections.singletonList(predecessor.getParentElement().getGraphId()));
    }

    private void filterVariablesWithScope(LinkedHashMap<String, ProcessVariableOperation> sourceList,
            LinkedHashMap<String, ProcessVariableOperation> targetList,
            List<String> scopes) {
        sourceList.forEach((key, value) -> {
            if (scopes.contains(value.getScopeId())) {
                targetList.remove(key);
            }
        });
    }

    private void filterVariablesWithoutScope(LinkedHashMap<String, ProcessVariableOperation> sourceList,
            LinkedHashMap<String, ProcessVariableOperation> targetList,
            List<String> scopes) {
        sourceList.forEach((key, value) -> {
            if (!scopes.contains(value.getScopeId())) {
                targetList.remove(key);
            }
        });
    }

    private boolean isChildOrSiblingOfScope(BaseElement element, String scopeId) {
        if (element == null) {
            return false;
        }
        if (element.getId().equals(scopeId)) {
            return true;
        }
        if (!(element.getParentElement() instanceof BaseElement)) {
            return false;
        }
        return isChildOrSiblingOfScope((BaseElement) element.getParentElement(), scopeId);
    }

    /**
     * Finds anomalies inside blocks by checking statements unit by unit
     */
    private void computeLineByLine() {
        nodes.values().forEach(analysisElement -> {
            final LinkedHashMap<String, ProcessVariableOperation> operations = new LinkedHashMap<>(
                    analysisElement.getOperations());

            // mapping delegate method/class
            // If element is delegate variable mapping node, ignore mapped variables because
            // they are only active inside child
            if (analysisElement.getParentElement().getBaseElement() instanceof CallActivity
                    && analysisElement instanceof Node) {
                Node tmpNode = (Node) analysisElement;
                if (tmpNode.getElementChapter().equals(ElementChapter.INPUT_IMPLEMENTATION)
                        || tmpNode.getElementChapter().equals(ElementChapter.OUTPUT_IMPLEMENTATION)) {
                    String childProcessId = ((CallActivity) analysisElement.getParentElement().getBaseElement())
                            .getCalledElement();
                    analysisElement.getOperations().forEach((key, value) -> {
                        if (value.getScopeId().equals(childProcessId)) {
                            operations.remove(key);
                        }
                    });
                }
            }

            if (operations.size() >= 2) {
                ProcessVariableOperation prev = null;
                for (ProcessVariableOperation operation : operations.values()) {
                    if (prev == null) {
                        prev = operation;
                        continue;
                    }
                    checkAnomaly(operation.getElement(), operation, prev, analysisElement.getGraphId());
                    prev = operation;
                }
            }
        });
    }

    /**
     * Based on the calculated sets, extract the anomalies found on source code
     * level
     */
    private void extractAnomalies() {
        nodes.values().forEach(node -> {
            if (node.getParentElement().getBaseElement() instanceof CallActivity && node instanceof Node) {
                Node tmpNode = (Node) node;
                if (tmpNode.getElementChapter().equals(ElementChapter.INPUT_IMPLEMENTATION)
                        || tmpNode.getElementChapter().equals(ElementChapter.OUTPUT_IMPLEMENTATION)) {
                    String childProcessId = ((CallActivity) node.getParentElement().getBaseElement())
                            .getCalledElement();
                    handleDelegateVariableMapping(node, childProcessId);
                }
            } else {
                ddAnomalies(node);

                duAnomalies(node);

                urAnomalies(node);

                uuAnomalies(node);
            }
        });
    }

    private void handleDelegateVariableMapping(AnalysisElement element, String childProcessId) {
        // Save original sets
        final LinkedHashMap<String, ProcessVariableOperation> originalOperations = new LinkedHashMap<>(
                element.getOperations());
        final LinkedHashMap<String, ProcessVariableOperation> originalDefined = new LinkedHashMap<>(
                element.getDefined());
        final LinkedHashMap<String, ProcessVariableOperation> originalUsed = new LinkedHashMap<>(element.getUsed());
        final LinkedHashMap<String, ProcessVariableOperation> originalInUnused = new LinkedHashMap<>(
                element.getInUnused());
        final LinkedHashMap<String, ProcessVariableOperation> originalInUsed = new LinkedHashMap<>(element.getInUsed());

        // Delete variables that are only used in the child process
        filterDelegateVariables(originalOperations, element.getOperations(), childProcessId);
        filterDelegateVariables(originalDefined, element.getDefined(), childProcessId);
        filterDelegateVariables(originalUsed, element.getUsed(), childProcessId);
        filterDelegateVariables(originalInUnused, element.getInUnused(), childProcessId);
        filterDelegateVariables(originalInUsed, element.getInUsed(), childProcessId);

        // Run anomaly check
        ddAnomalies(element);
        duAnomalies(element);
        urAnomalies(element);
        uuAnomalies(element);

        // Restore original sets
        element.setOperations(originalOperations);
        element.setDefined(originalDefined);
        element.setUsed(originalUsed);
        element.setInUnused(originalInUnused);
        element.setInUsed(originalInUsed);
    }

    private void filterDelegateVariables(final LinkedHashMap<String, ProcessVariableOperation> in,
            final LinkedHashMap<String, ProcessVariableOperation> out, String childProcessId) {
        in.forEach((key, value) -> {
            if (value.getScopeId().equals(childProcessId)) {
                out.remove(key);
            }
        });
    }

    /**
     * Extract DD anomalies
     *
     * @param node Current node
     */
    private void ddAnomalies(final AnalysisElement node) {
        final LinkedHashMap<String, ProcessVariableOperation> ddAnomalies = new LinkedHashMap<>(
                getIntersection(node.getInUnused(), node.getDefined()));
        if (!ddAnomalies.isEmpty()) {
            ddAnomalies.forEach((k,
                    v) -> node.addSourceCodeAnomaly(new AnomalyContainer(v.getName(), Anomaly.DD, node.getGraphId(),
                    node.getBaseElement().getId(),
                    node.getBaseElement().getAttributeValue(BpmnModelConstants.BPMN_ATTRIBUTE_NAME), v)));
        }
    }

    /**
     * Extract DU anomalies
     *
     * @param node Current node
     */
    private void duAnomalies(final AnalysisElement node) {
        final LinkedHashMap<String, ProcessVariableOperation> duAnomalies = new LinkedHashMap<>(
                getIntersection(node.getInUnused(), node.getKilled()));
        if (!duAnomalies.isEmpty()) {
            duAnomalies.forEach((k,
                    v) -> node.addSourceCodeAnomaly(new AnomalyContainer(v.getName(), Anomaly.DU, node.getGraphId(),
                    node.getBaseElement().getId(),
                    node.getBaseElement().getAttributeValue(BpmnModelConstants.BPMN_ATTRIBUTE_NAME), v)));
        }
    }

    /**
     * Extract UR anomalies
     *
     * @param node Current node
     */
    private void urAnomalies(final AnalysisElement node) {
        final LinkedHashMap<String, ProcessVariableOperation> urAnomaliesTemp = new LinkedHashMap<>(node.getUsed());
        final LinkedHashMap<String, ProcessVariableOperation> urAnomalies = new LinkedHashMap<>(urAnomaliesTemp);

        urAnomaliesTemp.forEach((key, value) -> node.getInUnused().forEach((key2, value2) -> {
            if (value.getName().equals(value2.getName())) {
                urAnomalies.remove(key);
            }
        }));

        urAnomaliesTemp.forEach((key, value) -> node.getInUsed().forEach((key2, value2) -> {
            if (value.getName().equals(value2.getName())) {
                urAnomalies.remove(key);
            }
        }));

        urAnomaliesTemp.forEach((key, value) -> node.getDefined().forEach((key2, value2) -> {
            if (value.getName().equals(value2.getName()) && value.getIndex() > value2.getIndex()) {
                urAnomalies.remove(key);
            }
        }));

        if (!urAnomalies.isEmpty()) {
            urAnomalies.forEach((k,
                    v) -> node.addSourceCodeAnomaly(new AnomalyContainer(v.getName(), Anomaly.UR, node.getGraphId(),
                    node.getBaseElement().getId(),
                    node.getBaseElement().getAttributeValue(BpmnModelConstants.BPMN_ATTRIBUTE_NAME), v)));
        }
    }

    /**
     * Extract UU anomalies
     *
     * @param node Current node
     */
    private void uuAnomalies(final AnalysisElement node) {
        final LinkedHashMap<String, ProcessVariableOperation> uuAnomaliesTemp = new LinkedHashMap<>(node.getKilled());
        final LinkedHashMap<String, ProcessVariableOperation> uuAnomalies = new LinkedHashMap<>(uuAnomaliesTemp);

        uuAnomaliesTemp.forEach((key, value) -> node.getInUnused().forEach((key2, value2) -> {
            if (value.getName().equals(value2.getName())) {
                uuAnomalies.remove(key);
            }
        }));

        uuAnomaliesTemp.forEach((key, value) -> node.getInUsed().forEach((key2, value2) -> {
            if (value.getName().equals(value2.getName())) {
                uuAnomalies.remove(key);
            }
        }));

        uuAnomaliesTemp.forEach((key, value) -> node.getDefined().forEach((key2, value2) -> {
            if (value.getName().equals(value2.getName()) && value.getIndex() > value2.getIndex()) {
                uuAnomalies.remove(key);
            }
        }));

        if (!uuAnomalies.isEmpty()) {
            uuAnomalies.forEach((k,
                    v) -> node.addSourceCodeAnomaly(new AnomalyContainer(v.getName(), Anomaly.UU, node.getGraphId(),
                    node.getBaseElement().getId(),
                    node.getBaseElement().getAttributeValue(BpmnModelConstants.BPMN_ATTRIBUTE_NAME), v)));
        }
    }

    /**
     * Check for data-flow anomaly between current and previous variable operation
     *
     * @param element Current BpmnElement
     * @param curr    current operation
     * @param prev    previous operation
     */
    private void checkAnomaly(final BpmnElement element, final ProcessVariableOperation curr,
            final ProcessVariableOperation prev, final String nodeId) {
        if (urSourceCode(prev, curr)) {
            element.addSourceCodeAnomaly(
                    new AnomalyContainer(curr.getName(), Anomaly.UR, nodeId, element.getBaseElement().getId(),
                            element.getBaseElement().getAttributeValue(BpmnModelConstants.BPMN_ATTRIBUTE_NAME), curr));
        }
        if (ddSourceCode(prev, curr)) {
            element.addSourceCodeAnomaly(
                    new AnomalyContainer(curr.getName(), Anomaly.DD, nodeId, element.getBaseElement().getId(),
                            element.getBaseElement().getAttributeValue(BpmnModelConstants.BPMN_ATTRIBUTE_NAME), curr));
        }
        if (duSourceCode(prev, curr)) {
            element.addSourceCodeAnomaly(
                    new AnomalyContainer(curr.getName(), Anomaly.DU, nodeId, element.getBaseElement().getId(),
                            element.getBaseElement().getAttributeValue(BpmnModelConstants.BPMN_ATTRIBUTE_NAME), curr));
        }
        if (uuSourceCode(prev, curr)) {
            element.addSourceCodeAnomaly(
                    new AnomalyContainer(curr.getName(), Anomaly.UU, nodeId, element.getBaseElement().getId(),
                            element.getBaseElement().getAttributeValue(BpmnModelConstants.BPMN_ATTRIBUTE_NAME), curr));
        }
    }

    /**
     * UU anomaly: second last operation of PV is DELETE, last operation is DELETE
     *
     * @param prev Previous ProcessVariable
     * @param curr Current ProcessVariable
     * @return true/false
     */
    private boolean uuSourceCode(ProcessVariableOperation prev, ProcessVariableOperation curr) {
        return curr.getOperation().equals(DELETE) && prev.getOperation().equals(DELETE);
    }

    /**
     * UR anomaly: second last operation of PV is DELETE, last operation is READ
     *
     * @param prev Previous ProcessVariable
     * @param curr Current ProcessVariable
     * @return true/false
     */
    private boolean urSourceCode(final ProcessVariableOperation prev, final ProcessVariableOperation curr) {
        return curr.getOperation().equals(READ) && prev.getOperation().equals(DELETE);
    }

    /**
     * DD anomaly: second last operation of PV is DEFINE, last operation is DELETE
     *
     * @param prev Previous ProcessVariable
     * @param curr Current ProcessVariable
     * @return true/false
     */
    private boolean ddSourceCode(final ProcessVariableOperation prev, final ProcessVariableOperation curr) {
        return curr.getOperation().equals(WRITE) && prev.getOperation().equals(WRITE) &&
                curr.getName().equals(prev.getName());
    }

    /**
     * DU anomaly: second last operation of PV is DEFINE, last operation is DELETE
     *
     * @param prev Previous ProcessVariable
     * @param curr Current ProcessVariable
     * @return true/false
     */
    private boolean duSourceCode(final ProcessVariableOperation prev, final ProcessVariableOperation curr) {
        return curr.getOperation().equals(DELETE) && prev.getOperation().equals(WRITE);
    }

    /**
     * Helper method to create the set difference of two given maps (based on
     * variable names)
     *
     * @param mapOne First map
     * @param mapTwo Second map
     * @return Set difference of given maps
     */
    private LinkedHashMap<String, ProcessVariableOperation> getSetDifference(
            final LinkedHashMap<String, ProcessVariableOperation> mapOne,
            final LinkedHashMap<String, ProcessVariableOperation> mapTwo) {
        final LinkedHashMap<String, ProcessVariableOperation> setDifference = new LinkedHashMap<>(mapOne);

        mapOne.forEach((key, value) -> mapTwo.forEach((key2, value2) -> {
            if (value.getName().equals(value2.getName()) && isDelegateVariable(value) == isDelegateVariable(value2)) {
                setDifference.remove(key);
            }
        }));
        return setDifference;
    }

    /**
     * Helper method to create the intersection of two given maps
     *
     * @param mapOne First map
     * @param mapTwo Second map
     * @return Intersection of given maps
     */
    private LinkedHashMap<String, ProcessVariableOperation> getIntersection(
            final LinkedHashMap<String, ProcessVariableOperation> mapOne,
            final LinkedHashMap<String, ProcessVariableOperation> mapTwo) {
        final LinkedHashMap<String, ProcessVariableOperation> intersection = new LinkedHashMap<>();

        mapOne.forEach((key, value) -> mapTwo.forEach((key2, value2) -> {
            if (value.getName().equals(value2.getName()) && isDelegateVariable(value) == isDelegateVariable(value2)) {
                intersection.put(key, value);
            }
        }));
        return intersection;
    }

    public Map<String, AnalysisElement> getNodes() {
        return nodes;
    }

    public Set<String> getCallActivitiesInMapping() {
        return callActivitiesInMapping;
    }

    public Set<String> getCallActivitiesOutMapping() {
        return callActivitiesOutMapping;
    }

    public int getOperationCounter() {
        return operationCounter;
    }

    public void incrementOperationCounter() {
        this.operationCounter++;
    }

    private boolean isDelegateVariable(ProcessVariableOperation value) {
        return (value.getChapter().equals(ElementChapter.INPUT_IMPLEMENTATION)
                || value.getChapter().equals(ElementChapter.OUTPUT_IMPLEMENTATION))
                && value.getScopeId().equals(((CallActivity) value.getElement().getBaseElement()).getCalledElement());
    }

    public void addCallActivityAllInMapping(String id) {
        this.callActivitiesInMapping.add(id);
    }

    public void addCallActivityAllOutMapping(String id) {
        this.callActivitiesOutMapping.add(id);
    }
}
