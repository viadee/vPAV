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
package de.viadee.bpm.vPAV.output;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;

import de.viadee.bpm.vPAV.processing.model.data.*;
import org.camunda.bpm.model.bpmn.impl.BpmnModelConstants;
import org.camunda.bpm.model.bpmn.instance.BaseElement;
import org.w3c.dom.Element;

import de.viadee.bpm.vPAV.config.model.ElementConvention;
import de.viadee.bpm.vPAV.config.model.Rule;
import de.viadee.bpm.vPAV.processing.model.data.ProcessVariableOperation;
import de.viadee.bpm.vPAV.processing.model.graph.Path;

public class IssueWriter {

    /**
     *
     * @param rule
     *            Rule
     * @param classification
     *            CriticalityEnum
     * @param element
     *            BpmnElement
     * @param message
     *            Errormessage
     * @return Issues
     */
    public static Collection<CheckerIssue> createIssue(final Rule rule, final CriticalityEnum classification,
            final BpmnElement element, final String message) {

        final Collection<CheckerIssue> issues = new ArrayList<CheckerIssue>();

        final BaseElement baseElement = element.getBaseElement();

        issues.add(new CheckerIssue(rule.getName(), rule.getRuleDescription(), classification,
                element.getProcessdefinition(), baseElement.getAttributeValue(BpmnModelConstants.BPMN_ATTRIBUTE_ID),
                baseElement.getAttributeValue(BpmnModelConstants.BPMN_ATTRIBUTE_NAME), message));

        return issues;
    }

    /**
     * @param rule
     *            Rule
     *
     * @param classification
     *            CriticalityEnum
     * @param element
     *            BpmnElement
     * @param message
     *            Errormessage
     * @param description
     *            Description
     * @return Issues
     */
    public static Collection<CheckerIssue> createIssue(final Rule rule, final CriticalityEnum classification,
            final BpmnElement element, final String message, final String description) {

        final Collection<CheckerIssue> issues = new ArrayList<CheckerIssue>();

        final BaseElement baseElement = element.getBaseElement();

        issues.add(new CheckerIssue(rule.getName(), rule.getRuleDescription(), classification,
                element.getProcessdefinition(), baseElement.getAttributeValue(BpmnModelConstants.BPMN_ATTRIBUTE_ID),
                baseElement.getAttributeValue(BpmnModelConstants.BPMN_ATTRIBUTE_NAME), message, description));

        return issues;
    }

    /**
     *
     * @param rule
     *            Rule
     * @param classification
     *            CriticalityEnum
     * @param var
     *            Variable
     * @param paths
     *            List of paths
     * @param anomaly
     *            Anomaly
     * @param message
     *            Errormessage
     * @return Issues
     */
    public static Collection<CheckerIssue> createIssue(final Rule rule, final CriticalityEnum classification,
                                                       final ProcessVariableOperation var, final List<Path> paths, final AnomalyContainer anomaly, final String message) {

        final Collection<CheckerIssue> issues = new ArrayList<CheckerIssue>();

        issues.add(new CheckerIssue(rule.getName(), rule.getRuleDescription(), classification,
                var.getElement().getProcessdefinition(), var.getResourceFilePath(),
                var.getElement().getBaseElement().getId(),
                var.getElement().getBaseElement().getAttributeValue(BpmnModelConstants.BPMN_ATTRIBUTE_NAME),
                var.getName(), anomaly.getAnomaly(), paths, message));

        return issues;
    }

    /**
     *
     * @param rule
     *            Rule
     * @param classification
     *            Criticality Enum
     * @param resourceFile
     *            ResourceFile
     * @param element
     *            BpmnElement
     * @param message
     *            Errormessage
     * @return Issues
     */
    public static Collection<CheckerIssue> createIssue(final Rule rule, final CriticalityEnum classification,
            final String resourceFile, final BpmnElement element, final String message) {

        final Collection<CheckerIssue> issues = new ArrayList<CheckerIssue>();

        final BaseElement baseElement = element.getBaseElement();
        
        issues.add(new CheckerIssue(rule.getName(), rule.getRuleDescription(), classification,
                element.getProcessdefinition(), resourceFile,
                baseElement.getAttributeValue(BpmnModelConstants.BPMN_ATTRIBUTE_ID),
                baseElement.getAttributeValue(BpmnModelConstants.BPMN_ATTRIBUTE_NAME), message, null));

        return issues;
    }

    /**
     *
     * @param rule
     *            Rule
     * @param classification
     *            CriticalityEnum
     * @param element
     *            BpmnElement
     * @param entry
     *            Single entry in Map
     * @param message
     *            Errormessage
     * @return Issues
     */
    public static CheckerIssue createIssue(final Rule rule, final CriticalityEnum classification,
            final BpmnElement element, final Entry<Element, Element> entry, final String message) {

        final BaseElement baseElement = element.getBaseElement();

        return new CheckerIssue(rule.getName(), rule.getRuleDescription(), classification,
                element.getProcessdefinition(), entry.getKey().getAttribute(BpmnModelConstants.BPMN_ATTRIBUTE_ID),
                baseElement.getAttributeValue(BpmnModelConstants.BPMN_ATTRIBUTE_NAME), message);

    }

    /**
     *
     * @param rule
     *            Rule
     * @param classification
     *            CriticalityEnum
     * @param element
     *            BpmnElement
     * @param bpmnFile
     *            BpmnFile
     * @param message
     *            ErrorMessage
     * @return Issue
     */
    public static CheckerIssue createSingleIssue(final Rule rule, final CriticalityEnum classification,
            final BpmnElement element, final String bpmnFile, final String message) {

        final BaseElement baseElement = element.getBaseElement();

        return new CheckerIssue(rule.getName(), classification, bpmnFile,
                baseElement.getAttributeValue(BpmnModelConstants.BPMN_ATTRIBUTE_ID),
                baseElement.getAttributeValue(BpmnModelConstants.BPMN_ATTRIBUTE_NAME), message);
    }

    /**
     *
     * @param rule
     *            Rule
     * @param classification
     *            CriticalityEnum
     * @param element
     *            BpmnElement
     * @param variableResourcePath
     *            variableResourcePath
     * @param varName
     *            variableName
     * @param message
     *            Errormessage
     * @param convention
     *            Convention specified in rule
     * @return Issue
     */
    public static CheckerIssue createSingleIssue(final Rule rule, final CriticalityEnum classification,
            final BpmnElement element, final String variableResourcePath, final String varName, final String message,
            final ElementConvention convention) {

        final BaseElement baseElement = element.getBaseElement();

        return new CheckerIssue(rule.getName(), rule.getRuleDescription(), classification,
                element.getProcessdefinition(), variableResourcePath,
                baseElement.getAttributeValue(BpmnModelConstants.BPMN_ATTRIBUTE_ID),
                baseElement.getAttributeValue(BpmnModelConstants.BPMN_ATTRIBUTE_NAME), varName, message,
                convention.getDescription());

    }

    /**
     *
     * @param rule
     *            Rule
     * @param classification
     *            CriticalityEnum
     * @param classPath
     *            classPath
     * @param element
     *            BpmnElement
     * @param message
     *            Errormessage
     * @return Issue
     */
    public static CheckerIssue createIssueWithClassPath(Rule rule, CriticalityEnum classification, String classPath,
            BpmnElement element, String message) {

        final BaseElement baseElement = element.getBaseElement();

        return new CheckerIssue(rule.getName(), rule.getRuleDescription(), classification,
                element.getProcessdefinition(), classPath,
                baseElement.getAttributeValue(BpmnModelConstants.BPMN_ATTRIBUTE_ID),
                baseElement.getAttributeValue(BpmnModelConstants.BPMN_ATTRIBUTE_NAME), message, null);

    }

    /**
     * 
     * @param rule
     *            Rule
     * @param classification
     *            CriticalityEnum
     * @param element
     *            BpmnElement
     * @param javaReference
     *            javaReference
     * @param message
     *            Errormessage
     * @return Issue
     */
    public static CheckerIssue createIssueWithJavaRef(Rule rule, CriticalityEnum classification, BpmnElement element,
            String javaReference, String message) {
        final BaseElement baseElement = element.getBaseElement();

        return new CheckerIssue(rule.getName(), rule.getRuleDescription(), classification,
                element.getProcessdefinition(), javaReference,
                baseElement.getAttributeValue(BpmnModelConstants.BPMN_ATTRIBUTE_ID),
                baseElement.getAttributeValue(BpmnModelConstants.BPMN_ATTRIBUTE_NAME), message, null);
    }

    /**
     * 
     * @param rule
     *            Rule
     * @param classification
     *            CriticalityEnum
     * @param element
     *            BpmnElement
     * @param beanReference
     *            beanReference
     * @param message
     *            Errormessage
     * @return Issue
     */
    public static CheckerIssue createIssueWithBeanRef(Rule rule, CriticalityEnum classification, BpmnElement element,
            String beanReference, String message) {
        final BaseElement baseElement = element.getBaseElement();

        return new CheckerIssue(rule.getName(), rule.getRuleDescription(), classification,
                element.getProcessdefinition(), beanReference,
                baseElement.getAttributeValue(BpmnModelConstants.BPMN_ATTRIBUTE_ID),
                baseElement.getAttributeValue(BpmnModelConstants.BPMN_ATTRIBUTE_NAME), message, null);
    }

}
