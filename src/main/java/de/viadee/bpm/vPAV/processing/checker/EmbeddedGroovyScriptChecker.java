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
import java.util.List;

import org.camunda.bpm.model.bpmn.instance.BaseElement;
import org.camunda.bpm.model.bpmn.instance.ExtensionElements;
import org.camunda.bpm.model.bpmn.instance.Script;
import org.camunda.bpm.model.bpmn.instance.ScriptTask;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaExecutionListener;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaScript;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaTaskListener;
import org.codehaus.groovy.control.CompilationFailedException;

import de.viadee.bpm.vPAV.BpmnScanner;
import de.viadee.bpm.vPAV.Messages;
import de.viadee.bpm.vPAV.config.model.Rule;
import de.viadee.bpm.vPAV.constants.ConfigConstants;
import de.viadee.bpm.vPAV.output.IssueWriter;
import de.viadee.bpm.vPAV.processing.model.data.BpmnElement;
import de.viadee.bpm.vPAV.processing.model.data.CheckerIssue;
import de.viadee.bpm.vPAV.processing.model.data.CriticalityEnum;
import groovy.lang.GroovyShell;
import groovy.lang.MissingPropertyException;

/**
 * Class EmbeddedGroovyScriptChecker
 *
 * Checks a bpmn model, if embedded groovy script references have been set correctly.
 *
 */
public class EmbeddedGroovyScriptChecker extends AbstractElementChecker {

    public EmbeddedGroovyScriptChecker(final Rule rule, final BpmnScanner bpmnScanner) {
        super(rule, bpmnScanner);
    }

    /**
     * Check for GroovyScript in a ScriptTask And checks for GroovyScript in ExtensionElements
     *
     * @return issues
     */
    @Override
    public Collection<CheckerIssue> check(final BpmnElement element) {

        final Collection<CheckerIssue> issues = new ArrayList<CheckerIssue>();

        final BaseElement baseElement = element.getBaseElement();
        issues.addAll(checkScriptTask(element.getProcessdefinition(), element));

        final ExtensionElements extensionElements = baseElement.getExtensionElements();
        if (extensionElements != null) {
            issues.addAll(
                    checkExecutionListener(element.getProcessdefinition(), element, baseElement, extensionElements));
            issues.addAll(
                    checkTaskListener(element.getProcessdefinition(), element, baseElement, extensionElements));
        }

        return issues;
    }

    /**
     * Check script tasks
     *
     * @param bpmnFile
     * @param baseElement
     * @return issues
     */
    private Collection<CheckerIssue> checkScriptTask(final String bpmnFile,
            final BpmnElement element) {

        final Collection<CheckerIssue> issues = new ArrayList<CheckerIssue>();
        final BaseElement baseElement = element.getBaseElement();

        if (baseElement instanceof ScriptTask) {
            final ScriptTask scriptTask = (ScriptTask) baseElement;
            final Script script = scriptTask.getScript();
            if (script != null && scriptTask.getCamundaResource() == null) {
                final CheckerIssue issueEmptyScript = checkEmptyScriptContent(bpmnFile, element, baseElement,
                        scriptTask.getScriptFormat(), script.getTextContent());
                if (issueEmptyScript != null)
                    issues.add(issueEmptyScript);
                final CheckerIssue issueEmptyFormat = checkEmptyScriptFormat(bpmnFile, element, baseElement,
                        scriptTask.getScriptFormat(), script.getTextContent());
                if (issueEmptyFormat != null)
                    issues.add(issueEmptyFormat);
                final CheckerIssue issueInvalidScript = checkInvalidScriptContent(bpmnFile, element, baseElement,
                        scriptTask.getScriptFormat(), script.getTextContent());
                if (issueInvalidScript != null)
                    issues.add(issueInvalidScript);
            } else {
                if (scriptTask.getCamundaResource() == null) {
                    issues.addAll(IssueWriter.createIssue(rule, CriticalityEnum.ERROR, element,
                            Messages.getString("EmbeddedGroovyScriptChecker.0"))); //$NON-NLS-1$
                }
            }
        }

        return issues;
    }

    /**
     * Check execution listeners
     *
     * @param bpmnFile
     * @param baseElement
     * @param extensionElements
     * @return issues
     */
    private Collection<CheckerIssue> checkExecutionListener(final String bpmnFile, final BpmnElement element,
            final BaseElement baseElement, final ExtensionElements extensionElements) {

        final Collection<CheckerIssue> issues = new ArrayList<CheckerIssue>();

        final List<CamundaExecutionListener> listenerList = extensionElements.getElementsQuery()
                .filterByType(CamundaExecutionListener.class).list();
        for (final CamundaExecutionListener listener : listenerList) {
            final CamundaScript script = listener.getCamundaScript();
            if (script != null && script.getCamundaResource() == null) {
                final CheckerIssue issueEmptyScript = checkEmptyScriptContent(bpmnFile, element, baseElement,
                        script.getCamundaScriptFormat(), script.getTextContent());
                if (issueEmptyScript != null)
                    issues.add(issueEmptyScript);
                final CheckerIssue issueEmptyFormat = checkEmptyScriptFormat(bpmnFile, element, baseElement,
                        script.getCamundaScriptFormat(), script.getTextContent());
                if (issueEmptyFormat != null)
                    issues.add(issueEmptyFormat);
                final CheckerIssue issueInvalidScript = checkInvalidScriptContent(bpmnFile, element, baseElement,
                        script.getCamundaScriptFormat(), script.getTextContent());
                if (issueInvalidScript != null)
                    issues.add(issueInvalidScript);
            }
        }
        return issues;
    }

    /**
     * Check task listeners
     *
     * @param bpmnFile
     * @param baseElement
     * @param extensionElements
     * @return issues
     */
    private Collection<CheckerIssue> checkTaskListener(final String bpmnFile, final BpmnElement element,
            final BaseElement baseElement, final ExtensionElements extensionElements) {

        final Collection<CheckerIssue> issues = new ArrayList<CheckerIssue>();

        final List<CamundaTaskListener> listenerList = extensionElements.getElementsQuery()
                .filterByType(CamundaTaskListener.class).list();
        for (final CamundaTaskListener listener : listenerList) {
            final CamundaScript script = listener.getCamundaScript();
            if (script != null && script.getCamundaResource() == null) {
                final CheckerIssue issueEmptyScript = checkEmptyScriptContent(bpmnFile, element, baseElement,
                        script.getCamundaScriptFormat(), script.getTextContent());
                if (issueEmptyScript != null)
                    issues.add(issueEmptyScript);
                final CheckerIssue issueEmptyFormat = checkEmptyScriptFormat(bpmnFile, element, baseElement,
                        script.getCamundaScriptFormat(), script.getTextContent());
                if (issueEmptyFormat != null)
                    issues.add(issueEmptyFormat);
                final CheckerIssue issueInvalidScript = checkInvalidScriptContent(bpmnFile, element, baseElement,
                        script.getCamundaScriptFormat(), script.getTextContent());
                if (issueInvalidScript != null)
                    issues.add(issueInvalidScript);
            }
        }
        return issues;
    }

    /**
     * Check if groovy code is valid
     *
     * @param bpmnFile
     * @param baseElement
     * @param scriptText
     * @return CheckerIssue or null
     */
    private CheckerIssue parseGroovyCode(final String bpmnFile, final BpmnElement element,
            final BaseElement baseElement,
            final String scriptText) {
        final GroovyShell shell = new GroovyShell();
        try {
            shell.evaluate(scriptText);
        } catch (final CompilationFailedException | MissingPropertyException ex) {
            if (ex instanceof CompilationFailedException) {
                return IssueWriter.createSingleIssue(rule, CriticalityEnum.ERROR, element, bpmnFile,
                        ex.getMessage());
            } else {
                return IssueWriter.createSingleIssue(rule, CriticalityEnum.ERROR, element, bpmnFile,
                        String.format(Messages.getString("EmbeddedGroovyScriptChecker.1"), ex.getMessage())); //$NON-NLS-1$
            }
        }
        return null;
    }

    /**
     * Check empty script content
     *
     * @param bpmnFile
     * @param baseElement
     * @param scriptFormat
     * @param script
     * @return CheckerIssue or null
     */
    private CheckerIssue checkEmptyScriptContent(final String bpmnFile, final BpmnElement element,
            final BaseElement baseElement,
            final String scriptFormat, final String script) {

        if (scriptFormat != null && (script == null || script.isEmpty())) {
            return IssueWriter.createSingleIssue(rule, CriticalityEnum.ERROR, element, bpmnFile,
                    Messages.getString("EmbeddedGroovyScriptChecker.2")); //$NON-NLS-1$
        }
        return null;
    }

    /**
     * Check empty script format
     *
     * @param bpmnFile
     * @param baseElement
     * @param scriptFormat
     * @param script
     * @return CheckerIssue or null
     */
    private CheckerIssue checkEmptyScriptFormat(final String bpmnFile, final BpmnElement element,
            final BaseElement baseElement,
            final String scriptFormat, final String script) {

        if (scriptFormat == null && script != null) {
            return IssueWriter.createSingleIssue(rule, CriticalityEnum.ERROR, element, bpmnFile,
                    Messages.getString("EmbeddedGroovyScriptChecker.3")); //$NON-NLS-1$
        }
        return null;
    }

    /**
     * Check groovy syntax
     *
     * @param bpmnFile
     * @param baseElement
     * @param scriptFormat
     * @param script
     * @return CheckerIssue or null
     */
    private CheckerIssue checkInvalidScriptContent(final String bpmnFile, final BpmnElement element,
            final BaseElement baseElement, final String scriptFormat, final String script) {

        if (scriptFormat != null && scriptFormat.toLowerCase().equals(ConfigConstants.GROOVY) && script != null) {
            return parseGroovyCode(bpmnFile, element, baseElement, script);
        }
        return null;
    }
}
