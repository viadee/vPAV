/**
 * BSD 3-Clause License
 * <p>
 * Copyright © 2019, viadee Unternehmensberatung AG
 * All rights reserved.
 * <p>
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * <p>
 * * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 * <p>
 * * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * <p>
 * * Neither the name of the copyright holder nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * <p>
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

import de.viadee.bpm.vPAV.*;
import de.viadee.bpm.vPAV.config.model.Rule;
import de.viadee.bpm.vPAV.config.model.RuleSet;
import de.viadee.bpm.vPAV.constants.ConfigConstants;
import de.viadee.bpm.vPAV.processing.code.flow.*;
import de.viadee.bpm.vPAV.processing.model.data.*;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.BpmnModelElementInstance;
import org.camunda.bpm.model.bpmn.instance.ServiceTask;
import org.camunda.bpm.model.bpmn.instance.camunda.*;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaScript;
import org.junit.*;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

import static org.mockito.Mockito.mock;

public class ProcessVariableMappingTest {

    private static final String BASE_PATH = "src/test/resources/";

    private static final String MODEL_PATH = BASE_PATH + "ModelWithMappingDelegate.bpmn";

    private File processDefinition;

    @BeforeClass
    public static void setup() throws MalformedURLException {
        final File file = new File(".");
        final String currentPath = file.toURI().toURL().toString();
        final URL classUrl = new URL(currentPath + "src/test/java");
        final URL[] classUrls = { classUrl };
        ClassLoader cl = new URLClassLoader(classUrls);
        RuntimeConfig.getInstance().setClassLoader(cl);
        RuntimeConfig.getInstance().getResource("en_US");
        RuntimeConfig.getInstance().setTest(true);
        final Map<String, String> beanMapping = new HashMap<>();
        beanMapping.put("mappingDelegate", "de/viadee/bpm/vPAV/delegates/MappingDelegate.class");
        RuntimeConfig.getInstance().setBeanMapping(beanMapping);
    }

    /**
     * Case: Test a model with Input Mapping of type text code
     */
    @Test
    public void testTypeText() {
        BpmnModelInstance modelInstance = createModelInstance();
        LinkedHashMap<String, BasicNode> nodes = processMapping(
                prepareServiceTask(modelInstance, "testInputText", "${myVariable}",
                        null, true));
        Assert.assertEquals(2, nodes.size());
        Assert.assertEquals("myVariable", nodes.get("Task_0wh7kto__0").getOperations().get("myVariable_1").getName());
        Assert.assertEquals(VariableOperation.READ,
                nodes.get("Task_0wh7kto__0").getOperations().get("myVariable_1").getOperation());
        Assert.assertEquals("testInputText",
                nodes.get("Task_0wh7kto__1").getOperations().get("testInputText_0").getName());
        Assert.assertEquals(VariableOperation.WRITE,
                nodes.get("Task_0wh7kto__1").getOperations().get("testInputText_0").getOperation());
    }

    /**
     * Case: Test a model with Input Mapping of type list code
     */
    @Test
    public void testTypeList() {
        BpmnModelInstance modelInstance = createModelInstance();

        CamundaValue firstValue = modelInstance.newInstance(CamundaValue.class);
        firstValue.setTextContent("first");
        CamundaValue secondValue = modelInstance.newInstance(CamundaValue.class);
        secondValue.setTextContent("${myVariable}");
        CamundaList list = modelInstance.newInstance(CamundaList.class);
        list.getValues().add(firstValue);
        list.getValues().add(secondValue);

        LinkedHashMap<String, BasicNode> nodes = processMapping(prepareServiceTask(modelInstance, "testInputList",
                list.getTextContent(), list, true));
        Assert.assertEquals(2, nodes.size());
        Assert.assertEquals(VariableOperation.READ,
                nodes.get("Task_0wh7kto__0").getOperations().get("myVariable_1").getOperation());
        Assert.assertEquals("myVariable", nodes.get("Task_0wh7kto__0").getOperations().get("myVariable_1").getName());
        Assert.assertEquals(VariableOperation.WRITE,
                nodes.get("Task_0wh7kto__1").getOperations().get("testInputList_0").getOperation());
        Assert.assertEquals("testInputList",
                nodes.get("Task_0wh7kto__1").getOperations().get("testInputList_0").getName());
    }

    /**
     * Case: Test a model with Input Mapping of type map code
     */
    @Test
    public void testTypeMap() {
        BpmnModelInstance modelInstance = createModelInstance();

        CamundaEntry firstEntry = modelInstance.newInstance(CamundaEntry.class);
        firstEntry.setCamundaKey("first");
        firstEntry.setTextContent("1");
        CamundaEntry secondEntry = modelInstance.newInstance(CamundaEntry.class);
        secondEntry.setCamundaKey("second");
        secondEntry.setTextContent("${myVariable}");
        CamundaMap map = modelInstance.newInstance(CamundaMap.class);
        map.getCamundaEntries().add(firstEntry);
        map.getCamundaEntries().add(secondEntry);

        LinkedHashMap<String, BasicNode> nodes = processMapping(prepareServiceTask(modelInstance, "testInputMap",
                map.getTextContent(), map, true));
        Assert.assertEquals(2, nodes.size());
        Assert.assertEquals(VariableOperation.READ,
                nodes.get("Task_0wh7kto__0").getOperations().get("myVariable_1").getOperation());
        Assert.assertEquals("myVariable", nodes.get("Task_0wh7kto__0").getOperations().get("myVariable_1").getName());
        Assert.assertEquals(VariableOperation.WRITE,
                nodes.get("Task_0wh7kto__1").getOperations().get("testInputMap_0").getOperation());
        Assert.assertEquals("testInputMap",
                nodes.get("Task_0wh7kto__1").getOperations().get("testInputMap_0").getName());
    }

    /**
     * Case: Test a model with Input Mapping of type text code
     */
    @Test
    public void testInputTypeScript() {
        BpmnModelInstance modelInstance = createModelInstance();

        CamundaScript script = modelInstance.newInstance(CamundaScript.class);
        script.setCamundaScriptFormat("groovy");
        script.setTextContent("§%&&//)()()(");

        LinkedHashMap<String, BasicNode> nodes = processMapping(prepareServiceTask(modelInstance, "testInputScript",
                script.getTextContent(), script, true));
        Assert.assertEquals(VariableOperation.WRITE,
                nodes.get("Task_0wh7kto__0").getOperations().get("testInputScript_0").getOperation());
        Assert.assertEquals("testInputScript", nodes.get("Task_0wh7kto__0").getOperations().get("testInputScript_0").getName());

        Collection<CheckerIssue> issues = IssueService.getInstance().getIssues();
        Assert.assertEquals(1, issues.size());

    }

    @Test
    public void testInputScope() {
        BpmnModelInstance modelInstance = createModelInstance();
        LinkedHashMap<String, BasicNode> nodes = analyzeInputOutputMapping(prepareServiceTask(modelInstance, "testInputText", "${myVariable}",
                null, true), true);
        Assert.assertEquals("Process_1", nodes.get("Task_0wh7kto__0").getOperations().get("myVariable_1").getScopeId());
        Assert.assertEquals("Task_0wh7kto",
                nodes.get("Task_0wh7kto__1").getOperations().get("testInputText_0").getScopeId());
    }

    @Test
    public void testOutputScope() {
        BpmnModelInstance modelInstance = createModelInstance();
        LinkedHashMap<String, BasicNode> nodes = analyzeInputOutputMapping(prepareServiceTask(modelInstance, "testOutputText", "${myVariable}",
                null, false), false);
        Assert.assertEquals("Task_0wh7kto", nodes.get("Task_0wh7kto__0").getOperations().get("myVariable_1").getScopeId());
        Assert.assertEquals("Process_1",
                nodes.get("Task_0wh7kto__1").getOperations().get("testOutputText_0").getScopeId());
    }

    public BpmnModelInstance createModelInstance() {
        final String PATH = BASE_PATH + "ModelWithMappingDelegate.bpmn";
        processDefinition = new File(PATH);
        FileScanner fileScanner = new FileScanner(new RuleSet());
        fileScanner.setScanPath(ConfigConstants.TEST_JAVAPATH);
        final Set<String> testSet = new HashSet<>();
        testSet.add("de/viadee/bpm/vPAV/delegates/MappingDelegate.java");
        fileScanner.setJavaResourcesFileInputStream(testSet);
        ProcessVariablesScanner scanner = new ProcessVariablesScanner(
                fileScanner.getJavaResourcesFileInputStream());
        scanner.scanProcessVariables();
        return Bpmn.readModelFromFile(processDefinition);
    }

    public LinkedHashMap<String, BasicNode> processMapping(ServiceTask task) {
        final BpmnElement node = new BpmnElement(processDefinition.getPath(), task, new ControlFlowGraph(),
                new FlowAnalysis());
        ExpressionNode expNode = new ExpressionNode(node, "", ElementChapter.InputOutput,
                KnownElementFieldType.CamundaIn);
        (new ProcessVariableReader(null, mock(Rule.class), null)).processMapping(node, expNode, new BasicNode[1], "Process_1",true);

        return node.getControlFlowGraph().getNodes();
    }

    public ServiceTask prepareServiceTask(BpmnModelInstance modelInstance, String name,
            String textContent,
            BpmnModelElementInstance value, boolean input) {
        ServiceTask task = modelInstance.getModelElementById("Task_0wh7kto");

        if (input) {
            CamundaInputParameter inputParameter = modelInstance.newInstance(CamundaInputParameter.class);
            inputParameter.setCamundaName(name);
            inputParameter.setTextContent(textContent);
            if (value != null)
                inputParameter.setValue(value);
            task.builder().addExtensionElement(inputParameter);
        } else {
            CamundaOutputParameter outputParameter = modelInstance.newInstance(CamundaOutputParameter.class);
            outputParameter.setCamundaName(name);
            outputParameter.setTextContent(textContent);
            if (value != null)
                outputParameter.setValue(value);
            task.builder().addExtensionElement(outputParameter);
        }

        return task;
    }

    public LinkedHashMap<String, BasicNode> analyzeInputOutputMapping(ServiceTask task, boolean input) {
        final BpmnElement node = new BpmnElement(processDefinition.getPath(), task, new ControlFlowGraph(),
                new FlowAnalysis());
        if (input) {
            (new ProcessVariableReader(null, null, null)).getVariablesFromInputMapping(node, new BasicNode[1]);

        } else {
            (new ProcessVariableReader(null, null, null)).getVariablesFromOutputMapping(node, new BasicNode[1]);

        }
        return node.getControlFlowGraph().getNodes();
    }

    @Before
    public void clearIssues() {
        IssueService.getInstance().clear();
        ProcessVariableOperation.resetIdCounter();
    }

}
