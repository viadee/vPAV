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
package de.viadee.bpm.vPAV.processing;

import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.runtime.ProcessInstantiationBuilder;
import org.camunda.bpm.engine.variable.Variables;
import org.camunda.bpm.spring.boot.starter.event.PostDeployEvent;

import java.util.HashMap;
import java.util.Map;

public class EntryPointRuntimeService {

    private RuntimeService runtimeService;

    private ProcessEngine processEngine;

    public void startProcessWithVariables(PostDeployEvent event) {
        HashMap<String, Object> variables = new HashMap<>();
        variables.put("variable", "firstValue");
        variables.put("anotherVariable", "anotherValue");
        variables.remove("variable");
        runtimeService.startProcessInstanceByKey("Process_1", variables);
    }

    public void startProcess(PostDeployEvent event) {
        runtimeService.startProcessInstanceById("myId");
        runtimeService.startProcessInstanceByKey("myKey");
        runtimeService.startProcessInstanceByMessage("myMessage");
        runtimeService.startProcessInstanceByMessageAndProcessDefinitionId("myMessage2", "myId2");
    }

    public void startWithProcessInstantiationBuilder() {
        ProcessInstantiationBuilder instantiationBuilder = processEngine.getRuntimeService()
                .createProcessInstanceByKey("processKey")
                .businessKey("key")
                .setVariable("var", "value");

        instantiationBuilder.execute();
    }

    public void withVariableMap(PostDeployEvent event) {
        Map<String, Object> processVariables = Variables.createVariables()
                .putValue("variable_camunda", "myValue");

        ProcessInstance instance =
                this.runtimeService
                        .startProcessInstanceByKey("Process_2", processVariables);
    }
}
