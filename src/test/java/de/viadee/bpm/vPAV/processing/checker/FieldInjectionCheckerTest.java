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
import de.viadee.bpm.vPAV.IssueService;
import de.viadee.bpm.vPAV.RuntimeConfig;
import de.viadee.bpm.vPAV.config.model.Rule;
import de.viadee.bpm.vPAV.processing.code.flow.BpmnElement;
import de.viadee.bpm.vPAV.processing.code.flow.ControlFlowGraph;
import de.viadee.bpm.vPAV.processing.code.flow.FlowAnalysis;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.ServiceTask;
import org.junit.After;
import org.junit.Assert;
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
 * unit tests for class FieldInjectionChecker
 *
 */
public class FieldInjectionCheckerTest {

	private static final String BASE_PATH = "src/test/resources/";

	private static FieldInjectionChecker checker;

	private static ClassLoader cl;

	private final Rule rule = new Rule("FieldInjectionChecker", true, null, null, null, null);

	@BeforeClass
	public static void setup() throws MalformedURLException {

		// Bean-Mapping
		final Map<String, String> beanMapping = new HashMap<String, String>();
		beanMapping.put("testDelegate", "de.viadee.bpm.vPAV.delegates.DelegateWithWrongType");
		RuntimeConfig.getInstance().setBeanMapping(beanMapping);

		final File file = new File(".");
		final String currentPath = file.toURI().toURL().toString();
		final URL classUrl = new URL(currentPath + "src/test/java");
		final URL[] classUrls = { classUrl };
		cl = new URLClassLoader(classUrls);
		RuntimeConfig.getInstance().setClassLoader(cl);
		RuntimeConfig.getInstance().getResource("en_US");
	}

	/**
	 * Case: JavaDelegate with correct implemented fieldInjection variable
	 *
	 */
	@Test
    public void testCorrectFieldInjection() {
		final String PATH = BASE_PATH + "FieldInjectionCheckerTest_CorrectFieldInjection.bpmn";
		checker = new FieldInjectionChecker(rule, new BpmnScanner(PATH));

		// parse bpmn model
		final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

		final Collection<ServiceTask> baseElements = modelInstance.getModelElementsByType(ServiceTask.class);

		final BpmnElement element = new BpmnElement(PATH, baseElements.iterator().next(), new ControlFlowGraph(),
				new FlowAnalysis());

        checker.check(element);

        if (IssueService.getInstance().getIssues().size() > 0) {
			Assert.fail("correct java delegate generates an issue");
		}
	}

	/**
	 * Case: Type of fieldInjection variable is incorrect
	 *
	 */
	@Test
    public void testWrongTypeOfFieldInjection() {
		final String PATH = BASE_PATH + "FieldInjectionCheckerTest_WrongType.bpmn";
		checker = new FieldInjectionChecker(rule, new BpmnScanner(PATH));

		// parse bpmn model
		final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

		final Collection<ServiceTask> baseElements = modelInstance.getModelElementsByType(ServiceTask.class);

		final BpmnElement element = new BpmnElement(PATH, baseElements.iterator().next(), new ControlFlowGraph(),
				new FlowAnalysis());

        checker.check(element);

        if (IssueService.getInstance().getIssues().size() != 1) {
			Assert.fail("wrong type doesn't generate an issue");
		}
	}

	/**
	 * Case: No setter method for variable
	 *
	 */
	@Test
    public void testNoSetterMethod() {
		final String PATH = BASE_PATH + "FieldInjectionCheckerTest_NoSetter.bpmn";
		checker = new FieldInjectionChecker(rule, new BpmnScanner(PATH));

		// parse bpmn model
		final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

		final Collection<ServiceTask> baseElements = modelInstance.getModelElementsByType(ServiceTask.class);

		final BpmnElement element = new BpmnElement(PATH, baseElements.iterator().next(), new ControlFlowGraph(),
				new FlowAnalysis());

        checker.check(element);

        if (IssueService.getInstance().getIssues().size() != 1) {
			Assert.fail("no setter method doesn't generate an issue");
		}
	}

    @After
    public void clearIssues() {
        IssueService.getInstance().clear();
    }
}
