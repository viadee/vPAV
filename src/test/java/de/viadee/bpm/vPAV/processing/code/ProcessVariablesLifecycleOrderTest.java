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
import de.viadee.bpm.vPAV.processing.code.flow.AnalysisElement;
import de.viadee.bpm.vPAV.processing.code.flow.FlowAnalysis;
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

public class ProcessVariablesLifecycleOrderTest {

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
	public void testProcessVariablesLifecycle() {
		// Test with Input/Output Parameters and Start/End Listeners
		// TODO add all other things linke links, signals, messages, ...

		final ProcessVariablesScanner scanner = new ProcessVariablesScanner(null);
		Properties myProperties = new Properties();
		myProperties.put("scanpath", "src/test/java");
		ConfigConstants.getInstance().setProperties(myProperties);
		final FileScanner fileScanner = new FileScanner(new RuleSet());
		final String PATH = BASE_PATH + "ProcessVariablesLifecycleOrderTest.bpmn";
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

		LinkedHashMap<String, AnalysisElement> nodes = flowAnalysis.getNodes();
		// Start from end event and go to start.
		AnalysisElement endEvent = nodes.get("MyEndEvent");
		AnalysisElement sequenceFlow1 = endEvent.getPredecessors().get(0);
		assertEquals("Second Sequence Flow should have two input parameters because the service task has two output parameters.", 1, sequenceFlow1.getInUnused().size());

		AnalysisElement endListener = sequenceFlow1.getPredecessors().get(0);
		assertEquals("End Listener was not correctly included.", "MyServiceTask__1", endListener.getId());

		AnalysisElement startListener = endListener.getPredecessors().get(0);
		assertEquals("Start Listener was not correctly included.", "MyServiceTask__0", startListener.getId());
		assertEquals("Start Listener should have two input parameters.", 1, startListener.getInUnused().size());

		AnalysisElement sequenceFlow0 = startListener.getPredecessors().get(0);
		AnalysisElement startEvent = sequenceFlow0.getPredecessors().get(0);
		assertEquals("Start event was not reached.", "MyStartEvent", startEvent.getId());
		assertEquals("Start event should not have any predecessors.", 0,startEvent.getPredecessors().size());
	}

	@Test
	public void testProcessVariablesLifecycleWithCallActivity() {
		// Test with In/Out Variable Injection, Input/Output Parameters and Start/End Listeners
		// TODO add all other things linke links, signals, messages, ... Delegate Variable Mapping

		final ProcessVariablesScanner scanner = new ProcessVariablesScanner(null);
		Properties myProperties = new Properties();
		myProperties.put("scanpath", "src/test/java");
		ConfigConstants.getInstance().setProperties(myProperties);
		final FileScanner fileScanner = new FileScanner(new RuleSet());
		final String PATH = BASE_PATH + "ProcessVariablesLifecycleOrderTest_WithCallActivity.bpmn";
		final File processDefinition = new File(PATH);

		// parse bpmn model
		final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(processDefinition);

		// add reference for called process
		final Map<String, String> processIdToPathMap = new HashMap<>();
		processIdToPathMap.put("calledProcess", "ProcessVariablesLifecycleOrderTest_CalledProcess.bpmn");

		final ElementGraphBuilder graphBuilder = new ElementGraphBuilder(null, processIdToPathMap, null, null, new BpmnScanner(PATH));

		// create data flow graphs
		final Collection<String> calledElementHierarchy = new ArrayList<>();
		FlowAnalysis flowAnalysis = new FlowAnalysis();
		final Collection<Graph> graphCollection = graphBuilder.createProcessGraph(fileScanner, modelInstance,
				processDefinition.getPath(), calledElementHierarchy, scanner, flowAnalysis);

		flowAnalysis.analyze(graphCollection);

		LinkedHashMap<String, AnalysisElement> nodes = flowAnalysis.getNodes();
		// Start from end event and go to start.
		AnalysisElement endEvent = nodes.get("MyEndEvent");
		AnalysisElement sequenceFlow1 = endEvent.getPredecessors().get(0);
		// TODO this is three because the camunda:in variable is also passed
		assertEquals("Second Sequence Flow should have two input parameters because the service task has two output parameters.", 2, sequenceFlow1.getInUnused().size());

		AnalysisElement endListener = sequenceFlow1.getPredecessors().get(0);
		assertEquals("End Listener was not correctly included.", "MyCallActivity__1", endListener.getId());

		AnalysisElement endEventCalledProcess = endListener.getPredecessors().get(0);
		assertEquals("_EndEvent_SUCC", endEventCalledProcess.getId());

		AnalysisElement serviceTaskCalledProcess = endEventCalledProcess.getPredecessors().get(0).getPredecessors().get(0);
		assertEquals("_MyCalledServiceTask", serviceTaskCalledProcess.getId());

		AnalysisElement startEventCalledProcess = serviceTaskCalledProcess.getPredecessors().get(0).getPredecessors().get(0);
		assertEquals("_StartEvent_1", startEventCalledProcess.getId());
		assertEquals( "Start Event in Subprocess should have two input parameters.", 2, startEventCalledProcess.getInUnused().size());

		AnalysisElement startListener = startEventCalledProcess.getPredecessors().get(0);
		assertEquals("Start Listener was not correctly included.", "MyCallActivity__0", startListener.getId());
		assertEquals("Start Listener should have two input parameters.", 2, startListener.getInUnused().size());

		AnalysisElement sequenceFlow0 = startListener.getPredecessors().get(0);
		AnalysisElement startEvent = sequenceFlow0.getPredecessors().get(0);
		assertEquals("Start event was not reached.", "MyStartEvent", startEvent.getId());
		assertEquals("Start event should not have any predecessors.", 0,startEvent.getPredecessors().size());
	}
}
