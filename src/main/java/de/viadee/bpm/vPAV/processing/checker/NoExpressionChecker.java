/**
 * Copyright � 2017, viadee Unternehmensberatung GmbH
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
package de.viadee.bpm.vPAV.processing.checker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

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
import org.xml.sax.SAXException;

import de.viadee.bpm.vPAV.BPMNScanner;
import de.viadee.bpm.vPAV.config.model.Rule;
import de.viadee.bpm.vPAV.config.model.Setting;
import de.viadee.bpm.vPAV.processing.CheckName;
import de.viadee.bpm.vPAV.processing.model.data.BpmnElement;
import de.viadee.bpm.vPAV.processing.model.data.CheckerIssue;
import de.viadee.bpm.vPAV.processing.model.data.CriticalityEnum;

public class NoExpressionChecker extends AbstractElementChecker {

    public NoExpressionChecker(final Rule rule, final String path) {
        super(rule, path);
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
        final BPMNScanner scan;

        try {

            scan = new BPMNScanner(path);

            final Map<String, Setting> settings = rule.getSettings();

            if (baseElement instanceof ServiceTask || baseElement instanceof BusinessRuleTask
                    || baseElement instanceof SendTask || baseElement instanceof ScriptTask) {

                // read attributes from task
                final String implementationAttr = scan.getImplementation(baseElement.getId());

                if (implementationAttr != null && implementationAttr.equals(scan.getC_exp())
                        && !settings.containsKey(baseElement.getElementType().getInstanceType().getSimpleName())) {
                    issues.add(new CheckerIssue(rule.getName(), CriticalityEnum.WARNING,
                            element.getProcessdefinition(), null, baseElement.getAttributeValue("id"),
                            baseElement.getAttributeValue("name"), null, null, null,
                            "Usage of expressions in '" + CheckName.checkName(baseElement)
                                    + "' is against best practices."));
                }

                // get the execution listener
                final ArrayList<String> listener = scan.getListener(baseElement.getId(), "expression",
                        "camunda:executionListener");

                if (!listener.isEmpty() && listener.size() > 0
                        && !settings.containsKey(baseElement.getElementType().getInstanceType().getSimpleName())) {
                    issues.add(new CheckerIssue(rule.getName(), CriticalityEnum.WARNING,
                            element.getProcessdefinition(), null, baseElement.getAttributeValue("id"),
                            baseElement.getAttributeValue("name"), null, null, null,
                            "Usage of expression in listeners for '" + CheckName.checkName(baseElement)
                                    + "' is against best practices."));
                }

            } else if (baseElement instanceof IntermediateThrowEvent
                    || baseElement instanceof EndEvent || baseElement instanceof StartEvent) {

                // read attributes from event
                final String implementationAttrEvent = scan.getEventImplementation(baseElement.getId());

                if (implementationAttrEvent != null && implementationAttrEvent.contains(scan.getC_exp())
                        && !settings.containsKey(baseElement.getElementType().getInstanceType().getSimpleName())) {
                    issues.add(new CheckerIssue(rule.getName(), CriticalityEnum.WARNING,
                            element.getProcessdefinition(), null, baseElement.getAttributeValue("id"),
                            baseElement.getAttributeValue("name"), null, null, null,
                            "Usage of expression in event '" + CheckName.checkName(baseElement)
                                    + "' is against best practices."));
                }

                // get the execution listener
                final ArrayList<String> listener = scan.getListener(baseElement.getId(), "expression",
                        "camunda:executionListener");

                if (!listener.isEmpty() && listener.size() > 0
                        && !settings.containsKey(baseElement.getElementType().getInstanceType().getSimpleName())) {
                    issues.add(new CheckerIssue(rule.getName(), CriticalityEnum.WARNING,
                            element.getProcessdefinition(), null, baseElement.getAttributeValue("id"),
                            baseElement.getAttributeValue("name"), null, null, null,
                            "Usage of expression in listeners for '" + CheckName.checkName(baseElement)
                                    + "' is against best practices."));
                }

            } else if (baseElement instanceof SequenceFlow) {

                // get the execution listener
                final ArrayList<String> listener = scan.getListener(baseElement.getId(), "expression",
                        "camunda:executionListener");
                if (!listener.isEmpty()
                        && !settings.containsKey(baseElement.getElementType().getInstanceType().getSimpleName())) {
                    issues.add(new CheckerIssue(rule.getName(), CriticalityEnum.WARNING,
                            element.getProcessdefinition(), null, baseElement.getAttributeValue("id"),
                            baseElement.getAttributeValue("name"), null, null, null,
                            "Usage of expression in listeners for '" + CheckName.checkName(baseElement)
                                    + "' is against best practices."));
                }

            } else if (baseElement instanceof ExclusiveGateway) {
                // get the execution listener
                final ArrayList<String> listener = scan.getListener(baseElement.getId(), "expression",
                        "camunda:executionListener");
                if (!listener.isEmpty()
                        && !settings.containsKey(baseElement.getElementType().getInstanceType().getSimpleName())) {
                    issues.add(new CheckerIssue(rule.getName(), CriticalityEnum.WARNING,
                            element.getProcessdefinition(), null, baseElement.getAttributeValue("id"),
                            baseElement.getAttributeValue("name"), null, null, null,
                            "Usage of expression in listeners for '" + CheckName.checkName(baseElement)
                                    + "' is against best practices."));
                }
            } else if (baseElement instanceof UserTask) {
                // get the execution listener
                final ArrayList<String> listener = scan.getListener(baseElement.getId(), "expression",
                        "camunda:executionListener");
                if (!listener.isEmpty()
                        && !settings.containsKey(baseElement.getElementType().getInstanceType().getSimpleName())) {
                    issues.add(new CheckerIssue(rule.getName(), CriticalityEnum.WARNING,
                            element.getProcessdefinition(), null, baseElement.getAttributeValue("id"),
                            baseElement.getAttributeValue("name"), null, null, null,
                            "Usage of expression in listeners for '" + CheckName.checkName(baseElement)
                                    + "' is against best practices."));
                }

                // get the task listener
                final ArrayList<String> taskListener = scan.getListener(baseElement.getId(), "expression",
                        "camunda:taskListener");
                if (!taskListener.isEmpty()
                        && !settings.containsKey(baseElement.getElementType().getInstanceType().getSimpleName())) {
                    issues.add(new CheckerIssue(rule.getName(), CriticalityEnum.WARNING,
                            element.getProcessdefinition(), null, baseElement.getAttributeValue("id"),
                            baseElement.getAttributeValue("name"), null, null, null,
                            "Usage of expression in listeners for '" + CheckName.checkName(baseElement)
                                    + "' is against best practices."));
                }

            } else if (baseElement instanceof ManualTask) {
                // get the execution listener
                final ArrayList<String> listener = scan.getListener(baseElement.getId(), "expression",
                        "camunda:executionListener");
                if (!listener.isEmpty()
                        && !settings.containsKey(baseElement.getElementType().getInstanceType().getSimpleName())) {
                    issues.add(new CheckerIssue(rule.getName(), CriticalityEnum.WARNING,
                            element.getProcessdefinition(), null, baseElement.getAttributeValue("id"),
                            baseElement.getAttributeValue("name"), null, null, null,
                            "Usage of expression in listeners for '" + CheckName.checkName(baseElement)
                                    + "' is against best practices."));
                }
            }

        } catch (ParserConfigurationException | SAXException |

                IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return issues;
    }

}
