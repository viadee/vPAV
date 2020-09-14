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
package de.viadee.bpm.vPAV.processing.model.graph;

import de.viadee.bpm.vPAV.FileScanner;
import de.viadee.bpm.vPAV.RuntimeConfig;
import de.viadee.bpm.vPAV.config.model.RuleSet;
import de.viadee.bpm.vPAV.constants.ConfigConstants;
import de.viadee.bpm.vPAV.processing.ElementGraphBuilder;
import de.viadee.bpm.vPAV.processing.EntryPointScanner;
import de.viadee.bpm.vPAV.processing.code.flow.FlowAnalysis;
import de.viadee.bpm.vPAV.processing.model.data.AnomalyContainer;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.bpmn.instance.*;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

public class ComplexModelTest {

	@BeforeClass
	public static void setup() throws MalformedURLException {
		RuntimeConfig.getInstance().setTest(true);
		final URL classUrl = new URL(new File(ConfigConstants.JAVA_PATH_TEST).toURI().toURL().toString());
		final URL resourcesUrl = new URL(new File(ConfigConstants.BASE_PATH_TEST).toURI().toURL().toString());
		final URL[] classUrls = { classUrl, resourcesUrl };
		ClassLoader cl = new URLClassLoader(classUrls);
		RuntimeConfig.getInstance().setClassLoader(cl);
	}

	/**
	 * Case: Check complex model for invalid paths
	 * <p>
	 * Included: * sub processes * boundary events * java delegate * spring bean *
	 * DMN model
	 */
	@Test
	public void testGraphOnComplexModel() {
		final EntryPointScanner scanner = new EntryPointScanner(null);
		final FileScanner fileScanner = new FileScanner(new RuleSet());
		final File processdefinition = new File(
				ConfigConstants.BASE_PATH_TEST + "ComplexModelTest_GraphOnComplexModel.bpmn");

		// parse bpmn model
		final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(processdefinition);

		// create many gateway paths to increase the complexity
		createGatewayPaths("ExclusiveGateway_0mkf3hf", "ExclusiveGateway_00pfwgg", modelInstance, 1);
		// createGatewayPaths("ExclusiveGateway_10gsr88", "ExclusiveGateway_13qew7s",
		// modelInstance,
		// 500);

		final Map<String, String> decisionRefToPathMap = new HashMap<>();
		decisionRefToPathMap.put("decision", "table.dmn");

		final Map<String, String> beanMappings = new HashMap<>();
		beanMappings.put("springBean", "de.viadee.bpm.vPAV.delegates.TestDelegate");

		long startTime = System.currentTimeMillis();

		final ElementGraphBuilder graphBuilder = new ElementGraphBuilder(decisionRefToPathMap, null, null, null);
		// create data flow graphs
		final Collection<Graph> graphCollection = graphBuilder.createProcessGraph(fileScanner, modelInstance,
				processdefinition.getPath(), new ArrayList<>(), scanner, new FlowAnalysis());

		long estimatedTime = System.currentTimeMillis() - startTime;
		System.out.println("Graph creation: " + estimatedTime + "ms");
		long startTime2 = System.currentTimeMillis();

		// calculate invalid paths based on data flow graphs
		@SuppressWarnings("unused")
		final Map<AnomalyContainer, List<Path>> invalidPathMap = graphBuilder.createInvalidPaths(graphCollection);

		long estimatedTime2 = System.currentTimeMillis() - startTime2;
		System.out.println("Graph search: " + estimatedTime2 + "ms");
	}

	/**
	 * Create paths between two gateways
	 *
	 * @param gateway1_id
	 * @param gateway2_id
	 * @param modelInstance
	 * @param count
	 */
	private void createGatewayPaths(final String gateway1_id, final String gateway2_id,
			final BpmnModelInstance modelInstance, final int count) {

		final ModelElementInstance element_von = modelInstance.getModelElementById(gateway1_id);
		final ModelElementInstance element_zu = modelInstance.getModelElementById(gateway2_id);

		for (int i = 1; i < count + 1; i++) {
			// 1) create task
			final Collection<Process> processes = modelInstance.getModelElementsByType(Process.class);
			final FlowNode task = createElement(modelInstance, processes.iterator().next(), "task" + i, Task.class);
			// 2) connect with sequence flows
			createSequenceFlow(modelInstance, processes.iterator().next(), (FlowNode) element_von, task);
			createSequenceFlow(modelInstance, processes.iterator().next(), task, (FlowNode) element_zu);
		}
	}

	/**
	 * create an bpmn element
	 *
	 * @param parentElement
	 * @param id
	 * @param elementClass
	 * @return
	 */
	private <T extends BpmnModelElementInstance> T createElement(final BpmnModelInstance modelInstance,
			BpmnModelElementInstance parentElement, String id, Class<T> elementClass) {
		T element = modelInstance.newInstance(elementClass);
		element.setAttributeValue("id", id, true);
		parentElement.addChildElement(element);
		return element;
	}

	/**
	 * create a sequence flow
	 *
	 * @param process
	 * @param from
	 * @param to
	 * @return
	 */
	private SequenceFlow createSequenceFlow(final BpmnModelInstance modelInstance, Process process, FlowNode from,
			FlowNode to) {
		SequenceFlow sequenceFlow = createElement(modelInstance, process, from.getId() + "-" + to.getId(),
				SequenceFlow.class);
		process.addChildElement(sequenceFlow);
		sequenceFlow.setSource(from);
		from.getOutgoing().add(sequenceFlow);
		sequenceFlow.setTarget(to);
		to.getIncoming().add(sequenceFlow);
		return sequenceFlow;
	}

	@AfterClass
	public static void tearDown() {
		RuntimeConfig.getInstance().setTest(false);
	}

}
