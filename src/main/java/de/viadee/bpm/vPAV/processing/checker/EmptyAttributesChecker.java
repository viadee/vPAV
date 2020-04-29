/*
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

import de.viadee.bpm.vPAV.BpmnScanner;
import de.viadee.bpm.vPAV.Messages;
import de.viadee.bpm.vPAV.config.model.Rule;
import de.viadee.bpm.vPAV.output.IssueWriter;
import de.viadee.bpm.vPAV.processing.CheckName;
import de.viadee.bpm.vPAV.processing.code.flow.BpmnElement;
import de.viadee.bpm.vPAV.processing.model.data.CheckerIssue;
import de.viadee.bpm.vPAV.processing.model.data.CriticalityEnum;
import org.camunda.bpm.model.bpmn.impl.BpmnModelConstants;
import org.camunda.bpm.model.bpmn.instance.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Class EmptyAttributesChecker
 * <p>
 * Checks a bpmn model, if attributes without value are defined.
 */
public class EmptyAttributesChecker extends AbstractElementChecker {

    public EmptyAttributesChecker(final Rule rule) {
        super(rule);
    }

    @Override
    public Collection<CheckerIssue> check(final BpmnElement element) {
        final Collection<CheckerIssue> issues = new ArrayList<>();
        final BaseElement bpmnElement = element.getBaseElement();

        // read attributes from task
        if ((bpmnElement instanceof ServiceTask || bpmnElement instanceof BusinessRuleTask
                || bpmnElement instanceof SendTask)) {
            HashMap<String, String> attributes = getImplementationAttribute(bpmnElement);
            for (Map.Entry<String, String> entry : attributes.entrySet()) {
                if (entry.getValue() == null || entry.getValue().isEmpty()) {
                    issues.addAll(generateIssue(rule, element, bpmnElement, entry));
                }
            }
        }

        // Check event definitions
        Collection<EventDefinition> eventDefinitions = BpmnScanner.getEventDefinitions(bpmnElement);
        if (eventDefinitions != null) {
            for (EventDefinition ed : eventDefinitions) {
                HashMap<String, String> attributes = getImplementationAttribute(ed);
                for (Map.Entry<String, String> entry : attributes.entrySet()) {
                    if (entry.getValue() == null || entry.getValue().isEmpty()) {
                        issues.addAll(generateIssue(rule, element, ed, entry));
                    }
                }
            }
        }

        if (bpmnElement instanceof TimerEventDefinition) {
            TimerEventDefinition timer = (TimerEventDefinition) bpmnElement;
            checkTimeEventDefinition(timer, element, issues);
        }
        return issues;
    }

    private void checkTimeEventDefinition(TimerEventDefinition timer, BpmnElement element,
            Collection<CheckerIssue> issues) {
        if (timer.getTimeDate() != null
                && timer.getTimeDate().getAttributeValueNs("http://www.w3.org/2001/XMLSchema-instance", "type")
                == null) {
            issues.addAll(IssueWriter.createIssue(rule, CriticalityEnum.ERROR, element, timer.getId(),
                    Messages.getString("EmptyAttributesChecker.5")));
        }
        if (timer.getTimeDuration() != null &&
                timer.getTimeDuration().getAttributeValueNs("http://www.w3.org/2001/XMLSchema-instance", "type")
                        == null) {
            issues.addAll(IssueWriter.createIssue(rule, CriticalityEnum.ERROR, element, timer.getId(),
                    Messages.getString("EmptyAttributesChecker.6")));

        }
        if (timer.getTimeCycle() != null
                && timer.getTimeCycle().getAttributeValueNs("http://www.w3.org/2001/XMLSchema-instance", "type")
                == null) {
            issues.addAll(IssueWriter.createIssue(rule, CriticalityEnum.ERROR, element, timer.getId(),
                    Messages.getString("EmptyAttributesChecker.7")));
        }
    }

    private Collection<CheckerIssue> generateIssue(Rule rule, BpmnElement element, BaseElement bpmnElement,
            Map.Entry<String, String> entry) {

        String message = "";
        switch (entry.getKey()) {
            case BpmnModelConstants.CAMUNDA_ATTRIBUTE_DECISION_REF:
                message = Messages.getString("EmptyAttributesChecker.0");
                break;
            case BpmnModelConstants.CAMUNDA_ATTRIBUTE_CLASS:
                message = Messages.getString("EmptyAttributesChecker.1");
                break;
            case BpmnModelConstants.CAMUNDA_ATTRIBUTE_DELEGATE_EXPRESSION:
                message = Messages.getString("EmptyAttributesChecker.2");
                break;
            case BpmnModelConstants.CAMUNDA_ATTRIBUTE_EXPRESSION:
                message = Messages.getString("EmptyAttributesChecker.3");
                break;
            case BpmnModelConstants.BPMN_ATTRIBUTE_IMPLEMENTATION:
                message = Messages.getString("EmptyAttributesChecker.4");
                break;
        }
        return IssueWriter.createIssue(rule, CriticalityEnum.ERROR, element,
                String.format(message, //$NON-NLS-1$
                        CheckName.checkName(bpmnElement)));
    }

    /**
     * Return the Implementation of an specific element (sendTask, ServiceTask or
     * BusinessRuleTask)
     *
     * @param element Element
     * @return return_implementation contains implementation
     */
    public HashMap<String, String> getImplementationAttribute(BaseElement element) {
        HashMap<String, String> attributes = new HashMap<>();

        // Check which implementation it is
        if (element.getDomElement()
                .hasAttribute(BpmnModelConstants.CAMUNDA_NS, BpmnModelConstants.CAMUNDA_ATTRIBUTE_CLASS)) {
            attributes.put(BpmnModelConstants.CAMUNDA_ATTRIBUTE_CLASS,
                    element.getAttributeValueNs(BpmnModelConstants.CAMUNDA_NS,
                            BpmnModelConstants.CAMUNDA_ATTRIBUTE_CLASS))
            ;
        }
        if (element.getDomElement().hasAttribute(BpmnModelConstants.CAMUNDA_NS,
                BpmnModelConstants.CAMUNDA_ATTRIBUTE_DELEGATE_EXPRESSION)) {
            attributes.put(BpmnModelConstants.CAMUNDA_ATTRIBUTE_DELEGATE_EXPRESSION,
                    element.getAttributeValueNs(BpmnModelConstants.CAMUNDA_NS,
                            BpmnModelConstants.CAMUNDA_ATTRIBUTE_DELEGATE_EXPRESSION));
        }
        if (element.getDomElement()
                .hasAttribute(BpmnModelConstants.CAMUNDA_NS, BpmnModelConstants.CAMUNDA_ATTRIBUTE_EXPRESSION)) {
            attributes.put(BpmnModelConstants.CAMUNDA_ATTRIBUTE_EXPRESSION,
                    element.getAttributeValueNs(BpmnModelConstants.CAMUNDA_NS,
                            BpmnModelConstants.CAMUNDA_ATTRIBUTE_EXPRESSION));
        }
        if (element.getDomElement().hasAttribute(BpmnModelConstants.CAMUNDA_NS,
                BpmnModelConstants.CAMUNDA_ATTRIBUTE_DECISION_REF)) {
            attributes.put(BpmnModelConstants.CAMUNDA_ATTRIBUTE_DECISION_REF,
                    element.getAttributeValueNs(BpmnModelConstants.CAMUNDA_NS,
                            BpmnModelConstants.CAMUNDA_ATTRIBUTE_DECISION_REF));
        }
        if (element.getDomElement()
                .hasAttribute(BpmnModelConstants.CAMUNDA_NS, BpmnModelConstants.CAMUNDA_ATTRIBUTE_TYPE)) {
            attributes.put(BpmnModelConstants.CAMUNDA_ATTRIBUTE_TYPE,
                    element.getAttributeValueNs(BpmnModelConstants.CAMUNDA_NS,
                            BpmnModelConstants.CAMUNDA_ATTRIBUTE_TYPE));
        }
        if (element.getDomElement()
                .hasAttribute(BpmnModelConstants.CAMUNDA_NS, BpmnModelConstants.BPMN_ATTRIBUTE_IMPLEMENTATION)) {
            attributes.put(BpmnModelConstants.BPMN_ATTRIBUTE_IMPLEMENTATION,
                    element.getAttributeValueNs(BpmnModelConstants.CAMUNDA_NS,
                            BpmnModelConstants.BPMN_ATTRIBUTE_IMPLEMENTATION));
        }

        return attributes;
    }
}
