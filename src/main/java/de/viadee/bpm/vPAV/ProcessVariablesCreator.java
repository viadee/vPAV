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

import de.viadee.bpm.vPAV.processing.ObjectReader;
import de.viadee.bpm.vPAV.processing.code.flow.BpmnElement;
import de.viadee.bpm.vPAV.processing.code.flow.Node;
import de.viadee.bpm.vPAV.processing.model.data.*;
import soot.Value;
import soot.toolkits.graph.Block;

import java.util.ArrayList;
import java.util.List;

public class ProcessVariablesCreator {

    // TODO  I'm not sure why we need nodes and variable blocks (maybe change this later)
    private VariableBlock variableBlock;

    private ArrayList<Node> nodes = new ArrayList<>();
    private BpmnElement element;
    private ElementChapter chapter;
    private KnownElementFieldType fieldType;

    // Does it make sense to store the top-level block or are there better ways?
    public ProcessVariablesCreator(final BpmnElement element,
            final ElementChapter chapter, final KnownElementFieldType fieldType) {
    //    variableBlock = new VariableBlock(block, new ArrayList<>());
        this.element = element;
        this.chapter = chapter;
        this.fieldType = fieldType;
    }

    // Only called for top-level block -> Rename
    // TODO do not return variable block, not needed at the moment
    public ArrayList<Node> blockIterator(final Block block, final List<Value> args) {
        ObjectReader objectReader = new ObjectReader(this);
        objectReader.processBlock(block, args);
        return nodes;
    }

    // This method adds process variables one by one
    public void handleProcessVariableManipulation(Block block, ProcessVariableOperation pvo) {
        // Block hasn't changed since the last operation, add operation to existing block
        if (nodes.size() > 0 && nodes.get(nodes.size() - 1).getBlock().equals(block)) {
            nodes.get(nodes.size() - 1).addOperation(pvo);
        } else {
            // Add new block
            Node node = new Node(element, block, chapter, fieldType);
            node.addOperation(pvo);
            if (nodes.size() > 0) {
                node.addPredecessor(nodes.get(nodes.size() - 1));
            }
            nodes.add(node);
        }
    }

    public ArrayList<Node> getNodes() {
        return nodes;
    }

    public void startIf() {

    }

    public void startElse() {

    }

    public void endIfElse() {

    }

    public void startLoop() {

    }

    public void endLoop() {

    }

    // TODO recursion based on blocks (maybe hashing if already inside?
}
