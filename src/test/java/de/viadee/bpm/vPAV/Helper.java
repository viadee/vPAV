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
package de.viadee.bpm.vPAV;

import de.viadee.bpm.vPAV.config.model.RuleSet;
import de.viadee.bpm.vPAV.constants.ConfigConstants;
import de.viadee.bpm.vPAV.processing.ElementGraphBuilder;
import de.viadee.bpm.vPAV.processing.ProcessVariablesScanner;
import de.viadee.bpm.vPAV.processing.code.flow.FlowAnalysis;
import de.viadee.bpm.vPAV.processing.model.data.AnomalyContainer;
import de.viadee.bpm.vPAV.processing.model.graph.Graph;
import de.viadee.bpm.vPAV.processing.model.graph.Path;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.impl.instance.ServiceTaskImpl;
import org.camunda.bpm.model.bpmn.instance.BpmnModelElementInstance;
import org.camunda.bpm.model.xml.ModelInstance;

import java.io.File;
import java.util.*;

public class Helper {

    private static final String BASE_PATH = "src/test/resources/";

    private static final String MODEL_DELEGATE_PATH = BASE_PATH + "ModelWithDelegate_UR.bpmn";

    private static final FileScanner fileScanner = new FileScanner(new RuleSet());

    public static ModelInstance emptyModel = Bpmn.createEmptyModel();

    static {
        fileScanner.setScanPath(ConfigConstants.TEST_JAVAPATH);
    }

    public static Collection<Graph> getModelWithDelegate(String delegateClass, FlowAnalysis flowAnalysis) {
        final ProcessVariablesScanner scanner = new ProcessVariablesScanner(null);
        final Collection<String> calledElementHierarchy = new ArrayList<>();
        final File processDefinition = new File(MODEL_DELEGATE_PATH);
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(processDefinition);
        ServiceTaskImpl serviceTask = modelInstance.getModelElementById("ServiceTask_108g52x");
        serviceTask.setCamundaClass(delegateClass);

        final ElementGraphBuilder graphBuilder = new ElementGraphBuilder(null, null, null, null);

        return graphBuilder.createProcessGraph(fileScanner, modelInstance,
                processDefinition.getPath(), calledElementHierarchy, scanner, flowAnalysis);
    }

    public static Map<AnomalyContainer, List<Path>> getModelWithBeanDelegate(String delegateClass) {
        final Map<String, String> beanMapping = new HashMap<>();
        beanMapping.put("methodDelegate", delegateClass);
        RuntimeConfig.getInstance().setBeanMapping(beanMapping);

        final ProcessVariablesScanner scanner = new ProcessVariablesScanner(null);
        final Collection<String> calledElementHierarchy = new ArrayList<>();
        final File processDefinition = new File(MODEL_DELEGATE_PATH);
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(processDefinition);
        ServiceTaskImpl serviceTask = modelInstance.getModelElementById("ServiceTask_108g52x");
        serviceTask.setCamundaDelegateExpression("${methodDelegate}");
        serviceTask.setCamundaClass(null);

        final ElementGraphBuilder graphBuilder = new ElementGraphBuilder(null, null, null, null);

        FlowAnalysis flowAnalysis = new FlowAnalysis();
        Collection<Graph> graphCollection = graphBuilder.createProcessGraph(fileScanner, modelInstance,
                processDefinition.getPath(), calledElementHierarchy, scanner, flowAnalysis);

        flowAnalysis.analyze(graphCollection);

        return graphBuilder.createInvalidPaths(graphCollection);

    }

    public static <T extends BpmnModelElementInstance> T createElement(String id,
            Class<T> elementClass) {
        T element = emptyModel.newInstance(elementClass);
        element.setAttributeValue("id", id, true);
        return element;
    }

}
