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
package de.viadee.bpm.vPAV.processing.code.flow;

import de.viadee.bpm.vPAV.processing.model.data.*;
import de.viadee.bpm.vPAV.processing.model.graph.Graph;

import java.util.*;


public class FlowAnalysis {

    private LinkedHashMap<String, AnalysisElement> nodes;

    public FlowAnalysis() {
        this.nodes = new LinkedHashMap<>();
    }

    public void analyze(final Collection<Graph> graphCollection) {
        for (Graph graph : graphCollection) {
            embedControlFlowGraph(graph);
            printGraph();
            computeReachingDefinitions();
            extractAnomalies();
        }
    }


    private void printGraph() {
        for (Map.Entry<String, AnalysisElement> entry : nodes.entrySet()) {
            entry.getValue().getPredecessors().forEach(element -> System.out.println("Current: " + entry.getValue().getId() + " -> Pred: " + element.getId()));
        }
    }

    private void embedControlFlowGraph(final Graph graph) {
        // Add all elements on bpmn level
        graph.getVertexInfo().keySet().forEach(element -> {
            AnalysisElement analysisElement = new BpmnElementDecorator(element);
            analysisElement.clearPredecessors();
            graph.getAdjacencyListPredecessor(element).forEach(value -> analysisElement.addPredecessor(new BpmnElementDecorator(value)));
            graph.getAdjacencyListSuccessor(element).forEach(value -> analysisElement.addSuccessor(new BpmnElementDecorator(value)));
            this.nodes.put(analysisElement.getId(), analysisElement);
        });

        graph.getVertexInfo().keySet().forEach(element -> {
            AnalysisElement analysisElement = new BpmnElementDecorator(element);
            this.nodes.put(analysisElement.getId(), analysisElement);
        });


        // Add all nodes on source code level and correct the pointers
        final LinkedHashMap<String, AnalysisElement> temp = new LinkedHashMap<>(nodes);
        final LinkedHashMap<String, AnalysisElement> cfgNodes = new LinkedHashMap<>();
        final ArrayList<String> ids = new ArrayList<>();
        temp.values().forEach(analysisElement -> {
            if (analysisElement.getControlFlowGraph().hasNodes()) {
                analysisElement.getControlFlowGraph().computePredecessorRelations();

                final Node firstNode = analysisElement.getControlFlowGraph().firstNode();
                final Node lastNode = analysisElement.getControlFlowGraph().lastNode();

                LinkedHashMap<String, ProcessVariableOperation> inputVariables = new LinkedHashMap<>();
                LinkedHashMap<String, ProcessVariableOperation> outputVariables = new LinkedHashMap<>();
                analysisElement.getOperations().values().forEach(operation -> {
                    if (operation.getFieldType().equals(KnownElementFieldType.InputParameter)) {
                        inputVariables.put(operation.getId(), operation);
                    }
                    if (operation.getFieldType().equals(KnownElementFieldType.OutputParameter)) {
                        outputVariables.put(operation.getId(), operation);
                    }
                });
                firstNode.setInUnused(inputVariables);
                lastNode.setOutUnused(outputVariables);

                analysisElement.getPredecessors().forEach(pred -> pred.addSuccessor(new NodeDecorator(pred)));
                analysisElement.getSuccessors().forEach(succ -> {
                    succ.removePredecessor(analysisElement.getId());
                    succ.addPredecessor(new NodeDecorator(lastNode));
                });

                Iterator<Node> iterator = analysisElement.getControlFlowGraph().getNodes().values().iterator();
                Node predDelegate = null;
                while (iterator.hasNext()) {
                    Node currNode = iterator.next();
                    if (predDelegate == null) {
                        predDelegate = currNode;
                    } else {
                        if (currNode.firstOperation() != null && predDelegate.lastOperation() != null &&
                                !currNode.firstOperation().getChapter().equals(predDelegate.lastOperation().getChapter())) {
                            currNode.setPredsIntraProcedural(predDelegate.getId());
                            predDelegate = currNode;
                        }
                    }
                }

                analysisElement.getControlFlowGraph().getNodes().values().forEach(node -> cfgNodes.put(node.getId(), node));
                ids.add(firstNode.getParentElement().getBaseElement().getId());
            }
        });
        temp.putAll(cfgNodes);
        nodes.putAll(temp);
        ids.forEach(id -> nodes.remove(id));
    }

    /**
     * Uses the approach from ALSU07 (Reaching Definitions) to compute data flow
     * anomalies across the CFG
     */
    private void computeReachingDefinitions() {
        boolean change = true;
        while (change) {
            change = false;
            for (AnalysisElement analysisElement : nodes.values()) {
                // Calculate in-sets (intersection of predecessors)
                final LinkedHashMap<String, ProcessVariableOperation> inUsed = analysisElement.getInUsed();
                final LinkedHashMap<String, ProcessVariableOperation> inUnused = analysisElement.getInUnused();
                for (AnalysisElement pred : analysisElement.getPredecessors()) {
                    inUsed.putAll(pred.getOutUsed());
                    inUnused.putAll(pred.getOutUnused());
                }
                analysisElement.setInUsed(inUsed);
                analysisElement.setInUnused(inUnused);

                // Calculate out-sets for used definitions (transfer functions)
                final LinkedHashMap<String, ProcessVariableOperation> outUsed = new LinkedHashMap<>();
                outUsed.putAll(analysisElement.getUsed());
                outUsed.putAll(getSetDifference(analysisElement.getInUsed(), analysisElement.getKilled()));
                analysisElement.setOutUsed(outUsed);

                // Calculate out-sets for unused definitions (transfer functions)
                final LinkedHashMap<String, ProcessVariableOperation> outUnused = new LinkedHashMap<>(
                        analysisElement.getDefined());
                final LinkedHashMap<String, ProcessVariableOperation> tempIntersection = new LinkedHashMap<>();
                tempIntersection.putAll(getSetDifference(analysisElement.getInUnused(), analysisElement.getKilled()));
                tempIntersection.putAll(getSetDifference(tempIntersection, analysisElement.getUsed()));
                outUnused.putAll(tempIntersection);
                analysisElement.setOutUnused(outUnused);

                // Compare old values with new values and check for changes
                final LinkedHashMap<String, ProcessVariableOperation> oldOutUnused = analysisElement
                        .getOutUnused();
                final LinkedHashMap<String, ProcessVariableOperation> oldOutUsed = analysisElement
                        .getOutUsed();

                if (!oldOutUnused.equals(outUnused) || !oldOutUsed.equals(outUsed)) {
                    change = true;
                }
            }
        }
    }

    /**
     * Finds anomalies inside blocks by checking statements unit by unit
     * @param element
     *            Current BpmnElement
     */
    private void computeLineByLine(final BpmnElement element) {
        element.getControlFlowGraph().getNodes().values().forEach(node -> {
            if (node.getOperations().size() >= 2) {
                ProcessVariableOperation prev = null;
                for (ProcessVariableOperation operation : node.getOperations().values()) {
                    if (prev == null) {
                        prev = operation;
                        continue;
                    }
                    checkAnomaly(element, operation, prev);
                    prev = operation;
                }
            }
        });
    }

    /**
     * Based on the calculated sets, extract the anomalies found on source code
     * level
     *
     */
    private void extractAnomalies() {
        nodes.values().forEach(node -> {
            // DD (inUnused U defined)
            ddAnomalies(node);

            // DU (inUnused U killed)
            duAnomalies(node);

            // UR (used - inUnused - inUsed - defined)
            urAnomalies(node);

            // UU ()
            uuAnomalies(node);

            // -R ()
//			nopRAnomalies(element, node);

            // D- (inUnused U inUsed - (defined - killed - used))
//			dNopAnomalies(element, node);

        });
    }

    /**
     * Extract DD anomalies
     *
     * @param node
     *            Current node
     */
    private void ddAnomalies(final AnalysisElement node) {
        final LinkedHashMap<String, ProcessVariableOperation> ddAnomalies = new LinkedHashMap<>(
                getIntersection(node.getInUnused(), node.getDefined()));
        if (!ddAnomalies.isEmpty()) {
            ddAnomalies.forEach((k, v) -> node.addSourceCodeAnomaly(
                    new AnomalyContainer(v.getName(), Anomaly.DD, node.getId(), v)));
        }
    }

    /**
     * Extract DU anomalies
     *
     * @param node
     *            Current node
     */
    private void duAnomalies(final AnalysisElement node) {
        final LinkedHashMap<String, ProcessVariableOperation> duAnomalies = new LinkedHashMap<>(
                getIntersection(node.getInUnused(), node.getKilled()));
        if (!duAnomalies.isEmpty()) {
            duAnomalies.forEach((k, v) -> node.addSourceCodeAnomaly(
                    new AnomalyContainer(v.getName(), Anomaly.DU, node.getId(), v)));
        }
    }

    /**
     * Extract UR anomalies
     *
     * @param node
     *            Current node
     */
    private void urAnomalies(final AnalysisElement node) {
        final LinkedHashMap<String, ProcessVariableOperation> urAnomaliesTemp = new LinkedHashMap<>(
                node.getUsed());
        final LinkedHashMap<String, ProcessVariableOperation> urAnomalies = new LinkedHashMap<>(
                urAnomaliesTemp);

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

        if (!urAnomalies.isEmpty()) {
            urAnomalies.forEach((k, v) -> node.addSourceCodeAnomaly(
                    new AnomalyContainer(v.getName(), Anomaly.UR, node.getId(), v)));
        }
    }

    /**
     * Extract UU anomalies
     *
     * @param node
     *            Current node
     */
    private void uuAnomalies(final AnalysisElement node) {
        final LinkedHashMap<String, ProcessVariableOperation> uuAnomaliesTemp = new LinkedHashMap<>(
                node.getKilled());
        final LinkedHashMap<String, ProcessVariableOperation> uuAnomalies = new LinkedHashMap<>(
                uuAnomaliesTemp);

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

       // TODO: Check definition of UU


        if (!uuAnomalies.isEmpty()) {
            uuAnomalies.forEach((k, v) -> node.addSourceCodeAnomaly(
                    new AnomalyContainer(v.getName(), Anomaly.UU, node.getId(), v)));
        }

    }

    /**
     * Extract -R anomalies
     *
     * @param element
     *            Current BpmnElement
     * @param node
     *            Current node
     */
    private void nopRAnomalies(BpmnElement element, Node node) {

    }

    /**
     * Extract D- anomalies
     *
     * @param element
     *            Current BpmnElement
     * @param node
     *            Current node
     */
    private void dNopAnomalies(final BpmnElement element, final Node node) {
        final LinkedHashMap<String, ProcessVariableOperation> dNopAnomaliesTemp = new LinkedHashMap<>(
                node.getInUnused());
        dNopAnomaliesTemp.putAll(node.getInUsed());
        final LinkedHashMap<String, ProcessVariableOperation> dNopAnomalies = new LinkedHashMap<>(
                dNopAnomaliesTemp);

        dNopAnomaliesTemp.forEach((key, value) -> node.getDefined().forEach((key2, value2) -> {
            if (value.getName().equals(value2.getName())) {
                dNopAnomalies.remove(key);
            }
        }));

        dNopAnomaliesTemp.forEach((key, value) -> node.getKilled().forEach((key2, value2) -> {
            if (value.getName().equals(value2.getName())) {
                dNopAnomalies.remove(key);
            }
        }));

        dNopAnomaliesTemp.forEach((key, value) -> node.getUsed().forEach((key2, value2) -> {
            if (value.getName().equals(value2.getName())) {
                dNopAnomalies.remove(key);
            }
        }));

        if (!dNopAnomalies.isEmpty()) {
            dNopAnomalies.forEach((k, v) -> element.addSourceCodeAnomaly(
                    new AnomalyContainer(v.getName(), Anomaly.D, element.getBaseElement().getId(), v)));
        }
    }

    /**
     * Check for data-flow anomaly between current and previous variable operation
     *
     * @param element
     *            Current BpmnElement
     * @param curr
     *            current operation
     * @param prev
     *            previous operation
     */
    private void checkAnomaly(final BpmnElement element, final ProcessVariableOperation curr,
                              final ProcessVariableOperation prev) {
        if (urSourceCode(prev, curr)) {
            element.addSourceCodeAnomaly(
                    new AnomalyContainer(curr.getName(), Anomaly.UR, element.getBaseElement().getId(), curr));
        }
        if (ddSourceCode(prev, curr)) {
            element.addSourceCodeAnomaly(
                    new AnomalyContainer(curr.getName(), Anomaly.DD, element.getBaseElement().getId(), curr));
        }
        if (duSourceCode(prev, curr)) {
            element.addSourceCodeAnomaly(
                    new AnomalyContainer(curr.getName(), Anomaly.DU, element.getBaseElement().getId(), curr));
        }
        if (uuSourceCode(prev, curr)) {
            element.addSourceCodeAnomaly(
                    new AnomalyContainer(curr.getName(), Anomaly.UU, element.getBaseElement().getId(), curr));
        }
    }

    /**
     * UU anomaly: second last operation of PV is DELETE, last operation is DELETE
     *
     * @param prev
     *            Previous ProcessVariable
     * @param curr
     *            Current ProcessVariable
     * @return true/false
     */
    private boolean uuSourceCode(ProcessVariableOperation prev, ProcessVariableOperation curr) {
        return curr.getOperation().equals(VariableOperation.DELETE)
                && prev.getOperation().equals(VariableOperation.DELETE);
    }

    /**
     * UR anomaly: second last operation of PV is DELETE, last operation is READ
     *
     * @param prev
     *            Previous ProcessVariable
     * @param curr
     *            Current ProcessVariable
     * @return true/false
     */
    private boolean urSourceCode(final ProcessVariableOperation prev, final ProcessVariableOperation curr) {
        return curr.getOperation().equals(VariableOperation.READ)
                && prev.getOperation().equals(VariableOperation.DELETE);
    }

    /**
     * DD anomaly: second last operation of PV is DEFINE, last operation is DELETE
     *
     * @param prev
     *            Previous ProcessVariable
     * @param curr
     *            Current ProcessVariable
     * @return true/false
     */
    private boolean ddSourceCode(final ProcessVariableOperation prev, final ProcessVariableOperation curr) {
        return curr.getOperation().equals(VariableOperation.WRITE)
                && prev.getOperation().equals(VariableOperation.WRITE);
    }

    /**
     * DU anomaly: second last operation of PV is DEFINE, last operation is DELETE
     *
     * @param prev
     *            Previous ProcessVariable
     * @param curr
     *            Current ProcessVariable
     * @return true/false
     */
    private boolean duSourceCode(final ProcessVariableOperation prev, final ProcessVariableOperation curr) {
        return curr.getOperation().equals(VariableOperation.DELETE)
                && prev.getOperation().equals(VariableOperation.WRITE);
    }

    /**
     * Helper method to create the set difference of two given maps (based on variable names)
     *
     * @param mapOne
     *            First map
     * @param mapTwo
     *            Second map
     * @return Set difference of given maps
     */
    private LinkedHashMap<String, ProcessVariableOperation> getSetDifference(
            final LinkedHashMap<String, ProcessVariableOperation> mapOne,
            final LinkedHashMap<String, ProcessVariableOperation> mapTwo) {
        final LinkedHashMap<String, ProcessVariableOperation> setDifference = new LinkedHashMap<>(
                mapOne);

        mapOne.keySet().forEach(key -> {
            if (mapTwo.containsKey(key)) {
                setDifference.remove(key);
            }
        });
        return setDifference;
    }

    /**
     * Helper method to create the intersection of two given maps
     *
     * @param mapOne
     *            First map
     * @param mapTwo
     *            Second map
     * @return Intersection of given maps
     */
    private LinkedHashMap<String, ProcessVariableOperation> getIntersection(
            final LinkedHashMap<String, ProcessVariableOperation> mapOne,
            final LinkedHashMap<String, ProcessVariableOperation> mapTwo) {
        final LinkedHashMap<String, ProcessVariableOperation> intersection = new LinkedHashMap<>();

        mapOne.forEach((key, value) -> mapTwo.forEach((key2, value2) -> {
            if (value.getName().equals(value2.getName())) {
                intersection.put(key, value);
            }
        }));
        return intersection;
    }
}
