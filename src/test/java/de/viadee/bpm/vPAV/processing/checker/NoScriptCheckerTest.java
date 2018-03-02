/**
 * BSD 3-Clause License
 *
 * Copyright © 2018, viadee Unternehmensberatung GmbH
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

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.BaseElement;
import org.camunda.bpm.model.bpmn.instance.Gateway;
import org.camunda.bpm.model.bpmn.instance.ScriptTask;
import org.camunda.bpm.model.bpmn.instance.ServiceTask;
import org.camunda.bpm.model.bpmn.instance.UserTask;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xml.sax.SAXException;

import de.viadee.bpm.vPAV.BpmnScanner;
import de.viadee.bpm.vPAV.RuntimeConfig;
import de.viadee.bpm.vPAV.config.model.Rule;
import de.viadee.bpm.vPAV.config.model.Setting;
import de.viadee.bpm.vPAV.processing.CheckName;
import de.viadee.bpm.vPAV.processing.model.data.BpmnElement;
import de.viadee.bpm.vPAV.processing.model.data.CheckerIssue;

/**
 * unit tests for class NoScriptTasks
 *
 */
public class NoScriptCheckerTest {

    private static final String BASE_PATH = "src/test/resources/";

    private static NoScriptChecker checker;

    private static ClassLoader cl;

    private static Map<String, Setting> setting = new HashMap<String, Setting>();

    private final Rule rule = new Rule("NoScriptChecker", true, null, setting, null, null);

    @BeforeClass
    public static void setup() throws MalformedURLException {
        final File file = new File(".");
        final String currentPath = file.toURI().toURL().toString();
        final URL classUrl = new URL(currentPath + "src/test/java");
        final URL[] classUrls = { classUrl };
        cl = new URLClassLoader(classUrls);
        RuntimeConfig.getInstance().setClassLoader(cl);
        RuntimeConfig.getInstance().getResource("en_US");
    }

    /**
     * Case: BPMN with no Script
     *
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws XPathExpressionException
     */
    @Test
    public void testModelWithNoScript()
            throws XPathExpressionException, ParserConfigurationException, SAXException, IOException {
        final String PATH = BASE_PATH + "NoScriptCheckerTest_ModelWithoutScript.bpmn";
        checker = new NoScriptChecker(rule, new BpmnScanner(PATH));

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

        final Collection<ServiceTask> baseElements = modelInstance
                .getModelElementsByType(ServiceTask.class);

        final BpmnElement element = new BpmnElement(PATH, baseElements.iterator().next());

        final Collection<CheckerIssue> issues = checker.check(element);

        if (issues.size() > 0) {
            Assert.fail("correct model generates an issue");
        }
    }

    /**
     * Case: Model with an InputScript
     *
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws XPathExpressionException
     */
    @Test
    public void testModelWithInputScript()
            throws XPathExpressionException, ParserConfigurationException, SAXException, IOException {
        final String PATH = BASE_PATH + "NoScriptCheckerTest_ModelWithInputScript.bpmn";
        checker = new NoScriptChecker(rule, new BpmnScanner(PATH));

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

        final Collection<ServiceTask> baseElements = modelInstance
                .getModelElementsByType(ServiceTask.class);

        final BpmnElement element = new BpmnElement(PATH, baseElements.iterator().next());
        final BaseElement baseElement = element.getBaseElement();

        final Collection<CheckerIssue> issues = checker.check(element);

        if (issues.size() != 1) {
            Assert.fail("collection with the issues is bigger or smaller as expected");
        } else {
            Assert.assertEquals("Task '" + CheckName.checkName(baseElement) + "' with 'inputParameter' script",
                    issues.iterator().next().getMessage());
        }
    }

    /**
     * Case: Model with an OutputScript
     *
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws XPathExpressionException
     */
    @Test
    public void testModelWithOutputScript()
            throws XPathExpressionException, ParserConfigurationException, SAXException, IOException {
        final String PATH = BASE_PATH + "NoScriptCheckerTest_ModelWithOutputScript.bpmn";
        checker = new NoScriptChecker(rule, new BpmnScanner(PATH));

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

        final Collection<ServiceTask> baseElements = modelInstance
                .getModelElementsByType(ServiceTask.class);

        final BpmnElement element = new BpmnElement(PATH, baseElements.iterator().next());
        final BaseElement baseElement = element.getBaseElement();

        final Collection<CheckerIssue> issues = checker.check(element);

        if (issues.size() != 1) {
            Assert.fail("collection with the issues is bigger or smaller as expected");
        } else {
            Assert.assertEquals("Task '" + CheckName.checkName(baseElement) + "' with 'outputParameter' script",
                    issues.iterator().next().getMessage());
        }
    }

    /**
     * Case: Model with an executionlistenerScript
     *
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws XPathExpressionException
     */
    @Test
    public void testModelWithExecutionlistenerScript()
            throws XPathExpressionException, ParserConfigurationException, SAXException, IOException {
        final String PATH = BASE_PATH + "NoScriptCheckerTest_ModelWithExecutionlistenerScript.bpmn";
        checker = new NoScriptChecker(rule, new BpmnScanner(PATH));

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

        final Collection<Gateway> baseElementsGate = modelInstance
                .getModelElementsByType(Gateway.class);

        final BpmnElement elementGate = new BpmnElement(PATH, baseElementsGate.iterator().next());
        final BaseElement baseElementGate = elementGate.getBaseElement();

        Collection<CheckerIssue> issues = checker.check(elementGate);

        if (issues.size() != 1) {
            Assert.fail("collection with the issues is bigger or smaller as expected");
        } else {
            Assert.assertEquals(
                    "Task '" + CheckName.checkName(baseElementGate) + "' with 'executionListener' script",
                    issues.iterator().next().getMessage());
        }
    }

    /**
     * Case: Model with a TasklistenerScript
     *
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws XPathExpressionException
     */
    @Test
    public void testModelWithTasklistenerScript()
            throws XPathExpressionException, ParserConfigurationException, SAXException, IOException {
        final String PATH = BASE_PATH + "NoScriptCheckerTest_ModelWithTasklistenerScript.bpmn";
        checker = new NoScriptChecker(rule, new BpmnScanner(PATH));

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

        final Collection<UserTask> baseElements = modelInstance
                .getModelElementsByType(UserTask.class);

        final BpmnElement element = new BpmnElement(PATH, baseElements.iterator().next());
        final BaseElement baseElement = element.getBaseElement();

        final Collection<CheckerIssue> issues = checker.check(element);

        if (issues.size() != 1) {
            Assert.fail("collection with the issues is bigger or smaller as expected");
        } else {
            Assert.assertEquals("Task '" + CheckName.checkName(baseElement) + "' with 'taskListener' script",
                    issues.iterator().next().getMessage());
        }
    }

    /**
     * Case: Model with a ScriptTask
     *
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws XPathExpressionException
     */
    @Test
    public void testModelWithScriptTask()
            throws XPathExpressionException, ParserConfigurationException, SAXException, IOException {
        final String PATH = BASE_PATH + "NoScriptCheckerTest_ModelWithScriptTask.bpmn";
        checker = new NoScriptChecker(rule, new BpmnScanner(PATH));

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

        final Collection<ScriptTask> baseElements = modelInstance
                .getModelElementsByType(ScriptTask.class);

        final BpmnElement element = new BpmnElement(PATH, baseElements.iterator().next());
        final BaseElement baseElement = element.getBaseElement();

        final Collection<CheckerIssue> issues = checker.check(element);

        if (issues.size() != 1) {
            Assert.fail("collection with the issues is bigger or smaller as expected");
        } else {
            Assert.assertEquals(
                    "ScriptTask '" + CheckName.checkName(baseElement) + "' not allowed",
                    issues.iterator().next().getMessage());
        }
    }
}
