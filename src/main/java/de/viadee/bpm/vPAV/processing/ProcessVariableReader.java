/**
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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import de.viadee.bpm.vPAV.BpmnScanner;
import de.viadee.bpm.vPAV.FileScanner;
import de.viadee.bpm.vPAV.Messages;
import de.viadee.bpm.vPAV.RuntimeConfig;
import de.viadee.bpm.vPAV.config.model.Rule;
import de.viadee.bpm.vPAV.constants.BpmnConstants;
import de.viadee.bpm.vPAV.constants.ConfigConstants;
import de.viadee.bpm.vPAV.output.IssueWriter;
import de.viadee.bpm.vPAV.processing.code.flow.*;
import de.viadee.bpm.vPAV.processing.model.data.*;
import org.apache.commons.collections4.map.LinkedMap;
import org.camunda.bpm.engine.impl.juel.Builder;
import org.camunda.bpm.engine.impl.juel.IdentifierNode;
import org.camunda.bpm.engine.impl.juel.Tree;
import org.camunda.bpm.engine.impl.juel.TreeBuilder;
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

    private final BpmnScanner bpmnScanner;

    private final Rule rule;

    public ProcessVariableReader(final Map<String, String> decisionRefToPathMap, final Rule rule, BpmnScanner scanner) {
        this.decisionRefToPathMap = decisionRefToPathMap;
        this.rule = rule;
        this.bpmnScanner = scanner;
    }

    /**
     * Examining an bpmn element for variables
     *
     * @param fileScanner      FileScanner
     * @param element          BpmnElement
     * @return returns processVariables
     */
    public ListMultimap<String, ProcessVariableOperation> getVariablesFromElement(final FileScanner fileScanner,
            final BpmnElement element,
            final LinkedHashMap<String, AnalysisElement> predecessors) {
        final JavaReaderStatic javaReaderStatic = new JavaReaderStatic();
        final ListMultimap<String, ProcessVariableOperation> processVariables = ArrayListMultimap.create();
        final BaseElement baseElement = element.getBaseElement();
        final BpmnModelElementInstance scopeElement = baseElement.getScope();
        String scopeElementId = null;
        if (scopeElement != null) {
            scopeElementId = scopeElement.getAttributeValue(BpmnConstants.ATTR_ID);
        }
        final ExtensionElements extensionElements = baseElement.getExtensionElements();

        // 1) Search for variables in multi instance task, if applicable
        processVariables
                .putAll(searchVariablesInMultiInstanceTask(javaReaderStatic, fileScanner, element, predecessors));

        // 2) Search variables in Input Parameters
        processVariables.putAll(getVariablesFromInputMapping(javaReaderStatic, element, fileScanner, predecessors));

        // 3) Search variables execution listener (start)
        if (extensionElements != null) {
            processVariables.putAll(getVariablesFromExecutionListener(javaReaderStatic, fileScanner, element,
                    extensionElements, scopeElementId, ElementChapter.ExecutionListenerStart, predecessors));
        }

        // 4) Search variables in task
        processVariables.putAll(getVariablesFromTask(javaReaderStatic, fileScanner, element, predecessors));

        // 5) Search variables in sequence flow
        processVariables
                .putAll(searchVariablesFromSequenceFlow(javaReaderStatic, fileScanner, element, predecessors));

        // 6) Search variables in ExtensionElements
        processVariables.putAll(searchExtensionsElements(javaReaderStatic, fileScanner, element, predecessors));

        // 7) Search variables in Signals and Messages
        processVariables
                .putAll(getVariablesFromSignalsAndMessage(javaReaderStatic, element, fileScanner, predecessors));

        // 8) Search variables in Links
        processVariables.putAll(getVariablesFromLinks(javaReaderStatic, element, fileScanner, predecessors));

        // 9) Search variables execution listener (end)
        if (extensionElements != null) {
            processVariables.putAll(getVariablesFromExecutionListener(javaReaderStatic, fileScanner, element,
                    extensionElements, scopeElementId, ElementChapter.ExecutionListenerEnd, predecessors));
        }

        // 10) Search variables in Output Parameters
        processVariables
                .putAll(getVariablesFromOutputMapping(javaReaderStatic, element, fileScanner, predecessors));

        return processVariables;
    }

    /**
     * Retrieve process variables from names
     *
     * @param javaReaderStatic Static java reader
     * @param element          BpmnElement
     * @param fileScanner      FileScanner
     * @return ProcessVariables retrieved from signals and messages
     */
    private ListMultimap<String, ProcessVariableOperation> getVariablesFromSignalsAndMessage(
            final JavaReaderStatic javaReaderStatic, final BpmnElement element, final FileScanner fileScanner,
            final LinkedHashMap<String, AnalysisElement> predecessors) {
        final ListMultimap<String, ProcessVariableOperation> processVariables = ArrayListMultimap.create();

        final ArrayList<String> signalRefs = bpmnScanner.getSignalRefs(element.getBaseElement().getId());
        final ArrayList<String> messagesRefs = bpmnScanner.getMessageRefs(element.getBaseElement().getId());

        processVariables
                .putAll(getSignalVariables(javaReaderStatic, signalRefs, element, fileScanner, predecessors));
        processVariables
                .putAll(getMessageVariables(javaReaderStatic, messagesRefs, element, fileScanner, predecessors));

        return processVariables;
    }

    /**
     * Retrieves variables from signal
     *
     * @param javaReaderStatic Static java reader
     * @param signalRefs       List of signal references
     * @param element          BpmnElement
     * @param fileScanner      FileScanner
     * @return ProcessVariables retrieved from signals
     */
    private ListMultimap<String, ProcessVariableOperation> getSignalVariables(final JavaReaderStatic javaReaderStatic,
            final ArrayList<String> signalRefs,
            final BpmnElement element,
            final FileScanner fileScanner, final LinkedHashMap<String, AnalysisElement> predecessors) {

        final ListMultimap<String, ProcessVariableOperation> processVariables = ArrayListMultimap.create();
        final BaseElement baseElement = element.getBaseElement();
        final BpmnModelElementInstance scopeElement = baseElement.getScope();

        String scopeElementId = null;
        if (scopeElement != null) {
            scopeElementId = scopeElement.getAttributeValue(BpmnConstants.ATTR_ID);
        }

        final ArrayList<String> names = new ArrayList<>();

        for (String signalID : signalRefs) {
            names.add(bpmnScanner.getSignalName(signalID));
        }

        for (String signalName : names) {
            processVariables
                    .putAll(checkMessageAndSignalForExpression(javaReaderStatic, signalName, element, fileScanner,
                            ElementChapter.Signal, KnownElementFieldType.Signal, scopeElementId, predecessors));
        }

        return processVariables;
    }

    /**
     * Retrieves variables from message
     *
     * @param javaReaderStatic Static java reader
     * @param messageRefs      List of message references
     * @param element          BpmnElement
     * @param fileScanner      FileScanner
     * @return ProcessVariables retrieved from messages
     */
    private ListMultimap<String, ProcessVariableOperation> getMessageVariables(final JavaReaderStatic javaReaderStatic,
            final ArrayList<String> messageRefs, final BpmnElement element,
            final FileScanner fileScanner, final LinkedHashMap<String, AnalysisElement> predecessors) {

        final ListMultimap<String, ProcessVariableOperation> processVariables = ArrayListMultimap.create();
        final BaseElement baseElement = element.getBaseElement();
        final BpmnModelElementInstance scopeElement = baseElement.getScope();

        String scopeElementId = null;
        if (scopeElement != null) {
            scopeElementId = scopeElement.getAttributeValue(BpmnConstants.ATTR_ID);
        }

        final ArrayList<String> names = new ArrayList<>();

        for (String messageID : messageRefs) {
            names.add(bpmnScanner.getMessageName(messageID));
        }

        for (String messageName : names) {
            processVariables
                    .putAll(checkMessageAndSignalForExpression(javaReaderStatic, messageName, element, fileScanner,
                            ElementChapter.Message, KnownElementFieldType.Message, scopeElementId, predecessors));
        }

        return processVariables;
    }

    /**
     * @param javaReaderStatic Static java reader
     * @param element          Current BPMN Element
     * @param fileScanner      FileScanner
     * @return ProcessVariables retrieved from events of type link
     */
    private ListMultimap<String, ProcessVariableOperation> getVariablesFromLinks(
            final JavaReaderStatic javaReaderStatic, final BpmnElement element, final FileScanner fileScanner,
            final LinkedHashMap<String, AnalysisElement> predecessors) {
        final ListMultimap<String, ProcessVariableOperation> processVariables = ArrayListMultimap.create();

        final ArrayList<String> links = bpmnScanner.getLinkRefs(element.getBaseElement().getId());

        processVariables.putAll(getLinkVariables(javaReaderStatic, links, element, fileScanner, predecessors));

        return processVariables;
    }

    /**
     * @param javaReaderStatic Static java reader
     * @param links            List of links for current element
     * @param element          Current BPMN Element
     * @param fileScanner      FileScanner
     * @return ProcessVariables retrieved from events of type link
     */
    private ListMultimap<String, ProcessVariableOperation> getLinkVariables(final JavaReaderStatic javaReaderStatic,
            final ArrayList<String> links, final BpmnElement element,
            final FileScanner fileScanner, final LinkedHashMap<String, AnalysisElement> predecessors) {
        final ListMultimap<String, ProcessVariableOperation> processVariables = ArrayListMultimap.create();
        final BaseElement baseElement = element.getBaseElement();
        final BpmnModelElementInstance scopeElement = baseElement.getScope();

        String scopeElementId = null;
        if (scopeElement != null) {
            scopeElementId = scopeElement.getAttributeValue(BpmnConstants.ATTR_ID);
        }

        for (String link : links) {
            processVariables.putAll(checkMessageAndSignalForExpression(javaReaderStatic, link, element, fileScanner,
                    ElementChapter.Signal, KnownElementFieldType.Signal, scopeElementId, predecessors));
        }

        return processVariables;
    }

    /**
     * Analyze Input Parameters for variables
     *
     * @param javaReaderStatic Static java reader
     * @param element          Current BPMN Element
     * @param fileScanner      FileScanner
     * @return Map of ProcessVariable
     */
    private ListMultimap<String, ProcessVariableOperation> getVariablesFromInputMapping(
            final JavaReaderStatic javaReaderStatic, final BpmnElement element, final FileScanner fileScanner,
            final LinkedHashMap<String, AnalysisElement> predecessors) {
        final ListMultimap<String, ProcessVariableOperation> inputMappingProcessVariables = ArrayListMultimap.create();
        final BaseElement baseElement = element.getBaseElement();

        final Map<String, Map<String, String>> inputVariables = bpmnScanner
                .getInputMapping(element.getBaseElement().getId());

        final LinkedMap<String, String> inputMappingType = bpmnScanner.getMappingType(element.getBaseElement().getId(),
                BpmnConstants.CAMUNDA_INPUT_PARAMETER);

        for (Map.Entry<String, Map<String, String>> entry : inputVariables.entrySet()) {
            for (Map.Entry<String, String> innerEntry : entry.getValue().entrySet()) {
                if (innerEntry.getValue().isEmpty()) {
                    IssueWriter.createSingleIssue(this.rule, CriticalityEnum.WARNING, element,
                            element.getProcessDefinition(),
                            Messages.getString("ProcessVariableReader.1")); //$NON-NLS-1$ );
                } else {
                    if (!inputMappingType.firstKey().equals(BpmnConstants.CAMUNDA_SCRIPT)) {
                        inputMappingProcessVariables
                                .putAll(checkExpressionForReadVariable(javaReaderStatic, innerEntry.getValue(),
                                        innerEntry.getKey(), element, fileScanner, ElementChapter.InputOutput,
                                        KnownElementFieldType.InputParameter, baseElement.getId(), predecessors));
                    } else {
                        IssueWriter.createSingleIssue(this.rule, CriticalityEnum.ERROR, element,
                                element.getProcessDefinition(),
                                Messages.getString("ProcessVariableReader.2")); //$NON-NLS-1$ );
                    }
                }
            }
        }
        return inputMappingProcessVariables;
    }

    /**
     * Analyze Input Parameters for variables
     *
     * @param javaReaderStatic Static java reader
     * @param element          Current BPMN Element
     * @param fileScanner      FileScanner
     * @return Map of ProcessVariable
     */
    private ListMultimap<String, ProcessVariableOperation> getVariablesFromOutputMapping(
            final JavaReaderStatic javaReaderStatic, final BpmnElement element, final FileScanner fileScanner,
            final LinkedHashMap<String, AnalysisElement> predecessors) {
        final ListMultimap<String, ProcessVariableOperation> outputMappingProcessVariables = ArrayListMultimap.create();
        final BaseElement baseElement = element.getBaseElement();
        final BpmnModelElementInstance scopeElement = baseElement.getScope();

        String scopeElementId = null;
        if (scopeElement != null) {
            scopeElementId = scopeElement.getAttributeValue(BpmnConstants.ATTR_ID);
        }

        final Map<String, Map<String, String>> outputVariables = bpmnScanner
                .getOutputMapping(element.getBaseElement().getId());

        final LinkedMap<String, String> outputMappingType = bpmnScanner.getMappingType(element.getBaseElement().getId(),
                BpmnConstants.CAMUNDA_OUTPUT_PARAMETER);

        for (Map.Entry<String, Map<String, String>> entry : outputVariables.entrySet()) {
            for (Map.Entry<String, String> innerEntry : entry.getValue().entrySet()) {
                if (innerEntry.getValue().isEmpty()) {
                    IssueWriter.createSingleIssue(this.rule, CriticalityEnum.WARNING, element,
                            element.getProcessDefinition(),
                            Messages.getString("ProcessVariableReader.1")); //$NON-NLS-1$ );
                } else {
                    if (!outputMappingType.firstKey().equals(BpmnConstants.CAMUNDA_SCRIPT)) {
                        outputMappingProcessVariables
                                .putAll(checkExpressionForReadVariable(javaReaderStatic, innerEntry.getValue(),
                                        innerEntry.getKey(), element, fileScanner, ElementChapter.InputOutput,
                                        KnownElementFieldType.OutputParameter, scopeElementId, predecessors));
                    } else {
                        IssueWriter.createSingleIssue(this.rule, CriticalityEnum.ERROR, element,
                                element.getProcessDefinition(),
                                Messages.getString("ProcessVariableReader.2")); //$NON-NLS-1$ );
                    }
                }
            }
        }
        return outputMappingProcessVariables;
    }

    /**
     * Analyze bpmn extension elements for variables
     *
     * @param javaReaderStatic Static java reader
     * @param fileScanner      FileScanner
     * @param element          BpmnElement
     * @return variables
     */
    private ListMultimap<String, ProcessVariableOperation> searchExtensionsElements(
            final JavaReaderStatic javaReaderStatic, final FileScanner fileScanner, final BpmnElement element,
            final LinkedHashMap<String, AnalysisElement> predecessors) {

        final ListMultimap<String, ProcessVariableOperation> processVariables = ArrayListMultimap.create();
        final BaseElement baseElement = element.getBaseElement();
        final BpmnModelElementInstance scopeElement = baseElement.getScope();
        String scopeElementId = null;
        if (scopeElement != null) {
            scopeElementId = scopeElement.getAttributeValue(BpmnConstants.ATTR_ID);
        }
        final ExtensionElements extensionElements = baseElement.getExtensionElements();
        if (extensionElements != null) {
            // 1) Search in Task Listeners
            processVariables.putAll(getVariablesFromTaskListener(javaReaderStatic, fileScanner, element,
                    extensionElements, scopeElementId, predecessors));

            // 2) Search in Form Data
            processVariables.putAll(getVariablesFromFormData(element, extensionElements, scopeElementId));

            // 3) Search in Input/Output-Associations (Call Activities)
            processVariables.putAll(searchVariablesInInputOutputExtensions(javaReaderStatic, fileScanner, element,
                    extensionElements, scopeElementId, predecessors));
        }

        return processVariables;
    }

    /**
     * Get process variables from execution listeners
     *
     * @param javaReaderStatic  Static java reader
     * @param fileScanner       FileScanner
     * @param element           Current BPMN Element
     * @param extensionElements Extension elements (e.g. Listeners)
     * @param scopeId           Scope ID
     * @return variables
     */
    private ListMultimap<String, ProcessVariableOperation> getVariablesFromExecutionListener(
            final JavaReaderStatic javaReaderStatic, final FileScanner fileScanner, final BpmnElement element,
            final ExtensionElements extensionElements, final String scopeId, final ElementChapter listenerChapter,
            final LinkedHashMap<String, AnalysisElement> predecessors) {

        final ListMultimap<String, ProcessVariableOperation> processVariables = ArrayListMultimap.create();
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
                    processVariables.putAll(findVariablesInExpression(javaReaderStatic, fileScanner,
                            l_expression, element, listenerChapter,
                            KnownElementFieldType.Expression, scopeId, predecessors));
                }
                final String l_delegateExpression = listener.getCamundaDelegateExpression();
                if (l_delegateExpression != null) {
                    processVariables.putAll(findVariablesInExpression(javaReaderStatic, fileScanner,
                            l_delegateExpression, element, listenerChapter,
                            KnownElementFieldType.DelegateExpression, scopeId, predecessors));
                }
                final String l_class = listener.getCamundaClass();
                if (l_class != null) {
                    processVariables.putAll(javaReaderStatic.getVariablesFromJavaDelegate(fileScanner,
                            listener.getCamundaClass(), element, listenerChapter,
                            KnownElementFieldType.Class, scopeId, predecessors));
                }
                final CamundaScript script = listener.getCamundaScript();
                if (script != null && script.getCamundaScriptFormat() != null
                        && script.getCamundaScriptFormat().equals(ConfigConstants.GROOVY)) {
                    // inline script or external file?
                    final String inlineScript = script.getTextContent();
                    if (inlineScript != null && inlineScript.trim().length() > 0) {
                        processVariables.putAll(ResourceFileReader.searchProcessVariablesInCode(element,
                                listenerChapter, KnownElementFieldType.InlineScript, null,
                                scopeId, inlineScript));
                    } else {
                        final String resourcePath = script.getCamundaResource();
                        if (resourcePath != null) {
                            processVariables.putAll(getVariablesFromGroovyScript(resourcePath, element,
                                    listenerChapter, KnownElementFieldType.ExternalScript,
                                    scopeId));
                        }
                    }
                }
            }
        }
        return processVariables;
    }

    /**
     * Get process variables from task listeners
     *
     * @param javaReaderStatic  Static java reader
     * @param fileScanner       FileScanner
     * @param element           BpmnElement
     * @param extensionElements ExtensionElements
     * @param scopeId           ScopeId
     * @return variables
     */
    private ListMultimap<String, ProcessVariableOperation> getVariablesFromTaskListener(
            final JavaReaderStatic javaReaderStatic, final FileScanner fileScanner, final BpmnElement element,
            final ExtensionElements extensionElements, final String scopeId,
            final LinkedHashMap<String, AnalysisElement> predecessors) {

        final ListMultimap<String, ProcessVariableOperation> processVariables = ArrayListMultimap.create();
        List<CamundaTaskListener> listenerList = extensionElements.getElementsQuery()
                .filterByType(CamundaTaskListener.class).list();
        for (final CamundaTaskListener listener : listenerList) {
            final String l_expression = listener.getCamundaExpression();
            if (l_expression != null) {
                processVariables.putAll(findVariablesInExpression(javaReaderStatic, fileScanner,
                        l_expression, element, ElementChapter.TaskListener, KnownElementFieldType.Expression, scopeId,
                        predecessors));
            }
            final String l_delegateExpression = listener.getCamundaDelegateExpression();
            if (l_delegateExpression != null) {
                processVariables.putAll(findVariablesInExpression(javaReaderStatic, fileScanner,
                        l_delegateExpression, element, ElementChapter.TaskListener,
                        KnownElementFieldType.DelegateExpression, scopeId, predecessors));
            }

            String filePath = "";
            if (listener.getCamundaClass() != null && listener.getCamundaClass().trim().length() > 0) {
                filePath = listener.getCamundaClass().replaceAll("\\.", "/") + ".java";
            }

            processVariables.putAll(ResourceFileReader.readResourceFile(filePath, element, ElementChapter.TaskListener,
                    KnownElementFieldType.Class, scopeId));

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
     * @param element           BpmnElement
     * @param extensionElements ExtensionElements
     * @param scopeElementId    ScopeElementId
     * @return variables
     */
    private ListMultimap<String, ProcessVariableOperation> getVariablesFromFormData(final BpmnElement element,
            final ExtensionElements extensionElements, final String scopeElementId) {

        final ListMultimap<String, ProcessVariableOperation> processVariables = ArrayListMultimap.create();

        final Query<CamundaFormData> formDataQuery = extensionElements.getElementsQuery()
                .filterByType(CamundaFormData.class);
        if (formDataQuery.count() > 0) {
            final CamundaFormData formData = formDataQuery.singleResult();
            if (formData != null) {
                final Collection<CamundaFormField> formFields = formData.getCamundaFormFields();
                for (final CamundaFormField field : formFields) {
                    processVariables.put(field.getCamundaId(),
                            new ProcessVariableOperation(field.getCamundaId(), element, ElementChapter.FormData,
                                    KnownElementFieldType.FormField, null, VariableOperation.WRITE, scopeElementId,
                                    element.getFlowAnalysis().getOperationCounter()));
                }
            }
        }

        return processVariables;
    }

    /**
     * Get process variables from camunda input/output associations (call
     * activities)
     *
     * @param element           BpmnElement
     * @param extensionElements ExtensionElements
     * @param scopeId           ScopeId
     * @return variables
     */
    private ListMultimap<String, ProcessVariableOperation> searchVariablesInInputOutputExtensions(
            final JavaReaderStatic javaReaderStatic, final FileScanner fileScanner, final BpmnElement element,
            final ExtensionElements extensionElements, final String scopeId,
            final LinkedHashMap<String, AnalysisElement> predecessors) {

        final ListMultimap<String, ProcessVariableOperation> processVariables = ArrayListMultimap.create();

        final BaseElement baseElement = element.getBaseElement();

        if (baseElement instanceof CallActivity) {
            final List<CamundaIn> inputAssociations = extensionElements.getElementsQuery().filterByType(CamundaIn.class)
                    .list();
            for (final CamundaIn inputAssociation : inputAssociations) {
                String source = inputAssociation.getCamundaSource();
                if (source == null || source.isEmpty()) {
                    source = inputAssociation.getCamundaSourceExpression();
                    if (source != null && !source.isEmpty()) {
                        processVariables.putAll(findVariablesInExpression(javaReaderStatic,
                                fileScanner, source, element, ElementChapter.InputData,
                                KnownElementFieldType.CamundaIn, scopeId, predecessors));
                    } else {
                        continue;
                    }

                } else {
                    processVariables.put(source,
                            new ProcessVariableOperation(source, element, ElementChapter.InputData,
                                    KnownElementFieldType.CamundaIn, null, VariableOperation.READ, scopeId,
                                    element.getFlowAnalysis().getOperationCounter()));
                }

                // Add target operation
                String target = inputAssociation.getCamundaTarget();
                processVariables.put(target,
                        new ProcessVariableOperation(target, element, ElementChapter.InputData,
                                KnownElementFieldType.CamundaIn, null, VariableOperation.WRITE,
                                ((CallActivity) baseElement).getCalledElement(),
                                element.getFlowAnalysis().getOperationCounter()));

            }
            final List<CamundaOut> outputAssociations = extensionElements.getElementsQuery()
                    .filterByType(CamundaOut.class).list();
            for (final CamundaOut outputAssociation : outputAssociations) {
                String source = outputAssociation.getCamundaSource();
                if (source == null || source.isEmpty()) {
                    source = outputAssociation.getCamundaSourceExpression();
                    processVariables.putAll(findVariablesInExpression(javaReaderStatic,
                            fileScanner, source, element, ElementChapter.OutputData, KnownElementFieldType.CamundaOut,
                            ((CallActivity) baseElement).getCalledElement(), predecessors));
                } else {
                    processVariables.put(source,
                            new ProcessVariableOperation(source, element, ElementChapter.OutputData,
                                    KnownElementFieldType.CamundaOut, null, VariableOperation.READ,
                                    ((CallActivity) baseElement).getCalledElement(),
                                    element.getFlowAnalysis().getOperationCounter()));
                }

                final String target = outputAssociation.getCamundaTarget();
                if (target != null && !target.isEmpty()) {
                    processVariables.put(target,
                            new ProcessVariableOperation(target, element, ElementChapter.OutputData,
                                    KnownElementFieldType.CamundaOut, null, VariableOperation.WRITE, scopeId,
                                    element.getFlowAnalysis().getOperationCounter()));
                }
            }
        }

        return processVariables;
    }

    /**
     * Get process variables from sequence flow conditions
     *
     * @param javaReaderStatic Static java reader
     * @param fileScanner      FileScanner
     * @param element          BpmnElement
     * @return variables
     */
    private ListMultimap<String, ProcessVariableOperation> searchVariablesFromSequenceFlow(
            final JavaReaderStatic javaReaderStatic, final FileScanner fileScanner, final BpmnElement element,
            final LinkedHashMap<String, AnalysisElement> predecessors) {

        final ListMultimap<String, ProcessVariableOperation> variables = ArrayListMultimap.create();
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
                        variables.putAll(findVariablesInExpression(javaReaderStatic, fileScanner,
                                expression.getTextContent(), element, ElementChapter.Details,
                                KnownElementFieldType.Expression, scopeId, predecessors));
                    }
                }
            }
        }
        return variables;
    }

    /**
     * Analyse all types of tasks for process variables
     *
     * @param javaReaderStatic Static java reader
     * @param fileScanner      FileScanner
     * @param element          BpmnElement
     * @return variables
     */
    private ListMultimap<String, ProcessVariableOperation> getVariablesFromTask(
            final JavaReaderStatic javaReaderStatic,
            final FileScanner fileScanner, final BpmnElement element,
            final LinkedHashMap<String, AnalysisElement> predecessors) {

        final ListMultimap<String, ProcessVariableOperation> processVariables = ArrayListMultimap.create();

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
                processVariables
                        .putAll(findVariablesInExpression(javaReaderStatic, fileScanner, t_expression,
                                element, ElementChapter.Implementation, KnownElementFieldType.Expression, scopeId,
                                predecessors));
            }

            final String t_delegateExpression = baseElement.getAttributeValueNs(BpmnModelConstants.CAMUNDA_NS,
                    BpmnConstants.ATTR_DEL);
            if (t_delegateExpression != null) {
                processVariables.putAll(findVariablesInExpression(javaReaderStatic, fileScanner,
                        t_delegateExpression, element, ElementChapter.Implementation,
                        KnownElementFieldType.DelegateExpression, scopeId, predecessors));
            }

            final ArrayList<String> t_fieldInjectionExpressions = bpmnScanner
                    .getFieldInjectionExpression(baseElement.getId());
            if (t_fieldInjectionExpressions != null && !t_fieldInjectionExpressions.isEmpty()) {
                for (String t_fieldInjectionExpression : t_fieldInjectionExpressions)
                    processVariables.putAll(findVariablesInExpression(javaReaderStatic, fileScanner,
                            t_fieldInjectionExpression, element, ElementChapter.FieldInjections,
                            KnownElementFieldType.Expression, scopeId, predecessors));
            }

            final String t_resultVariable = baseElement.getAttributeValueNs(BpmnModelConstants.CAMUNDA_NS,
                    BpmnConstants.RESULT_VARIABLE);
            if (t_resultVariable != null && t_resultVariable.trim().length() > 0) {
                processVariables.put(t_resultVariable,
                        new ProcessVariableOperation(t_resultVariable, element, ElementChapter.Details,
                                KnownElementFieldType.ResultVariable, null, VariableOperation.WRITE, scopeId,
                                element.getFlowAnalysis().getOperationCounter()));
            }

            if (baseElement.getAttributeValueNs(BpmnModelConstants.CAMUNDA_NS, BpmnConstants.ATTR_CLASS) != null) {
                processVariables
                        .putAll(javaReaderStatic.getVariablesFromJavaDelegate(fileScanner,
                                baseElement.getAttributeValueNs(BpmnModelConstants.CAMUNDA_NS,
                                        BpmnConstants.ATTR_CLASS),
                                element, ElementChapter.Implementation, KnownElementFieldType.Class, scopeId,
                                predecessors));
            }

            if (baseElement instanceof BusinessRuleTask) {
                final String t_decisionRef = baseElement.getAttributeValueNs(BpmnModelConstants.CAMUNDA_NS,
                        BpmnConstants.DECISION_REF);
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
                processVariables.putAll(findVariablesInExpression(javaReaderStatic, fileScanner,
                        assignee, element, ElementChapter.Details, KnownElementFieldType.Assignee, scopeId,
                        predecessors));
            final String candidateUsers = userTask.getCamundaCandidateUsers();
            if (candidateUsers != null)
                processVariables.putAll(
                        findVariablesInExpression(javaReaderStatic, fileScanner, candidateUsers,
                                element, ElementChapter.Details, KnownElementFieldType.CandidateUsers, scopeId,
                                predecessors));
            final String candidateGroups = userTask.getCamundaCandidateGroups();
            if (candidateGroups != null)
                processVariables.putAll(
                        findVariablesInExpression(javaReaderStatic, fileScanner, candidateGroups,
                                element, ElementChapter.Details, KnownElementFieldType.CandidateGroups, scopeId,
                                predecessors));
            final String dueDate = userTask.getCamundaDueDate();
            if (dueDate != null)
                processVariables.putAll(findVariablesInExpression(javaReaderStatic, fileScanner,
                        dueDate, element, ElementChapter.Details, KnownElementFieldType.DueDate, scopeId,
                        predecessors));
            final String followUpDate = userTask.getCamundaFollowUpDate();
            if (followUpDate != null)
                processVariables.putAll(findVariablesInExpression(javaReaderStatic, fileScanner,
                        followUpDate, element, ElementChapter.Details, KnownElementFieldType.FollowUpDate, scopeId,
                        predecessors));

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
                                KnownElementFieldType.ResultVariable, null, VariableOperation.WRITE, scopeId,
                                element.getFlowAnalysis().getOperationCounter()));
            }
        } else if (baseElement instanceof CallActivity) {
            final CallActivity callActivity = (CallActivity) baseElement;
            final String calledElement = callActivity.getCalledElement();
            if (calledElement != null && calledElement.trim().length() > 0) {
                processVariables.putAll(findVariablesInExpression(javaReaderStatic, fileScanner,
                        calledElement, element, ElementChapter.Details, KnownElementFieldType.CalledElement, scopeId,
                        predecessors));
            }
            final String caseRef = callActivity.getAttributeValueNs(BpmnModelConstants.CAMUNDA_NS,
                    BpmnConstants.CASE_REF);
            if (caseRef != null && caseRef.trim().length() > 0) {
                processVariables.putAll(findVariablesInExpression(javaReaderStatic, fileScanner,
                        caseRef, element, ElementChapter.Details, KnownElementFieldType.CaseRef, scopeId,
                        predecessors));
            }

            // Check DelegateVariableMapping
            if (baseElement.getAttributeValueNs(BpmnModelConstants.CAMUNDA_NS,
                    BpmnConstants.ATTR_VAR_MAPPING_CLASS) != null) {
                processVariables.putAll(javaReaderStatic.getVariablesFromJavaDelegate(fileScanner,
                        baseElement.getAttributeValueNs(BpmnModelConstants.CAMUNDA_NS,
                                BpmnConstants.ATTR_VAR_MAPPING_CLASS),
                        element, null, KnownElementFieldType.Class, scopeId, predecessors));
            } else if (baseElement.getAttributeValueNs(BpmnModelConstants.CAMUNDA_NS,
                    BpmnConstants.ATTR_VAR_MAPPING_DELEGATE) != null) {
                processVariables.putAll(findVariablesInExpression(javaReaderStatic, fileScanner,
                        callActivity.getCamundaVariableMappingDelegateExpression(), element, null,
                        KnownElementFieldType.Class, scopeId, predecessors));

            }
        }

        return processVariables;
    }

    /**
     * Examine multi instance tasks for process variables
     *
     * @param javaReaderStatic Static java reader
     * @param fileScanner      FileScanner
     * @param element          BpmnElement
     * @return variables
     */
    private ListMultimap<String, ProcessVariableOperation> searchVariablesInMultiInstanceTask(
            final JavaReaderStatic javaReaderStatic, final FileScanner fileScanner, final BpmnElement element,
            final LinkedHashMap<String, AnalysisElement> predecessors) {

        final ListMultimap<String, ProcessVariableOperation> processVariables = ArrayListMultimap.create();
        final ListMultimap<String, ProcessVariableOperation> processVariablesExpression = ArrayListMultimap.create();

        final BaseElement baseElement = element.getBaseElement();
        BpmnModelElementInstance scopeElement = baseElement.getScope();
        String scopeId = null;
        if (scopeElement != null) {
            scopeId = element.getId();
        }
        final ModelElementInstance loopCharacteristics = baseElement
                .getUniqueChildElementByType(LoopCharacteristics.class);
        if (loopCharacteristics != null) {
            ExpressionNode node = new ExpressionNode(element,
                    "", ElementChapter.MultiInstance);
            addNodeAndClearPredecessors(node, element.getControlFlowGraph(), predecessors);

            // Add default variables
            processVariables.putAll(addDefaultMultiInstanceTaskVariables(element));

            final String collectionName = loopCharacteristics.getAttributeValueNs(BpmnModelConstants.CAMUNDA_NS,
                    BpmnConstants.COLLECTION);
            if (collectionName != null && collectionName.trim().length() > 0) {

                // Check if collection name includes expression
                final Pattern pattern = Pattern.compile("\\$\\{.*\\}");
                Matcher matcher = pattern.matcher(collectionName);
                if (matcher.matches()) {
                    processVariablesExpression.putAll(findVariablesInExpression(javaReaderStatic, fileScanner,
                            collectionName, element, ElementChapter.MultiInstance,
                            KnownElementFieldType.CollectionElement,
                            scopeId, predecessors));
                } else {
                    processVariables.put(collectionName,
                            new ProcessVariableOperation(collectionName, element, ElementChapter.MultiInstance,
                                    KnownElementFieldType.CollectionElement, null, VariableOperation.READ, scopeId,
                                    element.getFlowAnalysis().getOperationCounter()));
                }
            }
            final String elementVariable = loopCharacteristics.getAttributeValueNs(BpmnModelConstants.CAMUNDA_NS,
                    BpmnConstants.ELEMENT_VARIABLE);
            if (elementVariable != null && elementVariable.trim().length() > 0) {
                processVariables.put(elementVariable,
                        new ProcessVariableOperation(elementVariable, element, ElementChapter.MultiInstance,
                                KnownElementFieldType.ElementVariable, null, VariableOperation.WRITE, scopeId,
                                element.getFlowAnalysis().getOperationCounter()));
            }
            final ModelElementInstance loopCardinality = loopCharacteristics
                    .getUniqueChildElementByType(LoopCardinality.class);
            if (loopCardinality != null) {
                final String cardinality = loopCardinality.getTextContent();

                if (cardinality != null && cardinality.trim().length() > 0) {
                    if (!cardinality.matches("\\d+")) {
                        processVariablesExpression.putAll(findVariablesInExpression(javaReaderStatic, fileScanner,
                                cardinality, element, ElementChapter.MultiInstance,
                                KnownElementFieldType.LoopCardinality,
                                scopeId, predecessors));
                    }
                }
            }
            final ModelElementInstance completionCondition = loopCharacteristics
                    .getUniqueChildElementByType(CompletionCondition.class);
            if (completionCondition != null) {
                final String completionConditionExpression = completionCondition.getTextContent();
                if (completionConditionExpression != null && completionConditionExpression.trim().length() > 0) {
                    processVariablesExpression.putAll(findVariablesInExpression(javaReaderStatic, fileScanner,
                            completionConditionExpression, element, ElementChapter.MultiInstance,
                            KnownElementFieldType.CompletionCondition, scopeId, predecessors));
                }
            }
            final ModelElementInstance loopDataInputRef = loopCharacteristics
                    .getUniqueChildElementByType(LoopDataInputRef.class);
            if (loopDataInputRef != null) {
                final String dataInputRefName = loopDataInputRef.getTextContent();
                if (dataInputRefName != null && dataInputRefName.trim().length() > 0) {
                    processVariables.put(dataInputRefName,
                            new ProcessVariableOperation(dataInputRefName, element, ElementChapter.MultiInstance,
                                    KnownElementFieldType.CollectionElement, null, VariableOperation.READ, scopeId,
                                    element.getFlowAnalysis().getOperationCounter()));
                }
            }
            final ModelElementInstance inputDataItem = loopCharacteristics
                    .getUniqueChildElementByType(InputDataItem.class);
            if (inputDataItem != null) {
                final String inputDataItemName = inputDataItem.getAttributeValue("name");
                if (inputDataItemName != null && inputDataItemName.trim().length() > 0) {
                    processVariables.put(inputDataItemName,
                            new ProcessVariableOperation(inputDataItemName, element, ElementChapter.MultiInstance,
                                    KnownElementFieldType.CollectionElement, null, VariableOperation.WRITE, scopeId,
                                    element.getFlowAnalysis().getOperationCounter()));
                }
            }

            for (ProcessVariableOperation operation : processVariables.values()) {
                node.addOperation(operation);
            }
            processVariables.putAll(processVariablesExpression);

        }
        return processVariables;
    }

    private ListMultimap<String, ProcessVariableOperation> addDefaultMultiInstanceTaskVariables(
            final BpmnElement element) {
        ListMultimap<String, ProcessVariableOperation> defaultVariables = ArrayListMultimap.create();
        String scopeElementId = element.getId();

        ProcessVariableOperation operation = new ProcessVariableOperation("nrOfInstances", element,
                ElementChapter.MultiInstance,
                KnownElementFieldType.CamundaStandardVariables, null, VariableOperation.WRITE,
                scopeElementId, element.getFlowAnalysis().getOperationCounter());
        defaultVariables.put("nrOfInstances", operation);

        operation = new ProcessVariableOperation("nrOfActiveInstances", element,
                ElementChapter.MultiInstance,
                KnownElementFieldType.CamundaStandardVariables, null, VariableOperation.WRITE,
                scopeElementId, element.getFlowAnalysis().getOperationCounter());
        defaultVariables.put("nrOfActiveInstances", operation);

        operation = new ProcessVariableOperation("nrOfCompletedInstances", element,
                ElementChapter.MultiInstance,
                KnownElementFieldType.CamundaStandardVariables, null, VariableOperation.WRITE,
                scopeElementId, element.getFlowAnalysis().getOperationCounter());
        defaultVariables.put("nrOfCompletedInstances", operation);

        operation = new ProcessVariableOperation("loopCounter", element,
                ElementChapter.MultiInstance,
                KnownElementFieldType.CamundaStandardVariables, null, VariableOperation.WRITE,
                scopeElementId, element.getFlowAnalysis().getOperationCounter());
        defaultVariables.put("loopCounter", operation);

        return defaultVariables;
    }

    /**
     * Checks an external groovy script for process variables (read/write).
     *
     * @param groovyFile Groovy File
     * @param element    BpmnElement
     * @param chapter    ElementChapter
     * @param fieldType  KnownElementFieldType
     * @param scopeId    ScopeId
     * @return variables
     */
    private ListMultimap<String, ProcessVariableOperation> getVariablesFromGroovyScript(final String groovyFile,
            final BpmnElement element, final ElementChapter chapter, final KnownElementFieldType fieldType,
            final String scopeId) {

        return ResourceFileReader.readResourceFile(groovyFile, element, chapter, fieldType, scopeId);
    }

    /**
     * Scans a dmn file for process variables
     *
     * @param decisionId DecisionId
     * @param fileName   File Name
     * @param element    BpmnElement
     * @param chapter    ElementChapter
     * @param fieldType  KnownElementFieldType
     * @param scopeId    ScopeID
     * @return Variables
     */
    private ListMultimap<String, ProcessVariableOperation> readDmnFile(final String decisionId,
            final String fileName,
            final BpmnElement element, final ElementChapter chapter, final KnownElementFieldType fieldType,
            final String scopeId) {

        final ListMultimap<String, ProcessVariableOperation> variables = ArrayListMultimap.create();

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
                    variables.put(variable.getTextContent(),
                            new ProcessVariableOperation(variable.getTextContent(), element, chapter, fieldType,
                                    fileName, VariableOperation.READ, scopeId,
                                    element.getFlowAnalysis().getOperationCounter()));
                }
                final Collection<Output> outputs = decision.getModelInstance().getModelElementsByType(Output.class);
                for (final Output output : outputs) {
                    final String variable = output.getName();
                    variables.put(variable,
                            new ProcessVariableOperation(variable, element, chapter, fieldType, fileName,
                                    VariableOperation.WRITE, scopeId, element.getFlowAnalysis().getOperationCounter()));
                }
            }
        }
        return variables;
    }

    /**
     * Examine JUEL expressions for variables
     *
     * @param javaReaderStatic Static java reader
     * @param fileScanner      FileScanner
     * @param expression       Expression
     * @param element          BpmnElement
     * @param chapter          ElementChapter
     * @param fieldType        KnownElementFieldType
     * @param scopeId          ScopeId
     * @return variables
     */
    private ListMultimap<String, ProcessVariableOperation> findVariablesInExpression(
            final JavaReaderStatic javaReaderStatic,
            final FileScanner fileScanner, final String expression, final BpmnElement element,
            final ElementChapter chapter, final KnownElementFieldType fieldType, final String scopeId,
            final LinkedHashMap<String, AnalysisElement> predecessors) {
        final ListMultimap<String, ProcessVariableOperation> variables = ArrayListMultimap.create();
        final ControlFlowGraph controlFlowGraph = element.getControlFlowGraph();

        // HOTFIX: Catch pattern like below to avoid crash of TreeBuilder
        // ${dateTime().plusWeeks(1).toDate()}
        final Pattern pattern = Pattern.compile("\\$\\{(\\w)*\\(.*\\)\\}");

        Matcher matcher = pattern.matcher(expression);

        if (matcher.matches()) {
            return variables;
        }

        boolean isDelegated = false;
        ExpressionNode expNode = new ExpressionNode(element, expression, chapter);

        try {
            // remove object name from method calls, otherwise the method arguments could
            // not be found
            final String filteredExpression = expression.replaceAll("[\\w]+\\.", "");
            final TreeBuilder treeBuilder = new Builder();
            final Tree tree = treeBuilder.build(filteredExpression);

            final Iterable<IdentifierNode> identifierNodes = tree.getIdentifierNodes();
            ProcessVariableOperation operation;
            for (final IdentifierNode node : identifierNodes) {
                // checks, if found variable is a bean
                final String className = isBean(node.getName());
                if (className != null) {
                    // read variables in class file (bean)
                    variables.putAll(javaReaderStatic.getVariablesFromJavaDelegate(fileScanner, className, element,
                            chapter, fieldType, scopeId, predecessors));
                    isDelegated = true;
                } else {
                    // save variable
                    operation = new ProcessVariableOperation(node.getName(), element, chapter, fieldType,
                            element.getProcessDefinition(), VariableOperation.READ, scopeId,
                            element.getFlowAnalysis().getOperationCounter());
                    variables.put(node.getName(), operation);
                    expNode.getUsed().put(node.getName(), operation);
                    expNode.getOperations().put(node.getName(), operation);
                }
            }
            // extract written variables
            ListMultimap<String, ProcessVariableOperation> writeOperations = ResourceFileReader
                    .searchWrittenProcessVariablesInCode(element, chapter, fieldType, element.getProcessDefinition(),
                            scopeId, expression);
            variables.putAll(writeOperations);
            writeOperations.asMap().forEach((key, value) -> value.forEach(op -> {
                expNode.getOperations().put(op.getId(), op);
                expNode.getDefined().put(op.getId(), op);
            }));

            // extract deleted variables
            ListMultimap<String, ProcessVariableOperation> deleteOperations = ResourceFileReader
                    .searchRemovedProcessVariablesInCode(element, chapter, fieldType, element.getProcessDefinition(),
                            scopeId, expression);
            variables.putAll(deleteOperations);
            deleteOperations.asMap().forEach((key, value) -> value.forEach(op -> {
                expNode.getOperations().put(op.getId(), op);
                expNode.getKilled().put(op.getId(), op);
            }));
        } catch (final ELException e) {
            throw new ProcessingException("EL expression " + expression + " in " + element.getProcessDefinition()
                    + ", element ID: " + element.getBaseElement().getId() + ", Type: " + fieldType.getDescription()
                    + " couldn't be parsed", e);
        }

        // TODO are there other field Types that should be skipped?
        if (!fieldType.equals(KnownElementFieldType.CalledElement)
                && !fieldType.equals(KnownElementFieldType.CamundaOut)
                && !fieldType.equals(KnownElementFieldType.CamundaIn) && !isDelegated) {
            // TODO do we add the node if it is a bean and there are no operations?
            addNodeAndClearPredecessors(expNode, controlFlowGraph, predecessors);
        }

        return variables;
    }

    /**
     * @param javaReaderStatic Static java reader
     * @param expression       Expression
     * @param element          BpmnElement
     * @param fileScanner      FileScanner
     * @param chapter          ElementChapter
     * @param fieldType        KnownElementFieldType
     * @param scopeId          ScopeId
     * @return variables
     */
    private ListMultimap<String, ProcessVariableOperation> checkMessageAndSignalForExpression(
            final JavaReaderStatic javaReaderStatic, final String expression, final BpmnElement element,
            final FileScanner fileScanner, final ElementChapter chapter, final KnownElementFieldType fieldType,
            final String scopeId, final LinkedHashMap<String, AnalysisElement> predecessors) {
        final ListMultimap<String, ProcessVariableOperation> variables = ArrayListMultimap.create();
        try {

            final Pattern pattern = Pattern.compile(".*\\$\\{(.*?)}");
            final Matcher matcher = pattern.matcher(expression);

            // if value is in the form of ${expression}, try to resolve a bean and find all
            // subsequent process variables
            if (matcher.matches()) {
                if (isBean(matcher.group(1)) != null) {
                    variables.putAll(javaReaderStatic.getVariablesFromJavaDelegate(fileScanner,
                            isBean(matcher.group(1)), element, chapter, fieldType, scopeId, predecessors));
                } else {
                    variables.put(expression,
                            new ProcessVariableOperation(expression, element, chapter, fieldType,
                                    element.getProcessDefinition(), VariableOperation.READ, scopeId,
                                    element.getFlowAnalysis().getOperationCounter()));
                }
            }
        } catch (final ELException e) {
            throw new ProcessingException("EL expression " + expression + " in " + element.getProcessDefinition()
                    + ", element ID: " + element.getBaseElement().getId() + ", Type: "
                    + KnownElementFieldType.Expression + " couldn't be parsed", e);
        }
        return variables;
    }

    /**
     * Examines an expression for variable read
     *
     * @param javaReaderStatic Static java reader
     * @param expression       Expression
     * @param name             Variable name
     * @param element          BpmnElement
     * @param fileScanner      FileScanner
     * @param chapter          ElementChapter
     * @param fieldType        KnownElementFieldType
     * @param scopeId          ScopeId
     * @return variables
     */
    private ListMultimap<String, ProcessVariableOperation> checkExpressionForReadVariable(
            final JavaReaderStatic javaReaderStatic, final String expression, final String name,
            final BpmnElement element, final FileScanner fileScanner, final ElementChapter chapter,
            final KnownElementFieldType fieldType, final String scopeId,
            final LinkedHashMap<String, AnalysisElement> predecessors) {
        final ListMultimap<String, ProcessVariableOperation> variables = ArrayListMultimap.create();
        try {

            final Pattern pattern = Pattern.compile(".*\\$\\{(.*?)}");
            final Matcher matcher = pattern.matcher(expression);

            // if value is in the form of ${expression}, try to resolve a bean and find all
            // subsequent process variables
            // else, we assume that value is no proper expression and take the raw value ->
            // create READ operation
            if (matcher.matches()) {
                if (isBean(matcher.group(1)) != null) {
                    variables.putAll(javaReaderStatic.getVariablesFromJavaDelegate(fileScanner,
                            isBean(matcher.group(1)), element, chapter, fieldType, scopeId, predecessors));
                } else {
                    variables.put(name,
                            new ProcessVariableOperation(name, element, chapter, fieldType,
                                    element.getProcessDefinition(), VariableOperation.READ, scopeId,
                                    element.getFlowAnalysis().getOperationCounter()));
                }
            } else {
                variables.put(name,
                        new ProcessVariableOperation(name, element, chapter, fieldType, element.getProcessDefinition(),
                                VariableOperation.WRITE, scopeId, element.getFlowAnalysis().getOperationCounter()));
            }
        } catch (final ELException e) {
            throw new ProcessingException("EL expression " + expression + " in " + element.getProcessDefinition()
                    + ", element ID: " + element.getBaseElement().getId() + ", Type: "
                    + KnownElementFieldType.Expression + " couldn't be parsed", e);
        }

        return variables;
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

    private void addNodeAndClearPredecessors(AbstractNode node, ControlFlowGraph cg,
            LinkedHashMap<String, AnalysisElement> predecessors) {
        cg.addNode(node);
        node.setPredecessors(new LinkedHashMap<>(predecessors));
        predecessors.clear();
        predecessors.put(node.getId(), node);
    }
}
