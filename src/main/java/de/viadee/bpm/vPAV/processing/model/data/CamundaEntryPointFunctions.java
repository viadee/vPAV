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

import soot.Scene;
import soot.SootClass;

import static de.viadee.bpm.vPAV.constants.CamundaMethodServices.*;

/**
 * Enum storing Camunda methods of ProcessVariable operations.
 * <p>
 * name and numberOfArgBoxes are for identification locationOfBox and operationType are for finding and storing the
 * ProcessVariable
 */
public enum CamundaEntryPointFunctions {

    StartProcessInstanceById(START_PROCESS_INSTANCE_BY_ID, 1, 4, false),
    StartProcessInstanceByKey(START_PROCESS_INSTANCE_BY_KEY, 1, 4, false),
    StartProcessInstanceByMessage(START_PROCESS_INSTANCE_BY_MESSAGE, 1, 3, true),
    StartProcessInstanceByMessageAndProcessDefinitionId(START_PROCESS_INSTANCE_BY_MESSAGE_AND_PROCESS_DEF, 2, 4, true),
    CorrelateMessage(CORRELATE_MESSAGE, 1, 4, true);
    //   CreateMessageCorrelation("createMessageCorrelation", 1, 1, true);

    private String name;

    private int minArgs;

    private int maxArgs;

    private boolean withMessage;

    /**
     * @param name     - of the Camunda method
     * @param minArgs- parameter number that must at least exist
     * @param maxArgs  - maximum number of parameters which are allowed
     */
    CamundaEntryPointFunctions(final String name, int minArgs, int maxArgs, boolean withMessage) {
        this.name = name;
        this.minArgs = minArgs;
        this.maxArgs = maxArgs;
        this.withMessage = withMessage;
    }

    public String getName() {
        return name;
    }

    public static CamundaEntryPointFunctions findEntryPoint(String name, SootClass sc, int argCount) {
        SootClass rs = Scene.v().forceResolve("org.camunda.bpm.engine.RuntimeService", 0);

        if (!(sc.getInterfaces().contains(rs) || sc.equals(rs))) {
            return null;
        }

        for (CamundaEntryPointFunctions f : values()) {
            if (f.getName().equals(name) && f.getMinArgs() <= argCount && f.getMaxArgs() >= argCount) {
                return f;
            }
        }
        return null;
    }

    public int getMinArgs() {
        return minArgs;
    }

    public int getMaxArgs() {
        return maxArgs;
    }

    public boolean isWithMessage() {
        return withMessage;
    }
}
