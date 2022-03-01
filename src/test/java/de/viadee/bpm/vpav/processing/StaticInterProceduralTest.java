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
package de.viadee.bpm.vpav.processing;

import com.google.common.collect.ListMultimap;
import de.viadee.bpm.vpav.FileScanner;
import de.viadee.bpm.vpav.RuntimeConfig;
import de.viadee.bpm.vpav.config.model.RuleSet;
import de.viadee.bpm.vpav.constants.ConfigConstants;
import de.viadee.bpm.vpav.processing.code.flow.BasicNode;
import de.viadee.bpm.vpav.processing.code.flow.BpmnElement;
import de.viadee.bpm.vpav.processing.code.flow.ControlFlowGraph;
import de.viadee.bpm.vpav.processing.code.flow.FlowAnalysis;
import de.viadee.bpm.vpav.processing.model.data.ProcessVariableOperation;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.ServiceTask;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.Collection;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

public class StaticInterProceduralTest {

	private static final String BASE_PATH = "src/test/resources/";

	@BeforeClass
	public static void setup() {
		RuntimeConfig.getInstance().setClassLoader(StaticInterProceduralTest.class.getClassLoader());
		RuntimeConfig.getInstance().setTest(true);
	}

	@Test
	public void testInterProceduralAnalysis() {
		final String PATH = BASE_PATH + "ProcessVariablesModelChecker_InterproceduralAnalysis.bpmn";

		// parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

        final Collection<ServiceTask> tasks = modelInstance.getModelElementsByType(ServiceTask.class);

        final BpmnElement element = new BpmnElement(PATH, tasks.iterator().next(), new ControlFlowGraph(),
                new FlowAnalysis());

        // Set custom basepath.
        Properties myProperties = new Properties();
        myProperties.put("scanpath", ConfigConstants.TARGET_TEST_PATH);
        RuntimeConfig.getInstance().setProperties(myProperties);
        final FileScanner fileScanner = new FileScanner(new RuleSet());
        JavaReaderStatic
                .getVariablesFromJavaDelegate("de.viadee.bpm.vpav.delegates.TestDelegateStaticInterProc", element, null,
                        null, new BasicNode[1]);
        ListMultimap<String, ProcessVariableOperation> variables = element.getControlFlowGraph().getOperations();

        // Then
        assertEquals("Static reader should also find variable from TestInterProcAnother class and TestInterPocOther", 5,
                variables.size());
    }

}
