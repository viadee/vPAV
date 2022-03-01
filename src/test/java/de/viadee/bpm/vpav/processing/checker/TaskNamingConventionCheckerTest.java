/*
 * BSD 3-Clause License
 *
 * Copyright © 2022, viadee Unternehmensberatung AG
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
package de.viadee.bpm.vpav.processing.checker;

import de.viadee.bpm.vpav.IssueService;
import de.viadee.bpm.vpav.RuntimeConfig;
import de.viadee.bpm.vpav.config.model.ElementConvention;
import de.viadee.bpm.vpav.config.model.Rule;
import de.viadee.bpm.vpav.processing.code.flow.BpmnElement;
import de.viadee.bpm.vpav.processing.code.flow.ControlFlowGraph;
import de.viadee.bpm.vpav.processing.code.flow.FlowAnalysis;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.BaseElement;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class TaskNamingConventionCheckerTest {

	private static final String BASE_PATH = "src/test/resources/";

	private static ElementChecker checker;

	@BeforeClass
	public static void setup() throws MalformedURLException {
		checker = new TaskNamingConventionChecker(createRule());
		final File file = new File(".");
		final String currentPath = file.toURI().toURL().toString();
		final URL classUrl = new URL(currentPath + "src/test/java/");
		final URL[] classUrls = { classUrl };
		ClassLoader cl = new URLClassLoader(classUrls);
		RuntimeConfig.getInstance().setClassLoader(cl);
		RuntimeConfig.getInstance().setResource("en_US");
	}

	/**
	 * Case 1: Recognise a task name that fits the naming convention
	 */
	@Test
	public void testCorrectTaskNamingConvention() {
		final String PATH = BASE_PATH + "TaskNamingConventionCheckerTest_CorrectTaskNamingConvention.bpmn";

		// parse bpmn model
		final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

		final Collection<BaseElement> baseElements = modelInstance.getModelElementsByType(BaseElement.class);


		for (final BaseElement baseElement : baseElements) {
			final BpmnElement element = new BpmnElement(PATH, baseElement, new ControlFlowGraph(), new FlowAnalysis());
            checker.check(element);
		}

        if (IssueService.getInstance().getIssues().size() > 0) {
			fail("There are issues, although the convention is correct.");
		}
	}

	/**
	 * Case 2: Recognise a violation against the naming convention
	 */
	@Test
	public void testWrongTaskNamingConvention() {
		final String PATH = BASE_PATH + "TaskNamingConventionCheckerTest_WrongTaskNamingConvention.bpmn";

		// parse bpmn model
		final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

		final Collection<BaseElement> baseElements = modelInstance.getModelElementsByType(BaseElement.class);


		for (final BaseElement baseElement : baseElements) {
			final BpmnElement element = new BpmnElement(PATH, baseElement, new ControlFlowGraph(), new FlowAnalysis());
            checker.check(element);
		}

        assertEquals("The issue wasn't recognised", 1, IssueService.getInstance().getIssues().size());
	}

	/**
	 * Creates rule configuration
	 *
	 * @return rule
	 */
	private static Rule createRule() {

		final Collection<ElementConvention> elementConventions = new ArrayList<>();

		final ElementConvention elementConvention = new ElementConvention("convention", null, null,
				"[A-ZÄÖÜ][a-zäöü\\-\\s]+");
		elementConventions.add(elementConvention);

        return new Rule("TaskNamingConventionChecker", true, null, null, elementConventions, null);
    }

	@Before
    public void clearIssues() {
        IssueService.getInstance().clear();
	}
}
