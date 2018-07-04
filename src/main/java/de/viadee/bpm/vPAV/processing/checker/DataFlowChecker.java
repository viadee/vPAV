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

import de.viadee.bpm.vPAV.config.model.Rule;
import de.viadee.bpm.vPAV.output.IssueWriter;
import de.viadee.bpm.vPAV.processing.dataflow.DataFlowRule;
import de.viadee.bpm.vPAV.processing.model.data.ProcessVariable;
import de.viadee.bpm.vPAV.processing.model.data.CheckerIssue;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;

import java.util.ArrayList;
import java.util.Collection;

public class DataFlowChecker implements ModelChecker {

    private Rule rule;
    private Collection<DataFlowRule> dataFlowRules;
    private Collection<ProcessVariable> processVariables;

    public DataFlowChecker(Rule rule, Collection<DataFlowRule> dataFlowRules, Collection<ProcessVariable> processVariables) {
        this.rule = rule;
        this.dataFlowRules = dataFlowRules;
        this.processVariables = processVariables;
    }

    @Override
    public Collection<CheckerIssue> check(BpmnModelInstance processdefinition) {
        final Collection<CheckerIssue> issues = new ArrayList<CheckerIssue>();
        for (DataFlowRule dataFlowRule : dataFlowRules) {
            dataFlowRule.evaluate(processVariables).stream()
                    .filter(r-> !r.isFulfilled())
                    // TODO: think about correct BPMN element
                    // TODO: Message template in resource bundle
                    .map(r -> IssueWriter.createIssue(rule, dataFlowRule.getCriticality(),
                            r.getEvaluatedVariable().getOperations().get(0).getElement(),
                            String.format("Rule '%s' violated:\n%s %s", dataFlowRule.getRuleDescription(),
                                    r.getEvaluatedVariable().getName(), r.getMessage().get())))
                    .forEach(issues::addAll);
        }
        return issues;
    }
}
