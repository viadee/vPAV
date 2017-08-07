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
package de.viadee.bpm.vPAV.processing;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.el.ELException;

import org.apache.commons.io.IOUtils;
import org.camunda.bpm.model.bpmn.Query;
import org.camunda.bpm.model.bpmn.impl.BpmnModelConstants;
import org.camunda.bpm.model.bpmn.instance.BaseElement;
import org.camunda.bpm.model.bpmn.instance.BpmnModelElementInstance;
import org.camunda.bpm.model.bpmn.instance.BusinessRuleTask;
import org.camunda.bpm.model.bpmn.instance.CallActivity;
import org.camunda.bpm.model.bpmn.instance.CompletionCondition;
import org.camunda.bpm.model.bpmn.instance.ConditionExpression;
import org.camunda.bpm.model.bpmn.instance.ExtensionElements;
import org.camunda.bpm.model.bpmn.instance.LoopCardinality;
import org.camunda.bpm.model.bpmn.instance.LoopCharacteristics;
import org.camunda.bpm.model.bpmn.instance.Script;
import org.camunda.bpm.model.bpmn.instance.ScriptTask;
import org.camunda.bpm.model.bpmn.instance.SendTask;
import org.camunda.bpm.model.bpmn.instance.SequenceFlow;
import org.camunda.bpm.model.bpmn.instance.ServiceTask;
import org.camunda.bpm.model.bpmn.instance.UserTask;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaExecutionListener;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaFormData;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaFormField;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaIn;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaOut;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaScript;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaTaskListener;
import org.camunda.bpm.model.dmn.Dmn;
import org.camunda.bpm.model.dmn.DmnModelInstance;
import org.camunda.bpm.model.dmn.instance.Decision;
import org.camunda.bpm.model.dmn.instance.InputExpression;
import org.camunda.bpm.model.dmn.instance.Output;
import org.camunda.bpm.model.dmn.instance.Text;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;

import de.odysseus.el.tree.IdentifierNode;
import de.odysseus.el.tree.Tree;
import de.odysseus.el.tree.TreeBuilder;
import de.odysseus.el.tree.impl.Builder;
import de.viadee.bpm.vPAV.RuntimeConfig;
import de.viadee.bpm.vPAV.processing.model.data.BpmnElement;
import de.viadee.bpm.vPAV.processing.model.data.ElementChapter;
import de.viadee.bpm.vPAV.processing.model.data.KnownElementFieldType;
import de.viadee.bpm.vPAV.processing.model.data.ProcessVariable;
import de.viadee.bpm.vPAV.processing.model.data.VariableOperation;

/**
 * search process variables for an bpmn element
 *
 */
public final class ProcessVariableReader {

    private final Map<String, String> decisionRefToPathMap;

    public ProcessVariableReader(final Map<String, String> decisionRefToPathMap) {
        this.decisionRefToPathMap = decisionRefToPathMap;
    }

    /**
     * Examining an bpmn element for variables
     *
     * @return variables
     */
    public Map<String, ProcessVariable> getVariablesFromElement(final BpmnElement element) {

        final Map<String, ProcessVariable> processVariables = new HashMap<String, ProcessVariable>();

        // 1) Search variables in task
        processVariables.putAll(getVariablesFromTask(element));
        // 2) Search variables in sequence flow
        processVariables.putAll(searchVariablesFromSequenceFlow(element));
        // 3) Search variables in ExtensionElements
        processVariables.putAll(searchExtensionsElements(element));

        return processVariables;
    }

    /**
     * Analyse bpmn extension elements for variables
     *
     * @param element
     * @return variables
     */
    private Map<String, ProcessVariable> searchExtensionsElements(final BpmnElement element) {

        final Map<String, ProcessVariable> processVariables = new HashMap<String, ProcessVariable>();
        final BaseElement baseElement = element.getBaseElement();
        final BpmnModelElementInstance scopeElement = baseElement.getScope();
        String scopeElementId = null;
        if (scopeElement != null) {
            scopeElementId = scopeElement.getAttributeValue("id");
        }
        final ExtensionElements extensionElements = baseElement.getExtensionElements();
        if (extensionElements != null) {
            // 1) Search in Execution Listeners
            processVariables.putAll(
                    getVariablesFromExecutionListener(element, extensionElements, scopeElementId));

            // 2) Search in Task Listeners
            processVariables
                    .putAll(getVariablesFromTaskListener(element, extensionElements, scopeElementId));

            // 3) Search in Form Data
            processVariables.putAll(getVariablesFromFormData(element, extensionElements, scopeElementId));

            // 4) Search in Input/Output-Associations (Call Activities)
            processVariables.putAll(
                    searchVariablesInInputOutputExtensions(element, extensionElements, scopeElementId));
        }

        return processVariables;
    }

    /**
     * get process variables from execution listeners
     *
     * @param extensionElements
     * @param processdefinition
     * @param elementId
     * @param cl
     *            ClassLoader
     * @return variables
     */
    private Map<String, ProcessVariable> getVariablesFromExecutionListener(final BpmnElement element,
            final ExtensionElements extensionElements, final String scopeId) {

        final Map<String, ProcessVariable> processVariables = new HashMap<String, ProcessVariable>();
        List<CamundaExecutionListener> listenerList = extensionElements.getElementsQuery()
                .filterByType(CamundaExecutionListener.class).list();
        for (final CamundaExecutionListener listener : listenerList) {
            final String l_expression = listener.getCamundaExpression();
            if (l_expression != null) {
                processVariables.putAll(findVariablesInExpression(l_expression, element,
                        ElementChapter.ExecutionListener, KnownElementFieldType.Expression, scopeId));
            }
            final String l_delegateExpression = listener.getCamundaDelegateExpression();
            if (l_delegateExpression != null) {
                processVariables.putAll(findVariablesInExpression(l_delegateExpression, element,
                        ElementChapter.ExecutionListener, KnownElementFieldType.DelegateExpression, scopeId));
            }
            processVariables.putAll(getVariablesFromJavaDelegate(listener.getCamundaClass(), element,
                    ElementChapter.ExecutionListener, KnownElementFieldType.Class, scopeId));

            final CamundaScript script = listener.getCamundaScript();
            if (script != null && script.getCamundaScriptFormat() != null
                    && script.getCamundaScriptFormat().equals("groovy")) {
                // inline script or external file?
                final String inlineScript = script.getTextContent();
                if (inlineScript != null && inlineScript.trim().length() > 0) {
                    processVariables
                            .putAll(searchProcessVariablesInCode(element, ElementChapter.ExecutionListener,
                                    KnownElementFieldType.InlineScript, null, scopeId, inlineScript));
                } else {
                    final String resourcePath = script.getCamundaResource();
                    if (resourcePath != null) {
                        processVariables.putAll(getVariablesFromGroovyScript(resourcePath, element,
                                ElementChapter.ExecutionListener, KnownElementFieldType.ExternalScript, scopeId));
                    }
                }
            }
        }
        return processVariables;
    }

    /**
     * get process variables from task listeners
     *
     * TODO: generalise this method eventually
     *
     * @param extensionElements
     * @param processdefinition
     * @param elementId
     * @param cl
     *            ClassLoader
     * @return variables
     */
    private Map<String, ProcessVariable> getVariablesFromTaskListener(final BpmnElement element,
            final ExtensionElements extensionElements, final String scopeId) {

        final Map<String, ProcessVariable> processVariables = new HashMap<String, ProcessVariable>();
        List<CamundaTaskListener> listenerList = extensionElements.getElementsQuery()
                .filterByType(CamundaTaskListener.class).list();
        for (final CamundaTaskListener listener : listenerList) {
            final String l_expression = listener.getCamundaExpression();
            if (l_expression != null) {
                processVariables.putAll(findVariablesInExpression(l_expression, element,
                        ElementChapter.TaskListener, KnownElementFieldType.Expression, scopeId));
            }
            final String l_delegateExpression = listener.getCamundaDelegateExpression();
            if (l_delegateExpression != null) {
                processVariables.putAll(findVariablesInExpression(l_delegateExpression, element,
                        ElementChapter.TaskListener, KnownElementFieldType.DelegateExpression, scopeId));
            }
            processVariables.putAll(getVariablesFromJavaDelegate(listener.getCamundaClass(), element,
                    ElementChapter.TaskListener, KnownElementFieldType.Class, scopeId));

            final CamundaScript script = listener.getCamundaScript();
            if (script != null && script.getCamundaScriptFormat() != null
                    && script.getCamundaScriptFormat().equals("groovy")) {
                // inline script or external file?
                final String inlineScript = script.getTextContent();
                if (inlineScript != null && inlineScript.trim().length() > 0) {
                    processVariables.putAll(searchProcessVariablesInCode(element, ElementChapter.TaskListener,
                            KnownElementFieldType.InlineScript, null, scopeId, inlineScript));
                } else {
                    final String resourcePath = script.getCamundaResource();
                    if (resourcePath != null) {
                        processVariables.putAll(getVariablesFromGroovyScript(resourcePath, element,
                                ElementChapter.TaskListener, KnownElementFieldType.ExternalScript, scopeId));
                    }
                }
            }
        }

        return processVariables;
    }

    /**
     * get process variables from form fields (user tasks)
     *
     * @param extensionElements
     * @param processdefinition
     * @param elementId
     * @return variables
     */
    private Map<String, ProcessVariable> getVariablesFromFormData(final BpmnElement element,
            final ExtensionElements extensionElements, final String scopeElementId) {

        final Map<String, ProcessVariable> processVariables = new HashMap<String, ProcessVariable>();

        final Query<CamundaFormData> formDataQuery = extensionElements.getElementsQuery()
                .filterByType(CamundaFormData.class);
        if (formDataQuery.count() > 0) {
            final CamundaFormData formData = formDataQuery.singleResult();
            if (formData != null) {
                final Collection<CamundaFormField> formFields = formData.getCamundaFormFields();
                for (final CamundaFormField field : formFields) {
                    processVariables.put(field.getCamundaId(),
                            new ProcessVariable(field.getCamundaId(), element, ElementChapter.FormData,
                                    KnownElementFieldType.FormField, null, VariableOperation.WRITE, scopeElementId));
                }
            }
        }

        return processVariables;
    }

    /**
     * get process variables from camunda input/output associations (call activities)
     *
     * @param element
     * @param extensionElements
     * @param elementScopeId
     * @return variables
     */
    private Map<String, ProcessVariable> searchVariablesInInputOutputExtensions(
            final BpmnElement element, final ExtensionElements extensionElements, final String scopeId) {

        final Map<String, ProcessVariable> processVariables = new HashMap<String, ProcessVariable>();

        final BaseElement baseElement = element.getBaseElement();

        if (baseElement instanceof CallActivity) {
            final List<CamundaIn> inputAssociations = extensionElements.getElementsQuery()
                    .filterByType(CamundaIn.class).list();
            for (final CamundaIn inputAssociation : inputAssociations) {
                final String source = inputAssociation.getCamundaSource();
                if (source != null && !source.isEmpty()) {
                    processVariables.put(source,
                            new ProcessVariable(source, element, ElementChapter.InputData,
                                    KnownElementFieldType.CamundaIn, null, VariableOperation.READ, scopeId));
                }
            }
            final List<CamundaOut> outputAssociations = extensionElements.getElementsQuery()
                    .filterByType(CamundaOut.class).list();
            for (final CamundaOut outputAssociation : outputAssociations) {
                final String target = outputAssociation.getCamundaTarget();
                if (target != null && !target.isEmpty()) {
                    processVariables.put(target,
                            new ProcessVariable(target, element, ElementChapter.OutputData,
                                    KnownElementFieldType.CamundaOut, null, VariableOperation.WRITE, scopeId));
                }
            }
        }

        return processVariables;
    }

    /**
     * get process variables from sequence flow conditions
     *
     * @param element
     * @param cl
     * @return variables
     */
    private Map<String, ProcessVariable> searchVariablesFromSequenceFlow(final BpmnElement element) {

        Map<String, ProcessVariable> variables = new HashMap<String, ProcessVariable>();
        final BaseElement baseElement = element.getBaseElement();
        if (baseElement instanceof SequenceFlow) {
            final SequenceFlow flow = (SequenceFlow) baseElement;
            BpmnModelElementInstance scopeElement = flow.getScope();
            String scopeId = null;
            if (scopeElement != null) {
                scopeId = scopeElement.getAttributeValue("id");
            }
            final ConditionExpression expression = flow.getConditionExpression();
            if (expression != null) {
                if (expression.getLanguage() != null && expression.getLanguage().equals("groovy")) {
                    // inline script or external file?
                    final String inlineScript = expression.getTextContent();
                    if (inlineScript != null && inlineScript.trim().length() > 0) {
                        variables.putAll(searchProcessVariablesInCode(element, ElementChapter.Details,
                                KnownElementFieldType.InlineScript, scopeId, null, inlineScript));
                    } else {
                        final String resourcePath = expression.getCamundaResource();
                        if (resourcePath != null) {
                            variables.putAll(getVariablesFromGroovyScript(resourcePath, element,
                                    ElementChapter.Details, KnownElementFieldType.ExternalScript, scopeId));
                        }
                    }
                } else {
                    if (expression.getTextContent().trim().length() > 0) {
                        variables = findVariablesInExpression(expression.getTextContent(), element,
                                ElementChapter.Details, KnownElementFieldType.Expression, scopeId);
                    }
                }
            }
        }
        return variables;
    }

    /**
     * Analyse all types of tasks for process variables
     *
     * @param element
     * @param cl
     *            ClassLoader
     * @return variables
     */
    private Map<String, ProcessVariable> getVariablesFromTask(final BpmnElement element) {

        final Map<String, ProcessVariable> processVariables = new HashMap<String, ProcessVariable>();

        final BaseElement baseElement = element.getBaseElement();
        BpmnModelElementInstance scopeElement = baseElement.getScope();
        String scopeId = null;
        if (scopeElement != null) {
            scopeId = scopeElement.getAttributeValue("id");
        }
        if (baseElement instanceof ServiceTask || baseElement instanceof SendTask
                || baseElement instanceof BusinessRuleTask) {
            final String t_expression = baseElement.getAttributeValueNs(BpmnModelConstants.CAMUNDA_NS,
                    "expression");
            if (t_expression != null) {

                processVariables.putAll(findVariablesInExpression(t_expression, element,
                        ElementChapter.Details, KnownElementFieldType.Expression, scopeId));
            }

            final String t_delegateExpression = baseElement
                    .getAttributeValueNs(BpmnModelConstants.CAMUNDA_NS, "delegateExpression");
            if (t_delegateExpression != null) {
                processVariables.putAll(findVariablesInExpression(t_delegateExpression, element,
                        ElementChapter.Details, KnownElementFieldType.DelegateExpression, scopeId));
            }
            final String t_resultVariable = baseElement.getAttributeValueNs(BpmnModelConstants.CAMUNDA_NS,
                    "resultVariable");
            if (t_resultVariable != null && t_resultVariable.trim().length() > 0) {
                processVariables.put(t_resultVariable,
                        new ProcessVariable(t_resultVariable, element, ElementChapter.Details,
                                KnownElementFieldType.ResultVariable, null, VariableOperation.WRITE, scopeId));
            }
            processVariables.putAll(getVariablesFromJavaDelegate(
                    baseElement.getAttributeValueNs(BpmnModelConstants.CAMUNDA_NS, "class"), element,
                    ElementChapter.Details, KnownElementFieldType.Class, scopeId));

            if (baseElement instanceof BusinessRuleTask) {
                final String t_decisionRef = baseElement.getAttributeValueNs(BpmnModelConstants.CAMUNDA_NS,
                        "decisionRef");
                if (t_decisionRef != null && t_decisionRef.trim().length() > 0
                        && decisionRefToPathMap != null) {
                    final String fileName = decisionRefToPathMap.get(t_decisionRef);
                    if (fileName != null) {
                        processVariables.putAll(readDmnFile(t_decisionRef, fileName, element,
                                ElementChapter.Details, KnownElementFieldType.DMN, scopeId));
                    }
                }
            }

        } else if (baseElement instanceof UserTask) {
            final UserTask userTask = (UserTask) baseElement;
            final String assignee = userTask.getCamundaAssignee();
            if (assignee != null)
                processVariables.putAll(findVariablesInExpression(assignee, element, ElementChapter.Details,
                        KnownElementFieldType.Assignee, scopeId));
            final String candidateUsers = userTask.getCamundaCandidateUsers();
            if (candidateUsers != null)
                processVariables.putAll(findVariablesInExpression(candidateUsers, element,
                        ElementChapter.Details, KnownElementFieldType.CandidateUsers, scopeId));
            final String candidateGroups = userTask.getCamundaCandidateGroups();
            if (candidateGroups != null)
                processVariables.putAll(findVariablesInExpression(candidateGroups, element,
                        ElementChapter.Details, KnownElementFieldType.CandidateGroups, scopeId));
            final String dueDate = userTask.getCamundaDueDate();
            if (dueDate != null)
                processVariables.putAll(findVariablesInExpression(dueDate, element, ElementChapter.Details,
                        KnownElementFieldType.DueDate, scopeId));
            final String followUpDate = userTask.getCamundaFollowUpDate();
            if (followUpDate != null)
                processVariables.putAll(findVariablesInExpression(followUpDate, element,
                        ElementChapter.Details, KnownElementFieldType.FollowUpDate, scopeId));

        } else if (baseElement instanceof ScriptTask) {
            // Examine script task for process variables
            final ScriptTask scriptTask = (ScriptTask) baseElement;
            if (scriptTask.getScriptFormat() != null && scriptTask.getScriptFormat().equals("groovy")) {
                // inline script or external file?
                final Script script = scriptTask.getScript();
                if (script != null && script.getTextContent() != null
                        && script.getTextContent().trim().length() > 0) {
                    processVariables.putAll(searchProcessVariablesInCode(element, ElementChapter.Details,
                            KnownElementFieldType.InlineScript, null, scopeId, script.getTextContent()));
                } else {
                    final String resourcePath = scriptTask.getCamundaResource();
                    if (resourcePath != null) {
                        processVariables.putAll(getVariablesFromGroovyScript(resourcePath, element,
                                ElementChapter.Details, KnownElementFieldType.ExternalScript, scopeId));
                    }
                }
            }
            String resultVariable = scriptTask.getCamundaResultVariable();
            if (resultVariable != null && resultVariable.trim().length() > 0) {
                processVariables.put(resultVariable,
                        new ProcessVariable(resultVariable, element, ElementChapter.Details,
                                KnownElementFieldType.ResultVariable, null, VariableOperation.WRITE, scopeId));
            }
        } else if (baseElement instanceof CallActivity) {
            final CallActivity callActivity = (CallActivity) baseElement;
            final String calledElement = callActivity.getCalledElement();
            if (calledElement != null && calledElement.trim().length() > 0) {
                processVariables.putAll(findVariablesInExpression(calledElement, element,
                        ElementChapter.Details, KnownElementFieldType.CalledElement, scopeId));
            }
            final String caseRef = callActivity.getAttributeValueNs(BpmnModelConstants.CAMUNDA_NS,
                    "caseRef");
            if (caseRef != null && caseRef.trim().length() > 0) {
                processVariables.putAll(findVariablesInExpression(caseRef, element, ElementChapter.Details,
                        KnownElementFieldType.CaseRef, scopeId));
            }
        }

        // Check multi instance attributes
        processVariables.putAll(searchVariablesInMultiInstanceTask(element));

        return processVariables;
    }

    /**
     * Examine multi instance tasks for process variables
     *
     * @param element
     * @return variables
     */
    private Map<String, ProcessVariable> searchVariablesInMultiInstanceTask(final BpmnElement element) {

        final Map<String, ProcessVariable> processVariables = new HashMap<String, ProcessVariable>();

        final BaseElement baseElement = element.getBaseElement();
        BpmnModelElementInstance scopeElement = baseElement.getScope();
        String scopeId = null;
        if (scopeElement != null) {
            scopeId = scopeElement.getAttributeValue("id");
        }
        final ModelElementInstance loopCharacteristics = baseElement
                .getUniqueChildElementByType(LoopCharacteristics.class);
        if (loopCharacteristics != null) {
            final String collectionName = loopCharacteristics
                    .getAttributeValueNs(BpmnModelConstants.CAMUNDA_NS, "collection");
            if (collectionName != null && collectionName.trim().length() > 0) {
                processVariables.put(collectionName,
                        new ProcessVariable(collectionName, element, ElementChapter.MultiInstance,
                                KnownElementFieldType.CollectionElement, null, VariableOperation.READ, scopeId));
            }
            final String elementVariable = loopCharacteristics
                    .getAttributeValueNs(BpmnModelConstants.CAMUNDA_NS, "elementVariable");
            if (elementVariable != null && elementVariable.trim().length() > 0) {
                processVariables.put(elementVariable,
                        new ProcessVariable(elementVariable, element, ElementChapter.MultiInstance,
                                KnownElementFieldType.ElementVariable, null, VariableOperation.READ, scopeId));
            }
            final ModelElementInstance loopCardinality = loopCharacteristics
                    .getUniqueChildElementByType(LoopCardinality.class);
            if (loopCardinality != null) {
                final String cardinality = loopCardinality.getTextContent();
                if (cardinality != null && cardinality.trim().length() > 0) {
                    processVariables.putAll(findVariablesInExpression(cardinality, element,
                            ElementChapter.MultiInstance, KnownElementFieldType.LoopCardinality, scopeId));
                }
            }
            final ModelElementInstance completionCondition = loopCharacteristics
                    .getUniqueChildElementByType(CompletionCondition.class);
            if (completionCondition != null) {
                final String completionConditionExpression = completionCondition.getTextContent();
                if (completionConditionExpression != null
                        && completionConditionExpression.trim().length() > 0) {
                    processVariables.putAll(findVariablesInExpression(completionConditionExpression, element,
                            ElementChapter.MultiInstance, KnownElementFieldType.CompletionCondition, scopeId));
                }
            }
        }
        return processVariables;
    }

    /**
     * Checks a java delegate for process variable references (read/write).
     *
     * Constraints: Method examine only variables in java delegate and not in the method references process variables
     * with names, which only could be determined at runtime, can't be analysed. e.g.
     * execution.setVariable(execution.getActivityId() + "-" + execution.getEventName(), true)
     *
     * @param classFile
     * @param element
     * @param cl
     *            ClassLoader
     * @return variables
     * @throws MalformedURLException
     */
    private Map<String, ProcessVariable> getVariablesFromJavaDelegate(final String classFile,
            final BpmnElement element, final ElementChapter chapter,
            final KnownElementFieldType fieldType, final String scopeId) {
        // convert package format in a concrete path to the java class (.java)
        String filePath = "";
        if (classFile != null && classFile.trim().length() > 0) {
            filePath = classFile.replaceAll("\\.", "/") + ".java";
        }
        final Map<String, ProcessVariable> variables = readResourceFile(filePath, element, chapter,
                fieldType, scopeId);
        return variables;
    }

    /**
     * Checks an external groovy script for process variables (read/write).
     *
     * @param groovyFile
     * @param cl
     *            ClassLoader
     * @return variables
     */
    private Map<String, ProcessVariable> getVariablesFromGroovyScript(final String groovyFile,
            final BpmnElement element, final ElementChapter chapter,
            final KnownElementFieldType fieldType, final String scopeId) {

        final Map<String, ProcessVariable> variables = readResourceFile(groovyFile, element, chapter,
                fieldType, scopeId);
        return variables;
    }

    /**
     * Reads a resource file from class path
     *
     * @param fileName
     * @param element
     * @param cl
     * @return variables
     */
    private Map<String, ProcessVariable> readResourceFile(final String fileName,
            final BpmnElement element, final ElementChapter chapter,
            final KnownElementFieldType fieldType, final String scopeId) {
        Map<String, ProcessVariable> variables = new HashMap<String, ProcessVariable>();
        if (fileName != null && fileName.trim().length() > 0) {
            final InputStream resource = RuntimeConfig.getInstance().getClassLoader().getResourceAsStream(fileName);
            if (resource != null) {
                try {
                    final String methodBody = IOUtils
                            .toString(RuntimeConfig.getInstance().getClassLoader().getResourceAsStream(fileName));
                    variables = searchProcessVariablesInCode(element, chapter, fieldType, fileName, scopeId,
                            methodBody);
                } catch (final IOException ex) {
                    throw new RuntimeException(
                            "resource '" + fileName + "' could not be read: " + ex.getMessage());
                }
            }
        }
        return variables;
    }

    /**
     * Scans a dmn file for process variables
     *
     * @param filePath
     * @return
     */
    private Map<String, ProcessVariable> readDmnFile(final String decisionId, final String fileName,
            final BpmnElement element, final ElementChapter chapter,
            final KnownElementFieldType fieldType, final String scopeId) {

        final Map<String, ProcessVariable> variables = new HashMap<String, ProcessVariable>();

        if (fileName != null && fileName.trim().length() > 0) {
            final InputStream resource = RuntimeConfig.getInstance().getClassLoader().getResourceAsStream(fileName);
            if (resource != null) {
                // parse dmn model
                final DmnModelInstance modelInstance = Dmn.readModelFromStream(resource);
                final Decision decision = modelInstance.getModelElementById(decisionId);
                final Collection<InputExpression> inputExpressions = decision.getModelInstance()
                        .getModelElementsByType(InputExpression.class);
                for (final InputExpression inputExpression : inputExpressions) {
                    final Text variable = inputExpression.getText();
                    variables.put(variable.getTextContent(), new ProcessVariable(variable.getTextContent(),
                            element, chapter, fieldType, fileName, VariableOperation.READ, scopeId));
                }
                final Collection<Output> outputs = decision.getModelInstance()
                        .getModelElementsByType(Output.class);
                for (final Output output : outputs) {
                    final String variable = output.getName();
                    variables.put(variable, new ProcessVariable(variable, element, chapter, fieldType,
                            fileName, VariableOperation.WRITE, scopeId));
                }
            }
        }
        return variables;
    }

    /**
     * Examine java code for process variables
     *
     * @param element
     * @param fileName
     * @param code
     * @return variables
     */
    private Map<String, ProcessVariable> searchProcessVariablesInCode(final BpmnElement element,
            final ElementChapter chapter, final KnownElementFieldType fieldType, final String fileName,
            final String scopeId, final String code) {

        final Map<String, ProcessVariable> variables = new HashMap<String, ProcessVariable>();
        variables.putAll(
                searchReadProcessVariablesInCode(element, chapter, fieldType, fileName, scopeId, code));
        variables.putAll(
                searchWrittenProcessVariablesInCode(element, chapter, fieldType, fileName, scopeId, code));
        variables.putAll(
                searchRemovedProcessVariablesInCode(element, chapter, fieldType, fileName, scopeId, code));

        return variables;
    }

    /**
     * search read process variables
     *
     * @param element
     * @param fileName
     * @param code
     * @return
     */
    private Map<String, ProcessVariable> searchReadProcessVariablesInCode(final BpmnElement element,
            final ElementChapter chapter, final KnownElementFieldType fieldType, final String fileName,
            final String scopeId, final String code) {

        final Map<String, ProcessVariable> variables = new HashMap<String, ProcessVariable>();

        // remove special characters from code
        final String FILTER_PATTERN = "'|\"| ";
        final String cleanedCode = code.replaceAll(FILTER_PATTERN, "");

        // search locations where variables are read
        final Pattern getVariablePatternRuntimeService = Pattern
                .compile("\\.getVariable\\((.*),(\\w+)\\)");
        final Matcher matcherRuntimeService = getVariablePatternRuntimeService.matcher(cleanedCode);

        while (matcherRuntimeService.find()) {
            final String match = matcherRuntimeService.group(2);
            variables.put(match, new ProcessVariable(match, element, chapter, fieldType, fileName,
                    VariableOperation.READ, scopeId));
        }

        final Pattern getVariablePatternDelegateExecution = Pattern
                .compile("\\.getVariable\\((\\w+)\\)");
        final Matcher matcherDelegateExecution = getVariablePatternDelegateExecution
                .matcher(cleanedCode);

        while (matcherDelegateExecution.find()) {
            final String match = matcherDelegateExecution.group(1);
            variables.put(match, new ProcessVariable(match, element, chapter, fieldType, fileName,
                    VariableOperation.READ, scopeId));
        }

        return variables;
    }

    /**
     * search written process variables
     *
     * @param element
     * @param fileName
     * @param code
     * @return
     */
    private Map<String, ProcessVariable> searchWrittenProcessVariablesInCode(
            final BpmnElement element, final ElementChapter chapter,
            final KnownElementFieldType fieldType, final String fileName, final String scopeId,
            final String code) {

        final Map<String, ProcessVariable> variables = new HashMap<String, ProcessVariable>();

        // remove special characters from code
        final String FILTER_PATTERN = "'|\"| ";
        final String cleanedCode = code.replaceAll(FILTER_PATTERN, "");

        // search locations where variables are written
        final Pattern setVariablePatternRuntimeService = Pattern
                .compile("\\.setVariable\\((.*),(\\w+),(.*)\\)");
        final Matcher matcherPatternRuntimeService = setVariablePatternRuntimeService
                .matcher(cleanedCode);
        while (matcherPatternRuntimeService.find()) {
            final String match = matcherPatternRuntimeService.group(2);
            variables.put(match, new ProcessVariable(match, element, chapter, fieldType, fileName,
                    VariableOperation.WRITE, scopeId));
        }

        final Pattern setVariablePatternDelegateExecution = Pattern
                .compile("\\.setVariable\\((\\w+),(.*)\\)");
        final Matcher matcherPatternDelegateExecution = setVariablePatternDelegateExecution
                .matcher(cleanedCode);
        while (matcherPatternDelegateExecution.find()) {
            final String match = matcherPatternDelegateExecution.group(1);
            variables.put(match, new ProcessVariable(match, element, chapter, fieldType, fileName,
                    VariableOperation.WRITE, scopeId));
        }

        return variables;
    }

    /**
     * search removed process variables
     *
     * @param element
     * @param chapter
     * @param fieldType
     * @param fileName
     * @param scopeId
     * @param code
     * @return variables
     */
    private Map<String, ProcessVariable> searchRemovedProcessVariablesInCode(
            final BpmnElement element, final ElementChapter chapter,
            final KnownElementFieldType fieldType, final String fileName, final String scopeId,
            final String code) {

        final Map<String, ProcessVariable> variables = new HashMap<String, ProcessVariable>();

        // remove special characters from code
        final String FILTER_PATTERN = "'|\"| ";
        final String cleanedCode = code.replaceAll(FILTER_PATTERN, "");

        // search locations where variables are removed
        final Pattern removeVariablePatternRuntimeService = Pattern
                .compile("\\.removeVariable\\((.*),(\\w+)\\)");
        final Matcher matcherRuntimeService = removeVariablePatternRuntimeService.matcher(cleanedCode);

        while (matcherRuntimeService.find()) {
            final String match = matcherRuntimeService.group(2);
            variables.put(match, new ProcessVariable(match, element, chapter, fieldType, fileName,
                    VariableOperation.DELETE, scopeId));
        }

        final Pattern removeVariablePatternDelegateExecution = Pattern
                .compile("\\.removeVariable\\((\\w+)\\)");
        final Matcher matcherDelegateExecution = removeVariablePatternDelegateExecution
                .matcher(cleanedCode);

        while (matcherDelegateExecution.find()) {
            final String match = matcherDelegateExecution.group(1);
            variables.put(match, new ProcessVariable(match, element, chapter, fieldType, fileName,
                    VariableOperation.DELETE, scopeId));
        }

        return variables;
    }

    /**
     * Examine JUEL expressions for variables
     *
     * @param expression
     * @param element
     * @return variables
     * @throws ProcessingException
     */
    private Map<String, ProcessVariable> findVariablesInExpression(final String expression,
            final BpmnElement element, final ElementChapter chapter,
            final KnownElementFieldType fieldType, final String scopeId) {
        final Map<String, ProcessVariable> variables = new HashMap<String, ProcessVariable>();

        try {
            // remove object name from method calls, otherwise the method arguments could not be found
            final String filteredExpression = expression.replaceAll("[\\w]+\\.", "");
            final TreeBuilder treeBuilder = new Builder();
            final Tree tree = treeBuilder.build(filteredExpression);

            final Iterable<IdentifierNode> identifierNodes = tree.getIdentifierNodes();
            for (final IdentifierNode node : identifierNodes) {
                // checks, if found variable is a bean
                final String className = isBean(node.getName());
                if (className != null) {
                    // read variables in class file (bean)
                    variables.putAll(
                            getVariablesFromJavaDelegate(className, element, chapter, fieldType, scopeId));
                } else {
                    // save variable
                    variables.put(node.getName(), new ProcessVariable(node.getName(), element, chapter,
                            fieldType, null, VariableOperation.READ, scopeId));
                }
            }
            // extract written variables
            variables.putAll(searchWrittenProcessVariablesInCode(element, chapter, fieldType, null,
                    scopeId, expression));
            // extract deleted variables
            variables.putAll(searchRemovedProcessVariablesInCode(element, chapter, fieldType, null,
                    scopeId, expression));
        } catch (final ELException e) {
            throw new ProcessingException("el expression " + expression + " in "
                    + element.getProcessdefinition() + ", element ID: " + element.getBaseElement().getId()
                    + ", Type: " + fieldType.getDescription() + " couldn't be parsed", e);
        }

        return variables;
    }

    /**
     * Checks a variable being a bean
     *
     * @param variable
     * @return classpath to bean definition
     */
    private String isBean(final String variable) {
        if (RuntimeConfig.getInstance().getBeanMapping() != null) {
            return RuntimeConfig.getInstance().getBeanMapping().get(variable);
        }
        return null;
    }
}
