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
package de.viadee.bpm.vPAV.processing.checker;

import de.viadee.bpm.vPAV.config.model.Rule;
import de.viadee.bpm.vPAV.output.IssueWriter;
import de.viadee.bpm.vPAV.processing.dataflow.DataFlowRule;
import de.viadee.bpm.vPAV.processing.model.data.CheckerIssue;
import de.viadee.bpm.vPAV.processing.model.data.ProcessVariable;

import java.util.ArrayList;
import java.util.Collection;

public class DataFlowChecker implements ModelChecker {

    private Rule rule;
    private Collection<DataFlowRule> dataFlowRules;
    private Collection<ProcessVariable> processVariables;

    public DataFlowChecker(final Rule rule, final Collection<DataFlowRule> dataFlowRules, final Collection<ProcessVariable> processVariables) {
        this.rule = rule;
        this.dataFlowRules = dataFlowRules;
        this.processVariables = processVariables;
    }

    @Override
    public Collection<CheckerIssue> check() {
        final Collection<CheckerIssue> issues = new ArrayList<CheckerIssue>();
        for (DataFlowRule dataFlowRule : dataFlowRules) {
            dataFlowRule.evaluate(processVariables).stream()
                    .filter(r-> !r.isFulfilled())
                    .map(r -> IssueWriter.createIssue(rule, dataFlowRule.getRuleDescription(), dataFlowRule.getCriticality(),
                            r.getEvaluatedVariable(), dataFlowRule.getViolationMessageFor(r)))
                    .forEach(issues::addAll);
        }
        return issues;
    }

    public boolean isSingletonChecker() {
        return true;
    }
}
