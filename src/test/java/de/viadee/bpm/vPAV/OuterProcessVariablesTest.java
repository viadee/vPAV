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
package de.viadee.bpm.vPAV;

import static org.junit.Assert.fail;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.junit.BeforeClass;
import org.junit.Test;

import de.viadee.bpm.vPAV.processing.ElementGraphBuilder;
import de.viadee.bpm.vPAV.processing.model.data.BpmnElement;
import de.viadee.bpm.vPAV.processing.model.data.ProcessVariable;
import edu.emory.mathcs.backport.java.util.Arrays;

/**
 * unit test checks, whether outer variables on data flow graph will be set
 *
 * a) startProcessInstanceByKey, b) startProcessInstanceByMessage and correlateMessage
 *
 * assumption: examining process variables in source code is done before
 */
public class OuterProcessVariablesTest {

    private static final String BASE_PATH = "src/test/resources/";

    private static ClassLoader cl;

    @BeforeClass
    public static void setup() throws MalformedURLException {
        final File file = new File(".");
        final String currentPath = file.toURI().toURL().toString();
        final URL classUrl = new URL(currentPath + "src/test/java");
        final URL[] classUrls = { classUrl };
        cl = new URLClassLoader(classUrls);
        RuntimeConfig.getInstance().setClassLoader(cl);
    }

    /**
     * checks outer variables set on process start by key
     */
    @Test
    public void testStartProcessByKey() {
        //// Given...
        final String PATH = BASE_PATH + "OuterProcessVariablesTest_StartProcessByKey.bpmn";
        final File processdefinition = new File(PATH);
        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(processdefinition);
        final Map<String, Collection<String>> processIdToVariables = new HashMap<String, Collection<String>>();
        processIdToVariables.put("OuterProcessVariablesTest", Arrays.asList(new String[] { "a" }));

        //// When...
        final ElementGraphBuilder graphBuilder = new ElementGraphBuilder(null, null, null,
                processIdToVariables);
        // create data flow graphs
        graphBuilder.createProcessGraph(modelInstance, processdefinition.getPath(), new ArrayList<String>());

        //// Then...
        // select start event from process and check variable
        final BpmnElement element = graphBuilder.getElement("StartEvent_0m79sut");
        final Map<String, ProcessVariable> variables = element.getProcessVariables();
        if (variables == null) {
            fail("there are no outer variables set on data flow graph");
        }
        if (!variables.containsKey("a")) {
            fail("variable a is not set on data flow graph");
        }
    }

    /**
     * checks outer process variables set on process start by message and message correlation
     */
    @Test
    public void testMessageCorrelation() {
        /// Given
        final String PATH = BASE_PATH + "OuterProcessVariablesTest_MessageCorrelation.bpmn";
        final File processdefinition = new File(PATH);

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(processdefinition);

        final Map<String, Collection<String>> messageIdToVariables = new HashMap<String, Collection<String>>();
        messageIdToVariables.put("startMessage", Arrays.asList(new String[] { "a" }));
        messageIdToVariables.put("intermediateMessage", Arrays.asList(new String[] { "b" }));

        /// When
        final ElementGraphBuilder graphBuilder = new ElementGraphBuilder(null, null,
                messageIdToVariables, null);
        // create data flow graphs
        graphBuilder.createProcessGraph(modelInstance, processdefinition.getPath(), new ArrayList<String>());

        /// Then
        // select start event from process and check variable
        final BpmnElement startEvent = graphBuilder.getElement("StartEvent_05bq8nu");
        final Map<String, ProcessVariable> startVariables = startEvent.getProcessVariables();
        if (startVariables == null) {
            fail("there are no outer variables set on data flow graph (message start event)");
        }

        if (!startVariables.containsKey("a")) {
            fail("variable a is not set on data flow graph (message start event)");
        }

        // select intermediate event from process and check variable
        final BpmnElement intermediateEvent = graphBuilder.getElement("IntermediateCatchEvent_103fbi3");
        final Map<String, ProcessVariable> intermediateVariables = intermediateEvent
                .getProcessVariables();
        if (intermediateVariables == null) {
            fail("there are no outer variables set on data flow graph (message intermediate event)");
        }
        if (!intermediateVariables.containsKey("b")) {
            fail("variable b is not set on data flow graph (message intermediate event)");
        }
    }
}
