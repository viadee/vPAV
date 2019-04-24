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

import de.viadee.bpm.vPAV.processing.model.data.ProcessVariableOperation;
import soot.toolkits.graph.Block;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

public class Node {

    private FlowGraph flowGraph;
    private List<ProcessVariableOperation> operations;

    private Block block;
    private List<Block> children;
    private String id;

    private BitSet def;
    private BitSet use;
    private BitSet kill;


    public Node(final FlowGraph flowGraph, final Block block) {
        this.flowGraph = flowGraph;
        this.block = block;
        this.children = new ArrayList<>();

        this.operations = new ArrayList<>();

        this.def = new BitSet();
        this.use = new BitSet();
        this.kill = new BitSet();
    }

    public void setId(final String id) {
        this.id = id;
    }

    // Adds an operation to the list of operations (used for line by line checking)
    // Based on operation type adds the operation to the set of corresponding operations
    public void addOperation(final ProcessVariableOperation processVariableOperation) {
        this.operations.add(processVariableOperation);
        flowGraph.incrementDefCounter();

        switch (processVariableOperation.getOperation()) {
            case WRITE:
                def.set(flowGraph.getDefCounter());
                printBits(def);
                break;
            case READ:
                use.set(flowGraph.getDefCounter());
                printBits(use);
                break;
            case DELETE:
                kill.set(flowGraph.getDefCounter());
                printBits(kill);
                break;
        }
    }


    public static void printBits(BitSet b) {
        for (int i = 0; i < b.size(); i++) {
            System.out.print(b.get(i) ? "1" : "0");
        }
        System.out.println();
    }

    public String getId() {
        return id;
    }

    public Block getBlock() {
        return block;
    }

}
