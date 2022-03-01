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

import de.viadee.bpm.vpav.config.model.Rule;
import de.viadee.bpm.vpav.output.IssueWriter;
import de.viadee.bpm.vpav.processing.code.flow.FlowAnalysis;
import de.viadee.bpm.vpav.processing.dataflow.DataFlowRule;
import de.viadee.bpm.vpav.processing.model.data.AnomalyContainer;
import de.viadee.bpm.vpav.processing.model.data.CheckerIssue;
import de.viadee.bpm.vpav.processing.model.data.ProcessVariable;
import de.viadee.bpm.vpav.processing.model.graph.Path;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class DataFlowChecker extends AbstractModelChecker {

    private final Collection<DataFlowRule> dataFlowRules;

    public DataFlowChecker(final Rule rule,
            final Map<AnomalyContainer, List<Path>> invalidPathsMap,
            final Collection<ProcessVariable> processVariables, final Collection<DataFlowRule> dataFlowRules,
            final FlowAnalysis flowAnalysis) {
        super(rule, invalidPathsMap, processVariables, flowAnalysis);
        this.dataFlowRules = dataFlowRules;
    }

    @Override
    public Collection<CheckerIssue> check() {
        final Collection<CheckerIssue> issues = new ArrayList<>();
        for (DataFlowRule dataFlowRule : dataFlowRules) {
            dataFlowRule.evaluate(processVariables).stream()
                    .filter(r -> !r.isFulfilled())
                    .map(r -> IssueWriter
                            .createIssue(rule, dataFlowRule.getRuleDescription(), dataFlowRule.getCriticality(),
                                    r.getEvaluatedVariable(), dataFlowRule.getViolationMessageFor(r)))
                    .forEach(issues::addAll);
        }
        return issues;
    }

    @Override
    public boolean isSingletonChecker() {
        return true;
    }
}
