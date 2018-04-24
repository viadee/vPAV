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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.el.ELException;

import org.camunda.bpm.model.bpmn.impl.BpmnModelConstants;
import org.camunda.bpm.model.bpmn.instance.BaseElement;
import org.camunda.bpm.model.bpmn.instance.BusinessRuleTask;
import org.camunda.bpm.model.bpmn.instance.ExtensionElements;
import org.camunda.bpm.model.bpmn.instance.MessageEventDefinition;
import org.camunda.bpm.model.bpmn.instance.ScriptTask;
import org.camunda.bpm.model.bpmn.instance.SendTask;
import org.camunda.bpm.model.bpmn.instance.ServiceTask;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaExecutionListener;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaScript;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaTaskListener;

import de.odysseus.el.tree.IdentifierNode;
import de.odysseus.el.tree.Tree;
import de.odysseus.el.tree.TreeBuilder;
import de.odysseus.el.tree.impl.Builder;
import de.viadee.bpm.vPAV.BpmnScanner;
import de.viadee.bpm.vPAV.FileScanner;
import de.viadee.bpm.vPAV.Messages;
import de.viadee.bpm.vPAV.RuntimeConfig;
import de.viadee.bpm.vPAV.config.model.Rule;
import de.viadee.bpm.vPAV.constants.BpmnConstants;
import de.viadee.bpm.vPAV.constants.ConfigConstants;
import de.viadee.bpm.vPAV.output.IssueWriter;
import de.viadee.bpm.vPAV.processing.ProcessingException;
import de.viadee.bpm.vPAV.processing.model.data.BpmnElement;
import de.viadee.bpm.vPAV.processing.model.data.CheckerIssue;
import de.viadee.bpm.vPAV.processing.model.data.CriticalityEnum;

/**
 * check versioning of the referenced classes, scripts and beans
 *
 */
public class VersioningChecker extends AbstractElementChecker {

    private Collection<String> resourcesNewestVersions;

    public VersioningChecker(final Rule rule, final BpmnScanner bpmnScanner,
            final Collection<String> resourcesNewestVersions) {
        super(rule, bpmnScanner);
        this.resourcesNewestVersions = resourcesNewestVersions;
    }

    /**
     * Check versions of referenced beans and/or classes
     *
     * @return issues
     */
    @Override
    public Collection<CheckerIssue> check(final BpmnElement element) {

        final Collection<CheckerIssue> issues = new ArrayList<CheckerIssue>();
        final BaseElement baseElement = element.getBaseElement();

        // Service Task, Business Task, Send Task
        issues.addAll(checkCommonTasks(element));

        // Script Task
        issues.addAll(checkScriptTask(element));

        // Listener
        final ExtensionElements extensionElements = baseElement.getExtensionElements();

        if (extensionElements != null) {
            // Execution Listener
            issues.addAll(checkExecutionListener(element, extensionElements));

            // Task Listener
            issues.addAll(checkTaskListener(element, extensionElements));
        }

        // Message Event Definition
        issues.addAll(checkMessageEventDefinition(element));
        return issues;

    }

    /**
     * check versioning for execution listener
     *
     * @param element
     * @param extensionElements
     * @return issues
     */
    private Collection<CheckerIssue> checkExecutionListener(final BpmnElement element,
            final ExtensionElements extensionElements) {
        final Collection<CheckerIssue> issues = new ArrayList<CheckerIssue>();
        List<CamundaExecutionListener> execListenerList = extensionElements.getElementsQuery()
                .filterByType(CamundaExecutionListener.class).list();
        for (final CamundaExecutionListener listener : execListenerList) {
            final String l_expression = listener.getCamundaExpression();
            if (l_expression != null) {
                prepareBeanWarning(l_expression, element, issues);
            }
            final String l_delegateExpression = listener.getCamundaDelegateExpression();
            if (l_delegateExpression != null) {
                prepareBeanWarning(l_delegateExpression, element, issues);
            }
            final String javaReference = getClassReference(listener.getCamundaClass());
            if (javaReference != null) {
                prepareClassWarning(javaReference, element, issues);
            }

            final CamundaScript script = listener.getCamundaScript();
            if (script != null && script.getCamundaScriptFormat() != null
                    && script.getCamundaScriptFormat().equals(ConfigConstants.GROOVY)) {
                final String resourcePath = getGroovyReference(script.getCamundaResource());
                prepareScriptWarning(resourcePath, element, issues);
            }
        }
        return issues;
    }

    /**
     * check versioning for task listener
     *
     * @param element
     * @param extensionElements
     * @return issues
     */
    private Collection<CheckerIssue> checkTaskListener(final BpmnElement element,
            final ExtensionElements extensionElements) {
        final Collection<CheckerIssue> issues = new ArrayList<CheckerIssue>();
        List<CamundaTaskListener> taskListenerList = extensionElements.getElementsQuery()
                .filterByType(CamundaTaskListener.class).list();
        for (final CamundaTaskListener listener : taskListenerList) {
            final String l_expression = listener.getCamundaExpression();
            if (l_expression != null) {
                prepareBeanWarning(l_expression, element, issues);
            }
            final String l_delegateExpression = listener.getCamundaDelegateExpression();
            if (l_delegateExpression != null) {
                prepareBeanWarning(l_delegateExpression, element, issues);
            }
            final String javaReference = getClassReference(listener.getCamundaClass());
            if (javaReference != null) {
                prepareClassWarning(javaReference, element, issues);
            }

            final CamundaScript script = listener.getCamundaScript();
            if (script != null && script.getCamundaScriptFormat() != null
                    && script.getCamundaScriptFormat().equals(ConfigConstants.GROOVY)) {
                final String resourcePath = getGroovyReference(script.getCamundaResource());
                prepareScriptWarning(resourcePath, element, issues);
            }
        }
        return issues;
    }

    /**
     * check versioning for service task, send task or business rule task
     *
     * @param element
     * @return issues
     */
    private Collection<CheckerIssue> checkCommonTasks(final BpmnElement element) {
        final Collection<CheckerIssue> issues = new ArrayList<CheckerIssue>();
        final BaseElement baseElement = element.getBaseElement();
        if (baseElement instanceof ServiceTask || baseElement instanceof SendTask
                || baseElement instanceof BusinessRuleTask) {
            // Class, Expression, Delegate Expression
            final String t_expression = baseElement.getAttributeValueNs(BpmnModelConstants.CAMUNDA_NS,
                    BpmnConstants.ATTR_EX);
            if (t_expression != null) {
                prepareBeanWarning(t_expression, element, issues);
            }

            final String t_delegateExpression = baseElement.getAttributeValueNs(BpmnModelConstants.CAMUNDA_NS,
                    BpmnConstants.ATTR_DEL);
            if (t_delegateExpression != null && !FileScanner.getIsDirectory()) {
                prepareBeanWarning(t_delegateExpression, element, issues);
            } else if (t_delegateExpression != null && FileScanner.getIsDirectory()) {
                prepareDirBasedBeanWarning(t_delegateExpression, element, issues);
            }

            final String javaReference = baseElement.getAttributeValueNs(BpmnModelConstants.CAMUNDA_NS,
                    BpmnConstants.ATTR_CLASS);
            if (getClassReference(javaReference) != null && !FileScanner.getIsDirectory()) {
                prepareClassWarning(getClassReference(javaReference), element, issues);
            } else if (javaReference != null && FileScanner.getIsDirectory()) {
                prepareDirBasedClassWarning(javaReference, element, issues);
            }
        }
        return issues;
    }

    /**
     * check versioning for script task
     *
     * @param element
     * @return issues
     */
    private Collection<CheckerIssue> checkScriptTask(final BpmnElement element) {
        final Collection<CheckerIssue> issues = new ArrayList<CheckerIssue>();
        final BaseElement baseElement = element.getBaseElement();
        if (baseElement instanceof ScriptTask) {
            final ScriptTask scriptTask = (ScriptTask) baseElement;
            if (scriptTask.getScriptFormat() != null && scriptTask.getScriptFormat().equals(ConfigConstants.GROOVY)) {
                // External Resource
                String resourcePath = scriptTask.getCamundaResource();
                resourcePath = getGroovyReference(resourcePath);
                prepareScriptWarning(resourcePath, element, issues);
            }
        }
        return issues;
    }

    /**
     * check versioning for message event
     *
     * @param element
     * @return
     */
    private Collection<CheckerIssue> checkMessageEventDefinition(final BpmnElement element) {
        final Collection<CheckerIssue> issues = new ArrayList<CheckerIssue>();
        final BaseElement baseElement = element.getBaseElement();

        if (baseElement instanceof MessageEventDefinition) {
            // Class, Expression, Delegate Expression
            final MessageEventDefinition eventDef = (MessageEventDefinition) baseElement;
            final String javaReference = getClassReference(eventDef.getCamundaClass());
            if (javaReference != null) {
                prepareClassWarning(javaReference, element, issues);
            }
            final String e_expression = eventDef.getCamundaExpression();
            if (e_expression != null) {
                prepareBeanWarning(e_expression, element, issues);
            }
            final String e_delegateExpression = eventDef.getCamundaDelegateExpression();
            if (e_delegateExpression != null) {
                prepareBeanWarning(e_delegateExpression, element, issues);
            }
        }
        return issues;
    }

    /**
     * convert package format into class file name
     *
     * @param javaResource
     * @return file
     */
    private String getClassReference(final String javaResource) {
        if (javaResource != null && !FileScanner.getIsDirectory()) {
            return javaResource.substring(javaResource.lastIndexOf('.') + 1, javaResource.length()) + ".class"; //$NON-NLS-1$
        } else if (javaResource != null && FileScanner.getIsDirectory()) {
            return javaResource;
        }
        return null;
    }

    /**
     * convert package format into groovy file name
     *
     * @param javaResource
     * @return file
     */
    private String getGroovyReference(String resourcePath) {
        if (resourcePath != null) {
            resourcePath = resourcePath.substring(0, resourcePath.lastIndexOf('.'));
            return resourcePath.substring(resourcePath.lastIndexOf('.') + 1, resourcePath.length()) + ".groovy"; //$NON-NLS-1$
        }
        return null;
    }

    /**
     * Finds java bean in an expression and returns the java file path
     *
     * @param expression
     * @return file path
     */
    private String findBeanReferenceInExpression(final String expression, final BpmnElement element,
            final Collection<CheckerIssue> issues) {

        try {
            final String filteredExpression = expression.replaceAll("[\\w]+\\.", ""); //$NON-NLS-1$ //$NON-NLS-2$
            final TreeBuilder treeBuilder = new Builder();
            final Tree tree = treeBuilder.build(filteredExpression);

            final Iterable<IdentifierNode> identifierNodes = tree.getIdentifierNodes();
            final Set<String> paths = new HashSet<String>();
            for (final IdentifierNode node : identifierNodes) {
                if (RuntimeConfig.getInstance().getBeanMapping() != null) {
                    final String packagePath = RuntimeConfig.getInstance().getBeanMapping().get(node.getName());
                    if (packagePath != null) {
                        paths.add(packagePath);
                    }
                }
            }
            if (!paths.isEmpty()) {
                return getClassReference(paths.iterator().next());
            }
        } catch (final ELException e) {
            throw new ProcessingException(
                    "el expression " + expression + " in " + element.getProcessdefinition() + ", element ID: " //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                            + element.getBaseElement().getId() + " couldn't be parsed", //$NON-NLS-1$
                    e);
        }
        return null;
    }

    /**
     * prepares an issue for script after check
     *
     * @param resourcePath
     * @param element
     * @param issues
     */
    private void prepareScriptWarning(final String resourcePath, final BpmnElement element,
            final Collection<CheckerIssue> issues) {
        if (resourcePath != null) {
            if (!resourcesNewestVersions.contains(resourcePath)) {
                issues.addAll(IssueWriter.createIssue(rule, CriticalityEnum.WARNING, resourcePath, element,
                        Messages.getString("VersioningChecker.8"))); //$NON-NLS-1$
            }
        }
    }

    /**
     * prepares an issue for bean after check
     *
     * @param expression
     * @param element
     * @param issues
     */
    private void prepareBeanWarning(final String expression, final BpmnElement element,
            final Collection<CheckerIssue> issues) {
        final String beanReference = findBeanReferenceInExpression(expression, element, issues);
        if (beanReference != null && !resourcesNewestVersions.contains(beanReference)) {
            issues.addAll(IssueWriter.createIssue(rule, CriticalityEnum.WARNING, beanReference, element,
                    String.format(Messages.getString("VersioningChecker.9"), //$NON-NLS-1$
                            beanReference, expression)));
        }
    }

    /**
     * Prepares an issue for bean after check. Gets called only if the versioning scheme is directory based
     *
     * @param expression
     * @param element
     * @param issues
     */
    private void prepareDirBasedBeanWarning(final String expression, final BpmnElement element,
            final Collection<CheckerIssue> issues) {
        String beanReference = findBeanReferenceInExpression(expression, element, issues);

        if (beanReference != null) {
            beanReference = beanReference.replace(".", "\\"); //$NON-NLS-1$ //$NON-NLS-2$
            beanReference = beanReference.substring(0, beanReference.lastIndexOf("\\")); //$NON-NLS-1$
            beanReference = beanReference.replace("\\", "/");

            if (!resourcesNewestVersions.contains(beanReference)) {
                issues.add(IssueWriter.createIssueWithBeanRef(rule, CriticalityEnum.WARNING, element, beanReference,
                        String.format(Messages.getString("VersioningChecker.13"), beanReference))); //$NON-NLS-1$
            }
        }
    }

    /**
     * Prepares an issue for class after check
     *
     * @param javaReference
     * @param element
     * @param issues
     */
    private void prepareClassWarning(final String javaReference, final BpmnElement element,
            final Collection<CheckerIssue> issues) {
        if (javaReference != null) {
            if (!resourcesNewestVersions.contains(javaReference)) {
                if (element.getBaseElement().getId() == null) {
                    issues.add(IssueWriter.createIssueWithJavaRef(rule, CriticalityEnum.WARNING, element, javaReference,
                            String.format(Messages.getString("VersioningChecker.14"), javaReference))); //$NON-NLS-1$

                } else {
                    issues.add(IssueWriter.createIssueWithJavaRef(rule, CriticalityEnum.WARNING, element, javaReference,
                            String.format(Messages.getString("VersioningChecker.14"), javaReference))); //$NON-NLS-1$
                }
            }
        }
    }

    /**
     * Prepares an issue for class after check. Gets called only if the versioning scheme is directory based
     *
     * @param javaReference
     * @param element
     * @param issues
     */
    private void prepareDirBasedClassWarning(String javaReference, final BpmnElement element,
            final Collection<CheckerIssue> issues) {
        if (javaReference != null) {
            javaReference = javaReference.replace(".", "\\"); //$NON-NLS-1$ //$NON-NLS-2$
            javaReference = javaReference.substring(0, javaReference.lastIndexOf("\\")); //$NON-NLS-1$

            if (!resourcesNewestVersions.contains(javaReference)) {
                issues.addAll(IssueWriter.createIssue(rule, CriticalityEnum.WARNING, javaReference, element,
                        String.format(Messages.getString("VersioningChecker.19"), javaReference))); //$NON-NLS-1$
            }
        }
    }

}
