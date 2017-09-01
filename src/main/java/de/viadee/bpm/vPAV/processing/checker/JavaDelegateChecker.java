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
 * THIS SOFTWARE IS PROVIDED BY <{owner}> ''AS IS'' AND ANY
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

import javax.xml.parsers.ParserConfigurationException;

import org.camunda.bpm.model.bpmn.impl.BpmnModelConstants;
import org.camunda.bpm.model.bpmn.instance.BaseElement;
import org.camunda.bpm.model.bpmn.instance.BusinessRuleTask;
import org.camunda.bpm.model.bpmn.instance.SendTask;
import org.camunda.bpm.model.bpmn.instance.ServiceTask;
import org.camunda.bpm.model.bpmn.instance.UserTask;
import org.xml.sax.SAXException;

import de.odysseus.el.tree.IdentifierNode;
import de.odysseus.el.tree.Tree;
import de.odysseus.el.tree.TreeBuilder;
import de.odysseus.el.tree.impl.Builder;
import de.viadee.bpm.vPAV.BPMNScanner;
import de.viadee.bpm.vPAV.RuntimeConfig;
import de.viadee.bpm.vPAV.config.model.Rule;
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

    private final String path;

    private final String c_executionList = "camunda:executionListener";

    private final String c_taskList = "camunda:taskListener";

    private final String c_class = "camunda:class";

    private final String c_del = "camunda:delegateExpression";

    private final String c_ex = "camunda:expression";

    private final String c_type = "camunda:type";

    private final String impl = "implementation";

    private final String attr_class = "class";

    private final String attr_del = "delegateExpression";

    private final String attr_ex = "expression";

    private final String attr_decR = "decisionRef";

    private final String attr_type = "type";

    public JavaDelegateChecker(final Rule rule, final String path) {
        super(rule);
        this.path = path;
    }

    @Override
    public Collection<CheckerIssue> check(final BpmnElement element) {

        final Collection<CheckerIssue> issues = new ArrayList<CheckerIssue>();
        final BaseElement bpmnElement = element.getBaseElement();
        final BPMNScanner scan;
        String implementationAttr = null;
        ArrayList<String> executionDelegate = new ArrayList<String>();
        ArrayList<String> executionClass = new ArrayList<String>();
        ArrayList<String> executionExpression = new ArrayList<String>();
        ArrayList<String> taskDelegate = new ArrayList<String>();
        ArrayList<String> taskClass = new ArrayList<String>();
        ArrayList<String> taskExpression = new ArrayList<String>();

        try {
            scan = new BPMNScanner();
            // read attributes from task
            if ((bpmnElement instanceof ServiceTask || bpmnElement instanceof BusinessRuleTask
                    || bpmnElement instanceof SendTask))
                implementationAttr = scan.getImplementation(path, bpmnElement.getId());

            if (bpmnElement instanceof UserTask) {
                taskDelegate = scan.getListener(path, bpmnElement.getId(), attr_del, c_taskList);
                taskClass = scan.getListener(path, bpmnElement.getId(), attr_class, c_taskList);
                taskExpression = scan.getListener(path, bpmnElement.getId(), attr_ex, c_taskList);
            }

            executionDelegate = scan.getListener(path, bpmnElement.getId(), attr_del, c_executionList);
            executionClass = scan.getListener(path, bpmnElement.getId(), attr_class, c_executionList);
            executionExpression = scan.getListener(path, bpmnElement.getId(), attr_ex, c_executionList);

        } catch (SAXException | IOException | ParserConfigurationException e) {
            e.printStackTrace();
        }

        final String classAttr = bpmnElement.getAttributeValueNs(BpmnModelConstants.CAMUNDA_NS,
                attr_class);
        final String delegateExprAttr = bpmnElement.getAttributeValueNs(BpmnModelConstants.CAMUNDA_NS,
                attr_del);
        final String exprAttr = bpmnElement.getAttributeValueNs(BpmnModelConstants.CAMUNDA_NS,
                attr_ex);
        final String typeAttr = bpmnElement.getAttributeValueNs(BpmnModelConstants.CAMUNDA_NS, attr_type);
        final String dmnAttr = bpmnElement.getAttributeValueNs(BpmnModelConstants.CAMUNDA_NS,
                attr_decR);

        if (implementationAttr != null && (bpmnElement instanceof ServiceTask || bpmnElement instanceof BusinessRuleTask
                || bpmnElement instanceof SendTask)) {
            // check if class is correct
            if (implementationAttr.equals(c_class)) {
                if (classAttr == null || classAttr.trim().length() == 0) {
                    // Error, because no class has been configured
                    issues.add(new CheckerIssue(rule.getName(), CriticalityEnum.ERROR,
                            element.getProcessdefinition(), null, bpmnElement.getAttributeValue("id"),
                            bpmnElement.getAttributeValue("name"), null, null, null,
                            "task with no class name"));
                } else {
                    issues.addAll(checkClassFile(element, classAttr, false, false));
                }
            }

            // check if delegateExpression is correct
            else if (implementationAttr.equals(c_del)) {
                if (delegateExprAttr == null || delegateExprAttr.trim().length() == 0) {
                    // Error, because no delegateExpression has been configured
                    issues.add(new CheckerIssue(rule.getName(), CriticalityEnum.ERROR,
                            element.getProcessdefinition(), null, bpmnElement.getAttributeValue("id"),
                            bpmnElement.getAttributeValue("name"), null, null, null,
                            "task with no delegate expression"));
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
                                    issues.add(new CheckerIssue(rule.getName(), CriticalityEnum.WARNING,
                                            element.getProcessdefinition(), null, bpmnElement.getAttributeValue("id"),
                                            bpmnElement.getAttributeValue("name"), null, null, null,
                                            "Couldn't find correct beanmapping for delegate expression: "
                                                    + delegateExprAttr));
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

            // check if expression is correct
            else if (implementationAttr.equals(c_ex)) {
                if (exprAttr == null || exprAttr.trim().length() == 0) {
                    // Error, because no delegateExpression has been configured
                    issues.add(new CheckerIssue(rule.getName(), CriticalityEnum.ERROR,
                            element.getProcessdefinition(), null, bpmnElement.getAttributeValue("id"),
                            bpmnElement.getAttributeValue("name"), null, null, null,
                            "task with no expression"));
                }
                // expression überprüfbar?
            }

            // check if external is correct
            else if (implementationAttr.equals(c_type)) {
                if (typeAttr == null || typeAttr.trim().length() == 0) {
                    // Error, because no delegateExpression has been configured
                    issues.add(new CheckerIssue(rule.getName(), CriticalityEnum.ERROR,
                            element.getProcessdefinition(), null, bpmnElement.getAttributeValue("id"),
                            bpmnElement.getAttributeValue("name"), null, null, null,
                            "task with no external topic"));
                }
            }

            else if (implementationAttr.equals(impl))
                if (dmnAttr == null && classAttr == null && delegateExprAttr == null
                        && exprAttr == null && typeAttr == null) {
                    // No technical attributes have been added
                    issues.add(new CheckerIssue(rule.getName(), CriticalityEnum.WARNING,
                            element.getProcessdefinition(), null, bpmnElement.getAttributeValue("id"),
                            bpmnElement.getAttributeValue("name"), null, null, null,
                            "task '" + CheckName.checkName(bpmnElement) + "' with no code reference yet"));
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

    private Collection<CheckerIssue> checkListener(final BpmnElement element, ArrayList<String> aClass,
            ArrayList<String> aDelegate, ArrayList<String> aExpression, boolean taskListener) {
        final Collection<CheckerIssue> issues = new ArrayList<CheckerIssue>();
        final BaseElement bpmnElement = element.getBaseElement();
        String location = "";
        if (taskListener)
            location = " taskListener";
        else
            location = " executionListener";

        // classes
        if (aClass == null || aClass.size() > 0) {
            for (String eClass : aClass) {
                if (eClass != null && eClass.trim().length() == 0) {
                    // Error, because no class has been configured
                    issues.add(new CheckerIssue(rule.getName(), CriticalityEnum.ERROR,
                            element.getProcessdefinition(), null, bpmnElement.getAttributeValue("id"),
                            bpmnElement.getAttributeValue("name"), null, null, null,
                            "task with no class name in" + location));
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
                    issues.add(new CheckerIssue(rule.getName(), CriticalityEnum.ERROR,
                            element.getProcessdefinition(), null, bpmnElement.getAttributeValue("id"),
                            bpmnElement.getAttributeValue("name"), null, null, null,
                            "task with no delegate expression in" + location));
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
                                    issues.add(new CheckerIssue(rule.getName(), CriticalityEnum.WARNING,
                                            element.getProcessdefinition(), null,
                                            bpmnElement.getAttributeValue("id"),
                                            bpmnElement.getAttributeValue("name"), null, null, null,
                                            "Couldn't find correct beanmapping for delegate expression "
                                                    + eDel + "  in" + location));
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

        // expression
        if (aExpression != null && aExpression.size() > 0) {
            for (String eEx : aExpression) {
                if (eEx == null || eEx.trim().length() == 0) {
                    // Error, because no delegateExpression has been configured
                    issues.add(new CheckerIssue(rule.getName(), CriticalityEnum.ERROR,
                            element.getProcessdefinition(), null, bpmnElement.getAttributeValue("id"),
                            bpmnElement.getAttributeValue("name"), null, null, null,
                            "task with no expression in" + location));
                }
            }
        }

        return issues;
    }

    private Collection<CheckerIssue> checkClassFile(final BpmnElement element, final String className,
            final boolean listener, final boolean taskListener) {

        final Collection<CheckerIssue> issues = new ArrayList<CheckerIssue>();
        final BaseElement bpmnElement = element.getBaseElement();
        final String classPath = className.replaceAll("\\.", "/") + ".java";
        String location = "";
        if (listener)
            if (taskListener)
                location = "in taskListener ";
            else
                location = "in executionListener ";

        // If a class path has been found, check the correctness
        try {
            Class<?> clazz = RuntimeConfig.getInstance().getClassLoader().loadClass(className);

            // Checks, whether the correct interface was implemented
            Class<?> sClass = clazz.getSuperclass();
            boolean extendsSuperClass = false;
            if (!listener && sClass.getName().contains("AbstractBpmnActivityBehavior")) {
                extendsSuperClass = true;
            }

            // Checks, whether the correct interface was implemented
            Class<?>[] interfaces = clazz.getInterfaces();
            boolean interfaceImplemented = false;
            for (final Class<?> _interface : interfaces) {
                if (!listener) {
                    if (_interface.getName().contains("JavaDelegate")
                            || _interface.getName().contains("SignallableActivityBehavior")) {
                        interfaceImplemented = true;
                    }
                } else {
                    if (taskListener && _interface.getName().contains("TaskListener")) {
                        interfaceImplemented = true;
                    } else if (_interface.getName().contains("ExecutionListener")
                            || _interface.getName().contains("JavaDelegate")) {
                        interfaceImplemented = true;
                    }
                }
            }

            if (interfaceImplemented == false && extendsSuperClass == false) {
                // class implements not the interface "JavaDelegate"
                issues.add(new CheckerIssue(rule.getName(), CriticalityEnum.ERROR,
                        element.getProcessdefinition(), classPath, bpmnElement.getAttributeValue("id"),
                        bpmnElement.getAttributeValue("name"), null, null, null,
                        "class '" + clazz.getSimpleName() + "' " + location
                                + "does not implement/extends the correct interface/class"));
            }

        } catch (final ClassNotFoundException e) {
            // Throws an error, if the class was not found
            issues.add(new CheckerIssue(rule.getName(), CriticalityEnum.ERROR,
                    element.getProcessdefinition(), classPath, bpmnElement.getAttributeValue("id"),
                    bpmnElement.getAttributeValue("name"), null, null, null,
                    "class '" + className.substring(className.lastIndexOf('.') + 1) + "' " + location
                            + "not found"));
        }

        return issues;
    }
}
