/**
 * Copyright � 2017, viadee Unternehmensberatung GmbH All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met: 1. Redistributions of source code must retain the above copyright notice, this list of
 * conditions and the following disclaimer. 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation and/or other materials provided with the
 * distribution. 3. All advertising materials mentioning features or use of this software must display the following
 * acknowledgement: This product includes software developed by the viadee Unternehmensberatung GmbH. 4. Neither the
 * name of the viadee Unternehmensberatung GmbH nor the names of its contributors may be used to endorse or promote
 * products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY <COPYRIGHT HOLDER> ''AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO
 * EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package de.viadee.bpm.vPAV.processing.model.graph;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.BpmnModelElementInstance;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.bpmn.instance.SequenceFlow;
import org.camunda.bpm.model.bpmn.instance.Task;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import org.junit.BeforeClass;
import org.junit.Test;

import de.viadee.bpm.vPAV.RuntimeConfig;
import de.viadee.bpm.vPAV.processing.ElementGraphBuilder;
import de.viadee.bpm.vPAV.processing.model.data.AnomalyContainer;

public class ComplexModelTest {

    private static final String BASE_PATH = "src/test/resources/";

    private static ClassLoader cl;

    @BeforeClass
    public static void setup() throws MalformedURLException {
        final File file = new File(".");
        final String currentPath = file.toURI().toURL().toString();
        final URL classUrl = new URL(currentPath + "src/test/java/");
        final URL resourcesUrl = new URL(currentPath + "src/test/resources/");
        final URL[] classUrls = { classUrl, resourcesUrl };
        cl = new URLClassLoader(classUrls);
        RuntimeConfig.getInstance().setClassLoader(cl);
    }

    /**
     * Case: Check complex model for invalid paths
     * 
     * Included: * sub processes * boundary events * java delegate * spring bean * DMN model
     */
    @Test
    public void testGraphOnComplexModel() {
        final String PATH = BASE_PATH + "ComplexModelTest_GraphOnComplexModel.bpmn";
        final File processdefinition = new File(PATH);

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(processdefinition);

        // create many gateway paths to increase the complexity
        createGatewayPaths("ExclusiveGateway_0mkf3hf", "ExclusiveGateway_00pfwgg", modelInstance, 1);
        // createGatewayPaths("ExclusiveGateway_10gsr88", "ExclusiveGateway_13qew7s", modelInstance,
        // 500);

        final Map<String, String> decisionRefToPathMap = new HashMap<String, String>();
        decisionRefToPathMap.put("decision", "table.dmn");

        final Map<String, String> beanMappings = new HashMap<String, String>();
        beanMappings.put("springBean", "de.viadee.bpm.vPAV.delegates.TestDelegate");

        long startTime = System.currentTimeMillis();

        final ElementGraphBuilder graphBuilder = new ElementGraphBuilder(decisionRefToPathMap, null, null, null);
        // create data flow graphs
        final Collection<IGraph> graphCollection = graphBuilder.createProcessGraph(modelInstance,
                processdefinition.getPath(), new ArrayList<String>());

        long estimatedTime = System.currentTimeMillis() - startTime;
        System.out.println("Graph creation: " + estimatedTime + "ms");
        long startTime2 = System.currentTimeMillis();

        // calculate invalid paths based on data flow graphs
        final Map<AnomalyContainer, List<Path>> invalidPathMap = graphBuilder
                .createInvalidPaths(graphCollection);

        long estimatedTime2 = System.currentTimeMillis() - startTime2;
        System.out.println("Graph search: " + estimatedTime2 + "ms");
    }

    /**
     * Create paths between two gateways
     * 
     * @param gateway1_id
     * @param gateway2_id
     * @param modelInstance
     * @param count
     */
    private void createGatewayPaths(final String gateway1_id, final String gateway2_id,
            final BpmnModelInstance modelInstance, final int count) {

        final ModelElementInstance element_von = modelInstance.getModelElementById(gateway1_id);
        final ModelElementInstance element_zu = modelInstance.getModelElementById(gateway2_id);

        for (int i = 1; i < count + 1; i++) {
            // 1) create task
            final Collection<Process> processes = modelInstance.getModelElementsByType(Process.class);
            final FlowNode task = createElement(modelInstance, processes.iterator().next(), "task" + i,
                    Task.class);
            // 2) connect with sequence flows
            createSequenceFlow(modelInstance, processes.iterator().next(), (FlowNode) element_von, task);
            createSequenceFlow(modelInstance, processes.iterator().next(), task, (FlowNode) element_zu);
        }
    }

    /**
     * create an bpmn element
     * 
     * @param parentElement
     * @param id
     * @param elementClass
     * @return
     */
    private <T extends BpmnModelElementInstance> T createElement(
            final BpmnModelInstance modelInstance, BpmnModelElementInstance parentElement, String id,
            Class<T> elementClass) {
        T element = modelInstance.newInstance(elementClass);
        element.setAttributeValue("id", id, true);
        parentElement.addChildElement(element);
        return element;
    }

    /**
     * create a sequence flow
     * 
     * @param process
     * @param from
     * @param to
     * @return
     */
    private SequenceFlow createSequenceFlow(final BpmnModelInstance modelInstance, Process process,
            FlowNode from, FlowNode to) {
        SequenceFlow sequenceFlow = createElement(modelInstance, process,
                from.getId() + "-" + to.getId(), SequenceFlow.class);
        process.addChildElement(sequenceFlow);
        sequenceFlow.setSource(from);
        from.getOutgoing().add(sequenceFlow);
        sequenceFlow.setTarget(to);
        to.getIncoming().add(sequenceFlow);
        return sequenceFlow;
    }
}
