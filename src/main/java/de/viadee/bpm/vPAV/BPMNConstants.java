/**
 * Copyright © 2017, viadee Unternehmensberatung GmbH
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. All advertising materials mentioning features or use of this software
 *    must display the following acknowledgement:
 *    This product includes software developed by the viadee Unternehmensberatung GmbH.
 * 4. Neither the name of the viadee Unternehmensberatung GmbH nor the
 *    names of its contributors may be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY <viadee Unternehmensberatung GmbH> ''AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package de.viadee.bpm.vPAV;

public class BPMNConstants {

    public static final String DEFINITIONS = "definitions";

    public static final String BUSINESSRULETASK = "businessRuleTask";

    public static final String SERVICETASK = "serviceTask";

    public static final String SENDTASK = "sendTask";

    public static final String GATEWAY = "exclusiveGateway";

    public static final String OUT = "outgoing";

    public static final String IN = "incoming";

    public static final String SEQUENCE = "sequenceFlow";

    public static final String INTERMEDIATECATCHEVENT = "intermediateCatchEvent";

    public static final String INTERMEDIATETHROWEVENT = "intermediateThrowEvent";

    public static final String STARTEVENT = "startEvent";

    public static final String BOUNDARYEVENT = "boundaryEvent";

    public static final String ENDEVENT = "endEvent";

    public static final String EXTELEMENTS = "extensionElements";

    public static final String MESSAGE = "message";

    public static final String ERROR = "error";

    public static final String PROCESS = "process";

    public static final String MESSAGEEVENTDEFINITION = "messageEventDefinition";

    public static final String SIGNALEVENTDEFINITION = "signalEventDefinition";

    public static final String SIGNAL = "signal";

    public static final String RECEIVETASK = "receiveTask";

    // ------------------------

    public static final String BPMN_DEFINITIONS = "bpmn:definitions";

    public static final String BPMN_BUSINESSRULETASK = "bpmn:businessRuleTask";

    public static final String BPMN_SERVICETASK = "bpmn:serviceTask";

    public static final String BPMN_SENDTASK = "bpmn:sendTask";

    public static final String BPMN_GATEWAY = "bpmn:exclusiveGateway";

    public static final String BPMN_OUT = "bpmn:outgoing";

    public static final String BPMN_IN = "bpmn:incoming";

    public static final String BPMN_SEQUENCE = "bpmn:sequenceFlow";

    public static final String BPMN_INTERMEDIATECATCHEVENT = "bpmn:intermediateCatchEvent";

    public static final String BPMN_INTERMEDIATETHROWEVENT = "bpmn:intermediateThrowEvent";

    public static final String BPMN_STARTEVENT = "bpmn:startEvent";

    public static final String BPMN_BOUNDARYEVENT = "bpmn:boundaryEvent";

    public static final String BPMN_ENDEVENT = "bpmn:endEvent";

    public static final String BPMN_EXTELEMENTS = "bpmn:extensionElements";

    public static final String BPMN_MESSAGE = "bpmn:message";

    public static final String BPMN_ERROR = "bpmn:error";

    public static final String BPMN_PROCESS = "bpmn:process";

    public static final String BPMN_SIGNAL = "bpmn:signal";

    public static final String BPMN_RECEIVETASK = "bpmn:receiveTask";

    // -----------------------

    public static final String BPMN2_DEFINITIONS = "bpmn2:definitions";

    public static final String BPMN2_BUSINESSRULETASK = "bpmn2:businessRuleTask";

    public static final String BPMN2_SERVICETASK = "bpmn2:serviceTask";

    public static final String BPMN2_SENDTASK = "bpmn2:sendTask";

    public static final String BPMN2_GATEWAY = "bpmn2:exclusiveGateway";

    public static final String BPMN2_OUT = "bpmn2:outgoing";

    public static final String BPMN2_IN = "bpmn2:incoming";

    public static final String BPMN2_SEQUENCE = "bpmn2:sequenceFlow";

    public static final String BPMN2_INTERMEDIATECATCHEVENT = "bpmn2:intermediateCatchEvent";

    public static final String BPMN2_INTERMEDIATETHROWEVENT = "bpmn2:intermediateThrowEvent";

    public static final String BPMN2_STARTEVENT = "bpmn2:startEvent";

    public static final String BPMN2_BOUNDARYEVENT = "bpmn2:boundaryEvent";

    public static final String BPMN2_ENDEVENT = "bpmn2:endEvent";

    public static final String BPMN2_EXTELEMENTS = "bpmn2:extensionElements";

    public static final String BPMN2_MESSAGE = "bpmn2:message";

    public static final String BPMN2_ERROR = "bpmn2:error";

    public static final String BPMN2_PROCESS = "bpmn2:process";

    public static final String BPMN2_SIGNAL = "bpmn2:signal";

    public static final String BPMN2_RECEIVETASK = "bpmn2:receiveTask";

    // -----------------------

    public static final String SCRIPT_TAG = "camunda:script";

    public static final String CAMUNDA_CLASS = "camunda:class";

    public static final String CAMUNDA_EXPRESSION = "camunda:expression";

    public static final String CAMUNDA_DEXPRESSION = "camunda:delegateExpression";

    public static final String CAMUNDA_DMN = "camunda:decisionRef";

    public static final String CAMUNDA_EXT = "camunda:type";

    public static final String CAMUNDA_OUTPAR = "camunda:outputParameter";

    public static final String CAMUNDA_INPAR = "camunda:inputParameter";

    public static final String CAMUNDA_FIELD = "camunda:field";

    public static final String CAMUNDA_PROPERTY = "camunda:property";

    public static final String CAMUNDA_ERRORCODEVAR = "camunda:errorCodeVariable";

    public static final String CAMUNDA_ERRORCODEMESSVAR = "camunda:errorMessageVariable";

    public static final String CAMUNDA_EXECUTIONLISTENER = "camunda:executionListener";

    public static final String CAMUNDA_TASKLISTENER = "camunda:taskListener";

    public static final String CAMUNDA_LIST = "camunda:list";

    public static final String CAMUNDA_VALUE = "camunda:value";

    public static final String CAMUNDA_MAP = "camunda:map";

    public static final String CAMUNDA_ENTRY = "camunda:entry";

    public static final String ATTACHED_TO_REF = "attachedToRef";

    public static final String IMPLEMENTATION = "implementation";

    public static final String TIMEREVENTDEFINTION = "timerEventDefinition";

    public static final String ERROREVENTDEFINITION = "errorEventDefinition";

    public static final String CONDEXP = "conditionExpression";

    public static final String SOURCEREF = "sourceRef";

    public static final String TARGETREF = "targetRef";

    public static final String LANG = "language";

    public static final String EXTERN_LOCATION = "external_Location";

    public static final String INTERN_LOCATION = "de.viadee.bpm.vPAV.processing.checker.";

    public static final String ATTR_CLASS = "class";

    public static final String ATTR_DEL = "delegateExpression";

    public static final String ATTR_EX = "expression";

    public static final String ATTR_ID = "id";

    public static final String ATTR_NAME = "name";

    public static final String ATTR_VALUE = "value";

    public static final String ATTR_ERRORREF = "errorRef";

    public static final String ATTR_ERRORCODE = "errorCode";

    public static final String ATTR_SIGNALREF = "signalRef";

    public static final String ATTR_MESSAGEREF = "messageRef";

    public static final String FIXED_VALUE = "org.camunda.bpm.engine.impl.el.FixedValue";

    public static final String EXPRESSION = "org.camunda.bpm.engine.delegate.Expression";

    private BPMNConstants() {
    }

}
