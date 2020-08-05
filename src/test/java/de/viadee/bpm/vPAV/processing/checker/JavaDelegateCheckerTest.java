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

import de.viadee.bpm.vPAV.FileScanner;
import de.viadee.bpm.vPAV.IssueService;
import de.viadee.bpm.vPAV.RuntimeConfig;
import de.viadee.bpm.vPAV.config.model.Rule;
import de.viadee.bpm.vPAV.processing.JavaReaderStatic;
import de.viadee.bpm.vPAV.processing.code.flow.BpmnElement;
import de.viadee.bpm.vPAV.processing.code.flow.ControlFlowGraph;
import de.viadee.bpm.vPAV.processing.code.flow.FlowAnalysis;
import de.viadee.bpm.vPAV.processing.model.data.CheckerIssue;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.MessageEventDefinition;
import org.camunda.bpm.model.bpmn.instance.ServiceTask;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import soot.Scene;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * unit tests for class JavaDelegateChecker
 */
public class JavaDelegateCheckerTest {

    private static final String BASE_PATH = "src/test/resources/";

    private static JavaDelegateChecker checker;

    private final Rule rule = new Rule("JavaDelegateChecker", true, null, null, null, null);

    @BeforeClass
    public static void setup() throws IOException {
        RuntimeConfig.getInstance().setTest(true);
        FileScanner.setupSootClassPaths(new LinkedList<>());
        new JavaReaderStatic().setupSoot();
        Scene.v().loadNecessaryClasses();

        final File file = new File(".");
        final String currentPath = file.toURI().toURL().toString();
        final URL classUrl = new URL(currentPath + "src/test/java");
        final URL[] classUrls = { classUrl };
        ClassLoader cl = new URLClassLoader(classUrls);
        RuntimeConfig.getInstance().setClassLoader(cl);
        RuntimeConfig.getInstance().setResource("en_US");

        // Bean-Mapping
        final Map<String, String> beanMapping = new HashMap<>();
        beanMapping.put("FalschesDelegate_bla", "de.test.Test");
        beanMapping.put("testDelegate", "de.viadee.bpm.vPAV.delegates.TestDelegate");
        beanMapping.put("transitiveDelegate", "de.viadee.bpm.vPAV.delegates.TransitiveDelegate");
        RuntimeConfig.getInstance().setBeanMapping(beanMapping);
    }

    /**
     * Case: JavaDelegate has been correct set with transitive interface
     * javaDelegate
     */
    @Test
    public void testJavaDelegateTransitiveInterface() {
        final String PATH = BASE_PATH + "JavaDelegateCheckerTest_TransitiveInterface.bpmn";
        checker = new JavaDelegateChecker(rule);
        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

        final Collection<ServiceTask> baseElements = modelInstance.getModelElementsByType(ServiceTask.class);

        final BpmnElement element = new BpmnElement(PATH, baseElements.iterator().next(), new ControlFlowGraph(),
                new FlowAnalysis());

        checker.check(element);

        if (IssueService.getInstance().getIssues().size() > 0) {
            Assert.fail("correct java delegate generates an issue");
        }
    }

    /**
     * Case: JavaDelegate has been correct set with interface javaDelegate
     */
    @Test
    public void testCorrectJavaDelegateReference() {
        final String PATH = BASE_PATH + "ModelWithDelegate_UR.bpmn";
        checker = new JavaDelegateChecker(rule);

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

        final Collection<ServiceTask> baseElements = modelInstance.getModelElementsByType(ServiceTask.class);

        final BpmnElement element = new BpmnElement(PATH, baseElements.iterator().next(), new ControlFlowGraph(),
                new FlowAnalysis());

        checker.check(element);

        if (IssueService.getInstance().getIssues().size() > 0) {
            Assert.fail("correct java delegate generates an issue");
        }
    }

    /**
     * Case: JavaDelegate has been correct set with interface
     * SignallableActivityBehavior
     */
    @Test
    public void testCorrectJavaDelegateReferenceSignal() {
        final String PATH = BASE_PATH + "JavaDelegateCheckerTest_CorrectJavaDelegateReferenceSignal.bpmn";
        checker = new JavaDelegateChecker(rule);

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

        final Collection<ServiceTask> baseElements = modelInstance.getModelElementsByType(ServiceTask.class);

        final BpmnElement element = new BpmnElement(PATH, baseElements.iterator().next(), new ControlFlowGraph(),
                new FlowAnalysis());

        checker.check(element);

        if (IssueService.getInstance().getIssues().size() > 0) {
            Assert.fail("correct java delegate generates an issue");
        }
    }

    /**
     * Case: JavaDelegate has been correct set with superclass
     * AbstractBpmnActivityBehavior
     */
    @Test
    public void testCorrectJavaDelegateReferenceAbstract() {
        final String PATH = BASE_PATH + "JavaDelegateCheckerTest_CorrectJavaDelegateReferenceAbstract.bpmn";
        checker = new JavaDelegateChecker(rule);

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

        final Collection<ServiceTask> baseElements = modelInstance.getModelElementsByType(ServiceTask.class);

        final BpmnElement element = new BpmnElement(PATH, baseElements.iterator().next(), new ControlFlowGraph(),
                new FlowAnalysis());

        checker.check(element);

        if (IssueService.getInstance().getIssues().size() > 0) {
            Assert.fail("correct java delegate generates an issue");
        }
    }

    /**
     * Case: java delegate has not been set
     */
    @Test
    public void testNoJavaDelegateEntered() {
        final String PATH = BASE_PATH + "JavaDelegateCheckerTest_NoJavaDelegateEntered.bpmn";
        checker = new JavaDelegateChecker(rule);

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

        final Collection<ServiceTask> baseElements = modelInstance.getModelElementsByType(ServiceTask.class);

        final BpmnElement element = new BpmnElement(PATH, baseElements.iterator().next(), new ControlFlowGraph(),
                new FlowAnalysis());

        checker.check(element);

        final Collection<CheckerIssue> issues = IssueService.getInstance().getIssues();

        if (issues.size() != 1) {
            Assert.fail("collection with the issues is bigger or smaller as expected");
        } else {
            Assert.assertEquals("Task 'Service Task 1' with no java class name. (compare model: Details, Java Class)",
                    issues.iterator().next().getMessage());
        }
    }

    /**
     * Case: The path of the java delegate isn't correct
     */
    @Test
    public void testWrongJavaDelegatePath() {
        final String PATH = BASE_PATH + "JavaDelegateCheckerTest_WrongJavaDelegatePath.bpmn";
        checker = new JavaDelegateChecker(rule);

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

        final Collection<ServiceTask> baseElements = modelInstance.getModelElementsByType(ServiceTask.class);

        final BpmnElement element = new BpmnElement(PATH, baseElements.iterator().next(), new ControlFlowGraph(),
                new FlowAnalysis());

        checker.check(element);

        final Collection<CheckerIssue> issues = IssueService.getInstance().getIssues();

        if (issues.size() != 1) {
            Assert.fail("collection with the issues is bigger or smaller as expected");
        } else {
            Assert.assertEquals("Class 'TestDelegate2' in 'camunda:class' not found",
                    issues.iterator().next().getMessage());
        }
    }

    /**
     * Case: The java delegates implements no or a wrong interface
     */
    @Test
    public void testWrongJavaDelegateInterface() {
        final String PATH = BASE_PATH + "JavaDelegateCheckerTest_WrongJavaDelegateInterface.bpmn";
        checker = new JavaDelegateChecker(rule);

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

        final Collection<ServiceTask> baseElements = modelInstance.getModelElementsByType(ServiceTask.class);

        final BpmnElement element = new BpmnElement(PATH, baseElements.iterator().next(), new ControlFlowGraph(),
                new FlowAnalysis());

        checker.check(element);

        final Collection<CheckerIssue> issues = IssueService.getInstance().getIssues();

        if (issues.size() != 1) {
            Assert.fail("collection with the issues is bigger or smaller as expected");
        } else {
            Assert.assertEquals(
                    "Class 'DelegateWithWrongInterface' in 'camunda:class' does not implement/extend the correct interface/class",
                    issues.iterator().next().getMessage());
        }
    }

    /**
     * Case: beanMapping exits, but first map is wrong
     */
    @Test
    public void testWrongJavaDelegateEntered() {
        final String PATH = BASE_PATH + "JavaDelegateCheckerTest_WrongJavaDelegateEntered.bpmn";
        checker = new JavaDelegateChecker(rule);

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

        final Collection<ServiceTask> baseElements = modelInstance.getModelElementsByType(ServiceTask.class);

        final BpmnElement element = new BpmnElement(PATH, baseElements.iterator().next(), new ControlFlowGraph(),
                new FlowAnalysis());

        checker.check(element);

        final Collection<CheckerIssue> issues = IssueService.getInstance().getIssues();

        if (issues.size() != 1) {
            Assert.fail("collection with the issues is bigger or smaller as expected");
        } else {
            Assert.assertEquals("Couldn't find correct beanmapping for delegate expression '${IncorrectBean}'",
                    issues.iterator().next().getMessage());
        }
    }

    /**
     * Case: incorrect JavaDelegateExpression reference
     */
    @Test
    public void testWrongJavaDelegateExpression() {
        final String PATH = BASE_PATH + "JavaDelegateCheckerTest_WrongJavaDelegateExpression.bpmn";
        checker = new JavaDelegateChecker(rule);

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

        final Collection<ServiceTask> baseElements = modelInstance.getModelElementsByType(ServiceTask.class);

        final BpmnElement element = new BpmnElement(PATH, baseElements.iterator().next(), new ControlFlowGraph(),
                new FlowAnalysis());

        checker.check(element);

        final Collection<CheckerIssue> issues = IssueService.getInstance().getIssues();

        if (issues.size() != 1) {
            Assert.fail("collection with the issues is bigger or smaller as expected");
        } else {
            Assert.assertEquals("Class 'TestDelegate2' in 'camunda:delegateExpression' not found",
                    issues.iterator().next().getMessage());
        }
    }

    /**
     * Case: incorrect JavaDelegateExpression reference
     */
    @Test
    public void testWrongClassReference() {
        final String PATH = BASE_PATH + "JavaDelegateCheckerTest_WrongClassReference.bpmn";
        checker = new JavaDelegateChecker(rule);

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

        final Collection<ServiceTask> baseElements = modelInstance.getModelElementsByType(ServiceTask.class);

        final BpmnElement element = new BpmnElement(PATH, baseElements.iterator().next(), new ControlFlowGraph(),
                new FlowAnalysis());

        checker.check(element);

        final Collection<CheckerIssue> issues = IssueService.getInstance().getIssues();

        if (issues.size() != 1) {
            Assert.fail("collection with the issues is bigger or smaller as expected");
        } else {
            Assert.assertEquals("Class '${testDelegate}' in 'camunda:class' not found",
                    issues.iterator().next().getMessage());
        }
    }

    /**
     * Case: implements the interface ActivityBehavior
     */
    @Test
    public void testInterfaceActivityBehavior() {
        final String PATH = BASE_PATH + "JavaDelegateCheckerTest_InterfaceActivityBehavior.bpmn";
        checker = new JavaDelegateChecker(rule);

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

        final Collection<ServiceTask> baseElements = modelInstance.getModelElementsByType(ServiceTask.class);

        final BpmnElement element = new BpmnElement(PATH, baseElements.iterator().next(), new ControlFlowGraph(),
                new FlowAnalysis());

        checker.check(element);

        final Collection<CheckerIssue> issues = IssueService.getInstance().getIssues();

        if (issues.size() != 1) {
            Assert.fail("collection with the issues is bigger or smaller as expected");
        } else {
            Assert.assertEquals(
                    "Class 'DelegateWithInterfaceActivityBehavior' in 'camunda:class' implements the interface ActivityBehavior, which is not a very good practice and should be avoided as much as possible",
                    issues.iterator().next().getMessage());
        }
    }

    @Test
    public void testImplementationInMessageEvent() {
        BpmnModelInstance modelInstance = Bpmn.createProcess().startEvent().intermediateThrowEvent("MyThrowEvent")
                .messageEventDefinition("MyMessageEventDefinition").messageEventDefinitionDone().endEvent().done();
        MessageEventDefinition eventDefinition = modelInstance.getModelElementById("MyMessageEventDefinition");
        eventDefinition.setCamundaClass("de.viadee.bpm.vPAV.delegates.TestDelegate");

        final BpmnElement element = new BpmnElement(null, modelInstance.getModelElementById("MyThrowEvent"),
                new ControlFlowGraph(),
                new FlowAnalysis());

        checker = new JavaDelegateChecker(rule);
        checker.check(element);

        if (IssueService.getInstance().getIssues().size() > 0) {
            Assert.fail("correct java delegate generates an issue");
        }
    }

    @Before
    public void clearIssues() {
        IssueService.getInstance().clear();
    }
}
