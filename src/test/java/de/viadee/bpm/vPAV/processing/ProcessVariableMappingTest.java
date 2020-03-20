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
import de.viadee.bpm.vPAV.config.model.RuleSet;
import de.viadee.bpm.vPAV.constants.ConfigConstants;
import de.viadee.bpm.vPAV.processing.code.flow.BpmnElement;
import de.viadee.bpm.vPAV.processing.code.flow.FlowAnalysis;
import de.viadee.bpm.vPAV.processing.model.data.ProcessVariable;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.BaseElement;
import org.camunda.bpm.model.bpmn.instance.BpmnModelElementInstance;
import org.camunda.bpm.model.bpmn.instance.ServiceTask;
import org.camunda.bpm.model.bpmn.instance.camunda.*;
import org.junit.*;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

import static de.viadee.bpm.vPAV.processing.BpmnModelDispatcher.getBpmnElements;
import static de.viadee.bpm.vPAV.processing.BpmnModelDispatcher.getProcessVariables;

public class ProcessVariableMappingTest {

    private static final String BASE_PATH = "src/test/resources/";

    private static final String MODEL_PATH = BASE_PATH + "ModelWithMappingDelegate.bpmn";

    private FileScanner fileScanner;

    private File processDefinition;

    private ProcessVariablesScanner scanner;

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
    public void testInputTypeText() {
        BpmnModelInstance modelInstance = createModelInstance();
        Collection<ProcessVariable> processVariables = analyzeModelInstance(modelInstance, "testInputText", "123", null,
                true);
        Assert.assertEquals(2, processVariables.size());
    }

    /**
     * Case: Test a model with Input Mapping of type list code
     */
    @Test
    public void testInputTypeList() {
        BpmnModelInstance modelInstance = createModelInstance();

        CamundaValue firstValue = modelInstance.newInstance(CamundaValue.class);
        firstValue.setTextContent("first");
        CamundaValue secondValue = modelInstance.newInstance(CamundaValue.class);
        secondValue.setTextContent("second");
        CamundaList list = modelInstance.newInstance(CamundaList.class);
        list.getValues().add(firstValue);
        list.getValues().add(secondValue);

        Collection<ProcessVariable> processVariables = analyzeModelInstance(modelInstance, "testInputList",
                list.getTextContent(), list, true);
        Assert.assertEquals(3, processVariables.size());
    }

    /**
     * Case: Test a model with Input Mapping of type map code
     */
    @Test
    public void testInputTypeMap() {
        BpmnModelInstance modelInstance = createModelInstance();

        CamundaEntry firstEntry = modelInstance.newInstance(CamundaEntry.class);
        firstEntry.setCamundaKey("first");
        firstEntry.setTextContent("1");
        CamundaEntry secondEntry = modelInstance.newInstance(CamundaEntry.class);
        secondEntry.setCamundaKey("second");
        secondEntry.setTextContent("2");
        CamundaMap map = modelInstance.newInstance(CamundaMap.class);
        map.getCamundaEntries().add(firstEntry);
        map.getCamundaEntries().add(secondEntry);

        Collection<ProcessVariable> processVariables = analyzeModelInstance(modelInstance, "testInputList",
                map.getTextContent(), map, true);
        Assert.assertEquals(3, processVariables.size());
    }

    /**
     * Case: Test a model with Output Mapping of type text code
     */
    @Test
    public void testOutputTypeText() {
        BpmnModelInstance modelInstance = createModelInstance();
        Collection<ProcessVariable> processVariables = analyzeModelInstance(modelInstance, "testOutputText", "456",
                null,
                false);
        Assert.assertEquals(2, processVariables.size());
    }

    /**
     * Case: Test a model with Output Mapping of type list code
     */
    @Test
    public void testOutputTypeList() {
        BpmnModelInstance modelInstance = createModelInstance();

        CamundaValue firstValue = modelInstance.newInstance(CamundaValue.class);
        firstValue.setTextContent("first");
        CamundaValue secondValue = modelInstance.newInstance(CamundaValue.class);
        secondValue.setTextContent("second");
        CamundaList list = modelInstance.newInstance(CamundaList.class);
        list.getValues().add(firstValue);
        list.getValues().add(secondValue);

        Collection<ProcessVariable> processVariables = analyzeModelInstance(modelInstance, "testOutputList",
                list.getTextContent(), list, false);
        Assert.assertEquals(3, processVariables.size());
    }

    /**
     * Case: Test a model with Output Mapping of type map code
     */
    @Test
    public void testOutputTypeMap() {
        BpmnModelInstance modelInstance = createModelInstance();

        CamundaEntry firstEntry = modelInstance.newInstance(CamundaEntry.class);
        firstEntry.setCamundaKey("first");
        firstEntry.setTextContent("1");
        CamundaEntry secondEntry = modelInstance.newInstance(CamundaEntry.class);
        secondEntry.setCamundaKey("second");
        secondEntry.setTextContent("2");
        CamundaMap map = modelInstance.newInstance(CamundaMap.class);
        map.getCamundaEntries().add(firstEntry);
        map.getCamundaEntries().add(secondEntry);

        Collection<ProcessVariable> processVariables = analyzeModelInstance(modelInstance, "testOutputList",
                map.getTextContent(), map, false);
        Assert.assertEquals(3, processVariables.size());
    }

    /**
     * Case: Test a model with Input Mapping of type text code
     */
    @Test
    public void testInputTypeScript() {
  /*      final BpmnModelInstance modelInstance = initializeModel();

        final Collection<BaseElement> baseElements = modelInstance.getModelElementsByType(BaseElement.class);
        final Rule rule = new Rule("ProcessVariablesModelChecker", true, null, null, null, null);

        final ElementGraphBuilder graphBuilder = new ElementGraphBuilder(new BpmnScanner(PATH), rule);
        // create data flow graphs
        graphBuilder.createProcessGraph(fileScanner, modelInstance, processDefinition.getPath(), new ArrayList<>(),
                scanner, new FlowAnalysis());

        final Collection<BpmnElement> bpmnElements = getBpmnElements(processDefinition, baseElements, graphBuilder,
                new FlowAnalysis());
        final Collection<ProcessVariable> processVariables = getProcessVariables(bpmnElements);

        Assert.assertEquals(1, processVariables.size()); */
    }

    public BpmnModelInstance createModelInstance() {
        final String PATH = BASE_PATH + "ModelWithMappingDelegate.bpmn";
        processDefinition = new File(PATH);
        fileScanner = new FileScanner(new RuleSet());
        fileScanner.setScanPath(ConfigConstants.TEST_JAVAPATH);
        final Set<String> testSet = new HashSet<>();
        testSet.add("de/viadee/bpm/vPAV/delegates/MappingDelegate.java");
        fileScanner.setJavaResourcesFileInputStream(testSet);
        scanner = new ProcessVariablesScanner(
                fileScanner.getJavaResourcesFileInputStream());
        scanner.scanProcessVariables();
        return Bpmn.readModelFromFile(processDefinition);
    }

    public Collection<ProcessVariable> analyzeModelInstance(BpmnModelInstance modelInstance, String name,
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

        final Collection<BaseElement> baseElements = modelInstance.getModelElementsByType(BaseElement.class);

        final ElementGraphBuilder graphBuilder = new ElementGraphBuilder(new BpmnScanner(MODEL_PATH));
        graphBuilder.createProcessGraph(fileScanner, modelInstance, processDefinition.getPath(), new ArrayList<>(),
                scanner, new FlowAnalysis());

        final Collection<BpmnElement> bpmnElements = getBpmnElements(processDefinition, baseElements, graphBuilder,
                new FlowAnalysis());
        return getProcessVariables(bpmnElements);
    }

    @Before
    public void clearIssues() {
        IssueService.getInstance().clear();
    }

}
