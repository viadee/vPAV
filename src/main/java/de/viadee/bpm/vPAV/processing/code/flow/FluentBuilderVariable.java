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
package de.viadee.bpm.vPAV.processing.code.flow;

import de.viadee.bpm.vPAV.processing.model.data.CamundaEntryPointFunctions;

/**
 * Represents a fluent builder variable. Currently, it is used to represent a ProcessInstantiationBuilder for finding entry points.
 */
public class FluentBuilderVariable extends ObjectVariable {

    private CamundaEntryPointFunctions createMethod;

    private String processDefinitionKey;

    private boolean wasExecuted;

    private MapVariable variables;

    public FluentBuilderVariable(CamundaEntryPointFunctions createMethod) {
        super();
        this.createMethod = createMethod;
        wasExecuted = false;
        processDefinitionKey = null;
        variables = new MapVariable();
    }

    public String getProcessDefinitionKey() {
        return processDefinitionKey;
    }

    public void setProcessDefinitionKey(String processDefinitionKey) {
        this.processDefinitionKey = processDefinitionKey;
    }

    public boolean isWasExecuted() {
        return wasExecuted;
    }

    public void setWasExecuted(boolean wasExecuted) {
        this.wasExecuted = wasExecuted;
    }

    public MapVariable getVariables() {
        return variables;
    }

    public void setVariables(MapVariable variables) {
        this.variables = variables;
    }

    public CamundaEntryPointFunctions getCreateMethod() {
        return createMethod;
    }

    public void setCreateMethod(CamundaEntryPointFunctions createMethod) {
        this.createMethod = createMethod;
    }

    public void addVariable(String key) {
        this.variables.put(key, null);
    }

    public void addAllVariables(MapVariable map) {
        this.variables.putAll(map.getValues());
    }
}
