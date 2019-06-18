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

import de.viadee.bpm.vPAV.BpmnScanner;
import de.viadee.bpm.vPAV.FileScanner;
import de.viadee.bpm.vPAV.RuntimeConfig;
import de.viadee.bpm.vPAV.config.model.Rule;
import de.viadee.bpm.vPAV.config.model.RuleSet;
import de.viadee.bpm.vPAV.constants.ConfigConstants;
import de.viadee.bpm.vPAV.processing.code.flow.BpmnElement;
import de.viadee.bpm.vPAV.processing.code.flow.FlowAnalysis;
import de.viadee.bpm.vPAV.processing.model.data.ProcessVariable;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.BaseElement;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

import static de.viadee.bpm.vPAV.processing.BpmnModelDispatcher.getBpmnElements;
import static de.viadee.bpm.vPAV.processing.BpmnModelDispatcher.getProcessVariables;

public class ProcessVariableMappingTest {

	private static final String BASE_PATH = "src/test/resources/";

	@BeforeClass
	public static void setup() throws MalformedURLException {
		final File file = new File(".");
		final String currentPath = file.toURI().toURL().toString();
		final URL classUrl = new URL(currentPath + "src/test/java");
		final URL[] classUrls = { classUrl };
		ClassLoader cl = new URLClassLoader(classUrls);
		RuntimeConfig.getInstance().setClassLoader(cl);
		RuntimeConfig.getInstance().getResource("en_US");
		RuntimeConfig.getInstance().setTest(true);
		final Map<String, String> beanMapping = new HashMap<>();
		beanMapping.put("mappingDelegate", "de/viadee/bpm/vPAV/delegates/MappingDelegate.java");
		RuntimeConfig.getInstance().setBeanMapping(beanMapping);
	}

	/**
	 * Case: Test a model with Input Mapping of type text code
	 */
	@Test
	public void testInputTypeText() throws IOException, SAXException, ParserConfigurationException {

		final String PATH = BASE_PATH + "ProcessVariablesMapping_InputText.bpmn";
		final File processDefinition = new File(PATH);
		final FileScanner fileScanner = new FileScanner(new RuleSet());
		fileScanner.setScanPath(ConfigConstants.TEST_JAVAPATH);
		final Set<String> testSet = new HashSet<>();
		testSet.add("de/viadee/bpm/vPAV/delegates/MappingDelegate.java");
		fileScanner.setJavaResourcesFileInputStream(testSet);

		final ProcessVariablesScanner scanner = new ProcessVariablesScanner(
				fileScanner.getJavaResourcesFileInputStream());

		scanner.scanProcessVariables();

		// parse bpmn model
		final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(processDefinition);

		final Collection<BaseElement> baseElements = modelInstance.getModelElementsByType(BaseElement.class);

		final ElementGraphBuilder graphBuilder = new ElementGraphBuilder(new BpmnScanner(PATH));
		// create data flow graphs
		graphBuilder.createProcessGraph(fileScanner, modelInstance, processDefinition.getPath(), new ArrayList<>(),
				scanner, new FlowAnalysis());

		final Collection<BpmnElement> bpmnElements = getBpmnElements(processDefinition, baseElements, graphBuilder,
				new FlowAnalysis());
		final Collection<ProcessVariable> processVariables = getProcessVariables(bpmnElements);

		Assert.assertEquals(2, processVariables.size());
	}

	/**
	 * Case: Test a model with Input Mapping of type list code
	 */
	@Test
	public void testInputTypeList() throws IOException, SAXException, ParserConfigurationException {

		final String PATH = BASE_PATH + "ProcessVariablesMapping_InputList.bpmn";
		final File processDefinition = new File(PATH);
		final FileScanner fileScanner = new FileScanner(new RuleSet());
		fileScanner.setScanPath(ConfigConstants.TEST_JAVAPATH);
		final Set<String> testSet = new HashSet<>();
		testSet.add("de/viadee/bpm/vPAV/delegates/MappingDelegate.java");
		fileScanner.setJavaResourcesFileInputStream(testSet);

		final ProcessVariablesScanner scanner = new ProcessVariablesScanner(
				fileScanner.getJavaResourcesFileInputStream());

		scanner.scanProcessVariables();

		// parse bpmn model
		final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(processDefinition);

		final Collection<BaseElement> baseElements = modelInstance.getModelElementsByType(BaseElement.class);

		final ElementGraphBuilder graphBuilder = new ElementGraphBuilder(new BpmnScanner(PATH));
		// create data flow graphs
		graphBuilder.createProcessGraph(fileScanner, modelInstance, processDefinition.getPath(), new ArrayList<>(),
				scanner, new FlowAnalysis());

		final Collection<BpmnElement> bpmnElements = getBpmnElements(processDefinition, baseElements, graphBuilder,
				new FlowAnalysis());
		final Collection<ProcessVariable> processVariables = getProcessVariables(bpmnElements);

		Assert.assertEquals(3, processVariables.size());
	}

	/**
	 * Case: Test a model with Input Mapping of type map code
	 */
	@Test
	public void testInputTypeMap() throws IOException, SAXException, ParserConfigurationException {

		final String PATH = BASE_PATH + "ProcessVariablesMapping_InputMap.bpmn";
		final File processDefinition = new File(PATH);
		final FileScanner fileScanner = new FileScanner(new RuleSet());
		fileScanner.setScanPath(ConfigConstants.TEST_JAVAPATH);
		final Set<String> testSet = new HashSet<>();
		testSet.add("de/viadee/bpm/vPAV/delegates/MappingDelegate.java");
		fileScanner.setJavaResourcesFileInputStream(testSet);

		final ProcessVariablesScanner scanner = new ProcessVariablesScanner(
				fileScanner.getJavaResourcesFileInputStream());

		scanner.scanProcessVariables();

		// parse bpmn model
		final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(processDefinition);

		final Collection<BaseElement> baseElements = modelInstance.getModelElementsByType(BaseElement.class);

		final ElementGraphBuilder graphBuilder = new ElementGraphBuilder(new BpmnScanner(PATH));
		// create data flow graphs
		graphBuilder.createProcessGraph(fileScanner, modelInstance, processDefinition.getPath(), new ArrayList<>(),
				scanner, new FlowAnalysis());

		final Collection<BpmnElement> bpmnElements = getBpmnElements(processDefinition, baseElements, graphBuilder,
				new FlowAnalysis());
		final Collection<ProcessVariable> processVariables = getProcessVariables(bpmnElements);

		Assert.assertEquals(3, processVariables.size());
	}

	/**
	 * Case: Test a model with Output Mapping of type text code
	 */
	@Test
	public void testOutputTypeText() throws IOException, SAXException, ParserConfigurationException {

		final String PATH = BASE_PATH + "ProcessVariablesMapping_OutputText.bpmn";
		final File processDefinition = new File(PATH);
		final FileScanner fileScanner = new FileScanner(new RuleSet());
		fileScanner.setScanPath(ConfigConstants.TEST_JAVAPATH);
		final Set<String> testSet = new HashSet<>();
		testSet.add("de/viadee/bpm/vPAV/delegates/MappingDelegate.java");
		fileScanner.setJavaResourcesFileInputStream(testSet);

		final ProcessVariablesScanner scanner = new ProcessVariablesScanner(
				fileScanner.getJavaResourcesFileInputStream());

		scanner.scanProcessVariables();

		// parse bpmn model
		final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(processDefinition);

		final Collection<BaseElement> baseElements = modelInstance.getModelElementsByType(BaseElement.class);

		final ElementGraphBuilder graphBuilder = new ElementGraphBuilder(new BpmnScanner(PATH));
		// create data flow graphs
		graphBuilder.createProcessGraph(fileScanner, modelInstance, processDefinition.getPath(), new ArrayList<>(),
				scanner, new FlowAnalysis());

		final Collection<BpmnElement> bpmnElements = getBpmnElements(processDefinition, baseElements, graphBuilder,
				new FlowAnalysis());
		final Collection<ProcessVariable> processVariables = getProcessVariables(bpmnElements);

		Assert.assertEquals(2, processVariables.size());
	}

	/**
	 * Case: Test a model with Output Mapping of type list code
	 */
	@Test
	public void testOutputTypeList() throws IOException, SAXException, ParserConfigurationException {

		final String PATH = BASE_PATH + "ProcessVariablesMapping_OutputList.bpmn";
		final File processDefinition = new File(PATH);
		final FileScanner fileScanner = new FileScanner(new RuleSet());
		fileScanner.setScanPath(ConfigConstants.TEST_JAVAPATH);
		final Set<String> testSet = new HashSet<>();
		testSet.add("de/viadee/bpm/vPAV/delegates/MappingDelegate.java");
		fileScanner.setJavaResourcesFileInputStream(testSet);

		final ProcessVariablesScanner scanner = new ProcessVariablesScanner(
				fileScanner.getJavaResourcesFileInputStream());

		scanner.scanProcessVariables();
		// parse bpmn model
		final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(processDefinition);

		final Collection<BaseElement> baseElements = modelInstance.getModelElementsByType(BaseElement.class);

		final ElementGraphBuilder graphBuilder = new ElementGraphBuilder(new BpmnScanner(PATH));
		// create data flow graphs
		graphBuilder.createProcessGraph(fileScanner, modelInstance, processDefinition.getPath(), new ArrayList<>(),
				scanner, new FlowAnalysis());

		final Collection<BpmnElement> bpmnElements = getBpmnElements(processDefinition, baseElements, graphBuilder,
				new FlowAnalysis());
		final Collection<ProcessVariable> processVariables = getProcessVariables(bpmnElements);

		Assert.assertEquals(3, processVariables.size());
	}

	/**
	 * Case: Test a model with Output Mapping of type map code
	 */
	@Test
	public void testOutputTypeMap() throws IOException, SAXException, ParserConfigurationException {

		final String PATH = BASE_PATH + "ProcessVariablesMapping_OutputMap.bpmn";
		final File processDefinition = new File(PATH);
		final FileScanner fileScanner = new FileScanner(new RuleSet());
		fileScanner.setScanPath(ConfigConstants.TEST_JAVAPATH);
		final Set<String> testSet = new HashSet<>();
		testSet.add("de/viadee/bpm/vPAV/delegates/MappingDelegate.java");
		fileScanner.setJavaResourcesFileInputStream(testSet);

		final ProcessVariablesScanner scanner = new ProcessVariablesScanner(
				fileScanner.getJavaResourcesFileInputStream());

		scanner.scanProcessVariables();

		// parse bpmn model
		final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(processDefinition);

		final Collection<BaseElement> baseElements = modelInstance.getModelElementsByType(BaseElement.class);

		final ElementGraphBuilder graphBuilder = new ElementGraphBuilder(new BpmnScanner(PATH));
		// create data flow graphs
		graphBuilder.createProcessGraph(fileScanner, modelInstance, processDefinition.getPath(), new ArrayList<>(),
				scanner, new FlowAnalysis());

		final Collection<BpmnElement> bpmnElements = getBpmnElements(processDefinition, baseElements, graphBuilder,
				new FlowAnalysis());
		final Collection<ProcessVariable> processVariables = getProcessVariables(bpmnElements);

		Assert.assertEquals(3, processVariables.size());
	}

	/**
	 * Case: Test a model with Input Mapping of type text code
	 */
	@Test
	public void testInputTypeScript() throws IOException, SAXException, ParserConfigurationException {

		final String PATH = BASE_PATH + "ProcessVariablesMapping_InputScript.bpmn";
		final File processDefinition = new File(PATH);
		final FileScanner fileScanner = new FileScanner(new RuleSet());
		fileScanner.setScanPath(ConfigConstants.TEST_JAVAPATH);
		final Set<String> testSet = new HashSet<>();
		testSet.add("de/viadee/bpm/vPAV/delegates/MappingDelegate.java");
		fileScanner.setJavaResourcesFileInputStream(testSet);

		final ProcessVariablesScanner scanner = new ProcessVariablesScanner(
				fileScanner.getJavaResourcesFileInputStream());

		scanner.scanProcessVariables();

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

}