package de.viadee.bpm.vPAV.processing.code.flow;

import de.viadee.bpm.vPAV.processing.model.data.BpmnElement;
import de.viadee.bpm.vPAV.processing.model.data.ProcessVariableOperation;
import de.viadee.bpm.vPAV.processing.model.graph.Graph;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

public class FlowAnalysis {

    private LinkedHashMap<BpmnElement, List<BpmnElement>> nodes;
    private LinkedHashMap<String, ProcessVariableOperation> operations;

    public FlowAnalysis() {
        this.nodes = new LinkedHashMap<>();
    }

    public void analyze(final Collection<Graph> graphCollection) {
        for (Graph graph : graphCollection) {
            embedControlFlowGraph(graph);
            printGraph();
            System.out.println();
        }
    }

    private void printGraph() {
        nodes.forEach((key, value) -> {
            if (key.getControlFlowGraph().hasNodes()) {
                key.getControlFlowGraph().computeReachingDefinitions();
                key.getProcessPredecessors().forEach(pred -> {
                    System.out.println("Curr: " + key.getControlFlowGraph().firstNode().getId() + " -> pred: " + pred.getBaseElement().getId());
                    key.getControlFlowGraph().getNodes().values().forEach(node -> {
                        node.getNodePredecessors().forEach(nodePred -> {
                            System.out.println("Curr: " + node.getId() + " -> pred: " + nodePred.getId());
                        });
                    });
                });
                System.out.println("Curr: " + key + " -> pred: " + key.getControlFlowGraph().lastNode().getId());
            } else {
                key.getProcessPredecessors().forEach(pred -> {
                    System.out.println("Curr: " + key.getBaseElement().getId() + " -> pred: " + pred.getBaseElement().getId());
                });
            }
        });
    }

    private void embedControlFlowGraph(final Graph graph) {
        graph.getVertexInfo().keySet().forEach(element -> {
            element.setProcessPredecessors(graph.getAdjacencyListPredecessor(element));
            element.setProcessSuccessors(graph.getAdjacencyListSuccessor(element));
            nodes.put(element, graph.getAdjacencyListPredecessor(element));
        });

        nodes.forEach((key, value) -> value.forEach(entry -> {
            if (entry.getControlFlowGraph().hasNodes()) {
                Iterator<Node> iterator = entry.getControlFlowGraph().getNodes().values().iterator();
                Node block = null;
                while (iterator.hasNext()) {
                    block = iterator.next();
                }
                final Node finalBlock = block;
                entry.getProcessSuccessors().forEach(successor -> {
                    if (finalBlock != null) {
                        successor.addNodePredecessor(finalBlock);
                        finalBlock.addProcessSuccessors(entry.getProcessSuccessors());
                    }
                });
            }
        }));
    }
}
