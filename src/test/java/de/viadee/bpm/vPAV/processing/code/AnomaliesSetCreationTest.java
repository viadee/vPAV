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
package de.viadee.bpm.vPAV.processing.code;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import de.viadee.bpm.vPAV.FileScanner;
import de.viadee.bpm.vPAV.RuntimeConfig;
import de.viadee.bpm.vPAV.constants.ConfigConstants;
import de.viadee.bpm.vPAV.processing.JavaReaderStatic;
import de.viadee.bpm.vPAV.processing.code.flow.BpmnElement;
import de.viadee.bpm.vPAV.processing.code.flow.ControlFlowGraph;
import de.viadee.bpm.vPAV.processing.model.data.Anomaly;
import de.viadee.bpm.vPAV.processing.model.data.ProcessVariableOperation;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.ServiceTask;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;

public class AnomaliesSetCreationTest {

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

    @Test
    public void findDD() {
        final String PATH = BASE_PATH + "ProcessVariablesModelChecker_AnomalyDD.bpmn";

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

        final Collection<ServiceTask> tasks = modelInstance
                .getModelElementsByType(ServiceTask.class);

        final BpmnElement element = new BpmnElement(PATH, tasks.iterator().next(), new ControlFlowGraph());
        final ControlFlowGraph cg = new ControlFlowGraph();
        final FileScanner fileScanner = new FileScanner(new HashMap<>(), ConfigConstants.TEST_JAVAPATH);
        final ListMultimap<String, ProcessVariableOperation> variables = ArrayListMultimap.create();
        variables.putAll(new JavaReaderStatic().getVariablesFromJavaDelegate(fileScanner,
                "de.viadee.bpm.vPAV.delegates.DelegateAnomalyDD", element, null, null, null, cg));
//        cg.analyze(element);

        Anomaly anomaly = element.getAnomalies().entrySet().iterator().next().getValue().iterator().next().getAnomaly();
        assertEquals("Expected 1 anomalie but found " + element.getAnomalies().size(), 1, element.getAnomalies().size());
        assertEquals("Expected a DD anomaly but found " + anomaly, Anomaly.DD, anomaly);
    }

    @Test
    public void findDU() {
        final String PATH = BASE_PATH + "ProcessVariablesModelChecker_AnomalyDU.bpmn";

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

        final Collection<ServiceTask> tasks = modelInstance
                .getModelElementsByType(ServiceTask.class);

        final BpmnElement element = new BpmnElement(PATH, tasks.iterator().next(), new ControlFlowGraph());
        final ControlFlowGraph cg = new ControlFlowGraph();
        final FileScanner fileScanner = new FileScanner(new HashMap<>(), ConfigConstants.TEST_JAVAPATH);
        final ListMultimap<String, ProcessVariableOperation> variables = ArrayListMultimap.create();
        variables.putAll(new JavaReaderStatic().getVariablesFromJavaDelegate(fileScanner,
                "de.viadee.bpm.vPAV.delegates.DelegateAnomalyDU", element, null, null, null, cg));
//        cg.analyze(element);

        Anomaly anomaly = element.getAnomalies().entrySet().iterator().next().getValue().iterator().next().getAnomaly();
        assertEquals("Expected 1 anomalie but found " + element.getAnomalies().size(), 1, element.getAnomalies().size());
        assertEquals("Expected a DU anomaly but found " + anomaly, Anomaly.DU, anomaly);
    }

    @Test
    public void findUR() {
        final String PATH = BASE_PATH + "ProcessVariablesModelChecker_AnomalyUR.bpmn";

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

        final Collection<ServiceTask> tasks = modelInstance
                .getModelElementsByType(ServiceTask.class);

        final BpmnElement element = new BpmnElement(PATH, tasks.iterator().next(), new ControlFlowGraph());
        final ControlFlowGraph cg = new ControlFlowGraph();
        final FileScanner fileScanner = new FileScanner(new HashMap<>(), ConfigConstants.TEST_JAVAPATH);
        final ListMultimap<String, ProcessVariableOperation> variables = ArrayListMultimap.create();
        variables.putAll(new JavaReaderStatic().getVariablesFromJavaDelegate(fileScanner,
                "de.viadee.bpm.vPAV.delegates.DelegateAnomalyUR", element, null, null, null, cg));
//        cg.analyze(element);

        Anomaly anomaly = element.getAnomalies().entrySet().iterator().next().getValue().iterator().next().getAnomaly();
        assertEquals("Expected 1 anomalie but found " + element.getAnomalies().size(), 1, element.getAnomalies().size());
        assertEquals("Expected a UR anomaly but found " + anomaly, Anomaly.UR, anomaly);
    }

    @Test
    public void findUU() {
        final String PATH = BASE_PATH + "ProcessVariablesModelChecker_AnomalyUU.bpmn";

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

        final Collection<ServiceTask> tasks = modelInstance
                .getModelElementsByType(ServiceTask.class);

        final BpmnElement element = new BpmnElement(PATH, tasks.iterator().next(), new ControlFlowGraph());
        final ControlFlowGraph cg = new ControlFlowGraph();
        final FileScanner fileScanner = new FileScanner(new HashMap<>(), ConfigConstants.TEST_JAVAPATH);
        final ListMultimap<String, ProcessVariableOperation> variables = ArrayListMultimap.create();
        variables.putAll(new JavaReaderStatic().getVariablesFromJavaDelegate(fileScanner,
                "de.viadee.bpm.vPAV.delegates.DelegateAnomalyUU", element, null, null, null, cg));
//        cg.analyze(element);

        Anomaly anomaly = element.getAnomalies().entrySet().iterator().next().getValue().iterator().next().getAnomaly();
        assertEquals("Expected 1 anomalie but found " + element.getAnomalies().size(), 1, element.getAnomalies().size());
        assertEquals("Expected a UU anomaly but found " + anomaly, Anomaly.UU, anomaly);
    }

    @Test
    public void findNOPR() {
    }

    @Test
    public void findDNOP() {
    }





}
