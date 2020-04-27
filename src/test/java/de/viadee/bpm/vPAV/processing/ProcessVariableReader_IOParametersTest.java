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
package de.viadee.bpm.vPAV.processing;

import de.viadee.bpm.vPAV.Helper;
import de.viadee.bpm.vPAV.IssueService;
import de.viadee.bpm.vPAV.RuntimeConfig;
import de.viadee.bpm.vPAV.config.model.Rule;
import de.viadee.bpm.vPAV.constants.BpmnConstants;
import de.viadee.bpm.vPAV.processing.code.flow.BasicNode;
import de.viadee.bpm.vPAV.processing.code.flow.BpmnElement;
import de.viadee.bpm.vPAV.processing.code.flow.ControlFlowGraph;
import de.viadee.bpm.vPAV.processing.code.flow.FlowAnalysis;
import de.viadee.bpm.vPAV.processing.model.data.CheckerIssue;
import de.viadee.bpm.vPAV.processing.model.data.ProcessVariableOperation;
import de.viadee.bpm.vPAV.processing.model.data.VariableOperation;
import org.camunda.bpm.model.bpmn.instance.BpmnModelElementInstance;
import org.camunda.bpm.model.bpmn.instance.ServiceTask;
import org.camunda.bpm.model.bpmn.instance.camunda.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.LinkedHashMap;

import static de.viadee.bpm.vPAV.Helper.createElement;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProcessVariableReader_IOParametersTest {

    @BeforeClass
    public static void setup() throws MalformedURLException {
        final File file = new File(".");
        final String currentPath = file.toURI().toURL().toString();
        final URL classUrl = new URL(currentPath + "src/test/java");
        final URL[] classUrls = { classUrl };
        ClassLoader cl = new URLClassLoader(classUrls);
        RuntimeConfig.getInstance().setClassLoader(cl);
        RuntimeConfig.getInstance().getResource("en_US");
    }

    /**
     * Case: Test a model with Input Parameter of type text code
     */
    @Test
    public void testTypeText() {
        LinkedHashMap<String, BasicNode> nodes = processIOParameters(
                prepareServiceTask("testInputText", "${myVariable}",
                        null, true));
        Assert.assertEquals(2, nodes.size());
        Assert.assertEquals("myVariable", nodes.get("MyServiceTask__0").getOperations().get("myVariable_1").getName());
        Assert.assertEquals(VariableOperation.READ,
                nodes.get("MyServiceTask__0").getOperations().get("myVariable_1").getOperation());
        Assert.assertEquals("testInputText",
                nodes.get("MyServiceTask__1").getOperations().get("testInputText_0").getName());
        Assert.assertEquals(VariableOperation.WRITE,
                nodes.get("MyServiceTask__1").getOperations().get("testInputText_0").getOperation());
    }

    /**
     * Case: Test a model with Input Parameter of type list code
     */
    @Test
    public void testTypeList() {
        CamundaValue firstValue = Helper.emptyModel.newInstance(CamundaValue.class);
        firstValue.setTextContent("first");
        CamundaValue secondValue = Helper.emptyModel.newInstance(CamundaValue.class);
        secondValue.setTextContent("${myVariable}");
        CamundaList list = Helper.emptyModel.newInstance(CamundaList.class);
        list.getValues().add(firstValue);
        list.getValues().add(secondValue);

        LinkedHashMap<String, BasicNode> nodes = processIOParameters(prepareServiceTask("testInputList",
                list.getTextContent(), list, true));
        Assert.assertEquals(2, nodes.size());
        Assert.assertEquals(VariableOperation.READ,
                nodes.get("MyServiceTask__0").getOperations().get("myVariable_1").getOperation());
        Assert.assertEquals("myVariable", nodes.get("MyServiceTask__0").getOperations().get("myVariable_1").getName());
        Assert.assertEquals(VariableOperation.WRITE,
                nodes.get("MyServiceTask__1").getOperations().get("testInputList_0").getOperation());
        Assert.assertEquals("testInputList",
                nodes.get("MyServiceTask__1").getOperations().get("testInputList_0").getName());
    }

    /**
     * Case: Test a model with Input Parameter of type map code
     */
    @Test
    public void testTypeMap() {
        CamundaEntry firstEntry = Helper.emptyModel.newInstance(CamundaEntry.class);
        firstEntry.setCamundaKey("first");
        firstEntry.setTextContent("1");
        CamundaEntry secondEntry = Helper.emptyModel.newInstance(CamundaEntry.class);
        secondEntry.setCamundaKey("second");
        secondEntry.setTextContent("${myVariable}");
        CamundaMap map = Helper.emptyModel.newInstance(CamundaMap.class);
        map.getCamundaEntries().add(firstEntry);
        map.getCamundaEntries().add(secondEntry);

        LinkedHashMap<String, BasicNode> nodes = processIOParameters(prepareServiceTask("testInputMap",
                map.getTextContent(), map, true));
        Assert.assertEquals(2, nodes.size());
        Assert.assertEquals(VariableOperation.READ,
                nodes.get("MyServiceTask__0").getOperations().get("myVariable_1").getOperation());
        Assert.assertEquals("myVariable", nodes.get("MyServiceTask__0").getOperations().get("myVariable_1").getName());
        Assert.assertEquals(VariableOperation.WRITE,
                nodes.get("MyServiceTask__1").getOperations().get("testInputMap_0").getOperation());
        Assert.assertEquals("testInputMap",
                nodes.get("MyServiceTask__1").getOperations().get("testInputMap_0").getName());
    }

    /**
     * Case: Test a model with Input Parameter of type text code
     */
    @Test
    public void testInputTypeScript() {
        CamundaScript script = Helper.emptyModel.newInstance(CamundaScript.class);
        script.setCamundaScriptFormat("groovy");
        script.setTextContent("§%&&//)()()(");

        LinkedHashMap<String, BasicNode> nodes = processIOParameters(
                prepareServiceTask("testInputScript",
                        script.getTextContent(), script, true));
        Assert.assertEquals(VariableOperation.WRITE,
                nodes.get("MyServiceTask__0").getOperations().get("testInputScript_0").getOperation());
        Assert.assertEquals("testInputScript",
                nodes.get("MyServiceTask__0").getOperations().get("testInputScript_0").getName());

        Collection<CheckerIssue> issues = IssueService.getInstance().getIssues();
        Assert.assertEquals(1, issues.size());

    }

    @Test
    public void testInputScope() {
        LinkedHashMap<String, BasicNode> nodes = analyzeInputOutputParameters(
                prepareServiceTask("testInputText", "${myVariable}",
                        null, true), true);
        Assert.assertEquals("Process_1",
                nodes.get("MyServiceTask__0").getOperations().get("myVariable_1").getScopeId());
        Assert.assertEquals("MyServiceTask",
                nodes.get("MyServiceTask__1").getOperations().get("testInputText_0").getScopeId());
    }

    @Test
    public void testOutputScope() {
        LinkedHashMap<String, BasicNode> nodes = analyzeInputOutputParameters(
                prepareServiceTask("testOutputText", "${myVariable}",
                        null, false), false);
        Assert.assertEquals("MyServiceTask",
                nodes.get("MyServiceTask__0").getOperations().get("myVariable_1").getScopeId());
        Assert.assertEquals("Process_1",
                nodes.get("MyServiceTask__1").getOperations().get("testOutputText_0").getScopeId());
    }

    public LinkedHashMap<String, BasicNode> processIOParameters(ServiceTask task) {
        final BpmnElement element = new BpmnElement("", task, new ControlFlowGraph(),
                new FlowAnalysis());
        (new ProcessVariableReader(null, mock(Rule.class)))
                .processInputOutputParameters(element, element.getBaseElement().getExtensionElements(),
                        new BasicNode[1], true);

        return element.getControlFlowGraph().getNodes();
    }

    public ServiceTask prepareServiceTask(String name,
            String textContent,
            BpmnModelElementInstance value, boolean input) {

        ServiceTask task = createElement("MyServiceTask", ServiceTask.class);
        CamundaInputOutput ioExtension = createElement("MyIOExtension", CamundaInputOutput.class);

        if (input) {
            CamundaInputParameter inputParameter = createElement("MyInputParameter", CamundaInputParameter.class);
            inputParameter.setCamundaName(name);
            inputParameter.setTextContent(textContent);
            if (value != null)
                inputParameter.setValue(value);
            ioExtension.addChildElement(inputParameter);
        } else {
            CamundaOutputParameter outputParameter = createElement("MyOutputParameter", CamundaOutputParameter.class);
            outputParameter.setCamundaName(name);
            outputParameter.setTextContent(textContent);
            if (value != null)
                outputParameter.setValue(value);
            ioExtension.addChildElement(outputParameter);
        }
        task.builder().addExtensionElement(ioExtension);

        ServiceTask spyTask = Mockito.spy(task);
        BpmnModelElementInstance parent = mock(BpmnModelElementInstance.class);
        when(parent.getAttributeValue(BpmnConstants.ATTR_ID)).thenReturn("Process_1");
        Mockito.doReturn(parent).when(spyTask).getScope();
        return spyTask;
    }

    public LinkedHashMap<String, BasicNode> analyzeInputOutputParameters(ServiceTask task, boolean input) {
        final BpmnElement element = new BpmnElement("", task, new ControlFlowGraph(),
                new FlowAnalysis());

        // TODO check predecessor
        (new ProcessVariableReader(null, null))
                .processInputOutputParameters(element, element.getBaseElement().getExtensionElements(),
                        new BasicNode[1], input);

        return element.getControlFlowGraph().getNodes();
    }

    @Before
    public void clearIssues() {
        IssueService.getInstance().clear();
        ProcessVariableOperation.resetIdCounter();
    }
}
