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
import de.viadee.bpm.vPAV.processing.CheckName;
import de.viadee.bpm.vPAV.processing.model.data.BpmnElement;
import de.viadee.bpm.vPAV.processing.model.data.CheckerIssue;
import de.viadee.bpm.vPAV.processing.model.data.CriticalityEnum;

/**
 * Class JavaDelegateChecker
 *
 * Checks a bpmn model, if code references (java delegates) for tasks have been set correctly.
 *
 */
public class JavaDelegateChecker extends AbstractElementChecker {

    public JavaDelegateChecker(final Rule rule, final BpmnScanner bpmnScanner) {
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
        String implementation = null;
        ArrayList<String> executionDelegate = new ArrayList<String>();
        ArrayList<String> executionClass = new ArrayList<String>();
        ArrayList<String> executionExpression = new ArrayList<String>();
        ArrayList<String> taskDelegate = new ArrayList<String>();
        ArrayList<String> taskClass = new ArrayList<String>();
        ArrayList<String> taskExpression = new ArrayList<String>();

        // read attributes from task
        if ((bpmnElement instanceof ServiceTask || bpmnElement instanceof BusinessRuleTask
                || bpmnElement instanceof SendTask)) {
        	implementationAttr = bpmnScanner.getImplementation(bpmnElement.getId());  
        }
            
        
        if (bpmnElement instanceof UserTask) {
            taskDelegate = bpmnScanner.getListener(bpmnElement.getId(), BpmnConstants.ATTR_DEL,
                    BpmnConstants.CAMUNDA_TASKLISTENER);
            taskClass = bpmnScanner.getListener(bpmnElement.getId(), BpmnConstants.ATTR_CLASS,
                    BpmnConstants.CAMUNDA_TASKLISTENER);
            taskExpression = bpmnScanner.getListener(bpmnElement.getId(), BpmnConstants.ATTR_EX,
                    BpmnConstants.CAMUNDA_TASKLISTENER);
        }

        if (bpmnElement.getElementType().getTypeName()
                .equals(BpmnModelConstants.BPMN_ELEMENT_INTERMEDIATE_THROW_EVENT)
                || bpmnElement.getElementType().getTypeName().equals(BpmnModelConstants.BPMN_ELEMENT_END_EVENT)) {
            final String tempImp = bpmnScanner.getEventImplementation(bpmnElement.getId());
            if (tempImp != null && tempImp.contains("=")) { //$NON-NLS-1$
                implementationAttr = tempImp.substring(0, tempImp.indexOf("=")).trim(); //$NON-NLS-1$
                implementation = tempImp.substring(tempImp.indexOf("=") + 1, tempImp.length()).replace("\"", "") //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                        .trim();
            }
        }

        executionDelegate = bpmnScanner.getListener(bpmnElement.getId(), BpmnConstants.ATTR_DEL,
                BpmnConstants.CAMUNDA_EXECUTIONLISTENER);
        executionClass = bpmnScanner.getListener(bpmnElement.getId(), BpmnConstants.ATTR_CLASS,
                BpmnConstants.CAMUNDA_EXECUTIONLISTENER);
        executionExpression = bpmnScanner.getListener(bpmnElement.getId(), BpmnConstants.ATTR_EX,
                BpmnConstants.CAMUNDA_EXECUTIONLISTENER);

        final String classAttr = bpmnElement.getAttributeValueNs(BpmnModelConstants.CAMUNDA_NS,
                BpmnConstants.ATTR_CLASS);
        final String delegateExprAttr = bpmnElement.getAttributeValueNs(BpmnModelConstants.CAMUNDA_NS,
                BpmnConstants.ATTR_DEL);
        final String exprAttr = bpmnElement.getAttributeValueNs(BpmnModelConstants.CAMUNDA_NS,
                BpmnConstants.ATTR_EX);
        final String typeAttr = bpmnElement.getAttributeValueNs(BpmnModelConstants.CAMUNDA_NS,
                BpmnModelConstants.CAMUNDA_ATTRIBUTE_TYPE);
        final String dmnAttr = bpmnElement.getAttributeValueNs(BpmnModelConstants.CAMUNDA_NS,
                BpmnModelConstants.CAMUNDA_ATTRIBUTE_DECISION_REF);

        if (implementationAttr != null && (bpmnElement instanceof ServiceTask || bpmnElement instanceof BusinessRuleTask
                || bpmnElement instanceof SendTask)) {
            // check if class is correct
            if (implementationAttr.equals(BpmnConstants.CAMUNDA_CLASS)) {
                if (classAttr == null || classAttr.trim().length() == 0) {
                    // Error, because no class has been configured
                    issues.addAll(IssueWriter.createIssue(rule, CriticalityEnum.ERROR, element,
                            String.format(Messages.getString("JavaDelegateChecker.5"), //$NON-NLS-1$
                                    bpmnElement.getAttributeValue(BpmnModelConstants.BPMN_ATTRIBUTE_NAME))));
                } else {
                    issues.addAll(checkClassFile(element, classAttr, false, false));
                }
            }

            // check if delegateExpression is correct
            else if (implementationAttr.equals(BpmnConstants.CAMUNDA_DEXPRESSION)) {
                if (delegateExprAttr == null || delegateExprAttr.trim().length() == 0) {
                    // Error, because no delegateExpression has been configured
                    issues.addAll(IssueWriter.createIssue(rule, CriticalityEnum.ERROR, element,
                            String.format(
                                    Messages.getString("JavaDelegateChecker.6"), //$NON-NLS-1$
                                    bpmnElement.getAttributeValue(BpmnModelConstants.BPMN_ATTRIBUTE_NAME))));
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
                                    issues.addAll(checkClassFile(element, classFile, false, false));
                                } else {
                                    // incorrect beanmapping
                                    issues.addAll(IssueWriter.createIssue(rule, CriticalityEnum.ERROR, element,
                                            String.format(
                                                    Messages.getString("JavaDelegateChecker.7"), //$NON-NLS-1$
                                                    delegateExprAttr)));
                                }
                            }
                        } else {
                            issues.addAll(checkClassFile(element, delegateExprAttr, false, false));
                        }
                    } else {
                        // check if class exists
                        issues.addAll(checkClassFile(element, delegateExprAttr, false, false));
                    }
                }
            }

            // check if external is correct
            else if (implementationAttr.equals(BpmnConstants.CAMUNDA_EXT)) {
                if (typeAttr == null || typeAttr.trim().length() == 0) {
                    // Error, because no delegateExpression has been configured
                    issues.addAll(IssueWriter.createIssue(rule, CriticalityEnum.ERROR, element,
                            String.format(Messages.getString("JavaDelegateChecker.8"), //$NON-NLS-1$
                                    bpmnElement.getAttributeValue(BpmnModelConstants.BPMN_ATTRIBUTE_NAME))));
                }
            }

            else if (implementationAttr.equals(BpmnConstants.IMPLEMENTATION))
                if (dmnAttr == null && classAttr == null && delegateExprAttr == null
                        && exprAttr == null && typeAttr == null) {
                    // No technical attributes have been added
                    issues.addAll(IssueWriter.createIssue(rule, CriticalityEnum.ERROR, element,
                            String.format(Messages.getString("JavaDelegateChecker.9"), //$NON-NLS-1$
                                    CheckName.checkName(bpmnElement))));
                }
        }

        // check implementations of BPMN_ELEMENT_INTERMEDIATE_THROW_EVENT and BPMN_ELEMENT_END_EVENT
        if (bpmnElement.getElementType().getTypeName().equals(BpmnModelConstants.BPMN_ELEMENT_INTERMEDIATE_THROW_EVENT)
                || bpmnElement.getElementType().getTypeName().equals(BpmnModelConstants.BPMN_ELEMENT_END_EVENT)) {

            if (implementationAttr != null && implementationAttr.equals(BpmnConstants.IMPLEMENTATION)) {
                issues.addAll(IssueWriter.createIssue(rule, CriticalityEnum.ERROR, element,
                        String.format(Messages.getString("JavaDelegateChecker.10"), //$NON-NLS-1$
                                bpmnElement.getElementType().getTypeName(),
                                bpmnElement.getAttributeValue(BpmnModelConstants.BPMN_ATTRIBUTE_ID))));

            } else if (implementationAttr != null && implementationAttr.equals(BpmnConstants.CAMUNDA_DEXPRESSION)) {
                // check validity of a bean
                if (RuntimeConfig.getInstance().getBeanMapping() != null) {
                    final TreeBuilder treeBuilder = new Builder();
                    final Tree tree = treeBuilder.build(implementation);
                    final Iterable<IdentifierNode> identifierNodes = tree.getIdentifierNodes();
                    // if beanMapping ${...} reference
                    if (identifierNodes.iterator().hasNext()) {
                        for (final IdentifierNode node : identifierNodes) {
                            final String classFile = RuntimeConfig.getInstance().getBeanMapping()
                                    .get(node.getName());
                            // correct beanmapping was found -> check if class exists
                            if (classFile != null && classFile.trim().length() > 0) {
                                issues.addAll(checkClassFile(element, classFile, false, false));
                            } else {
                                // incorrect beanmapping
                                issues.addAll(IssueWriter.createIssue(rule, CriticalityEnum.ERROR, element,
                                        String.format(Messages.getString("JavaDelegateChecker.11"), //$NON-NLS-1$
                                                implementation)));
                            }
                        }
                    } else {
                        issues.addAll(checkClassFile(element, implementation, false, false));
                    }
                } else {
                    // check if class exists
                    issues.addAll(checkClassFile(element, delegateExprAttr, false, false));
                }

            } else if (implementationAttr != null && implementationAttr.equals(BpmnConstants.CAMUNDA_CLASS)) {
                issues.addAll(checkClassFile(element, implementation, false, false));
            }

        }

        // checkListener
        if (executionClass != null || executionDelegate != null || executionExpression != null) {
            issues.addAll(checkListener(element, executionClass, executionDelegate, executionExpression, false));
        }
        if (taskClass != null || taskDelegate != null || taskExpression != null) {
            issues.addAll(checkListener(element, taskClass, taskDelegate, taskExpression, true));
        }
        return issues;
    }

    /**
     * Checks for JavaDelegates in Listeners
     *
     * @param element
     * @param aClass
     * @param aDelegate
     * @param aExpression
     * @param taskListener
     * @return issues
     */
    private Collection<CheckerIssue> checkListener(final BpmnElement element, ArrayList<String> aClass,
            ArrayList<String> aDelegate, ArrayList<String> aExpression, boolean taskListener) {
        final Collection<CheckerIssue> issues = new ArrayList<CheckerIssue>();
        String location = ""; //$NON-NLS-1$
        if (taskListener)
            location = BpmnConstants.TASK_LISTENER;
        else
            location = BpmnConstants.EXECUTION_LISTENER;

        // classes
        if (aClass == null || aClass.size() > 0) {
            for (String eClass : aClass) {
                if (eClass != null && eClass.trim().length() == 0) {
                    // Error, because no class has been configured
                    issues.addAll(IssueWriter.createIssue(rule, CriticalityEnum.ERROR, element,
                            String.format(Messages.getString("JavaDelegateChecker.13"), location))); //$NON-NLS-1$
                } else if (eClass != null) {
                    issues.addAll(checkClassFile(element, eClass, true, taskListener));
                }
            }
        }

        // delegateExpression
        if (aDelegate != null && aDelegate.size() > 0) {
            for (String eDel : aDelegate) {
                if (eDel == null || eDel.trim().length() == 0) {
                    // Error, because no delegateExpression has been configured
                    issues.addAll(IssueWriter.createIssue(rule, CriticalityEnum.ERROR, element,
                            String.format(Messages.getString("JavaDelegateChecker.14"), location))); //$NON-NLS-1$
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
                                    issues.addAll(checkClassFile(element, classFile, true, taskListener));
                                } else {
                                    // incorrect beanmapping
                                    issues.addAll(IssueWriter.createIssue(rule, CriticalityEnum.ERROR, element,
                                            String.format(
                                                    Messages.getString("JavaDelegateChecker.15"), //$NON-NLS-1$
                                                    eDel, location)));
                                }
                            }
                        } else {
                            issues.addAll(checkClassFile(element, eDel, true, taskListener));
                        }
                    } else {
                        // check if class exists
                        issues.addAll(checkClassFile(element, eDel, true, taskListener));
                    }
                }
            }
        }
        return issues;
    }

    /**
     * Check if class reference for a given element exists
     *
     * @param element
     * @param className
     * @param listener
     * @param taskListener
     * @return issues
     */
    private Collection<CheckerIssue> checkClassFile(final BpmnElement element, final String className,
            final boolean listener, final boolean taskListener) {

        final Collection<CheckerIssue> issues = new ArrayList<CheckerIssue>();
        final BaseElement bpmnElement = element.getBaseElement();
        final String classPath = className.replaceAll("\\.", "/") + ".java"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        String location = ""; //$NON-NLS-1$
        if (listener) {
            if (taskListener) {
                location = BpmnConstants.TASK_LISTENER;
            } else {
                location = BpmnConstants.EXECUTION_LISTENER;
            }
        }

        if (location.isEmpty()) {
            location = bpmnScanner.getImplementation(bpmnElement.getAttributeValue(BpmnConstants.ATTR_ID));
        }

        // If a class path has been found, check the correctness
        try {
            Class<?> clazz = RuntimeConfig.getInstance().getClassLoader().loadClass(className);

            // Checks, whether the correct interface was implemented
            Class<?> sClass = clazz.getSuperclass();
            boolean extendsSuperClass = false;
            if (!listener && sClass.getName().contains(BpmnConstants.SUPERCLASS_ABSTRACT_BPMN_ACTIVITY_BEHAVIOR)) {
                extendsSuperClass = true;
            }

            // Checks, whether the correct interface was implemented
            Class<?>[] interfaces = clazz.getInterfaces();
            boolean interfaceImplemented = false;
            for (final Class<?> _interface : interfaces) {
                if (!listener) {
                    if (_interface.getName().contains(BpmnConstants.INTERFACE_DEL)
                            || _interface.getName().contains(BpmnConstants.INTERFACE_SIGNALLABLE_ACTIVITY_BEHAVIOR)
                            || _interface.getName().contains(BpmnConstants.INTERFACE_ACTIVITY_BEHAVIOUR)) {
                        interfaceImplemented = true;
                        if (_interface.getName().contains(BpmnConstants.INTERFACE_ACTIVITY_BEHAVIOUR)
                                && !_interface.getName()
                                        .contains(BpmnConstants.INTERFACE_SIGNALLABLE_ACTIVITY_BEHAVIOR)) {
                            // ActivityBehavior is not a very good practice and should be avoided as much as possible
                            issues.add(
                                    IssueWriter.createIssueWithClassPath(rule, CriticalityEnum.INFO, classPath, element,
                                            String.format(
                                                    Messages.getString("JavaDelegateChecker.20"), //$NON-NLS-1$
                                                    clazz.getSimpleName(), location)));
                        }
                    }
                } else {
                    if (taskListener && _interface.getName().contains(BpmnConstants.INTERFACE_TASK_LISTENER)) {
                        interfaceImplemented = true;
                    } else if (_interface.getName().contains(BpmnConstants.INTERFACE_EXECUTION_LISTENER)
                            || _interface.getName().contains(BpmnConstants.INTERFACE_DEL)) {
                        interfaceImplemented = true;
                    }
                }
            }

            if (interfaceImplemented == false && extendsSuperClass == false) {
                // class implements not the interface "JavaDelegate"
                issues.add(IssueWriter.createIssueWithClassPath(rule, CriticalityEnum.ERROR, classPath, element,
                        String.format(Messages.getString("JavaDelegateChecker.21"), //$NON-NLS-1$
                                clazz.getSimpleName(), location)));
            }

        } catch (final ClassNotFoundException e) {
            // Throws an error, if the class was not found
            if (className == null || !className.isEmpty()) {
                issues.add(IssueWriter.createIssueWithClassPath(rule, CriticalityEnum.ERROR, classPath, element,
                        String.format(Messages.getString("JavaDelegateChecker.22"), //$NON-NLS-1$
                                className.substring(className.lastIndexOf('.') + 1), location)));
            } else {
                issues.add(IssueWriter.createIssueWithClassPath(rule, CriticalityEnum.ERROR, classPath, element,
                        String.format(Messages.getString("JavaDelegateChecker.23"), CheckName.checkName(bpmnElement)))); //$NON-NLS-1$
            }
        }

        return issues;
    }
}
