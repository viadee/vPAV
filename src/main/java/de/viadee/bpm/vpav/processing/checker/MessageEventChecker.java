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
package de.viadee.bpm.vpav.processing.checker;

import de.viadee.bpm.vpav.BpmnScanner;
import de.viadee.bpm.vpav.Messages;
import de.viadee.bpm.vpav.config.model.Rule;
import de.viadee.bpm.vpav.output.IssueWriter;
import de.viadee.bpm.vpav.processing.CheckName;
import de.viadee.bpm.vpav.processing.code.flow.BpmnElement;
import de.viadee.bpm.vpav.processing.model.data.CheckerIssue;
import de.viadee.bpm.vpav.processing.model.data.CriticalityEnum;
import org.camunda.bpm.model.bpmn.impl.BpmnModelConstants;
import org.camunda.bpm.model.bpmn.instance.*;

import java.util.ArrayList;
import java.util.Collection;

public class MessageEventChecker extends AbstractElementChecker {

    public MessageEventChecker(final Rule rule) {
        super(rule);
    }

    /**
     * Check MessageEvents for implementation and messages
     *
     * @return issues
     */
    @Override
    public Collection<CheckerIssue> check(BpmnElement element) {

        final Collection<CheckerIssue> issues = new ArrayList<>();
        final BaseElement baseElement = element.getBaseElement();

        if (baseElement instanceof EndEvent
                || baseElement instanceof IntermediateCatchEvent
                || baseElement instanceof IntermediateThrowEvent
                || baseElement instanceof BoundaryEvent) {

            checkEventsInSubProcess(element, issues, baseElement);
        } else if (baseElement instanceof ReceiveTask) {

            if (baseElement.getAttributeValue(BpmnModelConstants.BPMN_ATTRIBUTE_MESSAGE_REF) == null
                    || baseElement.getAttributeValue(BpmnModelConstants.BPMN_ATTRIBUTE_MESSAGE_REF).isEmpty()) {

                issues.addAll(IssueWriter.createIssue(rule, CriticalityEnum.ERROR, element,
                        String.format(Messages.getString("MessageEventChecker.0"),
                                CheckName.checkName(baseElement)))); //$NON-NLS-1$

            } else {
                String messageName = ((ReceiveTask) baseElement).getMessage().getName();
                if (messageName == null || messageName.isEmpty()) {
                    issues.addAll(IssueWriter.createIssue(rule, CriticalityEnum.ERROR, element,
                            String.format(Messages.getString("MessageEventChecker.1"), //$NON-NLS-1$
                                    CheckName.checkName(baseElement))));
                }
            }
        } else if (baseElement instanceof StartEvent) {

            // Depending on whether the startEvent is part of a subprocess, expressions may
            // be allowed to exist
            if (BpmnScanner.isSubprocess(baseElement)) {
                checkEventsInSubProcess(element, issues, baseElement);
            } else {
                checkEventsInProcess(element, issues, baseElement);
            }
        }
        return issues;
    }

    /**
     * Checks for existence of messages in startEvents. Expressions will create an
     * issue due to write/read anomaly
     *
     * @param element     BpmnElement
     * @param issues      Collection of CheckerIssues
     * @param baseElement BaseElement
     */
    private void checkEventsInProcess(BpmnElement element, final Collection<CheckerIssue> issues,
            final BaseElement baseElement) {
        final Event event = (Event) baseElement;
        final Collection<MessageEventDefinition> messageEventDefinitions = event
                .getChildElementsByType(MessageEventDefinition.class);
        if (messageEventDefinitions != null) {
            for (MessageEventDefinition eventDef : messageEventDefinitions) {
                if (eventDef != null) {
                    final Message message = eventDef.getMessage();
                    if (message == null) {
                        issues.addAll(IssueWriter.createIssue(rule, CriticalityEnum.ERROR, element,
                                String.format(Messages.getString("MessageEventChecker.2"), //$NON-NLS-1$
                                        CheckName.checkName(baseElement))));
                    } else {
                        if (message.getName() == null || message.getName().isEmpty()) {
                            issues.addAll(IssueWriter.createIssue(rule, CriticalityEnum.ERROR, element,
                                    String.format(Messages.getString("MessageEventChecker.3"), //$NON-NLS-1$
                                            CheckName.checkName(baseElement))));
                        } else if (message.getName().contains("{") || message.getName()
                                .contains("}")) { //$NON-NLS-1$ //$NON-NLS-2$
                            issues.addAll(IssueWriter.createIssue(rule, CriticalityEnum.ERROR, element,
                                    String.format(Messages.getString("MessageEventChecker.6"), //$NON-NLS-1$
                                            CheckName.checkName(baseElement))));
                        }
                    }
                }
            }
        }
    }

    /**
     * Checks for existence of messages and expression in startEvents
     *
     * @param element     BpmnElement
     * @param issues      Collection of CheckerIssues
     * @param baseElement BaseElement
     */
    private void checkEventsInSubProcess(BpmnElement element, final Collection<CheckerIssue> issues,
            final BaseElement baseElement) {
        final Event event = (Event) baseElement;
        final Collection<MessageEventDefinition> messageEventDefinitions = event
                .getChildElementsByType(MessageEventDefinition.class);
        if (messageEventDefinitions != null) {
            for (MessageEventDefinition eventDef : messageEventDefinitions) {
                if (eventDef != null) {
                    final Message message = eventDef.getMessage();
                    if (message == null) {
                        issues.addAll(IssueWriter.createIssue(rule, CriticalityEnum.ERROR, element,
                                String.format(Messages.getString("MessageEventChecker.7"), //$NON-NLS-1$
                                        CheckName.checkName(baseElement))));
                    } else {
                        if (message.getName() == null || message.getName().isEmpty()) {
                            issues.addAll(IssueWriter.createIssue(rule, CriticalityEnum.ERROR, element,
                                    String.format(Messages.getString("MessageEventChecker.8"), //$NON-NLS-1$
                                            CheckName.checkName(baseElement))));
                        }
                    }
                }
            }
        }
    }

}
