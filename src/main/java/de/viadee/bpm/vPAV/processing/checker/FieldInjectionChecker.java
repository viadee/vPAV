/*
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
import de.viadee.bpm.vPAV.processing.code.flow.BpmnElement;
import de.viadee.bpm.vPAV.processing.model.data.CheckerIssue;
import de.viadee.bpm.vPAV.processing.model.data.CriticalityEnum;
import org.camunda.bpm.engine.delegate.Expression;
import org.camunda.bpm.engine.impl.el.FixedValue;
import org.camunda.bpm.model.bpmn.instance.*;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaExecutionListener;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaTaskListener;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Class FieldInjectionChecker
 */
public class FieldInjectionChecker extends AbstractElementChecker {

    private static final Logger LOGGER = Logger.getLogger(FieldInjectionChecker.class.getName());

    public FieldInjectionChecker(final Rule rule) {
        super(rule);
    }

    /**
     * Check for JavaDelegates in Tasks
     *
     * @return issues
     */

    @Override
    public Collection<CheckerIssue> check(final BpmnElement element) {

        final Collection<CheckerIssue> issues = new ArrayList<>();
        final BaseElement bpmnElement = element.getBaseElement();
        Map.Entry<String, String> implementationAttr = null;
        final ArrayList<String> executionDelegate = new ArrayList<>();
        final ArrayList<String> executionClass = new ArrayList<>();
        final ArrayList<String> taskDelegate = new ArrayList<>();
        final ArrayList<String> taskClass = new ArrayList<>();

        // read attributes from task
        if ((bpmnElement instanceof ServiceTask || bpmnElement instanceof BusinessRuleTask
                || bpmnElement instanceof SendTask)) {
            implementationAttr = BpmnScanner.getImplementation(bpmnElement);
        }

        if (bpmnElement instanceof UserTask) {
            ArrayList<ModelElementInstance> taskListener = BpmnScanner
                    .getListener(bpmnElement,
                            BpmnConstants.CAMUNDA_TASK_LISTENER);
            taskListener.forEach(listener -> {
                taskDelegate.add(((CamundaTaskListener) listener).getCamundaDelegateExpression());
                taskClass.add(((CamundaTaskListener) listener).getCamundaClass());
            });
        }
        ArrayList<ModelElementInstance> executionListener = BpmnScanner
                .getListener(bpmnElement,
                        BpmnConstants.CAMUNDA_EXECUTION_LISTENER);
        executionListener.forEach(listener -> {
            executionDelegate.add(((CamundaExecutionListener) listener).getCamundaDelegateExpression());
            executionClass.add(((CamundaExecutionListener) listener).getCamundaClass());
        });

        final ArrayList<String> fieldInjectionVarNames = BpmnScanner.getFieldInjectionVarName(bpmnElement);

        if (implementationAttr != null && !fieldInjectionVarNames.isEmpty() && (bpmnElement instanceof ServiceTask ||
                bpmnElement instanceof BusinessRuleTask || bpmnElement instanceof SendTask)) {
            // check if class is correct
            if (implementationAttr.getKey().equals(BpmnConstants.CAMUNDA_CLASS)) {
                if (implementationAttr.getValue() != null && implementationAttr.getValue().trim().length() != 0) {
                    for (String fieldInjectionVarName : fieldInjectionVarNames)
                        issues.addAll(
                                checkClassFileForVar(element, implementationAttr.getValue(), fieldInjectionVarName));
                }
            }

            // check if delegateExpression is correct
            else if (implementationAttr.getKey().equals(BpmnConstants.CAMUNDA_DEXPRESSION)) {
                if (implementationAttr.getValue() != null && implementationAttr.getValue().trim().length() != 0) {
                    // check validity of a bean
                    if (RuntimeConfig.getInstance().getBeanMapping() != null) {
                        final TreeBuilder treeBuilder = new Builder();
                        final Tree tree = treeBuilder.build(implementationAttr.getValue());
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
                                }

                            }
                        } else {
                            for (String fieldInjectionVarName : fieldInjectionVarNames)
                                issues.addAll(checkClassFileForVar(element, implementationAttr.getValue(),
                                        fieldInjectionVarName));
                        }
                    } else {
                        // check if class exists
                        for (String fieldInjectionVarName : fieldInjectionVarNames)
                            issues.addAll(checkClassFileForVar(element, implementationAttr.getValue(),
                                    fieldInjectionVarName));
                    }
                }
            }
        }

        // checkListener
        if (!fieldInjectionVarNames.isEmpty() && fieldInjectionVarNames != null) {
            for (String fieldInjectionVarName : fieldInjectionVarNames)
                issues.addAll(checkListener(element, executionClass, executionDelegate,
                        fieldInjectionVarName));
        }
        if (!fieldInjectionVarNames.isEmpty() && fieldInjectionVarNames != null) {
            for (String fieldInjectionVarName : fieldInjectionVarNames)
                issues.addAll(
                        checkListener(element, taskClass, taskDelegate, fieldInjectionVarName));
        }

        return issues;
    }

    /**
     * Check listener for Classes, DelegateExpressions and Expressions
     *
     * @param element   BpmnElement
     * @param aClass    Class, can be null
     * @param aDelegate DelegateExpression, can be null
     * @param varName   name of the variable
     * @return Collection of CheckerIssues
     */
    private Collection<CheckerIssue> checkListener(final BpmnElement element, ArrayList<String> aClass,
            ArrayList<String> aDelegate, String varName) {
        final Collection<CheckerIssue> issues = new ArrayList<>();

        // classes
        if (aClass == null || aClass.size() > 0) {
            for (String eClass : aClass) {
                if (eClass != null && eClass.trim().length() > 0) {
                    issues.addAll(checkClassFileForVar(element, eClass, varName));
                }
            }
        }

        // delegateExpression
        if (aDelegate != null && !aDelegate.isEmpty()) {
            for (String eDel : aDelegate) {
                if (eDel != null && eDel.trim().length() > 0) {
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
     * @param element   the bpmn element
     * @param className name of the class
     * @param varName   name of the variable
     * @return collection if issues
     */
    private Collection<CheckerIssue> checkClassFileForVar(final BpmnElement element, final String className,
            final String varName) {

        final Collection<CheckerIssue> issues = new ArrayList<>();
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
                    {
                        hasMethod = true;
                        break;
                    }
                }

                if (!hasMethod) {
                    issues.add(IssueWriter.createIssueWithClassPath(rule, CriticalityEnum.WARNING, classPath, element,
                            String.format(Messages.getString("FieldInjectionChecker.6"), varName))); //$NON-NLS-1$
                }

            } catch (NoSuchFieldException | SecurityException e) {
                issues.add(IssueWriter.createIssueWithClassPath(rule, CriticalityEnum.WARNING, classPath, element,
                        String.format(Messages.getString("FieldInjectionChecker.7"), clazz.getSimpleName(),
                                //$NON-NLS-1$
                                varName)));
            }

        } catch (final ClassNotFoundException e) {
            LOGGER.warning("Class " + className + " does not exist." + e); //$NON-NLS-1$ //$NON-NLS-2$
        }

        return issues;
    }
}
