/**
 * Copyright � 2017, viadee Unternehmensberatung GmbH All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met: 1. Redistributions of source code must retain the above copyright notice, this list of
 * conditions and the following disclaimer. 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation and/or other materials provided with the
 * distribution. 3. All advertising materials mentioning features or use of this software must display the following
 * acknowledgement: This product includes software developed by the viadee Unternehmensberatung GmbH. 4. Neither the
 * name of the viadee Unternehmensberatung GmbH nor the names of its contributors may be used to endorse or promote
 * products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY <COPYRIGHT HOLDER> ''AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO
 * EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package de.viadee.bpm.vPAV.processing.checker;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;

import de.viadee.bpm.vPAV.config.model.Rule;
import de.viadee.bpm.vPAV.processing.model.data.Anomaly;
import de.viadee.bpm.vPAV.processing.model.data.AnomalyContainer;
import de.viadee.bpm.vPAV.processing.model.data.CheckerIssue;
import de.viadee.bpm.vPAV.processing.model.data.CriticalityEnum;
import de.viadee.bpm.vPAV.processing.model.data.ProcessVariable;
import de.viadee.bpm.vPAV.processing.model.graph.Path;

public class ProcessVariablesModelChecker implements ModelChecker {

    private final Rule rule;

    private final Map<AnomalyContainer, List<Path>> invalidPathsMap;

    public ProcessVariablesModelChecker(final Rule rule,
            final Map<AnomalyContainer, List<Path>> invalidPathsMap) {
        this.rule = rule;
        this.invalidPathsMap = invalidPathsMap;
    }

    @Override
    public Collection<CheckerIssue> check(final BpmnModelInstance processdefinition) {

        final Collection<CheckerIssue> issues = new ArrayList<CheckerIssue>();
        for (final AnomalyContainer anomaly : invalidPathsMap.keySet()) {
            final List<Path> paths = invalidPathsMap.get(anomaly);
            final ProcessVariable var = anomaly.getVariable();
            if (paths != null) {
                issues.add(new CheckerIssue(rule.getName(), determineCriticality(anomaly.getAnomaly()),
                        var.getElement().getProcessdefinition(), var.getResourceFilePath(),
                        var.getElement().getBaseElement().getId(),
                        var.getElement().getBaseElement().getAttributeValue("name"), var.getName(),
                        anomaly.getAnomaly(), paths,
                        "process variable (" + var.getName() + ") creates a "
                                + anomaly.getAnomaly().toString()
                                + "-anomaly (compare: "
                                + var.getChapter() + ", " + var.getFieldType().getDescription() + ")"));
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
