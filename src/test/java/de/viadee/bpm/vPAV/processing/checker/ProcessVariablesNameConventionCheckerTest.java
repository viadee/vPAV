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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import de.viadee.bpm.vPAV.BpmnScanner;
import de.viadee.bpm.vPAV.FileScanner;
import de.viadee.bpm.vPAV.RuntimeConfig;
import de.viadee.bpm.vPAV.config.model.ElementConvention;
import de.viadee.bpm.vPAV.config.model.ElementFieldTypes;
import de.viadee.bpm.vPAV.config.model.Rule;
import de.viadee.bpm.vPAV.config.model.RuleSet;
import de.viadee.bpm.vPAV.constants.ConfigConstants;
import de.viadee.bpm.vPAV.processing.ProcessVariableReader;
import de.viadee.bpm.vPAV.processing.code.flow.BpmnElement;
import de.viadee.bpm.vPAV.processing.code.flow.ControlFlowGraph;
import de.viadee.bpm.vPAV.processing.code.flow.FlowAnalysis;
import de.viadee.bpm.vPAV.processing.model.data.CheckerIssue;
import de.viadee.bpm.vPAV.processing.model.data.ProcessVariableOperation;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.BaseElement;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * unit tests for ProcessVariablesNameConventionChecker
 *
 */
public class ProcessVariablesNameConventionCheckerTest {

	private static final String BASE_PATH = "src/test/resources/";

	private static ElementChecker checker;

	private static ClassLoader cl;

	@BeforeClass
	public static void setup() throws MalformedURLException {
		RuntimeConfig.getInstance().setTest(true);
		Map<String, String> beanMapping = new HashMap<>();
		beanMapping.put("myBean", "de.viadee.bpm.vPAV.delegates.TestDelegate");
		RuntimeConfig.getInstance().setBeanMapping(beanMapping);
		checker = new ProcessVariablesNameConventionChecker(createRule(), null);
		final File file = new File(".");
		final String currentPath = file.toURI().toURL().toString();
		final URL classUrl = new URL(currentPath + "src/test/java/");
		final URL[] classUrls = { classUrl };
		cl = new URLClassLoader(classUrls);
		RuntimeConfig.getInstance().setClassLoader(cl);
		RuntimeConfig.getInstance().getResource("en_US");
	}

	@AfterClass
	public static void tearDown() {
		RuntimeConfig.getInstance().setTest(false);
	}

	/**
	 * case: internal and external process variables follows the conventions
	 *
	 */
	@Test
	public void testCorrectProcessVariableNames() {
		final FileScanner fileScanner = new FileScanner(new RuleSet());
		fileScanner.setScanPath(ConfigConstants.TEST_JAVAPATH);
		final String PATH = BASE_PATH
				+ "ProcessVariablesNameConventionCheckerTest_CorrectProcessVariablesNamingConvention.bpmn";

		// parse bpmn model
		final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

		final Collection<BaseElement> baseElements = modelInstance.getModelElementsByType(BaseElement.class);

		final Collection<CheckerIssue> issues = new ArrayList<>();
		for (final BaseElement baseElement : baseElements) {
			final ControlFlowGraph cg = new ControlFlowGraph();
			final BpmnElement element = new BpmnElement(PATH, baseElement, cg, new FlowAnalysis());
			ProcessVariableReader variableReader = new ProcessVariableReader(null,
					new Rule("ProcessVariableReader", true, null, null, null, null), new BpmnScanner(PATH));

			final ListMultimap<String, ProcessVariableOperation> variables = ArrayListMultimap.create();
			variables.putAll(variableReader.getVariablesFromElement(fileScanner, element, cg));

			element.setProcessVariables(variables);

			issues.addAll(checker.check(element));
		}

		assertEquals(0, issues.size());
	}

	/**
	 * case: recognise variables which are against the naming conventions
	 * (internal/external)
	 *
	 */
	@Test
	public void testWrongProcessVariableNames() {
		final FileScanner fileScanner = new FileScanner(new RuleSet());
		fileScanner.setScanPath(ConfigConstants.TEST_JAVAPATH);
		final String PATH = BASE_PATH
				+ "ProcessVariablesNameConventionCheckerTest_WrongProcessVariablesNamingConvention.bpmn";

		// parse bpmn model
		final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

		final Collection<BaseElement> baseElements = modelInstance.getModelElementsByType(BaseElement.class);

		final Collection<CheckerIssue> issues = new ArrayList<>();
		for (final BaseElement baseElement : baseElements) {
			final ControlFlowGraph cg = new ControlFlowGraph();
			final BpmnElement element = new BpmnElement(PATH, baseElement, cg, new FlowAnalysis());
			ProcessVariableReader variableReader = new ProcessVariableReader(null,
					new Rule("ProcessVariableReader", true, null, null, null, null), new BpmnScanner(PATH));

			final ListMultimap<String, ProcessVariableOperation> variables = ArrayListMultimap.create();
			variables.putAll(variableReader.getVariablesFromElement(fileScanner, element, cg));

			element.setProcessVariables(variables);

			issues.addAll(checker.check(element));
		}
		int externalConventions = 0;
		int internalConventions = 0;
		for (CheckerIssue issue : issues) {
			if (issue.getMessage().contains("external")) {
				externalConventions++;
			}
			if (issue.getMessage().contains("internal")) {
				internalConventions++;
			}
		}

		assertEquals(4, issues.size());
		assertEquals(1, internalConventions);
		assertEquals(3, externalConventions);
	}

	/**
	 * Creates the configuration rule
	 *
	 * @return rule
	 */
	private static Rule createRule() {

		final Collection<ElementConvention> elementConventions = new ArrayList<>();
		final Collection<String> fieldTypeNames = new ArrayList<>();
		fieldTypeNames.add("Class");
		fieldTypeNames.add("ExternalScript");
		fieldTypeNames.add("DelegateExpression");

		final ElementFieldTypes internalTypes = new ElementFieldTypes(fieldTypeNames, true);

		final ElementConvention internalElementConvention = new ElementConvention("internal", internalTypes, null,
				"int_[a-zA-Z]+");

		final ElementFieldTypes externalTypes = new ElementFieldTypes(fieldTypeNames, false);

		final ElementConvention externalElementConvention = new ElementConvention("external", externalTypes, null,
				"ext_[a-zA-Z]+");
		elementConventions.add(internalElementConvention);
		elementConventions.add(externalElementConvention);

		return new Rule("ProcessVariablesNameConventionChecker", true, null, null, elementConventions, null);
	}
}
