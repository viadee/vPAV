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
import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;

import org.camunda.bpm.model.bpmn.instance.BaseElement;
import org.camunda.bpm.model.bpmn.instance.ExclusiveGateway;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import de.viadee.bpm.vPAV.BPMNScanner;
import de.viadee.bpm.vPAV.config.model.ElementConvention;
import de.viadee.bpm.vPAV.config.model.Rule;
import de.viadee.bpm.vPAV.processing.CheckName;
import de.viadee.bpm.vPAV.processing.ProcessingException;
import de.viadee.bpm.vPAV.processing.model.data.BpmnElement;
import de.viadee.bpm.vPAV.processing.model.data.CheckerIssue;
import de.viadee.bpm.vPAV.processing.model.data.CriticalityEnum;

public class XorNamingConventionChecker extends AbstractElementChecker {

    final private String path;

    public XorNamingConventionChecker(final Rule rule, final String path) {
        super(rule);
        this.path = path;
    }

    @Override
    public Collection<CheckerIssue> check(final BpmnElement element) {

        final Collection<CheckerIssue> issues = new ArrayList<CheckerIssue>();
        final BaseElement bpmnElement = element.getBaseElement();
        final BPMNScanner scan;

        if (bpmnElement instanceof ExclusiveGateway) {

            try {
                scan = new BPMNScanner();

                String xor_gateway = scan.getXorGateWays(path, bpmnElement.getId());

                if (scan.getOutgoing(path, xor_gateway) > 1) {

                    final ArrayList<ElementConvention> elementConventions = (ArrayList<ElementConvention>) rule
                            .getElementConventions();

                    if (elementConventions == null) {
                        throw new ProcessingException(
                                "xor naming convention checker must have one element convention!");
                    }

                    final String patternString = elementConventions.get(0).getPattern().trim();
                    final String taskName = bpmnElement.getAttributeValue("name");
                    if (taskName != null && taskName.trim().length() > 0) {
                        final Pattern pattern = Pattern.compile(patternString);
                        Matcher matcher = pattern.matcher(taskName);

                        if (!matcher.matches()) {
                            issues.add(new CheckerIssue(rule.getName(), CriticalityEnum.WARNING,
                                    element.getProcessdefinition(), null, bpmnElement.getId(),
                                    bpmnElement.getAttributeValue("name"), null, null, null, "xor gateway name '"
                                            + CheckName.checkName(bpmnElement) + "' is against the naming convention"));
                        }
                    } else {
                        issues.add(
                                new CheckerIssue(rule.getName(), CriticalityEnum.ERROR, element.getProcessdefinition(),
                                        null, bpmnElement.getId(), bpmnElement.getAttributeValue("name"), null, null,
                                        null, "xor gateway name must be specified"));
                    }

                    final ArrayList<Node> edges = scan.getOutgoingEdges(path, bpmnElement.getId());
                    final String patternString2 = elementConventions.get(1).getPattern().trim();

                    for (int i = 0; i < edges.size(); i++) {
                        Element Task_Element = (Element) edges.get(i);
                        final String edgeName = Task_Element.getAttribute("name");
                        if (edgeName != null && edgeName.trim().length() > 0) {
                            final Pattern pattern = Pattern.compile(patternString2);
                            Matcher matcher = pattern.matcher(edgeName);
                            if (!matcher.matches()) {
                                issues.add(new CheckerIssue(rule.getName(), CriticalityEnum.WARNING,
                                        element.getProcessdefinition(), null, Task_Element.getAttribute("id"),
                                        Task_Element.getAttribute("name"), null, null, null,
                                        "outgoing edges of xor gateway '"
                                                + CheckName.checkName(bpmnElement)
                                                + "' are against the naming convention"));
                            }
                        } else {
                            issues.add(
                                    new CheckerIssue(rule.getName(), CriticalityEnum.WARNING,
                                            element.getProcessdefinition(), null, Task_Element.getAttribute("id"),
                                            Task_Element.getAttribute("name"), null, null, null,
                                            "outgoing edges of xor gateway need to be named"));
                        }
                    }

                }

            } catch (ParserConfigurationException | SAXException | IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }
        return issues;
    }

}
