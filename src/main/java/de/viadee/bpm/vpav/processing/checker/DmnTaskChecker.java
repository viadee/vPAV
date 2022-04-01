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

import de.viadee.bpm.vpav.BpmnScanner;
import de.viadee.bpm.vpav.Messages;
import de.viadee.bpm.vpav.RuntimeConfig;
import de.viadee.bpm.vpav.config.model.Rule;
import de.viadee.bpm.vpav.constants.BpmnConstants;
import de.viadee.bpm.vpav.output.IssueWriter;
import de.viadee.bpm.vpav.processing.CheckName;
import de.viadee.bpm.vpav.processing.code.flow.BpmnElement;
import de.viadee.bpm.vpav.processing.model.data.CheckerIssue;
import de.viadee.bpm.vpav.processing.model.data.CriticalityEnum;
import org.camunda.bpm.model.bpmn.instance.BaseElement;
import org.camunda.bpm.model.bpmn.instance.BusinessRuleTask;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 * Checks, whether a business rule task with dmn implementation is valid
 */
public class DmnTaskChecker extends AbstractElementChecker {

    public DmnTaskChecker(final Rule rule) {
        super(rule);
    }

    /**
     * Check a BusinessRuleTask for a DMN reference
     *
     * @return issues
     */
    @Override
    public Collection<CheckerIssue> check(final BpmnElement element) {
        final Collection<CheckerIssue> issues = new ArrayList<>();
        final BaseElement bpmnElement = element.getBaseElement();
        if (bpmnElement instanceof BusinessRuleTask) {
            // read attributes from task
            final Map.Entry<String, String> implementation = BpmnScanner
                    .getImplementation(bpmnElement);

            if (implementation != null && implementation.getKey().equals(BpmnConstants.CAMUNDA_DMN)) {
                // check if DMN reference is not empty
                if (implementation.getValue() == null || implementation.getValue().trim().length() == 0) {
                    // Error, because no delegateExpression has been configured
                    issues.addAll(IssueWriter.createIssue(rule, CriticalityEnum.ERROR, element,
                            String.format(Messages.getString("DmnTaskChecker.0"), //$NON-NLS-1$
                                    CheckName.checkName(bpmnElement))));
                } else {

                    issues.addAll(checkDMNFile(element, implementation.getValue()));
                }
            }
        }
        return issues;
    }

    /**
     * Check if the referenced DMN in a BusinessRuleTask exists
     *
     * @param element BpmnElement
     * @param dmnName Name of DMN-File
     * @return issues
     */
    private Collection<CheckerIssue> checkDMNFile(final BpmnElement element, final String dmnName) {

        final Collection<CheckerIssue> issues = new ArrayList<>();
        final BaseElement bpmnElement = element.getBaseElement();
        final String dmnPath = dmnName.replaceAll("\\.", "/") + ".dmn"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        // If a dmn path has been found, check the correctness
        URL urlDMN = RuntimeConfig.getInstance().getClassLoader().getResource(dmnPath);
        if (urlDMN == null && RuntimeConfig.getInstance().getFileScanner().getDecisionRefToPathMap()
                .containsKey(dmnName)) {
            // Trying to retrieve the dmn filename by dmn id if the native reference doesn't result in a match
            String dmnFileName = RuntimeConfig.getInstance()
                    .getFileScanner().getDecisionRefToPathMap().get(dmnName);
            //Set new dmn url if retrieval by dmn id was successful
            if (dmnFileName != null) {
                urlDMN = RuntimeConfig.getInstance().getClassLoader().getResource(dmnFileName);
            }
        }

        if (urlDMN == null) {
            // Throws an error, if the class was not found
            issues.addAll(IssueWriter.createIssue(rule, CriticalityEnum.ERROR, element,
                    String.format(Messages.getString("DmnTaskChecker.4"),
                            CheckName.checkName(bpmnElement)))); //$NON-NLS-1$
        }
        return issues;
    }
}
