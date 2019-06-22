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
package de.viadee.bpm.vPAV.processing.checker;

import de.viadee.bpm.vPAV.BpmnScanner;
import de.viadee.bpm.vPAV.FileScanner;
import de.viadee.bpm.vPAV.RuntimeConfig;
import de.viadee.bpm.vPAV.config.model.Rule;
import de.viadee.bpm.vPAV.config.model.RuleSet;
import de.viadee.bpm.vPAV.processing.ProcessVariablesScanner;
import de.viadee.bpm.vPAV.processing.code.flow.BpmnElement;
import de.viadee.bpm.vPAV.processing.code.flow.ControlFlowGraph;
import de.viadee.bpm.vPAV.processing.code.flow.FlowAnalysis;
import de.viadee.bpm.vPAV.processing.model.data.CheckerIssue;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class MessageCorrelationCheckerTest {

	private static final String BASE_PATH = "src/test/resources/";

	private static MessageCorrelationChecker checker;

	private static Rule rule;

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
		rule = createRule();
	}

	/**
	 * Case: Test a model with correct message inside model (start event) + source
	 * code
	 */
	@Test
	public void testCorrectMessageStartEvent() throws IOException, SAXException, ParserConfigurationException {

		final String PATH = BASE_PATH + "MessageCorrelationChecker_correctMessageStartEvent.bpmn";

		final FileScanner fileScanner = new FileScanner(new RuleSet());
		final Set<String> testSet = new HashSet<String>();
		testSet.add("de/viadee/bpm/vPAV/delegates/MessageCorrelationDelegate.java");
		fileScanner.setJavaResourcesFileInputStream(testSet);

		final ProcessVariablesScanner scanner = new ProcessVariablesScanner(
				fileScanner.getJavaResourcesFileInputStream());

		scanner.scanProcessVariables();

		checker = new MessageCorrelationChecker(rule, new BpmnScanner(PATH), scanner);

		// parse bpmn model
		final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

		final Collection<BaseElement> baseElements = modelInstance.getModelElementsByType(BaseElement.class);

		final Collection<CheckerIssue> issues = new ArrayList<>();

		for (BaseElement baseElement : baseElements) {
			final BpmnElement element = new BpmnElement(PATH, baseElement, new ControlFlowGraph(), new FlowAnalysis());
			issues.addAll(checker.check(element));
		}

		if (issues.size() > 0) {
			Assert.fail("Correct message was not identified for start event");
		}
	}

	/**
	 * Case: Test a model with correct message inside model (receive task) + source
	 * code
	 */
	@Test
	public void testCorrectMessageReceiveTask() throws IOException, SAXException, ParserConfigurationException {
		final String PATH = BASE_PATH + "MessageCorrelationChecker_correctMessageReceiveTask.bpmn";

		final FileScanner fileScanner = new FileScanner(new RuleSet());
		final Set<String> testSet = new HashSet<String>();
		testSet.add("de/viadee/bpm/vPAV/delegates/MessageCorrelationDelegate2.java");
		fileScanner.setJavaResourcesFileInputStream(testSet);

		final ProcessVariablesScanner scanner = new ProcessVariablesScanner(
				fileScanner.getJavaResourcesFileInputStream());

		scanner.scanProcessVariables();

		checker = new MessageCorrelationChecker(rule, new BpmnScanner(PATH), scanner);

		// parse bpmn model
		final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

		final Collection<BaseElement> baseElements = modelInstance.getModelElementsByType(BaseElement.class);

		final Collection<CheckerIssue> issues = new ArrayList<>();

		for (BaseElement baseElement : baseElements) {
			final BpmnElement element = new BpmnElement(PATH, baseElement, new ControlFlowGraph(), new FlowAnalysis());
			issues.addAll(checker.check(element));
		}

		if (issues.size() > 0) {
			Assert.fail("Correct message was not identified for receive task");
		}
	}

	/**
	 * Case: Test a model with correct messages inside model (start event + receive
	 * task) + source code
	 */
	@Test
	public void testAllCorrectMessages() throws IOException, SAXException, ParserConfigurationException {
		final String PATH = BASE_PATH + "MessageCorrelationChecker_correctMessages.bpmn";

		final FileScanner fileScanner = new FileScanner(new RuleSet());
		final Set<String> testSet = new HashSet<>();
		testSet.add("de/viadee/bpm/vPAV/delegates/MessageCorrelationDelegate3.java");
		fileScanner.setJavaResourcesFileInputStream(testSet);

		final ProcessVariablesScanner scanner = new ProcessVariablesScanner(
				fileScanner.getJavaResourcesFileInputStream());

		scanner.scanProcessVariables();

		checker = new MessageCorrelationChecker(rule, new BpmnScanner(PATH), scanner);

		// parse bpmn model
		final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

		final Collection<BaseElement> baseElements = modelInstance.getModelElementsByType(BaseElement.class);

		final Collection<CheckerIssue> issues = new ArrayList<>();

		for (BaseElement baseElement : baseElements) {
			final BpmnElement element = new BpmnElement(PATH, baseElement, new ControlFlowGraph(), new FlowAnalysis());
			issues.addAll(checker.check(element));
		}

		if (issues.size() > 0) {
			Assert.fail("Correct messages were not identified");
		}
	}

	/**
	 * Case: Test a model with incorrect message inside model (start event + receive
	 * task)
	 */
	@Test
	public void testIncorrectMessages() throws IOException, SAXException, ParserConfigurationException {
		final String PATH = BASE_PATH + "MessageCorrelationChecker_incorrectMessage.bpmn";

		final FileScanner fileScanner = new FileScanner(new RuleSet());
		final Set<String> testSet = new HashSet<>();
		testSet.add("de/viadee/bpm/vPAV/delegates/MessageCorrelationDelegate4.java");
		fileScanner.setJavaResourcesFileInputStream(testSet);

		final ProcessVariablesScanner scanner = new ProcessVariablesScanner(
				fileScanner.getJavaResourcesFileInputStream());

		scanner.scanProcessVariables();

		checker = new MessageCorrelationChecker(rule, new BpmnScanner(PATH), scanner);

		// parse bpmn model
		final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

		final Collection<BaseElement> baseElements = modelInstance.getModelElementsByType(BaseElement.class);

		final Collection<CheckerIssue> issues = new ArrayList<>();

		for (BaseElement baseElement : baseElements) {
			final BpmnElement element = new BpmnElement(PATH, baseElement, new ControlFlowGraph(), new FlowAnalysis());
			issues.addAll(checker.check(element));
		}

		if (issues.size() != 1) {
			Assert.fail("Incorrect message was not identified");
		}
	}

	private static Rule createRule() {
		return new Rule("MessageChecker", true, "Checks for correct resolving of messages", null, null, null);
	}
}
