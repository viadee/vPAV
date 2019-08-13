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
import de.viadee.bpm.vPAV.RuntimeConfig;
import de.viadee.bpm.vPAV.config.model.Rule;
import de.viadee.bpm.vPAV.config.model.RuleSet;
import de.viadee.bpm.vPAV.constants.ConfigConstants;
import de.viadee.bpm.vPAV.processing.code.flow.BpmnElement;
import de.viadee.bpm.vPAV.processing.code.flow.ControlFlowGraph;
import de.viadee.bpm.vPAV.processing.code.flow.FlowAnalysis;
import de.viadee.bpm.vPAV.processing.model.data.ProcessVariable;
import de.viadee.bpm.vPAV.processing.model.data.ProcessVariableOperation;
import de.viadee.bpm.vPAV.processing.model.data.VariableOperation;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.BaseElement;
import org.camunda.bpm.model.bpmn.instance.CallActivity;
import org.camunda.bpm.model.bpmn.instance.ServiceTask;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static de.viadee.bpm.vPAV.processing.BpmnModelDispatcher.getBpmnElements;
import static de.viadee.bpm.vPAV.processing.BpmnModelDispatcher.getProcessVariables;

public class ProcessVariableReaderTest {

	private static final String BASE_PATH = "src/test/resources/";

	private static ClassLoader cl;

	@BeforeClass
	public static void setup() throws MalformedURLException {
		RuntimeConfig.getInstance().setTest(true);
		final File file = new File(".");
		final String currentPath = file.toURI().toURL().toString();
		final URL classUrl = new URL(currentPath + "src/test/java/");
		final URL resourcesUrl = new URL(currentPath + "src/test/resources/");
		final URL[] classUrls = { classUrl, resourcesUrl };
		cl = new URLClassLoader(classUrls);
		RuntimeConfig.getInstance().setClassLoader(cl);
	}

	@AfterClass
	public static void tearDown() {
		RuntimeConfig.getInstance().setTest(false);
	}

	@Test
	public void testRecogniseVariablesInClass() {
		final String PATH = BASE_PATH + "ProcessVariableReaderTest_RecogniseVariablesInClass.bpmn";
		final FileScanner fileScanner = new FileScanner(new RuleSet());
		fileScanner.setScanPath(ConfigConstants.TEST_JAVAPATH);

		// parse bpmn model
		final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

		final Collection<ServiceTask> allServiceTasks = modelInstance.getModelElementsByType(ServiceTask.class);

		final ProcessVariableReader variableReader = new ProcessVariableReader(null, null, new BpmnScanner(PATH));
		final ControlFlowGraph cg = new ControlFlowGraph();
		final BpmnElement element = new BpmnElement(PATH, allServiceTasks.iterator().next(), cg, new FlowAnalysis());

		final ListMultimap<String, ProcessVariableOperation> variables = ArrayListMultimap.create();
		variables.putAll(variableReader.getVariablesFromElement(fileScanner, element, cg));

		Assert.assertEquals(2, variables.size());
	}

	@Test
	public void testRecogniseInputOutputAssociations() {
		final FileScanner fileScanner = new FileScanner(new RuleSet());
		fileScanner.setScanPath(ConfigConstants.TEST_JAVAPATH);
		final String PATH = BASE_PATH + "ProcessVariableReaderTest_InputOutputCallActivity.bpmn";

		// parse bpmn model
		final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

		final Collection<CallActivity> allServiceTasks = modelInstance.getModelElementsByType(CallActivity.class);

		final ProcessVariableReader variableReader = new ProcessVariableReader(null, null, new BpmnScanner(PATH));
		final ControlFlowGraph cg = new ControlFlowGraph();
		final BpmnElement element = new BpmnElement(PATH, allServiceTasks.iterator().next(), cg, new FlowAnalysis());
		final ListMultimap<String, ProcessVariableOperation> variables = ArrayListMultimap.create();
		variables.putAll(variableReader.getVariablesFromElement(fileScanner, element, cg));

		final List<ProcessVariableOperation> nameOfVariableInMainProcess = variables.get("nameOfVariableInMainProcess");
		Assert.assertNotNull(nameOfVariableInMainProcess);
		Assert.assertEquals(VariableOperation.WRITE, nameOfVariableInMainProcess.get(0).getOperation());

		final List<ProcessVariableOperation> nameOfVariableInMainProcess2 = variables
				.get("nameOfVariableInMainProcess2");
		Assert.assertNotNull(nameOfVariableInMainProcess2);
		Assert.assertEquals(VariableOperation.WRITE, nameOfVariableInMainProcess2.get(0).getOperation());

		final List<ProcessVariableOperation> someVariableInMainProcess = variables.get("someVariableInMainProcess");
		Assert.assertNotNull(someVariableInMainProcess);
		Assert.assertEquals(VariableOperation.READ, someVariableInMainProcess.get(0).getOperation());

		final List<ProcessVariableOperation> someVariableInMainProcess2 = variables.get("someVariableInMainProcess2");
		Assert.assertNotNull(someVariableInMainProcess2);
		Assert.assertEquals(VariableOperation.READ, someVariableInMainProcess2.get(0).getOperation());
	}

	@Test
	public void testRecogniseSignals() {
		final String PATH = BASE_PATH + "ProcessVariablesReader_SignalVariables.bpmn";
		final File processDefinition = new File(PATH);
		final FileScanner fileScanner = new FileScanner(new RuleSet());
		fileScanner.setScanPath(ConfigConstants.TEST_JAVAPATH);
		final ProcessVariablesScanner scanner = new ProcessVariablesScanner(
				fileScanner.getJavaResourcesFileInputStream());

		// parse bpmn model
		final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(processDefinition);

		final Collection<BaseElement> baseElements = modelInstance.getModelElementsByType(BaseElement.class);
		final Rule rule = new Rule("ProcessVariablesModelChecker", true, null, null, null, null);

		final ElementGraphBuilder graphBuilder = new ElementGraphBuilder(new BpmnScanner(PATH), rule);
		// create data flow graphs
		graphBuilder.createProcessGraph(fileScanner, modelInstance, processDefinition.getPath(), new ArrayList<>(),
				scanner, new FlowAnalysis());

		final Collection<BpmnElement> bpmnElements = getBpmnElements(processDefinition, baseElements, graphBuilder,
				new FlowAnalysis());
		final Collection<ProcessVariable> processVariables = getProcessVariables(bpmnElements);

		Assert.assertEquals(2, processVariables.size());
	}

	@Test
	public void testRecogniseMessages() {
		final String PATH = BASE_PATH + "ProcessVariablesReader_MessageVariables.bpmn";
		final File processDefinition = new File(PATH);
		final FileScanner fileScanner = new FileScanner(new RuleSet());
		fileScanner.setScanPath(ConfigConstants.TEST_JAVAPATH);
		final ProcessVariablesScanner scanner = new ProcessVariablesScanner(
				fileScanner.getJavaResourcesFileInputStream());

		// parse bpmn model
		final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(processDefinition);

		final Collection<BaseElement> baseElements = modelInstance.getModelElementsByType(BaseElement.class);
		final Rule rule = new Rule("ProcessVariablesModelChecker", true, null, null, null, null);

		final ElementGraphBuilder graphBuilder = new ElementGraphBuilder(new BpmnScanner(PATH), rule);
		// create data flow graphs
		graphBuilder.createProcessGraph(fileScanner, modelInstance, processDefinition.getPath(), new ArrayList<>(),
				scanner, new FlowAnalysis());

		final Collection<BpmnElement> bpmnElements = getBpmnElements(processDefinition, baseElements, graphBuilder,
				new FlowAnalysis());
		final Collection<ProcessVariable> processVariables = getProcessVariables(bpmnElements);

		Assert.assertEquals(1, processVariables.size());
	}

	@Test
	public void testRecogniseLinks() {
		final String PATH = BASE_PATH + "ProcessVariablesReader_LinkVariables.bpmn";
		final File processDefinition = new File(PATH);
		final FileScanner fileScanner = new FileScanner(new RuleSet());
		fileScanner.setScanPath(ConfigConstants.TEST_JAVAPATH);
		final ProcessVariablesScanner scanner = new ProcessVariablesScanner(
				fileScanner.getJavaResourcesFileInputStream());

		// parse bpmn model
		final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(processDefinition);

		final Collection<BaseElement> baseElements = modelInstance.getModelElementsByType(BaseElement.class);
		final Rule rule = new Rule("ProcessVariablesModelChecker", true, null, null, null, null);

		final ElementGraphBuilder graphBuilder = new ElementGraphBuilder(new BpmnScanner(PATH), rule);
		// create data flow graphs
		graphBuilder.createProcessGraph(fileScanner, modelInstance, processDefinition.getPath(), new ArrayList<>(),
				scanner, new FlowAnalysis());

		final Collection<BpmnElement> bpmnElements = getBpmnElements(processDefinition, baseElements, graphBuilder,
				new FlowAnalysis());
		final Collection<ProcessVariable> processVariables = getProcessVariables(bpmnElements);

		Assert.assertEquals(2, processVariables.size());
	}
}
