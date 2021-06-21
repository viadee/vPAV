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

import de.viadee.bpm.vPAV.processing.code.flow.BpmnElement;
import de.viadee.bpm.vPAV.processing.code.flow.ControlFlowGraph;
import de.viadee.bpm.vPAV.processing.code.flow.FlowAnalysis;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.ServiceTask;
import org.junit.Test;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BpmnScannerTest {

    private static final String BASE_PATH = "src/test/resources/";

    /**
     * Case: BPMN-Model in Version 1
     */
    @Test
    public void testModelVersionV1() {
        final String PATH = BASE_PATH + "BPMN_Model_Version_V1.bpmn";
        final String impClass = "camunda:delegateExpression";

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

        final Collection<ServiceTask> baseElements = modelInstance.getModelElementsByType(ServiceTask.class);

        final BpmnElement element = new BpmnElement(PATH, baseElements.iterator().next(), new ControlFlowGraph(),
                new FlowAnalysis());

        Map.Entry<String, String> imp = BpmnScanner.getImplementation(element.getBaseElement());

        assertEquals("Get unexpected implementation", imp.getKey(), impClass);
    }

    /**
     * Case: BPMN-Model in Version 2
     */
    @Test
    public void testModelVersionV2() {
        final String PATH = BASE_PATH + "BPMN_Model_Version_V2.bpmn";
        final String impEx = "camunda:class";

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

        final Collection<ServiceTask> baseElements = modelInstance.getModelElementsByType(ServiceTask.class);

        final BpmnElement element = new BpmnElement(PATH, baseElements.iterator().next(), new ControlFlowGraph(),
                new FlowAnalysis());

        Map.Entry<String, String> imp = BpmnScanner.getImplementation(element.getBaseElement());

        assertEquals("Get unexpected implementation", imp.getKey(), impEx);
    }

    /**
     * Case: BPMN-Model in Version 3
     */
    @Test
    public void testModelVersionV3() {
        final String PATH = BASE_PATH + "BPMN_Model_Version_V3.bpmn";
        final String impDel = "camunda:expression";

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

        final Collection<ServiceTask> baseElements = modelInstance.getModelElementsByType(ServiceTask.class);

        final BpmnElement element = new BpmnElement(PATH, baseElements.iterator().next(), new ControlFlowGraph(),
                new FlowAnalysis());

        Map.Entry<String, String> imp = BpmnScanner.getImplementation(element.getBaseElement());

        assertEquals("Get unexpected implementation", impDel, imp.getKey());
        assertEquals("Get unexpected implementation reference",
                "org.camunda.bpm.platform.example.servlet.ExampleServiceTask", imp.getValue());
    }

    /**
     * Case: Test getScriptType
     */
    @Test
    public void testGetScriptType() {
        final String PATH = BASE_PATH + "BPMN_Model_Version_V1.bpmn";
        final String scriptType = "inputParameter";

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

        final Collection<ServiceTask> baseElements = modelInstance.getModelElementsByType(ServiceTask.class);

        final BpmnElement element = new BpmnElement(PATH, baseElements.iterator().next(), new ControlFlowGraph(),
                new FlowAnalysis());

        List<String> scripts = BpmnScanner.getScriptTypes(element.getBaseElement());

        assertTrue("Get unexpected implementation", scripts.contains(scriptType));
    }
}
