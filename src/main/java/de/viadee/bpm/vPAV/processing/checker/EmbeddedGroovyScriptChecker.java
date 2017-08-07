/**
 * Copyright � 2017, viadee Unternehmensberatung GmbH All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met: 1. Redistributions of source code must retain the above copyright notice, this list of
 * conditions and the following disclaimer. 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation and/or other materials provided with the
 * distribution. 3. All advertising materials mentioning features or use of this software must display the following
 * acknowledgement: This product includes software developed by the viadee Unternehmensberatung GmbH. 4. Neither the
 * name of the viadee Unternehmensberatung GmbH nor the names of its contributors may be used to endorse or promote
 * products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY <COPYRIGHT HOLDER> ''AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO
 * EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package de.viadee.bpm.vPAV.processing.checker;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.camunda.bpm.model.bpmn.instance.BaseElement;
import org.camunda.bpm.model.bpmn.instance.ExtensionElements;
import org.camunda.bpm.model.bpmn.instance.Script;
import org.camunda.bpm.model.bpmn.instance.ScriptTask;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaExecutionListener;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaScript;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaTaskListener;
import org.codehaus.groovy.control.CompilationFailedException;

import de.viadee.bpm.vPAV.config.model.Rule;
import de.viadee.bpm.vPAV.processing.model.data.BpmnElement;
import de.viadee.bpm.vPAV.processing.model.data.CheckerIssue;
import de.viadee.bpm.vPAV.processing.model.data.CriticalityEnum;
import groovy.lang.GroovyShell;

/**
 * Class EmbeddedGroovyScriptChecker
 *
 * Checks a bpmn model, if embedded groovy script references have been set correctly.
 *
 */
public class EmbeddedGroovyScriptChecker extends AbstractElementChecker {

    public EmbeddedGroovyScriptChecker(final Rule rule) {
        super(rule);
    }

    @Override
    public Collection<CheckerIssue> check(final BpmnElement element) {

        final Collection<CheckerIssue> issues = new ArrayList<CheckerIssue>();

        final BaseElement baseElement = element.getBaseElement();
        issues.addAll(checkScriptTask(element.getProcessdefinition(), baseElement));

        final ExtensionElements extensionElements = baseElement.getExtensionElements();
        if (extensionElements != null) {
            issues.addAll(
                    checkExecutionListener(element.getProcessdefinition(), baseElement, extensionElements));
            issues.addAll(
                    checkTaskListener(element.getProcessdefinition(), baseElement, extensionElements));
        }

        return issues;
    }

    /**
     * check script tasks
     * 
     * @param bpmnFile
     * @param baseElement
     * @return issues
     */
    private Collection<CheckerIssue> checkScriptTask(final String bpmnFile,
            final BaseElement baseElement) {

        final Collection<CheckerIssue> issues = new ArrayList<CheckerIssue>();

        if (baseElement instanceof ScriptTask) {
            final ScriptTask scriptTask = (ScriptTask) baseElement;
            final Script script = scriptTask.getScript();
            if (script != null && scriptTask.getCamundaResource() == null) {
                final CheckerIssue issueEmptyScript = checkEmptyScriptContent(bpmnFile, baseElement,
                        scriptTask.getScriptFormat(), script.getTextContent());
                if (issueEmptyScript != null)
                    issues.add(issueEmptyScript);
                final CheckerIssue issueEmptyFormat = checkEmptyScriptFormat(bpmnFile, baseElement,
                        scriptTask.getScriptFormat(), script.getTextContent());
                if (issueEmptyFormat != null)
                    issues.add(issueEmptyFormat);
                final CheckerIssue issueInvalidScript = checkInvalidScriptContent(bpmnFile, baseElement,
                        scriptTask.getScriptFormat(), script.getTextContent());
                if (issueInvalidScript != null)
                    issues.add(issueInvalidScript);
            } else {
                if (script == null) {
                    issues.add(new CheckerIssue(rule.getName(), CriticalityEnum.ERROR, bpmnFile, null,
                            baseElement.getId(), baseElement.getAttributeValue("name"), null, null, null,
                            "there is an empty script reference"));
                }
            }
        }

        return issues;
    }

    /**
     * check execution listeners
     * 
     * @param bpmnFile
     * @param baseElement
     * @param extensionElements
     * @return issues
     */
    private Collection<CheckerIssue> checkExecutionListener(final String bpmnFile,
            final BaseElement baseElement, final ExtensionElements extensionElements) {

        final Collection<CheckerIssue> issues = new ArrayList<CheckerIssue>();

        final List<CamundaExecutionListener> listenerList = extensionElements.getElementsQuery()
                .filterByType(CamundaExecutionListener.class).list();
        for (final CamundaExecutionListener listener : listenerList) {
            final CamundaScript script = listener.getCamundaScript();
            if (script != null && script.getCamundaResource() == null) {
                final CheckerIssue issueEmptyScript = checkEmptyScriptContent(bpmnFile, baseElement,
                        script.getCamundaScriptFormat(), script.getTextContent());
                if (issueEmptyScript != null)
                    issues.add(issueEmptyScript);
                final CheckerIssue issueEmptyFormat = checkEmptyScriptFormat(bpmnFile, baseElement,
                        script.getCamundaScriptFormat(), script.getTextContent());
                if (issueEmptyFormat != null)
                    issues.add(issueEmptyFormat);
                final CheckerIssue issueInvalidScript = checkInvalidScriptContent(bpmnFile, baseElement,
                        script.getCamundaScriptFormat(), script.getTextContent());
                if (issueInvalidScript != null)
                    issues.add(issueInvalidScript);
            }
        }
        return issues;
    }

    /**
     * check task listeners
     *
     * @param bpmnFile
     * @param baseElement
     * @param extensionElements
     * @return issues
     */
    private Collection<CheckerIssue> checkTaskListener(final String bpmnFile,
            final BaseElement baseElement, final ExtensionElements extensionElements) {

        final Collection<CheckerIssue> issues = new ArrayList<CheckerIssue>();

        final List<CamundaTaskListener> listenerList = extensionElements.getElementsQuery()
                .filterByType(CamundaTaskListener.class).list();
        for (final CamundaTaskListener listener : listenerList) {
            final CamundaScript script = listener.getCamundaScript();
            if (script != null && script.getCamundaResource() == null) {
                final CheckerIssue issueEmptyScript = checkEmptyScriptContent(bpmnFile, baseElement,
                        script.getCamundaScriptFormat(), script.getTextContent());
                if (issueEmptyScript != null)
                    issues.add(issueEmptyScript);
                final CheckerIssue issueEmptyFormat = checkEmptyScriptFormat(bpmnFile, baseElement,
                        script.getCamundaScriptFormat(), script.getTextContent());
                if (issueEmptyFormat != null)
                    issues.add(issueEmptyFormat);
                final CheckerIssue issueInvalidScript = checkInvalidScriptContent(bpmnFile, baseElement,
                        script.getCamundaScriptFormat(), script.getTextContent());
                if (issueInvalidScript != null)
                    issues.add(issueInvalidScript);
            }
        }
        return issues;
    }

    /**
     * check, if groovy code is valid
     * 
     * @param bpmnFile
     * @param baseElement
     * @param scriptText
     * @return issue
     */
    private CheckerIssue parseGroovyCode(final String bpmnFile, final BaseElement baseElement,
            final String scriptText) {
        final GroovyShell shell = new GroovyShell();
        try {
            shell.evaluate(scriptText);
        } catch (final CompilationFailedException ex) {
            return new CheckerIssue(rule.getName(), CriticalityEnum.ERROR, bpmnFile, null,
                    baseElement.getId(), baseElement.getAttributeValue("name"), null, null, null,
                    ex.getMessage());
        }
        return null;
    }

    /**
     * check empty script content
     * 
     * @param bpmnFile
     * @param baseElement
     * @param scriptFormat
     * @param script
     * @return issue
     */
    private CheckerIssue checkEmptyScriptContent(final String bpmnFile, final BaseElement baseElement,
            final String scriptFormat, final String script) {

        if (scriptFormat != null && (script == null || script.isEmpty())) {
            return new CheckerIssue(rule.getName(), CriticalityEnum.ERROR, bpmnFile, null,
                    baseElement.getId(), baseElement.getAttributeValue("name"), null, null, null,
                    "there is no script content for given script format");
        }
        return null;
    }

    /**
     * check empty script format
     * 
     * @param bpmnFile
     * @param baseElement
     * @param scriptFormat
     * @param script
     * @return issue
     */
    private CheckerIssue checkEmptyScriptFormat(final String bpmnFile, final BaseElement baseElement,
            final String scriptFormat, final String script) {

        if (scriptFormat == null && script != null) {
            return new CheckerIssue(rule.getName(), CriticalityEnum.ERROR, bpmnFile, null,
                    baseElement.getId(), baseElement.getAttributeValue("name"), null, null, null,
                    "there is no script format for given script");
        }
        return null;
    }

    /**
     * check groovy syntax
     * 
     * @param bpmnFile
     * @param baseElement
     * @param scriptFormat
     * @param script
     * @return issue
     */
    private CheckerIssue checkInvalidScriptContent(final String bpmnFile,
            final BaseElement baseElement, final String scriptFormat, final String script) {

        if (scriptFormat != null && scriptFormat.toLowerCase().equals("groovy") && script != null) {
            return parseGroovyCode(bpmnFile, baseElement, script);
        }
        return null;
    }
}
