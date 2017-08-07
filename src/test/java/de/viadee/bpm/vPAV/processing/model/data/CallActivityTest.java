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
package de.viadee.bpm.vPAV.processing.model.data;

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
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import de.viadee.bpm.vPAV.RuntimeConfig;
import de.viadee.bpm.vPAV.processing.ElementGraphBuilder;
import de.viadee.bpm.vPAV.processing.model.graph.IGraph;
import de.viadee.bpm.vPAV.processing.model.graph.Path;

/**
 * test the handling of call activities
 *
 */
public class CallActivityTest {

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

    @Test
    public void testEmbedding() {
        final String PATH = BASE_PATH + "CallActivityTest_embeddingCallActivity.bpmn";
        final File processdefinition = new File(PATH);

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(processdefinition);

        // add reference for called process
        final Map<String, String> processIdToPathMap = new HashMap<String, String>();
        processIdToPathMap.put("calledProcess", BASE_PATH + "CallActivityTest_calledProcess.bpmn");
        processIdToPathMap.put("calledcalledProcess",
                BASE_PATH + "CallActivityTest_calledcalledProcess.bpmn");

        final ElementGraphBuilder graphBuilder = new ElementGraphBuilder(null, processIdToPathMap, null, null);

        // create data flow graphs
        final Collection<String> calledElementHierarchy = new ArrayList<String>();
        final Collection<IGraph> graphCollection = graphBuilder.createProcessGraph(modelInstance,
                processdefinition.getPath(), calledElementHierarchy);

        // calculate invalid paths based on data flow graphs
        final Map<AnomalyContainer, List<Path>> invalidPathMap = graphBuilder
                .createInvalidPaths(graphCollection);

        Assert.assertEquals("there are only three anomalies", 3, invalidPathMap.size());
        Assert.assertNull("variable operation has to be valid",
                invalidPathMap.get(new AnomalyContainer("bla", Anomaly.UR, "_ServiceTask_1gq1azp", null)));
        Assert.assertNull("variable operation has to be valid",
                invalidPathMap.get(new AnomalyContainer("blub", Anomaly.UR, "SequenceFlow_1aa0qpb", null)));
        Assert.assertNull("variable operation has to be valid", invalidPathMap
                .get(new AnomalyContainer("definedVar", Anomaly.UR, "ServiceTask_0mfcclv", null)));

        final Collection<Path> paths1 = invalidPathMap
                .get(new AnomalyContainer("definedVar", Anomaly.UR, "_ServiceTask_01owrcj", null));
        Assert.assertNotNull("variable operation has to be invalid", paths1);
        Assert.assertEquals(
                "[[_StartEvent_1, _SequenceFlow_0nqrfhe, _ServiceTask_1gq1azp, _SequenceFlow_060xg7b, _ExclusiveGateway_08b6vsy, _SequenceFlow_1xdmvz5, _ServiceTask_01owrcj]]",
                paths1.toString());
        final Collection<Path> paths2 = invalidPathMap
                .get(new AnomalyContainer("definedVar", Anomaly.UR, "__ServiceTask_0edbu4z", null));
        Assert.assertNotNull("variable operation has to be invalid", paths2);
        Assert.assertEquals(
                "[[__StartEvent_1, __SequenceFlow_0nqrfhe, __Task_0ac8n27, __SequenceFlow_060xg7b, __ExclusiveGateway_08b6vsy, __SequenceFlow_1xdmvz5, __ServiceTask_0edbu4z]]",
                paths2.toString());
        final Collection<Path> paths3 = invalidPathMap
                .get(new AnomalyContainer("newVar", Anomaly.UR, "SequenceFlow_1xq2ktt", null));
        Assert.assertNotNull("variable operation has to be invalid", paths3);
        Assert.assertEquals(
                "[[StartEvent_1, SequenceFlow_1sofdlp, ServiceTask_1s4v2j8, SequenceFlow_0zgt1ib, _gw_in, CallActivity_0vlq6qr, _gw_out, SequenceFlow_1aa0qpb, ServiceTask_0mfcclv, SequenceFlow_1xq2ktt]]",
                paths3.toString());
    }
}