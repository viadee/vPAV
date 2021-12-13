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

import de.viadee.bpm.vPAV.Messages;
import de.viadee.bpm.vPAV.config.model.Rule;
import de.viadee.bpm.vPAV.config.model.Setting;
import de.viadee.bpm.vPAV.constants.LinterConstants;
import de.viadee.bpm.vPAV.output.IssueWriter;
import de.viadee.bpm.vPAV.processing.CheckName;
import de.viadee.bpm.vPAV.processing.code.flow.BpmnElement;
import de.viadee.bpm.vPAV.processing.model.data.CheckerIssue;
import de.viadee.bpm.vPAV.processing.model.data.CriticalityEnum;
import org.camunda.bpm.model.bpmn.instance.BaseElement;
import org.camunda.bpm.model.bpmn.instance.ExclusiveGateway;
import org.camunda.bpm.model.bpmn.instance.SequenceFlow;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

public class LinterChecker extends AbstractElementChecker {

    public LinterChecker(Rule rule) {
        super(rule);
    }

    @Override
    public Collection<CheckerIssue> check(BpmnElement element) {
        final Collection<CheckerIssue> issues = new ArrayList<>();
        final BaseElement bpmnElement = element.getBaseElement();
        final Map<String, Setting> settings = rule.getSettings();

        if (settings.containsKey(LinterConstants.CONDITIONAL_SEQUENCE_FLOWS)) {
            issues.addAll(checkConditionalFlows(bpmnElement, element));
        }

        return issues;
    }

    private Collection<CheckerIssue> checkConditionalFlows(BaseElement bpmnElement, BpmnElement element) {
        final Collection<CheckerIssue> issues = new ArrayList<>();
        if (bpmnElement instanceof ExclusiveGateway && ((ExclusiveGateway) bpmnElement).getOutgoing().size() > 1) {
            final Collection<SequenceFlow> edges = ((ExclusiveGateway) bpmnElement).getOutgoing();
            for (SequenceFlow flow : edges) {
                boolean missingCondition = !hasCondition(flow) && !isDefaultFlow(bpmnElement, flow);
                if (missingCondition) {
                    issues.addAll(IssueWriter.createIssue(rule, CriticalityEnum.ERROR, flow, element.getProcessDefinition(),
                            String.format(Messages.getString("LinterChecker.1"),
                                    CheckName.checkName(bpmnElement))));
                }
            }
        }
        return issues;
    }

    private boolean hasCondition(SequenceFlow flow) {
        return flow.getConditionExpression() != null;
    }

    private boolean isDefaultFlow(BaseElement element, SequenceFlow flow) {
        return element.getAttributeValue("default") != null && element.getAttributeValue("default").equals(flow.getId());
    }
}
