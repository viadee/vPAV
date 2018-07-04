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
package de.viadee.bpm.vPAV.processing;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.el.ELException;

import de.viadee.bpm.vPAV.processing.model.data.*;
import org.apache.commons.io.IOUtils;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.Resource;
import org.camunda.bpm.engine.impl.juel.Builder;
import org.camunda.bpm.engine.impl.juel.IdentifierNode;
import org.camunda.bpm.engine.impl.juel.Tree;
import org.camunda.bpm.engine.impl.juel.TreeBuilder;
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

import de.viadee.bpm.vPAV.BpmnScanner;
import de.viadee.bpm.vPAV.Runner;
import de.viadee.bpm.vPAV.RuntimeConfig;
import de.viadee.bpm.vPAV.constants.BpmnConstants;
import de.viadee.bpm.vPAV.constants.ConfigConstants;
import de.viadee.bpm.vPAV.processing.model.data.ProcessVariableOperation;

/**
 * search process variables for an bpmn element
 *
 */
public final class ProcessVariableReader {

    private final Map<String, String> decisionRefToPathMap;

    private final BpmnScanner bpmnScanner;

    public static final Logger LOGGER = Logger.getLogger(ProcessVariableReader.class.getName());

    public ProcessVariableReader(final Map<String, String> decisionRefToPathMap, BpmnScanner scanner) {
        this.decisionRefToPathMap = decisionRefToPathMap;
        this.bpmnScanner = scanner;
    }

    /**
     * Examining an bpmn element for variables
     *
     * @param element
     *            BpmnElement
     * @return processVariables returns processVariables
     */
    public Map<String, ProcessVariableOperation> getVariablesFromElement(final BpmnElement element) {

        final Map<String, ProcessVariableOperation> processVariables = new HashMap<String, ProcessVariableOperation>();

        // 1) Search variables in task
        processVariables.putAll(getVariablesFromTask(element));
        // 2) Search variables in sequence flow
        processVariables.putAll(searchVariablesFromSequenceFlow(element));
        // 3) Search variables in ExtensionElements
        processVariables.putAll(searchExtensionsElements(element));
        // 4) Search variables in In/Output Parameters
        processVariables.putAll(getVariablesFromParameters(element));
        // 5) Search variables in Signal and Messagenames
        processVariables.putAll(getVariablesFromNames(element));

        return processVariables;
    }

    private Map<String, ProcessVariableOperation> getVariablesFromNames(BpmnElement element) {
        final Map<String, ProcessVariableOperation> processVariables = new HashMap<String, ProcessVariableOperation>();
        final BaseElement baseElement = element.getBaseElement();
        final BpmnModelElementInstance scopeElement = baseElement.getScope();

        String scopeElementId = null;
        if (scopeElement != null) {
            scopeElementId = scopeElement.getAttributeValue(BpmnConstants.ATTR_ID);
        }

        ArrayList<String> signalRefs = bpmnScanner.getSignalRefs(element.getBaseElement().getId());
        ArrayList<String> messagesRefs = bpmnScanner.getMessageRefs(element.getBaseElement().getId());

        ArrayList<String> signalVariables = getSignalVariables(signalRefs, element);
        ArrayList<String> messageVariables = getMessageVariables(messagesRefs, element);

        for (String variable : messageVariables) {
            processVariables.put(variable,
                    new ProcessVariableOperation(variable, element, ElementChapter.General,
                            KnownElementFieldType.Message, element.getProcessdefinition(), VariableOperation.READ,
                            scopeElementId));
        }

        for (String variable : signalVariables) {
            processVariables.put(variable,
                    new ProcessVariableOperation(variable, element, ElementChapter.General,
                            KnownElementFieldType.Signal, element.getProcessdefinition(), VariableOperation.READ,
                            scopeElementId));
        }

        return processVariables;
    }

    private ArrayList<String> getSignalVariables(ArrayList<String> signalRefs, BpmnElement element) {
        ArrayList<String> names = new ArrayList<String>();

        for (String signalID : signalRefs) {
            names.add(bpmnScanner.getSignalName(signalID));
        }

        ArrayList<String> variables = new ArrayList<String>();
        for (String signalName : names) {
            variables.addAll(checkExpressionForReadVariable(signalName, element));
        }

        return variables;
    }

    private ArrayList<String> getMessageVariables(ArrayList<String> messageRefs, BpmnElement element) {
        ArrayList<String> names = new ArrayList<String>();

        for (String messageID : messageRefs) {
            names.add(bpmnScanner.getMessageName(messageID));
        }

        ArrayList<String> variables = new ArrayList<String>();
        for (String messageName : names) {
            variables.addAll(checkExpressionForReadVariable(messageName, element));
        }

        return variables;
    }

    /**
     * Analyze Output Parameters for variables
     *
     * @param element
     * @return Map of ProcessVariable
     *
     */
    private Map<String, ProcessVariableOperation> getVariablesFromParameters(BpmnElement element) {
        final Map<String, ProcessVariableOperation> processVariables = new HashMap<String, ProcessVariableOperation>();
        final BaseElement baseElement = element.getBaseElement();
        final BpmnModelElementInstance scopeElement = baseElement.getScope();

        String scopeElementId = null;
        if (scopeElement != null) {
            scopeElementId = scopeElement.getAttributeValue(BpmnConstants.ATTR_ID);
        }

        ArrayList<String> outVar = bpmnScanner.getOutputVariables(element.getBaseElement().getId());
        ArrayList<String> inVar = bpmnScanner.getInputVariables(element.getBaseElement().getId());
        ArrayList<String> varValues = bpmnScanner.getInOutputVariablesValue(element.getBaseElement().getId());

        // save output variables
        for (String name : outVar) {
            processVariables.put(name,
                    new ProcessVariableOperation(name, element, ElementChapter.InputOutput,
                            KnownElementFieldType.OutputParameter, element.getProcessdefinition(),
                            VariableOperation.WRITE, scopeElementId));
        }

        ArrayList<String> varValueClean = new ArrayList<String>();
        for (String expression : varValues) {
            varValueClean.addAll(checkExpressionForReadVariable(expression, element));
        }
        varValueClean.removeAll(inVar);

        // add all processVariables to List
        for (String var : varValueClean) {
            processVariables.put(var,
                    new ProcessVariableOperation(var, element, ElementChapter.InputOutput,
                            KnownElementFieldType.OutputParameter, element.getProcessdefinition(),
                            VariableOperation.READ, scopeElementId));
        }

        for (String name : outVar)
            processVariables.put(name,
                    new ProcessVariableOperation(name, element, ElementChapter.InputOutput,
                            KnownElementFieldType.OutputParameter, element.getProcessdefinition(),
                            VariableOperation.WRITE, scopeElementId));

        return processVariables;
    }

    /**
     * Analyze bpmn extension elements for variables
     *
     * @param element
     * @return variables
     */
    private Map<String, ProcessVariableOperation> searchExtensionsElements(final BpmnElement element) {

        final Map<String, ProcessVariableOperation> processVariables = new HashMap<String, ProcessVariableOperation>();
        final BaseElement baseElement = element.getBaseElement();
        final BpmnModelElementInstance scopeElement = baseElement.getScope();
        String scopeElementId = null;
        if (scopeElement != null) {
            scopeElementId = scopeElement.getAttributeValue(BpmnConstants.ATTR_ID);
        }
        final ExtensionElements extensionElements = baseElement.getExtensionElements();
        if (extensionElements != null) {
            // 1) Search in Execution Listeners
            processVariables.putAll(getVariablesFromExecutionListener(element, extensionElements, scopeElementId));

            // 2) Search in Task Listeners
            processVariables.putAll(getVariablesFromTaskListener(element, extensionElements, scopeElementId));

            // 3) Search in Form Data
            processVariables.putAll(getVariablesFromFormData(element, extensionElements, scopeElementId));

            // 4) Search in Input/Output-Associations (Call Activities)
            processVariables.putAll(searchVariablesInInputOutputExtensions(element, extensionElements, scopeElementId));
        }

        return processVariables;
    }

    /**
     * Get process variables from execution listeners
     *
     * @param extensionElements
     * @param processdefinition
     * @param elementId
     * @return variables
     */
    private Map<String, ProcessVariableOperation> getVariablesFromExecutionListener(final BpmnElement element,
            final ExtensionElements extensionElements, final String scopeId) {

        final Map<String, ProcessVariableOperation> processVariables = new HashMap<String, ProcessVariableOperation>();
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
            processVariables.putAll(new JavaReaderRegex().getVariablesFromJavaDelegate(listener.getCamundaClass(),
                    element, ElementChapter.ExecutionListener, KnownElementFieldType.Class, scopeId));

            final CamundaScript script = listener.getCamundaScript();
            if (script != null && script.getCamundaScriptFormat() != null
                    && script.getCamundaScriptFormat().equals(ConfigConstants.GROOVY)) {
                // inline script or external file?
                final String inlineScript = script.getTextContent();
                if (inlineScript != null && inlineScript.trim().length() > 0) {
                    processVariables.putAll(
                            ResourceFileReader.searchProcessVariablesInCode(element, ElementChapter.ExecutionListener,
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
     * Get process variables from task listeners
     *
     * TODO: generalise this method eventually
     *
     * @param extensionElements
     * @param processdefinition
     * @param elementId
     * @return variables
     */
    private Map<String, ProcessVariableOperation> getVariablesFromTaskListener(final BpmnElement element,
            final ExtensionElements extensionElements, final String scopeId) {

        final Map<String, ProcessVariableOperation> processVariables = new HashMap<String, ProcessVariableOperation>();
        List<CamundaTaskListener> listenerList = extensionElements.getElementsQuery()
                .filterByType(CamundaTaskListener.class).list();
        for (final CamundaTaskListener listener : listenerList) {
            final String l_expression = listener.getCamundaExpression();
            if (l_expression != null) {
                processVariables.putAll(findVariablesInExpression(l_expression, element, ElementChapter.TaskListener,
                        KnownElementFieldType.Expression, scopeId));
            }
            final String l_delegateExpression = listener.getCamundaDelegateExpression();
            if (l_delegateExpression != null) {
                processVariables.putAll(findVariablesInExpression(l_delegateExpression, element,
                        ElementChapter.TaskListener, KnownElementFieldType.DelegateExpression, scopeId));
            }
            processVariables.putAll(new JavaReaderRegex().getVariablesFromJavaDelegate(listener.getCamundaClass(),
                    element, ElementChapter.TaskListener, KnownElementFieldType.Class, scopeId));

            final CamundaScript script = listener.getCamundaScript();
            if (script != null && script.getCamundaScriptFormat() != null
                    && script.getCamundaScriptFormat().equals(ConfigConstants.GROOVY)) {
                // inline script or external file?
                final String inlineScript = script.getTextContent();
                if (inlineScript != null && inlineScript.trim().length() > 0) {
                    processVariables.putAll(
                            ResourceFileReader.searchProcessVariablesInCode(element, ElementChapter.TaskListener,
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
     * Get process variables from form fields (user tasks)
     *
     * @param extensionElements
     * @param processdefinition
     * @param elementId
     * @return variables
     */
    private Map<String, ProcessVariableOperation> getVariablesFromFormData(final BpmnElement element,
            final ExtensionElements extensionElements, final String scopeElementId) {

        final Map<String, ProcessVariableOperation> processVariables = new HashMap<String, ProcessVariableOperation>();

        final Query<CamundaFormData> formDataQuery = extensionElements.getElementsQuery()
                .filterByType(CamundaFormData.class);
        if (formDataQuery.count() > 0) {
            final CamundaFormData formData = formDataQuery.singleResult();
            if (formData != null) {
                final Collection<CamundaFormField> formFields = formData.getCamundaFormFields();
                for (final CamundaFormField field : formFields) {
                    processVariables.put(field.getCamundaId(),
                            new ProcessVariableOperation(field.getCamundaId(), element, ElementChapter.FormData,
                                    KnownElementFieldType.FormField, null, VariableOperation.WRITE, scopeElementId));
                }
            }
        }

        return processVariables;
    }

    /**
     * Get process variables from camunda input/output associations (call activities)
     *
     * @param element
     * @param extensionElements
     * @param elementScopeId
     * @return variables
     */
    private Map<String, ProcessVariableOperation> searchVariablesInInputOutputExtensions(final BpmnElement element,
            final ExtensionElements extensionElements, final String scopeId) {

        final Map<String, ProcessVariableOperation> processVariables = new HashMap<String, ProcessVariableOperation>();

        final BaseElement baseElement = element.getBaseElement();

        if (baseElement instanceof CallActivity) {
            final List<CamundaIn> inputAssociations = extensionElements.getElementsQuery().filterByType(CamundaIn.class)
                    .list();
            for (final CamundaIn inputAssociation : inputAssociations) {
                final String source = inputAssociation.getCamundaSource();
                if (source != null && !source.isEmpty()) {
                    processVariables.put(source, new ProcessVariableOperation(source, element, ElementChapter.InputData,
                            KnownElementFieldType.CamundaIn, null, VariableOperation.READ, scopeId));
                }
            }
            final List<CamundaOut> outputAssociations = extensionElements.getElementsQuery()
                    .filterByType(CamundaOut.class).list();
            for (final CamundaOut outputAssociation : outputAssociations) {
                final String target = outputAssociation.getCamundaTarget();
                if (target != null && !target.isEmpty()) {
                    processVariables.put(target,
                            new ProcessVariableOperation(target, element, ElementChapter.OutputData,
                                    KnownElementFieldType.CamundaOut, null, VariableOperation.WRITE, scopeId));
                }
            }
        }

        return processVariables;
    }

    /**
     * Get process variables from sequence flow conditions
     *
     * @param element
     * @return variables
     */
    private Map<String, ProcessVariableOperation> searchVariablesFromSequenceFlow(final BpmnElement element) {

        Map<String, ProcessVariableOperation> variables = new HashMap<String, ProcessVariableOperation>();
        final BaseElement baseElement = element.getBaseElement();
        if (baseElement instanceof SequenceFlow) {
            final SequenceFlow flow = (SequenceFlow) baseElement;
            BpmnModelElementInstance scopeElement = flow.getScope();
            String scopeId = null;
            if (scopeElement != null) {
                scopeId = scopeElement.getAttributeValue(BpmnConstants.ATTR_ID);
            }
            final ConditionExpression expression = flow.getConditionExpression();
            if (expression != null) {
                if (expression.getLanguage() != null && expression.getLanguage().equals(ConfigConstants.GROOVY)) {
                    // inline script or external file?
                    final String inlineScript = expression.getTextContent();
                    if (inlineScript != null && inlineScript.trim().length() > 0) {
                        variables
                                .putAll(ResourceFileReader.searchProcessVariablesInCode(element, ElementChapter.Details,
                                        KnownElementFieldType.InlineScript, scopeId, null, inlineScript));
                    } else {
                        final String resourcePath = expression.getCamundaResource();
                        if (resourcePath != null) {
                            variables.putAll(getVariablesFromGroovyScript(resourcePath, element, ElementChapter.Details,
                                    KnownElementFieldType.ExternalScript, scopeId));
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
     * @return variables
     */
    private Map<String, ProcessVariableOperation> getVariablesFromTask(final BpmnElement element) {

        final Map<String, ProcessVariableOperation> processVariables = new HashMap<String, ProcessVariableOperation>();

        final BaseElement baseElement = element.getBaseElement();
        BpmnModelElementInstance scopeElement = baseElement.getScope();
        String scopeId = null;
        if (scopeElement != null) {
            scopeId = scopeElement.getAttributeValue(BpmnConstants.ATTR_ID);
        }
        if (baseElement instanceof ServiceTask || baseElement instanceof SendTask
                || baseElement instanceof BusinessRuleTask) {
            final String t_expression = baseElement.getAttributeValueNs(BpmnModelConstants.CAMUNDA_NS,
                    BpmnConstants.ATTR_EX);
            if (t_expression != null) {

                processVariables.putAll(findVariablesInExpression(t_expression, element, ElementChapter.Details,
                        KnownElementFieldType.Expression, scopeId));
            }

            final String t_delegateExpression = baseElement.getAttributeValueNs(BpmnModelConstants.CAMUNDA_NS,
                    BpmnConstants.ATTR_DEL);
            if (t_delegateExpression != null) {
                processVariables.putAll(findVariablesInExpression(t_delegateExpression, element, ElementChapter.Details,
                        KnownElementFieldType.DelegateExpression, scopeId));
            }

            final ArrayList<String> t_fieldInjectionExpressions = bpmnScanner
                    .getFieldInjectionExpression(baseElement.getId());
            if (t_fieldInjectionExpressions != null && !t_fieldInjectionExpressions.isEmpty()) {
                for (String t_fieldInjectionExpression : t_fieldInjectionExpressions)
                    processVariables.putAll(findVariablesInExpression(t_fieldInjectionExpression, element,
                            ElementChapter.FieldInjections, KnownElementFieldType.Expression, scopeId));
            }

            final String t_resultVariable = baseElement.getAttributeValueNs(BpmnModelConstants.CAMUNDA_NS,
                    BpmnConstants.RESULT_VARIABLE);
            if (t_resultVariable != null && t_resultVariable.trim().length() > 0) {
                processVariables.put(t_resultVariable,
                        new ProcessVariableOperation(t_resultVariable, element, ElementChapter.Details,
                                KnownElementFieldType.ResultVariable, null, VariableOperation.WRITE, scopeId));
            }

            // Depending on Regex/Static analysis, find Process Variables from Java Delegate
            JavaReaderContext jvc = new JavaReaderContext();
            if (Runner.getIsStatic()) {
                jvc.setJavaReadingStrategy(new JavaReaderStatic());
            } else {
                jvc.setJavaReadingStrategy(new JavaReaderRegex());
            }
            processVariables.putAll(jvc.readJavaDelegate(
                    baseElement.getAttributeValueNs(BpmnModelConstants.CAMUNDA_NS, BpmnConstants.ATTR_CLASS), element,
                    ElementChapter.Details, KnownElementFieldType.Class, scopeId));

            if (baseElement instanceof BusinessRuleTask) {
                final String t_decisionRef = baseElement.getAttributeValueNs(BpmnModelConstants.CAMUNDA_NS,
                        BpmnConstants.DECISIONREF);
                if (t_decisionRef != null && t_decisionRef.trim().length() > 0 && decisionRefToPathMap != null) {
                    final String fileName = decisionRefToPathMap.get(t_decisionRef);
                    if (fileName != null) {
                        processVariables.putAll(readDmnFile(t_decisionRef, fileName, element, ElementChapter.Details,
                                KnownElementFieldType.DMN, scopeId));
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
                processVariables.putAll(findVariablesInExpression(candidateUsers, element, ElementChapter.Details,
                        KnownElementFieldType.CandidateUsers, scopeId));
            final String candidateGroups = userTask.getCamundaCandidateGroups();
            if (candidateGroups != null)
                processVariables.putAll(findVariablesInExpression(candidateGroups, element, ElementChapter.Details,
                        KnownElementFieldType.CandidateGroups, scopeId));
            final String dueDate = userTask.getCamundaDueDate();
            if (dueDate != null)
                processVariables.putAll(findVariablesInExpression(dueDate, element, ElementChapter.Details,
                        KnownElementFieldType.DueDate, scopeId));
            final String followUpDate = userTask.getCamundaFollowUpDate();
            if (followUpDate != null)
                processVariables.putAll(findVariablesInExpression(followUpDate, element, ElementChapter.Details,
                        KnownElementFieldType.FollowUpDate, scopeId));

        } else if (baseElement instanceof ScriptTask) {
            // Examine script task for process variables
            final ScriptTask scriptTask = (ScriptTask) baseElement;
            if (scriptTask.getScriptFormat() != null && scriptTask.getScriptFormat().equals(ConfigConstants.GROOVY)) {
                // inline script or external file?
                final Script script = scriptTask.getScript();
                if (script != null && script.getTextContent() != null && script.getTextContent().trim().length() > 0) {
                    processVariables
                            .putAll(ResourceFileReader.searchProcessVariablesInCode(element, ElementChapter.Details,
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
                        new ProcessVariableOperation(resultVariable, element, ElementChapter.Details,
                                KnownElementFieldType.ResultVariable, null, VariableOperation.WRITE, scopeId));
            }
        } else if (baseElement instanceof CallActivity) {
            final CallActivity callActivity = (CallActivity) baseElement;
            final String calledElement = callActivity.getCalledElement();
            if (calledElement != null && calledElement.trim().length() > 0) {
                processVariables.putAll(findVariablesInExpression(calledElement, element, ElementChapter.Details,
                        KnownElementFieldType.CalledElement, scopeId));
            }
            final String caseRef = callActivity.getAttributeValueNs(BpmnModelConstants.CAMUNDA_NS,
                    BpmnConstants.CASEREF);
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
    private Map<String, ProcessVariableOperation> searchVariablesInMultiInstanceTask(final BpmnElement element) {

        final Map<String, ProcessVariableOperation> processVariables = new HashMap<String, ProcessVariableOperation>();

        final BaseElement baseElement = element.getBaseElement();
        BpmnModelElementInstance scopeElement = baseElement.getScope();
        String scopeId = null;
        if (scopeElement != null) {
            scopeId = scopeElement.getAttributeValue(BpmnConstants.ATTR_ID);
        }
        final ModelElementInstance loopCharacteristics = baseElement
                .getUniqueChildElementByType(LoopCharacteristics.class);
        if (loopCharacteristics != null) {
            final String collectionName = loopCharacteristics.getAttributeValueNs(BpmnModelConstants.CAMUNDA_NS,
                    BpmnConstants.COLLECTION);
            if (collectionName != null && collectionName.trim().length() > 0) {
                processVariables.put(collectionName,
                        new ProcessVariableOperation(collectionName, element, ElementChapter.MultiInstance,
                                KnownElementFieldType.CollectionElement, null, VariableOperation.READ, scopeId));
            }
            final String elementVariable = loopCharacteristics.getAttributeValueNs(BpmnModelConstants.CAMUNDA_NS,
                    BpmnConstants.ELEMENT_VARIABLE);
            if (elementVariable != null && elementVariable.trim().length() > 0) {
                processVariables.put(elementVariable,
                        new ProcessVariableOperation(elementVariable, element, ElementChapter.MultiInstance,
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
                if (completionConditionExpression != null && completionConditionExpression.trim().length() > 0) {
                    processVariables.putAll(findVariablesInExpression(completionConditionExpression, element,
                            ElementChapter.MultiInstance, KnownElementFieldType.CompletionCondition, scopeId));
                }
            }
        }
        return processVariables;
    }

    /**
     * Checks an external groovy script for process variables (read/write).
     *
     * @param groovyFile
     * @return variables
     */
    private Map<String, ProcessVariableOperation> getVariablesFromGroovyScript(final String groovyFile,
            final BpmnElement element, final ElementChapter chapter, final KnownElementFieldType fieldType,
            final String scopeId) {

        final Map<String, ProcessVariableOperation> variables = ResourceFileReader.readResourceFile(groovyFile, element,
                chapter, fieldType, scopeId);
        return variables;
    }

    /**
     * Scans a dmn file for process variables
     *
     * @param filePath
     * @return
     */
    private Map<String, ProcessVariableOperation> readDmnFile(final String decisionId, final String fileName,
            final BpmnElement element, final ElementChapter chapter, final KnownElementFieldType fieldType,
            final String scopeId) {

        final Map<String, ProcessVariableOperation> variables = new HashMap<String, ProcessVariableOperation>();

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
                    variables.put(variable.getTextContent(), new ProcessVariableOperation(variable.getTextContent(),
                            element, chapter, fieldType, fileName, VariableOperation.READ, scopeId));
                }
                final Collection<Output> outputs = decision.getModelInstance().getModelElementsByType(Output.class);
                for (final Output output : outputs) {
                    final String variable = output.getName();
                    variables.put(variable, new ProcessVariableOperation(variable, element, chapter, fieldType,
                            fileName, VariableOperation.WRITE, scopeId));
                }
            }
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
    private Map<String, ProcessVariableOperation> findVariablesInExpression(final String expression,
            final BpmnElement element, final ElementChapter chapter, final KnownElementFieldType fieldType,
            final String scopeId) {
        final Map<String, ProcessVariableOperation> variables = new HashMap<String, ProcessVariableOperation>();

        // HOTFIX: Catch pattern like below to avoid crash of TreeBuilder
        // ${dateTime().plusWeeks(1).toDate()}
        final Pattern pattern = Pattern.compile("\\$\\{(\\w)*\\(.*\\)\\}");

        Matcher matcher = pattern.matcher(expression);

        if (matcher.matches()) {
            return variables;
        }

        try {
            // remove object name from method calls, otherwise the method arguments could
            // not be found
            final String filteredExpression = expression.replaceAll("[\\w]+\\.", "");
            final TreeBuilder treeBuilder = new Builder();
            final Tree tree = treeBuilder.build(filteredExpression);

            final Iterable<IdentifierNode> identifierNodes = tree.getIdentifierNodes();
            for (final IdentifierNode node : identifierNodes) {
                // checks, if found variable is a bean
                final String className = isBean(node.getName());
                if (className != null) {
                    // read variables in class file (bean)
                    variables.putAll(new JavaReaderRegex().getVariablesFromJavaDelegate(className, element, chapter,
                            fieldType, scopeId));
                } else {
                    // save variable
                    variables.put(node.getName(), new ProcessVariableOperation(node.getName(), element, chapter,
                            fieldType, null, VariableOperation.READ, scopeId));
                }
            }
            // extract written variables
            variables.putAll(ResourceFileReader.searchWrittenProcessVariablesInCode(element, chapter, fieldType, null,
                    scopeId, expression));
            // extract deleted variables
            variables.putAll(ResourceFileReader.searchRemovedProcessVariablesInCode(element, chapter, fieldType, null,
                    scopeId, expression));
        } catch (final ELException e) {
            throw new ProcessingException("EL expression " + expression + " in " + element.getProcessdefinition()
                    + ", element ID: " + element.getBaseElement().getId() + ", Type: " + fieldType.getDescription()
                    + " couldn't be parsed", e);
        }

        return variables;
    }

    private ArrayList<String> checkExpressionForReadVariable(final String expression, final BpmnElement element) {
        final ArrayList<String> variables = new ArrayList<String>();
        try {
            // remove object name from method calls, otherwise the method arguments could
            // not be found
            final String filteredExpression = expression.replaceAll("[\\w]+\\.", "");
            final TreeBuilder treeBuilder = new Builder();
            final Tree tree = treeBuilder.build(filteredExpression);

            final Iterable<IdentifierNode> identifierNodes = tree.getIdentifierNodes();
            for (final IdentifierNode node : identifierNodes) {
                // checks, if found variable is a bean
                if (isBean(node.getName()) == null) {
                    variables.add(node.getName());
                }
            }
        } catch (final ELException e) {
            throw new ProcessingException("EL expression " + expression + " in " + element.getProcessdefinition()
                    + ", element ID: " + element.getBaseElement().getId() + ", Type: "
                    + KnownElementFieldType.Expression + " couldn't be parsed", e);
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
