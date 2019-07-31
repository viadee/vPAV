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
package de.viadee.bpm.vPAV.processing.code;

import de.viadee.bpm.vPAV.BpmnScanner;
import de.viadee.bpm.vPAV.FileScanner;
import de.viadee.bpm.vPAV.RuntimeConfig;
import de.viadee.bpm.vPAV.config.model.RuleSet;
import de.viadee.bpm.vPAV.constants.ConfigConstants;
import de.viadee.bpm.vPAV.processing.ElementGraphBuilder;
import de.viadee.bpm.vPAV.processing.ProcessVariablesScanner;
import de.viadee.bpm.vPAV.processing.code.flow.FlowAnalysis;
import de.viadee.bpm.vPAV.processing.model.data.Anomaly;
import de.viadee.bpm.vPAV.processing.model.data.AnomalyContainer;
import de.viadee.bpm.vPAV.processing.model.graph.Graph;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

import static org.junit.Assert.assertEquals;

public class AnomaliesSetCreationTest {

	private static final String BASE_PATH = "src/test/resources/";

	@BeforeClass
	public static void setup() throws IOException {
		final File file = new File(".");
		final String currentPath = file.toURI().toURL().toString();
		final URL classUrl = new URL(currentPath + "src/test/java");
		final URL[] classUrls = { classUrl };
		ClassLoader cl = new URLClassLoader(classUrls);
		RuntimeConfig.getInstance().setClassLoader(cl);
		RuntimeConfig.getInstance().getResource("en_US");
		RuntimeConfig.getInstance().setTest(true);
	}

	@Test
	public void findModelAnomalies() {
		final ProcessVariablesScanner scanner = new ProcessVariablesScanner(null);
		Properties myProperties = new Properties();
		myProperties.put("scanpath", "src/test/java");
		ConfigConstants.getInstance().setProperties(myProperties);
		final FileScanner fileScanner = new FileScanner(new RuleSet());
		final String PATH = BASE_PATH + "ProcessVariablesModelCheckerTest_AnomaliesCreationModel.bpmn";
		final File processDefinition = new File(PATH);

		// parse bpmn model
		final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(processDefinition);

		final ElementGraphBuilder graphBuilder = new ElementGraphBuilder(null, null, null, null, new BpmnScanner(PATH));

		// create data flow graphs
		final Collection<String> calledElementHierarchy = new ArrayList<>();
		FlowAnalysis flowAnalysis = new FlowAnalysis();
		final Collection<Graph> graphCollection = graphBuilder.createProcessGraph(fileScanner, modelInstance,
				processDefinition.getPath(), calledElementHierarchy, scanner, flowAnalysis);

		flowAnalysis.analyze(graphCollection);

		Set<AnomalyContainer> anomalies = new HashSet<>();
		flowAnalysis.getNodes().values().forEach(
				analysisElement -> analysisElement.getAnomalies().forEach((key, value) -> anomalies.addAll(value)));

		Iterator<AnomalyContainer> iterator = anomalies.iterator();

		AnomalyContainer anomaly1 = iterator.next();
		AnomalyContainer anomaly2 = iterator.next();
		assertEquals("Expected 2 anomalies but found " + anomalies.size(), 2, anomalies.size());
		assertEquals("Expected a DU anomaly but found " + anomaly2.getAnomaly(), Anomaly.DU, anomaly1.getAnomaly());
		assertEquals("Expected a UR anomaly but found " + anomaly1.getAnomaly(), Anomaly.UR, anomaly2.getAnomaly());
	}

	@Test
	public void findDD() {
		final ProcessVariablesScanner scanner = new ProcessVariablesScanner(null);
		Properties myProperties = new Properties();
		myProperties.put("scanpath", "src/test/java");
		ConfigConstants.getInstance().setProperties(myProperties);
		final FileScanner fileScanner = new FileScanner(new RuleSet());
		final String PATH = BASE_PATH + "ProcessVariablesModelChecker_AnomalyDD.bpmn";
		final File processDefinition = new File(PATH);

		// parse bpmn model
		final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(processDefinition);

		final ElementGraphBuilder graphBuilder = new ElementGraphBuilder(null, null, null, null, new BpmnScanner(PATH));

		// create data flow graphs
		final Collection<String> calledElementHierarchy = new ArrayList<>();
		FlowAnalysis flowAnalysis = new FlowAnalysis();
		final Collection<Graph> graphCollection = graphBuilder.createProcessGraph(fileScanner, modelInstance,
				processDefinition.getPath(), calledElementHierarchy, scanner, flowAnalysis);

		flowAnalysis.analyze(graphCollection);

		Set<AnomalyContainer> anomalies = new HashSet<>();
		flowAnalysis.getNodes().values().forEach(
				analysisElement -> analysisElement.getAnomalies().forEach((key, value) -> anomalies.addAll(value)));

		Anomaly anomaly = anomalies.iterator().next().getAnomaly();
		assertEquals("Expected 2 anomalies but found " + anomalies.size(), 1, anomalies.size());
		assertEquals("Expected a DD anomaly but found " + anomaly, Anomaly.DD, anomaly);
	}

	@Test
	public void findDU() {
		final ProcessVariablesScanner scanner = new ProcessVariablesScanner(null);
		Properties myProperties = new Properties();
		myProperties.put("scanpath", "src/test/java");
		ConfigConstants.getInstance().setProperties(myProperties);
		final FileScanner fileScanner = new FileScanner(new RuleSet());
		final String PATH = BASE_PATH + "ProcessVariablesModelChecker_AnomalyDU.bpmn";
		final File processDefinition = new File(PATH);

		// parse bpmn model
		final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(processDefinition);

		final ElementGraphBuilder graphBuilder = new ElementGraphBuilder(null, null, null, null, new BpmnScanner(PATH));

		// create data flow graphs
		final Collection<String> calledElementHierarchy = new ArrayList<>();
		FlowAnalysis flowAnalysis = new FlowAnalysis();
		final Collection<Graph> graphCollection = graphBuilder.createProcessGraph(fileScanner, modelInstance,
				processDefinition.getPath(), calledElementHierarchy, scanner, flowAnalysis);

		flowAnalysis.analyze(graphCollection);

		Set<AnomalyContainer> anomalies = new HashSet<>();
		flowAnalysis.getNodes().values().forEach(
				analysisElement -> analysisElement.getAnomalies().forEach((key, value) -> anomalies.addAll(value)));

		Anomaly anomaly = anomalies.iterator().next().getAnomaly();
		assertEquals("Expected 1 anomalie but found " + anomalies.size(), 1, anomalies.size());
		assertEquals("Expected a DU anomaly but found " + anomaly, Anomaly.DU, anomaly);
	}

	@Test
	public void findUR() {
		final ProcessVariablesScanner scanner = new ProcessVariablesScanner(null);
		Properties myProperties = new Properties();
		myProperties.put("scanpath", "src/test/java");
		ConfigConstants.getInstance().setProperties(myProperties);
		final FileScanner fileScanner = new FileScanner(new RuleSet());
		final String PATH = BASE_PATH + "ProcessVariablesModelChecker_AnomalyUR.bpmn";
		final File processDefinition = new File(PATH);

		// parse bpmn model
		final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(processDefinition);

		final ElementGraphBuilder graphBuilder = new ElementGraphBuilder(null, null, null, null, new BpmnScanner(PATH));

		// create data flow graphs
		final Collection<String> calledElementHierarchy = new ArrayList<>();
		FlowAnalysis flowAnalysis = new FlowAnalysis();
		final Collection<Graph> graphCollection = graphBuilder.createProcessGraph(fileScanner, modelInstance,
				processDefinition.getPath(), calledElementHierarchy, scanner, flowAnalysis);

		flowAnalysis.analyze(graphCollection);

		Set<AnomalyContainer> anomalies = new HashSet<>();
		flowAnalysis.getNodes().values().forEach(
				analysisElement -> analysisElement.getAnomalies().forEach((key, value) -> anomalies.addAll(value)));

		Anomaly anomaly = anomalies.iterator().next().getAnomaly();
		assertEquals("Expected 1 anomalie but found " + anomalies.size(), 1, anomalies.size());
		assertEquals("Expected a UR anomaly but found " + anomaly, Anomaly.UR, anomaly);
	}

	@Test
	public void findUU() {
		final ProcessVariablesScanner scanner = new ProcessVariablesScanner(null);
		Properties myProperties = new Properties();
		myProperties.put("scanpath", "src/test/java");
		ConfigConstants.getInstance().setProperties(myProperties);
		final FileScanner fileScanner = new FileScanner(new RuleSet());
		final String PATH = BASE_PATH + "ProcessVariablesModelChecker_AnomalyUU.bpmn";
		final File processDefinition = new File(PATH);

		// parse bpmn model
		final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(processDefinition);

		final ElementGraphBuilder graphBuilder = new ElementGraphBuilder(null, null, null, null, new BpmnScanner(PATH));

		// create data flow graphs
		final Collection<String> calledElementHierarchy = new ArrayList<>();
		FlowAnalysis flowAnalysis = new FlowAnalysis();
		final Collection<Graph> graphCollection = graphBuilder.createProcessGraph(fileScanner, modelInstance,
				processDefinition.getPath(), calledElementHierarchy, scanner, flowAnalysis);

		flowAnalysis.analyze(graphCollection);

		Set<AnomalyContainer> anomalies = new HashSet<>();
		flowAnalysis.getNodes().values().forEach(
				analysisElement -> analysisElement.getAnomalies().forEach((key, value) -> anomalies.addAll(value)));

		Anomaly anomaly1 = anomalies.iterator().next().getAnomaly();
		Anomaly anomaly2 = anomalies.iterator().next().getAnomaly();
		Anomaly anomaly3 = anomalies.iterator().next().getAnomaly();
		// TODO change this test to two anomalies as soon as EU is realized
		assertEquals("Expected 3 anomalies but found " + anomalies.size(), 3, anomalies.size());
		assertEquals("Expected a UU anomaly but found " + anomaly1, Anomaly.UU, anomaly1);
		assertEquals("Expected a UU anomaly but found " + anomaly2, Anomaly.UU, anomaly2);
		assertEquals("Expected a UU anomaly but found " + anomaly3, Anomaly.UU, anomaly3);
	}

}
