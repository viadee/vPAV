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
import de.viadee.bpm.vPAV.processing.CheckName;
import de.viadee.bpm.vPAV.processing.code.flow.BpmnElement;
import de.viadee.bpm.vPAV.processing.model.data.CheckerIssue;
import de.viadee.bpm.vPAV.processing.model.data.CriticalityEnum;
import org.camunda.bpm.model.bpmn.impl.BpmnModelConstants;
import org.camunda.bpm.model.bpmn.instance.*;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaExecutionListener;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaTaskListener;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Class JavaDelegateChecker
 * <p>
 * Checks a bpmn model, if code references (java delegates) for tasks have been
 * set correctly.
 */
public class JavaDelegateChecker extends AbstractElementChecker {

    public JavaDelegateChecker(final Rule rule) {
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
        final ArrayList<String> executionExpression = new ArrayList<>();
        final ArrayList<String> taskDelegate = new ArrayList<>();
        final ArrayList<String> taskClass = new ArrayList<>();
        final ArrayList<String> taskExpression = new ArrayList<>();

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
                taskExpression.add(((CamundaTaskListener) listener).getCamundaExpression());
            });
        }

        if (bpmnElement instanceof IntermediateThrowEvent
                || bpmnElement instanceof EndEvent) {
            final Map.Entry<String, String> tempImp = BpmnScanner.getEventImplementation(bpmnElement);
            if (tempImp != null) { //$NON-NLS-1$
                HashMap<String, String> tempMap = new HashMap<>();
                tempMap.put(tempImp.getKey(), tempImp.getValue());
                implementationAttr = tempMap.entrySet().iterator().next();
            }
        }

        ArrayList<ModelElementInstance> executionListener = BpmnScanner
                .getListener(bpmnElement,
                        BpmnConstants.CAMUNDA_EXECUTION_LISTENER);
        executionListener.forEach(listener -> {
            executionDelegate.add(((CamundaExecutionListener) listener).getCamundaDelegateExpression());
            executionClass.add(((CamundaExecutionListener) listener).getCamundaClass());
            executionExpression.add(((CamundaExecutionListener) listener).getCamundaExpression());
        });

        if (implementationAttr != null && (bpmnElement instanceof ServiceTask || bpmnElement instanceof BusinessRuleTask
                || bpmnElement instanceof SendTask)) {
            // check if class is correct
            switch (implementationAttr.getKey()) {
                case BpmnConstants.CAMUNDA_CLASS:
                    if (implementationAttr.getValue() == null || implementationAttr.getValue().trim().length() == 0) {
                        // Error, because no class has been configured
                        issues.addAll(IssueWriter.createIssue(rule, CriticalityEnum.ERROR, element,
                                String.format(Messages.getString("JavaDelegateChecker.5"), //$NON-NLS-1$
                                        bpmnElement.getAttributeValue(BpmnModelConstants.BPMN_ATTRIBUTE_NAME))));
                    } else {
                        issues.addAll(checkClassFile(element, implementationAttr.getValue(), false, false));
                    }
                    break;

                // check if delegateExpression is correct
                case BpmnConstants.CAMUNDA_DEXPRESSION:
                    if (implementationAttr.getValue() == null || implementationAttr.getValue().trim().length() == 0) {
                        // Error, because no delegateExpression has been configured
                        issues.addAll(IssueWriter.createIssue(rule, CriticalityEnum.ERROR, element,
                                String.format(Messages.getString("JavaDelegateChecker.6"), //$NON-NLS-1$
                                        bpmnElement.getAttributeValue(BpmnModelConstants.BPMN_ATTRIBUTE_NAME))));
                    } else {
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
                                        issues.addAll(checkClassFile(element, classFile, false, false));
                                    } else {
                                        // incorrect beanmapping
                                        issues.addAll(IssueWriter.createIssue(rule, CriticalityEnum.ERROR, element,
                                                String.format(Messages.getString("JavaDelegateChecker.7"), //$NON-NLS-1$
                                                        implementationAttr.getValue())));
                                    }
                                }
                            } else {
                                issues.addAll(checkClassFile(element, implementationAttr.getValue(), false, false));
                            }
                        } else {
                            // check if class exists
                            issues.addAll(checkClassFile(element, implementationAttr.getValue(), false, false));
                        }
                    }
                    break;

                // check if external is correct
                case BpmnConstants.CAMUNDA_EXT:
                    if (implementationAttr.getValue() == null || implementationAttr.getValue().trim().length() == 0) {
                        // Error, because no delegateExpression has been configured
                        issues.addAll(IssueWriter.createIssue(rule, CriticalityEnum.ERROR, element,
                                String.format(Messages.getString("JavaDelegateChecker.8"), //$NON-NLS-1$
                                        bpmnElement.getAttributeValue(BpmnModelConstants.BPMN_ATTRIBUTE_NAME))));
                    }
                    break;
                case BpmnConstants.IMPLEMENTATION:
                    if (implementationAttr.getValue() == null || implementationAttr.getValue().isEmpty()) {
                        // No technical attributes have been added
                        issues.addAll(IssueWriter.createIssue(rule, CriticalityEnum.ERROR, element,
                                String.format(Messages.getString("JavaDelegateChecker.9"), //$NON-NLS-1$
                                        CheckName.checkName(bpmnElement))));
                    }
                    break;
            }
        }

        // check implementations of BPMN_ELEMENT_INTERMEDIATE_THROW_EVENT and
        // BPMN_ELEMENT_END_EVENT
        if (bpmnElement instanceof  IntermediateThrowEvent
                || bpmnElement instanceof EndEvent) {

            if (implementationAttr != null && implementationAttr.getKey().equals(BpmnConstants.IMPLEMENTATION)) {
                issues.addAll(IssueWriter.createIssue(rule, CriticalityEnum.ERROR, element,
                        String.format(Messages.getString("JavaDelegateChecker.10"), //$NON-NLS-1$
                                bpmnElement.getElementType().getTypeName(),
                                bpmnElement.getAttributeValue(BpmnModelConstants.BPMN_ATTRIBUTE_ID))));

            } else if (implementationAttr != null && implementationAttr.getKey()
                    .equals(BpmnConstants.CAMUNDA_DEXPRESSION)) {
                // check validity of a bean
                if (RuntimeConfig.getInstance().getBeanMapping() != null) {
                    final TreeBuilder treeBuilder = new Builder();
                    final Tree tree = treeBuilder.build(implementationAttr.getValue());
                    final Iterable<IdentifierNode> identifierNodes = tree.getIdentifierNodes();
                    // if beanMapping ${...} reference
                    if (identifierNodes.iterator().hasNext()) {
                        for (final IdentifierNode node : identifierNodes) {
                            final String classFile = RuntimeConfig.getInstance().getBeanMapping().get(node.getName());
                            // correct beanmapping was found -> check if class exists
                            if (classFile != null && classFile.trim().length() > 0) {
                                issues.addAll(checkClassFile(element, classFile, false, false));
                            } else {
                                // incorrect beanmapping
                                issues.addAll(IssueWriter.createIssue(rule, CriticalityEnum.ERROR, element,
                                        String.format(Messages.getString("JavaDelegateChecker.11"), //$NON-NLS-1$
                                                implementationAttr.getValue())));
                            }
                        }
                    } else {
                        issues.addAll(checkClassFile(element, implementationAttr.getValue(), false, false));
                    }
                } else {
                    // check if class exists
                    issues.addAll(checkClassFile(element, implementationAttr.getValue(), false, false));
                }

            } else if (implementationAttr != null && implementationAttr.getKey().equals(BpmnConstants.CAMUNDA_CLASS)) {
                issues.addAll(checkClassFile(element, implementationAttr.getValue(), false, false));
            }

        }

        // checkListener
        if (!executionClass.isEmpty() || !executionDelegate.isEmpty() || !executionExpression.isEmpty()) {
            issues.addAll(checkListener(element, executionClass, executionDelegate, false));
        }
        if (!taskClass.isEmpty() || !taskDelegate.isEmpty() || !taskExpression.isEmpty()) {
            issues.addAll(checkListener(element, taskClass, taskDelegate,true));
        }
        return issues;
    }

    /**
     * Checks for JavaDelegates in Listeners
     *
     * @param element
     * @param aClass
     * @param aDelegate
     * @param taskListener
     * @return issues
     */
    private Collection<CheckerIssue> checkListener(final BpmnElement element, ArrayList<String> aClass,
            ArrayList<String> aDelegate, boolean taskListener) {
        final Collection<CheckerIssue> issues = new ArrayList<>();
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
                } else {
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
                                            String.format(Messages.getString("JavaDelegateChecker.15"), //$NON-NLS-1$
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
     * @param element      Current BPMN Element
     * @param className    Classname referenced in the element
     * @param listener     Indicates whether the element is a listener
     * @param taskListener Indicates whether the element is a tasklistener
     * @return issues
     */
    private Collection<CheckerIssue> checkClassFile(final BpmnElement element, final String className,
            final boolean listener, final boolean taskListener) {

        final Collection<CheckerIssue> issues = new ArrayList<>();
        final BaseElement bpmnElement = element.getBaseElement();
        final String classPath = className.replaceAll("\\.", "/") + ".java"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        HashMap<String, String> tempMap = new HashMap<>();
        Map.Entry<String, String> location; //$NON-NLS-1$
        if (listener) {
            if (taskListener) {
                tempMap.put(BpmnConstants.TASK_LISTENER, "");
            } else {
                tempMap.put(BpmnConstants.EXECUTION_LISTENER, "");
            }
        }

        if (tempMap.isEmpty()) {
            location = BpmnScanner.getImplementation(bpmnElement.getModelInstance()
                    .getModelElementById(bpmnElement.getAttributeValue(BpmnConstants.ATTR_ID)));
        } else {
            location = tempMap.entrySet().iterator().next();
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
                        if (_interface.getName().contains(BpmnConstants.INTERFACE_ACTIVITY_BEHAVIOUR) && !_interface
                                .getName().contains(BpmnConstants.INTERFACE_SIGNALLABLE_ACTIVITY_BEHAVIOR)) {
                            // ActivityBehavior is not a very good practice and should be avoided as much as
                            // possible
                            issues.add(IssueWriter.createIssueWithClassPath(rule, CriticalityEnum.INFO, classPath,
                                    element, String.format(Messages.getString("JavaDelegateChecker.20"), //$NON-NLS-1$
                                            clazz.getSimpleName(), location.getKey())));
                        }
                    } else {
                        interfaceImplemented = checkTransitiveInterfaces(_interface, false);
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

            if (!interfaceImplemented && !extendsSuperClass) {
                // class implements not the interface "JavaDelegate"
                issues.add(IssueWriter.createIssueWithClassPath(rule, CriticalityEnum.ERROR, classPath, element,
                        String.format(Messages.getString("JavaDelegateChecker.21"), //$NON-NLS-1$
                                clazz.getSimpleName(), location.getKey())));
            }

        } catch (final ClassNotFoundException e) {
            // Throws an error, if the class was not found
            if (!className.isEmpty()) {
                issues.add(IssueWriter.createIssueWithClassPath(rule, CriticalityEnum.ERROR, classPath, element,
                        String.format(Messages.getString("JavaDelegateChecker.22"), //$NON-NLS-1$
                                className.substring(className.lastIndexOf('.') + 1), location.getKey())));
            } else {
                issues.add(IssueWriter.createIssueWithClassPath(rule, CriticalityEnum.ERROR, classPath, element,
                        String.format(Messages.getString("JavaDelegateChecker.23"),
                                CheckName.checkName(bpmnElement)))); //$NON-NLS-1$
            }
        }

        return issues;
    }

    /**
     * Recursively checks for the correct interface implementation of a given
     * delegate
     *
     * @param _interface          Current interface
     * @param implementsInterface Bool that indicates whether the JavaDelegate Interface is
     *                            implemented
     * @return True/false
     */
    private boolean checkTransitiveInterfaces(final Class<?> _interface, boolean implementsInterface) {
        if (_interface.getSimpleName().equals(BpmnConstants.INTERFACE_DEL)) {
            implementsInterface = true;
        } else {
            for (Class<?> superInterface : _interface.getInterfaces()) {
                implementsInterface = checkTransitiveInterfaces(superInterface, implementsInterface);
            }
        }
        return implementsInterface;
    }
}
