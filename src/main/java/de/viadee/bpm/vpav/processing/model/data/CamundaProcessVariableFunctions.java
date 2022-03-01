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
package de.viadee.bpm.vpav.processing.model.data;

import de.viadee.bpm.vpav.constants.CamundaMethodServices;

import static de.viadee.bpm.vpav.constants.CamundaMethodServices.*;

/**
 *
 * Enum storing Camunda methods of ProcessVariable operations.
 *
 * name and numberOfArgBoxes are for identification locationOfBox and operationType are for finding and storing the
 * ProcessVariable
 *
 */
public enum CamundaProcessVariableFunctions {

    FCT_SET_VARIABLE(SET_VARIABLE, CamundaMethodServices.DELEGATE, 2, 1, VariableOperation.WRITE),
    FCT_SET_VARIABLE2(SET_VARIABLE, CamundaMethodServices.RUNTIME, 3, 1, VariableOperation.WRITE),
    FCT_SET_VARIABLED(SET_VARIABLE, CamundaMethodServices.DELEGATE_TASK, 2, 1, VariableOperation.WRITE),
    FCT_SET_VARIABLES(SET_VARIABLE, CamundaMethodServices.SCOPE, 2, 1, VariableOperation.WRITE),
    FCT_SET_VARIABLE_LOCAL(SET_VARIABLE_LOCAL, CamundaMethodServices.SCOPE, 2, 1, VariableOperation.WRITE),
    FCT_SET_VARIABLE_LOCAL2(SET_VARIABLE_LOCAL, CamundaMethodServices.DELEGATE_TASK, 2, 1, VariableOperation.WRITE),
    FCT_SET_VARIABLEA(SET_VARIABLE, CamundaMethodServices.ACTIVITY_EXECUTION, 2, 1, VariableOperation.WRITE),
    FCT_GET_VARIABLE(GET_VARIABLE, CamundaMethodServices.DELEGATE, 1, 1, VariableOperation.READ),
    FCT_GET_VARIABLE2(GET_VARIABLE, CamundaMethodServices.DELEGATE, 2, 1, VariableOperation.READ),
    FCT_GET_VARIABLE3(GET_VARIABLE, CamundaMethodServices.DELEGATE_TASK, 1, 1, VariableOperation.READ),
    FCT_GET_VARIABLES(GET_VARIABLE, CamundaMethodServices.SCOPE, 1, 1, VariableOperation.READ),
    FCT_GET_VARIABLE_LOCAL(GET_VARIABLE_LOCAL, CamundaMethodServices.DELEGATE_TASK, 1, 1, VariableOperation.READ),
    FCT_GET_VARIABLE_LOCAL_TYPED(GET_VARIABLE_LOCAL_TYPED, CamundaMethodServices.DELEGATE_TASK, 1, 1, VariableOperation.READ),
    FCT_GET_VARIABLE_LOCAL_TYPED2(GET_VARIABLE_LOCAL_TYPED, CamundaMethodServices.DELEGATE_TASK, 2, 1, VariableOperation.READ),
    FCT_GET_VARIABLEA(GET_VARIABLE, CamundaMethodServices.ACTIVITY_EXECUTION, 1, 1, VariableOperation.READ),
    FCT_GET_VARIABLEA2(GET_VARIABLE, CamundaMethodServices.ACTIVITY_EXECUTION, 2, 1, VariableOperation.READ),
    FCT_REMOVE_VARIABLE(REMOVE_VARIABLE, CamundaMethodServices.DELEGATE, 1, 1, VariableOperation.DELETE),
    FCT_REMOVE_VARIABLE2(REMOVE_VARIABLE, CamundaMethodServices.DELEGATE_TASK, 1, 1, VariableOperation.DELETE),
    FCT_REMOVE_VARIABLE_LOCAL(REMOVE_VARIABLE_LOCAL, CamundaMethodServices.DELEGATE_TASK, 1, 1, VariableOperation.DELETE),
    FCT_REMOVE_VARIABLEA(REMOVE_VARIABLE, CamundaMethodServices.ACTIVITY_EXECUTION, 1, 1, VariableOperation.DELETE),
    FCT_PUT(PUT, CamundaMethodServices.VARIABLE_MAP, 2, 1, VariableOperation.WRITE),
    FCT_PUT2(PUT, CamundaMethodServices.MAP, 2, 1, VariableOperation.WRITE),
    FCT_PUT_VALUE(PUT_VALUE, CamundaMethodServices.VARIABLE_MAP, 2, 1, VariableOperation.WRITE),
    FCT_PUT_VALUE_TYPED("putValueTyped", CamundaMethodServices.VARIABLE_MAP, 2, 1, VariableOperation.WRITE),
    FCT_GET_VALUE("getValue", CamundaMethodServices.VARIABLE_MAP, 2, 1, VariableOperation.READ),
    FCT_GET_VALUE_TYPED("getValueTyped", CamundaMethodServices.VARIABLE_MAP, 1, 1, VariableOperation.READ),
    FCT_REMOVE(REMOVE, CamundaMethodServices.VARIABLE_MAP, 1, 1, VariableOperation.DELETE),
    FCT_REMOVE2(REMOVE, CamundaMethodServices.VARIABLE_MAP, 2, 1, VariableOperation.DELETE);

    private final String name;

    private final String service;

    private final int numberOfArgBoxes;

    private final int locationOfBox;

    private final VariableOperation operationType;

    /**
     *
     * @param name
     *            - of the Camunda method
     * @param number
     *            - of parameter the method takes
     * @param loc
     *            - which parameter is the ProcessVariable name
     * @param type
     *            - VariableOperation
     */
    CamundaProcessVariableFunctions(final String name, final String service, int number, int loc,
            VariableOperation type) {
        this.name = name;
        this.service = service;
        this.numberOfArgBoxes = number;
        this.locationOfBox = loc;
        this.operationType = type;
    }

    public String getName() {
        return name;
    }

    public String getService() {
        return service;
    }

    public int getNumberOfArgBoxes() {
        return numberOfArgBoxes;
    }

    public int getLocation() {
        return locationOfBox;
    }

    public VariableOperation getOperationType() {
        return operationType;
    }

    public static CamundaProcessVariableFunctions findByNameAndNumberOfBoxes(final String name, final String service,
            int numberOfBoxes) {

        for (CamundaProcessVariableFunctions f : values()) {
            if (f.getName().equals(name) && f.getNumberOfArgBoxes() == numberOfBoxes
                    && f.getService().equals(service)) {
                return f;
            }
        }
        return null;
    }

}
