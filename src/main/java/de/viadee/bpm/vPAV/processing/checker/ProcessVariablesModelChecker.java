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
import java.util.List;
import java.util.Map;

import de.viadee.bpm.vPAV.processing.model.data.*;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.impl.BpmnModelConstants;

import de.viadee.bpm.vPAV.Messages;
import de.viadee.bpm.vPAV.config.model.Rule;
import de.viadee.bpm.vPAV.output.IssueWriter;
import de.viadee.bpm.vPAV.processing.model.data.ProcessVariableOperation;
import de.viadee.bpm.vPAV.processing.model.graph.Path;

public class ProcessVariablesModelChecker implements ModelChecker {

    private final Rule rule;

    private final Map<AnomalyContainer, List<Path>> invalidPathsMap;

    public ProcessVariablesModelChecker(final Rule rule,
            final Map<AnomalyContainer, List<Path>> invalidPathsMap) {
        this.rule = rule;
        this.invalidPathsMap = invalidPathsMap;
    }

    /**
     * Checks variables of a given process and identifies read/write/delete anomalies
     *
     * @return issues
     */
    @Override
    public Collection<CheckerIssue> check(final BpmnModelInstance processdefinition) {

        final Collection<CheckerIssue> issues = new ArrayList<CheckerIssue>();
        for (final AnomalyContainer anomaly : invalidPathsMap.keySet()) {
            final List<Path> paths = invalidPathsMap.get(anomaly);
            final ProcessVariableOperation var = anomaly.getVariable();
            if (paths != null) {
                if (anomaly.getAnomaly() == Anomaly.DD) {
                    issues.addAll(
                            IssueWriter.createIssue(rule, determineCriticality(anomaly.getAnomaly()), var, paths,
                                    anomaly,
                                    String.format(
                                            Messages.getString("ProcessVariablesModelChecker.0"), //$NON-NLS-1$
                                            var.getName(),
                                            var.getElement().getBaseElement()
                                                    .getAttributeValue(BpmnModelConstants.BPMN_ATTRIBUTE_NAME),
                                            var.getChapter(), var.getFieldType().getDescription())));
                } else if (anomaly.getAnomaly() == Anomaly.DU) {
                    issues.addAll(
                            IssueWriter.createIssue(rule, determineCriticality(anomaly.getAnomaly()), var, paths,
                                    anomaly,
                                    String.format(
                                            Messages.getString("ProcessVariablesModelChecker.1"), //$NON-NLS-1$
                                            var.getName(),
                                            var.getElement().getBaseElement()
                                                    .getAttributeValue(BpmnModelConstants.BPMN_ATTRIBUTE_NAME),
                                            var.getChapter(), var.getFieldType().getDescription())));
                } else if (anomaly.getAnomaly() == Anomaly.UR) {
                    issues.addAll(
                            IssueWriter.createIssue(rule, determineCriticality(anomaly.getAnomaly()), var, paths,
                                    anomaly, String.format(
                                            Messages.getString("ProcessVariablesModelChecker.2"), //$NON-NLS-1$
                                            var.getName(),
                                            var.getElement().getBaseElement()
                                                    .getAttributeValue(BpmnModelConstants.BPMN_ATTRIBUTE_NAME),
                                            var.getChapter(), var.getFieldType().getDescription())));
                }
            }
        }

        return issues;
    }

    private CriticalityEnum determineCriticality(final Anomaly anomaly) {

        if (anomaly == Anomaly.DD || anomaly == Anomaly.DU) {
            return CriticalityEnum.WARNING;
        } else {
            return CriticalityEnum.ERROR;
        }
    }
}
