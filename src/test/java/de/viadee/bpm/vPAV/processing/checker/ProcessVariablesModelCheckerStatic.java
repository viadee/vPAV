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
import de.viadee.bpm.vPAV.constants.ConfigConstants;
import de.viadee.bpm.vPAV.processing.ElementGraphBuilder;
import de.viadee.bpm.vPAV.processing.ProcessVariablesScanner;
import de.viadee.bpm.vPAV.processing.code.flow.FlowAnalysis;
import de.viadee.bpm.vPAV.processing.model.data.AnomalyContainer;
import de.viadee.bpm.vPAV.processing.model.data.CheckerIssue;
import de.viadee.bpm.vPAV.processing.model.graph.Graph;
import de.viadee.bpm.vPAV.processing.model.graph.Path;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

public class ProcessVariablesModelCheckerStatic {

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

	/**
	 * Case: there is an empty script reference
	 */
	@Test
	public void testProcessVariablesModelChecker() {
		final Set<String> resources = new HashSet<>();
		final ProcessVariablesScanner scanner = new ProcessVariablesScanner(resources);
		Properties myProperties = new Properties();
		myProperties.put("scanpath", "src/test/java");
		ConfigConstants.getInstance().setProperties(myProperties);
		final FileScanner fileScanner = new FileScanner(new RuleSet());
		final String PATH = BASE_PATH + "ProcessVariablesModelCheckerTestStatic_GraphCreation.bpmn";
		final File processDefinition = new File(PATH);

		// parse bpmn model
		BpmnModelInstance modelInstance = Bpmn.readModelFromFile(processDefinition);

		final ElementGraphBuilder graphBuilder = new ElementGraphBuilder(new BpmnScanner(PATH));
		// create data flow graphs
		final Collection<Graph> graphCollection = graphBuilder.createProcessGraph(fileScanner, modelInstance,
				processDefinition.getPath(), new ArrayList<>(), scanner, new FlowAnalysis());

		FlowAnalysis flowAnalysis = new FlowAnalysis();
		flowAnalysis.analyze(graphCollection);

		// calculate invalid paths based on data flow graphs
		final Map<AnomalyContainer, List<Path>> invalidPathMap = graphBuilder.createInvalidPaths(graphCollection);

		final Rule rule = new Rule("ProcessVariablesModelChecker", true, null, null, null, null);
		ModelChecker checker = new ProcessVariablesModelChecker(rule, invalidPathMap);

		final Collection<CheckerIssue> issues = checker.check();

		if (issues.size() == 0) {
			Assert.fail("there should be generated an issue");
		}

		Iterator<CheckerIssue> iterator = issues.iterator();
		final CheckerIssue issue1 = iterator.next();
		Assert.assertEquals("Task_0546a8y__0", issue1.getElementId());
		Assert.assertEquals("ProcessVariable2", issue1.getVariable());
		Assert.assertEquals("UR", issue1.getAnomaly().toString());
		final CheckerIssue issue2 = iterator.next();
		Assert.assertEquals("Task_0546a8y__2", issue2.getElementId());
		Assert.assertEquals("ProcessVariable3", issue2.getVariable());
		Assert.assertEquals("UU", issue2.getAnomaly().toString());
		final CheckerIssue issue3 = iterator.next();
		Assert.assertEquals("SequenceFlow_0btqo3y", issue3.getElementId());
		Assert.assertEquals("jepppa", issue3.getVariable());
		Assert.assertEquals("DD", issue3.getAnomaly().toString());
		final CheckerIssue issue4 = iterator.next();
		Assert.assertEquals("SequenceFlow_0bi6kaa", issue4.getElementId());
		Assert.assertEquals("geloeschteVariable", issue4.getVariable());
		Assert.assertEquals("DU", issue4.getAnomaly().toString());
		final CheckerIssue issue5 = iterator.next();
		Assert.assertEquals("Task_0oj9gln__0", issue5.getElementId());
		Assert.assertEquals("ProcessVariable2", issue5.getVariable());
		Assert.assertEquals("UR", issue5.getAnomaly().toString());
		final CheckerIssue issue6 = iterator.next();
		Assert.assertEquals("Task_0oj9gln__2", issue6.getElementId());
		Assert.assertEquals("ProcessVariable3", issue6.getVariable());
		Assert.assertEquals("UU", issue6.getAnomaly().toString());
		final CheckerIssue issue7 = iterator.next();
		Assert.assertEquals("ServiceTask_05g4a96", issue7.getElementId());
		Assert.assertEquals("intHallo", issue7.getVariable());
		Assert.assertEquals("UR", issue7.getAnomaly().toString());
		final CheckerIssue issue8 = iterator.next();
		Assert.assertEquals("BusinessRuleTask_119jb6t", issue8.getElementId());
		Assert.assertEquals("hallo2", issue8.getVariable());
		Assert.assertEquals("UR", issue8.getAnomaly().toString());

	}

}
