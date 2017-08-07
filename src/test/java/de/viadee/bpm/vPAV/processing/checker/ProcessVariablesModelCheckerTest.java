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
package de.viadee.bpm.vPAV.processing.checker;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import de.viadee.bpm.vPAV.RuntimeConfig;
import de.viadee.bpm.vPAV.config.model.Rule;
import de.viadee.bpm.vPAV.processing.ElementGraphBuilder;
import de.viadee.bpm.vPAV.processing.model.data.AnomalyContainer;
import de.viadee.bpm.vPAV.processing.model.data.CheckerIssue;
import de.viadee.bpm.vPAV.processing.model.graph.IGraph;
import de.viadee.bpm.vPAV.processing.model.graph.Path;

public class ProcessVariablesModelCheckerTest {

    private static final String BASE_PATH = "src/test/resources/";

    private static BpmnModelInstance modelInstance;

    private static ModelChecker checker;

    private static ClassLoader cl;

    @BeforeClass
    public static void setup() throws MalformedURLException {
        final File file = new File(".");
        final String currentPath = file.toURI().toURL().toString();
        final URL classUrl = new URL(currentPath + "src/test/java");
        final URL[] classUrls = { classUrl };
        cl = new URLClassLoader(classUrls);
        RuntimeConfig.getInstance().setClassLoader(cl);

        final String PATH = BASE_PATH + "ProcessVariablesModelCheckerTest_GraphCreation.bpmn";
        final File processdefinition = new File(PATH);

        // parse bpmn model
        modelInstance = Bpmn.readModelFromFile(processdefinition);

        final ElementGraphBuilder graphBuilder = new ElementGraphBuilder();
        // create data flow graphs
        final Collection<IGraph> graphCollection = graphBuilder.createProcessGraph(modelInstance,
                processdefinition.getPath(), new ArrayList<String>());

        // calculate invalid paths based on data flow graphs
        final Map<AnomalyContainer, List<Path>> invalidPathMap = graphBuilder
                .createInvalidPaths(graphCollection);

        final Rule rule = new Rule("ProcessVariablesModelChecker", true, null, null, null);
        checker = new ProcessVariablesModelChecker(rule, invalidPathMap);
    }

    /**
     * Case: there is an empty script reference
     */
    @Test
    public void testProcessVariablesModelChecker() {
        final Collection<CheckerIssue> issues = checker.check(modelInstance);

        if (issues.size() == 0) {
            Assert.fail("there should be generated an issue");
        }

        Iterator<CheckerIssue> iterator = issues.iterator();
        final CheckerIssue issue1 = iterator.next();
        Assert.assertEquals("SequenceFlow_0bi6kaa", issue1.getElementId());
        Assert.assertEquals("geloeschteVariable", issue1.getVariable());
        Assert.assertEquals("DU", issue1.getAnomaly().toString());
        final CheckerIssue issue2 = iterator.next();
        Assert.assertEquals("SequenceFlow_0btqo3y", issue2.getElementId());
        Assert.assertEquals("jepppa", issue2.getVariable());
        Assert.assertEquals("DD", issue2.getAnomaly().toString());
        final CheckerIssue issue3 = iterator.next();
        Assert.assertEquals("ServiceTask_05g4a96", issue3.getElementId());
        Assert.assertEquals("intHallo", issue3.getVariable().toString());
        Assert.assertEquals("UR", issue3.getAnomaly().toString());
        final CheckerIssue issue4 = iterator.next();
        Assert.assertEquals("BusinessRuleTask_119jb6t", issue4.getElementId());
        Assert.assertEquals("hallo2", issue4.getVariable());
        Assert.assertEquals("UR", issue4.getAnomaly().toString());
    }
}