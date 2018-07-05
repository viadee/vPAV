/**
 * BSD 3-Clause License
 *
 * Copyright © 2018, viadee Unternehmensberatung GmbH
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
package de.viadee.bpm.vPAV.processing.checker;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.camunda.bpm.model.bpmn.instance.BaseElement;
import org.camunda.bpm.model.bpmn.instance.BusinessRuleTask;
import org.camunda.bpm.model.bpmn.instance.EndEvent;
import org.camunda.bpm.model.bpmn.instance.ExclusiveGateway;
import org.camunda.bpm.model.bpmn.instance.IntermediateThrowEvent;
import org.camunda.bpm.model.bpmn.instance.ManualTask;
import org.camunda.bpm.model.bpmn.instance.ScriptTask;
import org.camunda.bpm.model.bpmn.instance.SendTask;
import org.camunda.bpm.model.bpmn.instance.SequenceFlow;
import org.camunda.bpm.model.bpmn.instance.ServiceTask;
import org.camunda.bpm.model.bpmn.instance.StartEvent;
import org.camunda.bpm.model.bpmn.instance.UserTask;

import de.viadee.bpm.vPAV.BpmnScanner;
import de.viadee.bpm.vPAV.config.model.Rule;
import de.viadee.bpm.vPAV.config.model.Setting;
import de.viadee.bpm.vPAV.constants.BpmnConstants;
import de.viadee.bpm.vPAV.output.IssueWriter;
import de.viadee.bpm.vPAV.processing.CheckName;
import de.viadee.bpm.vPAV.processing.model.data.BpmnElement;
import de.viadee.bpm.vPAV.processing.model.data.CheckerIssue;
import de.viadee.bpm.vPAV.processing.model.data.CriticalityEnum;

public class NoExpressionChecker extends AbstractElementChecker {

    public NoExpressionChecker(final Rule rule, final BpmnScanner bpmnScanner) {
        super(rule, bpmnScanner);
    }

    /**
     * Check if ServiceTasks, BusinessRuleTasks, SendTasks and ScriptTasks use expressions against best practices
     *
     * @return issues
     */
    @Override
    public Collection<CheckerIssue> check(BpmnElement element) {

        final Collection<CheckerIssue> issues = new ArrayList<CheckerIssue>();
        final BaseElement baseElement = element.getBaseElement();     
        

        final Map<String, Setting> settings = rule.getSettings();

        if (baseElement instanceof ServiceTask || baseElement instanceof BusinessRuleTask
                || baseElement instanceof SendTask || baseElement instanceof ScriptTask) {

            // read attributes from task
            final String implementationAttr = bpmnScanner.getImplementation(baseElement.getId());

            if (implementationAttr != null && implementationAttr.equals(BpmnConstants.CAMUNDA_EXPRESSION)
                    && !settings.containsKey(baseElement.getElementType().getInstanceType().getSimpleName())) {
                issues.addAll(IssueWriter.createIssue(rule, CriticalityEnum.WARNING, element,
                        String.format("Usage of expressions in '%s' is against best practices.",
                                CheckName.checkName(baseElement))));
            }

            // get the execution listener
            final ArrayList<String> listener = bpmnScanner.getListener(baseElement.getId(), BpmnConstants.ATTR_EX,
                    BpmnConstants.CAMUNDA_EXECUTIONLISTENER);

            if (!listener.isEmpty() && listener.size() > 0
                    && !settings.containsKey(baseElement.getElementType().getInstanceType().getSimpleName())) {
                addIssue(element, issues, baseElement);
            }

        } else if (baseElement instanceof IntermediateThrowEvent
                || baseElement instanceof EndEvent || baseElement instanceof StartEvent) {

            // read attributes from event
            final String implementationAttrEvent = bpmnScanner.getEventImplementation(baseElement.getId());

            if (implementationAttrEvent != null && implementationAttrEvent.contains(BpmnConstants.CAMUNDA_EXPRESSION)
                    && !settings.containsKey(baseElement.getElementType().getInstanceType().getSimpleName())) {
                issues.addAll(IssueWriter.createIssue(rule, CriticalityEnum.WARNING, element,
                        String.format("Usage of expression in event '%s' is against best practices.",
                                CheckName.checkName(baseElement))));
            }

            // get the execution listener
            final ArrayList<String> listener = bpmnScanner.getListener(baseElement.getId(), BpmnConstants.ATTR_EX,
                    BpmnConstants.CAMUNDA_EXECUTIONLISTENER);

            if (!listener.isEmpty() && listener.size() > 0
                    && !settings.containsKey(baseElement.getElementType().getInstanceType().getSimpleName())) {
                addIssue(element, issues, baseElement);
            }

        } else if (baseElement instanceof SequenceFlow) {

            // get the execution listener
            final ArrayList<String> listener = bpmnScanner.getListener(baseElement.getId(), BpmnConstants.ATTR_EX,
                    BpmnConstants.CAMUNDA_EXECUTIONLISTENER);
            if (!listener.isEmpty()
                    && !settings.containsKey(baseElement.getElementType().getInstanceType().getSimpleName())) {
                addIssue(element, issues, baseElement);
            }

        } else if (baseElement instanceof ExclusiveGateway) {
            // get the execution listener
            final ArrayList<String> listener = bpmnScanner.getListener(baseElement.getId(), BpmnConstants.ATTR_EX,
                    BpmnConstants.CAMUNDA_EXECUTIONLISTENER);
            if (!listener.isEmpty()
                    && !settings.containsKey(baseElement.getElementType().getInstanceType().getSimpleName())) {
                addIssue(element, issues, baseElement);
            }
        } else if (baseElement instanceof UserTask) {
            // get the execution listener
            final ArrayList<String> listener = bpmnScanner.getListener(baseElement.getId(), BpmnConstants.ATTR_EX,
                    BpmnConstants.CAMUNDA_EXECUTIONLISTENER);
            if (!listener.isEmpty()
                    && !settings.containsKey(baseElement.getElementType().getInstanceType().getSimpleName())) {
                addIssue(element, issues, baseElement);
            }

            // get the task listener
            final ArrayList<String> taskListener = bpmnScanner.getListener(baseElement.getId(), BpmnConstants.ATTR_EX,
                    BpmnConstants.CAMUNDA_EXECUTIONLISTENER);
            if (!taskListener.isEmpty()
                    && !settings.containsKey(baseElement.getElementType().getInstanceType().getSimpleName())) {
                addIssue(element, issues, baseElement);
            }

        } else if (baseElement instanceof ManualTask) {
            // get the execution listener
            final ArrayList<String> listener = bpmnScanner.getListener(baseElement.getId(), BpmnConstants.ATTR_EX,
                    BpmnConstants.CAMUNDA_EXECUTIONLISTENER);
            if (!listener.isEmpty()
                    && !settings.containsKey(baseElement.getElementType().getInstanceType().getSimpleName())) {
                addIssue(element, issues, baseElement);
            }
        }

        return issues;
    }

    /**
     * Adds an issue to the collection
     *
     * @param element
     *            BpmnElement to be added
     * @param issues
     *            Collection of issues
     * @param baseElement
     *            BaseElement
     */
    private void addIssue(BpmnElement element, final Collection<CheckerIssue> issues, final BaseElement baseElement) {
        issues.addAll(IssueWriter.createIssue(rule, CriticalityEnum.WARNING, element,
                String.format("Usage of expression in listeners for '%s' is against best practices.",
                        CheckName.checkName(baseElement))));
    }

}
