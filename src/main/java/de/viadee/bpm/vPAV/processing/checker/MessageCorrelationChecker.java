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
package de.viadee.bpm.vPAV.processing.checker;

import de.viadee.bpm.vPAV.BpmnScanner;
import de.viadee.bpm.vPAV.Messages;
import de.viadee.bpm.vPAV.config.model.Rule;
import de.viadee.bpm.vPAV.output.IssueWriter;
import de.viadee.bpm.vPAV.processing.EntryPoint;
import de.viadee.bpm.vPAV.processing.ProcessVariablesScanner;
import de.viadee.bpm.vPAV.processing.code.flow.BpmnElement;
import de.viadee.bpm.vPAV.processing.model.data.CheckerIssue;
import de.viadee.bpm.vPAV.processing.model.data.CriticalityEnum;
import org.camunda.bpm.model.bpmn.impl.BpmnModelConstants;
import org.camunda.bpm.model.bpmn.instance.BaseElement;
import org.camunda.bpm.model.bpmn.instance.MessageEventDefinition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
/**
 * Class MessageCorrelationChecker
 *
 * Checks a bpmn model, if message references can be resolved for message
 * events/receive tasks.
 *
 */
public class MessageCorrelationChecker extends AbstractElementChecker {

	private ProcessVariablesScanner scanner;

	MessageCorrelationChecker(final Rule rule, final BpmnScanner bpmnScanner, final ProcessVariablesScanner scanner) {
		super(rule, bpmnScanner);
		this.scanner = scanner;
	}

	@Override
	public Collection<CheckerIssue> check(final BpmnElement element) {
		final Collection<CheckerIssue> issues = new ArrayList<>();

		final BaseElement baseElement = element.getBaseElement();

		if (baseElement.getElementType().getTypeName().equals(BpmnModelConstants.BPMN_ELEMENT_START_EVENT)) {

			final Collection<MessageEventDefinition> messageEventDefinition = baseElement
					.getChildElementsByType(MessageEventDefinition.class);
			if (messageEventDefinition != null && messageEventDefinition.size() > 0) {
				retrieveMessage(element, issues, baseElement, scanner.getEntryPoints());
			}
		}
		if (baseElement.getElementType().getTypeName().equals(BpmnModelConstants.BPMN_ELEMENT_END_EVENT)
				|| baseElement.getElementType().getTypeName()
						.equals(BpmnModelConstants.BPMN_ELEMENT_INTERMEDIATE_CATCH_EVENT)
				|| baseElement.getElementType().getTypeName()
						.equals(BpmnModelConstants.BPMN_ELEMENT_INTERMEDIATE_THROW_EVENT)
				|| baseElement.getElementType().getTypeName().equals(BpmnModelConstants.BPMN_ELEMENT_BOUNDARY_EVENT)) {

			final Collection<MessageEventDefinition> messageEventDefinition = baseElement
					.getChildElementsByType(MessageEventDefinition.class);
			if (messageEventDefinition != null && messageEventDefinition.size() > 0) {
				retrieveMessage(element, issues, baseElement, scanner.getIntermediateEntryPoints());
			}
		}
		if (baseElement.getElementType().getTypeName().equals(BpmnModelConstants.BPMN_ELEMENT_RECEIVE_TASK)) {
			retrieveMessage(element, issues, baseElement, scanner.getIntermediateEntryPoints());
		}

		return issues;
	}

    /**
     * Retrieves the message from the bpmn element and checks if it can be resolved
     * @param element
     *            Current BpmnElement
     * @param issues
     *            List of issues
     * @param baseElement
     *            BaseElement of current BpmnElement
     * @param entryPoints
     *            List of entryPoints
     */
	private void retrieveMessage(final BpmnElement element, final Collection<CheckerIssue> issues,
			final BaseElement baseElement, final List<EntryPoint> entryPoints) {
		final ArrayList<String> messageRefs = bpmnScanner.getMessageRefs(baseElement.getId());
		String messageName = "";
		if (messageRefs.size() == 1) {
			messageName = bpmnScanner.getMessageName(messageRefs.get(0));
		}

		for (EntryPoint ep : entryPoints) {
			if (!ep.getMessageName().equals(messageName)) {
				issues.addAll(IssueWriter.createIssue(rule, CriticalityEnum.ERROR, element,
						String.format(Messages.getString("MessageCorrelationChecker.1"), //$NON-NLS-1$
								messageName)));
			}
		}
	}

}
