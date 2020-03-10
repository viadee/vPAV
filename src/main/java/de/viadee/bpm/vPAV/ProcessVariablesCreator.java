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
package de.viadee.bpm.vPAV;

import de.viadee.bpm.vPAV.constants.BpmnConstants;
import de.viadee.bpm.vPAV.processing.ObjectReader;
import de.viadee.bpm.vPAV.processing.code.flow.BasicNode;
import de.viadee.bpm.vPAV.processing.code.flow.BpmnElement;
import de.viadee.bpm.vPAV.processing.code.flow.Node;
import de.viadee.bpm.vPAV.processing.model.data.*;
import org.camunda.bpm.model.bpmn.instance.BaseElement;
import org.camunda.bpm.model.bpmn.instance.BpmnModelElementInstance;
import soot.Value;
import soot.toolkits.graph.Block;

import java.util.ArrayList;
import java.util.List;

public class ProcessVariablesCreator {
    private static final int CONTROL_NONE = 0;
    private static final int CONTROL_IF = 1;
    private static final int CONTROL_ELSE = 2;
    private static final int CONTROL_COND_END = 3;

    private ArrayList<Node> nodes = new ArrayList<>();

    private BpmnElement element;

    private ElementChapter chapter;

    private KnownElementFieldType fieldType;

    private String defaultScopeId;

    private ArrayList<Node> stack = new ArrayList<>();

    private int control = 0;
    private boolean hasElse = false;

    // Used for testing
    public ProcessVariablesCreator(final BpmnElement element,
            final ElementChapter chapter, final KnownElementFieldType fieldType, String scopeId) {
        //    variableBlock = new VariableBlock(block, new ArrayList<>());
        this.element = element;
        this.chapter = chapter;
        this.fieldType = fieldType;
        defaultScopeId = scopeId;
    }

    // Does it make sense to store the top-level block or are there better ways?
    public ProcessVariablesCreator(final BpmnElement element,
            final ElementChapter chapter, final KnownElementFieldType fieldType) {
        //    variableBlock = new VariableBlock(block, new ArrayList<>());
        this.element = element;
        this.chapter = chapter;
        this.fieldType = fieldType;
        determineScopeId();
    }

    // Only called for top-level block -> Rename
    // TODO do not return variable block, not needed at the moment
    public void blockIterator(final Block block, final List<Value> args) {
        ObjectReader objectReader = new ObjectReader(this);
        objectReader.processBlock(block, args, null);
    }

    // This method adds process variables one by one
    public void handleProcessVariableManipulation(Block block, ProcessVariableOperation pvo) {
        // Block hasn't changed since the last operation, add operation to existing block
        if (nodes.size() > 0 && lastNode().getBlock().equals(block)) {
            // TODO add test for skipped control flow
            handleSkippedControlFlow();
            lastNode().addOperation(pvo);
        } else {
            // Add new block
            Node node = new Node(element, block, chapter, fieldType);
            node.addOperation(pvo);
            nodes.add(node);
            element.getControlFlowGraph().addNode(node);

            if (control != CONTROL_NONE) {
                handleControlFlow(node);
            }
        }
    }

    private void handleControlFlow(Node node) {
        switch (control) {
            case CONTROL_IF:
                node.addPredecessor(peekStack());
                break;
            case CONTROL_ELSE:
                node.addPredecessor(popStack());
                break;
            case CONTROL_COND_END:
                if(hasElse) {
                    node.addPredecessor(popStack());
                    node.addPredecessor(popStack());
                    hasElse = false;
                }
                else {
                    popStack();
                    node.addPredecessor(popStack());
                }
                break;
            default:
                assert false;
        }
        control = CONTROL_NONE;
    }

    // Reset control flow if no variable manipulations happened
    private void handleSkippedControlFlow() {
        switch (control) {
            case CONTROL_COND_END:
                if(hasElse) {
                    popStack();
                    popStack();
                }
                else {
                    popStack();
                    popStack();
                }
                break;
            default:
                assert false;
        }
        hasElse = false;
        control = CONTROL_NONE;
    }

    public void startIf() {
        // TODO that must not necessarily hold but assume for the moment for simplicity
        assert nodes.size() > 0;
        stack.add(lastNode());
        control = CONTROL_IF;
    }

    public void startElse() {
        stack.add(stack.size()-1, lastNode()); // add if node
        control = CONTROL_ELSE;
        hasElse = true;
    }

    public void endIfElse() {
        stack.add(stack.size()-1, lastNode()); // either else or if node is added
        control = CONTROL_COND_END;
    }

    public void startLoop() {

    }

    public void endLoop() {

    }

    private void determineScopeId() {
        // TODO there might be another "calculation" for multi instance tasks
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

    public Node lastNode() {
        return nodes.get(nodes.size() - 1);
    }

    private Node peekStack() {
        return stack.get(stack.size() - 1);
    }

    private Node popStack() {
        Node n = peekStack();
        stack.remove(stack.size() - 1);
        return n;
    }

    // TODO recursion based on blocks (maybe hashing if already inside?
}
