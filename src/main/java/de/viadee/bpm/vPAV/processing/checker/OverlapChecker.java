/**
 * Copyright © 2017, viadee Unternehmensberatung GmbH
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.camunda.bpm.model.bpmn.instance.BaseElement;
import org.camunda.bpm.model.bpmn.instance.SequenceFlow;

import de.viadee.bpm.vPAV.AbstractRunner;
import de.viadee.bpm.vPAV.BpmnScanner;
import de.viadee.bpm.vPAV.config.model.Rule;
import de.viadee.bpm.vPAV.output.IssueWriter;
import de.viadee.bpm.vPAV.processing.CheckName;
import de.viadee.bpm.vPAV.processing.model.data.BpmnElement;
import de.viadee.bpm.vPAV.processing.model.data.CheckerIssue;
import de.viadee.bpm.vPAV.processing.model.data.CriticalityEnum;

public class OverlapChecker extends AbstractElementChecker {

    public OverlapChecker(final Rule rule, final BpmnScanner bpmnScanner) {
        super(rule, bpmnScanner);
    }

    /**
     * Check for redundant edges between common elements (double or more flows instead of one)
     *
     * @return issues
     */

    @Override
    public Collection<CheckerIssue> check(BpmnElement element) {
        final Collection<CheckerIssue> issues = new ArrayList<CheckerIssue>();
        final BaseElement bpmnElement = element.getBaseElement();

        if (bpmnElement instanceof SequenceFlow) {

            final ArrayList<String> sequenceFlowDef = bpmnScanner.getSequenceFlowDef(bpmnElement.getId());

            if (AbstractRunner.getSequenceFlowList().isEmpty()) {
                AbstractRunner.addToSequenceFlowList(bpmnElement.getId(), sequenceFlowDef);
            }

            for (Map.Entry<String, ArrayList<String>> entry : AbstractRunner.getSequenceFlowList().entrySet()) {
                // Check whether targetRef & sourceRef of current item exist in global list
                if (sequenceFlowDef.equals(entry.getValue()) && !bpmnElement.getId().equals(entry.getKey())) {
                    issues.addAll(IssueWriter.createIssue(rule, CriticalityEnum.ERROR, element,
                            "Multiple SequenceFlows detected. Delete " + CheckName.checkName(bpmnElement)
                                    + " with identical source and target"));
                    return issues;
                }
            }

            if (!AbstractRunner.getSequenceFlowList().containsKey(bpmnElement.getId())) {
                AbstractRunner.addToSequenceFlowList(bpmnElement.getId(), sequenceFlowDef);
            }

        }

        return issues;
    }

}