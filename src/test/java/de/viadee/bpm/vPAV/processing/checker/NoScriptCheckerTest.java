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
package de.viadee.bpm.vPAV.processing.checker;

import de.viadee.bpm.vPAV.IssueService;
import de.viadee.bpm.vPAV.RuntimeConfig;
import de.viadee.bpm.vPAV.config.model.Rule;
import de.viadee.bpm.vPAV.config.model.Setting;
import de.viadee.bpm.vPAV.processing.CheckName;
import de.viadee.bpm.vPAV.processing.code.flow.BpmnElement;
import de.viadee.bpm.vPAV.processing.code.flow.ControlFlowGraph;
import de.viadee.bpm.vPAV.processing.code.flow.FlowAnalysis;
import de.viadee.bpm.vPAV.processing.model.data.CheckerIssue;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * unit tests for class NoScriptTasks
 */
public class NoScriptCheckerTest {

    private static final String BASE_PATH = "src/test/resources/";

    private static NoScriptChecker checker;

    private static Map<String, Setting> setting = new HashMap<>();

    private final Rule rule = new Rule("NoScriptChecker", true, null, setting, null, null);

    @BeforeClass
    public static void setup() throws MalformedURLException {
        final File file = new File(".");
        final String currentPath = file.toURI().toURL().toString();
        final URL classUrl = new URL(currentPath + "src/test/java");
        final URL[] classUrls = { classUrl };
        ClassLoader cl = new URLClassLoader(classUrls);
        RuntimeConfig.getInstance().setClassLoader(cl);
        RuntimeConfig.getInstance().getResource("en_US");
    }

    /**
     * Case: BPMN with no Script
     */
    @Test
    public void testModelWithNoScript() {
        final String PATH = BASE_PATH + "NoScriptCheckerTest_ModelWithoutScript.bpmn";
        checker = new NoScriptChecker(rule);

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

        final Collection<ServiceTask> baseElements = modelInstance.getModelElementsByType(ServiceTask.class);

        new BpmnElement(PATH, baseElements.iterator().next(), new ControlFlowGraph(),
                new FlowAnalysis());

        if (IssueService.getInstance().getIssues().size() > 0) {
            Assert.fail("correct model generates an issue");
        }
    }

    /**
     * Case: Model with an InputScript
     */
    @Test
    public void testModelWithInputScript() {
        final String PATH = BASE_PATH + "NoScriptCheckerTest_ModelWithInputScript.bpmn";
        checker = new NoScriptChecker(rule);

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

        final Collection<ServiceTask> baseElements = modelInstance.getModelElementsByType(ServiceTask.class);

        final BpmnElement element = new BpmnElement(PATH, baseElements.iterator().next(), new ControlFlowGraph(),
                new FlowAnalysis());
        final BaseElement baseElement = element.getBaseElement();

        checker.check(element);

        final Collection<CheckerIssue> issues = IssueService.getInstance().getIssues();

        if (issues.size() != 1) {
            Assert.fail("collection with the issues is bigger or smaller as expected");
        } else {
            Assert.assertEquals("Task '" + CheckName.checkName(baseElement) + "' with 'inputParameter' script",
                    issues.iterator().next().getMessage());
        }
    }

    /**
     * Case: Model with an OutputScript
     */
    @Test
    public void testModelWithOutputScript() {
        final String PATH = BASE_PATH + "NoScriptCheckerTest_ModelWithOutputScript.bpmn";
        checker = new NoScriptChecker(rule);

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

        final Collection<ServiceTask> baseElements = modelInstance.getModelElementsByType(ServiceTask.class);

        final BpmnElement element = new BpmnElement(PATH, baseElements.iterator().next(), new ControlFlowGraph(),
                new FlowAnalysis());
        final BaseElement baseElement = element.getBaseElement();

        checker.check(element);

        final Collection<CheckerIssue> issues = IssueService.getInstance().getIssues();

        if (issues.size() != 1) {
            Assert.fail("collection with the issues is bigger or smaller as expected");
        } else {
            Assert.assertEquals("Task '" + CheckName.checkName(baseElement) + "' with 'outputParameter' script",
                    issues.iterator().next().getMessage());
        }
    }

    /**
     * Case: Model with an executionlistenerScript
     */
    @Test
    public void testModelWithExecutionlistenerScript() {
        final String PATH = BASE_PATH + "NoScriptCheckerTest_ModelWithExecutionlistenerScript.bpmn";
        checker = new NoScriptChecker(rule);

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

        final Collection<Gateway> baseElementsGate = modelInstance.getModelElementsByType(Gateway.class);

        final BpmnElement elementGate = new BpmnElement(PATH, baseElementsGate.iterator().next(),
                new ControlFlowGraph(), new FlowAnalysis());
        final BaseElement baseElementGate = elementGate.getBaseElement();

        checker.check(elementGate);

        Collection<CheckerIssue> issues = IssueService.getInstance().getIssues();
        if (issues.size() != 1) {
            Assert.fail("collection with the issues is bigger or smaller as expected");
        } else {
            Assert.assertEquals("Task '" + CheckName.checkName(baseElementGate) + "' with 'executionListener' script",
                    issues.iterator().next().getMessage());
        }
    }

    /**
     * Case: Model with a TasklistenerScript
     */
    @Test
    public void testModelWithTasklistenerScript() {
        final String PATH = BASE_PATH + "NoScriptCheckerTest_ModelWithTasklistenerScript.bpmn";
        checker = new NoScriptChecker(rule);

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

        final Collection<UserTask> baseElements = modelInstance.getModelElementsByType(UserTask.class);

        final BpmnElement element = new BpmnElement(PATH, baseElements.iterator().next(), new ControlFlowGraph(),
                new FlowAnalysis());
        final BaseElement baseElement = element.getBaseElement();

        checker.check(element);

        final Collection<CheckerIssue> issues = IssueService.getInstance().getIssues();

        if (issues.size() != 1) {
            Assert.fail("collection with the issues is bigger or smaller as expected");
        } else {
            Assert.assertEquals("Task '" + CheckName.checkName(baseElement) + "' with 'taskListener' script",
                    issues.iterator().next().getMessage());
        }
    }

    /**
     * Case: Model with a ScriptTask
     */
    @Test
    public void testModelWithScriptTask() {
        final String PATH = BASE_PATH + "NoScriptCheckerTest_ModelWithScriptTask.bpmn";
        checker = new NoScriptChecker(rule);

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

        final Collection<ScriptTask> baseElements = modelInstance.getModelElementsByType(ScriptTask.class);

        final BpmnElement element = new BpmnElement(PATH, baseElements.iterator().next(), new ControlFlowGraph(),
                new FlowAnalysis());
        final BaseElement baseElement = element.getBaseElement();

        checker.check(element);

        final Collection<CheckerIssue> issues = IssueService.getInstance().getIssues();

        if (issues.size() != 1) {
            Assert.fail("collection with the issues is bigger or smaller as expected");
        } else {
            Assert.assertEquals("ScriptTask '" + CheckName.checkName(baseElement) + "' not allowed",
                    issues.iterator().next().getMessage());
        }
    }

    @Test
    public void testScriptInSequenceFlow() {
        checker = new NoScriptChecker(rule);
        BpmnModelInstance modelInstance = Bpmn.createProcess().startEvent().sequenceFlowId("MySequenceFlow").endEvent()
                .done();
        SequenceFlow sequenceFlow = modelInstance.getModelElementById("MySequenceFlow");
        ConditionExpression cond = modelInstance.newInstance(ConditionExpression.class);
        cond.setType("tFormalExpression");
        cond.setLanguage("groovy");
        cond.setTextContent(" status == 'closed'");
        sequenceFlow.setConditionExpression(cond);

        final BpmnElement element = new BpmnElement(null, sequenceFlow, new ControlFlowGraph(),
                new FlowAnalysis());
        checker.check(element);
        final Collection<CheckerIssue> issues = IssueService.getInstance().getIssues();

        if (issues.size() != 1) {
            Assert.fail("collection with the issues is bigger or smaller as expected");
        }
    }

    @Before
    public void clearIssues() {
        IssueService.getInstance().clear();
    }
}
