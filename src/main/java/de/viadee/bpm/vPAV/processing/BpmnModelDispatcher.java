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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.BaseElement;
import org.xml.sax.SAXException;

import de.viadee.bpm.vPAV.BpmnScanner;
import de.viadee.bpm.vPAV.FileScanner;
import de.viadee.bpm.vPAV.config.model.Rule;
import de.viadee.bpm.vPAV.config.model.Setting;
import de.viadee.bpm.vPAV.constants.BpmnConstants;
import de.viadee.bpm.vPAV.constants.ConfigConstants;
import de.viadee.bpm.vPAV.processing.checker.CheckerFactory;
import de.viadee.bpm.vPAV.processing.checker.DataFlowChecker;
import de.viadee.bpm.vPAV.processing.checker.ElementChecker;
import de.viadee.bpm.vPAV.processing.checker.ModelChecker;
import de.viadee.bpm.vPAV.processing.checker.ProcessVariablesModelChecker;
import de.viadee.bpm.vPAV.processing.dataflow.DataFlowRule;
import de.viadee.bpm.vPAV.processing.model.data.AnomalyContainer;
import de.viadee.bpm.vPAV.processing.model.data.BpmnElement;
import de.viadee.bpm.vPAV.processing.model.data.CheckerIssue;
import de.viadee.bpm.vPAV.processing.model.data.ModelDispatchResult;
import de.viadee.bpm.vPAV.processing.model.data.ProcessVariable;
import de.viadee.bpm.vPAV.processing.model.data.ProcessVariableOperation;
import de.viadee.bpm.vPAV.processing.model.graph.IGraph;
import de.viadee.bpm.vPAV.processing.model.graph.Path;

/**
 * Calls model and element checkers for a concrete bpmn processdefinition
 *
 */
public class BpmnModelDispatcher {
	
	private Map<String, String> incorrectCheckers = new HashMap<>();
	
	/**
	 * The BpmnModelDispatcher reads a model and creates a collection of all elements. Iterates through collection and
     * checks each element for validity Additionally a graph is created to check for invalid paths.
	 * @param fileScanner
	 * - FileScanner
	 * @param processdefinition
	 * - Holds the path to the BPMN model
	 * @param decisionRefToPathMap
	 * - DecisionRefToPathMap
	 * @param processIdToPathMap
	 * - Map of processId to BPMN file
	 * @param messageIdToVariables
	 * - Map of messages and their variables
	 * @param processIdToVariables
	 * - Map of processId and their variables
	 * @param dataFlowRules
	 * - DataFlowRules to be checked for
	 * @param resourcesNewestVersions
	 * - Collection with newest versions of class files
	 * @param conf
	 * - ruleSet
	 * @return issues
	 */
    public ModelDispatchResult dispatchWithVariables(final FileScanner fileScanner, final File processdefinition,
            final Map<String, String> decisionRefToPathMap, final Map<String, String> processIdToPathMap,
            final Map<String, Collection<String>> messageIdToVariables,
            final Map<String, Collection<String>> processIdToVariables,
            final Collection<DataFlowRule> dataFlowRules,
            final Collection<String> resourcesNewestVersions, final Map<String, Rule> conf) {

        final BpmnScanner bpmnScanner = createScanner(processdefinition);
        
        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(processdefinition);

        // hold bpmn elements
        final Collection<BaseElement> baseElements = modelInstance
                .getModelElementsByType(BaseElement.class);

        final ElementGraphBuilder graphBuilder = new ElementGraphBuilder(decisionRefToPathMap,
                processIdToPathMap, messageIdToVariables,
                processIdToVariables, bpmnScanner);

        
        // Depending on Regex/Static analysis, find Process Variables from Java Delegate
        JavaReaderContext jvc = new JavaReaderContext();
             
        if (getIsStatic(conf)) {
            jvc.setJavaReadingStrategy(new JavaReaderStatic());
        } else {
            jvc.setJavaReadingStrategy(new JavaReaderRegex());
        }
              
        // create data flow graphs for bpmn model
        final Collection<IGraph> graphCollection = graphBuilder.createProcessGraph(jvc, fileScanner, modelInstance,
                processdefinition.getPath(), new ArrayList<>());

        // add data flow information to graph and calculate invalid paths
        final Map<AnomalyContainer, List<Path>> invalidPathMap = graphBuilder
                .createInvalidPaths(graphCollection);

        final Collection<BpmnElement> bpmnElements =
                getBpmnElements(processdefinition, baseElements, graphBuilder);
        final Collection<ProcessVariable> processVariables = getProcessVariables(bpmnElements);

        final Collection<CheckerIssue> issues = new ArrayList<>();

        // call model checkers
        // TODO: move it to a factory class later
        final Rule processVariablesModelRule = conf
                .get(getClassName(ProcessVariablesModelChecker.class));
        if (processVariablesModelRule != null && processVariablesModelRule.isActive()) {
            final ModelChecker processVarChecker = new ProcessVariablesModelChecker(
                    processVariablesModelRule, invalidPathMap);
            issues.addAll(processVarChecker.check(modelInstance));
        }
        final Rule dataFlowRule = conf
                .get(getClassName(DataFlowChecker.class));
        if (dataFlowRule != null && dataFlowRule.isActive() && !dataFlowRules.isEmpty()) {
            final DataFlowChecker dataFlowChecker = new DataFlowChecker(
                    dataFlowRule, dataFlowRules, processVariables
            );
            issues.addAll(dataFlowChecker.check(modelInstance));
        }

        // create checkerInstances
        Collection<ElementChecker> checkerInstances = createCheckerInstances(resourcesNewestVersions, conf,
                bpmnScanner, issues);

        executeCheckers(processdefinition, baseElements, graphBuilder, issues, checkerInstances);

        return new ModelDispatchResult(issues, bpmnElements, processVariables);
    }


    /**
     * The BpmnModelDispatcher reads a model and creates a collection of all elements. Iterates through collection and
     * checks each element for validity Additionally a graph is created to check for invalid paths.
     *
     * @param processdefinition
     *            Holds the path to the BPMN model
     * @param decisionRefToPathMap
     *            decisionRefToPathMap
     * @param processIdToPathMap
     *            Map of prozessId to bpmn file
     * @param resourcesNewestVersions
     *            collection with newest versions of class files
     * @param conf
     *            ruleSet
     * @return issues
     */
    public ModelDispatchResult dispatchWithoutVariables(final File processdefinition,
           final Map<String, String> decisionRefToPathMap,
           final Map<String, String> processIdToPathMap,
           final Collection<String> resourcesNewestVersions,
           final Map<String, Rule> conf) {

        BpmnScanner bpmnScanner = createScanner(processdefinition);

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(processdefinition);

        // hold bpmn elements
        final Collection<BaseElement> baseElements = modelInstance
                .getModelElementsByType(BaseElement.class);

        final ElementGraphBuilder graphBuilder = new ElementGraphBuilder(decisionRefToPathMap,
                processIdToPathMap, bpmnScanner);

        final Collection<CheckerIssue> issues = new ArrayList<>();

        // create checkerInstances as singletons
        Collection<ElementChecker> checkerInstances = createCheckerInstances(resourcesNewestVersions, conf,
                bpmnScanner, issues);

        executeCheckers(processdefinition, baseElements, graphBuilder, issues, checkerInstances);

        return new ModelDispatchResult(issues, getBpmnElements(processdefinition, baseElements, graphBuilder), Collections.emptyList());
    }

    /**
     * @param baseElements
     *            Collection of baseelements
     * @param graphBuilder
     *            graphBuilder
     * @param processdefinition
     *            bpmn file
     *            @return Collection of BpmnElements
     */
    public static Collection<BpmnElement> getBpmnElements(
            final File processdefinition, final Collection<BaseElement> baseElements, final ElementGraphBuilder graphBuilder) {
        final List<BpmnElement> elements = new ArrayList<>();
        for (final BaseElement baseElement : baseElements) {
            BpmnElement element = graphBuilder.getElement(baseElement.getId());
            if (element == null) {
                // if element is not in the data flow graph, create it.
                element = new BpmnElement(processdefinition.getPath(), baseElement);
            }
            elements.add(element);
        }
        return elements;
    }

    /**
     * @param elements
     *            Collection of BPMN elements
     *            @return Collection of process variables
     */
    public static Collection<ProcessVariable> getProcessVariables(Collection<BpmnElement> elements) {
        // write variables containing elements
        // first, we need to inverse mapping to process variable -> operations (including element)
        final Map<String, ProcessVariable> processVariables = new HashMap<>();
        for (final BpmnElement element : elements) {
            for (final ProcessVariableOperation variableOperation : element.getProcessVariables().values()) {
                final String variableName = variableOperation.getName();
                if (!processVariables.containsKey(variableName)) {
                    processVariables.put(variableName, new ProcessVariable(variableName));
                }
                final ProcessVariable processVariable = processVariables.get(variableName);
                switch (variableOperation.getOperation()) {
                    case READ:
                        processVariable.addRead(variableOperation);
                        break;
                    case WRITE:
                        processVariable.addWrite(variableOperation);
                        break;
                    case DELETE:
                        processVariable.addDelete(variableOperation);
                        break;
                }
            }
        }
        return processVariables.values();
    }

    /**
     * 
     * @param processdefinition
     *            Holds the path to the BPMN model
     * @param baseElements
     *            List of baseElements
     * @param graphBuilder
     *            ElementGraphBuilder used for data flow of a BPMN Model
     * @param issues
     *            List of issues
     * @param checkerInstances
     *            ElementCheckers from ruleSet
     */
    private void executeCheckers(final File processdefinition, final Collection<BaseElement> baseElements,
            final ElementGraphBuilder graphBuilder, final Collection<CheckerIssue> issues,
            Collection<ElementChecker> checkerInstances) {
        // execute element checkers
        for (final BaseElement baseElement : baseElements) {
            BpmnElement element = graphBuilder.getElement(baseElement.getId());
            if (element == null) {
                // if element is not in the data flow graph, create it.
                element = new BpmnElement(processdefinition.getPath(), baseElement);
            }
            for (final ElementChecker checker : checkerInstances) {
                issues.addAll(checker.check(element));
            }
        }
    }

    /**
     * 
     * @param processdefinition
     *            Holds the path to the BPMN model
     * @return BpmnScanner
     */
    public BpmnScanner createScanner(final File processdefinition) {
        // create BPMNScanner
        BpmnScanner bpmnScanner;
        try {
            bpmnScanner = new BpmnScanner(processdefinition.getPath());
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new RuntimeException("Model couldn't be parsed");
        }
        return bpmnScanner;
    }

    /**
     * 
     * @param resourcesNewestVersions
     *            Resources with their newest version as found on classpath during runtime
     * @param conf
     *            ruleSet
     * @param bpmnScanner
     *            BPMNScanner
     * @param issues
     *            List of issues
     * @return CheckerCollection
     */
    private Collection<ElementChecker> createCheckerInstances(final Collection<String> resourcesNewestVersions,
            final Map<String, Rule> conf, BpmnScanner bpmnScanner, final Collection<CheckerIssue> issues) {
    	CheckerFactory checkerFactory = new CheckerFactory();

        final Collection<ElementChecker> checkerCollection = checkerFactory
                .createCheckerInstances(conf, resourcesNewestVersions, bpmnScanner);
        
        setIncorrectCheckers(checkerFactory.getIncorrectCheckers());

        return checkerCollection;
    }
    
    /**
	 * Based on the RuleSet config file, set the usage of Static Analysis to true.
	 * Based on the boolean value the // * Process Variables are collected with
	 * Regex or Static Analysis // * // * @param rules // * @return //
	 */
	private boolean getIsStatic(Map<String, Rule> rules) {
		boolean isStatic = false;
		Rule rule = rules.get(BpmnConstants.PROCESS_VARIABLE_MODEL_CHECKER);
		if (rule != null) {
			Setting setting = rule.getSettings().get(ConfigConstants.USE_STATIC_ANALYSIS_BOOLEAN);
			if (setting != null && setting.getValue().equals("true"))
				isStatic = true;
		}
		return isStatic;
	}

    private String getClassName(Class<?> clazz) {
        return clazz.getSimpleName();
    }


	public Map<String, String> getIncorrectCheckers() {
		return incorrectCheckers;
	}


	public void setIncorrectCheckers(Map<String, String> incorrectCheckers) {
		this.incorrectCheckers = incorrectCheckers;
	}
}
