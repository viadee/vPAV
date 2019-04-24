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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Node {

    private Set<ProcessVariableOperation> defined;
    private Set<ProcessVariableOperation> used;
    private Set<ProcessVariableOperation> killed;
    private Set<ProcessVariableOperation> inUnused;
    private Set<ProcessVariableOperation> inUsed;
    private Set<ProcessVariableOperation> outUnused;
    private Set<ProcessVariableOperation> outUsed;
    private List<ProcessVariableOperation> operations;
    private Block block;
    private int id;

    public Node(final Block block) {
        this.block = block;
        this.defined = new HashSet<>();
        this.used = new HashSet<>();
        this.killed = new HashSet<>();
        this.inUnused = new HashSet<>();
        this.inUsed = new HashSet<>();
        this.outUnused = new HashSet<>();
        this.outUsed = new HashSet<>();
        this.operations = new ArrayList<>();
    }

    public void setId(final int id) {
        this.id = id;
    }

    // Adds an operation to the list of operations (used for line by line checking)
    // Based on operation type adds the operation to the set of corresponding operations
    public void addOperation(final ProcessVariableOperation processVariableOperation) {
        this.operations.add(processVariableOperation);

        switch (processVariableOperation.getOperation()) {
            case WRITE:
                addDefined(processVariableOperation);
                break;
            case READ:
                addUsed(processVariableOperation);
                break;
            case DELETE:
                addKilled(processVariableOperation);
                break;
        }
    }

    private void addDefined(final ProcessVariableOperation processVariableOperation) {
        this.defined.add(processVariableOperation);
    }

    private void addUsed(final ProcessVariableOperation processVariableOperation) {
        this.used.add(processVariableOperation);
    }

    private void addKilled(final ProcessVariableOperation processVariableOperation) {
        this.killed.add(processVariableOperation);
    }

    public Set<ProcessVariableOperation> getDefined() {
        return defined;
    }

    public Set<ProcessVariableOperation> getUsed() {
        return used;
    }

    public Set<ProcessVariableOperation> getKilled() {
        return killed;
    }

}
