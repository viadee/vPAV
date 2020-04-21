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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import de.viadee.bpm.vPAV.BpmnScanner;
import de.viadee.bpm.vPAV.FileScanner;
import de.viadee.bpm.vPAV.Helper;
import de.viadee.bpm.vPAV.RuntimeConfig;
import de.viadee.bpm.vPAV.config.model.RuleSet;
import de.viadee.bpm.vPAV.constants.ConfigConstants;
import de.viadee.bpm.vPAV.processing.code.flow.*;
import de.viadee.bpm.vPAV.processing.model.data.ElementChapter;
import de.viadee.bpm.vPAV.processing.model.data.KnownElementFieldType;
import de.viadee.bpm.vPAV.processing.model.data.ProcessVariable;
import de.viadee.bpm.vPAV.processing.model.data.ProcessVariableOperation;
import de.viadee.bpm.vPAV.processing.model.graph.Graph;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.impl.instance.ServiceTaskImpl;
import org.camunda.bpm.model.bpmn.instance.BaseElement;
import org.camunda.bpm.model.bpmn.instance.ServiceTask;
import org.camunda.bpm.model.bpmn.instance.StartEvent;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

import static de.viadee.bpm.vPAV.processing.BpmnModelDispatcher.getBpmnElements;
import static de.viadee.bpm.vPAV.processing.BpmnModelDispatcher.getProcessVariables;
import static org.junit.Assert.assertEquals;

public class ProcessVariableReaderStaticTest {

    private static final String BASE_PATH = "src/test/resources/";

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
        beanMapping.put("testDelegate", "de/viadee/bpm/vPAV/delegates/TestDelegateFlowGraph.class");
        RuntimeConfig.getInstance().setBeanMapping(beanMapping);
    }

    @Test
    public void testSootReachingMethod() {
        final String PATH = BASE_PATH + "ProcessVariablesModelCheckerTest_InitialProcessVariables.bpmn";

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

        final Collection<ServiceTask> tasks = modelInstance.getModelElementsByType(ServiceTask.class);

        final BpmnElement element = new BpmnElement(PATH, tasks.iterator().next(), new ControlFlowGraph(),
                new FlowAnalysis());
        final FileScanner fileScanner = new FileScanner(new RuleSet());
        fileScanner.setScanPath(ConfigConstants.TEST_JAVAPATH);
        new JavaReaderStatic()
                .getVariablesFromJavaDelegate("de.viadee.bpm.vPAV.delegates.TestDelegateStatic", element, null, null,
                        null, new BasicNode[1]);

        assertEquals(4, element.getControlFlowGraph().getOperations().size());
    }

    @Test
    public void findInitialProcessVariables() {
        final String PATH = BASE_PATH + "ProcessVariablesModelCheckerTest_InitialProcessVariables.bpmn";

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

        final Collection<StartEvent> startElement = modelInstance.getModelElementsByType(StartEvent.class);

        final BpmnElement element = new BpmnElement(PATH, startElement.iterator().next(), new ControlFlowGraph(),
                new FlowAnalysis());

        final EntryPoint entry = new EntryPoint(
                "de.viadee.bpm.vPAV.delegates.TestDelegateStaticInitialProcessVariables.java", "startProcess",
                "schadensmeldungKfzGlasbruch", "startProcessInstanceByMessage");
        final ListMultimap<String, ProcessVariableOperation> variables = ArrayListMultimap.create();

        final FileScanner fileScanner = new FileScanner(new RuleSet());
        fileScanner.setScanPath(ConfigConstants.TEST_JAVAPATH);

        new JavaReaderStatic()
                .getVariablesFromClass("de.viadee.bpm.vPAV.delegates.TestDelegateStaticInitialProcessVariables",
                        element, entry, new BasicNode[1]);

        assertEquals(3, element.getControlFlowGraph().getOperations().size());
    }

    @Test
    public void followMethodInvocation() {
        final String PATH = BASE_PATH + "ProcessVariablesReader_MethodInvocation.bpmn";

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

        final Collection<ServiceTask> tasks = modelInstance.getModelElementsByType(ServiceTask.class);

        final BpmnElement element = new BpmnElement(PATH, tasks.iterator().next(), new ControlFlowGraph(),
                new FlowAnalysis());
        Properties myProperties = new Properties();
        myProperties.put("scanpath", ConfigConstants.TEST_TARGET_PATH);
        ConfigConstants.getInstance().setProperties(myProperties);
        final FileScanner fileScanner = new FileScanner(new RuleSet());
        new JavaReaderStatic()
                .getVariablesFromJavaDelegate("de.viadee.bpm.vPAV.delegates.MethodInvocationDelegate", element,
                        ElementChapter.Implementation,
                        KnownElementFieldType.CalledElement, element.getBaseElement().getScope().toString(),
                        new BasicNode[1]);
        assertEquals(3, element.getControlFlowGraph().getOperations().size());
    }

    @Test
    public void followObjectInstantiationWithDynamicVariables() {
        final String PATH = BASE_PATH + "ModelWithDelegate_UR.bpmn";
        final FileScanner fileScanner = new FileScanner(new RuleSet());
        fileScanner.setScanPath(ConfigConstants.TEST_JAVAPATH);

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));
        ServiceTaskImpl serviceTask = modelInstance.getModelElementById("ServiceTask_108g52x");
        serviceTask.setCamundaClass("de.viadee.bpm.vPAV.delegates.DelegateDynamicObjectInstantiation");

        final Collection<ServiceTask> allServiceTasks = modelInstance.getModelElementsByType(ServiceTask.class);

        final ProcessVariableReader variableReader = new ProcessVariableReader(null, null, new BpmnScanner(PATH));
        final BpmnElement element = new BpmnElement(PATH, allServiceTasks.iterator().next(), new ControlFlowGraph(),
                new FlowAnalysis());

        variableReader.getVariablesFromElement(fileScanner, element, new BasicNode[1]);

        Assert.assertEquals(1, element.getControlFlowGraph().getOperations().size());
        // TODO
        //	Assert.assertNotNull(variables.get("myValue"));
    }

    @Test
    public void followObjectInstantiation() {
        FlowAnalysis flowAnalysis = new FlowAnalysis();
        final Collection<Graph> graphCollection = Helper
                .getModelWithDelegate("de.viadee.bpm.vPAV.delegates.TechnicalDelegate", flowAnalysis);

        Iterator<BpmnElement> elements = graphCollection.iterator().next().getVertices().iterator();
        elements.next();
        BpmnElement serviceTask = elements.next();
        Iterator<BasicNode> nodes = serviceTask.getControlFlowGraph().getNodes().values().iterator();
        // Remove operation in TechnicalProcessContext
        assertEquals(1, nodes.next().getKilled().size());
        // Read operation in TechnicalDelegate
        assertEquals(1, nodes.next().getUsed().size());
    }

    @Test
    public void retrieveVariableOperations() {
        FlowAnalysis flowAnalysis = new FlowAnalysis();
        final Collection<Graph> graphCollection = Helper
                .getModelWithDelegate("de.viadee.bpm.vPAV.delegates.TestDelegateFlowGraph", flowAnalysis);

        Iterator<BpmnElement> elements = graphCollection.iterator().next().getVertices().iterator();
        elements.next();
        BpmnElement serviceTask = elements.next();
        assertEquals(5, serviceTask.getControlFlowGraph().getNodes().values().size());
    }

}
