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
import de.viadee.bpm.vpav.processing.code.flow.BasicNode;
import de.viadee.bpm.vpav.processing.code.flow.BpmnElement;
import de.viadee.bpm.vpav.processing.code.flow.ControlFlowGraph;
import de.viadee.bpm.vpav.processing.code.flow.FlowAnalysis;
import de.viadee.bpm.vpav.processing.model.data.ProcessVariableOperation;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.ServiceTask;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import soot.Scene;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.LinkedList;

public class ProcessVariableOperationReaderTest {

    private static final String BASE_PATH = "src/test/resources/";

    @BeforeClass
    public static void setup() throws MalformedURLException {
        final File file = new File(".");
        final String currentPath = file.toURI().toURL().toString();
        final URL classUrl = new URL(currentPath + "src/test/java");
        final URL[] classUrls = { classUrl };
        ClassLoader cl = new URLClassLoader(classUrls);
        RuntimeConfig.getInstance().setClassLoader(cl);
        RuntimeConfig.getInstance().setTest(true);
        FileScanner.setupSootClassPaths(new LinkedList<>());
        JavaReaderStatic.setupSoot();
        Scene.v().loadNecessaryClasses();
    }

    @AfterClass
    public static void tearDown() {
        RuntimeConfig.getInstance().setTest(false);
    }

    @Test
    public void testRecogniseVariablesInClass() {
        final String PATH = BASE_PATH + "ModelWithDelegate_UR.bpmn";

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

        final Collection<ServiceTask> allServiceTasks = modelInstance.getModelElementsByType(ServiceTask.class);

        final ProcessVariableReader variableReader = new ProcessVariableReader(null, null);

        final BpmnElement element = new BpmnElement(PATH, allServiceTasks.iterator().next(), new ControlFlowGraph(), new FlowAnalysis());
        variableReader.getVariablesFromElement(element, new BasicNode[1]);
        ListMultimap<String, ProcessVariableOperation> variables = element.getControlFlowGraph().getOperations();

        Assert.assertEquals(2, variables.size());
    }
}
