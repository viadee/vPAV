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
package de.viadee.bpm.vPAV.processing;

import com.google.common.collect.ListMultimap;
import de.viadee.bpm.vPAV.FileScanner;
import de.viadee.bpm.vPAV.IssueService;
import de.viadee.bpm.vPAV.RuntimeConfig;
import de.viadee.bpm.vPAV.config.model.RuleSet;
import de.viadee.bpm.vPAV.config.reader.XmlVariablesReader;
import de.viadee.bpm.vPAV.constants.ConfigConstants;
import de.viadee.bpm.vPAV.processing.code.flow.BpmnElement;
import de.viadee.bpm.vPAV.processing.code.flow.FlowAnalysis;
import de.viadee.bpm.vPAV.processing.model.data.ProcessVariableOperation;
import de.viadee.bpm.vPAV.processing.model.data.VariableOperation;
import de.viadee.bpm.vPAV.processing.model.graph.Graph;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.junit.*;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Properties;

public class UserVariablesTest {

    private static final String BASE_PATH = "src/test/resources/";

    @BeforeClass
    public static void setup() throws MalformedURLException {
        RuntimeConfig.getInstance().setTest(true);
        final File file = new File(".");
        final String currentPath = file.toURI().toURL().toString();
        final URL classUrl = new URL(currentPath + "src/test/java/");
        final URL resourcesUrl = new URL(currentPath + "src/test/resources/");
        final URL[] classUrls = { classUrl, resourcesUrl };
        ClassLoader cl = new URLClassLoader(classUrls);
        RuntimeConfig.getInstance().setClassLoader(cl);
    }

    @AfterClass
    public static void tearDown() {
        RuntimeConfig.getInstance().setTest(false);
    }

    @Test
    public void testUserVariablesInclusionInGraph() {
        final ProcessVariablesScanner scanner = new ProcessVariablesScanner(null);
        final FileScanner fileScanner = new FileScanner(new RuleSet());
        final String PATH = BASE_PATH + "ModelWithDelegate_UR.bpmn";
        final File processDefinition = new File(PATH);

        Properties myProperties = new Properties();
        myProperties.put("userVariablesFilePath", "UserVariablesTest/" + ConfigConstants.USER_VARIABLES_FILE);
        RuntimeConfig.getInstance().setProperties(myProperties);

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

        final ElementGraphBuilder graphBuilder = new ElementGraphBuilder();

        FlowAnalysis flowAnalysis = new FlowAnalysis();

        // create data flow graphs
        final Collection<String> calledElementHierarchy = new ArrayList<>();
        final Collection<Graph> graphCollection = graphBuilder.createProcessGraph(fileScanner, modelInstance,
                processDefinition.getPath(), calledElementHierarchy, scanner, flowAnalysis);

        Graph graph = graphCollection.iterator().next();
        ProcessVariableOperation pvo;
        for (BpmnElement element : graph.getVertices()) {
            switch (element.getId()) {
                case "StartEvent_1":
                    // Variable 'anotherVariable' should be included as the scope is not restricted
                    pvo = element.getControlFlowGraph().getOperations().get("anotherVariable_2").get(0);
                    Assert.assertNotNull(pvo);
                    break;
                case "ServiceTask_108g52x":
                    // Variable 'numberEntities' should be ony included in the scope of 'ServiceTask_108g52x'
                    pvo = element.getControlFlowGraph().getOperations().get("numberEntities_0").get(0);
                    Assert.assertNotNull(pvo);
                    break;
                case "EndEvent_13uioac":
                    // Variable 'numberEntities' should be ony included in the scope of 'EndEvent_13uioac'
                    pvo = element.getControlFlowGraph().getOperations().get("numberEntities_1").get(0);
                    Assert.assertNotNull(pvo);
                    break;
            }
        }
    }

    @Test
    public void testXmlVariablesReader() throws JAXBException {
        // Tests that user variables are correctly read from xml and translated to ProcessVariableOperations
        XmlVariablesReader xmlVariablesReader = new XmlVariablesReader();

        HashMap<String, ListMultimap<String, ProcessVariableOperation>> userVariables = xmlVariablesReader
                .read("UserVariablesTest/variables_allOperations.xml", "Process_1");

        ListMultimap<String, ProcessVariableOperation> processVariables = userVariables.get("StartEvent");
        ListMultimap<String, ProcessVariableOperation> serviceVariables = userVariables.get("ServiceTask_108g52x");
        ListMultimap<String, ProcessVariableOperation> sequenceVariables = userVariables.get("SequenceFlow_1g7pl28");

        Assert.assertEquals(1, processVariables.size());
        Assert.assertEquals(VariableOperation.WRITE, processVariables.get("int_Hallo").get(0).getOperation());

        Assert.assertEquals(1, serviceVariables.size());
        Assert.assertEquals(VariableOperation.WRITE, serviceVariables.get("ext_Blub").get(0).getOperation());
        Assert.assertEquals("ServiceTask_108g52x", serviceVariables.get("ext_Blub").get(0).getScopeId());

        Assert.assertEquals(2, sequenceVariables.size());
        Assert.assertEquals(VariableOperation.READ, sequenceVariables.get("ext_Blub").get(0).getOperation());
        Assert.assertEquals("Process_1", sequenceVariables.get("ext_Blub").get(0).getScopeId());
        Assert.assertEquals(VariableOperation.DELETE, sequenceVariables.get("int_Hallo").get(0).getOperation());
        Assert.assertEquals("Process_1", sequenceVariables.get("int_Hallo").get(0).getScopeId());
    }

    @Test
    public void testUserDefinedVariablesIncorrect() throws JAXBException {
        XmlVariablesReader xmlVariablesReader = new XmlVariablesReader();

        HashMap<String, ListMultimap<String, ProcessVariableOperation>> userVariables = xmlVariablesReader
                .read("UserVariablesTest/variables_incorrect.xml", "testProcess");

        Assert.assertEquals("Ill-defined user variable should not be included.", 0, userVariables.size());
    }

    @Before
    public void clear() {
        IssueService.getInstance().clear();
        ProcessVariableOperation.resetIdCounter();
    }
}
