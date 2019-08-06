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
package de.viadee.bpm.vPAV.processing.model.data;

import de.viadee.bpm.vPAV.BpmnScanner;
import de.viadee.bpm.vPAV.FileScanner;
import de.viadee.bpm.vPAV.RuntimeConfig;
import de.viadee.bpm.vPAV.config.model.RuleSet;
import de.viadee.bpm.vPAV.constants.ConfigConstants;
import de.viadee.bpm.vPAV.processing.ElementGraphBuilder;
import de.viadee.bpm.vPAV.processing.ProcessVariablesScanner;
import de.viadee.bpm.vPAV.processing.code.flow.AnalysisElement;
import de.viadee.bpm.vPAV.processing.code.flow.FlowAnalysis;
import de.viadee.bpm.vPAV.processing.model.graph.Graph;
import de.viadee.bpm.vPAV.processing.model.graph.Path;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

/**
 * test the handling of call activities
 *
 */
public class CallActivityTest {

	private static final String BASE_PATH = "src/test/resources/";

	@BeforeClass
	public static void setup() throws MalformedURLException {
		final File file = new File(".");
		final String currentPath = file.toURI().toURL().toString();
		final URL classUrl = new URL(currentPath + "src/test/java");
		final URL[] classUrls = { classUrl };
		ClassLoader cl = new URLClassLoader(classUrls);
		RuntimeConfig.getInstance().setClassLoader(cl);
		RuntimeConfig.getInstance().setTest(true);
	}

	@Test
	public void testEmbedding() {
		final ProcessVariablesScanner scanner = new ProcessVariablesScanner(null);
		final FileScanner fileScanner = new FileScanner(new RuleSet());
		final String PATH = BASE_PATH + "CallActivityTest_embeddingCallActivity.bpmn";
		final File processDefinition = new File(PATH);

		// parse bpmn model
		final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(processDefinition);

		// add reference for called process
		final Map<String, String> processIdToPathMap = new HashMap<>();
		processIdToPathMap.put("calledProcess", "CallActivityTest_calledProcess.bpmn");
		processIdToPathMap.put("calledcalledProcess", "CallActivityTest_calledcalledProcess.bpmn");

		final ElementGraphBuilder graphBuilder = new ElementGraphBuilder(null, processIdToPathMap, null, null,
				new BpmnScanner(PATH));

		FlowAnalysis flowAnalysis = new FlowAnalysis();

		// create data flow graphs
		final Collection<String> calledElementHierarchy = new ArrayList<>();
		final Collection<Graph> graphCollection = graphBuilder.createProcessGraph(fileScanner, modelInstance,
				processDefinition.getPath(), calledElementHierarchy, scanner, flowAnalysis);

		flowAnalysis.analyze(graphCollection);

		// calculate invalid paths based on data flow graphs
		final Map<AnomalyContainer, List<Path>> invalidPathMap = graphBuilder.createInvalidPaths(graphCollection);
		Iterator<AnomalyContainer> iterator = invalidPathMap.keySet().iterator();

		Assert.assertEquals("There are exactly three anomalies", 3, invalidPathMap.size());
		AnomalyContainer anomaly1 = iterator.next();
		AnomalyContainer anomaly2 = iterator.next();
		AnomalyContainer anomaly3 = iterator.next();
		// var2
		Assert.assertEquals("Expected a DD anomaly but got " + anomaly1.getAnomaly().toString(), Anomaly.DD,
				anomaly1.getAnomaly());
		// var4
		Assert.assertEquals("Expected a UR anomaly but got " + anomaly2.getAnomaly().toString(), Anomaly.UR,
				anomaly2.getAnomaly());
		// var3
		Assert.assertEquals("Expected a UR anomaly but got " + anomaly3.getAnomaly().toString(), Anomaly.UR,
				anomaly3.getAnomaly());
	}

	@Test
	public void testEmbeddedWithVariableMapping() {
		// TODO out in source expression verändern
		// Usage of camunda:in and camunda:out
		// Test with source expression
		final ProcessVariablesScanner scanner = new ProcessVariablesScanner(null);
		final FileScanner fileScanner = new FileScanner(new RuleSet());
		final String PATH = BASE_PATH + "CallActivityTest_variableMapping.bpmn";
		final File processDefinition = new File(PATH);

		// parse bpmn model
		final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(processDefinition);

		// add reference for called process
		final Map<String, String> processIdToPathMap = new HashMap<>();
		processIdToPathMap.put("calledProcess", "CallActivityTest_delegatedVarMapping_calledProcess.bpmn");

		final ElementGraphBuilder graphBuilder = new ElementGraphBuilder(null, processIdToPathMap, null, null,
				new BpmnScanner(PATH));

		FlowAnalysis flowAnalysis = new FlowAnalysis();

		// create data flow graphs
		final Collection<String> calledElementHierarchy = new ArrayList<>();
		final Collection<Graph> graphCollection = graphBuilder.createProcessGraph(fileScanner, modelInstance,
				processDefinition.getPath(), calledElementHierarchy, scanner, flowAnalysis);

		flowAnalysis.analyze(graphCollection);

		// calculate invalid paths based on data flow graphs
		final Map<AnomalyContainer, List<Path>> invalidPathMap = graphBuilder.createInvalidPaths(graphCollection);
		Iterator<AnomalyContainer> iterator = invalidPathMap.keySet().iterator();

		AnomalyContainer anomaly1 = iterator.next();
		Assert.assertEquals("There should be exactly one anomaly", 1, invalidPathMap.size());
		Assert.assertEquals("Expected a UR anomaly but got " + anomaly1.getAnomaly().toString(), Anomaly.UR,
				anomaly1.getAnomaly());
	}


	@Test
	public void testEmbeddedWithDelegateVariableMapping() {
		//T ODO test also delegate expression
		final ProcessVariablesScanner scanner = new ProcessVariablesScanner(null);
		final FileScanner fileScanner = new FileScanner(new RuleSet());
		final String PATH = BASE_PATH + "CallActivityTest_delegatedVarMapping.bpmn";
		final File processDefinition = new File(PATH);

		// parse bpmn model
		final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(processDefinition);

		// add reference for called process
		final Map<String, String> processIdToPathMap = new HashMap<>();
		processIdToPathMap.put("calledProcess", "CallActivityTest_delegatedVarMapping_calledProcess.bpmn");

		final ElementGraphBuilder graphBuilder = new ElementGraphBuilder(null, processIdToPathMap, null, null,
				new BpmnScanner(PATH));

		FlowAnalysis flowAnalysis = new FlowAnalysis();

		// create data flow graphs
		final Collection<String> calledElementHierarchy = new ArrayList<>();
		final Collection<Graph> graphCollection = graphBuilder.createProcessGraph(fileScanner, modelInstance,
				processDefinition.getPath(), calledElementHierarchy, scanner, flowAnalysis);

		flowAnalysis.analyze(graphCollection);

		// calculate invalid paths based on data flow graphs
		final Map<AnomalyContainer, List<Path>> invalidPathMap = graphBuilder.createInvalidPaths(graphCollection);
		Iterator<AnomalyContainer> iterator = invalidPathMap.keySet().iterator();

		Assert.assertEquals("There should be exactly one anomaly", 2, invalidPathMap.size());
		AnomalyContainer anomaly1 = iterator.next();
		AnomalyContainer anomaly2 = iterator.next();
		Assert.assertEquals("Expected a UR anomaly but got " + anomaly1.getAnomaly().toString(), Anomaly.UR,
				anomaly1.getAnomaly());
		Assert.assertEquals("Expected a UR anomaly but got " + anomaly2.getAnomaly().toString(), Anomaly.UR,
				anomaly2.getAnomaly());
	}


	@Test
	public void testEmbeddingCallActivitiesWithListenerStart() {
		final ProcessVariablesScanner scanner = new ProcessVariablesScanner(null);
		Properties myProperties = new Properties();
		myProperties.put("scanpath", "src/test/java");
		ConfigConstants.getInstance().setProperties(myProperties);
		final FileScanner fileScanner = new FileScanner(new RuleSet());
		final String PATH = BASE_PATH + "CallActivityWithListenerStartTest.bpmn";
		final File processDefinition = new File(PATH);

		// parse bpmn model
		final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(processDefinition);

		// add reference for called process
		final Map<String, String> processIdToPathMap = new HashMap<>();
		processIdToPathMap.put("calledElement", "CalledActivityWithListenerTest.bpmn");

		final ElementGraphBuilder graphBuilder = new ElementGraphBuilder(null, processIdToPathMap, null, null,
				new BpmnScanner(PATH));

		FlowAnalysis flowAnalysis = new FlowAnalysis();

		// create data flow graphs
		final Collection<String> calledElementHierarchy = new ArrayList<>();
		final Collection<Graph> graphCollection = graphBuilder.createProcessGraph(fileScanner, modelInstance,
				processDefinition.getPath(), calledElementHierarchy, scanner, flowAnalysis);

		flowAnalysis.analyze(graphCollection);
		Iterator<AnalysisElement> iterator = flowAnalysis.getNodes().values().iterator();
		AnalysisElement startEvent1 = iterator.next();
		Assert.assertEquals("", 0, startEvent1.getPredecessors().size());
		AnalysisElement sequenceFlow1 = iterator.next();
		Assert.assertEquals("", "StartEvent_1", sequenceFlow1.getPredecessors().get(0).getId());
		AnalysisElement endEvent1 = iterator.next();
		Assert.assertEquals("", "SequenceFlow_2", endEvent1.getPredecessors().get(0).getId());
		AnalysisElement sequenceFlow2 = iterator.next();
		Assert.assertEquals("", "_EndEvent_1_1", sequenceFlow2.getPredecessors().get(0).getId());
		AnalysisElement startEvent1_1 = iterator.next();
		Assert.assertEquals("", "CallActivity__2", startEvent1_1.getPredecessors().get(0).getId());
		AnalysisElement endEvent1_1 = iterator.next();
		Assert.assertEquals("", "_SequenceFlow_2_2", endEvent1_1.getPredecessors().get(0).getId());
		AnalysisElement sequenceFlow1_1 = iterator.next();
		Assert.assertEquals("", "_StartEvent_1_1", sequenceFlow1_1.getPredecessors().get(0).getId());
		AnalysisElement sequenceFlow2_2 = iterator.next();
		Assert.assertEquals("", "_Task_1_1", sequenceFlow2_2.getPredecessors().get(0).getId());
		AnalysisElement task1_1 = iterator.next();
		Assert.assertEquals("", "_SequenceFlow_1_1", task1_1.getPredecessors().get(0).getId());
		AnalysisElement ca0 = iterator.next();
		Assert.assertEquals("", "SequenceFlow_1", ca0.getPredecessors().get(0).getId());
		AnalysisElement ca1 = iterator.next();
		Assert.assertEquals("", "CallActivity__0", ca1.getPredecessors().get(0).getId());
		AnalysisElement ca2 = iterator.next();
		Assert.assertEquals("", "CallActivity__1", ca2.getPredecessors().get(0).getId());
	}

	@Test
	public void testEmbeddingCallActivitiesWithListenerEnd() {
		final ProcessVariablesScanner scanner = new ProcessVariablesScanner(null);
		Properties myProperties = new Properties();
		myProperties.put("scanpath", "src/test/java");
		ConfigConstants.getInstance().setProperties(myProperties);
		final FileScanner fileScanner = new FileScanner(new RuleSet());
		final String PATH = BASE_PATH + "CallActivityWithListenerEndTest.bpmn";
		final File processDefinition = new File(PATH);

		// parse bpmn model
		final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(processDefinition);

		// add reference for called process
		final Map<String, String> processIdToPathMap = new HashMap<>();
		processIdToPathMap.put("calledElement", "CalledActivityWithListenerTest.bpmn");

		final ElementGraphBuilder graphBuilder = new ElementGraphBuilder(null, processIdToPathMap, null, null,
				new BpmnScanner(PATH));

		FlowAnalysis flowAnalysis = new FlowAnalysis();

		// create data flow graphs
		final Collection<String> calledElementHierarchy = new ArrayList<>();
		final Collection<Graph> graphCollection = graphBuilder.createProcessGraph(fileScanner, modelInstance,
				processDefinition.getPath(), calledElementHierarchy, scanner, flowAnalysis);

		flowAnalysis.analyze(graphCollection);
		Iterator<AnalysisElement> iterator = flowAnalysis.getNodes().values().iterator();
		AnalysisElement startEvent1 = iterator.next();
		Assert.assertEquals("", 0, startEvent1.getPredecessors().size());
		AnalysisElement sequenceFlow1 = iterator.next();
		Assert.assertEquals("", "StartEvent_1", sequenceFlow1.getPredecessors().get(0).getId());
		AnalysisElement endEvent1 = iterator.next();
		Assert.assertEquals("", "SequenceFlow_2", endEvent1.getPredecessors().get(0).getId());
		AnalysisElement sequenceFlow2 = iterator.next();
		Assert.assertEquals("", "CallActivity__2", sequenceFlow2.getPredecessors().get(0).getId());
		AnalysisElement startEvent1_1 = iterator.next();
		Assert.assertEquals("", "SequenceFlow_1", startEvent1_1.getPredecessors().get(0).getId());
		AnalysisElement endEvent1_1 = iterator.next();
		Assert.assertEquals("", "_SequenceFlow_2_2", endEvent1_1.getPredecessors().get(0).getId());
		AnalysisElement sequenceFlow1_1 = iterator.next();
		Assert.assertEquals("", "_StartEvent_1_1", sequenceFlow1_1.getPredecessors().get(0).getId());
		AnalysisElement sequenceFlow2_2 = iterator.next();
		Assert.assertEquals("", "_Task_1_1", sequenceFlow2_2.getPredecessors().get(0).getId());
		AnalysisElement task1_1 = iterator.next();
		Assert.assertEquals("", "_SequenceFlow_1_1", task1_1.getPredecessors().get(0).getId());
		AnalysisElement ca0 = iterator.next();
		Assert.assertEquals("", "_EndEvent_1_1", ca0.getPredecessors().get(0).getId());
		AnalysisElement ca1 = iterator.next();
		Assert.assertEquals("", "CallActivity__0", ca1.getPredecessors().get(0).getId());
		AnalysisElement ca2 = iterator.next();
		Assert.assertEquals("", "CallActivity__1", ca2.getPredecessors().get(0).getId());
	}

}