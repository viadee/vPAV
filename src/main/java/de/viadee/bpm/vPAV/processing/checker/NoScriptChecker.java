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
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.camunda.bpm.model.bpmn.instance.BaseElement;
import org.camunda.bpm.model.bpmn.instance.ScriptTask;
import org.camunda.bpm.model.bpmn.instance.SequenceFlow;
import org.camunda.bpm.model.bpmn.instance.SubProcess;
import org.xml.sax.SAXException;

import de.viadee.bpm.vPAV.BPMNScanner;
import de.viadee.bpm.vPAV.config.model.Rule;
import de.viadee.bpm.vPAV.config.model.Setting;
import de.viadee.bpm.vPAV.processing.CheckName;
import de.viadee.bpm.vPAV.processing.model.data.BpmnElement;
import de.viadee.bpm.vPAV.processing.model.data.CheckerIssue;
import de.viadee.bpm.vPAV.processing.model.data.CriticalityEnum;

public class NoScriptChecker extends AbstractElementChecker {

    private final String process = "Process";

    private final String subProcess = "SubProcess";

    private final String scriptTask = "ScriptTask";

    private final String sequenceFlow = "SequenceFlow";

    public NoScriptChecker(final Rule rule, final String path) {
        super(rule, path);
    }

    /**
     * Checks a bpmn model, if there is any script (Script inside a script task - Script as an execution listener -
     * Script as a task listener - Script inside an inputOutput parameter mapping)
     *
     * @return issues
     */
    @Override
    public Collection<CheckerIssue> check(final BpmnElement element) {

        final Collection<CheckerIssue> issues = new ArrayList<CheckerIssue>();
        final BaseElement bpmnElement = element.getBaseElement();
        final BPMNScanner scan;

        try {
            if (!(bpmnElement instanceof Process) && !(bpmnElement instanceof SubProcess)
                    && !bpmnElement.getElementType().getInstanceType().getSimpleName().equals(process)
                    && !bpmnElement.getElementType().getInstanceType().getSimpleName().equals(subProcess)) {
                scan = new BPMNScanner(path);
                Map<String, Setting> settings = rule.getSettings();

                // Check all Elements with camunda:script tag
                ArrayList<String> scriptTypes = scan.getScriptTypes(bpmnElement.getAttributeValue("id"));
                if (scriptTypes != null && !scriptTypes.isEmpty()) {
                    if (!settings.containsKey(bpmnElement.getElementType().getInstanceType().getSimpleName())) {
                        for (String place : scriptTypes)
                            issues.add(new CheckerIssue(rule.getName(), CriticalityEnum.ERROR,
                                    element.getProcessdefinition(), null, bpmnElement.getAttributeValue("id"),
                                    bpmnElement.getAttributeValue("name"), null, null, null,
                                    "task '" + CheckName.checkName(bpmnElement) + "' with '"
                                            + place + "' script"));
                    } else {
                        ArrayList<String> allowedPlaces = settings
                                .get(bpmnElement.getElementType().getInstanceType().getSimpleName()).getScriptPlaces();
                        if (!allowedPlaces.isEmpty())
                            for (String scriptType : scriptTypes)
                                if (!allowedPlaces.contains(scriptType))
                                    issues.add(new CheckerIssue(rule.getName(), CriticalityEnum.ERROR,
                                            element.getProcessdefinition(), null, bpmnElement.getAttributeValue("id"),
                                            bpmnElement.getAttributeValue("name"), null, null, null,
                                            "task '" + CheckName.checkName(bpmnElement) + "' with '"
                                                    + scriptType + "' script"));

                    }
                }

                // ScriptTask
                if (bpmnElement instanceof ScriptTask && !settings.containsKey(scriptTask)) {
                    issues.add(new CheckerIssue(rule.getName(), CriticalityEnum.ERROR,
                            element.getProcessdefinition(), null, bpmnElement.getAttributeValue("id"),
                            bpmnElement.getAttributeValue("name"), null, null, null,
                            "ScriptTask '" + CheckName.checkName(bpmnElement) + "' not allowed"));
                }

                // Check SequenceFlow on script in conditionExpression
                if (bpmnElement instanceof SequenceFlow) {
                    boolean scriptCondExp = scan.hasScriptInCondExp(bpmnElement.getAttributeValue("id"));
                    if (settings.containsKey(sequenceFlow)) {
                        ArrayList<String> allowedPlaces = settings.get(sequenceFlow).getScriptPlaces();
                        if (!allowedPlaces.isEmpty())
                            if (!allowedPlaces.contains("conditionExpression") && scriptCondExp)
                                issues.add(new CheckerIssue(rule.getName(), CriticalityEnum.ERROR,
                                        element.getProcessdefinition(), null, bpmnElement.getAttributeValue("id"),
                                        bpmnElement.getAttributeValue("name"), null, null, null,
                                        "SequenceFlow '" + CheckName.checkName(bpmnElement)
                                                + "' with script in condition Expression"));
                    } else {
                        if (scriptCondExp) {
                            issues.add(new CheckerIssue(rule.getName(), CriticalityEnum.ERROR,
                                    element.getProcessdefinition(), null, bpmnElement.getAttributeValue("id"),
                                    bpmnElement.getAttributeValue("name"), null, null, null,
                                    "SequenceFlow '" + CheckName.checkName(bpmnElement)
                                            + "' with script in condition Expression"));
                        }
                    }
                }
            }
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }

        return issues;
    }
}
