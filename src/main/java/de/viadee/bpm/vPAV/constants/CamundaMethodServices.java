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
package de.viadee.bpm.vPAV.constants;

import soot.RefType;

public class CamundaMethodServices {

    private CamundaMethodServices() {
    }

    public static final String DELEGATE = "org.camunda.bpm.engine.delegate.DelegateExecution";

    public static final String ACTIVITY_EXECUTION = "org.camunda.bpm.engine.impl.pvm.delegate.ActivityExecution";

    public static final String DELEGATE_TASK = "org.camunda.bpm.engine.delegate.DelegateTask";

    public static final String VARIABLE_MAP = "org.camunda.bpm.engine.variable.VariableMap";

    public static final String RUNTIME = "org.camunda.bpm.engine.RuntimeService";

    public static final String SCOPE = "org.camunda.bpm.engine.delegate.VariableScope";

    public static final String MAP = "java.util.Map";

    public static final String START_PROCESS_INSTANCE_BY_MESSAGE = "startProcessInstanceByMessage";

    public static final String START_PROCESS_INSTANCE_BY_ID = "startProcessInstanceById";

    public static final String START_PROCESS_INSTANCE_BY_MESSAGE_AND_PROCESS_DEF = "startProcessInstanceByMessageAndProcessDefinitionId";

    public static final String START_PROCESS_INSTANCE_BY_KEY = "startProcessInstanceByKey";

    public static final String CORRELATE_MESSAGE = "correlateMessage";

    public static final String CREATE_PROCESS_INSTANCE_BY_ID = "createProcessInstanceById";

    public static final String CREATE_PROCESS_INSTANCE_BY_KEY = "createProcessInstanceByKey";

    public static final String EXECUTE = "execute";

    public static final String EXECUTE_WITH_VARIABLES_IN_RETURN = "executeWithVariablesInReturn";

    public static final String EXECUTION_OBJECT = "execution";

    public static final String TASK_OBJECT = "task";

    public static final RefType DELEGATE_EXECUTION_TYPE = RefType.v(DELEGATE);

    public static final RefType ACTIVITY_EXECUTION_TYPE = RefType.v(ACTIVITY_EXECUTION);

    public static final RefType DELEGATE_TASK_TYPE = RefType.v(DELEGATE_TASK);

    public static final RefType MAP_VARIABLES_TYPE = RefType.v(VARIABLE_MAP);

    public static final RefType VARIABLE_SCOPE_TYPE = RefType.v(SCOPE);

}
