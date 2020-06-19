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
package de.viadee.bpm.vPAV;

import de.viadee.bpm.vPAV.constants.BpmnConstants;
import de.viadee.bpm.vPAV.processing.ObjectReader;
import de.viadee.bpm.vPAV.processing.code.flow.BasicNode;
import de.viadee.bpm.vPAV.processing.code.flow.BpmnElement;
import de.viadee.bpm.vPAV.processing.code.flow.Node;
import de.viadee.bpm.vPAV.processing.model.data.*;
import org.camunda.bpm.model.bpmn.instance.BaseElement;
import org.camunda.bpm.model.bpmn.instance.BpmnModelElementInstance;
import org.camunda.bpm.model.bpmn.instance.CallActivity;
import soot.Value;
import soot.toolkits.graph.Block;

import java.util.ArrayList;
import java.util.List;

public class ProcessVariablesCreator {

    private ArrayList<Node> nodes = new ArrayList<>();

    private BpmnElement element;

    private ElementChapter chapter;

    private KnownElementFieldType fieldType;

    private String defaultScopeId;

    private BasicNode predecessor;

    // Used for testing
    ProcessVariablesCreator(final BpmnElement element,
            final ElementChapter chapter, final KnownElementFieldType fieldType, String scopeId) {
        this.element = element;
        this.chapter = chapter;
        this.fieldType = fieldType;
        defaultScopeId = scopeId;
    }

    public ProcessVariablesCreator(final BpmnElement element,
            final ElementChapter chapter, final KnownElementFieldType fieldType, final BasicNode[] predecessor) {
        this.element = element;
        this.chapter = chapter;
        this.fieldType = fieldType;
        determineScopeId();
        if (predecessor[0] != null) {
            this.predecessor = predecessor[0];
        }
    }

    /**
     * Start the processing of a block and all its successors. Extracts the process variables manipulations.
     *
     * @param block To processed block
     * @param args  Arguments passed to block
     * @return last created BasicNode or null if no were created
     */
    public BasicNode startBlockProcessing(final Block block, final List<Value> args, final String javaClass) {
        ObjectReader objectReader = new ObjectReader(this, javaClass);
        objectReader.processBlock(block, args, null, null);
        cleanEmptyNodes();

        // find last node
        for (int i = nodes.size() - 1; i >= 0; i--) {
            if (element.getControlFlowGraph().getNodes().get(nodes.get(i).getId()) != null) {
                return nodes.get(i);
            }
        }
        return null;
    }

    /**
     * @param block Block in which the process variable manipulation was found
     * @param pvo   ProcessVariableOperation that was found
     */
    public void handleProcessVariableManipulation(Block block, ProcessVariableOperation pvo, String javaClass) {
        pvo.setIndex(element.getFlowAnalysis().getOperationCounter());
        element.getFlowAnalysis().incrementOperationCounter();

        // Block hasn't changed since the last operation, add operation to existing block
        if (nodes.size() > 0 && lastNode().getBlock().equals(block)) {
            lastNode().addOperation(pvo);
        } else {
            // Add new block
            Node node = new Node(element, block, javaClass, chapter, fieldType);
            node.addOperation(pvo);
            nodes.add(node);
            element.getControlFlowGraph().addNode(node);
            updatePredecessor(node);
        }
    }

    /**
     * Adds a new node to the ControlFlowGraph if no nodes exist yet or the last node isn´t associated with the block.
     *
     * @param block Block for that a node is created
     * @return Newly created block or last block if it is associated with the passed block
     */
    public BasicNode addNodeIfNotExisting(Block block, String javaClass) {
        if (nodes.size() > 0 && lastNode().getBlock().equals(block)) {
            return lastNode();
        } else {
            // Add new block
            Node node = new Node(element, block, javaClass, chapter, fieldType);
            nodes.add(node);
            element.getControlFlowGraph().addNode(node);
            updatePredecessor(node);
            return node;
        }
    }

    private void determineScopeId() {
        final BaseElement baseElement = element.getBaseElement();
        BpmnModelElementInstance scopeElement = baseElement.getScope();

        String scopeId = null;
        if (scopeElement != null) {
            scopeId = scopeElement.getAttributeValue(BpmnConstants.ATTR_ID);
        }
        this.defaultScopeId = scopeId;
    }

    public String getScopeId() {
        return defaultScopeId;
    }

    public String getScopeIdOfChild() {
        if (element.getBaseElement() instanceof CallActivity) {
            return ((CallActivity) element.getBaseElement()).getCalledElement();
        } else {
            return defaultScopeId;
        }
    }

    private Node lastNode() {
        return nodes.get(nodes.size() - 1);
    }

    /**
     * Removes nodes from ControlFlowGraph which don´t contain any process variable operations
     */
    public void cleanEmptyNodes() {
        // Clean up nodes without operations to make analysis faster
        for (Node node : nodes) {
            if (node.getOperations().size() == 0) {
                element.getControlFlowGraph().removeNode(node);
                // For all predecessors, remove node from successors and add successors of node
                node.getPredecessors().forEach(pred -> {
                    pred.removeSuccessor(node.getId());
                    node.getSuccessors().forEach(pred::addSuccessor);
                });
                // For all successors, remove node from predecessors and add predecessors of node
                node.getSuccessors().forEach(succ -> {
                    succ.removePredecessor(node.getId());
                    node.getPredecessors().forEach(succ::addPredecessor);
                });
            }
        }
    }

    public void visitBlockAgain(Block block) {
        // find first node that is associated with block and set successor
        for (Node node : nodes) {
            if (ObjectReader.hashBlock(node.getBlock()) == ObjectReader.hashBlock(block)) {
                node.addPredecessor(predecessor);
                break;
            }
        }
    }

    private void updatePredecessor(BasicNode node) {
        if (predecessor != null) {
            node.addPredecessor(predecessor);
        }
        predecessor = node;
    }

    public Node getNodeOfBlock(Block block, String javaClass) {
        if (nodes.size() > 0 && lastNode().getBlock().equals(block)) {
            return lastNode();
        } else {
            Node node = new Node(element, block, javaClass, chapter, fieldType);
            nodes.add(node);
            element.getControlFlowGraph().addNode(node);
            if (predecessor != null) {
                node.addPredecessor(predecessor);
            }
            return node;
        }
    }

    public void pushNodeToStack(BasicNode blockNode) {
        predecessor = blockNode;
    }

    public ArrayList<Node> getNodes() {
        return nodes;
    }
}
