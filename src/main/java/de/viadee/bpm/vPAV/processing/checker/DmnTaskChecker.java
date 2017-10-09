/**
 * Copyright � 2017, viadee Unternehmensberatung GmbH
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. All advertising materials mentioning features or use of this software
 *    must display the following acknowledgement:
 *    This product includes software developed by the viadee Unternehmensberatung GmbH.
 * 4. Neither the name of the viadee Unternehmensberatung GmbH nor the
 *    names of its contributors may be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY <viadee Unternehmensberatung GmbH> ''AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package de.viadee.bpm.vPAV.processing.checker;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.parsers.ParserConfigurationException;

import org.camunda.bpm.model.bpmn.impl.BpmnModelConstants;
import org.camunda.bpm.model.bpmn.instance.BaseElement;
import org.xml.sax.SAXException;

import de.viadee.bpm.vPAV.BPMNScanner;
import de.viadee.bpm.vPAV.RuntimeConfig;
import de.viadee.bpm.vPAV.config.model.Rule;
import de.viadee.bpm.vPAV.processing.CheckName;
import de.viadee.bpm.vPAV.processing.model.data.BpmnElement;
import de.viadee.bpm.vPAV.processing.model.data.CheckerIssue;
import de.viadee.bpm.vPAV.processing.model.data.CriticalityEnum;

/**
 * Checks, whether a business rule task with dmn implementation is valid
 *
 */
public class DmnTaskChecker extends AbstractElementChecker {

    final private String path;

    public DmnTaskChecker(final Rule rule, String path) {
        super(rule);
        this.path = path;
    }

    /**
     * Check a BusinessRuleTask for a DMN reference
     *
     * @return issues
     */
    @Override
    public Collection<CheckerIssue> check(final BpmnElement element) {
        final Collection<CheckerIssue> issues = new ArrayList<CheckerIssue>();
        final BaseElement bpmnElement = element.getBaseElement();
        final BPMNScanner scan;
        try {
            scan = new BPMNScanner();

            // read attributes from task
            final String implementationAttr = scan.getImplementation(path, bpmnElement.getId());

            final String dmnAttr = bpmnElement.getAttributeValueNs(BpmnModelConstants.CAMUNDA_NS,
                    "decisionRef");
            if (implementationAttr != null) {
                // check if DMN reference is not empty
                if (implementationAttr.equals("camunda:decisionRef")) {
                    if (dmnAttr == null || dmnAttr.trim().length() == 0) {
                        // Error, because no delegateExpression has been configured
                        issues.add(new CheckerIssue(rule.getName(), CriticalityEnum.ERROR,
                                element.getProcessdefinition(), null, bpmnElement.getAttributeValue("id"),
                                bpmnElement.getAttributeValue("name"), null, null, null,
                                "task " + CheckName.checkName(bpmnElement) + " with no dmn reference"));
                    } else {
                        issues.addAll(checkDMNFile(element, dmnAttr, path));
                    }
                }
            }
        } catch (ParserConfigurationException | SAXException | IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return issues;
    }

    /**
     * Check if the referenced DMN in a BusinessRuleTask exists
     *
     * @param element
     * @param dmnName
     * @param path
     * @return issues
     */
    private Collection<CheckerIssue> checkDMNFile(final BpmnElement element, final String dmnName, final String path) {

        final Collection<CheckerIssue> issues = new ArrayList<CheckerIssue>();
        final BaseElement bpmnElement = element.getBaseElement();
        final String dmnPath = dmnName.replaceAll("\\.", "/") + ".dmn";

        // If a dmn path has been found, check the correctness
        URL urlDMN = RuntimeConfig.getInstance().getClassLoader().getResource(dmnPath);

        if (urlDMN == null) {
            // Throws an error, if the class was not found
            issues.add(new CheckerIssue(rule.getName(), CriticalityEnum.WARNING,
                    element.getProcessdefinition(), dmnPath, bpmnElement.getAttributeValue("id"),
                    bpmnElement.getAttributeValue("name"), null, null, null,
                    "dmn file for task " + CheckName.checkName(bpmnElement) + " not found"));
        }
        return issues;
    }
}
