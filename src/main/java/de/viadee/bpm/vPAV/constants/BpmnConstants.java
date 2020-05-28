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

public class BpmnConstants {

	public static final String OUT = "outgoing";

	public static final String IN = "incoming";

	public static final String START_EVENT = "startEvent";

	public static final String END_EVENT = "endEvent";

	public static final String ERROR = "error";

	public static final String PROCESS = "process";

	// ------------------------

	public static final String CAMUNDA_CLASS = "camunda:class";

	public static final String CAMUNDA_EXPRESSION = "camunda:expression";

	public static final String CAMUNDA_DEXPRESSION = "camunda:delegateExpression";

	public static final String CAMUNDA_DMN = "camunda:decisionRef";

	public static final String CAMUNDA_EXT = "camunda:type";

	public static final String CAMUNDA_EXECUTION_LISTENER = "camunda:executionListener";

	public static final String CAMUNDA_TASK_LISTENER = "camunda:taskListener";

	public static final String RESULT_VARIABLE = "resultVariable";

	public static final String IMPLEMENTATION = "implementation";

	public static final String DECISION_REF = "decisionRef";

	public static final String CASE_REF = "caseRef";

	public static final String EXTERN_LOCATION = "external_Location";

	public static final String INTERN_LOCATION = "de.viadee.bpm.vPAV.processing.checker.";

	public static final String ATTR_CLASS = "class";

	public static final String ATTR_DEL = "delegateExpression";

	public static final String ATTR_EX = "expression";

	public static final String ATTR_ID = "id";

	public static final String ATTR_NAME = "name";

	public static final String ATTR_VAR_MAPPING_CLASS = "variableMappingClass";

	public static final String ATTR_VAR_MAPPING_DELEGATE = "variableMappingDelegateExpression";

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
