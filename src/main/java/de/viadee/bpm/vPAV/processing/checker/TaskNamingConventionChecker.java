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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.camunda.bpm.model.bpmn.instance.BaseElement;
import org.camunda.bpm.model.bpmn.instance.Task;

import de.viadee.bpm.vPAV.config.model.ElementConvention;
import de.viadee.bpm.vPAV.config.model.Rule;
import de.viadee.bpm.vPAV.processing.ProcessingException;
import de.viadee.bpm.vPAV.processing.model.data.BpmnElement;
import de.viadee.bpm.vPAV.processing.model.data.CheckerIssue;
import de.viadee.bpm.vPAV.processing.model.data.CriticalityEnum;

public class TaskNamingConventionChecker extends AbstractElementChecker {

    public TaskNamingConventionChecker(final Rule rule) {
        super(rule);
    }

    @Override
    public Collection<CheckerIssue> check(final BpmnElement element) {
        final Collection<CheckerIssue> issues = new ArrayList<CheckerIssue>();

        final BaseElement baseElement = element.getBaseElement();
        if (baseElement instanceof Task) {
            final Collection<ElementConvention> elementConventions = rule.getElementConventions();
            if (elementConventions == null || elementConventions.size() < 1
                    || elementConventions.size() > 1) {
                throw new ProcessingException(
                        "task naming convention checker must have one element convention!");
            }
            final String patternString = elementConventions.iterator().next().getPattern();
            final String taskName = baseElement.getAttributeValue("name");
            if (taskName != null && taskName.trim().length() > 0) {
                final Pattern pattern = Pattern.compile(patternString);
                Matcher matcher = pattern.matcher(taskName);
                if (!matcher.matches()) {
                    issues.add(new CheckerIssue(rule.getName(), CriticalityEnum.WARNING,
                            element.getProcessdefinition(), null, baseElement.getId(),
                            baseElement.getAttributeValue("name"), null, null, null,
                            "task name " + taskName + " is against the naming convention"));
                }
            } else {
                issues.add(
                        new CheckerIssue(rule.getName(), CriticalityEnum.ERROR, element.getProcessdefinition(),
                                null, baseElement.getId(), baseElement.getAttributeValue("name"), null, null, null,
                                "task name must be specified"));
            }
        }
        return issues;
    }
}
