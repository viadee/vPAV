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
import de.viadee.bpm.vPAV.FileScanner;
import de.viadee.bpm.vPAV.RuntimeConfig;
import de.viadee.bpm.vPAV.config.model.RuleSet;
import de.viadee.bpm.vPAV.processing.code.flow.BpmnElement;
import de.viadee.bpm.vPAV.processing.code.flow.ControlFlowGraph;
import de.viadee.bpm.vPAV.processing.code.flow.FlowAnalysis;
import de.viadee.bpm.vPAV.processing.model.data.ProcessVariableOperation;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.ServiceTask;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

public class ReachingDefinitionTest {

	private static ClassLoader cl;

	private static final String BASE_PATH = "src/test/resources/";

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

	@Test
	public void testSootReachingMethod() {
		final String PATH = BASE_PATH + "ProcessVariablesModelCheckerTest_InitialProcessVariables.bpmn";

		// parse bpmn model
		final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

		final Collection<ServiceTask> tasks = modelInstance.getModelElementsByType(ServiceTask.class);
		final ControlFlowGraph cg = new ControlFlowGraph();
		final BpmnElement element = new BpmnElement(PATH, tasks.iterator().next(), cg, new FlowAnalysis());
		final FileScanner fileScanner = new FileScanner(new RuleSet());
		final ListMultimap<String, ProcessVariableOperation> variables = ArrayListMultimap.create();
		variables.putAll(new JavaReaderStatic().getVariablesFromJavaDelegate(fileScanner,
				"de.viadee.bpm.vPAV.delegates.TestDelegateReachingDef", element, null, null, null, cg));
		assertEquals(3, variables.asMap().size());
	}
}
