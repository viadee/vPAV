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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.camunda.bpm.model.bpmn.impl.BpmnModelConstants;
import org.camunda.bpm.model.bpmn.instance.BaseElement;
import org.camunda.bpm.model.bpmn.instance.ExclusiveGateway;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import de.viadee.bpm.vPAV.BpmnScanner;
import de.viadee.bpm.vPAV.config.model.ElementConvention;
import de.viadee.bpm.vPAV.config.model.Rule;
import de.viadee.bpm.vPAV.config.model.Setting;
import de.viadee.bpm.vPAV.constants.BpmnConstants;
import de.viadee.bpm.vPAV.output.IssueWriter;
import de.viadee.bpm.vPAV.processing.CheckName;
import de.viadee.bpm.vPAV.processing.ProcessingException;
import de.viadee.bpm.vPAV.processing.model.data.BpmnElement;
import de.viadee.bpm.vPAV.processing.model.data.CheckerIssue;
import de.viadee.bpm.vPAV.processing.model.data.CriticalityEnum;

public class XorConventionChecker extends AbstractElementChecker {

    public XorConventionChecker(final Rule rule, final BpmnScanner bpmnScanner) {
        super(rule, bpmnScanner);
    }

    /**
     * Check if XOR gateways and their outgoing edges adhere to naming conventions
     *
     * @return issues
     */
    @Override
    public Collection<CheckerIssue> check(final BpmnElement element) {

        final Collection<CheckerIssue> issues = new ArrayList<CheckerIssue>();
        final BaseElement bpmnElement = element.getBaseElement();
        final Map<String, Setting> settings = rule.getSettings();

        if (bpmnElement instanceof ExclusiveGateway) {
            String xor_gateway = bpmnScanner.getXorGateWays(bpmnElement.getId());

            if (bpmnScanner.getOutgoing(xor_gateway) > 1) {

                // check default path
                if (settings != null && settings.containsKey(BpmnConstants.REQUIRED_DEFAULT)
                        && settings.get(BpmnConstants.REQUIRED_DEFAULT).getValue().equals("true")
                        && bpmnElement.getAttributeValue(BpmnConstants.DEFAULT) == null) {
                    issues.addAll(IssueWriter.createIssue(rule, CriticalityEnum.WARNING, element, "Xor gateway '"
                            + CheckName.checkName(bpmnElement) + "' has no default path"));
                }

                final ArrayList<ElementConvention> elementConventions = (ArrayList<ElementConvention>) rule
                        .getElementConventions();

                if (elementConventions == null) {
                    throw new ProcessingException(
                            "xor naming convention checker must have one element convention!");
                }

                // TODO: dont use indices
                final String patternString = elementConventions.get(0).getPattern().trim();
                final String taskName = bpmnElement.getAttributeValue(BpmnModelConstants.BPMN_ATTRIBUTE_NAME);
                if (taskName != null && taskName.trim().length() > 0) {
                    final Pattern pattern = Pattern.compile(patternString);
                    final String taskNameClean = taskName.replaceAll("\n", "").replaceAll("\r", "");
                    Matcher matcher = pattern.matcher(taskNameClean);

                    if (!matcher.matches()) {
                        issues.addAll(
                                IssueWriter.createIssue(rule, CriticalityEnum.WARNING, element, "Xor gateway name '"
                                        + CheckName.checkName(bpmnElement) + "' is against the naming convention",
                                        elementConventions.get(0).getDescription()));
                    }
                } else {
                    issues.addAll(
                            IssueWriter.createIssue(rule, CriticalityEnum.WARNING, element,
                                    "Xor gateway name must be specified"));
                }

                // TODO: dont use indices
                final ArrayList<Node> edges = bpmnScanner.getOutgoingEdges(bpmnElement.getId());
                final String patternStringEdge = elementConventions.get(1).getPattern().trim();

                for (int i = 0; i < edges.size(); i++) {
                    Element Task_Element = (Element) edges.get(i);
                    final String edgeName = Task_Element.getAttribute(BpmnModelConstants.BPMN_ATTRIBUTE_NAME);
                    if (edgeName != null && edgeName.trim().length() > 0) {
                        final Pattern pattern = Pattern.compile(patternStringEdge);
                        final String edgeNameClean = edgeName.replaceAll("\n", "").replaceAll("\r", "");
                        Matcher matcher = pattern.matcher(edgeNameClean);
                        if (!matcher.matches()) {
                            issues.addAll(
                                    IssueWriter.createIssue(rule, CriticalityEnum.WARNING, element,
                                            "Outgoing edges of xor gateway '"
                                                    + CheckName.checkName(bpmnElement)
                                                    + "' are against the naming convention",
                                            elementConventions.get(1).getDescription()));
                        }
                    } else {
                        issues.addAll(IssueWriter.createIssue(rule, CriticalityEnum.WARNING, element,
                                "Outgoing edges of xor gateway need to be named"));
                    }
                }
            }
        }
        return issues;
    }
}
