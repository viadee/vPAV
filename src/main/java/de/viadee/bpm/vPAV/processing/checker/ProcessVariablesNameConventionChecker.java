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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.viadee.bpm.vPAV.BpmnScanner;
import de.viadee.bpm.vPAV.Messages;
import de.viadee.bpm.vPAV.config.model.ElementConvention;
import de.viadee.bpm.vPAV.config.model.ElementFieldTypes;
import de.viadee.bpm.vPAV.config.model.Rule;
import de.viadee.bpm.vPAV.output.IssueWriter;
import de.viadee.bpm.vPAV.processing.model.data.*;
import de.viadee.bpm.vPAV.processing.model.data.ProcessVariableOperation;

public class ProcessVariablesNameConventionChecker extends AbstractElementChecker {

    public ProcessVariablesNameConventionChecker(final Rule rule, final BpmnScanner bpmnScanner) {
        super(rule, bpmnScanner);
    }


    /**
     * Checks process variables in an bpmn element, whether they comply naming conventions
     *
     * @param element
     *            BpmnElement
     * @return issues collection of issues
     */
    @Override
    public Collection<CheckerIssue> check(final BpmnElement element) {

        // analyse process variables are matching naming conventions
        final Collection<CheckerIssue> issues = checkNamingConvention(element);

        return issues;
    }

    /**
     * Use regular expressions to check process variable conventions
     *
     * @param element
     * @return issues
     */
    private Collection<CheckerIssue> checkNamingConvention(final BpmnElement element) {

        final Collection<CheckerIssue> issues = new ArrayList<CheckerIssue>();

        final Collection<ElementConvention> elementConventions = rule.getElementConventions();
        if (elementConventions != null) {
            for (final ElementConvention convention : elementConventions) {
                final Pattern pattern = Pattern.compile(convention.getPattern());
                final ElementFieldTypes fieldTypes = convention.getElementFieldTypes();
                final Collection<String> fieldTypeItems = fieldTypes.getElementFieldTypes();
                for (final ProcessVariableOperation variable : element.getProcessVariables().values()) {
                    if (variable.getOperation() == VariableOperation.WRITE) {
                        if (fieldTypeItems != null) {
                            boolean isInRange = false;
                            if (fieldTypes.isExcluded()) {
                                isInRange = !fieldTypeItems.contains(variable.getFieldType().name());
                            } else {
                                isInRange = fieldTypeItems.contains(variable.getFieldType().name());
                            }
                            if (isInRange) {
                                final Matcher patternMatcher = pattern.matcher(variable.getName());
                                if (!patternMatcher.matches()) {
                                    issues.add(IssueWriter.createSingleIssue(rule, CriticalityEnum.WARNING, element,
                                            variable.getResourceFilePath(), variable.getName(),
                                            String.format(
                                                    Messages.getString("ProcessVariablesNameConventionChecker.0"), //$NON-NLS-1$
                                                    variable.getName(), convention.getName(), variable.getChapter(),
                                                    variable.getFieldType().getDescription()),
                                            convention));

                                }
                            }
                        }
                    }
                }
            }
        }

        return issues;
    }
}
