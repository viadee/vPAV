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

import de.viadee.bpm.vPAV.processing.model.data.BpmnElement;
import de.viadee.bpm.vPAV.processing.model.graph.Graph;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FlowAnalysis {

    private LinkedHashMap<BpmnElement, List<BpmnElement>> nodes;

    public FlowAnalysis() {
        this.nodes = new LinkedHashMap<>();
    }

    public void analyze(final Collection<Graph> graphCollection) {
        for (Graph graph : graphCollection) {
            embedControlFlowGraph(graph);
//            printGraph();
//            System.out.println();
        }
    }

    private void printGraph() {
        for (Map.Entry<BpmnElement, List<BpmnElement>> entry : nodes.entrySet()) {
            if (entry.getKey().getControlFlowGraph().hasNodes()) {
                entry.getKey().getControlFlowGraph().computePredecessorRelations();
                for (BpmnElement element : entry.getKey().getProcessPredecessors()) {
                    System.out.println("Curr: " + entry.getKey().getControlFlowGraph().firstNode().getId() + " -> pred: " + element.getBaseElement().getId());
                }
                for (Node node : entry.getKey().getControlFlowGraph().getNodes().values()) {
                    for (Node pred : node.getNodePredecessors()) {
                        System.out.println("Curr: " + node.getId() + " -> pred: " + pred.getId());
                    }
                }

            } else {
                if (entry.getKey().getNodePredecessors().size() > 0) {
                    for (Node pred : entry.getKey().getNodePredecessors()) {
                        System.out.println("Curr: " + entry.getKey().getBaseElement().getId() + " -> pred: " + pred.getId());
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
}
