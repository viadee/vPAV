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
import java.util.List;
import java.util.Map;

import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import de.viadee.bpm.vPAV.RuntimeConfig;
import de.viadee.bpm.vPAV.processing.ElementGraphBuilder;
import de.viadee.bpm.vPAV.processing.model.data.Anomaly;
import de.viadee.bpm.vPAV.processing.model.data.AnomalyContainer;

/**
 * Unit Tests for data flow graph creation and calculation of invalid paths
 *
 */
public class GraphCreationTest {

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
     * Case: Data flow graph creation and calculation of invalid paths
     */
    @Test
    public void testGraph() {
        final String PATH = BASE_PATH + "ProcessVariablesModelCheckerTest_GraphCreation.bpmn";
        final File processdefinition = new File(PATH);

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(processdefinition);

        final ElementGraphBuilder graphBuilder = new ElementGraphBuilder();
        // create data flow graphs
        final Collection<IGraph> graphCollection = graphBuilder.createProcessGraph(modelInstance,
                processdefinition.getPath(), new ArrayList<String>());

        // calculate invalid paths based on data flow graphs
        final Map<AnomalyContainer, List<Path>> invalidPathMap = graphBuilder
                .createInvalidPaths(graphCollection);

        // get invalid paths
        final List<Path> validVarTest = invalidPathMap
                .get(new AnomalyContainer("validVar", Anomaly.UR, "SequenceFlow_1mggduw", null));
        Assert.assertNull("valid variable is marked as invalid", validVarTest);

        final List<Path> jepppaTest = invalidPathMap
                .get(new AnomalyContainer("jepppa", Anomaly.DD, "SequenceFlow_0btqo3y", null));
        Assert.assertEquals(
                "[[SequenceFlow_1aapyv6, ServiceTask_108g52x, SequenceFlow_0yhv5j2, ServiceTask_05g4a96, SequenceFlow_09j6ilt, ExclusiveGateway_0su45e1, SequenceFlow_0t7iwpj, Task_0546a8y, SequenceFlow_1m6lt2o, ExclusiveGateway_0fsjxd1, SequenceFlow_0btqo3y], [SequenceFlow_1aapyv6, ServiceTask_108g52x, SequenceFlow_0yhv5j2, ServiceTask_05g4a96, SequenceFlow_09j6ilt, ExclusiveGateway_0su45e1, SequenceFlow_1mggduw, Task_11t5rso, SequenceFlow_06ehu4z, ExclusiveGateway_0fsjxd1, SequenceFlow_0btqo3y]]",
                jepppaTest.toString());

        final List<Path> testHallo2 = invalidPathMap
                .get(new AnomalyContainer("hallo2", Anomaly.UR, "BusinessRuleTask_119jb6t", null));
        Assert.assertEquals(
                "[[StartEvent_1, SequenceFlow_1aapyv6, ServiceTask_108g52x, SequenceFlow_0yhv5j2, ServiceTask_05g4a96, SequenceFlow_09j6ilt, ExclusiveGateway_0su45e1, SequenceFlow_1mggduw, Task_11t5rso, SequenceFlow_1ck3twv, StartEvent_0bcezfo, SequenceFlow_0jkf21p, StartEvent_0sqm3mr, SequenceFlow_1qax2e0, BusinessRuleTask_119jb6t]]",
                testHallo2.toString());

        final List<Path> geloeschteVarTest = invalidPathMap
                .get(new AnomalyContainer("geloeschteVariable", Anomaly.DU, "SequenceFlow_0bi6kaa", null));
        Assert.assertEquals(
                "[[SequenceFlow_09j6ilt, ExclusiveGateway_0su45e1, SequenceFlow_1mggduw, Task_11t5rso, BoundaryEvent_11udorz, SequenceFlow_0bi6kaa]]",
                geloeschteVarTest.toString());
    }
}
