/**
 * BSD 3-Clause License
 *
 * Copyright © 2019, viadee Unternehmensberatung AG
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

public class BpmnConstants {

	public static final String DEFINITIONS = "definitions";

	public static final String BUSINESS_RULE_TASK = "businessRuleTask";

	public static final String SERVICE_TASK = "serviceTask";

	public static final String SEND_TASK = "sendTask";

	public static final String GATEWAY = "exclusiveGateway";

	public static final String OUT = "outgoing";

	public static final String IN = "incoming";

	public static final String SEQUENCE = "sequenceFlow";

	public static final String INTERMEDIATE_CATCH_EVENT = "intermediateCatchEvent";

	public static final String INTERMEDIATE_THROW_EVENT = "intermediateThrowEvent";

	public static final String START_EVENT = "startEvent";

	public static final String BOUNDARY_EVENT = "boundaryEvent";

	public static final String END_EVENT = "endEvent";

	public static final String EXTENSION_ELEMENTS = "extensionElements";

	public static final String MESSAGE = "message";

	public static final String ERROR = "error";

	public static final String PROCESS = "process";

	public static final String SUBPROCESS = "subProcess";

	public static final String MESSAGE_EVENT_DEFINITION = "messageEventDefinition";

	public static final String SIGNAL_EVENT_DEFINITION = "signalEventDefinition";

	public static final String LINK_EVENT_DEFINITION = "linkEventDefinition";

	public static final String SIGNAL = "signal";

	public static final String RECEIVE_TASK = "receiveTask";

	// ------------------------

	public static final String BPMN_DEFINITIONS = "bpmn:definitions";

	public static final String BPMN_BUSINESS_RULE_TASK = "bpmn:businessRuleTask";

	public static final String BPMN_SERVICE_TASK = "bpmn:serviceTask";

	public static final String BPMN_SEND_TASK = "bpmn:sendTask";

	public static final String BPMN_GATEWAY = "bpmn:exclusiveGateway";

	public static final String BPMN_OUT = "bpmn:outgoing";

	public static final String BPMN_SEQUENCE = "bpmn:sequenceFlow";

	public static final String BPMN_INTERMEDIATE_CATCH_EVENT = "bpmn:intermediateCatchEvent";

	public static final String BPMN_INTERMEDIATE_THROW_EVENT = "bpmn:intermediateThrowEvent";

	public static final String BPMN_START_EVENT = "bpmn:startEvent";

	public static final String BPMN_BOUNDARY_EVENT = "bpmn:boundaryEvent";

	public static final String BPMN_END_EVENT = "bpmn:endEvent";

	public static final String BPMN_EXTENSION_ELEMENTS = "bpmn:extensionElements";

	public static final String BPMN_MESSAGE = "bpmn:message";

	public static final String BPMN_ERROR = "bpmn:error";

	public static final String BPMN_SIGNAL = "bpmn:signal";

	public static final String BPMN_RECEIVE_TASK = "bpmn:receiveTask";

	// -----------------------

	public static final String BPMN2_DEFINITIONS = "bpmn2:definitions";

	public static final String BPMN2_BUSINESS_RULE_TASK = "bpmn2:businessRuleTask";

	public static final String BPMN2_SERVICE_TASK = "bpmn2:serviceTask";

	public static final String BPMN2_SEND_TASK = "bpmn2:sendTask";

	public static final String BPMN2_GATEWAY = "bpmn2:exclusiveGateway";

	public static final String BPMN2_OUT = "bpmn2:outgoing";

	public static final String BPMN2_SEQUENCE = "bpmn2:sequenceFlow";

	public static final String BPMN2_INTERMEDIATE_CATCH_EVENT = "bpmn2:intermediateCatchEvent";

	public static final String BPMN2_INTERMEDIATE_THROW_EVENT = "bpmn2:intermediateThrowEvent";

	public static final String BPMN2_START_EVENT = "bpmn2:startEvent";

	public static final String BPMN2_BOUNDARY_EVENT = "bpmn2:boundaryEvent";

	public static final String BPMN2_END_EVENT = "bpmn2:endEvent";

	public static final String BPMN2_EXTENSION_ELEMENTS = "bpmn2:extensionElements";

	public static final String BPMN2_MESSAGE = "bpmn2:message";

	public static final String BPMN2_ERROR = "bpmn2:error";

	public static final String BPMN2_SIGNAL = "bpmn2:signal";

	public static final String BPMN2_RECEIVE_TASK = "bpmn2:receiveTask";

	// -----------------------

	public static final String SCRIPT_TAG = "camunda:script";

	public static final String CAMUNDA_CLASS = "camunda:class";

	public static final String CAMUNDA_EXPRESSION = "camunda:expression";

	public static final String CAMUNDA_DEXPRESSION = "camunda:delegateExpression";

	public static final String CAMUNDA_DMN = "camunda:decisionRef";

	public static final String CAMUNDA_EXT = "camunda:type";

	public static final String CAMUNDA_OUTPUT_PARAMETER = "camunda:outputParameter";

	public static final String CAMUNDA_INPUT_PARAMETER = "camunda:inputParameter";

	public static final String CAMUNDA_FIELD = "camunda:field";

	public static final String CAMUNDA_PROPERTY = "camunda:property";

	public static final String CAMUNDA_ERROR_CODE_VARIABLE = "camunda:errorCodeVariable";

	public static final String CAMUNDA_ERROR_MESSAGE_VARIABLE = "camunda:errorMessageVariable";

	public static final String CAMUNDA_EXECUTION_LISTENER = "camunda:executionListener";

	public static final String CAMUNDA_TASK_LISTENER = "camunda:taskListener";

	public static final String CAMUNDA_LIST = "camunda:list";

	public static final String CAMUNDA_VALUE = "camunda:value";

	public static final String CAMUNDA_MAP = "camunda:map";

	public static final String CAMUNDA_ENTRY = "camunda:entry";

	public static final String CAMUNDA_SCRIPT = "camunda:script";

	public static final String SCRIPT_FORMAT = "scriptFormat";

	public static final String RESULT_VARIABLE = "resultVariable";

	public static final String ATTACHED_TO_REF = "attachedToRef";

	public static final String IMPLEMENTATION = "implementation";

	public static final String TIMER_EVENT_DEFINITION = "timerEventDefinition";

	public static final String ERROR_EVENT_DEFINITION = "errorEventDefinition";

	public static final String CONDITION_EXPRESSION = "conditionExpression";

	public static final String SOURCE_REF = "sourceRef";

	public static final String TARGET_REF = "targetRef";

	public static final String DECISION_REF = "decisionRef";

	public static final String CASE_REF = "caseRef";

	public static final String LANG = "language";

	public static final String EXTERN_LOCATION = "external_Location";

	public static final String INTERN_LOCATION = "de.viadee.bpm.vPAV.processing.checker.";

	public static final String ATTR_KEY = "key";

	public static final String ATTR_CLASS = "class";

	public static final String ATTR_DEL = "delegateExpression";

	public static final String ATTR_EX = "expression";

	public static final String ATTR_ID = "id";

	public static final String ATTR_NAME = "name";

	public static final String ATTR_VALUE = "value";

	public static final String ATTR_ERROR_REF = "errorRef";

	public static final String ATTR_ERROR_CODE = "errorCode";

	public static final String ATTR_SIGNAL_REF = "signalRef";

	public static final String ATTR_MESSAGE_REF = "messageRef";

	public static final String ATTR_VAR_MAPPING_CLASS = "variableMappingClass";

	public static final String DEFAULT = "default";

	public static final String REQUIRED_DEFAULT = "requiredDefault";

	public static final String COLLECTION = "collection";

	public static final String ELEMENT_VARIABLE = "elementVariable";

	public static final String SIMPLE_NAME_PROCESS = "Process";

	public static final String SIMPLE_NAME_SUB_PROCESS = "SubProcess";

	public static final String SIMPLE_NAME_SCRIPT_TASK = "ScriptTask";

	public static final String SIMPLE_NAME_SEQUENCE_FLOW = "SequenceFlow";

	public static final String COND_EXP = "conditionExpression";

	public static final String TASK_LISTENER = "taskListener";

	public static final String EXECUTION_LISTENER = "executionListener";

	public static final String INTERFACE_DEL = "JavaDelegate";

	public static final String INTERFACE_EXECUTION_LISTENER = "ExecutionListener";

	public static final String INTERFACE_TASK_LISTENER = "TaskListener";

	public static final String INTERFACE_SIGNALLABLE_ACTIVITY_BEHAVIOR = "SignallableActivityBehavior";

	public static final String INTERFACE_ACTIVITY_BEHAVIOUR = "ActivityBehavior";

	public static final String INTERFACE_DEL_VARIABLE_MAPPING = "DelegateVariableMapping";

	public static final String SUPERCLASS_ABSTRACT_BPMN_ACTIVITY_BEHAVIOR = "AbstractBpmnActivityBehavior";

	public static final String VPAV_ID = "id";

	public static final String VPAV_BPMN_FILE = "bpmnFile";

	public static final String VPAV_RULE_NAME = "ruleName";

	public static final String VPAV_RULE_DESCRIPTION = "ruleDescription";

	public static final String VPAV_ELEMENT_ID = "elementId";

	public static final String VPAV_ELEMENT_NAME = "elementName";

	public static final String VPAV_CLASSIFICATION = "classification";

	public static final String VPAV_RESOURCE_FILE = "resourceFile";

	public static final String VPAV_VARIABLE = "variable";

	public static final String VPAV_ANOMALY = "anomaly";

	public static final String VPAV_PATHS = "paths";

	public static final String VPAV_MESSAGE = "message";

	public static final String VPAV_ELEMENT_DESCRIPTION = "elementDescription";

	public static final String VPAV_ELEMENTS_TO_MARK = "elementsToMark";

	public static final String VPAV_NO_ISSUES_ELEMENTS = "noIssuesElements";

	public static final String PROCESS_VARIABLE_MODEL_CHECKER = "ProcessVariablesModelChecker";

	private BpmnConstants() {
	}

}
