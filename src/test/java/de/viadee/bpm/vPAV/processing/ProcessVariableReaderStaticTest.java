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
import de.viadee.bpm.vPAV.RuntimeConfig;
import de.viadee.bpm.vPAV.constants.ConfigConstants;
import de.viadee.bpm.vPAV.processing.code.flow.ControlFlowGraph;
import de.viadee.bpm.vPAV.processing.model.data.BpmnElement;
import de.viadee.bpm.vPAV.processing.model.data.ProcessVariable;
import de.viadee.bpm.vPAV.processing.model.data.ProcessVariableOperation;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
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

    private static ClassLoader cl;
    
    private static final String BASE_PATH = "src/test/resources/";

    @BeforeClass
    public static void setup() throws MalformedURLException {
        final File file = new File(".");
        final String currentPath = file.toURI().toURL().toString();
        final URL classUrl = new URL(currentPath + "src/test/java");
        final URL[] classUrls = {classUrl};
        ClassLoader cl = new URLClassLoader(classUrls);
        RuntimeConfig.getInstance().setClassLoader(cl);
        RuntimeConfig.getInstance().getResource("en_US");
        RuntimeConfig.getInstance().setTest(true);
        final Map<String, String> beanMapping = new HashMap<>();
        beanMapping.put("testDelegate", "de/viadee/bpm/vPAV/delegates/TestDelegateFlowGraph.java");
        RuntimeConfig.getInstance().setBeanMapping(beanMapping);
    }

    @Test
    public void testSootReachingMethod() {
        final String PATH = BASE_PATH + "ProcessVariablesModelCheckerTest_InitialProcessVariables.bpmn";

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

        final Collection<ServiceTask> tasks = modelInstance
                .getModelElementsByType(ServiceTask.class);

        final BpmnElement element = new BpmnElement(PATH, tasks.iterator().next());
        final ControlFlowGraph cg = new ControlFlowGraph(element);
    	final FileScanner fileScanner = new FileScanner(new HashMap<>(), ConfigConstants.TEST_JAVAPATH);
    	final ListMultimap<String, ProcessVariableOperation> variables = ArrayListMultimap.create();
        variables.putAll(new JavaReaderStatic().getVariablesFromJavaDelegate(fileScanner,
                "de.viadee.bpm.vPAV.delegates.TestDelegateStatic", element, null, null, null, cg));

        assertEquals(3, variables.asMap().size());
    }
    
    @Test
    public void findInitialProcessVariables() {
        final String PATH = BASE_PATH + "ProcessVariablesModelCheckerTest_InitialProcessVariables.bpmn";

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

        final Collection<StartEvent> startElement = modelInstance
                .getModelElementsByType(StartEvent.class);

        final BpmnElement element = new BpmnElement(PATH, startElement.iterator().next());

        final EntryPoint entry = new EntryPoint("de.viadee.bpm.vPAV.delegates.TestDelegateStaticInitialProcessVariables.java","startProcess", "schadensmeldungKfzGlasbruch", "startProcessInstanceByMessage");
        final ListMultimap<String, ProcessVariableOperation> variables = ArrayListMultimap.create();
        final Set<String> resources = new HashSet<String>();
        resources.add("");
        
        ProcessVariablesScanner scanner = new ProcessVariablesScanner(resources);

        variables.putAll(new JavaReaderStatic().getVariablesFromClass(
            		"de.viadee.bpm.vPAV.delegates.TestDelegateStaticInitialProcessVariables", scanner, element, null, entry));


        assertEquals(3, variables.size());

    }

    @Test
    public void followMethodInvocation() {
        final String PATH = BASE_PATH + "ProcessVariablesReader_MethodInvocation.bpmn";

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

        final Collection<ServiceTask> tasks = modelInstance
                .getModelElementsByType(ServiceTask.class);

        final BpmnElement element = new BpmnElement(PATH, tasks.iterator().next());
        final ControlFlowGraph cg = new ControlFlowGraph(element);
        final FileScanner fileScanner = new FileScanner(new HashMap<>(), ConfigConstants.TEST_JAVAPATH);
        final ListMultimap<String, ProcessVariableOperation> variables = ArrayListMultimap.create();
        variables.putAll(new JavaReaderStatic().getVariablesFromJavaDelegate(fileScanner,
                "de.viadee.bpm.vPAV.delegates.MethodInvocationDelegate", element, null, null, null, cg));
        assertEquals(2, variables.values().size());

    }

    @Test
    public void followObjectInstantiation() {
        final String PATH = BASE_PATH + "ProcessVariablesReader_ObjectInstantiation.bpmn";

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

        final Collection<ServiceTask> tasks = modelInstance
                .getModelElementsByType(ServiceTask.class);

        final BpmnElement element = new BpmnElement(PATH, tasks.iterator().next());
        final ControlFlowGraph cg = new ControlFlowGraph(element);
        final FileScanner fileScanner = new FileScanner(new HashMap<>(), ConfigConstants.TEST_JAVAPATH);
        final ListMultimap<String, ProcessVariableOperation> variables = ArrayListMultimap.create();
        variables.putAll(new JavaReaderStatic().getVariablesFromJavaDelegate(fileScanner,
                "de.viadee.bpm.vPAV.delegates.TechnicalDelegate", element, null, null, null, cg));
        assertEquals(2, variables.values().size());
    }

    @Test
    public void retrieveVariableOperations() {
        final String PATH = BASE_PATH + "ProcessVariableReader_RetrieveOperations.bpmn";
        final File processDefinition = new File(PATH);
        final FileScanner fileScanner = new FileScanner(new HashMap<>(), ConfigConstants.TEST_JAVAPATH);
        final Set<String> testSet = new HashSet<>();
        testSet.add("de/viadee/bpm/vPAV/delegates/TestDelegateFlowGraph.java");
        fileScanner.setJavaResourcesFileInputStream(testSet);

        final ProcessVariablesScanner scanner = new ProcessVariablesScanner(
                fileScanner.getJavaResourcesFileInputStream());

        scanner.scanProcessVariables();

        final JavaReaderContext jvc = new JavaReaderContext();
        jvc.setJavaReadingStrategy(new JavaReaderStatic());

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(processDefinition);

        final Collection<BaseElement> baseElements = modelInstance.getModelElementsByType(BaseElement.class);

        final ElementGraphBuilder graphBuilder = new ElementGraphBuilder(new BpmnScanner(PATH));
        // create data flow graphs
        graphBuilder.createProcessGraph(jvc, fileScanner, modelInstance,
                processDefinition.getPath(), new ArrayList<>(), scanner);

        final Collection<BpmnElement> bpmnElements = getBpmnElements(processDefinition, baseElements, graphBuilder);
        final Collection<ProcessVariable> processVariables = getProcessVariables(bpmnElements);

        Assert.assertEquals(2, processVariables.size());
    }

}
