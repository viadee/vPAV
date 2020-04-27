/*
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

import de.viadee.bpm.vPAV.IssueService;
import de.viadee.bpm.vPAV.RuntimeConfig;
import de.viadee.bpm.vPAV.config.model.Rule;
import de.viadee.bpm.vPAV.config.model.Setting;
import de.viadee.bpm.vPAV.processing.code.flow.BpmnElement;
import de.viadee.bpm.vPAV.processing.code.flow.ControlFlowGraph;
import de.viadee.bpm.vPAV.processing.code.flow.FlowAnalysis;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.BaseElement;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * unit tests for class NoExpressionChecke
 *
 */
public class NoExpressionCheckerTest {

	private static final String BASE_PATH = "src/test/resources/";

	private static NoExpressionChecker checker;

	private static Map<String, Setting> setting = new HashMap<>();

	private final Rule rule = new Rule("NoExpressionChecker", true, null, setting, null, null);

	@BeforeClass
	public static void setup() throws MalformedURLException {
		final File file = new File(".");
		final String currentPath = file.toURI().toURL().toString();
		final URL classUrl = new URL(currentPath + "src/test/java");
		final URL[] classUrls = { classUrl };
		ClassLoader cl = new URLClassLoader(classUrls);
		RuntimeConfig.getInstance().setClassLoader(cl);
		RuntimeConfig.getInstance().getResource("en_US");
	}

	/**
	 * Case: Tasks without Expressions
	 */
	@Test
    public void testTaskWithoutExpression() {
		final String PATH = BASE_PATH + "NoExpressionChecker_WithoutExpressions.bpmn";
		checker = new NoExpressionChecker(rule);

		// parse bpmn model
		final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

		final Collection<BaseElement> baseElements = modelInstance.getModelElementsByType(BaseElement.class);

		for (BaseElement baseElement : baseElements) {
			final BpmnElement element = new BpmnElement(PATH, baseElement, new ControlFlowGraph(), new FlowAnalysis());
            checker.check(element);
		}

        if (IssueService.getInstance().getIssues().size() > 0) {
			Assert.fail("correct model generates an issue");
		}
	}

	/**
	 * Case: Tasks with Expressions
	 */
	@Test
    public void testTaskWithExpression() {
		final String PATH = BASE_PATH + "NoExpressionChecker_WithExpressions.bpmn";
		checker = new NoExpressionChecker(rule);

		// parse bpmn model
		final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

		final Collection<BaseElement> baseElements = modelInstance.getModelElementsByType(BaseElement.class);

		for (BaseElement baseElement : baseElements) {
			final BpmnElement element = new BpmnElement(PATH, baseElement, new ControlFlowGraph(), new FlowAnalysis());
            checker.check(element);
		}

        if (IssueService.getInstance().getIssues().size() != 2) {
			Assert.fail("correct model generates an issue");
		}
	}

	/**
	 * Case: Events with Expressions
	 */
	@Test
    public void testEventsWithExpression() {
		final String PATH = BASE_PATH + "NoExpressionChecker_EventsWithExpressions.bpmn";
		checker = new NoExpressionChecker(rule);

		// parse bpmn model
		final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

		final Collection<BaseElement> baseElements = modelInstance.getModelElementsByType(BaseElement.class);

		for (BaseElement baseElement : baseElements) {
			final BpmnElement element = new BpmnElement(PATH, baseElement, new ControlFlowGraph(), new FlowAnalysis());
            checker.check(element);
		}

        if (IssueService.getInstance().getIssues().size() != 2) {
			Assert.fail("model should generate 2 issues");
		}
	}

	/**
	 * Case: Sequenceflow with Expressions
	 */
	@Test
    public void testSequenceFlowWithExpression() {
		final String PATH = BASE_PATH + "NoExpressionChecker_SequenceFlowWithExpression.bpmn";
		checker = new NoExpressionChecker(rule);

		// parse bpmn model
		final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

		final Collection<BaseElement> baseElements = modelInstance.getModelElementsByType(BaseElement.class);

		for (BaseElement baseElement : baseElements) {
			final BpmnElement element = new BpmnElement(PATH, baseElement, new ControlFlowGraph(), new FlowAnalysis());
            checker.check(element);
		}

        if (IssueService.getInstance().getIssues().size() != 1) {
			Assert.fail("model should generate 3 issues");
		}
	}

	@Before
    public void clearIssues() {
        IssueService.getInstance().clear();
    }
}
