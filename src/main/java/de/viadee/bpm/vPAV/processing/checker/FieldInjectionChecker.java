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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Logger;

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
import de.viadee.bpm.vPAV.BPMNScanner;
import de.viadee.bpm.vPAV.RuntimeConfig;
import de.viadee.bpm.vPAV.config.model.Rule;
import de.viadee.bpm.vPAV.processing.model.data.BpmnElement;
import de.viadee.bpm.vPAV.processing.model.data.CheckerIssue;
import de.viadee.bpm.vPAV.processing.model.data.CriticalityEnum;

/**
 * Class JavaDelegateChecker
 *
 * Checks a bpmn model, if code references (java delegates) for tasks have been set correctly.
 *
 */
public class FieldInjectionChecker extends AbstractElementChecker {

    private final String attr_class = "class";

    private final String attr_del = "delegateExpression";

    private final String c_executionList = "camunda:executionListener";

    private final String c_class = "camunda:class";

    private final String c_del = "camunda:delegateExpression";

    private final String c_taskList = "camunda:taskListener";

    private final String attr_ex = "expression";

    private final String fixedValue = "org.camunda.bpm.engine.impl.el.FixedValue";

    private final String expression = "org.camunda.bpm.engine.delegate.Expression";

    public static Logger logger = Logger.getLogger(FieldInjectionChecker.class.getName());

    public FieldInjectionChecker(final Rule rule, final BPMNScanner bpmnScanner) {
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
            taskDelegate = bpmnScanner.getListener(bpmnElement.getId(), attr_del, c_taskList);
            taskClass = bpmnScanner.getListener(bpmnElement.getId(), attr_class, c_taskList);
            taskExpression = bpmnScanner.getListener(bpmnElement.getId(), attr_ex, c_taskList);
        }

        executionDelegate = bpmnScanner.getListener(bpmnElement.getId(), attr_del, c_executionList);
        executionClass = bpmnScanner.getListener(bpmnElement.getId(), attr_class, c_executionList);
        executionExpression = bpmnScanner.getListener(bpmnElement.getId(), attr_ex, c_executionList);

        final ArrayList<String> fieldInjectionVarNames = bpmnScanner.getFieldInjectionVarName(bpmnElement.getId());
        final String classAttr = bpmnElement.getAttributeValueNs(BpmnModelConstants.CAMUNDA_NS,
                attr_class);
        final String delegateExprAttr = bpmnElement.getAttributeValueNs(BpmnModelConstants.CAMUNDA_NS,
                attr_del);

        if (implementationAttr != null && !fieldInjectionVarNames.isEmpty() && fieldInjectionVarNames != null
                && (bpmnElement instanceof ServiceTask || bpmnElement instanceof BusinessRuleTask
                        || bpmnElement instanceof SendTask)) {
            // check if class is correct
            if (implementationAttr.equals(c_class)) {
                if (classAttr == null || classAttr.trim().length() == 0) {
                } else {
                    for (String fieldInjectionVarName : fieldInjectionVarNames)
                        issues.addAll(checkClassFileForVar(element, classAttr, fieldInjectionVarName));
                }
            }

            // check if delegateExpression is correct
            else if (implementationAttr.equals(c_del)) {
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
        if (aDelegate != null && aDelegate.size() > 0) {
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
        final BaseElement bpmnElement = element.getBaseElement();
        final String classPath = className.replaceAll("\\.", "/") + ".java";

        try {
            Class<?> clazz = RuntimeConfig.getInstance().getClassLoader().loadClass(className);
            try {
                Field field = clazz.getDeclaredField(varName);

                if (!field.getType().getName().equals(fixedValue)
                        && !field.getType().getName().equals(expression))
                    issues.add(new CheckerIssue(rule.getName(), CriticalityEnum.WARNING,
                            element.getProcessdefinition(), classPath, bpmnElement.getAttributeValue("id"),
                            bpmnElement.getAttributeValue("name"), null, null, null,
                            "the type of the variable '" + varName + "' is incorrect"));

                if (!Modifier.isPublic(field.getModifiers()))
                    issues.add(new CheckerIssue(rule.getName(), CriticalityEnum.WARNING,
                            element.getProcessdefinition(), classPath, bpmnElement.getAttributeValue("id"),
                            bpmnElement.getAttributeValue("name"), null, null, null,
                            "the variable '" + varName + "' should be public"));

                Method[] methods = clazz.getMethods();
                boolean hasMethod = false;
                for (Method method : methods) {
                    if (method.getName().toLowerCase().contains("set" + varName.toLowerCase()))
                        hasMethod = true;
                }

                if (!hasMethod)
                    issues.add(new CheckerIssue(rule.getName(), CriticalityEnum.WARNING,
                            element.getProcessdefinition(), classPath, bpmnElement.getAttributeValue("id"),
                            bpmnElement.getAttributeValue("name"), null, null, null,
                            "no setter found for variable '" + varName + "'"));

            } catch (NoSuchFieldException | SecurityException e) {
                issues.add(new CheckerIssue(rule.getName(), CriticalityEnum.WARNING,
                        element.getProcessdefinition(), classPath, bpmnElement.getAttributeValue("id"),
                        bpmnElement.getAttributeValue("name"), null, null, null,
                        "class '" + clazz.getSimpleName() + "' does not declared the variable '" + varName + "'"));
            }

        } catch (final ClassNotFoundException e) {
            logger.warning("Class " + className + " does not exist");
        }

        return issues;
    }
}
