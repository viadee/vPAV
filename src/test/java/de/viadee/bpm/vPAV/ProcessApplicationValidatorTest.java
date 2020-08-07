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
import de.viadee.bpm.vPAV.processing.ElementGraphBuilder;
import de.viadee.bpm.vPAV.processing.JavaReaderStatic;
import de.viadee.bpm.vPAV.processing.EntryPointScanner;
import de.viadee.bpm.vPAV.processing.code.flow.FlowAnalysis;
import de.viadee.bpm.vPAV.processing.model.data.AnomalyContainer;
import de.viadee.bpm.vPAV.processing.model.graph.Graph;
import de.viadee.bpm.vPAV.processing.model.graph.Path;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.impl.instance.ServiceTaskImpl;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import soot.Scene;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

public class ProcessApplicationValidatorTest {

    @BeforeClass
    public static void setup() throws MalformedURLException {
        // Set custom basepath.
        Properties myProperties = new Properties();
        myProperties.put("basepath", "src/test/resources/ProcessApplicationValidatorTest/");
        RuntimeConfig.getInstance().setProperties(myProperties);

        // Bean-Mapping
        final Map<String, String> beanMapping = new HashMap<>();
        beanMapping.put("testDelegate", "de.viadee.bpm.vPAV.TestDelegate");
        RuntimeConfig.getInstance().setBeanMapping(beanMapping);

        final File file = new File(".");
        final String currentPath = file.toURI().toURL().toString();
        final URL classUrl = new URL(currentPath + "src/test/java");
        final URL[] classUrls = { classUrl };
        ClassLoader cl = new URLClassLoader(classUrls);
        RuntimeConfig.getInstance().setClassLoader(cl);
        RuntimeConfig.getInstance().setTest(true);

        // Setup soot
        RuntimeConfig.getInstance().setTest(true);
        FileScanner.setupSootClassPaths(new LinkedList<>());
        JavaReaderStatic.setupSoot();
        Scene.v().loadNecessaryClasses();
    }

    /**
     * This test fails if soot is not able to process Lambda expressions.
     */
    @Test
    public void testLamdbaExpression() {
        final FileScanner fileScanner = new FileScanner(new RuleSet());
        final Set<String> testSet = new HashSet<>();
        testSet.add("de/viadee/bpm/vPAV/TestDelegate.java");
        fileScanner.setJavaResourcesFileInputStream(testSet);
        final EntryPointScanner scanner = new EntryPointScanner(
                fileScanner.getJavaResourcesFileInputStream());
        scanner.scanProcessVariables();
    }

    @Test
    public void testOverloadedExecuteMethod() {
        final FileScanner fileScanner = new FileScanner(new RuleSet());
        final String PATH = "src/test/resources/ModelWithDelegate_UR.bpmn";
        final File processDefinition = new File(PATH);
        final EntryPointScanner scanner = new EntryPointScanner(
                fileScanner.getJavaResourcesFileInputStream());

        // parse bpmn model and set delegate
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(processDefinition);
        ServiceTaskImpl serviceTask = modelInstance.getModelElementById("ServiceTask_108g52x");
        serviceTask.setCamundaClass("de.viadee.bpm.vPAV.delegates.OverloadedExecuteDelegate");

        final ElementGraphBuilder graphBuilder = new ElementGraphBuilder(null, null, null, null);

        // create data flow graphs
        graphBuilder.createProcessGraph(fileScanner, modelInstance, processDefinition.getPath(), new ArrayList<>(),
                scanner, new FlowAnalysis());

        // create data flow graphs
        final Collection<String> calledElementHierarchy = new ArrayList<>();
        FlowAnalysis flowAnalysis = new FlowAnalysis();
        final Collection<Graph> graphCollection = graphBuilder.createProcessGraph(fileScanner, modelInstance,
                processDefinition.getPath(), calledElementHierarchy, scanner, flowAnalysis);

        flowAnalysis.analyze(graphCollection);
        // calculate invalid paths based on data flow graphs
        final Map<AnomalyContainer, List<Path>> invalidPathMap = graphBuilder.createInvalidPaths(graphCollection);
        Assert.assertEquals("Model should have one UR anomaly.", 1, invalidPathMap.size());
        Iterator<AnomalyContainer> iterator = invalidPathMap.keySet().iterator();
        Assert.assertEquals("myVariable should raise UR anomaly.", "myVariable", iterator.next().getName());
    }
}
