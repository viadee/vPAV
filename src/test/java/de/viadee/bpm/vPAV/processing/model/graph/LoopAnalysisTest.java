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
package de.viadee.bpm.vPAV.processing.model.graph;

import de.viadee.bpm.vPAV.BpmnScanner;
import de.viadee.bpm.vPAV.FileScanner;
import de.viadee.bpm.vPAV.RuntimeConfig;
import de.viadee.bpm.vPAV.config.model.RuleSet;
import de.viadee.bpm.vPAV.processing.ElementGraphBuilder;
import de.viadee.bpm.vPAV.processing.ProcessVariablesScanner;
import de.viadee.bpm.vPAV.processing.code.flow.FlowAnalysis;
import de.viadee.bpm.vPAV.processing.model.data.Anomaly;
import de.viadee.bpm.vPAV.processing.model.data.AnomalyContainer;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

public class LoopAnalysisTest {

	private static final String BASE_PATH = "src/test/resources/";

	private static ClassLoader cl;

	@BeforeClass
	public static void setup() throws MalformedURLException {
		final File file = new File(".");
		final String currentPath = file.toURI().toURL().toString();
		final URL classUrl = new URL(currentPath + "src/test/java");
		final URL[] classUrls = { classUrl };
		cl = new URLClassLoader(classUrls);
		RuntimeConfig.getInstance().setClassLoader(cl);
		RuntimeConfig.getInstance().setTest(true);
	}

	/**
	 * Case: Data flow graph creation and calculation of invalid paths
	 *
	 */
	@Ignore
	@Test
	public void testLoop() {
		final ProcessVariablesScanner scanner = new ProcessVariablesScanner(null);
		final FileScanner fileScanner = new FileScanner(new RuleSet());
		final String PATH = BASE_PATH + "LoopAnalysisTest_TestLoop.bpmn";
		final File processDefinition = new File(PATH);

		// parse bpmn model
		final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(processDefinition);

		final ElementGraphBuilder graphBuilder = new ElementGraphBuilder(new BpmnScanner(PATH));
		// create data flow graphs

		FlowAnalysis flowAnalysis = new FlowAnalysis();
		final Collection<Graph> graphCollection = graphBuilder.createProcessGraph(fileScanner, modelInstance,
				processDefinition.getPath(), new ArrayList<>(), scanner, flowAnalysis);

		flowAnalysis.analyze(graphCollection);

		// calculate invalid paths based on data flow graphs
		final Map<AnomalyContainer, List<Path>> invalidPathMap = graphBuilder.createInvalidPaths(graphCollection);

		// get invalid paths
		final List<Path> varTest = invalidPathMap
				.get(new AnomalyContainer("dd", Anomaly.DD, "ServiceTask_1ev9i13", "", null));

		final Iterator<Path> iterator = varTest.iterator();
		final Path firstPath = iterator.next();
		final Path secondPath = iterator.next();
		Assert.assertEquals(
				"[StartEvent_1, SequenceFlow_0uop1ch, ExclusiveGateway_0utydka, SequenceFlow_0g3rb21, ServiceTask_1ev9i13]",
				firstPath.toString());
		Assert.assertEquals(
				"[ServiceTask_1ev9i13, SequenceFlow_0s4fyqh, ExclusiveGateway_1vhe4nv, SequenceFlow_12tyqqh, ExclusiveGateway_0utydka, SequenceFlow_0g3rb21, ServiceTask_1ev9i13]",
				secondPath.toString());
	}
}
