/**
 * BSD 3-Clause License
 *
 * Copyright © 2018, viadee Unternehmensberatung GmbH
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
package de.viadee.bpm.vPAV.processing.model.data;

import de.viadee.bpm.vPAV.processing.model.data.ProcessVariableOperation;

import java.util.ArrayList;
import java.util.List;

public class ProcessVariable {

    private final String name;
    private final List<ProcessVariableOperation> operations;
    private final List<ProcessVariableOperation> writes;
    private final List<ProcessVariableOperation> reads;
    private final List<ProcessVariableOperation> deletes;

    public ProcessVariable(String name) {
        this.name = name;
        this.operations = new ArrayList<>();
        this.writes = new ArrayList<>();
        this.reads = new ArrayList<>();
        this.deletes = new ArrayList<>();
    }

    public void addWrite(ProcessVariableOperation operation) {
        operations.add(operation);
        writes.add(operation);
    }

    public void addRead(ProcessVariableOperation operation) {
        operations.add(operation);
        reads.add(operation);
    }

    public void addDelete(ProcessVariableOperation operation) {
        operations.add(operation);
        deletes.add(operation);
    }

    public String getName() {
        return name;
    }

    public List<ProcessVariableOperation> getWrites() {
        return writes;
    }

    public List<ProcessVariableOperation> getReads() {
        return reads;
    }

    public List<ProcessVariableOperation> getDeletes() {
        return deletes;
    }

    public List<ProcessVariableOperation> getOperations() {
        return operations;
    }
}
