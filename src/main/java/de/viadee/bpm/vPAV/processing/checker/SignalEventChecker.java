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
import java.util.HashMap;
import java.util.Map;

import org.camunda.bpm.model.bpmn.impl.BpmnModelConstants;
import org.camunda.bpm.model.bpmn.instance.BaseElement;
import org.camunda.bpm.model.bpmn.instance.Event;
import org.camunda.bpm.model.bpmn.instance.Signal;
import org.camunda.bpm.model.bpmn.instance.SignalEventDefinition;

import de.viadee.bpm.vPAV.BpmnScanner;
import de.viadee.bpm.vPAV.Messages;
import de.viadee.bpm.vPAV.config.model.Rule;
import de.viadee.bpm.vPAV.output.IssueWriter;
import de.viadee.bpm.vPAV.processing.CheckName;
import de.viadee.bpm.vPAV.processing.model.data.BpmnElement;
import de.viadee.bpm.vPAV.processing.model.data.CheckerIssue;
import de.viadee.bpm.vPAV.processing.model.data.CriticalityEnum;

public class SignalEventChecker extends AbstractElementChecker {
	
	private Map<String, BaseElement> signalNames = new HashMap<>();	

    public SignalEventChecker(Rule rule, BpmnScanner bpmnScanner) {
        super(rule, bpmnScanner);
    }

    @Override
    public Collection<CheckerIssue> check(BpmnElement element) {
        final Collection<CheckerIssue> issues = new ArrayList<CheckerIssue>();
        final BaseElement baseElement = element.getBaseElement();

        if (baseElement.getElementType().getTypeName().equals(BpmnModelConstants.BPMN_ELEMENT_START_EVENT)
                || baseElement.getElementType().getTypeName()
                        .equals(BpmnModelConstants.BPMN_ELEMENT_END_EVENT)
                || baseElement.getElementType().getTypeName()
                        .equals(BpmnModelConstants.BPMN_ELEMENT_INTERMEDIATE_CATCH_EVENT)
                || baseElement.getElementType().getTypeName()
                        .equals(BpmnModelConstants.BPMN_ELEMENT_INTERMEDIATE_THROW_EVENT)
                || baseElement.getElementType().getTypeName()
                        .equals(BpmnModelConstants.BPMN_ELEMENT_BOUNDARY_EVENT)) {

            final Event event = (Event) baseElement;
            final Collection<SignalEventDefinition> signalEventDefinitions = event
                    .getChildElementsByType(SignalEventDefinition.class);
            if (signalEventDefinitions != null) {
                for (SignalEventDefinition eventDef : signalEventDefinitions) {
                    if (eventDef != null) {
                        final Signal signal = eventDef.getSignal();
                        if (signal == null) {
                            issues.addAll(IssueWriter.createIssue(rule, CriticalityEnum.ERROR, element,
                                    String.format(Messages.getString("SignalEventChecker.0"), //$NON-NLS-1$
                                            CheckName.checkName(baseElement))));
                        } else {
                            if (signal.getName() == null || signal.getName().isEmpty()) {
                                issues.addAll(IssueWriter.createIssue(rule, CriticalityEnum.ERROR, element,
                                        String.format(Messages.getString("SignalEventChecker.1"), //$NON-NLS-1$
                                                CheckName.checkName(baseElement))));
                            } else if (baseElement.getElementType().getTypeName()
                                    .equals(BpmnModelConstants.BPMN_ELEMENT_START_EVENT)) {
                                issues.addAll(checkDoubleUsage(element, baseElement, signal));
                            }
                        }
                    }
                }
            }

        }

        return issues;
    }

    /**
     * Check for multiple usage of the same signal name
     * 
     * @param element
     * @param baseElement
     * @param signal
     * @return
     */
    private Collection<CheckerIssue> checkDoubleUsage(final BpmnElement element, final BaseElement baseElement,
            final Signal signal) {

        final Collection<CheckerIssue> issues = new ArrayList<CheckerIssue>();

        if (!addSignal(baseElement, signal.getName())) {
            issues.addAll(IssueWriter.createIssue(rule, CriticalityEnum.ERROR, element,
                    String.format(Messages.getString("SignalEventChecker.2"), //$NON-NLS-1$
                            CheckName.checkName(baseElement),
                            CheckName.checkName(getSignal(signal.getName())))));
        }
        return issues;
    }
    
	public boolean addSignal(final BaseElement baseElement, final String name) {
		if (!signalNames.containsKey(name)) {
			signalNames.put(name, baseElement);
			return true;
		} else {
			return false;
		}
	}

	public BaseElement getSignal(final String name) {
		return signalNames.get(name);
	}

	public void removeElement(final String name) {
		signalNames.remove(name);
	}

}
