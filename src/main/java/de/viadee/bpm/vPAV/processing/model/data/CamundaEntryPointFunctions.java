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
package de.viadee.bpm.vPAV.processing.model.data;

import de.viadee.bpm.vPAV.constants.CamundaMethodServices;
import soot.Scene;
import soot.SootClass;

/**
 * Enum storing Camunda methods of ProcessVariable operations.
 * <p>
 * name and numberOfArgBoxes are for identification locationOfBox and operationType are for finding and storing the
 * ProcessVariable
 */
public enum CamundaEntryPointFunctions {

    StartProcessInstanceById("startProcessInstanceById", 1, 4),
    StartProcessInstanceByKey("startProcessInstanceByKey", 1, 4),
    StartProcessInstanceByMessage("startProcessInstanceByMessage", 1, 3),
    StartProcessInstanceByMessageAndProcessDefinitionId("startProcessInstanceByMessageAndProcessDefinitionId", 2, 4),
    CreateMessageCorrelation("createMessageCorrelation", 1, 1);

    private String name;

    private int minArgs;

    private int maxArgs;

    /**
     * @param name     - of the Camunda method
     * @param minArgs- parameter number that must at least exist
     * @param maxArgs  - maximum number of parameters which are allowed
     */
    CamundaEntryPointFunctions(final String name, int minArgs, int maxArgs) {
        this.name = name;
        this.minArgs = minArgs;
        this.maxArgs = maxArgs;
    }

    public String getName() {
        return name;
    }

    public static boolean isEntryPoint(String name, SootClass sc, int argCount) {
        SootClass rs = Scene.v().forceResolve("org.camunda.bpm.engine.RuntimeService", 0);

        if (!(sc.getInterfaces().contains(rs) || sc.equals(rs))) {
            return false;
        }

        for (CamundaEntryPointFunctions f : values()) {
            if (f.getName().equals(name) && f.getMinArgs() <= argCount && f.getMaxArgs() >= argCount) {
                return true;
            }
        }
        return false;
    }

    public int getMinArgs() {
        return minArgs;
    }

    public void setMinArgs(int minArgs) {
        this.minArgs = minArgs;
    }

    public int getMaxArgs() {
        return maxArgs;
    }

    public void setMaxArgs(int maxArgs) {
        this.maxArgs = maxArgs;
    }
}
