/*
 * BSD 3-Clause License
 *
 * Copyright © 2022, viadee Unternehmensberatung AG
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
package de.viadee.bpm.vpav.processing.model.graph;

import de.viadee.bpm.vpav.FileScanner;
import de.viadee.bpm.vpav.Helper;
import de.viadee.bpm.vpav.RuntimeConfig;
import de.viadee.bpm.vpav.config.model.RuleSet;
import de.viadee.bpm.vpav.processing.ElementGraphBuilder;
import de.viadee.bpm.vpav.processing.EntryPointScanner;
import de.viadee.bpm.vpav.processing.JavaReaderStatic;
import de.viadee.bpm.vpav.processing.code.flow.FlowAnalysis;
import de.viadee.bpm.vpav.processing.model.data.Anomaly;
import de.viadee.bpm.vpav.processing.model.data.AnomalyContainer;
import de.viadee.bpm.vpav.processing.model.data.ProcessVariableOperation;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import soot.Scene;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

/**
 * Unit Tests for data flow graph creation and calculation of invalid paths
 */
public class GraphCreationTest {

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
        FileScanner.setupSootClassPaths(new LinkedList<>());
        JavaReaderStatic.setupSoot();
        Scene.v().loadNecessaryClasses();
    }

    @AfterClass
    public static void tearDown() {
        RuntimeConfig.getInstance().setTest(false);
    }

    @Test
    public void testMethodInvocationOrder() {
        final Map<AnomalyContainer, List<Path>> invalidPathMap = Helper
                .getModelWithBeanDelegate("de/viadee/bpm/vpav/delegates/MethodInvocationDelegate.class");

        // DU + UR anomaly
        Assert.assertEquals(2, invalidPathMap.size());
        Iterator<AnomalyContainer> iter = invalidPathMap.keySet().iterator();
        Assert.assertEquals(Anomaly.DU, iter.next().getAnomaly());
        Assert.assertEquals(Anomaly.UR, iter.next().getAnomaly());
    }

    /**
     * Case: Data flow graph creation and calculation of invalid paths
     */
    @Test
    public void testGraph() {
        final EntryPointScanner scanner = new EntryPointScanner(null);
        final FileScanner fileScanner = new FileScanner(new RuleSet());
        final String PATH = BASE_PATH + "ProcessVariablesModelCheckerTest_GraphCreation.bpmn";
        final File processDefinition = new File(PATH);
        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(processDefinition);
        final ElementGraphBuilder graphBuilder = new ElementGraphBuilder();
        // create data flow graphs
        FlowAnalysis flowAnalysis = new FlowAnalysis();
        final Collection<Graph> graphCollection = graphBuilder.createProcessGraph(fileScanner, modelInstance,
                processDefinition.getPath(), new ArrayList<>(), scanner, flowAnalysis);
        flowAnalysis.analyze(graphCollection);
        // calculate invalid paths based on data flow graphs
        final Map<AnomalyContainer, List<Path>> invalidPathMap = graphBuilder.createInvalidPaths(graphCollection);
        Iterator<Map.Entry<AnomalyContainer, List<Path>>> iterator = invalidPathMap.entrySet().iterator();

        // get invalid paths
        Map.Entry<AnomalyContainer, List<Path>> entry1 = iterator.next();
        Map.Entry<AnomalyContainer, List<Path>> entry2 = iterator.next();
        Map.Entry<AnomalyContainer, List<Path>> entry3 = iterator.next();
        Map.Entry<AnomalyContainer, List<Path>> entry4 = iterator.next();

        AnomalyContainer anomalyContainerGeloeschteVar = entry4.getKey();
        ProcessVariableOperation geloeschteVarOperation = Mockito.mock(ProcessVariableOperation.class);
        Mockito.when(geloeschteVarOperation.getIndex()).thenReturn(4);
        Assert.assertEquals("AnomalyContainer geloeschteVariable does not equal actual container",
                new AnomalyContainer("geloeschteVariable", Anomaly.DU, "SequenceFlow_0bi6kaa__0",
                        "SequenceFlow_0bi6kaa", "", geloeschteVarOperation), anomalyContainerGeloeschteVar);

        AnomalyContainer anomalyContainerJepppa = entry1.getKey();
        ProcessVariableOperation jepppaOperation = Mockito.mock(ProcessVariableOperation.class);
        Mockito.when(jepppaOperation.getIndex()).thenReturn(11);
        Assert.assertEquals("AnomalyContainer jepppa does not equal actual container",
                new AnomalyContainer("jepppa", Anomaly.DD,
                        "SequenceFlow_0btqo3y__0",
                        "SequenceFlow_0btqo3y",
                        null,
                        jepppaOperation), anomalyContainerJepppa);

        AnomalyContainer anomalyContainerHallo2 = entry3.getKey();
        ProcessVariableOperation hallo2Operation = Mockito.mock(ProcessVariableOperation.class);
        Mockito.when(hallo2Operation.getIndex()).thenReturn(2);
        Assert.assertEquals("AnomalyContainer hallo2 does not equal actual container",
                new AnomalyContainer("hallo2", Anomaly.UR,
                        "BusinessRuleTask_119jb6t__0",
                        "BusinessRuleTask_119jb6t", "", hallo2Operation), anomalyContainerHallo2);

        AnomalyContainer anomalyContainerIntHallo = entry2.getKey();
        ProcessVariableOperation intHalloOperation = Mockito.mock(ProcessVariableOperation.class);
        Assert.assertEquals("AnomalyContainer intHallo does not equal actual container",
                new AnomalyContainer("intHallo", Anomaly.UR, "ServiceTask_05g4a96", "Service Task2", intHalloOperation),
                anomalyContainerIntHallo);
    }
}
