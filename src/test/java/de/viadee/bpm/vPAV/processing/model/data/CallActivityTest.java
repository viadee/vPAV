/*
 * BSD 3-Clause License
 *
 * Copyright © 2020, viadee Unternehmensberatung AG
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
package de.viadee.bpm.vPAV.processing.model.data;

import de.viadee.bpm.vPAV.FileScanner;
import de.viadee.bpm.vPAV.IssueService;
import de.viadee.bpm.vPAV.RuntimeConfig;
import de.viadee.bpm.vPAV.config.model.RuleSet;
import de.viadee.bpm.vPAV.constants.ConfigConstants;
import de.viadee.bpm.vPAV.processing.ElementGraphBuilder;
import de.viadee.bpm.vPAV.processing.JavaReaderStatic;
import de.viadee.bpm.vPAV.processing.EntryPointScanner;
import de.viadee.bpm.vPAV.processing.code.flow.*;
import de.viadee.bpm.vPAV.processing.model.graph.Graph;
import de.viadee.bpm.vPAV.processing.model.graph.Path;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.CallActivity;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaExecutionListener;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaIn;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaOut;
import org.junit.*;
import soot.Scene;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

/**
 * test the handling of call activities
 */
public class CallActivityTest {

    private static final String BASE_PATH = "src/test/resources/CallActivityTest/";

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
        Properties myProperties = new Properties();
        RuntimeConfig.getInstance().setProperties(myProperties);
    }

    @Test
    public void testEmbedding() {
        final EntryPointScanner scanner = new EntryPointScanner(null);
        final FileScanner fileScanner = new FileScanner(new RuleSet());
        final String PATH = BASE_PATH + "CallActivityTest_embeddingCallActivity.bpmn";
        final File processDefinition = new File(PATH);

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(processDefinition);

        // add reference for called process
        final Map<String, String> processIdToPathMap = new HashMap<>();
        processIdToPathMap.put("calledProcess", "CallActivityTest/CallActivityTest_calledProcess.bpmn");
        processIdToPathMap.put("calledcalledProcess", "CallActivityTest/CallActivityTest_calledcalledProcess.bpmn");

        final ElementGraphBuilder graphBuilder = new ElementGraphBuilder(null, processIdToPathMap, null, null);

        FlowAnalysis flowAnalysis = new FlowAnalysis();

        // create data flow graphs
        final Collection<String> calledElementHierarchy = new ArrayList<>();
        final Collection<Graph> graphCollection = graphBuilder.createProcessGraph(fileScanner, modelInstance,
                processDefinition.getPath(), calledElementHierarchy, scanner, flowAnalysis);

        flowAnalysis.analyze(graphCollection);

        // calculate invalid paths based on data flow graphs
        final Map<AnomalyContainer, List<Path>> invalidPathMap = graphBuilder.createInvalidPaths(graphCollection);
        Iterator<AnomalyContainer> iterator = invalidPathMap.keySet().iterator();

        Assert.assertEquals("There should be eight anomalies", 8, invalidPathMap.size());
        AnomalyContainer anomaly1 = iterator.next();
        AnomalyContainer anomaly2 = iterator.next();
        AnomalyContainer anomaly3 = iterator.next();
        AnomalyContainer anomaly4 = iterator.next();
        AnomalyContainer anomaly5 = iterator.next();
        AnomalyContainer anomaly6 = iterator.next();
        AnomalyContainer anomaly7 = iterator.next();
        AnomalyContainer anomaly8 = iterator.next();

        // var4 in sequence flow 1xq2ktt after task 2
        Assert.assertEquals("Expected a UR anomaly but got " + anomaly1.getAnomaly().toString(), Anomaly.UR,
                anomaly1.getAnomaly());
        // var3 in ServiceTask_0edbu4z in calledcalledProcess
        Assert.assertEquals("Expected a UR anomaly but got " + anomaly4.getAnomaly().toString(), Anomaly.UR,
                anomaly4.getAnomaly());
        // var1 in ServiceTask_1gq1azp in called Process
        Assert.assertEquals("Expected a UR anomaly but got " + anomaly2.getAnomaly().toString(), Anomaly.UR,
                anomaly2.getAnomaly());
        // var2 in SequenceFlow_1gfmaoe in called element
        Assert.assertEquals("Expected a DD anomaly but got " + anomaly7.getAnomaly().toString(), Anomaly.DD,
                anomaly7.getAnomaly());
        // variable2 in CallActivity_0vlq6qr in In Mapping
        Assert.assertEquals("Expected a UR anomaly but got " + anomaly3.getAnomaly().toString(), Anomaly.UR,
                anomaly3.getAnomaly());
        // var1 in ServiceTask_01owrcj in called Process
        Assert.assertEquals("Expected a UR anomaly but got " + anomaly5.getAnomaly().toString(), Anomaly.UR,
                anomaly5.getAnomaly());
        // var1 in ServiceTask_0mfcclv in called Process
        Assert.assertEquals("Expected a DD anomaly but got " + anomaly6.getAnomaly().toString(), Anomaly.DD,
                anomaly6.getAnomaly());
        // variable3 in CallActivity_0vlq6qr in Out Mapping
        Assert.assertEquals("Expected a UR anomaly but got " + anomaly8.getAnomaly().toString(), Anomaly.UR,
                anomaly8.getAnomaly());
    }

    @Test
    public void testEmbeddedWithVariableMapping() {
        // Usage of camunda:in and camunda:out
        final EntryPointScanner scanner = new EntryPointScanner(null);
        final FileScanner fileScanner = new FileScanner(new RuleSet());
        final String PATH = BASE_PATH + "CallActivityTest_TwoLevels.bpmn";
        final File processDefinition = new File(PATH);

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(processDefinition);
        CallActivity callActivity = modelInstance.getModelElementById("CallActivity");

        // add reference for called process
        final Map<String, String> processIdToPathMap = new HashMap<>();
        processIdToPathMap.put("calledProcess", "CallActivityTest/CallActivityTest_calledProcess.bpmn");
        processIdToPathMap.put("calledcalledProcess", "CallActivityTest/CallActivityTest_calledcalledProcess.bpmn");

        callActivity.builder().camundaIn("varIn", "inMapping");
        callActivity.builder().camundaOut("inMapping", "outMapping");

        final ElementGraphBuilder graphBuilder = new ElementGraphBuilder(null, processIdToPathMap, null, null);

        for (int i = 0; i < 2; i++) {
            if (i == 1) {
                // Test 2: Use source expression instead of source
                CamundaIn inVariable = callActivity.getExtensionElements().
                        getElementsQuery().filterByType(CamundaIn.class).singleResult();
                inVariable.setCamundaSourceExpression("${varIn}");
                inVariable.removeAttribute("source");
                CamundaOut outVariable = callActivity.getExtensionElements().
                        getElementsQuery().filterByType(CamundaOut.class).singleResult();
                outVariable.setCamundaSourceExpression("${inMapping}");
                outVariable.removeAttribute("source");
            }

            checkTwoLevelsAnomalies(graphBuilder, fileScanner, modelInstance, processDefinition, scanner, false);
        }
    }

    @Test
    public void testEmbeddedWithDelegateVariableMapping() {
        // Usage of camunda:in and camunda:out
        final EntryPointScanner scanner = new EntryPointScanner(null);
        final FileScanner fileScanner = new FileScanner(new RuleSet());
        final String PATH = BASE_PATH + "CallActivityTest_TwoLevels.bpmn";
        final File processDefinition = new File(PATH);

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(processDefinition);
        CallActivity callActivity = modelInstance.getModelElementById("CallActivity");

        // add reference for called process
        final Map<String, String> processIdToPathMap = new HashMap<>();
        processIdToPathMap.put("calledProcess", "CallActivityTest/CallActivityTest_calledProcess.bpmn");
        processIdToPathMap.put("calledcalledProcess", "CallActivityTest/CallActivityTest_calledcalledProcess.bpmn");

        callActivity.builder().camundaVariableMappingClass("de.viadee.bpm.vPAV.delegates.DelegatedVarMapping");

        final ElementGraphBuilder graphBuilder = new ElementGraphBuilder(null, processIdToPathMap, null, null);

        checkTwoLevelsAnomalies(graphBuilder, fileScanner, modelInstance, processDefinition, scanner, true);

        for (int i = 0; i < 2; i++) {
            if (i == 1) {
                // Test 2: Use expression instead of class
                callActivity.setCamundaVariableMappingClass(null);
                callActivity.setCamundaVariableMappingDelegateExpression("${delegatedVarMapping}");
                final Map<String, String> beanMapping = new HashMap<>();
                beanMapping.put("delegatedVarMapping", "de/viadee/bpm/vPAV/delegates/DelegatedVarMapping.class");
                RuntimeConfig.getInstance().setBeanMapping(beanMapping);
            }

            checkTwoLevelsAnomalies(graphBuilder, fileScanner, modelInstance, processDefinition, scanner, true);
        }
    }

    @Test
    public void testEmbeddingCallActivitiesWithListener() {
        final EntryPointScanner scanner = new EntryPointScanner(null);
        Properties myProperties = new Properties();
        myProperties.put("scanpath", ConfigConstants.TARGET_TEST_PATH);
        RuntimeConfig.getInstance().setProperties(myProperties);
        final FileScanner fileScanner = new FileScanner(new RuleSet());
        final String PATH = BASE_PATH + "CallActivityTest_SingleCallActivity.bpmn";
        final File processDefinition = new File(PATH);

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(processDefinition);
        CallActivity callActivity = modelInstance.getModelElementById("CallActivity");

        // add reference for called process
        final Map<String, String> processIdToPathMap = new HashMap<>();
        processIdToPathMap.put("calledElement", "CallActivityTest/CallActivityTest_calledElement.bpmn");

        final ElementGraphBuilder graphBuilder = new ElementGraphBuilder(null, processIdToPathMap, null, null);

        // Test 1: Add Start Listener
        callActivity.builder().camundaExecutionListenerClass("start",
                "de.viadee.bpm.vPAV.delegates.CallActivityListenerDelegate");

        for (int i = 0; i < 2; i++) {
            if (i == 1) {
                // Test 2: Change start listener to end listener
                CamundaExecutionListener startListener = callActivity.getExtensionElements().getElementsQuery()
                        .filterByType(CamundaExecutionListener.class).singleResult();
                startListener.setCamundaEvent("end");
            }

            // create data flow graphs
            FlowAnalysis flowAnalysis = new FlowAnalysis();
            final Collection<String> calledElementHierarchy = new ArrayList<>();
            final Collection<Graph> graphCollection = graphBuilder.createProcessGraph(fileScanner, modelInstance,
                    processDefinition.getPath(), calledElementHierarchy, scanner, flowAnalysis);

            flowAnalysis.analyze(graphCollection);
            Iterator<AnalysisElement> iterator = flowAnalysis.getNodes().values().iterator();
            AnalysisElement startEvent1 = iterator.next();
            AnalysisElement sequenceFlow1 = iterator.next();
            AnalysisElement endEvent1 = iterator.next();
            AnalysisElement sequenceFlow2 = iterator.next();
            AnalysisElement startEvent1_1 = iterator.next();
            AnalysisElement endEvent1_1 = iterator.next();
            AnalysisElement sequenceFlow1_1 = iterator.next();
            AnalysisElement sequenceFlow2_2 = iterator.next();
            AnalysisElement task1_1 = iterator.next();
            AnalysisElement ca0 = iterator.next();
            AnalysisElement ca2 = iterator.next();

            Assert.assertEquals("", 0, startEvent1.getPredecessors().size());
            Assert.assertEquals("", "StartEvent_1", sequenceFlow1.getPredecessors().get(0).getGraphId());
            Assert.assertEquals("", "SequenceFlow_2", endEvent1.getPredecessors().get(0).getGraphId());
            Assert.assertEquals("", "_SequenceFlow_2_2", endEvent1_1.getPredecessors().get(0).getGraphId());
            Assert.assertEquals("", "_StartEvent_1_1", sequenceFlow1_1.getPredecessors().get(0).getGraphId());
            Assert.assertEquals("", "_Task_1_1", sequenceFlow2_2.getPredecessors().get(0).getGraphId());
            Assert.assertEquals("", "_SequenceFlow_1_1", task1_1.getPredecessors().get(0).getGraphId());

            if (i == 0) {
                // Start Listener
                Assert.assertEquals("", "SequenceFlow_1", ca0.getPredecessors().get(0).getGraphId());
                Assert.assertEquals("", "_EndEvent_1_1", sequenceFlow2.getPredecessors().get(0).getGraphId());
                Assert.assertEquals("", "CallActivity__3", startEvent1_1.getPredecessors().get(0).getGraphId());

            } else {
                // End Listener
                Assert.assertEquals("", "_EndEvent_1_1", ca2.getPredecessors().get(0).getGraphId());
                Assert.assertEquals("", "CallActivity__2", sequenceFlow2.getPredecessors().get(0).getGraphId());
                Assert.assertEquals("", "CallActivity__0", startEvent1_1.getPredecessors().get(0).getGraphId());
            }
        }
    }

    @Test
    public void testAllVariableMapping() {
        // Create processes
        BpmnModelInstance calledProcess = Bpmn.createProcess("MyCalledProcess").startEvent("MySubStartEvent")
                .serviceTask().camundaExpression("${execution.setVariable('varOut', true)}")
                .serviceTask().camundaExpression("${execution.setVariable('varOut2', true)}")
                .endEvent().done();
        BpmnModelInstance modelInstance = Bpmn.createProcess("MyProcess").startEvent()
                .serviceTask().camundaExpression("${execution.setVariable('varIn', true)}")
                .serviceTask().camundaExpression("${execution.setVariable('varIn2', true)}")
                .callActivity("MyCallActivity")
                .calledElement("MyCalledProcess").endEvent("MyEndEvent").done();

        // Setup variable mapping
        CamundaIn inMapping = modelInstance.newInstance(CamundaIn.class);
        inMapping.setCamundaVariables("all");
        CamundaOut outMapping = modelInstance.newInstance(CamundaOut.class);
        outMapping.setCamundaVariables("all");
        CallActivity myCallActivity = modelInstance.getModelElementById("MyCallActivity");
        myCallActivity.builder().addExtensionElement(inMapping).addExtensionElement(outMapping);

        HashMap<String, BpmnModelInstance> processIdToModels = new HashMap<>();
        processIdToModels.put("MyCalledProcess", calledProcess);

        final ElementGraphBuilder graphBuilder = new ElementGraphBuilder(processIdToModels);
        FlowAnalysis flowAnalysis = new FlowAnalysis();
        final Collection<String> calledElementHierarchy = new ArrayList<>();
        final FileScanner fileScanner = new FileScanner(new RuleSet());
        final EntryPointScanner scanner = new EntryPointScanner(null);
        final Collection<Graph> graphCollection = graphBuilder.createProcessGraph(fileScanner, modelInstance,
                "MyProcess", calledElementHierarchy, scanner, flowAnalysis);

        flowAnalysis.analyze(graphCollection);

        Assert.assertTrue(flowAnalysis.getCallActivitiesInMapping().contains("MyCalledProcess"));
        Assert.assertTrue(flowAnalysis.getCallActivitiesOutMapping().contains("MyCalledProcess"));

        Assert.assertEquals(2, flowAnalysis.getNodes().get("_MySubStartEvent").getInUnused().size());
        Assert.assertEquals(4, flowAnalysis.getNodes().get("MyEndEvent").getInUnused().size());
        flowAnalysis.getNodes().get("MyEndEvent").getInUnused().forEach((key,value) -> {
            Assert.assertEquals("MyProcess", value.getScopeId());
        });

    }

    private void checkTwoLevelsAnomalies(ElementGraphBuilder graphBuilder, FileScanner fileScanner,
            BpmnModelInstance modelInstance,
            File processDefinition, EntryPointScanner scanner, boolean delegateTest) {
        FlowAnalysis flowAnalysis = new FlowAnalysis();

        // create data flow graphs
        final Collection<String> calledElementHierarchy = new ArrayList<>();
        final Collection<Graph> graphCollection = graphBuilder.createProcessGraph(fileScanner, modelInstance,
                processDefinition.getPath(), calledElementHierarchy, scanner, flowAnalysis);

        flowAnalysis.analyze(graphCollection);

        // calculate invalid paths based on data flow graphs
        final Map<AnomalyContainer, List<Path>> invalidPathMap = graphBuilder.createInvalidPaths(graphCollection);
        Iterator<AnomalyContainer> iterator = invalidPathMap.keySet().iterator();

        Assert.assertEquals("There should be exactly three anomalies.", (delegateTest) ? 4 : 3, invalidPathMap.size());
        AnomalyContainer anomaly1 = iterator.next();
        AnomalyContainer anomaly2 = iterator.next();
        AnomalyContainer anomaly3 = iterator.next();

        // var1 ServiceTask_1gq1azp
        Assert.assertEquals("Expected a UR anomaly but got " + anomaly1.getAnomaly().toString(), Anomaly.UR,
                anomaly1.getAnomaly());
        // var1 ServiceTask_01owrcj
        Assert.assertEquals("Expected a UR anomaly but got " + anomaly2.getAnomaly().toString(), Anomaly.UR,
                anomaly2.getAnomaly());
        // var3 ServiceTask_0edbu4z
        Assert.assertEquals("Expected a UR anomaly but got " + anomaly3.getAnomaly().toString(), Anomaly.UR,
                anomaly3.getAnomaly());

        if (delegateTest) {
            AnomalyContainer anomaly4 = iterator.next();
            // inMapping CallActivity
            Assert.assertEquals("Expected a UR anomaly but got " + anomaly4.getAnomaly().toString(), Anomaly.UR,
                    anomaly4.getAnomaly());
            Assert.assertEquals("There should be one input variable.", 1,
                    flowAnalysis.getNodes().get("_StartEvent_1").getInUnused().size());
        }
    }

    @Before
    public void clear() {
        IssueService.getInstance().clear();
        ProcessVariableOperation.resetIdCounter();
    }
}