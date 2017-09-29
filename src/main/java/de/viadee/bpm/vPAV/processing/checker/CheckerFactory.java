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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.camunda.bpm.model.bpmn.instance.BaseElement;
import org.camunda.bpm.model.bpmn.instance.BusinessRuleTask;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.bpmn.instance.SubProcess;
import org.camunda.bpm.model.bpmn.instance.Task;

import de.viadee.bpm.vPAV.config.model.Rule;
import de.viadee.bpm.vPAV.processing.ConfigItemNotFoundException;
import de.viadee.bpm.vPAV.processing.model.data.BpmnElement;

/**
 * Factory decides which Checkers will be used in defined situations
 *
 */
public final class CheckerFactory {

    public static String implementation;

    /**
     * create checkers
     *
     * @param ruleConf
     *            rules for checker
     * @param resourcesNewestVersions
     *            resourcesNewestVersions in context
     * @param element
     *            given BpmnElement
     * @param path
     *            path to model file
     * @return checkers returns checkers
     *
     * @throws ConfigItemNotFoundException
     *             exception when ConfigItem (e.g. rule) not found
     */
    public static Collection<ElementChecker> createCheckerInstancesBpmnElement(
            final Map<String, Rule> ruleConf, final Collection<String> resourcesNewestVersions,
            final BpmnElement element, String path)
            throws ConfigItemNotFoundException {

        final Collection<ElementChecker> checkers = new ArrayList<ElementChecker>();
        final BaseElement baseElement = element.getBaseElement();
        if (baseElement == null) {
            throw new RuntimeException("Bpmn Element couldn't be found");
        }

        final Rule javaDelegateRule = ruleConf.get(getClassName(JavaDelegateChecker.class));
        if (javaDelegateRule == null)
            throw new ConfigItemNotFoundException(getClassName(JavaDelegateChecker.class) + " not found");
        if (javaDelegateRule.isActive() && !(baseElement instanceof Process) && !(baseElement instanceof SubProcess)) {
            checkers.add(new JavaDelegateChecker(javaDelegateRule, path));
        }

        final Rule dmnTaskRule = ruleConf.get(getClassName(DmnTaskChecker.class));
        if (dmnTaskRule == null)
            throw new ConfigItemNotFoundException(getClassName(DmnTaskChecker.class) + " not found");
        if (dmnTaskRule.isActive() && baseElement instanceof BusinessRuleTask) {
            checkers.add(new DmnTaskChecker(dmnTaskRule, path));
        }

        final Rule xorNamingConventionRule = ruleConf.get(getClassName(XorNamingConventionChecker.class));
        if (xorNamingConventionRule == null)
            throw new ConfigItemNotFoundException(getClassName(XorNamingConventionChecker.class) + " not found");
        if (xorNamingConventionRule.isActive()) {
            checkers.add(new XorNamingConventionChecker(xorNamingConventionRule, path));
        }

        final Rule noScriptCheckerRule = ruleConf.get(getClassName(NoScriptChecker.class));
        if (noScriptCheckerRule == null)
            throw new ConfigItemNotFoundException(getClassName(NoScriptChecker.class) + " not found");
        if (noScriptCheckerRule.isActive()) {
            checkers.add(new NoScriptChecker(noScriptCheckerRule, path));
        }

        final Rule processVariablesNameConventionRule = ruleConf
                .get(getClassName(ProcessVariablesNameConventionChecker.class));
        if (processVariablesNameConventionRule == null)
            throw new ConfigItemNotFoundException(
                    getClassName(ProcessVariablesNameConventionChecker.class) + " not found");
        if (processVariablesNameConventionRule.isActive()) {
            checkers.add(new ProcessVariablesNameConventionChecker(processVariablesNameConventionRule));
        }

        final Rule taskNamingConventionRule = ruleConf
                .get(getClassName(TaskNamingConventionChecker.class));
        if (taskNamingConventionRule == null)
            throw new ConfigItemNotFoundException(
                    getClassName(TaskNamingConventionChecker.class) + " not found");
        if (baseElement instanceof Task && taskNamingConventionRule.isActive()) {
            checkers.add(new TaskNamingConventionChecker(taskNamingConventionRule));
        }

        final Rule versioningRule = ruleConf.get(getClassName(VersioningChecker.class));
        if (versioningRule == null)
            throw new ConfigItemNotFoundException(getClassName(VersioningChecker.class) + " not found");
        if (versioningRule.isActive()) {
            checkers.add(new VersioningChecker(versioningRule, resourcesNewestVersions));
        }

        final Rule embeddedGroovyScriptRule = ruleConf
                .get(getClassName(EmbeddedGroovyScriptChecker.class));
        if (embeddedGroovyScriptRule == null)
            throw new ConfigItemNotFoundException(
                    getClassName(EmbeddedGroovyScriptChecker.class) + " not found");
        if (embeddedGroovyScriptRule.isActive()) {
            checkers.add(new EmbeddedGroovyScriptChecker(embeddedGroovyScriptRule));
        }

        final Rule timerExpressionRule = ruleConf
                .get(getClassName(TimerExpressionChecker.class));
        if (timerExpressionRule == null)
            throw new ConfigItemNotFoundException(
                    getClassName(TimerExpressionChecker.class) + " not found");
        if (timerExpressionRule.isActive()) {
            checkers.add(new TimerExpressionChecker(timerExpressionRule, path));
        }

        final Rule elementIdConventionRule = ruleConf
                .get(getClassName(ElementIdConventionChecker.class));
        if (elementIdConventionRule == null)
            throw new ConfigItemNotFoundException(
                    getClassName(ElementIdConventionChecker.class) + " not found");
        if (elementIdConventionRule.isActive()) {
            checkers.add(new ElementIdConventionChecker(elementIdConventionRule));
        }

        final Rule noExpressionCheckerRule = ruleConf
                .get(getClassName(NoExpressionChecker.class));
        if (noExpressionCheckerRule == null)
            throw new ConfigItemNotFoundException(
                    getClassName(NoExpressionChecker.class) + " not found");
        if (noExpressionCheckerRule.isActive()) {
            checkers.add(new NoExpressionChecker(noExpressionCheckerRule, path));
        }

        return checkers;
    }

    private static String getClassName(Class<?> clazz) {
        return clazz.getSimpleName();
    }
}
