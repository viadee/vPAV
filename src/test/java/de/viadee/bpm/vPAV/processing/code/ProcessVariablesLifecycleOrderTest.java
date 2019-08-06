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

import de.viadee.bpm.vPAV.BpmnScanner;
import de.viadee.bpm.vPAV.FileScanner;
import de.viadee.bpm.vPAV.RuntimeConfig;
import de.viadee.bpm.vPAV.config.model.RuleSet;
import de.viadee.bpm.vPAV.constants.ConfigConstants;
import de.viadee.bpm.vPAV.processing.ElementGraphBuilder;
import de.viadee.bpm.vPAV.processing.ProcessVariablesScanner;
import de.viadee.bpm.vPAV.processing.code.flow.AnalysisElement;
import de.viadee.bpm.vPAV.processing.code.flow.FlowAnalysis;
import de.viadee.bpm.vPAV.processing.model.graph.Graph;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

import static org.junit.Assert.assertEquals;

public class ProcessVariablesLifecycleOrderTest {

    private static final String BASE_PATH = "src/test/resources/";

    @BeforeClass
    public static void setup() throws IOException {
        final File file = new File(".");
        final String currentPath = file.toURI().toURL().toString();
        final URL classUrl = new URL(currentPath + "src/test/java");
        final URL[] classUrls = {classUrl};
        ClassLoader cl = new URLClassLoader(classUrls);
        RuntimeConfig.getInstance().setClassLoader(cl);
        RuntimeConfig.getInstance().getResource("en_US");
        RuntimeConfig.getInstance().setTest(true);
    }

    @Test
    public void testProcessVariablesLifecycle() {
        // Test with Input/Output Parameters and Start/End Listeners
        // TODO add all other things linke links, signals, messages, ...

        final ProcessVariablesScanner scanner = new ProcessVariablesScanner(null);
        Properties myProperties = new Properties();
        myProperties.put("scanpath", "src/test/java");
        ConfigConstants.getInstance().setProperties(myProperties);
        final FileScanner fileScanner = new FileScanner(new RuleSet());
        final String PATH = BASE_PATH + "ProcessVariablesLifecycleOrderTest.bpmn";
        final File processDefinition = new File(PATH);

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(processDefinition);

        final ElementGraphBuilder graphBuilder = new ElementGraphBuilder(null, null, null, null, new BpmnScanner(PATH));

        // create data flow graphs
        final Collection<String> calledElementHierarchy = new ArrayList<>();
        FlowAnalysis flowAnalysis = new FlowAnalysis();
        final Collection<Graph> graphCollection = graphBuilder.createProcessGraph(fileScanner, modelInstance,
                processDefinition.getPath(), calledElementHierarchy, scanner, flowAnalysis);

        flowAnalysis.analyze(graphCollection);

        LinkedHashMap<String, AnalysisElement> nodes = flowAnalysis.getNodes();
        // Start from end event and go to start.
        AnalysisElement endEvent = nodes.get("MyEndEvent");
        AnalysisElement sequenceFlow1 = endEvent.getPredecessors().get(0);
        AnalysisElement endListener = sequenceFlow1.getPredecessors().get(0);
        AnalysisElement startListener = endListener.getPredecessors().get(0);
        AnalysisElement sequenceFlow0 = startListener.getPredecessors().get(0);
        AnalysisElement startEvent = sequenceFlow0.getPredecessors().get(0);

        assertEquals("End Listener was not correctly included.", "MyServiceTask__1", endListener.getId());
        assertEquals("Start Listener was not correctly included.", "MyServiceTask__0", startListener.getId());
        assertEquals("Start event was not reached.", "MyStartEvent", startEvent.getId());
        assertEquals("Start event should not have any predecessors.", 0, startEvent.getPredecessors().size());

        // Check discovery of process variables
        assertEquals("Second Sequence Flow should have two input parameters because the service task has two output parameters.", 2, sequenceFlow1.getInUnused().size());
        assertEquals("Start Listener should have one input parameter.", 1, startListener.getDefined().size());
    }

    @Test
    public void testProcessVariablesLifecycleWithCallActivity() {
        // Test with In/Out Variable Injection, Input/Output Parameters and Start/End Listeners
        // TODO add all other things linke links, signals, messages, ... Delegate Variable Mapping
        // TODO Add delegate

        final ProcessVariablesScanner scanner = new ProcessVariablesScanner(null);
        Properties myProperties = new Properties();
        myProperties.put("scanpath", "src/test/java");
        ConfigConstants.getInstance().setProperties(myProperties);
        final FileScanner fileScanner = new FileScanner(new RuleSet());
        final String PATH = BASE_PATH + "ProcessVariablesLifecycleOrderTest_WithCallActivity.bpmn";
        final File processDefinition = new File(PATH);

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(processDefinition);

        // add reference for called process
        final Map<String, String> processIdToPathMap = new HashMap<>();
        processIdToPathMap.put("calledProcess", "ProcessVariablesLifecycleOrderTest_CalledProcess.bpmn");

        final ElementGraphBuilder graphBuilder = new ElementGraphBuilder(null, processIdToPathMap, null, null, new BpmnScanner(PATH));

        // create data flow graphs
        final Collection<String> calledElementHierarchy = new ArrayList<>();
        FlowAnalysis flowAnalysis = new FlowAnalysis();
        final Collection<Graph> graphCollection = graphBuilder.createProcessGraph(fileScanner, modelInstance,
                processDefinition.getPath(), calledElementHierarchy, scanner, flowAnalysis);

        flowAnalysis.analyze(graphCollection);

        LinkedHashMap<String, AnalysisElement> nodes = flowAnalysis.getNodes();
        // Start from end event and go to start.
        AnalysisElement endEvent = nodes.get("MyEndEvent");
        AnalysisElement sequenceFlow1 = endEvent.getPredecessors().get(0);
        AnalysisElement endListener2 = sequenceFlow1.getPredecessors().get(0);
        AnalysisElement endListener1 = endListener2.getPredecessors().get(0);
        AnalysisElement endEventCalledProcess = endListener1.getPredecessors().get(0);
        AnalysisElement serviceTaskCalledProcess = endEventCalledProcess.getPredecessors().get(0).getPredecessors().get(0);
        AnalysisElement startEventCalledProcess = serviceTaskCalledProcess.getPredecessors().get(0).getPredecessors().get(0);
        AnalysisElement startListener2 = startEventCalledProcess.getPredecessors().get(0);
        AnalysisElement startListener13 = startListener2.getPredecessors().get(0);
        AnalysisElement startListener12 = startListener13.getPredecessors().get(0);
        AnalysisElement startListener11 = startListener12.getPredecessors().get(0);
        AnalysisElement serviceTask = startListener11.getPredecessors().get(0).getPredecessors().get(0);
        AnalysisElement startEvent = serviceTask.getPredecessors().get(0).getPredecessors().get(0);

        assertEquals("End Listener 2 was not correctly included.", "MyCallActivity__5", endListener2.getId());
        assertEquals("End Listener 1 was not correctly included.", "MyCallActivity__4", endListener1.getId());
        assertEquals("Start Listener 1 was not correctly included.", "MyCallActivity__0", startListener11.getId());
        assertEquals("Start Listener 2 was not correctly included.", "MyCallActivity__3", startListener2.getId());
        assertEquals("_EndEvent_SUCC", endEventCalledProcess.getId());
        assertEquals("_MyCalledServiceTask", serviceTaskCalledProcess.getId());
        assertEquals("_StartEvent_1", startEventCalledProcess.getId());
        assertEquals("Start event was not reached.", "MyStartEvent", startEvent.getId());
        assertEquals("Start event should not have any predecessors.", 0, startEvent.getPredecessors().size());

        // Check discovery of process variables
        assertEquals("Start Listener should have one input variable from service task.", 1, startListener11.getInUnused().size());
        assertEquals("Start Listener should have two variables from input parameters.", 2, startListener11.getDefined().size());
        assertEquals("Start Listener 2 should have four input variables.", 4, startListener2.getInUnused().size());
        assertEquals("Child start event should have five input variables.", 5, startEventCalledProcess.getInUnused().size());
        assertEquals("Input variables of call activity parent should not be listed as defined in child start event.", 0, startEventCalledProcess.getDefined().size());
        assertEquals("End Listener should have five input variables", 5, endListener1.getInUnused().size());
        assertEquals("End Listener should have seven output variables", 7, endListener2.getOutUnused().size() + endListener2.getOutUsed().size());
        assertEquals("End Listener should have to defined variables because the call activity has two output mappings/parameters.", 2, endListener2.getDefined().size());

        assertEquals("Third Sequence Flow (1qw9mzs) shouldn't have defined variables because the output parameters are already defined in the listener.", 0, sequenceFlow1.getDefined().size());

        assertEquals("End event should have five unused input variables.", 5, endEvent.getInUnused().size());
        assertEquals("End event should have one used input variable.", 1, endEvent.getInUsed().size());
    }
}
