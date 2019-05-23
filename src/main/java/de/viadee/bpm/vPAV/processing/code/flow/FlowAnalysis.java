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

    private LinkedHashMap<BpmnElement, List<BpmnElement>> nodes;

    public FlowAnalysis() {
        this.nodes = new LinkedHashMap<>();
    }

    public void analyze(final Collection<Graph> graphCollection) {
        for (Graph graph : graphCollection) {
            embedControlFlowGraph(graph);
            computeReachingDefinitions();
            printGraph();
        }
    }

    /**
     * Uses the approach from ALSU07 (Reaching Definitions) to compute data flow
     * anomalies across the CFG
     */
    private void computeReachingDefinitions() {
        boolean change = true;
        while (change) {
            change = false;
            for (BpmnElement element : nodes.keySet()) {
                element.getControlFlowGraph().computePredecessorRelations();
                // Calculate in-sets (intersection of predecessors)
                final LinkedHashMap<String, ProcessVariableOperation> inUsedB = element.getInUsed();
                final LinkedHashMap<String, ProcessVariableOperation> inUnusedB = element.getInUnused();
                if (element.getControlFlowGraph().hasNodes()) {
                    for (Node pred : element.getNodePredecessors()) {
                        inUsedB.putAll(pred.getOutUsed());
                        inUnusedB.putAll(pred.getOutUnused());
                    }
                } else {
                    for (BpmnElement pred : element.getProcessPredecessors()) {
                        inUsedB.putAll(pred.getOutUsed());
                        inUnusedB.putAll(pred.getOutUnused());
                    }
                }
                element.setInUsed(inUsedB);
                element.setInUnused(inUnusedB);

                // Calculate out-sets for used definitions (transfer functions)
                final LinkedHashMap<String, ProcessVariableOperation> outUsedB = new LinkedHashMap<>();
                outUsedB.putAll(element.getUsed2());
                outUsedB.putAll(getSetDifference(element.getInUsed(), element.getKilled2()));
                element.setOutUsed(outUsedB);

                // Calculate out-sets for unused definitions (transfer functions)
                final LinkedHashMap<String, ProcessVariableOperation> outUnusedB = new LinkedHashMap<>(
                        element.getDefined2());
                final LinkedHashMap<String, ProcessVariableOperation> tempIntersectionB = new LinkedHashMap<>();
                tempIntersectionB.putAll(getSetDifference(element.getInUnused(), element.getKilled2()));
                tempIntersectionB.putAll(getSetDifference(tempIntersectionB, element.getUsed2()));
                outUnusedB.putAll(tempIntersectionB);
                element.setOutUnused(outUnusedB);


                for (Node node : element.getControlFlowGraph().getNodes().values()) {
                    // Calculate in-sets (intersection of predecessors)
                    final LinkedHashMap<String, ProcessVariableOperation> inUsed = node.getInUsed();
                    final LinkedHashMap<String, ProcessVariableOperation> inUnused = node.getInUnused();
                    for (Node pred : node.getNodePredecessors()) {
                        inUsed.putAll(pred.getOutUsed());
                        inUnused.putAll(pred.getOutUnused());
                    }
                    node.setInUsed(inUsed);
                    node.setInUnused(inUnused);

                    // Calculate out-sets for used definitions (transfer functions)
                    final LinkedHashMap<String, ProcessVariableOperation> outUsed = new LinkedHashMap<>();
                    outUsed.putAll(node.getUsed());
                    outUsed.putAll(getSetDifference(node.getInUsed(), node.getKilled()));
                    node.setOutUsed(outUsed);

                    // Calculate out-sets for unused definitions (transfer functions)
                    final LinkedHashMap<String, ProcessVariableOperation> outUnused = new LinkedHashMap<>(
                            node.getDefined());
                    final LinkedHashMap<String, ProcessVariableOperation> tempIntersection = new LinkedHashMap<>();
                    tempIntersection.putAll(getSetDifference(node.getInUnused(), node.getKilled()));
                    tempIntersection.putAll(getSetDifference(tempIntersection, node.getUsed()));
                    outUnused.putAll(tempIntersection);
                    node.setOutUnused(outUnused);

                    // Compare old values with new values and check for changes
                    final LinkedHashMap<String, ProcessVariableOperation> oldOutUnused = node
                            .getOutUnused();
                    final LinkedHashMap<String, ProcessVariableOperation> oldOutUsed = node
                            .getOutUsed();

                    if (!oldOutUnused.equals(outUnused) || !oldOutUsed.equals(outUsed)) {
                        change = true;
                    }
                }
                computeLineByLine(element);
                extractAnomalies(element);
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
     * @param element
     *            Current BpmnElement
     */
    private void extractAnomalies(final BpmnElement element) {
        element.getControlFlowGraph().getNodes().values().forEach(node -> {
            // DD (inUnused U defined)
            ddAnomalies(element, node);

            // DU (inUnused U killed)
            duAnomalies(element, node);

            // UR (used - inUnused - inUsed - defined)
            urAnomalies(element, node);

            // UU ()
            uuAnomalies(element, node);

            // -R ()
//			nopRAnomalies(element, node);

            // D- (inUnused U inUsed - (defined - killed - used))
//			dNopAnomalies(element, node);

        });
    }

    /**
     * Extract DD anomalies
     *
     * @param element
     *            Current BpmnElement
     * @param node
     *            Current node
     */
    private void ddAnomalies(final BpmnElement element, final Node node) {
        final LinkedHashMap<String, ProcessVariableOperation> ddAnomalies = new LinkedHashMap<>(
                getIntersection(node.getInUnused(), node.getDefined()));
        if (!ddAnomalies.isEmpty()) {
            ddAnomalies.forEach((k, v) -> element.addSourceCodeAnomaly(
                    new AnomalyContainer(v.getName(), Anomaly.DD, element.getBaseElement().getId(), v)));
        }
    }

    /**
     * Extract DU anomalies
     *
     * @param element
     *            Current BpmnElement
     * @param node
     *            Current node
     */
    private void duAnomalies(final BpmnElement element, final Node node) {
        final LinkedHashMap<String, ProcessVariableOperation> duAnomalies = new LinkedHashMap<>(
                getIntersection(node.getInUnused(), node.getKilled()));
        if (!duAnomalies.isEmpty()) {
            duAnomalies.forEach((k, v) -> element.addSourceCodeAnomaly(
                    new AnomalyContainer(v.getName(), Anomaly.DU, element.getBaseElement().getId(), v)));
        }
    }

    /**
     * Extract UR anomalies
     *
     * @param element
     *            Current BpmnElement
     * @param node
     *            Current node
     */
    private void urAnomalies(final BpmnElement element, final Node node) {
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
            urAnomalies.forEach((k, v) -> element.addSourceCodeAnomaly(
                    new AnomalyContainer(v.getName(), Anomaly.UR, element.getBaseElement().getId(), v)));
        }
    }

    /**
     * Extract UU anomalies
     *
     * @param element
     *            Current BpmnElement
     * @param node
     *            Current node
     */
    private void uuAnomalies(BpmnElement element, Node node) {
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

        uuAnomaliesTemp.forEach((key, value) -> node.getDefined().forEach((key2, value2) -> {
            if (value.getName().equals(value2.getName())) {
                if (Integer.parseInt(key) > Integer.parseInt(key2)) {
                    uuAnomalies.remove(key);
                }
            }
        }));


        if (!uuAnomalies.isEmpty()) {
            uuAnomalies.forEach((k, v) -> element.addSourceCodeAnomaly(
                    new AnomalyContainer(v.getName(), Anomaly.UU, element.getBaseElement().getId(), v)));
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

    private void printGraph() {
        for (Map.Entry<BpmnElement, List<BpmnElement>> entry : nodes.entrySet()) {
            if (entry.getKey().getControlFlowGraph().hasNodes()) {
                for (BpmnElement element : entry.getKey().getProcessPredecessors()) {
                    System.out.println("Curr: " + entry.getKey().getControlFlowGraph().firstNode().getId() + " -> pred: " + element.getBaseElement().getId());
                }

                Iterator<Node> iterator = entry.getKey().getControlFlowGraph().getNodes().values().iterator();
                Node predDelegate = null;
                while (iterator.hasNext()) {
                    Node node = iterator.next();
                    if (predDelegate == null) {
                        predDelegate = node;
                    } else {
                        if (node.firstOperation() != null && predDelegate.lastOperation() != null &&
                                !node.firstOperation().getChapter().equals(predDelegate.lastOperation().getChapter())) {
                            node.setPredsIntraProcedural(predDelegate.getId());
                            predDelegate = node;
                        }
                    }
                    for (Node pred : node.getNodePredecessors()) {
                        System.out.println("Curr: " + node.getId() + " -> pred: " + pred.getId());
                    }
                }
            } else {
                if (entry.getKey().getNodePredecessors().size() > 0) {
                    for (Node pred : entry.getKey().getNodePredecessors()) {
                        System.out.println("Curr: " + entry.getKey().getBaseElement().getId() + " -> pred: " + pred.getId());
                    }
                } else if (entry.getKey().getNodeSuccessors().size() > 0) {
                    for (BpmnElement element : entry.getKey().getProcessPredecessors()) {
                        System.out.println("Curr: " + entry.getKey().getBaseElement().getId() + " -> pred: " + element.getBaseElement().getId());
                    }
                } else {
                    for (BpmnElement element : entry.getValue()) {
                        System.out.println("Curr: " + entry.getKey().getBaseElement().getId() + " -> pred: " + element.getBaseElement().getId());
                    }
                }
            }
        }
    }

    private void embedControlFlowGraph(final Graph graph) {
        graph.getVertexInfo().keySet().forEach(element -> {
            element.setProcessPredecessors(graph.getAdjacencyListPredecessor(element));
            element.setProcessSuccessors(graph.getAdjacencyListSuccessor(element));
            nodes.put(element, graph.getAdjacencyListPredecessor(element));
        });

        nodes.forEach((key, value) -> {
            if (key.getControlFlowGraph().hasNodes()) {
                final Node firstNode = key.getControlFlowGraph().firstNode();
                final Node lastNode = key.getControlFlowGraph().lastNode();
                key.getProcessPredecessors().forEach(element -> element.addNodeSuccessor(firstNode));
                key.getProcessSuccessors().forEach(element -> element.addNodePredecessor(lastNode));
            }
        });
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
