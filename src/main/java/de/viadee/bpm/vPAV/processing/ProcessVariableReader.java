/*
 * BSD 3-Clause License
 *
 * Copyright © 2020, viadee Unternehmensberatung AG
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

import com.google.common.collect.ListMultimap;
import de.viadee.bpm.vPAV.BpmnScanner;
import de.viadee.bpm.vPAV.Messages;
import de.viadee.bpm.vPAV.RuntimeConfig;
import de.viadee.bpm.vPAV.config.model.Rule;
import de.viadee.bpm.vPAV.constants.BpmnConstants;
import de.viadee.bpm.vPAV.constants.CamundaMethodServices;
import de.viadee.bpm.vPAV.constants.ConfigConstants;
import de.viadee.bpm.vPAV.output.IssueWriter;
import de.viadee.bpm.vPAV.processing.code.flow.BasicNode;
import de.viadee.bpm.vPAV.processing.code.flow.BpmnElement;
import de.viadee.bpm.vPAV.processing.code.flow.ControlFlowGraph;
import de.viadee.bpm.vPAV.processing.code.flow.ExpressionNode;
import de.viadee.bpm.vPAV.processing.model.data.*;
import org.camunda.bpm.engine.impl.juel.*;
import org.camunda.bpm.model.bpmn.Query;
import org.camunda.bpm.model.bpmn.impl.BpmnModelConstants;
import org.camunda.bpm.model.bpmn.impl.instance.LoopDataInputRef;
import org.camunda.bpm.model.bpmn.instance.*;
import org.camunda.bpm.model.bpmn.instance.camunda.*;
import org.camunda.bpm.model.dmn.Dmn;
import org.camunda.bpm.model.dmn.DmnModelInstance;
import org.camunda.bpm.model.dmn.instance.Decision;
import org.camunda.bpm.model.dmn.instance.InputExpression;
import org.camunda.bpm.model.dmn.instance.Output;
import org.camunda.bpm.model.dmn.instance.Text;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;

import java.io.InputStream;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static de.viadee.bpm.vPAV.constants.ConfigConstants.JAVA_FILE_ENDING;

/**
 * search process variables for an bpmn element
 */
public final class ProcessVariableReader {

    private static final Logger LOGGER = Logger.getLogger(ProcessVariableReader.class.getName());

    private final Map<String, String> decisionRefToPathMap;

    private final Rule rule;

    public ProcessVariableReader(final Map<String, String> decisionRefToPathMap, final Rule rule) {
        this.decisionRefToPathMap = decisionRefToPathMap;
        this.rule = rule;
    }

    /**
     * Examining an bpmn element for variables
     *
     * @param element     BpmnElement
     * @param predecessor List of predecessors
     */
    public void getVariablesFromElement(final BpmnElement element,
            BasicNode[] predecessor) {
        ProcessVariableOperation.resetIdCounter();
        final BaseElement baseElement = element.getBaseElement();
        final BpmnModelElementInstance scopeElement = baseElement.getScope();
        String scopeElementId = null;
        String scopeId = null;
        if (scopeElement != null) {
            scopeElementId = scopeElement.getAttributeValue(BpmnConstants.ATTR_ID);
            scopeId = getProcessScope(scopeElement);
        }
        final ExtensionElements extensionElements = baseElement.getExtensionElements();

        // 1) Search for variables in multi instance task, if applicable
        searchVariablesInMultiInstanceTask(element, predecessor);

        if (extensionElements != null) {
            // 2) Search variables in Input Parameters
            processInputOutputParameters(element, extensionElements, predecessor, true);

            // 3) Search variables execution listener (start)
            getVariablesFromExecutionListener(element,
                    extensionElements, scopeId, ElementChapter.EXECUTION_LISTENER_START, predecessor);
        }

        // 4) Search variables in task
        getVariablesFromTask(element, predecessor);

        // 5) Search variables in sequence flow
        searchVariablesFromSequenceFlow(element, predecessor);

        if (extensionElements != null) {
            // 6) Search variables in ExtensionElements
            searchExtensionsElements(element, extensionElements, predecessor);
        }

        // 7) Search variables in Signals and Messages and Links
        getVariablesFromSignalsAndMessagesAndLinks(element, predecessor);

        if (extensionElements != null) {
            // 8) Search in Input/Output-Associations (Call Activities)
            searchVariablesInInputOutputExtensions(element,
                    extensionElements, scopeElementId, predecessor);

            // 9) Search variables execution listener (end)
            getVariablesFromExecutionListener(element,
                    extensionElements, scopeId, ElementChapter.EXECUTION_LISTENER_END, predecessor);

            // 10) Search variables in Output Parameters
            processInputOutputParameters(element, extensionElements, predecessor, false);
        }
    }

    /**
     * Retrieve process variables from signals and messages
     *
     * @param element     BpmnElement
     * @param predecessor Predecessor
     */
    public void getVariablesFromSignalsAndMessagesAndLinks(final BpmnElement element,
            BasicNode[] predecessor) {
        final BaseElement baseElement = element.getBaseElement();
        final BpmnModelElementInstance scopeElement = baseElement.getScope();

        String scopeElementId = null;
        if (scopeElement != null) {
            scopeElementId = getProcessScope(scopeElement);
        }

        final ArrayList<String> signals = new ArrayList<>();
        final ArrayList<String> messages = new ArrayList<>();
        final ArrayList<String> links = new ArrayList<>();
        Collection<EventDefinition> eventDefinitions = new HashSet<>();

        if (element.getBaseElement() instanceof CatchEvent) {
            eventDefinitions = ((CatchEvent) element.getBaseElement()).getEventDefinitions();
        } else if (element.getBaseElement() instanceof ThrowEvent) {
            eventDefinitions = ((ThrowEvent) element.getBaseElement()).getEventDefinitions();
        } else if (element.getBaseElement() instanceof ReceiveTask) {
            Message msg = ((ReceiveTask) element.getBaseElement()).getMessage();
            if (msg != null) {
                messages.add(msg.getName());
            }
        }

        for (EventDefinition eventDefinition : eventDefinitions) {
            if (eventDefinition instanceof SignalEventDefinition) {
                if (Objects.nonNull(((SignalEventDefinition) eventDefinition).getSignal())) {
                    signals.add(((SignalEventDefinition) eventDefinition).getSignal().getName());
                }
            } else if (eventDefinition instanceof MessageEventDefinition) {
                if (Objects.nonNull(((MessageEventDefinition) eventDefinition).getMessage())) {
                    messages.add(((MessageEventDefinition) eventDefinition).getMessage().getName());
                }
            } else if (eventDefinition instanceof LinkEventDefinition) {
                links.add(((LinkEventDefinition) eventDefinition).getName());
            }
        }

        for (String signalName : signals) {
            parseJuelExpression(element, ElementChapter.SIGNAL, KnownElementFieldType.Signal, signalName,
                    scopeElementId, predecessor);
        }
        for (String messageName : messages) {
            parseJuelExpression(element, ElementChapter.MESSAGE, KnownElementFieldType.Message, messageName,
                    scopeElementId, predecessor);
        }
        for (String linkName : links) {
            parseJuelExpression(element, ElementChapter.LINK, KnownElementFieldType.Link, linkName, scopeElementId,
                    predecessor);
        }
    }

    /**
     * Retrieves process variables from input / output parameters.
     *
     * @param element           BpmnElement
     * @param extensionElements Extension elements
     * @param predecessor       Current predecessor
     * @param input             true if input parameters are processed, false for output parameters
     */
    void processInputOutputParameters(BpmnElement element, ExtensionElements extensionElements, BasicNode[] predecessor,
            boolean input) {
        final BaseElement baseElement = element.getBaseElement();

        BasicNode node;
        if (input) {
            node = new BasicNode(element, ElementChapter.INPUT_OUTPUT,
                    KnownElementFieldType.InputParameter);
        } else {
            node = new BasicNode(element, ElementChapter.INPUT_OUTPUT,
                    KnownElementFieldType.OutputParameter);
        }

        String parentScope = null;
        final BpmnModelElementInstance scopeElement = baseElement.getScope();
        if (scopeElement != null) {
            parentScope = getProcessScope(scopeElement);
        }

        for (ModelElementInstance extension : extensionElements.getElements()) {
            String textContent;
            String name;
            String writeScope;
            String readScope;
            BpmnModelElementInstance value;
            KnownElementFieldType fieldType;

            if (extension instanceof CamundaInputOutput) {
                if (input) {
                    fieldType = KnownElementFieldType.InputParameter;
                    writeScope = baseElement.getId();
                    readScope = parentScope;

                    for (CamundaInputParameter inputParameter : ((CamundaInputOutput) extension)
                            .getCamundaInputParameters()) {
                        textContent = inputParameter.getTextContent();
                        name = inputParameter.getCamundaName();
                        value = inputParameter.getValue();
                        handleIOParameter(element, node, fieldType, textContent, name, writeScope, readScope, value,
                                predecessor);
                    }
                } else {
                    fieldType = KnownElementFieldType.OutputParameter;
                    writeScope = parentScope;
                    readScope = baseElement.getId();
                    for (CamundaOutputParameter outputParameter : ((CamundaInputOutput) extension)
                            .getCamundaOutputParameters()) {
                        textContent = outputParameter.getTextContent();
                        name = outputParameter.getCamundaName();
                        value = outputParameter.getValue();
                        handleIOParameter(element, node, fieldType, textContent, name, writeScope, readScope, value,
                                predecessor);
                    }
                }
            }
        }
        if (node.getOperations().size() > 0) {
            predecessor[0] = addNodeAndGetNewPredecessor(node, element.getControlFlowGraph(), predecessor[0]);
        }
    }

    /**
     * Processes input / output parameters.
     *
     * @param element     BpmnElement with I/O extension
     * @param node        BasicNode that will contain the process variables
     * @param fieldType   InputParameter or OutputParameter
     * @param textContent Text content of parameter
     * @param name        Name of parameter
     * @param writeScope  Scope if a write access is detected
     * @param readScope   Scope if a read access is detected
     * @param value       Value of parameter
     * @param predecessor BasicNode Predecessor if exists
     */
    private void handleIOParameter(BpmnElement element, BasicNode node,
            KnownElementFieldType fieldType,
            String textContent, String name,
            String writeScope, String readScope, BpmnModelElementInstance value, BasicNode[] predecessor) {

        if (textContent.isEmpty()) {
            IssueWriter.createSingleIssue(this.rule, CriticalityEnum.WARNING, element,
                    element.getProcessDefinition(),
                    Messages.getString("ProcessVariableReader.1"));
        } else {
            node.addOperation(
                    new ProcessVariableOperation(name,
                            VariableOperation.WRITE,
                            writeScope));

            if (value != null) {
                if (value instanceof CamundaList) {
                    CamundaList list = (CamundaList) value;
                    for (BpmnModelElementInstance listValue : list.getValues()) {
                        String listText = listValue.getTextContent();
                        if (listText.startsWith("${")) {
                            // Parse expression
                            parseJuelExpression(element, ElementChapter.INPUT_OUTPUT, fieldType, listText,
                                    readScope, predecessor);
                        }
                    }
                } else if (value instanceof CamundaMap) {
                    for (CamundaEntry entry : ((CamundaMap) value)
                            .getCamundaEntries()) {
                        if (entry.getTextContent().startsWith("${")) {
                            // Parse expression
                            // Not sure about the scope because read is above ?
                            parseJuelExpression(element, ElementChapter.INPUT_OUTPUT, fieldType,
                                    entry.getTextContent(),
                                    readScope, predecessor);
                        }
                    }
                } else if (value instanceof CamundaScript) {
                    IssueWriter.createSingleIssue(this.rule, CriticalityEnum.ERROR, element,
                            element.getProcessDefinition(),
                            Messages.getString("ProcessVariableReader.2"));
                }

            } else {
                parseJuelExpression(element, ElementChapter.INPUT_OUTPUT, fieldType,
                        textContent,
                        readScope, predecessor);
            }
        }
    }

    /**
     * @param element           BpmnElement
     * @param extensionElements Extension elements
     * @param predecessor       Predecessor
     */
    private void searchExtensionsElements(final BpmnElement element,
            final ExtensionElements extensionElements, BasicNode[] predecessor) {

        final BaseElement baseElement = element.getBaseElement();
        final BpmnModelElementInstance scopeElement = baseElement.getScope();
        String scopeId = null;
        if (scopeElement != null) {
            scopeId = getProcessScope(scopeElement);
        }

        // 1) Search in Task Listeners
        getVariablesFromTaskListener(element,
                extensionElements, scopeId, predecessor);

        // 2) Search in Form Data
        getVariablesFromFormData(element, extensionElements, scopeId, predecessor);
    }

    /**
     * Get process variables from execution listeners
     *
     * @param element           Current BPMN Element
     * @param extensionElements Extension elements (e.g. Listeners)
     * @param scopeId           Scope ID
     * @param listenerChapter   Listener chapter
     * @param predecessor       Predecessor
     */
    private void getVariablesFromExecutionListener(
            final BpmnElement element,
            final ExtensionElements extensionElements, final String scopeId, final ElementChapter listenerChapter,
            BasicNode[] predecessor) {

        List<CamundaExecutionListener> listenerList = extensionElements.getElementsQuery()
                .filterByType(CamundaExecutionListener.class).list();
        for (final CamundaExecutionListener listener : listenerList) {
            if ((listenerChapter.equals(ElementChapter.EXECUTION_LISTENER_START) && listener.getCamundaEvent()
                    .equals("start"))
                    || (listenerChapter.equals(ElementChapter.EXECUTION_LISTENER_END) && listener.getCamundaEvent()
                    .equals("end"))
            ) {
                final String l_expression = listener.getCamundaExpression();
                if (l_expression != null) {
                    parseJuelExpression(element, listenerChapter, KnownElementFieldType.Expression, l_expression,
                            scopeId, predecessor);
                }
                final String l_delegateExpression = listener.getCamundaDelegateExpression();
                if (l_delegateExpression != null) {
                    parseJuelExpression(element, listenerChapter, KnownElementFieldType.DelegateExpression,
                            l_delegateExpression, scopeId, predecessor);
                }
                final String l_class = listener.getCamundaClass();
                if (l_class != null) {
                    JavaReaderStatic.getVariablesFromJavaDelegate(listener.getCamundaClass(), element, listenerChapter,
                            KnownElementFieldType.Class, predecessor);
                }
                final CamundaScript script = listener.getCamundaScript();
                if (script != null && script.getCamundaScriptFormat() != null
                        && script.getCamundaScriptFormat().equals(ConfigConstants.GROOVY)) {
                    // inline script or external file?
                    final String inlineScript = script.getTextContent();
                    if (inlineScript != null && inlineScript.trim().length() > 0) {
                        ResourceFileReader.searchProcessVariablesInCode(element,
                                listenerChapter, KnownElementFieldType.InlineScript, null,
                                scopeId, inlineScript, predecessor);
                    } else {
                        final String resourcePath = script.getCamundaResource();
                        if (resourcePath != null) {
                            getVariablesFromGroovyScript(resourcePath, element,
                                    listenerChapter,
                                    scopeId, predecessor);
                        }
                    }
                }
            }
        }
    }

    /**
     * Get process variables from task listeners
     *
     * @param element           BpmnElement
     * @param extensionElements ExtensionElements
     * @param scopeId           ScopeId
     * @param predecessor       Predecessor
     */
    private void getVariablesFromTaskListener(
            final BpmnElement element,
            final ExtensionElements extensionElements, final String scopeId,
            BasicNode[] predecessor) {

        List<CamundaTaskListener> listenerList = extensionElements.getElementsQuery()
                .filterByType(CamundaTaskListener.class).list();
        for (final CamundaTaskListener listener : listenerList) {
            final String l_expression = listener.getCamundaExpression();
            if (l_expression != null) {
                parseJuelExpression(element, ElementChapter.TASK_LISTENER, KnownElementFieldType.Expression,
                        l_expression, scopeId, predecessor);
            }
            final String l_delegateExpression = listener.getCamundaDelegateExpression();
            if (l_delegateExpression != null) {
                parseJuelExpression(element, ElementChapter.TASK_LISTENER, KnownElementFieldType.DelegateExpression,
                        l_delegateExpression, scopeId, predecessor);
            }

            String filePath = "";
            if (listener.getCamundaClass() != null && listener.getCamundaClass().trim().length() > 0) {
                filePath = listener.getCamundaClass().replaceAll("\\.", "/") + JAVA_FILE_ENDING;
            }

            ResourceFileReader.readResourceFile(filePath, element, ElementChapter.TASK_LISTENER,
                    KnownElementFieldType.Class, scopeId, predecessor);

            final CamundaScript script = listener.getCamundaScript();
            if (script != null && script.getCamundaScriptFormat() != null
                    && script.getCamundaScriptFormat().equals(ConfigConstants.GROOVY)) {
                // inline script or external file?
                final String inlineScript = script.getTextContent();
                if (inlineScript != null && inlineScript.trim().length() > 0) {
                    ResourceFileReader.searchProcessVariablesInCode(element, ElementChapter.TASK_LISTENER,
                            KnownElementFieldType.InlineScript, null, scopeId, inlineScript, predecessor);
                } else {
                    final String resourcePath = script.getCamundaResource();
                    if (resourcePath != null) {
                        getVariablesFromGroovyScript(resourcePath, element,
                                ElementChapter.TASK_LISTENER, scopeId, predecessor);
                    }
                }
            }
        }
    }

    /**
     * Get process variables from form fields (user tasks)
     *
     * @param element           BpmnElement
     * @param extensionElements ExtensionElements
     * @param scopeElementId    ScopeElementId
     * @param predecessor       Predecessor
     */
    private void getVariablesFromFormData(final BpmnElement element,
            final ExtensionElements extensionElements, final String scopeElementId, BasicNode[] predecessor) {
        BasicNode node = new BasicNode(element, ElementChapter.FORM_DATA, KnownElementFieldType.FormField);

        final Query<CamundaFormData> formDataQuery = extensionElements.getElementsQuery()
                .filterByType(CamundaFormData.class);
        if (formDataQuery.count() > 0) {
            final CamundaFormData formData = formDataQuery.singleResult();
            if (formData != null) {
                final Collection<CamundaFormField> formFields = formData.getCamundaFormFields();
                for (final CamundaFormField field : formFields) {
                    node.addOperation(new ProcessVariableOperation(field.getCamundaId(), VariableOperation.WRITE,
                            scopeElementId));
                }
            }
        }
        if (node.getOperations().size() > 0) {
            predecessor[0] = addNodeAndGetNewPredecessor(node, element.getControlFlowGraph(), predecessor[0]);
        }
    }

    /**
     * Get process variables from camunda input/output associations (call
     * activities)
     *
     * @param element           BpmnElement
     * @param extensionElements ExtensionElements
     * @param scopeId           ScopeId
     * @param predecessor       Predecessor
     */
    private void searchVariablesInInputOutputExtensions(final BpmnElement element,
            final ExtensionElements extensionElements, final String scopeId,
            BasicNode[] predecessor) {
        final BaseElement baseElement = element.getBaseElement();
        if (baseElement instanceof CallActivity) {
            searchVariablesInInMapping(element, extensionElements, scopeId, predecessor);
            searchVariablesInOutMapping(element, extensionElements, scopeId,
                    predecessor);
        }
    }

    private void searchVariablesInInMapping(final BpmnElement element,
            final ExtensionElements extensionElements, final String scopeId,
            BasicNode[] predecessor) {
        final BaseElement baseElement = element.getBaseElement();
        BasicNode node = new BasicNode(element, ElementChapter.INPUT_DATA,
                KnownElementFieldType.CamundaIn);
        final List<CamundaIn> inputAssociations = extensionElements.getElementsQuery().filterByType(CamundaIn.class)
                .list();

        for (final CamundaIn inputAssociation : inputAssociations) {
            String sourceExpr = inputAssociation.getCamundaSourceExpression();
            if (sourceExpr != null && !sourceExpr.isEmpty()) {
                parseJuelExpression(element, ElementChapter.INPUT_DATA,
                        KnownElementFieldType.CamundaIn,
                        sourceExpr, scopeId, predecessor);
            } else if (inputAssociation.getCamundaVariables() != null && inputAssociation.getCamundaVariables()
                    .equals("all")) {
                // Handle all mapping in flow analysis
                element.getFlowAnalysis()
                        .addCallActivityAllInMapping(((CallActivity) element.getBaseElement()).getCalledElement());
                return;
            } else if (inputAssociation.getCamundaSource() == null) {
                // Delegate variable mapping is probably defined
                return;
            } else {
                node.addOperation(
                        new ProcessVariableOperation(inputAssociation.getCamundaSource(), VariableOperation.READ,
                                scopeId));
            }

            // Add target operation
            String target = inputAssociation.getCamundaTarget();

            node.addOperation(new ProcessVariableOperation(target, VariableOperation.WRITE,
                    ((CallActivity) baseElement).getCalledElement()));
        }
        if (node.getOperations().size() > 0) {
            predecessor[0] = addNodeAndGetNewPredecessor(node, element.getControlFlowGraph(), predecessor[0]);
        }
    }

    private void searchVariablesInOutMapping(final BpmnElement element,
            final ExtensionElements extensionElements, final String scopeId,
            BasicNode[] predecessor) {
        final BaseElement baseElement = element.getBaseElement();
        BasicNode node = new BasicNode(element, ElementChapter.OUTPUT_DATA,
                KnownElementFieldType.CamundaOut);
        final List<CamundaOut> outputAssociations = extensionElements.getElementsQuery()
                .filterByType(CamundaOut.class).list();

        for (final CamundaOut outputAssociation : outputAssociations) {
            String sourceExp = outputAssociation.getCamundaSourceExpression();
            if (!(sourceExp == null || sourceExp.isEmpty())) {
                parseJuelExpression(element, ElementChapter.OUTPUT_DATA, KnownElementFieldType.CamundaOut,
                        sourceExp, ((CallActivity) baseElement).getCalledElement(), predecessor);
            } else if (outputAssociation.getCamundaVariables() != null && outputAssociation.getCamundaVariables()
                    .equals("all")) {
                // Handle all mapping in flow analysis
                element.getFlowAnalysis()
                        .addCallActivityAllOutMapping(((CallActivity) element.getBaseElement()).getCalledElement());
                return;
            } else if (outputAssociation.getCamundaSource() == null) {
                // Delegate variable mapping is probably defined
                return;
            } else {
                node.addOperation(
                        new ProcessVariableOperation(outputAssociation.getCamundaSource(), VariableOperation.READ,
                                ((CallActivity) baseElement).getCalledElement()));
            }

            final String target = outputAssociation.getCamundaTarget();
            if (target != null && !target.isEmpty()) {
                node.addOperation(new ProcessVariableOperation(target, VariableOperation.WRITE, scopeId));
            }
        }
        if (node.getOperations().

                size() > 0) {
            predecessor[0] = addNodeAndGetNewPredecessor(node, element.getControlFlowGraph(), predecessor[0]);
        }

    }

    /**
     * Get process variables from sequence flow conditions
     *
     * @param element     BpmnElement
     * @param predecessor Predecessor
     */
    private void searchVariablesFromSequenceFlow(BpmnElement element,
            BasicNode[] predecessor) {

        final BaseElement baseElement = element.getBaseElement();
        if (baseElement instanceof SequenceFlow) {
            final SequenceFlow flow = (SequenceFlow) baseElement;
            BpmnModelElementInstance scopeElement = flow.getScope();
            String scopeId = null;
            if (scopeElement != null) {
                scopeId = getProcessScope(scopeElement);
            }
            final ConditionExpression expression = flow.getConditionExpression();
            if (expression != null) {
                if (expression.getLanguage() != null && expression.getLanguage().equals(ConfigConstants.GROOVY)) {
                    // inline script or external file?
                    final String inlineScript = expression.getTextContent();
                    if (inlineScript != null && inlineScript.trim().length() > 0) {
                        ResourceFileReader.searchProcessVariablesInCode(element, ElementChapter.DETAILS,
                                KnownElementFieldType.InlineScript, scopeId, null, inlineScript, predecessor);
                    } else {
                        final String resourcePath = expression.getCamundaResource();
                        if (resourcePath != null) {
                            getVariablesFromGroovyScript(resourcePath, element, ElementChapter.DETAILS,
                                    scopeId, predecessor);
                        }
                    }
                } else {
                    if (expression.getTextContent().trim().length() > 0) {
                        parseJuelExpression(element, ElementChapter.DETAILS, KnownElementFieldType.Expression,
                                expression.getTextContent(), scopeId, predecessor);
                    }
                }
            }
        }
    }

    /**
     * Analyse all types of tasks for process variables
     *
     * @param element     BpmnElement
     * @param predecessor Predecessor
     */
    private void getVariablesFromTask(
            final BpmnElement element,
            BasicNode[] predecessor) {

        final BaseElement baseElement = element.getBaseElement();
        BpmnModelElementInstance scopeElement = baseElement.getScope();

        String scopeId = null;
        if (scopeElement != null) {
            scopeId = getProcessScope(scopeElement);
        }
        if (baseElement instanceof ServiceTask || baseElement instanceof SendTask
                || baseElement instanceof BusinessRuleTask) {
            final String t_expression = baseElement.getAttributeValueNs(BpmnModelConstants.CAMUNDA_NS,
                    BpmnConstants.ATTR_EX);
            if (t_expression != null) {
                parseJuelExpression(element, ElementChapter.IMPLEMENTATION, KnownElementFieldType.Expression,
                        t_expression, scopeId, predecessor);
            }

            final String t_delegateExpression = baseElement.getAttributeValueNs(BpmnModelConstants.CAMUNDA_NS,
                    BpmnConstants.ATTR_DEL);
            if (t_delegateExpression != null) {
                parseJuelExpression(element, ElementChapter.IMPLEMENTATION, KnownElementFieldType.DelegateExpression,
                        t_delegateExpression, scopeId, predecessor);
            }

            final List<String> t_fieldInjectionExpressions = BpmnScanner
                    .getFieldInjectionExpression(baseElement);
            if (!t_fieldInjectionExpressions.isEmpty()) {
                for (String t_fieldInjectionExpression : t_fieldInjectionExpressions)
                    parseJuelExpression(element, ElementChapter.FIELD_INJECTIONS, KnownElementFieldType.Expression,
                            t_fieldInjectionExpression, scopeId, predecessor);
            }

            final String t_resultVariable = baseElement.getAttributeValueNs(BpmnModelConstants.CAMUNDA_NS,
                    BpmnConstants.RESULT_VARIABLE);
            if (t_resultVariable != null && t_resultVariable.trim().length() > 0) {

                new ProcessVariableOperation(t_resultVariable, VariableOperation.WRITE, scopeId);
            }

            if (baseElement.getAttributeValueNs(BpmnModelConstants.CAMUNDA_NS, BpmnConstants.ATTR_CLASS) != null) {
                JavaReaderStatic
                        .getVariablesFromJavaDelegate(baseElement.getAttributeValueNs(BpmnModelConstants.CAMUNDA_NS,
                                BpmnConstants.ATTR_CLASS),
                                element, ElementChapter.IMPLEMENTATION, KnownElementFieldType.Class,
                                predecessor);
            }

            if (baseElement instanceof BusinessRuleTask) {
                final String t_decisionRef = baseElement.getAttributeValueNs(BpmnModelConstants.CAMUNDA_NS,
                        BpmnConstants.DECISION_REF);
                if (t_decisionRef != null && t_decisionRef.trim().length() > 0 && decisionRefToPathMap != null) {
                    final String fileName = decisionRefToPathMap.get(t_decisionRef);
                    if (fileName != null) {
                        readDmnFile(t_decisionRef, fileName, element,
                                scopeId);
                    }
                }
            }

        } else if (baseElement instanceof UserTask) {
            final UserTask userTask = (UserTask) baseElement;
            final String assignee = userTask.getCamundaAssignee();
            if (assignee != null)
                parseJuelExpression(element, ElementChapter.DETAILS, KnownElementFieldType.Assignee,
                        assignee, scopeId, predecessor);
            final String candidateUsers = userTask.getCamundaCandidateUsers();
            if (candidateUsers != null)
                parseJuelExpression(element, ElementChapter.DETAILS, KnownElementFieldType.CandidateUsers,
                        candidateUsers, scopeId, predecessor);
            final String candidateGroups = userTask.getCamundaCandidateGroups();
            if (candidateGroups != null)
                parseJuelExpression(element, ElementChapter.DETAILS, KnownElementFieldType.CandidateGroups,
                        candidateGroups, scopeId, predecessor);
            final String dueDate = userTask.getCamundaDueDate();
            if (dueDate != null)
                parseJuelExpression(element, ElementChapter.DETAILS, KnownElementFieldType.DueDate,
                        dueDate, scopeId, predecessor);
            final String followUpDate = userTask.getCamundaFollowUpDate();
            if (followUpDate != null)
                parseJuelExpression(element, ElementChapter.DETAILS, KnownElementFieldType.FollowUpDate,
                        followUpDate, scopeId, predecessor);

        } else if (baseElement instanceof ScriptTask) {
            // Examine script task for process variables
            final ScriptTask scriptTask = (ScriptTask) baseElement;
            if (scriptTask.getScriptFormat() != null && scriptTask.getScriptFormat().equals(ConfigConstants.GROOVY)) {
                // inline script or external file?
                final Script script = scriptTask.getScript();
                if (script != null && script.getTextContent() != null && script.getTextContent().trim().length() > 0) {
                    ResourceFileReader.searchProcessVariablesInCode(element, ElementChapter.DETAILS,
                            KnownElementFieldType.InlineScript, null, scopeId, script.getTextContent(), predecessor);
                } else {
                    final String resourcePath = scriptTask.getCamundaResource();
                    if (resourcePath != null) {
                        getVariablesFromGroovyScript(resourcePath, element,
                                ElementChapter.DETAILS, scopeId, predecessor);
                    }
                }
            }
            String resultVariable = scriptTask.getCamundaResultVariable();
            if (resultVariable != null && resultVariable.trim().length() > 0) {

                new ProcessVariableOperation(resultVariable, VariableOperation.WRITE, scopeId);
            }
        } else if (baseElement instanceof CallActivity) {
            final CallActivity callActivity = (CallActivity) baseElement;
            final String calledElement = callActivity.getCalledElement();
            if (calledElement != null && calledElement.trim().length() > 0) {
                parseJuelExpression(element, ElementChapter.DETAILS, KnownElementFieldType.CalledElement,
                        calledElement, scopeId, predecessor);
            }
            final String caseRef = callActivity.getAttributeValueNs(BpmnModelConstants.CAMUNDA_NS,
                    BpmnConstants.CASE_REF);
            if (caseRef != null && caseRef.trim().length() > 0) {
                parseJuelExpression(element, ElementChapter.DETAILS, KnownElementFieldType.CaseRef,
                        caseRef, scopeId, predecessor);
            }

            // Check DelegateVariableMapping
            if (baseElement.getAttributeValueNs(BpmnModelConstants.CAMUNDA_NS,
                    BpmnConstants.ATTR_VAR_MAPPING_CLASS) != null) {
                JavaReaderStatic
                        .getVariablesFromJavaDelegate(baseElement.getAttributeValueNs(BpmnModelConstants.CAMUNDA_NS,
                                BpmnConstants.ATTR_VAR_MAPPING_CLASS),
                                element, ElementChapter.GENERAL, KnownElementFieldType.Class, predecessor);
            } else if (baseElement.getAttributeValueNs(BpmnModelConstants.CAMUNDA_NS,
                    BpmnConstants.ATTR_VAR_MAPPING_DELEGATE) != null) {
                parseJuelExpression(element, ElementChapter.GENERAL, KnownElementFieldType.Class,
                        callActivity.getCamundaVariableMappingDelegateExpression(), scopeId, predecessor);

            }
        }

    }

    public String getProcessScope(BpmnModelElementInstance scopeElement) {
        if (scopeElement instanceof SubProcess) {
            return scopeElement.getParentElement().getAttributeValue(BpmnConstants.ATTR_ID);
        } else {
            return scopeElement.getAttributeValue(BpmnConstants.ATTR_ID);
        }
    }

    /**
     * Examine multi instance tasks for process variables
     *
     * @param element     BpmnElement
     * @param predecessor Predecessor
     */
    private void searchVariablesInMultiInstanceTask(
            final BpmnElement element,
            BasicNode[] predecessor) {

        final BaseElement baseElement = element.getBaseElement();
        BpmnModelElementInstance scopeElement = baseElement.getScope();
        String scopeId = null;
        if (scopeElement != null) {
            scopeId = element.getId();
        }
        final ModelElementInstance loopCharacteristics = baseElement
                .getUniqueChildElementByType(LoopCharacteristics.class);
        if (loopCharacteristics != null) {

            BasicNode node = new BasicNode(element,
                    ElementChapter.MULTI_INSTANCE, KnownElementFieldType.CamundaStandardVariables);

            // Add default variables
            addDefaultMultiInstanceTaskVariables(node, scopeId);
            predecessor[0] = addNodeAndGetNewPredecessor(node, element.getControlFlowGraph(), predecessor[0]);

            node = new BasicNode(element,
                    ElementChapter.MULTI_INSTANCE, KnownElementFieldType.LoopCharacteristics);

            final String collectionName = loopCharacteristics.getAttributeValueNs(BpmnModelConstants.CAMUNDA_NS,
                    BpmnConstants.COLLECTION);
            if (collectionName != null && collectionName.trim().length() > 0) {
                // Check if collection name includes expression
                final Pattern pattern = Pattern.compile("\\$\\{.*}");
                Matcher matcher = pattern.matcher(collectionName);
                if (matcher.matches()) {
                    parseJuelExpression(element, ElementChapter.MULTI_INSTANCE,
                            KnownElementFieldType.CollectionElement,
                            collectionName, scopeId, predecessor);
                } else {
                    node.addOperation(
                            new ProcessVariableOperation(collectionName, VariableOperation.READ, scopeId));
                }
            }
            final String elementVariable = loopCharacteristics.getAttributeValueNs(BpmnModelConstants.CAMUNDA_NS,
                    BpmnConstants.ELEMENT_VARIABLE);
            if (elementVariable != null && elementVariable.trim().length() > 0) {
                node.addOperation(
                        new ProcessVariableOperation(elementVariable,
                                VariableOperation.WRITE, KnownElementFieldType.ElementVariable, scopeId));
            }
            final ModelElementInstance loopCardinality = loopCharacteristics
                    .getUniqueChildElementByType(LoopCardinality.class);
            if (loopCardinality != null) {
                final String cardinality = loopCardinality.getTextContent();

                if (cardinality != null && cardinality.trim().length() > 0) {
                    if (!cardinality.matches("\\d+")) {
                        parseJuelExpression(element, ElementChapter.MULTI_INSTANCE,
                                KnownElementFieldType.LoopCardinality,
                                cardinality, scopeId, predecessor);
                    }
                }
            }
            final ModelElementInstance completionCondition = loopCharacteristics
                    .getUniqueChildElementByType(CompletionCondition.class);
            if (completionCondition != null) {
                final String completionConditionExpression = completionCondition.getTextContent();
                if (completionConditionExpression != null && completionConditionExpression.trim().length() > 0) {
                    parseJuelExpression(element, ElementChapter.MULTI_INSTANCE,
                            KnownElementFieldType.CompletionCondition, completionConditionExpression, scopeId,
                            predecessor);
                }
            }
            final ModelElementInstance loopDataInputRef = loopCharacteristics
                    .getUniqueChildElementByType(LoopDataInputRef.class);
            if (loopDataInputRef != null) {
                final String dataInputRefName = loopDataInputRef.getTextContent();
                if (dataInputRefName != null && dataInputRefName.trim().length() > 0) {
                    node.addOperation(
                            new ProcessVariableOperation(dataInputRefName, VariableOperation.READ, scopeId));
                }
            }
            final ModelElementInstance inputDataItem = loopCharacteristics
                    .getUniqueChildElementByType(InputDataItem.class);
            if (inputDataItem != null) {
                final String inputDataItemName = inputDataItem.getAttributeValue("name");
                if (inputDataItemName != null && inputDataItemName.trim().length() > 0) {
                    node.addOperation(
                            new ProcessVariableOperation(inputDataItemName, VariableOperation.WRITE, scopeId));
                }
            }
            if (node.getOperations().size() > 0) {
                predecessor[0] = addNodeAndGetNewPredecessor(node, element.getControlFlowGraph(), predecessor[0]);
            }
        }
    }

    private void addDefaultMultiInstanceTaskVariables(BasicNode node, String scopeElementId) {
        node.addOperation(new ProcessVariableOperation("nrOfInstances", VariableOperation.WRITE,
                scopeElementId));

        node.addOperation(new ProcessVariableOperation("nrOfActiveInstances", VariableOperation.WRITE,
                scopeElementId));

        node.addOperation(new ProcessVariableOperation("nrOfCompletedInstances", VariableOperation.WRITE,
                scopeElementId));

        node.addOperation(new ProcessVariableOperation("loopCounter", VariableOperation.WRITE,
                scopeElementId));
    }

    /**
     * Checks an external groovy script for process variables (read/write).
     *
     * @param groovyFile  Groovy File
     * @param element     BpmnElement
     * @param chapter     ElementChapter
     * @param scopeId     ScopeId
     * @param predecessor Predecessor
     */
    private void getVariablesFromGroovyScript(final String groovyFile,
            final BpmnElement element, final ElementChapter chapter,
            final String scopeId, BasicNode[] predecessor) {

        ResourceFileReader
                .readResourceFile(groovyFile, element, chapter, KnownElementFieldType.ExternalScript, scopeId,
                        predecessor);
    }

    /**
     * Scans a dmn file for process variables
     *
     * @param decisionId DecisionId
     * @param fileName   File Name
     * @param element    BpmnElement
     * @param scopeId    ScopeID
     */
    private void readDmnFile(final String decisionId,
            final String fileName,
            final BpmnElement element,
            final String scopeId) {

        BasicNode node = new BasicNode(element, ElementChapter.DETAILS, KnownElementFieldType.DMN);

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
                    node.addOperation(
                            new ProcessVariableOperation(variable.getTextContent(), VariableOperation.READ, scopeId));
                }
                final Collection<Output> outputs = decision.getModelInstance().getModelElementsByType(Output.class);
                for (final Output output : outputs) {
                    final String variable = output.getName();
                    node.addOperation(
                            new ProcessVariableOperation(variable,
                                    VariableOperation.WRITE, scopeId));
                }
            }
        }

        if (node.getOperations().size() > 0) {
            element.getControlFlowGraph().addNode(node);
        }
    }

    public void parseJuelExpression(BpmnElement element,
            ElementChapter elementChapter,
            KnownElementFieldType fieldType,
            String expression, String scopeId, BasicNode[] predecessor) {
        ExpressionNode expNode = new ExpressionNode(element, expression, elementChapter, fieldType);

        TreeStore store = new TreeStore(new Builder(Builder.Feature.METHOD_INVOCATIONS), null);
        if (Objects.isNull(expression)) {
            return;
        }
        Tree tree = store.get(expression);

        // Only support simple expressions at the moment (only one method call or only simple reads)
        if (tree.getRoot().getChild(0) instanceof AstMethod) {
            AstMethod method = (AstMethod) tree.getRoot().getChild(0);
            AstDot property = (AstDot) method.getChild(0);
            AstParameters parameters = (AstParameters) method.getChild(1);
            if (property.getChild(0) instanceof AstIdentifier) {
                String objectName = ((AstIdentifier) property.getChild(0)).getName();
                if (objectName.equals(CamundaMethodServices.EXECUTION_OBJECT) || objectName
                        .equals(CamundaMethodServices.TASK_OBJECT)) {
                    handleExecutionInExpression(parameters, property, scopeId, expNode);
                } else {
                    handleMethodCallInExpression(parameters, property, objectName, scopeId, expNode, element,
                            elementChapter, fieldType, predecessor);
                }
            }
        } else {
            for (Node n : tree.getIdentifierNodes()) {
                String varName = ((AstIdentifier) n).getName();

                // Check if bean is called
                final String className = isBean(varName);
                if (className != null) {
                    // read variables in class file (bean)
                    JavaReaderStatic.getVariablesFromJavaDelegate(className, element,
                            elementChapter, fieldType, predecessor);
                } else {
                    // Read operation
                    expNode.addOperation(new ProcessVariableOperation(varName,
                            VariableOperation.READ, scopeId));
                }
            }
        }

        if (expression.startsWith(" <![CDATA[") || !expression.startsWith("${")) {
            extractVariablesFromCode(expression, scopeId, expNode);
        }

        if (expNode.getOperations().size() > 0) {
            predecessor[0] = addNodeAndGetNewPredecessor(expNode, element.getControlFlowGraph(), predecessor[0]);
        }
    }

    private void extractVariablesFromCode(String expression, String scopeId, ExpressionNode expNode) {
        // extract read variables
        ListMultimap<String, ProcessVariableOperation> readOperations = ResourceFileReader
                .searchReadProcessVariablesInCode(
                        scopeId, expression);
        readOperations.asMap().forEach((key, value) -> value.forEach(expNode::addOperation));

        // extract written variables
        ListMultimap<String, ProcessVariableOperation> writeOperations = ResourceFileReader
                .searchWrittenProcessVariablesInCode(
                        scopeId, expression);
        writeOperations.asMap().forEach((key, value) -> value.forEach(expNode::addOperation));

        // extract deleted variables
        ListMultimap<String, ProcessVariableOperation> deleteOperations = ResourceFileReader
                .searchRemovedProcessVariablesInCode(
                        scopeId, expression);
        deleteOperations.asMap().forEach((key, value) -> value.forEach(expNode::addOperation));
    }

    private void handleMethodCallInExpression(AstParameters parameters, AstDot property, String objectName,
            String scopeId,
            ExpressionNode expNode, BpmnElement element, ElementChapter elementChapter, KnownElementFieldType fieldType,
            BasicNode[] predecessor) {
        // Check if method on bean is called
        final String className = isBean(objectName);
        if (className != null) {
            boolean containsExecution = false;

            for (int i = 0; i < parameters.getCardinality(); i++) {
                if (parameters.getChild(i) instanceof AstIdentifier) {
                    String varName = ((AstIdentifier) parameters.getChild(i)).getName();
                    if (varName.equals("execution")) {
                        containsExecution = true;
                    } else if (!varName.equals("task") && !varName.equals("caseExecution") && !varName
                            .equals("authenticatedUserId")) {
                        // Exclude variable provided by camunda
                        expNode.addOperation(new ProcessVariableOperation(varName,
                                VariableOperation.READ, scopeId));
                    }
                }
            }

            if (containsExecution) {
                // Call method as it might modify variables
                // REMEMBER: execution might already be saved as field of bean but currently we cannot detect this
                String methodName = property.toString().split(" ")[1];

                EntryPoint entryPoint = new EntryPoint(className, methodName, "", BpmnConstants.ATTR_EX, "");
                // read variables in class file (bean)
                JavaReaderStatic
                        .getVariablesFromClass(className, element, elementChapter, fieldType, entryPoint,
                                predecessor);
            }
        }
    }

    private void handleExecutionInExpression(AstParameters parameters, AstDot property, String scopeId,
            ExpressionNode expNode) {
        String varName = ((AstString) parameters.getChild(0)).toString();
        varName = varName.substring(1, varName.length() - 1);

        switch (property.toString()) {
            case ". setVariable":
                expNode.addOperation(new ProcessVariableOperation(varName,
                        VariableOperation.WRITE, scopeId));
                break;
            case ". getVariable":
                expNode.addOperation(new ProcessVariableOperation(varName,
                        VariableOperation.READ, scopeId));
                break;
            case ". removeVariable":
                expNode.addOperation(new ProcessVariableOperation(varName,
                        VariableOperation.DELETE, scopeId));
                break;
            default:
                LOGGER.warning(String.format("Method %s of execution is not supported.", property.toString()));
        }
    }

    /**
     * Checks a variable being a bean
     *
     * @param variable Name of variable
     * @return classpath to bean definition
     */
    private String isBean(final String variable) {
        if (RuntimeConfig.getInstance().getBeanMapping() != null) {
            return RuntimeConfig.getInstance().getBeanMapping().get(variable);
        }
        return null;
    }

    static BasicNode addNodeAndGetNewPredecessor(BasicNode node, ControlFlowGraph cg,
            BasicNode predecessor) {
        cg.addNode(node);
        if (predecessor != null) {
            node.addPredecessor(predecessor);
        }

        return node;
    }
}
