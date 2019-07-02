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
package de.viadee.bpm.vPAV.config.reader;

import de.viadee.bpm.vPAV.BpmnScanner;
import de.viadee.bpm.vPAV.config.model.ElementConvention;
import de.viadee.bpm.vPAV.config.model.Rule;
import de.viadee.bpm.vPAV.processing.ProcessingException;
import de.viadee.bpm.vPAV.processing.checker.AbstractElementChecker;
import de.viadee.bpm.vPAV.processing.code.flow.BpmnElement;
import de.viadee.bpm.vPAV.processing.model.data.CheckerIssue;
import de.viadee.bpm.vPAV.processing.model.data.CriticalityEnum;
import org.camunda.bpm.model.bpmn.instance.BaseElement;
import org.camunda.bpm.model.bpmn.instance.Task;

import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TaskNamingConventionCheckerExtern extends AbstractElementChecker {

    private String patternString;

    private String description;

    public TaskNamingConventionCheckerExtern(final Rule rule, final BpmnScanner bpmnScanner) {
        super(rule, bpmnScanner);

        final Collection<ElementConvention> elementConventions = rule.getElementConventions();
        if (elementConventions == null || elementConventions.size() < 1
                || elementConventions.size() > 1) {
            throw new ProcessingException(
                    "task naming convention checker must have one element convention!");
        }
        patternString = elementConventions.iterator().next().getPattern();
        description = elementConventions.iterator().next().getDescription();
    }

    /**
     * Check if elements adhere to a configurable naming convention
     *
     * @return issues
     */
    @Override
    public Collection<CheckerIssue> check(final BpmnElement element) {
        final Collection<CheckerIssue> issues = new ArrayList<CheckerIssue>();
        final BaseElement baseElement = element.getBaseElement();
        if (baseElement instanceof Task) {
            final String taskName = baseElement.getAttributeValue("name");
            boolean taskNameExists = taskName != null && taskName.trim().length() > 0;
            if (taskNameExists) {
                if (isTasknameInvalid(taskName)) {
                    String message = "task name '" + taskName + "' is against the naming convention";
                    issues.add(createIssue(element, baseElement, message, description, CriticalityEnum.WARNING));
                }
            } else {
                issues.add(
                        createIssue(element, baseElement, "task name must be specified", null, CriticalityEnum.ERROR));
            }
        }
        return issues;
    }

    private boolean isTasknameInvalid(final String taskName) {

        final Pattern pattern = Pattern.compile(patternString);
        Matcher matcher = pattern.matcher(taskName);
        boolean b = !matcher.matches();
        return b;
    }

    private CheckerIssue createIssue(final BpmnElement element, final BaseElement baseElement, String message,
                                     String description, CriticalityEnum error) {
        return new CheckerIssue(rule.getName(), rule.getRuleDescription(), error,
                element.getProcessDefinition(),
                null, baseElement.getId(), baseElement.getAttributeValue("name"), null, null, null,
                message, description);
    }
}