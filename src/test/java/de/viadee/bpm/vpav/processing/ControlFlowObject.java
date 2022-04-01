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

import org.camunda.bpm.engine.delegate.DelegateExecution;

public class ControlFlowObject {

    private void methodWithIfElse(DelegateExecution execution) {
        int notRandom = 3;
        execution.getVariable("variableBefore");
        if (notRandom < 3) {
            String localIf = "notAvailableOutsideIf";
            execution.setVariable(localIf, "test");
        } else {
            String localElse = "notAvailableOutsideElse";
            execution.removeVariable(localElse);
        }
        String afterVar = "afterIfElse";
        execution.getVariable(afterVar);
    }

    private void methodWithLoop(DelegateExecution execution) {
        for (int i = 0; i < 5; i++) {
            execution.getVariable("test");
        }
        execution.removeVariable("test");
    }

    private void methodWithNestedControls(DelegateExecution execution) {
        for (int i = 0; i < 5; i++) {
            if ("value".equals("notvalue")) {
                execution.setVariable("test", true);
            } else {
                execution.removeVariable("test");
            }
            execution.getVariable("test");
        }
        execution.removeVariable("test");
    }

    private void methodWithRecursion(DelegateExecution execution) {
        execution.getVariable("variableBefore");
        int counter = 3;
        if (counter < 2) {
            methodWithRecursion(execution);
        }
        execution.getVariable("variableAfter");
    }

}
