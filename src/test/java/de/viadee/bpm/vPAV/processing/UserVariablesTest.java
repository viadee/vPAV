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
package de.viadee.bpm.vPAV.processing;

import com.google.common.collect.ListMultimap;
import de.viadee.bpm.vPAV.BpmnScanner;
import de.viadee.bpm.vPAV.FileScanner;
import de.viadee.bpm.vPAV.RuntimeConfig;
import de.viadee.bpm.vPAV.config.model.RuleSet;
import de.viadee.bpm.vPAV.config.reader.ConfigReaderException;
import de.viadee.bpm.vPAV.config.reader.XmlVariablesReader;
import de.viadee.bpm.vPAV.constants.ConfigConstants;
import de.viadee.bpm.vPAV.processing.code.flow.FlowAnalysis;
import de.viadee.bpm.vPAV.processing.model.data.AnomalyContainer;
import de.viadee.bpm.vPAV.processing.model.data.ProcessVariableOperation;
import de.viadee.bpm.vPAV.processing.model.graph.Graph;
import de.viadee.bpm.vPAV.processing.model.graph.Path;
import fj.Hash;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.testng.Assert;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

public class UserVariablesTest {

    private static final String BASE_PATH = "src/test/resources/";

    private static ClassLoader cl;

    @BeforeClass
    public static void setup() throws MalformedURLException {
        RuntimeConfig.getInstance().setTest(true);
        final File file = new File(".");
        final String currentPath = file.toURI().toURL().toString();
        final URL classUrl = new URL(currentPath + "src/test/java/");
        final URL resourcesUrl = new URL(currentPath + "src/test/resources/");
        final URL[] classUrls = { classUrl, resourcesUrl };
        cl = new URLClassLoader(classUrls);
        RuntimeConfig.getInstance().setClassLoader(cl);
    }

    @AfterClass
    public static void tearDown() {
        RuntimeConfig.getInstance().setTest(false);
    }

    @Test
    public void testUserDefinedVariablesCorrect() {
        final ProcessVariablesScanner scanner = new ProcessVariablesScanner(null);
        final FileScanner fileScanner = new FileScanner(new RuleSet());
        fileScanner.setScanPath(ConfigConstants.TEST_JAVAPATH);
        final String PATH = BASE_PATH + "ModelWithDelegate_UR.bpmn";
        final File processDefinition = new File(PATH);

        Properties myProperties = new Properties();
        myProperties.put("userVariablesFilePath", "UserVariablesTest/" + ConfigConstants.USER_VARIABLES_FILE);
        ConfigConstants.getInstance().setProperties(myProperties);

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

        final ElementGraphBuilder graphBuilder = new ElementGraphBuilder(new BpmnScanner(PATH));

        FlowAnalysis flowAnalysis = new FlowAnalysis();

        // create data flow graphs
        final Collection<String> calledElementHierarchy = new ArrayList<>();
        final Collection<Graph> graphCollection = graphBuilder.createProcessGraph(fileScanner, modelInstance,
                processDefinition.getPath(), calledElementHierarchy, scanner, flowAnalysis);

        flowAnalysis.analyze(graphCollection);

        // calculate invalid paths based on data flow graphs
        final Map<AnomalyContainer, List<Path>> invalidPathMap = graphBuilder.createInvalidPaths(graphCollection);

        Assert.assertEquals(invalidPathMap.size(), 0);

        Assert.assertEquals(flowAnalysis.getNodes().get("ServiceTask_108g52x__0").getDefined().get("2").getName(),
                "numberEntities",
                "'numberEntities' should be listed as defined because it was defined by the user.");
        Assert.assertEquals(flowAnalysis.getNodes().get("SequenceFlow_0znqs8t").getInUsed().size(), 0,
                "Variable 'numberEntities' should not be passed to Sequence Flow because it is out of scope.");
        Assert.assertEquals(flowAnalysis.getNodes().get("StartEvent_1__0").getDefined().get("1").getName(),
                "anotherVariable",
                "'anotherVariable' should be listed as defined in the start event");
    }

    @Test
    public void testUserDefinedVariablesIncorrect() throws JAXBException {
        XmlVariablesReader xmlVariablesReader = new XmlVariablesReader();

        HashMap<String, ListMultimap<String, ProcessVariableOperation>> userVariables = xmlVariablesReader
                .read("UserVariablesTest/variables_incorrect.xml", "testProcess");

        Assert.assertEquals(0, userVariables.size(), "Ill-defined user variable should not be included.");
    }
}
