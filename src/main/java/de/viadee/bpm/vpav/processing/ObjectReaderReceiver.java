/*
 * BSD 3-Clause License
 *
 * Copyright © 2022, viadee Unternehmensberatung AG
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
package de.viadee.bpm.vpav.processing;

import de.viadee.bpm.vpav.processing.code.flow.BasicNode;
import de.viadee.bpm.vpav.processing.code.flow.FluentBuilderVariable;
import de.viadee.bpm.vpav.processing.code.flow.Node;
import de.viadee.bpm.vpav.processing.model.data.CamundaEntryPointFunctions;
import de.viadee.bpm.vpav.processing.model.data.ProcessVariableOperation;
import soot.SootClass;
import soot.jimple.InvokeExpr;
import soot.toolkits.graph.Block;

import java.util.List;

public abstract class ObjectReaderReceiver {

    public void handleProcessVariableManipulation(Block block, ProcessVariableOperation pvo, SootClass javaClass) {
    }

    public BasicNode addNodeIfNotExisting(Block block, SootClass javaClass) {
        return null;
    }

    public void visitBlockAgain(Block block) {
    }

    public Node getNodeOfBlock(Block block, SootClass javaClass) {
        return null;
    }

    public String getScopeId() {
        return "";
    }

    public String getScopeIdOfChild() {
        return "";
    }

    public void pushNodeToStack(BasicNode blockNode) {
    }

    public void addEntryPoint(CamundaEntryPointFunctions func, String className, String methodName, InvokeExpr expr,
            List<Object> args) {
    }

    public void addEntryPoint(FluentBuilderVariable fb, String className, String methodName) {
    }

}
