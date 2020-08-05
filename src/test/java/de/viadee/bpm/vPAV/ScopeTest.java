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
package de.viadee.bpm.vPAV;

import de.viadee.bpm.vPAV.config.model.RuleSet;
import de.viadee.bpm.vPAV.constants.ConfigConstants;
import de.viadee.bpm.vPAV.processing.ElementGraphBuilder;
import de.viadee.bpm.vPAV.processing.JavaReaderStatic;
import de.viadee.bpm.vPAV.processing.ProcessVariableReader;
import de.viadee.bpm.vPAV.processing.ProcessVariablesScanner;
import de.viadee.bpm.vPAV.processing.code.flow.BasicNode;
import de.viadee.bpm.vPAV.processing.code.flow.BpmnElement;
import de.viadee.bpm.vPAV.processing.code.flow.ControlFlowGraph;
import de.viadee.bpm.vPAV.processing.code.flow.FlowAnalysis;
import de.viadee.bpm.vPAV.processing.model.data.ProcessVariableOperation;
import de.viadee.bpm.vPAV.processing.model.data.VariableOperation;
import de.viadee.bpm.vPAV.processing.model.graph.Graph;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.BaseElement;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import soot.Scene;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

public class ScopeTest {

    @BeforeClass
    public static void setupSoot() throws MalformedURLException {
        final File file = new File(".");
        final String currentPath = file.toURI().toURL().toString();
        final URL classUrl = new URL(currentPath + "src/test/java");
        final URL[] classUrls = { classUrl };
        ClassLoader cl = new URLClassLoader(classUrls);
        RuntimeConfig.getInstance().setClassLoader(cl);
        RuntimeConfig.getInstance().getResource("en_US");
        RuntimeConfig.getInstance().setTest(true);
        FileScanner.setupSootClassPaths(new LinkedList<>());
        JavaReaderStatic.setupSoot();
        Scene.v().loadNecessaryClasses();
    }

    @Before
    public void setupProperties() {
        Properties myProperties = new Properties();
        myProperties.put("scanpath", ConfigConstants.TARGET_TEST_PATH);
        RuntimeConfig.getInstance().setProperties(myProperties);
    }

    @Test
    public void testScopeMultiInstanceTasks() {
        BpmnModelInstance modelInstance = Bpmn.createProcess("MyProcess").startEvent().
                serviceTask("MyServiceTask").multiInstance().camundaCollection("collection")
                .camundaElementVariable("loopElement").multiInstanceDone()
                .endEvent().done();
        ProcessVariableReader reader = new ProcessVariableReader(null, null);
        BpmnElement element = getBpmnElement(modelInstance.getModelElementById("MyServiceTask"));

        reader.getVariablesFromElement(element, new BasicNode[1]);
        Assert.assertEquals(2, element.getControlFlowGraph().getNodes().size());
        Iterator<BasicNode> nodes = element.getControlFlowGraph().getNodes().values().iterator();
        BasicNode defaultVariables = nodes.next();
        BasicNode collection = nodes.next();
        // Multi instance variables are only available within the task
        Assert.assertEquals("MyServiceTask", defaultVariables.getDefined().values().iterator().next().getScopeId());
        Assert.assertEquals("loopElement", collection.getDefined().values().iterator().next().getName());
        Assert.assertEquals("MyServiceTask", collection.getDefined().values().iterator().next().getScopeId());
    }

    @Test
    public void testScopeExecutionListener() {
        BpmnModelInstance modelInstance = Bpmn.createProcess("MyProcess").startEvent().serviceTask("MyServiceTask")
                .camundaExecutionListenerExpression("start", "${execution.setVariable('var',true)}").endEvent().done();
        ProcessVariableReader reader = new ProcessVariableReader(null, null);
        BpmnElement element = getBpmnElement(modelInstance.getModelElementById("MyServiceTask"));

        reader.getVariablesFromElement(element, new BasicNode[1]);
        Assert.assertEquals(1, element.getControlFlowGraph().getNodes().size());
        // Variables are normally set globally
        Assert.assertEquals("MyProcess",
                element.getControlFlowGraph().getNodes().values().iterator().next().getDefined().values().iterator()
                        .next()
                        .getScopeId());
    }

    @Test
    public void testScopeTaskListener() {
        // TODO test resolving of task in java classes
        BpmnModelInstance modelInstance = Bpmn.createProcess("MyProcess").startEvent()
                .userTask("MyUserTask").camundaTaskListenerExpression("create", "${task.setVariable('var', true)}")
                .endEvent()
                .done();
        ProcessVariableReader reader = new ProcessVariableReader(null, null);
        BpmnElement element = getBpmnElement(modelInstance.getModelElementById("MyUserTask"));

        reader.getVariablesFromElement(element, new BasicNode[1]);
        Assert.assertEquals(1, element.getControlFlowGraph().getNodes().size());
        // Variables are normally set globally
        Assert.assertEquals("var",
                element.getControlFlowGraph().getNodes().values().iterator().next().getDefined().values().iterator()
                        .next()
                        .getName());
        Assert.assertEquals("MyProcess",
                element.getControlFlowGraph().getNodes().values().iterator().next().getDefined().values().iterator()
                        .next()
                        .getScopeId());
    }

    @Test
    public void testScopeDelegate() {
        BpmnModelInstance modelInstance = Bpmn.createProcess("MyProcess").startEvent().serviceTask("MyServiceTask")
                .camundaClass("de.viadee.bpm.vPAV.delegates.TestDelegate")
                .endEvent().done();
        ProcessVariableReader reader = new ProcessVariableReader(null, null);
        BpmnElement element = getBpmnElement(modelInstance.getModelElementById("MyServiceTask"));

        reader.getVariablesFromElement(element, new BasicNode[1]);
        // Variables are normally set globally
        Assert.assertEquals(1, element.getControlFlowGraph().getNodes().size());
        Assert.assertEquals("MyProcess",
                element.getControlFlowGraph().getNodes().values().iterator().next().getDefined().values().iterator()
                        .next()
                        .getScopeId());
    }

    @Test
    public void testScopeIOParameter() {
        BpmnModelInstance modelInstance = Bpmn.createProcess("MyProcess").startEvent().serviceTask("MyServiceTask")
                .camundaInputParameter("myInputParameter", "${globalReadVariable}")
                .camundaOutputParameter("myOutputParameter", "myValue")
                .endEvent().done();
        ProcessVariableReader reader = new ProcessVariableReader(null, null);
        BpmnElement element = getBpmnElement(modelInstance.getModelElementById("MyServiceTask"));

        reader.getVariablesFromElement(element, new BasicNode[1]);
        Assert.assertEquals(3, element.getControlFlowGraph().getNodes().size());
        Iterator<BasicNode> iterator = element.getControlFlowGraph().getNodes().values().iterator();
        BasicNode inputNodeValue = iterator.next();
        BasicNode inputNodeName = iterator.next();
        BasicNode outputNode = iterator.next();
        // Input parameter is only available in Service Task
        Assert.assertEquals("myInputParameter", inputNodeName.getDefined().values().iterator().next().getName());
        Assert.assertEquals("MyServiceTask", inputNodeName.getDefined().values().iterator().next().getScopeId());
        Assert.assertEquals("globalReadVariable", inputNodeValue.getUsed().values().iterator().next().getName());
        Assert.assertEquals("MyProcess", inputNodeValue.getUsed().values().iterator().next().getScopeId());

        // Output parameter is globally accessible
        Assert.assertEquals("myOutputParameter", outputNode.getDefined().values().iterator().next().getName());
        Assert.assertEquals("MyProcess", outputNode.getDefined().values().iterator().next().getScopeId());
    }

    @Test
    public void testScopeVariableMapping() {
        BpmnModelInstance modelInstance = Bpmn.createProcess("MyProcess").startEvent().callActivity("MyCallActivity")
                .camundaVariableMappingClass("de.viadee.bpm.vPAV.delegates.DelegatedVarMapping")
                .calledElement("MyCalledProcess").endEvent().done();

        ProcessVariableReader reader = new ProcessVariableReader(null, null);
        BpmnElement element = getBpmnElement(modelInstance.getModelElementById("MyCallActivity"));

        reader.getVariablesFromElement(element, new BasicNode[1]);
        Assert.assertEquals(2, element.getControlFlowGraph().getNodes().size());
        Iterator<BasicNode> iterator = element.getControlFlowGraph().getNodes().values().iterator();
        BasicNode inputVariables = iterator.next();
        BasicNode outputVariables = iterator.next();
        // In mapped variable is only available in called process
        Assert.assertEquals("inMapping", inputVariables.getDefined().values().iterator().next().getName());
        Assert.assertEquals("MyCalledProcess", inputVariables.getDefined().values().iterator().next().getScopeId());

        // Out mapped variable is only available in normal process
        Assert.assertEquals("outMapping", outputVariables.getDefined().values().iterator().next().getName());
        Assert.assertEquals("MyProcess", outputVariables.getDefined().values().iterator().next().getScopeId());
    }

    @Test
    public void testScopeSubprocess() {
        // Tested: delegate, input parameter, output parameter, listener, multi instance, task listener
        // TODO add more elements which are tested
        // Variables in Subprocesses are globally available if  execution.setVariableLocal is not used (not supported yet)
        BpmnModelInstance modelInstance = Bpmn.createProcess("MyProcess").startEvent().subProcess()
                .embeddedSubProcess().startEvent()
                .serviceTask("MyFirstTask").camundaExpression("${execution.setVariable('test', true)}")
                .serviceTask("MySecondTask").camundaInputParameter("aNewValue", "myValue")
                .camundaOutputParameter("outputVar", "1234")
                .camundaExecutionListenerExpression("start", "${execution.setVariable('var', true)}").endEvent()
                .userTask("MyMultiInstanceTask").multiInstance().cardinality("5").sequential().multiInstanceDone()
                .userTask("MyTaskListener")
                .camundaTaskListenerExpression("create", "${task.setVariable('varTaskListener', true)}").endEvent()
                .subProcessDone()
                .endEvent()
                .done();

        ProcessVariableReader reader = new ProcessVariableReader(null, null);

        // Expression (Implementation)
        BpmnElement element = getBpmnElement(modelInstance.getModelElementById("MyFirstTask"));
        reader.getVariablesFromElement(element, new BasicNode[1]);
        Assert.assertEquals(1, element.getControlFlowGraph().getNodes().size());
        Assert.assertEquals("test",
                element.getControlFlowGraph().getNodes().values().iterator().next().getDefined().values().iterator()
                        .next().getName());
        Assert.assertEquals("MyProcess",
                element.getControlFlowGraph().getNodes().values().iterator().next().getDefined().values().iterator()
                        .next().getScopeId());

        // Input/Output Parameter, Execution Listener
        BpmnElement element2 = getBpmnElement(modelInstance.getModelElementById("MySecondTask"));
        reader.getVariablesFromElement(element2, new BasicNode[1]);
        Iterator<ProcessVariableOperation> defined = element2.getControlFlowGraph().getOperations().values().iterator();
        ProcessVariableOperation inputParameter = defined.next();
        ProcessVariableOperation listener = defined.next();
        ProcessVariableOperation outputParameter = defined.next();
        Assert.assertEquals(3, element2.getControlFlowGraph().getNodes().size());
        Assert.assertEquals("aNewValue", inputParameter.getName());
        Assert.assertEquals("MySecondTask", inputParameter.getScopeId());
        Assert.assertEquals("outputVar", outputParameter.getName());
        Assert.assertEquals("MyProcess", outputParameter.getScopeId());
        Assert.assertEquals("var", listener.getName());
        Assert.assertEquals("MyProcess", listener.getScopeId());

        // Multi-instance tasks
        BpmnElement multiInstanceTask = getBpmnElement(modelInstance.getModelElementById("MyMultiInstanceTask"));
        reader.getVariablesFromElement(multiInstanceTask, new BasicNode[1]);
        Assert.assertEquals(1, multiInstanceTask.getControlFlowGraph().getNodes().size());
        Assert.assertEquals("nrOfInstances",
                multiInstanceTask.getControlFlowGraph().getNodes().values().iterator().next().getDefined().values()
                        .iterator()
                        .next().getName());
        Assert.assertEquals("MyMultiInstanceTask",
                multiInstanceTask.getControlFlowGraph().getNodes().values().iterator().next().getDefined().values()
                        .iterator()
                        .next().getScopeId());

        // Task Listener
        BpmnElement taskListener = getBpmnElement(modelInstance.getModelElementById("MyTaskListener"));

        reader.getVariablesFromElement(taskListener, new BasicNode[1]);
        Assert.assertEquals(1, taskListener.getControlFlowGraph().getNodes().size());
        Assert.assertEquals("varTaskListener",
                taskListener.getControlFlowGraph().getNodes().values().iterator().next().getDefined().values()
                        .iterator()
                        .next()
                        .getName());
        Assert.assertEquals("MyProcess",
                taskListener.getControlFlowGraph().getNodes().values().iterator().next().getDefined().values()
                        .iterator()
                        .next()
                        .getScopeId());
    }

    @Test
    public void testScopeCallActivity() {
        BpmnModelInstance modelInstance = Bpmn.createProcess().startEvent().callActivity()
                .calledElement("calledProcess")
                .endEvent().done();

        final Map<String, String> processIdToPathMap = new HashMap<>();
        processIdToPathMap.put("calledProcess", "ModelWithWriteExpression.bpmn");
        ElementGraphBuilder graphBuilder = new ElementGraphBuilder(null, processIdToPathMap);
        FlowAnalysis flowAnalysis = new FlowAnalysis();
        FileScanner fileScanner = new FileScanner(new RuleSet());
        Collection<Graph> graphs = graphBuilder.createProcessGraph(fileScanner, modelInstance, "", new ArrayList<>(),
                new ProcessVariablesScanner(null), flowAnalysis);

        flowAnalysis.analyze(graphs);
        Assert.assertEquals("test",
                flowAnalysis.getNodes().get("MyServiceTask__0").getDefined().values().iterator().next().getName());
        Assert.assertEquals("calledProcess",
                flowAnalysis.getNodes().get("MyServiceTask__0").getDefined().values().iterator().next().getScopeId());
    }

    @Test
    public void testScopeMessages() {
        // Setup bean mapping because variables cannot be directly modified in the message name
        // expression as it must return a string
        final Map<String, String> beanMapping = new HashMap<>();
        beanMapping.put("msgNameDelegate", "de.viadee.bpm.vPAV.delegates.SignalMessageNameDelegate");
        RuntimeConfig.getInstance().setBeanMapping(beanMapping);

        BpmnModelInstance modelInstance = Bpmn.createProcess("MyProcess").startEvent().serviceTask()
                .intermediateThrowEvent("MyThrowEvent").message("${msgNameDelegate.giveMeTheName(execution)}")
                .endEvent().done();
        ProcessVariableReader reader = new ProcessVariableReader(null, null);
        BpmnElement element = getBpmnElement(modelInstance.getModelElementById("MyThrowEvent"));

        reader.getVariablesFromElement(element, new BasicNode[1]);
        Assert.assertEquals(1, element.getControlFlowGraph().getNodes().size());

        Iterator<BasicNode> iterator = element.getControlFlowGraph().getNodes().values().iterator();
        BasicNode node = iterator.next();
        Assert.assertEquals("variable", node.getDefined().get("variable_0").getName());
        Assert.assertEquals(VariableOperation.WRITE, node.getDefined().get("variable_0").getOperation());
        Assert.assertEquals("MyProcess", node.getDefined().get("variable_0").getScopeId());
    }

    @Test
    public void testScopeSignals() {
        // Setup bean mapping because variables cannot be directly modified in the message name
        // expression as it must return a string
        final Map<String, String> beanMapping = new HashMap<>();
        beanMapping.put("signalNameDelegate", "de.viadee.bpm.vPAV.delegates.SignalMessageNameDelegate");
        RuntimeConfig.getInstance().setBeanMapping(beanMapping);

        BpmnModelInstance modelInstance = Bpmn.createProcess("MyProcess").startEvent().serviceTask()
                .intermediateThrowEvent("MyThrowEvent").signal("${signalNameDelegate.giveMeTheName(execution)}")
                .endEvent().done();
        ProcessVariableReader reader = new ProcessVariableReader(null, null);
        BpmnElement element = getBpmnElement(modelInstance.getModelElementById("MyThrowEvent"));

        reader.getVariablesFromElement(element, new BasicNode[1]);
        Assert.assertEquals(1, element.getControlFlowGraph().getNodes().size());

        Iterator<BasicNode> iterator = element.getControlFlowGraph().getNodes().values().iterator();
        BasicNode node = iterator.next();
        Assert.assertEquals("variable", node.getDefined().get("variable_0").getName());
        Assert.assertEquals(VariableOperation.WRITE, node.getDefined().get("variable_0").getOperation());
        Assert.assertEquals("MyProcess", node.getDefined().get("variable_0").getScopeId());
    }

    @Test
    public void testScopeFormData() {
        // Setup bean mapping because variables cannot be directly modified in the message name
        // expression as it must return a string
        final Map<String, String> beanMapping = new HashMap<>();
        beanMapping.put("signalNameDelegate", "de.viadee.bpm.vPAV.delegates.SignalMessageNameDelegate");
        RuntimeConfig.getInstance().setBeanMapping(beanMapping);

        BpmnModelInstance modelInstance = Bpmn.createProcess("MyProcess").startEvent()
                .userTask("MyUserTask").camundaFormField().camundaId("firstname")
                .camundaFormFieldDone()
                .endEvent().done();
        ProcessVariableReader reader = new ProcessVariableReader(null, null);
        BpmnElement element = getBpmnElement(modelInstance.getModelElementById("MyUserTask"));

        reader.getVariablesFromElement(element, new BasicNode[1]);
        Assert.assertEquals(1, element.getControlFlowGraph().getNodes().size());

        Iterator<BasicNode> iterator = element.getControlFlowGraph().getNodes().values().iterator();
        BasicNode formField = iterator.next();
        Assert.assertEquals("firstname", formField.getDefined().get("firstname_0").getName());
        Assert.assertEquals(VariableOperation.WRITE, formField.getDefined().get("firstname_0").getOperation());
        Assert.assertEquals("MyProcess", formField.getDefined().get("firstname_0").getScopeId());
    }

    @Test
    public void testScopeConsideredInAnalysisInputParameters() {
        ElementGraphBuilder graphBuilder = new ElementGraphBuilder(null, null);
        FlowAnalysis flowAnalysis = new FlowAnalysis();
        FileScanner fileScanner = new FileScanner(new RuleSet());

        // Test that input parameters are not passed beyond scope
        BpmnModelInstance modelInstance = Bpmn.createProcess("MyProcess").startEvent().serviceTask("MyServiceTask")
                .camundaInputParameter("myInputParameter", "${globalReadVariable}")
                .camundaOutputParameter("myOutputParameter", "myValue").sequenceFlowId("MySequenceFlow")
                .endEvent().done();
        Collection<Graph> graphs = graphBuilder.createProcessGraph(fileScanner, modelInstance, "", new ArrayList<>(),
                new ProcessVariablesScanner(null), flowAnalysis);
        flowAnalysis.analyze(graphs);

        // Only myOutputParameter is accessible after the service task
        Assert.assertEquals(1, flowAnalysis.getNodes().get("MySequenceFlow").getInUnused().size());
        Assert.assertEquals("myOutputParameter",
                flowAnalysis.getNodes().get("MySequenceFlow").getInUnused().values().iterator().next().getName());
        Assert.assertEquals(0, flowAnalysis.getNodes().get("MySequenceFlow").getInUsed().size());
    }

    @Test
    public void testScopeConsideredInAnalysisSubprocess() {
        ElementGraphBuilder graphBuilder = new ElementGraphBuilder(null, null);
        FlowAnalysis flowAnalysis = new FlowAnalysis();
        FileScanner fileScanner = new FileScanner(new RuleSet());

        // Test that global variables are accessible in subprocesses and that variables are accessible outside
        BpmnModelInstance modelInstance = Bpmn.createProcess("MyProcess").startEvent().serviceTask()
                .camundaExpression("${execution.setVariable('globalVar', true)}").subProcess()
                .embeddedSubProcess().startEvent("MyStartEvent")
                .serviceTask("MyServiceTask").camundaExpression("${execution.setVariable('test', true)}").endEvent()
                .subProcessDone()
                .endEvent("MyEndEvent")
                .done();
        flowAnalysis = new FlowAnalysis();
        Collection<Graph> graphs = graphBuilder.createProcessGraph(fileScanner, modelInstance, "", new ArrayList<>(),
                new ProcessVariablesScanner(null), flowAnalysis);
        flowAnalysis.analyze(graphs);
        Assert.assertEquals(1, flowAnalysis.getNodes().get("MyServiceTask__0").getInUnused().size());
        Assert.assertEquals("globalVar",
                flowAnalysis.getNodes().get("MyServiceTask__0").getInUnused().values().iterator().next().getName());
        Assert.assertEquals(2, flowAnalysis.getNodes().get("MyEndEvent").getInUnused().size());
        Iterator<ProcessVariableOperation> iter = flowAnalysis.getNodes().get("MyEndEvent").getInUnused().values()
                .iterator();
        Assert.assertEquals("globalVar", iter.next().getName());
        Assert.assertEquals("test", iter.next().getName());
    }

    @Test
    public void testScopeConsideredInAnalysisCallActivity() {
        final Map<String, String> processIdToPathMap = new HashMap<>();
        processIdToPathMap.put("calledProcess", "ModelWithWriteExpression.bpmn");
        ElementGraphBuilder graphBuilder = new ElementGraphBuilder(null, processIdToPathMap);
        FlowAnalysis flowAnalysis = new FlowAnalysis();
        FileScanner fileScanner = new FileScanner(new RuleSet());

        // Test that variables in called processes are scoped
        BpmnModelInstance modelInstance = Bpmn.createProcess().startEvent().serviceTask()
                .camundaExpression("${execution.setVariable('globalVar',true)}").callActivity()
                .calledElement("calledProcess")
                .endEvent("MyEndEvent").done();
        Collection<Graph> graphs = graphBuilder.createProcessGraph(fileScanner, modelInstance, "", new ArrayList<>(),
                new ProcessVariablesScanner(null), flowAnalysis);
        flowAnalysis.analyze(graphs);

        // Variable set in caller process is not available in called process
        Assert.assertEquals(0, flowAnalysis.getNodes().get("MyServiceTask__0").getInUnused().size());
        Assert.assertEquals(0, flowAnalysis.getNodes().get("MyServiceTask__0").getInUsed().size());

        // Variable set in called process is not available in caller process
        Assert.assertEquals(1, flowAnalysis.getNodes().get("MyEndEvent").getInUnused().size());
        Assert.assertEquals("globalVar",
                flowAnalysis.getNodes().get("MyEndEvent").getInUnused().values().iterator().next().getName());
    }

    private BpmnElement getBpmnElement(BaseElement element) {
        return new BpmnElement(null, element, new ControlFlowGraph(), new FlowAnalysis());
    }

}
