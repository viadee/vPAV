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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import de.viadee.bpm.vPAV.FileScanner;
import de.viadee.bpm.vPAV.IssueService;
import de.viadee.bpm.vPAV.config.model.Rule;
import de.viadee.bpm.vPAV.config.model.RuleSet;
import de.viadee.bpm.vPAV.constants.BpmnConstants;
import de.viadee.bpm.vPAV.processing.checker.CheckerFactory;
import de.viadee.bpm.vPAV.processing.checker.ElementChecker;
import de.viadee.bpm.vPAV.processing.checker.ModelChecker;
import de.viadee.bpm.vPAV.processing.code.flow.BpmnElement;
import de.viadee.bpm.vPAV.processing.code.flow.ControlFlowGraph;
import de.viadee.bpm.vPAV.processing.code.flow.FlowAnalysis;
import de.viadee.bpm.vPAV.processing.dataflow.DataFlowRule;
import de.viadee.bpm.vPAV.processing.model.data.*;
import de.viadee.bpm.vPAV.processing.model.graph.Graph;
import de.viadee.bpm.vPAV.processing.model.graph.Path;
import org.apache.commons.io.FilenameUtils;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.BaseElement;
import org.camunda.bpm.model.bpmn.instance.Process;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Calls model and element checkers for a concrete bpmn processdefinition
 */
public class BpmnModelDispatcher {

    private Map<String, String> incorrectCheckers = new HashMap<>();

    private BpmnModelInstance modelInstance;

    Collection<BaseElement> baseElements;

    private void prepareDispatcher(final File processDefinition) {
        final String key = FilenameUtils.separatorsToUnix(processDefinition.getPath());
        // parse bpmn model
        modelInstance = Bpmn.readModelFromFile(processDefinition);
        // hold bpmn elements
        baseElements = modelInstance.getModelElementsByType(BaseElement.class);

        final Set<String> elementIdsSet = new HashSet<String>(baseElements.stream()
                .filter(element -> !(element.getElementType().getInstanceType()//Process elements aren't checked
                        .equals((Process.class))))
                .map(BaseElement::getId)
                .collect(Collectors.toSet()));
        if (IssueService.getInstance().getElementIdToBpmnFileMap().containsKey(key)) {
            IssueService.getInstance().getElementIdToBpmnFileMap().get(key).addAll(elementIdsSet);
        } else {
            IssueService.getInstance().getElementIdToBpmnFileMap().put(key, new HashSet<String>(elementIdsSet));
        }
    }

    /**
     * The BpmnModelDispatcher reads a model and creates a collection of all
     * elements. Iterates through collection and checks each element for validity
     * Additionally a graph is created to check for invalid paths.
     *
     * @param fileScanner       - FileScanner
     * @param processDefinition - Holds the path to the BPMN model
     * @param scanner           - OuterProcessVariableScanner
     * @param dataFlowRules     - DataFlowRules to be checked for
     * @param conf              - ruleSet
     * @return issues
     */
    public ModelDispatchResult dispatchWithVariables(final FileScanner fileScanner, final File processDefinition,
            final EntryPointScanner scanner, final Collection<DataFlowRule> dataFlowRules, final RuleSet conf) {
        final Map<String, String> decisionRefToPathMap = fileScanner.getDecisionRefToPathMap();
        final Map<String, String> processIdToPathMap = fileScanner.getProcessIdToPathMap();
        final Collection<String> resourcesNewestVersions = fileScanner.getResourcesNewestVersions();
        FlowAnalysis flowAnalysis = new FlowAnalysis();

        prepareDispatcher(processDefinition);

        Rule rule = null;
        if (conf.getModelRules().containsKey(BpmnConstants.PROCESS_VARIABLE_MODEL_CHECKER)) {
            rule = conf.getModelRules().get(BpmnConstants.PROCESS_VARIABLE_MODEL_CHECKER)
                    .get(BpmnConstants.PROCESS_VARIABLE_MODEL_CHECKER);
        }

        final ElementGraphBuilder graphBuilder = new ElementGraphBuilder(decisionRefToPathMap, processIdToPathMap,
                scanner.getMessageIdToVariableMap(), scanner.getProcessIdToVariableMap(), rule);

        // create data flow graphs for bpmn model
        final Collection<Graph> graphCollection = graphBuilder.createProcessGraph(fileScanner, modelInstance,
                processDefinition.getPath(), new ArrayList<>(), scanner, flowAnalysis);

        // analyze data flows
        flowAnalysis.analyze(graphCollection);

        // calculate invalid paths
        final Map<AnomalyContainer, List<Path>> invalidPathMap = graphBuilder.createInvalidPaths(graphCollection);

        final Collection<BpmnElement> bpmnElements = getBpmnElements(processDefinition, baseElements, graphBuilder,
                flowAnalysis);
        final Collection<ProcessVariable> processVariables = getProcessVariables(bpmnElements);

        final Collection<CheckerIssue> issues = new ArrayList<>();

        final Collection[] checkers = createCheckerInstances(resourcesNewestVersions, conf, scanner,
                dataFlowRules, processVariables, invalidPathMap, flowAnalysis);

        // Execute model checkers.
        for (ModelChecker checker : (Collection<ModelChecker>) checkers[1]) {
            issues.addAll(checker.check());
        }

        // Execute element checkers.
        executeCheckers(processDefinition, baseElements, graphBuilder, (Collection<ElementChecker>) checkers[0],
                flowAnalysis);

        return new ModelDispatchResult(issues, bpmnElements, processVariables);
    }

    /**
     * The BpmnModelDispatcher reads a model and creates a collection of all
     * elements. Iterates through collection and checks each element for validity
     * Additionally a graph is created to check for invalid paths.
     *
     * @param processDefinition       Holds the path to the BPMN model
     * @param decisionRefToPathMap    decisionRefToPathMap
     * @param processIdToPathMap      Map of prozessId to bpmn file
     * @param resourcesNewestVersions collection with newest versions of class files
     * @param conf                    ruleSet
     * @return issues
     */
    public ModelDispatchResult dispatchWithoutVariables(final File processDefinition,
            final Map<String, String> decisionRefToPathMap, final Map<String, String> processIdToPathMap,
            final Collection<String> resourcesNewestVersions, final RuleSet conf) {
        FlowAnalysis flowAnalysis = new FlowAnalysis();

        JavaReaderStatic.setupSoot();

        prepareDispatcher(processDefinition);

        final ElementGraphBuilder graphBuilder = new ElementGraphBuilder(decisionRefToPathMap, processIdToPathMap);

        final Collection<CheckerIssue> issues = new ArrayList<>();

        final Collection[] checkers = createCheckerInstances(resourcesNewestVersions, conf, null, null,
                null, null, null);

        // Execute element checkers.
        executeCheckers(processDefinition, baseElements, graphBuilder, (Collection<ElementChecker>) checkers[0],
                flowAnalysis);

        return new ModelDispatchResult(issues,
                getBpmnElements(processDefinition, baseElements, graphBuilder, flowAnalysis), Collections.emptyList());
    }

    /**
     * @param baseElements      Collection of baseElements
     * @param graphBuilder      graphBuilder
     * @param processDefinition bpmn file
     * @param flowAnalysis      FlowAnalysis
     * @return Collection of BpmnElements
     */
    public static Collection<BpmnElement> getBpmnElements(final File processDefinition,
            final Collection<BaseElement> baseElements, final ElementGraphBuilder graphBuilder,
            final FlowAnalysis flowAnalysis) {
        final List<BpmnElement> elements = new ArrayList<>();
        for (final BaseElement baseElement : baseElements) {
            BpmnElement element = graphBuilder.getElement(baseElement.getId());
            if (element == null) {
                // if element is not in the data flow graph, create it.
                ControlFlowGraph controlFlowGraph = new ControlFlowGraph();
                element = new BpmnElement(processDefinition.getPath(), baseElement, controlFlowGraph, flowAnalysis);
            }
            elements.add(element);
        }
        return elements;
    }

    /**
     * @param elements Collection of BPMN elements
     * @return Collection of process variables
     */
    public static Collection<ProcessVariable> getProcessVariables(final Collection<BpmnElement> elements) {
        // write variables containing elements
        // first, we need to inverse mapping to process variable -> operations
        // (including element)
        final ListMultimap<String, ProcessVariable> variables = ArrayListMultimap.create();
        for (final BpmnElement element : elements) {
            for (final ProcessVariableOperation variableOperation : element.getProcessVariables().values()) {
                final String variableName = variableOperation.getName();
                if (!variables.containsKey(variableName)) {
                    variables.put(variableName, new ProcessVariable(variableName));
                }
                final Collection<ProcessVariable> processVariables = variables.asMap().get(variableName);
                for (ProcessVariable pv : processVariables) {
                    switch (variableOperation.getOperation()) {
                        case READ:
                            pv.addRead(variableOperation);
                            break;
                        case WRITE:
                            pv.addWrite(variableOperation);
                            break;
                        case DELETE:
                            pv.addDelete(variableOperation);
                            break;
                    }
                }
            }
        }
        return variables.values();
    }

    /**
     * @param processDefinition Holds the path to the BPMN model
     * @param baseElements      List of baseElements
     * @param graphBuilder      ElementGraphBuilder used for data flow of a BPMN
     *                          Model
     * @param checkerInstances  ElementCheckers from ruleSet
     * @param flowAnalysis      FlowAnalysis
     */
    private void executeCheckers(final File processDefinition, final Collection<BaseElement> baseElements,
            final ElementGraphBuilder graphBuilder, final Collection<ElementChecker> checkerInstances,
            final FlowAnalysis flowAnalysis) {
        // execute element checkers
        for (final BaseElement baseElement : baseElements) {
            BpmnElement element = graphBuilder.getElement(baseElement.getId());
            if (element == null) {
                // if element is not in the data flow graph, create it.
                ControlFlowGraph controlFlowGraph = new ControlFlowGraph();
                element = new BpmnElement(processDefinition.getPath(), baseElement, controlFlowGraph, flowAnalysis);
            }
            for (final ElementChecker checker : checkerInstances) {
                checker.check(element);
            }
        }
    }

    /**
     * @param resourcesNewestVersions Resources with their newest version as found
     *                                on classpath during runtime
     * @param conf                    ruleSet
     * @param scanner                 ProcessVariablesScanner
     * @param dataFlowRules           Collection of data flow rules
     * @param processVariables        collection of variables
     * @param invalidPathMap          invalid paths
     * @return CheckerCollection
     */
    Collection[] createCheckerInstances(final Collection<String> resourcesNewestVersions, final RuleSet conf,
            final EntryPointScanner scanner,
            final Collection<DataFlowRule> dataFlowRules, final Collection<ProcessVariable> processVariables,
            final Map<AnomalyContainer, List<Path>> invalidPathMap, final FlowAnalysis flowAnalysis) {
        CheckerFactory checkerFactory = new CheckerFactory();

        final Collection[] checkerCollection = checkerFactory.createCheckerInstances(conf, resourcesNewestVersions,
                scanner, dataFlowRules, processVariables, invalidPathMap, flowAnalysis);

        setIncorrectCheckers(checkerFactory.getIncorrectCheckers());

        return checkerCollection;
    }

    public Map<String, String> getIncorrectCheckers() {
        return incorrectCheckers;
    }

    private void setIncorrectCheckers(Map<String, String> incorrectCheckers) {
        this.incorrectCheckers = incorrectCheckers;
    }
}
