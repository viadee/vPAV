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
package de.viadee.bpm.vPAV.processing.checker;

import de.viadee.bpm.vPAV.BpmnScanner;
import de.viadee.bpm.vPAV.Messages;
import de.viadee.bpm.vPAV.config.model.Rule;
import de.viadee.bpm.vPAV.config.model.Setting;
import de.viadee.bpm.vPAV.constants.BpmnConstants;
import de.viadee.bpm.vPAV.output.IssueWriter;
import de.viadee.bpm.vPAV.processing.CheckName;
import de.viadee.bpm.vPAV.processing.code.flow.BpmnElement;
import de.viadee.bpm.vPAV.processing.model.data.CheckerIssue;
import de.viadee.bpm.vPAV.processing.model.data.CriticalityEnum;
import org.camunda.bpm.model.bpmn.instance.BaseElement;
import org.camunda.bpm.model.bpmn.instance.ScriptTask;
import org.camunda.bpm.model.bpmn.instance.SequenceFlow;
import org.camunda.bpm.model.bpmn.instance.SubProcess;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class NoScriptChecker extends AbstractElementChecker {

    public NoScriptChecker(final Rule rule) {
        super(rule);
    }

    /**
     * Checks a bpmn model, if there is any script (Script inside a script task -
     * Script as an execution listener - Script as a task listener - Script inside
     * an inputOutput parameter mapping)
     *
     * @return issues
     */
    @Override
    public Collection<CheckerIssue> check(final BpmnElement element) {

        final Collection<CheckerIssue> issues = new ArrayList<>();
        final BaseElement bpmnElement = element.getBaseElement();

        if (!(bpmnElement instanceof Process) && !(bpmnElement instanceof SubProcess)
                && !bpmnElement.getElementType().getInstanceType().getSimpleName()
                .equals(BpmnConstants.SIMPLE_NAME_PROCESS)
                && !bpmnElement.getElementType().getInstanceType().getSimpleName()
                .equals(BpmnConstants.SIMPLE_NAME_SUB_PROCESS)) {
            Map<String, Setting> settings = rule.getSettings();

            // Check all Elements with camunda:script tag
            List<String> scriptTypes = BpmnScanner
                    .getScriptTypes(bpmnElement);
            if (!scriptTypes.isEmpty()) {
                if (!settings.containsKey(bpmnElement.getElementType().getInstanceType().getSimpleName())) {
                    for (String place : scriptTypes)
                        issues.addAll(IssueWriter.createIssue(rule, CriticalityEnum.ERROR, element,
                                String.format(Messages.getString("NoScriptChecker.0"), CheckName.checkName(bpmnElement),
                                        //$NON-NLS-1$
                                        place)));
                } else {
                    List<String> allowedPlaces = settings
                            .get(bpmnElement.getElementType().getInstanceType().getSimpleName()).getScriptPlaces();
                    if (!allowedPlaces.isEmpty())
                        for (String scriptType : scriptTypes)
                            if (!allowedPlaces.contains(scriptType))
                                issues.addAll(IssueWriter.createIssue(rule, CriticalityEnum.ERROR, element,
                                        String.format(Messages.getString("NoScriptChecker.1"), //$NON-NLS-1$
                                                CheckName.checkName(bpmnElement), scriptType)));
                }
            }

            // ScriptTask
            if (bpmnElement instanceof ScriptTask && !settings.containsKey(BpmnConstants.SIMPLE_NAME_SCRIPT_TASK)) {
                issues.addAll(IssueWriter.createIssue(rule, CriticalityEnum.ERROR, element,
                        String.format(Messages.getString("NoScriptChecker.2"),
                                CheckName.checkName(bpmnElement)))); //$NON-NLS-1$
            }

            // Check SequenceFlow on script in conditionExpression
            if (bpmnElement instanceof SequenceFlow) {
                boolean scriptCondExp = BpmnScanner
                        .hasScriptInCondExp((SequenceFlow) bpmnElement);
                if (settings.containsKey(BpmnConstants.SIMPLE_NAME_SEQUENCE_FLOW)) {
                    List<String> allowedPlaces = settings.get(BpmnConstants.SIMPLE_NAME_SEQUENCE_FLOW)
                            .getScriptPlaces();
                    if (!allowedPlaces.isEmpty() && !allowedPlaces.contains(BpmnConstants.COND_EXP) && scriptCondExp) {
                        issues.addAll(IssueWriter.createIssue(rule, CriticalityEnum.ERROR, element,
                                String.format(Messages.getString("NoScriptChecker.3"), //$NON-NLS-1$
                                        CheckName.checkName(bpmnElement))));
                    }

                } else {
                    if (scriptCondExp) {
                        issues.addAll(IssueWriter.createIssue(rule, CriticalityEnum.ERROR, element,
                                String.format(Messages.getString("NoScriptChecker.4"), //$NON-NLS-1$
                                        CheckName.checkName(bpmnElement))));
                    }
                }
            }
        }

        return issues;
    }
}
