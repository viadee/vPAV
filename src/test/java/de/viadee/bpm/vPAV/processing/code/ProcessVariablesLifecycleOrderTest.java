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
package de.viadee.bpm.vPAV.processing.code;

import de.viadee.bpm.vPAV.FileScanner;
import de.viadee.bpm.vPAV.RuntimeConfig;
import de.viadee.bpm.vPAV.config.model.RuleSet;
import de.viadee.bpm.vPAV.constants.ConfigConstants;
import de.viadee.bpm.vPAV.processing.ElementGraphBuilder;
import de.viadee.bpm.vPAV.processing.ProcessVariablesScanner;
import de.viadee.bpm.vPAV.processing.code.flow.AnalysisElement;
import de.viadee.bpm.vPAV.processing.code.flow.BasicNode;
import de.viadee.bpm.vPAV.processing.code.flow.FlowAnalysis;
import de.viadee.bpm.vPAV.processing.model.data.KnownElementFieldType;
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
        final URL[] classUrls = { classUrl };
        ClassLoader cl = new URLClassLoader(classUrls);
        RuntimeConfig.getInstance().setClassLoader(cl);
        RuntimeConfig.getInstance().getResource("en_US");
        RuntimeConfig.getInstance().setTest(true);
    }

    @Test
    public void testProcessVariablesLifecycle() {
        // Test with Start Event (Signal)
        // and 1 Service Task (Input/Output Parameters, Start/End Listeners, Expression Implementation)
        // and with 1 Multi Instance Task (Loop Cardinality & Completion Condition expression)
        // and with 1 Receive Task (Message)
        // and with 1 Intermediate Throw Event (Link)

        final ProcessVariablesScanner scanner = new ProcessVariablesScanner(null);
        Properties myProperties = new Properties();
        myProperties.put("scanpath", ConfigConstants.TEST_TARGET_PATH);
        ConfigConstants.getInstance().setProperties(myProperties);
        final FileScanner fileScanner = new FileScanner(new RuleSet());
        final String PATH = BASE_PATH + "ProcessVariablesLifecycleOrderTest.bpmn";
        final File processDefinition = new File(PATH);

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(processDefinition);

        final ElementGraphBuilder graphBuilder = new ElementGraphBuilder(null, null, null, null);

        // create data flow graphs
        final Collection<String> calledElementHierarchy = new ArrayList<>();
        FlowAnalysis flowAnalysis = new FlowAnalysis();
        final Collection<Graph> graphCollection = graphBuilder.createProcessGraph(fileScanner, modelInstance,
                processDefinition.getPath(), calledElementHierarchy, scanner, flowAnalysis);

        flowAnalysis.analyze(graphCollection);

        LinkedHashMap<String, AnalysisElement> nodes = flowAnalysis.getNodes();
        assertEquals("There should be 22 nodes.", 22, nodes.size());

        // Find throw event
        AnalysisElement throwEvent = nodes.get("MyIntermediateThrowEvent__0");

        // Start from end event and go to start.
        AnalysisElement endEvent = nodes.get("MyEndEvent");
        AnalysisElement sequenceFlow_0ml2hlg = endEvent.getPredecessors().get(0);
        AnalysisElement gateway = sequenceFlow_0ml2hlg.getPredecessors().get(0);
        AnalysisElement sequenceFlow_1f9tqmg= gateway.getPredecessors().get(0);
        AnalysisElement receiveTask = sequenceFlow_1f9tqmg.getPredecessors().get(0);
        AnalysisElement sequenceFlow_0zu24g0 = receiveTask.getPredecessors().get(0);

        // Mulit-Instance Task
        AnalysisElement multiInstanceDelegate = sequenceFlow_0zu24g0.getPredecessors().get(0);
        AnalysisElement completionCondition = multiInstanceDelegate.getPredecessors().get(0);
        AnalysisElement loopCardinality = completionCondition.getPredecessors().get(0);
        AnalysisElement defaultLoopVariables = loopCardinality.getPredecessors().get(0);
        AnalysisElement sequenceFlow_1qw9mzs = defaultLoopVariables.getPredecessors().get(0);

        // Service Task
        // Order is only correct because the listeners are ordered like this in the bpmn file
        AnalysisElement outputParameter = sequenceFlow_1qw9mzs.getPredecessors().get(0);
        AnalysisElement endListenerDelegate = outputParameter.getPredecessors().get(0);
        AnalysisElement endListenerExpression = endListenerDelegate.getPredecessors().get(0);
        AnalysisElement implementationExpression = endListenerExpression.getPredecessors().get(0);
        AnalysisElement startListenerExpression = implementationExpression.getPredecessors().get(0);
        AnalysisElement startListenerDelegate = startListenerExpression.getPredecessors().get(0);
        AnalysisElement inputParameter = startListenerDelegate.getPredecessors().get(0);

        AnalysisElement sequenceFlow0 = inputParameter.getPredecessors().get(0);
        AnalysisElement startEvent = sequenceFlow0.getPredecessors().get(0);

        assertEquals("Throw Event should use variable {linkName} in link name.", 1,
                throwEvent.getUsed().size());

        assertEquals("Receive task should use variable {messageName} in message name.", 1,
                receiveTask.getUsed().size());

        assertEquals("Mapping Delegate should read five variables.", 5, multiInstanceDelegate.getUsed().size());
        assertEquals("Completion Condition expression should read two variables.", 2,
                completionCondition.getUsed().size());
        assertEquals("Loop Cardinality expression should read two variables.", 2, loopCardinality.getUsed().size());
        assertEquals("Default variables should define four variables.", 4, defaultLoopVariables.getDefined().size());

        assertEquals("Output Parameter should define variable {MyOutputParameter}.", 1,
                outputParameter.getDefined().size());
        assertEquals("Delegate End Listener should define variable {isExternalProcess}.", 1,
                endListenerDelegate.getDefined().size());
        assertEquals("Delegate End Listener should read variable {numberEntities}.", 1,
                endListenerDelegate.getUsed().size());
        assertEquals("Expression End Listener should read variable {varEnd}.", 1,
                endListenerExpression.getUsed().size());
        assertEquals("Expression Start Listener should read variable {var2}.", 1,
                startListenerExpression.getUsed().size());
        assertEquals("Delegate Start Listener should read variable {inputVariable}.", 1,
                startListenerDelegate.getUsed().size());
        assertEquals("Input Parameter should define variable {MyInputParameter}.", 1,
                inputParameter.getDefined().size());

        assertEquals("Start event was not reached.", "MyStartEvent__0", startEvent.getId());
        assertEquals("Start event should not have any predecessors.", 0, startEvent.getPredecessors().size());
        assertEquals("Start event should use variable {signalName} in signal name.", 1, startEvent.getUsed().size());

        // Check discovery of process variables
        assertEquals(
                "Last Sequence Flow should have two input parameters because the service task has one output parameter and one defined variable.",
                2, sequenceFlow_0ml2hlg.getInUnused().size());
        assertEquals("Delegate Start Listener should have one passed input parameter.", 1,
                startListenerDelegate.getInUnused().size());
    }

    @Test
    public void testProcessVariablesLifecycleWithCallActivity() {
        // Test with In/Out Variable Injection, Input/Output Parameters and Start/End Listeners
        final ProcessVariablesScanner scanner = new ProcessVariablesScanner(null);
        Properties myProperties = new Properties();
        myProperties.put("scanpath", ConfigConstants.TEST_TARGET_PATH);
        ConfigConstants.getInstance().setProperties(myProperties);
        final FileScanner fileScanner = new FileScanner(new RuleSet());
        final String PATH = BASE_PATH + "ProcessVariablesLifecycleOrderTest_WithCallActivity.bpmn";
        final File processDefinition = new File(PATH);

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(processDefinition);

        // add reference for called process
        final Map<String, String> processIdToPathMap = new HashMap<>();
        processIdToPathMap.put("calledProcess", "ProcessVariablesLifecycleOrderTest_CalledProcess.bpmn");

        final ElementGraphBuilder graphBuilder = new ElementGraphBuilder(null, processIdToPathMap, null, null);

        // create data flow graphs
        final Collection<String> calledElementHierarchy = new ArrayList<>();
        FlowAnalysis flowAnalysis = new FlowAnalysis();
        final Collection<Graph> graphCollection = graphBuilder.createProcessGraph(fileScanner, modelInstance,
                processDefinition.getPath(), calledElementHierarchy, scanner, flowAnalysis);

        flowAnalysis.analyze(graphCollection);

        LinkedHashMap<String, AnalysisElement> nodes = flowAnalysis.getNodes();
        // Start from end event and go to start.
        AnalysisElement endEvent = nodes.get("MyEndEvent");
        AnalysisElement sequenceFlow_1qw9mzs = endEvent.getPredecessors().get(0);
        AnalysisElement outputParameter = sequenceFlow_1qw9mzs.getPredecessors().get(0);
        AnalysisElement endListener2 = outputParameter.getPredecessors().get(0);
        AnalysisElement endListener1 = endListener2.getPredecessors().get(0);
        AnalysisElement endListenerExpression = endListener1.getPredecessors().get(0);
        AnalysisElement outMapping =  endListenerExpression.getPredecessors().get(0);
        AnalysisElement endEventCalledProcess = outMapping.getPredecessors().get(0);
        AnalysisElement serviceTaskCalledProcess = endEventCalledProcess.getPredecessors().get(0).getPredecessors()
                .get(0);
        AnalysisElement startEventCalledProcess = serviceTaskCalledProcess.getPredecessors().get(0).getPredecessors()
                .get(0);
        AnalysisElement inMapping = startEventCalledProcess.getPredecessors().get(0);
        AnalysisElement startListenerExpression = inMapping.getPredecessors().get(0);
        AnalysisElement startListener2 = startListenerExpression.getPredecessors().get(0);
        AnalysisElement startListener1 = startListener2.getPredecessors().get(0);
        AnalysisElement inputParameter = startListener1.getPredecessors().get(0);
        AnalysisElement serviceTask = inputParameter.getPredecessors().get(0).getPredecessors().get(0);
        AnalysisElement startEvent = serviceTask.getPredecessors().get(0).getPredecessors().get(0);

        assertEquals("Output parameter should write variable {MyOutputParameter}.", 1, outputParameter.getDefined().size());
        assertEquals("Input parameter should write variable {MyInputParamter}.", 1, inMapping.getDefined().size());
        assertEquals("End Listener 2 was not correctly included.", KnownElementFieldType.Class, ((BasicNode)endListener2).getFieldType());
        assertEquals("Expression End Listener was not correctly included.", KnownElementFieldType.Expression,
                ((BasicNode)endListenerExpression).getFieldType());
        assertEquals("Start Listener 1 was not correctly included.", "MyCallActivity__1", startListener1.getId());
        assertEquals("Expression Start Listener was not correctly included.", "MyCallActivity__3",
                startListenerExpression.getId());
        assertEquals("_EndEvent_SUCC", endEventCalledProcess.getId());
        assertEquals("_MyCalledServiceTask", serviceTaskCalledProcess.getId());
        assertEquals("_StartEvent_1", startEventCalledProcess.getId());
        assertEquals("Start event was not reached.", "MyStartEvent", startEvent.getId());
        assertEquals("Start event should not have any predecessors.", 0, startEvent.getPredecessors().size());

        // Check discovery of process variables
        assertEquals("Start Listener should have one input variable from service task and one input parameter.", 2,
                startListener1.getInUnused().size());
        assertEquals("Start Listener should have one own defined variable.", 1,
                startListener1.getDefined().size());
        assertEquals("Start Listener 2 should have three input variables.", 3, startListener2.getInUnused().size());
        assertEquals("Child start event should have one input variables.", 1,
                startEventCalledProcess.getInUnused().size());
        assertEquals("End Listener with Expression should have five input variables", 5,
                endListenerExpression.getInUnused().size());
        assertEquals("Second End Listener with Expression should have four unused input variables", 4,
                endListener1.getInUnused().size());
        assertEquals("Last End Listener should have five output variables", 5,
                endListener2.getOutUnused().size() + endListener2.getOutUsed().size());

        assertEquals(
                "Third Sequence Flow (1qw9mzs) shouldn't have defined variables because the output parameters are already defined in an own node.",
                0, sequenceFlow_1qw9mzs.getDefined().size());

        assertEquals("End event should have four unused input variables.", 4, endEvent.getInUnused().size());
        assertEquals("End event should have one used input variable.", 1, endEvent.getInUsed().size());
    }
}
