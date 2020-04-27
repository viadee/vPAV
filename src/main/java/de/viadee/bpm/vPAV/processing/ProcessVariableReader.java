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
package de.viadee.bpm.vPAV.processing;

import com.google.common.collect.ListMultimap;
import de.viadee.bpm.vPAV.BpmnScanner;
import de.viadee.bpm.vPAV.Messages;
import de.viadee.bpm.vPAV.RuntimeConfig;
import de.viadee.bpm.vPAV.config.model.Rule;
import de.viadee.bpm.vPAV.constants.BpmnConstants;
import de.viadee.bpm.vPAV.constants.ConfigConstants;
import de.viadee.bpm.vPAV.output.IssueWriter;
import de.viadee.bpm.vPAV.processing.code.flow.*;
import de.viadee.bpm.vPAV.processing.code.flow.ExpressionNode;
import de.viadee.bpm.vPAV.processing.model.data.*;
import org.camunda.bpm.engine.impl.juel.*;
import org.camunda.bpm.engine.impl.juel.Node;
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

import javax.el.ELException;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * search process variables for an bpmn element
 */
public final class ProcessVariableReader {

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
        final JavaReaderStatic javaReaderStatic = new JavaReaderStatic();
        final BaseElement baseElement = element.getBaseElement();
        final BpmnModelElementInstance scopeElement = baseElement.getScope();
        String scopeElementId = null;
        if (scopeElement != null) {
            scopeElementId = scopeElement.getAttributeValue(BpmnConstants.ATTR_ID);
        }
        final ExtensionElements extensionElements = baseElement.getExtensionElements();

        // 1) Search for variables in multi instance task, if applicable
        searchVariablesInMultiInstanceTask(javaReaderStatic, element, predecessor);

        if (extensionElements != null) {
            // 2) Search variables in Input Parameters
            processInputOutputParameters(element, extensionElements, predecessor, true);

            // 3) Search variables execution listener (start)
            getVariablesFromExecutionListener(javaReaderStatic, element,
                    extensionElements, scopeElementId, ElementChapter.ExecutionListenerStart, predecessor);
        }

        // 4) Search variables in task
        getVariablesFromTask(javaReaderStatic, element, predecessor);

        // 5) Search variables in sequence flow
        searchVariablesFromSequenceFlow(element, predecessor);

        if (extensionElements != null) {
            // 6) Search variables in ExtensionElements
            searchExtensionsElements(javaReaderStatic, element, extensionElements, predecessor);
        }

        // 7) Search variables in Signals and Messages and Links
        getVariablesFromSignalsAndMessagesAndLinks(javaReaderStatic, element, predecessor);

        if (extensionElements != null) {
            // TODO not 100% sure whether this is the right position or not
            // 8) Search in Input/Output-Associations (Call Activities)
            searchVariablesInInputOutputExtensions(javaReaderStatic, element,
                    extensionElements, scopeElementId, predecessor);

            // 9) Search variables execution listener (end)
            getVariablesFromExecutionListener(javaReaderStatic, element,
                    extensionElements, scopeElementId, ElementChapter.ExecutionListenerEnd, predecessor);

            // 10) Search variables in Output Parameters
            processInputOutputParameters(element, extensionElements, predecessor, false);
        }
    }

    /**
     *  Retrieve process variables from signals and messages
     * @param javaReaderStatic Static java reader
     * @param element BpmnElement
     * @param predecessor Predecessor
     */
    public void getVariablesFromSignalsAndMessagesAndLinks(
            final JavaReaderStatic javaReaderStatic, final BpmnElement element,
            BasicNode[] predecessor) {
        final BaseElement baseElement = element.getBaseElement();
        final BpmnModelElementInstance scopeElement = baseElement.getScope();

        String scopeElementId = null;
        if (scopeElement != null) {
            scopeElementId = scopeElement.getAttributeValue(BpmnConstants.ATTR_ID);
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
                signals.add(((SignalEventDefinition) eventDefinition).getSignal().getName());
            } else if (eventDefinition instanceof MessageEventDefinition) {
                messages.add(((MessageEventDefinition) eventDefinition).getMessage().getName());
            } else if (eventDefinition instanceof LinkEventDefinition) {
                links.add(((LinkEventDefinition) eventDefinition).getName());
            }
        }

        for (String signalName : signals) {
            checkMessageAndSignalForExpression(javaReaderStatic, signalName, element,
                    ElementChapter.Signal, KnownElementFieldType.Signal, scopeElementId, predecessor);
        }
        for (String messageName : messages) {
            checkMessageAndSignalForExpression(javaReaderStatic, messageName, element,
                    ElementChapter.Message, KnownElementFieldType.Message, scopeElementId, predecessor);
        }
        for (String linkName : links) {
            checkMessageAndSignalForExpression(javaReaderStatic, linkName, element,
                    ElementChapter.Link, KnownElementFieldType.Link, scopeElementId, predecessor);
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
            node = new BasicNode(element, ElementChapter.InputOutput,
                    KnownElementFieldType.InputParameter);
        } else {
            node = new BasicNode(element, ElementChapter.InputOutput,
                    KnownElementFieldType.OutputParameter);
        }

        String parentScope = null;
        final BpmnModelElementInstance scopeElement = baseElement.getScope();
        if (scopeElement != null) {
            parentScope = scopeElement.getAttributeValue(BpmnConstants.ATTR_ID);
        }

        for (ModelElementInstance extension : extensionElements.getElements()) {
            String textContent, name, writeScope, readScope;
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
            // TODO is this still necessary with predecessor (I don´t think so...)
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
    private void handleIOParameter(BpmnElement element, BasicNode node, KnownElementFieldType fieldType,
            String textContent, String name,
            String writeScope, String readScope, BpmnModelElementInstance value, BasicNode[] predecessor) {

        if (textContent.isEmpty()) {
            IssueWriter.createSingleIssue(this.rule, CriticalityEnum.WARNING, element,
                    element.getProcessDefinition(),
                    Messages.getString("ProcessVariableReader.1")); //$NON-NLS-1$ );
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
                            parseJuelExpression(element, ElementChapter.InputOutput, fieldType, listText,
                                    readScope, predecessor);
                        }
                    }
                } else if (value instanceof CamundaMap) {
                    for (CamundaEntry entry : ((CamundaMap) value)
                            .getCamundaEntries()) {
                        if (entry.getTextContent().startsWith("${")) {
                            // Parse expression
                            // Not sure about the scope because read is above ?
                            parseJuelExpression(element, ElementChapter.InputOutput, fieldType,
                                    entry.getTextContent(),
                                    readScope, predecessor);
                        }
                    }
                } else if (value instanceof CamundaScript) {
                    IssueWriter.createSingleIssue(this.rule, CriticalityEnum.ERROR, element,
                            element.getProcessDefinition(),
                            Messages.getString("ProcessVariableReader.2")); //$NON-NLS-1$ );
                }

            } else {
                parseJuelExpression(element, ElementChapter.InputOutput, fieldType,
                        textContent,
                        readScope, predecessor);
            }
        }
    }

    /**
     *
     * @param javaReaderStatic Static java reader
     * @param element  BpmnElement
     * @param extensionElements Extension elements
     * @param predecessor Predecessor
     */
    private void searchExtensionsElements(
            final JavaReaderStatic javaReaderStatic, final BpmnElement element,
            final ExtensionElements extensionElements, BasicNode[] predecessor) {

        final BaseElement baseElement = element.getBaseElement();
        final BpmnModelElementInstance scopeElement = baseElement.getScope();
        String scopeElementId = null;
        if (scopeElement != null) {
            scopeElementId = scopeElement.getAttributeValue(BpmnConstants.ATTR_ID);
        }

        // 1) Search in Task Listeners
        getVariablesFromTaskListener(javaReaderStatic, element,
                extensionElements, scopeElementId, predecessor);

        // 2) Search in Form Data
        getVariablesFromFormData(element, extensionElements, scopeElementId, predecessor);

    }

    /**
     * Get process variables from execution listeners
     * @param javaReaderStatic Static java reader
     * @param element Current BPMN Element
     * @param extensionElements Extension elements (e.g. Listeners)
     * @param scopeId Scope ID
     * @param listenerChapter Listener chapter
     * @param predecessor Predecessor
     */
    private void getVariablesFromExecutionListener(
            final JavaReaderStatic javaReaderStatic, final BpmnElement element,
            final ExtensionElements extensionElements, final String scopeId, final ElementChapter listenerChapter,
            BasicNode[] predecessor) {

        List<CamundaExecutionListener> listenerList = extensionElements.getElementsQuery()
                .filterByType(CamundaExecutionListener.class).list();
        for (final CamundaExecutionListener listener : listenerList) {
            if ((listenerChapter.equals(ElementChapter.ExecutionListenerStart) && listener.getCamundaEvent()
                    .equals("start"))
                    || (listenerChapter.equals(ElementChapter.ExecutionListenerEnd) && listener.getCamundaEvent()
                    .equals("end"))
            ) {
                final String l_expression = listener.getCamundaExpression();
                if (l_expression != null) {
                    findVariablesInExpression(javaReaderStatic,
                            l_expression, element, listenerChapter,
                            KnownElementFieldType.Expression, scopeId, predecessor);
                }
                final String l_delegateExpression = listener.getCamundaDelegateExpression();
                if (l_delegateExpression != null) {
                    findVariablesInExpression(javaReaderStatic,
                            l_delegateExpression, element, listenerChapter,
                            KnownElementFieldType.DelegateExpression, scopeId, predecessor);
                }
                final String l_class = listener.getCamundaClass();
                if (l_class != null) {
                    javaReaderStatic.getVariablesFromJavaDelegate(listener.getCamundaClass(), element, listenerChapter,
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
     * @param javaReaderStatic  Static java reader
     * @param element           BpmnElement
     * @param extensionElements ExtensionElements
     * @param scopeId           ScopeId
     * @param predecessor Predecessor
     */
    private void getVariablesFromTaskListener(
            final JavaReaderStatic javaReaderStatic, final BpmnElement element,
            final ExtensionElements extensionElements, final String scopeId,
            BasicNode[] predecessor) {

        List<CamundaTaskListener> listenerList = extensionElements.getElementsQuery()
                .filterByType(CamundaTaskListener.class).list();
        for (final CamundaTaskListener listener : listenerList) {
            final String l_expression = listener.getCamundaExpression();
            if (l_expression != null) {
                findVariablesInExpression(javaReaderStatic,
                        l_expression, element, ElementChapter.TaskListener, KnownElementFieldType.Expression, scopeId,
                        predecessor);
            }
            final String l_delegateExpression = listener.getCamundaDelegateExpression();
            if (l_delegateExpression != null) {
                findVariablesInExpression(javaReaderStatic,
                        l_delegateExpression, element, ElementChapter.TaskListener,
                        KnownElementFieldType.DelegateExpression, scopeId, predecessor);
            }

            String filePath = "";
            if (listener.getCamundaClass() != null && listener.getCamundaClass().trim().length() > 0) {
                filePath = listener.getCamundaClass().replaceAll("\\.", "/") + ".java";
            }

            ResourceFileReader.readResourceFile(filePath, element, ElementChapter.TaskListener,
                    KnownElementFieldType.Class, scopeId, predecessor);

            final CamundaScript script = listener.getCamundaScript();
            if (script != null && script.getCamundaScriptFormat() != null
                    && script.getCamundaScriptFormat().equals(ConfigConstants.GROOVY)) {
                // inline script or external file?
                final String inlineScript = script.getTextContent();
                if (inlineScript != null && inlineScript.trim().length() > 0) {
                    ResourceFileReader.searchProcessVariablesInCode(element, ElementChapter.TaskListener,
                            KnownElementFieldType.InlineScript, null, scopeId, inlineScript, predecessor);
                } else {
                    final String resourcePath = script.getCamundaResource();
                    if (resourcePath != null) {
                        getVariablesFromGroovyScript(resourcePath, element,
                                ElementChapter.TaskListener, scopeId, predecessor);
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
     * @param predecessor Predecessor
     */
    private void getVariablesFromFormData(final BpmnElement element,
            final ExtensionElements extensionElements, final String scopeElementId, BasicNode[] predecessor) {
        BasicNode node = new BasicNode(element, ElementChapter.FormData, KnownElementFieldType.FormField);

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
     * @param javaReaderStatic JavaReaderStatic
     * @param element  BpmnElement
     * @param extensionElements  ExtensionElements
     * @param scopeId ScopeId
     * @param predecessor Predecessor
     */
    private void searchVariablesInInputOutputExtensions(
            final JavaReaderStatic javaReaderStatic, final BpmnElement element,
            final ExtensionElements extensionElements, final String scopeId,
            BasicNode[] predecessor) {
        final BaseElement baseElement = element.getBaseElement();
        if (baseElement instanceof CallActivity) {
            searchVariablesInInMapping(javaReaderStatic, element, extensionElements, scopeId, predecessor);
            searchVariablesInOutMapping(javaReaderStatic, element, extensionElements, scopeId,
                    predecessor);
        }
    }

    private void searchVariablesInInMapping(final JavaReaderStatic javaReaderStatic,
            final BpmnElement element,
            final ExtensionElements extensionElements, final String scopeId,
            BasicNode[] predecessor) {
        final BaseElement baseElement = element.getBaseElement();
        BasicNode node = new BasicNode(element, ElementChapter.InputData,
                KnownElementFieldType.CamundaIn);
        final List<CamundaIn> inputAssociations = extensionElements.getElementsQuery().filterByType(CamundaIn.class)
                .list();
        for (final CamundaIn inputAssociation : inputAssociations) {
            String source = inputAssociation.getCamundaSource();
            if (source == null || source.isEmpty()) {
                source = inputAssociation.getCamundaSourceExpression();
                if (source != null && !source.isEmpty()) {
                    findVariablesInExpression(javaReaderStatic,
                            source, element, ElementChapter.InputData,
                            KnownElementFieldType.CamundaIn, scopeId, predecessor);
                } else {
                    continue;
                }

            } else {
                node.addOperation(new ProcessVariableOperation(source, VariableOperation.READ, scopeId));
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

    private void searchVariablesInOutMapping(final JavaReaderStatic javaReaderStatic,
            final BpmnElement element,
            final ExtensionElements extensionElements, final String scopeId,
            BasicNode[] predecessor) {
        final BaseElement baseElement = element.getBaseElement();
        BasicNode node = new BasicNode(element, ElementChapter.OutputData,
                KnownElementFieldType.CamundaOut);
        final List<CamundaOut> outputAssociations = extensionElements.getElementsQuery()
                .filterByType(CamundaOut.class).list();
        for (final CamundaOut outputAssociation : outputAssociations) {
            String source = outputAssociation.getCamundaSource();
            if (source == null || source.isEmpty()) {
                source = outputAssociation.getCamundaSourceExpression();
                findVariablesInExpression(javaReaderStatic,
                        source, element, ElementChapter.OutputData, KnownElementFieldType.CamundaOut,
                        ((CallActivity) baseElement).getCalledElement(), predecessor);
            } else {

                node.addOperation(new ProcessVariableOperation(source, VariableOperation.READ,
                        ((CallActivity) baseElement).getCalledElement()));
            }

            final String target = outputAssociation.getCamundaTarget();
            if (target != null && !target.isEmpty()) {
                node.addOperation(new ProcessVariableOperation(target, VariableOperation.WRITE, scopeId));
            }
        }
        if (node.getOperations().size() > 0) {
            predecessor[0] = addNodeAndGetNewPredecessor(node, element.getControlFlowGraph(), predecessor[0]);
        }
    }

    /**
     * Get process variables from sequence flow conditions
     * @param element BpmnElement
     * @param predecessor Predecessor
     */
    private void searchVariablesFromSequenceFlow(final BpmnElement element,
            BasicNode[] predecessor) {

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
                        ResourceFileReader.searchProcessVariablesInCode(element, ElementChapter.Details,
                                KnownElementFieldType.InlineScript, scopeId, null, inlineScript, predecessor);
                    } else {
                        final String resourcePath = expression.getCamundaResource();
                        if (resourcePath != null) {
                            getVariablesFromGroovyScript(resourcePath, element, ElementChapter.Details,
                                    scopeId, predecessor);
                        }
                    }
                } else {
                    if (expression.getTextContent().trim().length() > 0) {
                        parseJuelExpression(element, ElementChapter.Details, KnownElementFieldType.Expression,
                                expression.getTextContent(), scopeId, predecessor);
                    }
                }
            }
        }
    }

    /**
     * Analyse all types of tasks for process variables
     *
     * @param javaReaderStatic Static java reader
     * @param element          BpmnElement
     * @param predecessor Predecessor
     */
    private void getVariablesFromTask(
            final JavaReaderStatic javaReaderStatic,
            final BpmnElement element,
            BasicNode[] predecessor) {

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
                findVariablesInExpression(javaReaderStatic, t_expression,
                        element, ElementChapter.Implementation, KnownElementFieldType.Expression, scopeId,
                        predecessor);
            }

            final String t_delegateExpression = baseElement.getAttributeValueNs(BpmnModelConstants.CAMUNDA_NS,
                    BpmnConstants.ATTR_DEL);
            if (t_delegateExpression != null) {
                findVariablesInExpression(javaReaderStatic,
                        t_delegateExpression, element, ElementChapter.Implementation,
                        KnownElementFieldType.DelegateExpression, scopeId, predecessor);
            }

            final ArrayList<String> t_fieldInjectionExpressions = BpmnScanner
                    .getFieldInjectionExpression(baseElement);
            if (!t_fieldInjectionExpressions.isEmpty()) {
                for (String t_fieldInjectionExpression : t_fieldInjectionExpressions)
                    findVariablesInExpression(javaReaderStatic,
                            t_fieldInjectionExpression, element, ElementChapter.FieldInjections,
                            KnownElementFieldType.Expression, scopeId, predecessor);
            }

            final String t_resultVariable = baseElement.getAttributeValueNs(BpmnModelConstants.CAMUNDA_NS,
                    BpmnConstants.RESULT_VARIABLE);
            if (t_resultVariable != null && t_resultVariable.trim().length() > 0) {

                new ProcessVariableOperation(t_resultVariable, VariableOperation.WRITE, scopeId);
            }

            if (baseElement.getAttributeValueNs(BpmnModelConstants.CAMUNDA_NS, BpmnConstants.ATTR_CLASS) != null) {
                javaReaderStatic
                        .getVariablesFromJavaDelegate(baseElement.getAttributeValueNs(BpmnModelConstants.CAMUNDA_NS,
                                BpmnConstants.ATTR_CLASS),
                                element, ElementChapter.Implementation, KnownElementFieldType.Class,
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
                findVariablesInExpression(javaReaderStatic,
                        assignee, element, ElementChapter.Details, KnownElementFieldType.Assignee, scopeId,
                        predecessor);
            final String candidateUsers = userTask.getCamundaCandidateUsers();
            if (candidateUsers != null)
                findVariablesInExpression(javaReaderStatic, candidateUsers,
                        element, ElementChapter.Details, KnownElementFieldType.CandidateUsers, scopeId,
                        predecessor);
            final String candidateGroups = userTask.getCamundaCandidateGroups();
            if (candidateGroups != null)
                findVariablesInExpression(javaReaderStatic, candidateGroups,
                        element, ElementChapter.Details, KnownElementFieldType.CandidateGroups, scopeId,
                        predecessor);
            final String dueDate = userTask.getCamundaDueDate();
            if (dueDate != null)
                findVariablesInExpression(javaReaderStatic,
                        dueDate, element, ElementChapter.Details, KnownElementFieldType.DueDate, scopeId,
                        predecessor);
            final String followUpDate = userTask.getCamundaFollowUpDate();
            if (followUpDate != null)
                findVariablesInExpression(javaReaderStatic,
                        followUpDate, element, ElementChapter.Details, KnownElementFieldType.FollowUpDate, scopeId,
                        predecessor);

        } else if (baseElement instanceof ScriptTask) {
            // Examine script task for process variables
            final ScriptTask scriptTask = (ScriptTask) baseElement;
            if (scriptTask.getScriptFormat() != null && scriptTask.getScriptFormat().equals(ConfigConstants.GROOVY)) {
                // inline script or external file?
                final Script script = scriptTask.getScript();
                if (script != null && script.getTextContent() != null && script.getTextContent().trim().length() > 0) {
                    ResourceFileReader.searchProcessVariablesInCode(element, ElementChapter.Details,
                            KnownElementFieldType.InlineScript, null, scopeId, script.getTextContent(), predecessor);
                } else {
                    final String resourcePath = scriptTask.getCamundaResource();
                    if (resourcePath != null) {
                        getVariablesFromGroovyScript(resourcePath, element,
                                ElementChapter.Details, scopeId, predecessor);
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
                findVariablesInExpression(javaReaderStatic,
                        calledElement, element, ElementChapter.Details, KnownElementFieldType.CalledElement, scopeId,
                        predecessor);
            }
            final String caseRef = callActivity.getAttributeValueNs(BpmnModelConstants.CAMUNDA_NS,
                    BpmnConstants.CASE_REF);
            if (caseRef != null && caseRef.trim().length() > 0) {
                findVariablesInExpression(javaReaderStatic,
                        caseRef, element, ElementChapter.Details, KnownElementFieldType.CaseRef, scopeId,
                        predecessor);
            }

            // Check DelegateVariableMapping
            if (baseElement.getAttributeValueNs(BpmnModelConstants.CAMUNDA_NS,
                    BpmnConstants.ATTR_VAR_MAPPING_CLASS) != null) {
                javaReaderStatic
                        .getVariablesFromJavaDelegate(baseElement.getAttributeValueNs(BpmnModelConstants.CAMUNDA_NS,
                                BpmnConstants.ATTR_VAR_MAPPING_CLASS),
                                element, null, KnownElementFieldType.Class, predecessor);
            } else if (baseElement.getAttributeValueNs(BpmnModelConstants.CAMUNDA_NS,
                    BpmnConstants.ATTR_VAR_MAPPING_DELEGATE) != null) {
                findVariablesInExpression(javaReaderStatic,
                        callActivity.getCamundaVariableMappingDelegateExpression(), element, null,
                        KnownElementFieldType.Class, scopeId, predecessor);

            }
        }

    }

    /**
     * Examine multi instance tasks for process variables
     *
     * @param javaReaderStatic Static java reader
     * @param element          BpmnElement
     * @param predecessor  Predecessor
     */
    private void searchVariablesInMultiInstanceTask(
            final JavaReaderStatic javaReaderStatic, final BpmnElement element,
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
            // TODO I dont know if the known element field type does really fit
            BasicNode node = new BasicNode(element,
                    ElementChapter.MultiInstance, KnownElementFieldType.CollectionElement);

            // Node is already added since there are at least the default variables
            predecessor[0] = addNodeAndGetNewPredecessor(node, element.getControlFlowGraph(), predecessor[0]);

            // Add default variables
            addDefaultMultiInstanceTaskVariables(node, scopeId);

            final String collectionName = loopCharacteristics.getAttributeValueNs(BpmnModelConstants.CAMUNDA_NS,
                    BpmnConstants.COLLECTION);
            if (collectionName != null && collectionName.trim().length() > 0) {

                // Check if collection name includes expression
                final Pattern pattern = Pattern.compile("\\$\\{.*}");
                Matcher matcher = pattern.matcher(collectionName);
                if (matcher.matches()) {
                    findVariablesInExpression(javaReaderStatic,
                            collectionName, element, ElementChapter.MultiInstance,
                            KnownElementFieldType.CollectionElement,
                            scopeId, predecessor);
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
                        findVariablesInExpression(javaReaderStatic,
                                cardinality, element, ElementChapter.MultiInstance,
                                KnownElementFieldType.LoopCardinality,
                                scopeId, predecessor);
                    }
                }
            }
            final ModelElementInstance completionCondition = loopCharacteristics
                    .getUniqueChildElementByType(CompletionCondition.class);
            if (completionCondition != null) {
                final String completionConditionExpression = completionCondition.getTextContent();
                if (completionConditionExpression != null && completionConditionExpression.trim().length() > 0) {
                    parseJuelExpression(element, ElementChapter.MultiInstance,
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
                element.getControlFlowGraph().addNode(node);
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
     * @param groovyFile Groovy File
     * @param element    BpmnElement
     * @param chapter    ElementChapter
     * @param scopeId    ScopeId
     * @param predecessor  Predecessor
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

        BasicNode node = new BasicNode(element, ElementChapter.Details, KnownElementFieldType.DMN);

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

    /**
     * Examine JUEL expressions for variables
     *
     * @param javaReaderStatic Static java reader
     * @param expression       Expression
     * @param element          BpmnElement
     * @param chapter          ElementChapter
     * @param fieldType        KnownElementFieldType
     * @param scopeId          ScopeId
     * @param predecessor  Predecessor
     */
    private void findVariablesInExpression(
            final JavaReaderStatic javaReaderStatic,
            final String expression, final BpmnElement element,
            final ElementChapter chapter, final KnownElementFieldType fieldType, final String scopeId,
            BasicNode[] predecessor) {
        final ControlFlowGraph controlFlowGraph = element.getControlFlowGraph();

        // HOTFIX: Catch pattern like below to avoid crash of TreeBuilder
        // ${dateTime().plusWeeks(1).toDate()}
        final Pattern pattern = Pattern.compile("\\$\\{(\\w)*\\(.*\\)}");
        // final Pattern pattern = Pattern.compile("\\$\\{(\\w*)\\.(\\w*\\(.*\\))*\\}|\\$\\{(\\w*\\(.*\\))*\\}");

        Matcher matcher = pattern.matcher(expression);

        if (matcher.matches()) {
            return;
        }

        boolean isDelegated = false;
        ExpressionNode expNode = new ExpressionNode(element, expression, chapter, fieldType);

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
                    javaReaderStatic.getVariablesFromJavaDelegate(className, element,
                            chapter, fieldType, predecessor);
                    isDelegated = true;
                } else {
                    // save variable
                    expNode.addOperation(new ProcessVariableOperation(node.getName(), VariableOperation.READ, scopeId));
                }
            }
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
        } catch (final ELException e) {
            throw new ProcessingException("EL expression " + expression + " in " + element.getProcessDefinition()
                    + ", element ID: " + element.getBaseElement().getId() + ", Type: " + fieldType.getDescription()
                    + " couldn't be parsed", e);
        }

        // TODO what happens now if expnode contains elements and is skipped?
        // TODO are there other field Types that should be skipped?
        if (!fieldType.equals(KnownElementFieldType.CalledElement)
                && !fieldType.equals(KnownElementFieldType.CamundaOut)
                && !fieldType.equals(KnownElementFieldType.CamundaIn) && !isDelegated
                && expNode.getOperations().size() > 0) {
            // TODO do we add the node if it is a bean and there are no operations?
            predecessor[0] = addNodeAndGetNewPredecessor(expNode, controlFlowGraph, predecessor[0]);
        }
    }

    /**
     * @param javaReaderStatic Static java reader
     * @param expression       Expression
     * @param element          BpmnElement
     * @param chapter          ElementChapter
     * @param fieldType        KnownElementFieldType
     * @param scopeId          ScopeId
     * @param predecessor Predecessor
     */
    private void checkMessageAndSignalForExpression(
            final JavaReaderStatic javaReaderStatic, final String expression, final BpmnElement element,
            final ElementChapter chapter, final KnownElementFieldType fieldType,
            final String scopeId, BasicNode[] predecessor) {
        try {

            final Pattern pattern = Pattern.compile(".*(\\$\\{.*?})");
            final Matcher matcher = pattern.matcher(expression);

            // if value is in the form of ${expression}, extract expression and find variables
            if (matcher.matches()) {
                findVariablesInExpression(javaReaderStatic, matcher.group(1), element, chapter,
                        fieldType, scopeId, predecessor);
            }
        } catch (final ELException e) {
            throw new ProcessingException("EL expression " + expression + " in " + element.getProcessDefinition()
                    + ", element ID: " + element.getBaseElement().getId() + ", Type: "
                    + KnownElementFieldType.Expression + " couldn't be parsed", e);
        }
    }

    // TODO add test
    private void parseJuelExpression(final BpmnElement element, final ElementChapter elementChapter,
            final KnownElementFieldType fieldType,
            String expression, final String scopeId, BasicNode[] predecessor) {
        ExpressionNode expNode = new ExpressionNode(element, expression, elementChapter, fieldType);

        TreeStore store = new TreeStore(new Builder(Builder.Feature.METHOD_INVOCATIONS), null);
        Tree tree = store.get(expression);

        // Only support simple expressions at the moment (only one method call or only simple reads)
        if (tree.getRoot().getChild(0) instanceof AstMethod) {
            AstMethod method = (AstMethod) tree.getRoot().getChild(0);
            AstProperty property = (AstDot) method.getChild(0);
            AstParameters parameters = (AstParameters) method.getChild(1);
            if (((AstIdentifier) property.getChild(0)).getName().equals("execution")) {
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
                }
            }
        } else {
            for (Node n : tree.getIdentifierNodes()) {
                // Read operation
                String varName = ((AstIdentifier) n).getName();
                expNode.addOperation(new ProcessVariableOperation(varName,
                        VariableOperation.READ, scopeId));
            }
        }

        if (expNode.getOperations().size() > 0) {
            predecessor[0] = addNodeAndGetNewPredecessor(expNode, element.getControlFlowGraph(), predecessor[0]);
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

    private BasicNode addNodeAndGetNewPredecessor(BasicNode node, ControlFlowGraph cg,
            BasicNode predecessor) {
        cg.addNode(node);
        if (predecessor != null) {
            node.addPredecessor(predecessor);
        }

        return node;
    }
}
