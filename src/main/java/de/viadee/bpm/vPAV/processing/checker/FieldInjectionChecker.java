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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Logger;

import org.camunda.bpm.engine.delegate.Expression;
import org.camunda.bpm.engine.impl.el.FixedValue;
import org.camunda.bpm.model.bpmn.impl.BpmnModelConstants;
import org.camunda.bpm.model.bpmn.instance.BaseElement;
import org.camunda.bpm.model.bpmn.instance.BusinessRuleTask;
import org.camunda.bpm.model.bpmn.instance.SendTask;
import org.camunda.bpm.model.bpmn.instance.ServiceTask;
import org.camunda.bpm.model.bpmn.instance.UserTask;

import de.odysseus.el.tree.IdentifierNode;
import de.odysseus.el.tree.Tree;
import de.odysseus.el.tree.TreeBuilder;
import de.odysseus.el.tree.impl.Builder;
import de.viadee.bpm.vPAV.BpmnScanner;
import de.viadee.bpm.vPAV.Messages;
import de.viadee.bpm.vPAV.RuntimeConfig;
import de.viadee.bpm.vPAV.config.model.Rule;
import de.viadee.bpm.vPAV.constants.BpmnConstants;
import de.viadee.bpm.vPAV.output.IssueWriter;
import de.viadee.bpm.vPAV.processing.model.data.BpmnElement;
import de.viadee.bpm.vPAV.processing.model.data.CheckerIssue;
import de.viadee.bpm.vPAV.processing.model.data.CriticalityEnum;

/**
 * Class FieldInjectionChecker
 */
public class FieldInjectionChecker extends AbstractElementChecker {

    private static final Logger LOGGER = Logger.getLogger(FieldInjectionChecker.class.getName());

    public FieldInjectionChecker(final Rule rule, final BpmnScanner bpmnScanner) {
        super(rule, bpmnScanner);
    }

    /**
     * Check for JavaDelegates in Tasks
     *
     * @return issues
     */

    @Override
    public Collection<CheckerIssue> check(final BpmnElement element) {

        final Collection<CheckerIssue> issues = new ArrayList<CheckerIssue>();
        final BaseElement bpmnElement = element.getBaseElement();
        String implementationAttr = null;
        ArrayList<String> executionDelegate = new ArrayList<String>();
        ArrayList<String> executionClass = new ArrayList<String>();
        ArrayList<String> executionExpression = new ArrayList<String>();
        ArrayList<String> taskDelegate = new ArrayList<String>();
        ArrayList<String> taskClass = new ArrayList<String>();
        ArrayList<String> taskExpression = new ArrayList<String>();

        // read attributes from task
        if ((bpmnElement instanceof ServiceTask || bpmnElement instanceof BusinessRuleTask
                || bpmnElement instanceof SendTask))
            implementationAttr = bpmnScanner.getImplementation(bpmnElement.getId());

        if (bpmnElement instanceof UserTask) {
            taskDelegate = bpmnScanner.getListener(bpmnElement.getId(), BpmnConstants.ATTR_DEL,
                    BpmnConstants.CAMUNDA_TASKLISTENER);
            taskClass = bpmnScanner.getListener(bpmnElement.getId(), BpmnConstants.ATTR_CLASS,
                    BpmnConstants.CAMUNDA_TASKLISTENER);
            taskExpression = bpmnScanner.getListener(bpmnElement.getId(), BpmnConstants.ATTR_EX,
                    BpmnConstants.CAMUNDA_TASKLISTENER);
        }

        executionDelegate = bpmnScanner.getListener(bpmnElement.getId(), BpmnConstants.CAMUNDA_EXECUTIONLISTENER,
                BpmnConstants.CAMUNDA_EXECUTIONLISTENER);
        executionClass = bpmnScanner.getListener(bpmnElement.getId(), BpmnConstants.ATTR_CLASS,
                BpmnConstants.CAMUNDA_EXECUTIONLISTENER);
        executionExpression = bpmnScanner.getListener(bpmnElement.getId(), BpmnConstants.ATTR_EX,
                BpmnConstants.CAMUNDA_EXECUTIONLISTENER);

        final ArrayList<String> fieldInjectionVarNames = bpmnScanner.getFieldInjectionVarName(bpmnElement.getId());
        final String classAttr = bpmnElement.getAttributeValueNs(BpmnModelConstants.CAMUNDA_NS,
                BpmnConstants.ATTR_CLASS);
        final String delegateExprAttr = bpmnElement.getAttributeValueNs(BpmnModelConstants.CAMUNDA_NS,
                BpmnConstants.ATTR_DEL);

        if (implementationAttr != null && !fieldInjectionVarNames.isEmpty() && fieldInjectionVarNames != null
                && (bpmnElement instanceof ServiceTask || bpmnElement instanceof BusinessRuleTask
                        || bpmnElement instanceof SendTask)) {
            // check if class is correct
            if (implementationAttr.equals(BpmnConstants.CAMUNDA_CLASS)) {
                if (classAttr == null || classAttr.trim().length() == 0) {
                } else {
                    for (String fieldInjectionVarName : fieldInjectionVarNames)
                        issues.addAll(checkClassFileForVar(element, classAttr, fieldInjectionVarName));
                }
            }

            // check if delegateExpression is correct
            else if (implementationAttr.equals(BpmnConstants.CAMUNDA_DEXPRESSION)) {
                if (delegateExprAttr == null || delegateExprAttr.trim().length() == 0) {
                    // no delegateExpression has been configured
                } else {
                    // check validity of a bean
                    if (RuntimeConfig.getInstance().getBeanMapping() != null) {
                        final TreeBuilder treeBuilder = new Builder();
                        final Tree tree = treeBuilder.build(delegateExprAttr);
                        final Iterable<IdentifierNode> identifierNodes = tree.getIdentifierNodes();
                        // if beanMapping ${...} reference
                        if (identifierNodes.iterator().hasNext()) {
                            for (final IdentifierNode node : identifierNodes) {
                                final String classFile = RuntimeConfig.getInstance().getBeanMapping()
                                        .get(node.getName());
                                // correct beanmapping was found -> check if class exists
                                if (classFile != null && classFile.trim().length() > 0) {
                                    for (String fieldInjectionVarName : fieldInjectionVarNames)
                                        issues.addAll(checkClassFileForVar(element, classFile, fieldInjectionVarName));
                                } else {
                                    // incorrect beanmapping
                                }
                            }
                        } else {
                            for (String fieldInjectionVarName : fieldInjectionVarNames)
                                issues.addAll(checkClassFileForVar(element, delegateExprAttr, fieldInjectionVarName));
                        }
                    } else {
                        // check if class exists
                        for (String fieldInjectionVarName : fieldInjectionVarNames)
                            issues.addAll(checkClassFileForVar(element, delegateExprAttr, fieldInjectionVarName));
                    }
                }
            }
        }

        // checkListener
        if (executionClass != null || executionDelegate != null || executionExpression != null
                && (!fieldInjectionVarNames.isEmpty() && fieldInjectionVarNames != null)) {
            for (String fieldInjectionVarName : fieldInjectionVarNames)
                issues.addAll(checkListener(element, executionClass, executionDelegate, executionExpression,
                        fieldInjectionVarName));
        }
        if (taskClass != null || taskDelegate != null
                || taskExpression != null && (!fieldInjectionVarNames.isEmpty() && fieldInjectionVarNames != null)) {
            for (String fieldInjectionVarName : fieldInjectionVarNames)
                issues.addAll(
                        checkListener(element, taskClass, taskDelegate, taskExpression, fieldInjectionVarName));
        }

        return issues;
    }

    /**
     *
     * Check listener for Classes, DelegateExpressions and Expressions
     *
     * @param element
     *            BpmnElement
     * @param aClass
     *            Class, can be null
     * @param aDelegate
     *            DelegateExpression, can be null
     * @param aExpression
     *            Expression, can be null
     * @param varName
     *            name of the variable
     * @return Collection of CheckerIssues
     */
    private Collection<CheckerIssue> checkListener(final BpmnElement element, ArrayList<String> aClass,
            ArrayList<String> aDelegate, ArrayList<String> aExpression, String varName) {
        final Collection<CheckerIssue> issues = new ArrayList<CheckerIssue>();

        // classes
        if (aClass == null || aClass.size() > 0) {
            for (String eClass : aClass) {
                if (eClass != null && eClass.trim().length() == 0) {
                    // no class has been configured
                } else if (eClass != null) {
                    issues.addAll(checkClassFileForVar(element, eClass, varName));
                }
            }
        }

        // delegateExpression
        if (aDelegate != null && !aDelegate.isEmpty()) {
            for (String eDel : aDelegate) {
                if (eDel == null || eDel.trim().length() == 0) {
                    // no delegateExpression has been configured
                } else if (eDel != null) {
                    // check validity of a bean
                    if (RuntimeConfig.getInstance().getBeanMapping() != null) {
                        final TreeBuilder treeBuilder = new Builder();
                        final Tree tree = treeBuilder.build(eDel);
                        final Iterable<IdentifierNode> identifierNodes = tree.getIdentifierNodes();
                        // if beanMapping ${...} reference
                        if (identifierNodes.iterator().hasNext()) {
                            for (final IdentifierNode node : identifierNodes) {
                                final String classFile = RuntimeConfig.getInstance().getBeanMapping()
                                        .get(node.getName());
                                // correct beanmapping was found -> check if class exists
                                if (classFile != null && classFile.trim().length() > 0) {
                                    issues.addAll(checkClassFileForVar(element, classFile, varName));
                                }
                            }
                        } else {
                            issues.addAll(checkClassFileForVar(element, eDel, varName));
                        }
                    } else {
                        // check if class exists
                        issues.addAll(checkClassFileForVar(element, eDel, varName));
                    }
                }
            }
        }
        return issues;
    }

    /**
     * check class for correct variable
     *
     * @param element
     *            the bpmn element
     * @param className
     *            name of the class
     * @param varName
     *            name of the variable
     * @return collection if issues
     */
    private Collection<CheckerIssue> checkClassFileForVar(final BpmnElement element, final String className,
            final String varName) {

        final Collection<CheckerIssue> issues = new ArrayList<CheckerIssue>();
        final String classPath = className.replaceAll("\\.", "/") + ".java"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        try {
            Class<?> clazz = RuntimeConfig.getInstance().getClassLoader().loadClass(className);
            try {
                Field field = clazz.getDeclaredField(varName);

                if (!field.getType().isAssignableFrom(FixedValue.class)
                        && !field.getType().isAssignableFrom(Expression.class)) {
                    issues.add(IssueWriter.createIssueWithClassPath(rule, CriticalityEnum.WARNING, classPath, element,
                            String.format(Messages.getString("FieldInjectionChecker.3"), varName))); //$NON-NLS-1$
                }

                if (!Modifier.isPublic(field.getModifiers())) {
                    issues.add(IssueWriter.createIssueWithClassPath(rule, CriticalityEnum.WARNING, classPath, element,
                            String.format(Messages.getString("FieldInjectionChecker.4"), varName))); //$NON-NLS-1$

                }
                Method[] methods = clazz.getMethods();
                boolean hasMethod = false;
                for (Method method : methods) {
                    if (method.getName().toLowerCase().contains("set" + varName.toLowerCase())) //$NON-NLS-1$
                        hasMethod = true;
                }

                if (!hasMethod) {
                    issues.add(IssueWriter.createIssueWithClassPath(rule, CriticalityEnum.WARNING, classPath, element,
                            String.format(Messages.getString("FieldInjectionChecker.6"), varName))); //$NON-NLS-1$
                }

            } catch (NoSuchFieldException | SecurityException e) {
                issues.add(IssueWriter.createIssueWithClassPath(rule, CriticalityEnum.WARNING, classPath, element,
                        String.format(Messages.getString("FieldInjectionChecker.7"), clazz.getSimpleName(), //$NON-NLS-1$
                                varName)));
            }

        } catch (final ClassNotFoundException e) {
            LOGGER.warning("Class " + className + " does not exist." + e); //$NON-NLS-1$ //$NON-NLS-2$
        }

        return issues;
    }
}
