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
package de.viadee.bpm.vPAV.processing.model.data;

import de.viadee.bpm.vPAV.BpmnScanner;
import de.viadee.bpm.vPAV.FileScanner;
import de.viadee.bpm.vPAV.RuntimeConfig;
import de.viadee.bpm.vPAV.config.model.RuleSet;
import de.viadee.bpm.vPAV.processing.ElementGraphBuilder;
import de.viadee.bpm.vPAV.processing.ProcessVariablesScanner;
import de.viadee.bpm.vPAV.processing.code.flow.FlowAnalysis;
import de.viadee.bpm.vPAV.processing.model.graph.Graph;
import de.viadee.bpm.vPAV.processing.model.graph.Path;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.impl.instance.LoopCardinalityImpl;
import org.camunda.bpm.model.bpmn.instance.LoopCardinality;
import org.camunda.bpm.model.bpmn.instance.ServiceTask;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

public class MultiInstanceActivityTest {

    private static final String BASE_PATH = "src/test/resources/";

    @BeforeClass
    public static void setup() throws MalformedURLException {
        final File file = new File(".");
        final String currentPath = file.toURI().toURL().toString();
        final URL classUrl = new URL(currentPath + "src/test/java");
        final URL[] classUrls = {classUrl};
        ClassLoader cl = new URLClassLoader(classUrls);
        RuntimeConfig.getInstance().setClassLoader(cl);
        RuntimeConfig.getInstance().setTest(true);
    }

    @Test
    public void testCollection() {
        final ProcessVariablesScanner scanner = new ProcessVariablesScanner(null);
        final FileScanner fileScanner = new FileScanner(new RuleSet());
        final String PATH = BASE_PATH + "MultiInstanceActivityTest_Collection.bpmn";
        final File processDefinition = new File(PATH);

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(processDefinition);
        final ElementGraphBuilder graphBuilder = new ElementGraphBuilder(new BpmnScanner(PATH));

        FlowAnalysis flowAnalysis = new FlowAnalysis();

        // create data flow graphs
        final Collection<String> calledElementHierarchy = new ArrayList<>();
        final Collection<Graph> graphCollection = graphBuilder.createProcessGraph(fileScanner, modelInstance,
                processDefinition.getPath(), calledElementHierarchy, scanner, flowAnalysis);

        flowAnalysis.analyze(graphCollection);

        // calculate invalid paths based on data flow graphs
        final Map<AnomalyContainer, List<Path>> invalidPathMap = graphBuilder.createInvalidPaths(graphCollection);

        Assert.assertEquals("There should  be one issue because 'element' is not available in non-multi instance tasks.",
                1, invalidPathMap.size());

        Iterator<AnomalyContainer> iterator = invalidPathMap.keySet().iterator();
        AnomalyContainer anomaly1 = iterator.next();
        Assert.assertEquals("Expected a UR anomaly but got " + anomaly1.getAnomaly().toString(), Anomaly.UR,
                anomaly1.getAnomaly());
    }


    /**
     * Test multi instance activity with collection that is defined with child elements instead of attributes
     */
    @Test
    public void testCollectionChildElement() {
        final ProcessVariablesScanner scanner = new ProcessVariablesScanner(null);
        final FileScanner fileScanner = new FileScanner(new RuleSet());
        final String PATH = BASE_PATH + "MultiInstanceActivityTest_Collection2.bpmn";
        final File processDefinition = new File(PATH);

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(processDefinition);
        final ElementGraphBuilder graphBuilder = new ElementGraphBuilder(new BpmnScanner(PATH));

        FlowAnalysis flowAnalysis = new FlowAnalysis();

        // create data flow graphs
        final Collection<String> calledElementHierarchy = new ArrayList<>();
        final Collection<Graph> graphCollection = graphBuilder.createProcessGraph(fileScanner, modelInstance,
                processDefinition.getPath(), calledElementHierarchy, scanner, flowAnalysis);

        flowAnalysis.analyze(graphCollection);

        // calculate invalid paths based on data flow graphs
        final Map<AnomalyContainer, List<Path>> invalidPathMap = graphBuilder.createInvalidPaths(graphCollection);

        Assert.assertEquals("There should  be one issue because 'element' is not available in non-multi instance tasks.",
                1, invalidPathMap.size());

        Iterator<AnomalyContainer> iterator = invalidPathMap.keySet().iterator();
        AnomalyContainer anomaly1 = iterator.next();
        Assert.assertEquals("Expected a UR anomaly but got " + anomaly1.getAnomaly().toString(), Anomaly.UR,
                anomaly1.getAnomaly());
    }

    @Test
    public void testCorrectLoopCardinality() {
        final ProcessVariablesScanner scanner = new ProcessVariablesScanner(null);
        final FileScanner fileScanner = new FileScanner(new RuleSet());
        final String PATH = BASE_PATH + "MultiInstanceActivityTest.bpmn";
        final File processDefinition = new File(PATH);

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(processDefinition);

        final ElementGraphBuilder graphBuilder = new ElementGraphBuilder(new BpmnScanner(PATH));

        FlowAnalysis flowAnalysis = new FlowAnalysis();

        // create data flow graphs
        final Collection<String> calledElementHierarchy = new ArrayList<>();
        final Collection<Graph> graphCollection = graphBuilder.createProcessGraph(fileScanner, modelInstance,
                processDefinition.getPath(), calledElementHierarchy, scanner, flowAnalysis);

        flowAnalysis.analyze(graphCollection);

        // calculate invalid paths based on data flow graphs
        final Map<AnomalyContainer, List<Path>> invalidPathMap = graphBuilder.createInvalidPaths(graphCollection);

        Assert.assertEquals("There should  be two issues.",
                2, invalidPathMap.size());

        Iterator<AnomalyContainer> iterator = invalidPathMap.keySet().iterator();
        AnomalyContainer anomaly1 = iterator.next();
        AnomalyContainer anomaly2 = iterator.next();
        // "element" does not exist in delegate
        Assert.assertEquals("Expected a UR anomaly but got " + anomaly1.getAnomaly().toString(), Anomaly.UR,
                anomaly1.getAnomaly());
        // "loopCounter" does not exist in second task
        Assert.assertEquals("Expected a UR anomaly but got " + anomaly2.getAnomaly().toString(), Anomaly.UR,
                anomaly2.getAnomaly());
    }

    @Test
    public void testLoopCardinalityExpressionVariables() {
        final ProcessVariablesScanner scanner = new ProcessVariablesScanner(null);
        final FileScanner fileScanner = new FileScanner(new RuleSet());
        final String PATH = BASE_PATH + "MultiInstanceActivityTest.bpmn";
        final File processDefinition = new File(PATH);

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(processDefinition);

        // Use undefined variable in loop cardinality expression
        ServiceTask serviceTask = modelInstance.getModelElementById("Sequential_ServiceTask");
        LoopCardinalityImpl loopCardinality = (LoopCardinalityImpl) serviceTask.getLoopCharacteristics().getChildElementsByType(LoopCardinality.class).iterator().next();
        loopCardinality.setTextContent("${notExistingVariable}");

        final ElementGraphBuilder graphBuilder = new ElementGraphBuilder(new BpmnScanner(PATH));

        FlowAnalysis flowAnalysis = new FlowAnalysis();

        // create data flow graphs
        final Collection<String> calledElementHierarchy = new ArrayList<>();
        final Collection<Graph> graphCollection = graphBuilder.createProcessGraph(fileScanner, modelInstance,
                processDefinition.getPath(), calledElementHierarchy, scanner, flowAnalysis);

        flowAnalysis.analyze(graphCollection);

        // calculate invalid paths based on data flow graphs
        final Map<AnomalyContainer, List<Path>> invalidPathMap = graphBuilder.createInvalidPaths(graphCollection);

        Assert.assertEquals("There should three issues.",
                3, invalidPathMap.size());

        Iterator<AnomalyContainer> iterator = invalidPathMap.keySet().iterator();
        AnomalyContainer anomaly1 = iterator.next();
        AnomalyContainer anomaly2 = iterator.next();
        Assert.assertEquals("Expected a UR anomaly but got " + anomaly2.getAnomaly().toString(), Anomaly.UR,
                anomaly2.getAnomaly());
        Assert.assertEquals("The variable in the loop cardinality expression should raise an issue.",
                "notExistingVariable", anomaly2.getVariable().getName());
    }
}
