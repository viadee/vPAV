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
import org.camunda.bpm.model.bpmn.instance.ServiceTask;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xml.sax.SAXException;

import de.viadee.bpm.vPAV.BpmnScanner;
import de.viadee.bpm.vPAV.RuntimeConfig;
import de.viadee.bpm.vPAV.config.model.Rule;
import de.viadee.bpm.vPAV.processing.model.data.BpmnElement;
import de.viadee.bpm.vPAV.processing.model.data.CheckerIssue;

/**
 * unit tests for class JavaDelegateChecker
 *
 */
public class JavaDelegateCheckerTest {

    private static final String BASE_PATH = "src/test/resources/";

    private static JavaDelegateChecker checker;

    private static ClassLoader cl;

    private final Rule rule = new Rule("JavaDelegateChecker", true, null, null, null, null);

    @BeforeClass
    public static void setup() throws MalformedURLException {

        // Bean-Mapping
        final Map<String, String> beanMapping = new HashMap<String, String>();
        beanMapping.put("FalschesDelegate_bla", "de.test.Test");
        beanMapping.put("testDelegate", "de.viadee.bpm.vPAV.delegates.TestDelegate");
        RuntimeConfig.getInstance().setBeanMapping(beanMapping);

        final File file = new File(".");
        final String currentPath = file.toURI().toURL().toString();
        final URL classUrl = new URL(currentPath + "src/test/java");
        final URL[] classUrls = { classUrl };
        cl = new URLClassLoader(classUrls);
        RuntimeConfig.getInstance().setClassLoader(cl);
        RuntimeConfig.getInstance().getResource("en_US");
    }

    /**
     * Case: JavaDelegate has been correct set with interface javaDelegate
     *
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws XPathExpressionException
     */
    @Test
    public void testCorrectJavaDelegateReference()
            throws XPathExpressionException, ParserConfigurationException, SAXException, IOException {
        final String PATH = BASE_PATH + "JavaDelegateCheckerTest_CorrectJavaDelegateReference.bpmn";
        checker = new JavaDelegateChecker(rule, new BpmnScanner(PATH));

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

        final Collection<ServiceTask> baseElements = modelInstance
                .getModelElementsByType(ServiceTask.class);

        final BpmnElement element = new BpmnElement(PATH, baseElements.iterator().next());

        final Collection<CheckerIssue> issues = checker.check(element);

        if (issues.size() > 0) {
            Assert.fail("correct java delegate generates an issue");
        }
    }

    /**
     * Case: JavaDelegate has been correct set with interface SignallableActivityBehavior
     *
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws XPathExpressionException
     */
    @Test
    public void testCorrectJavaDelegateReferenceSignal()
            throws XPathExpressionException, ParserConfigurationException, SAXException, IOException {
        final String PATH = BASE_PATH + "JavaDelegateCheckerTest_CorrectJavaDelegateReferenceSignal.bpmn";
        checker = new JavaDelegateChecker(rule, new BpmnScanner(PATH));

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

        final Collection<ServiceTask> baseElements = modelInstance
                .getModelElementsByType(ServiceTask.class);

        final BpmnElement element = new BpmnElement(PATH, baseElements.iterator().next());

        final Collection<CheckerIssue> issues = checker.check(element);

        if (issues.size() > 0) {
            Assert.fail("correct java delegate generates an issue");
        }
    }

    /**
     * Case: JavaDelegate has been correct set with superclass AbstractBpmnActivityBehavior
     *
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws XPathExpressionException
     */
    @Test
    public void testCorrectJavaDelegateReferenceAbstract()
            throws XPathExpressionException, ParserConfigurationException, SAXException, IOException {
        final String PATH = BASE_PATH + "JavaDelegateCheckerTest_CorrectJavaDelegateReferenceAbstract.bpmn";
        checker = new JavaDelegateChecker(rule, new BpmnScanner(PATH));

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

        final Collection<ServiceTask> baseElements = modelInstance
                .getModelElementsByType(ServiceTask.class);

        final BpmnElement element = new BpmnElement(PATH, baseElements.iterator().next());

        final Collection<CheckerIssue> issues = checker.check(element);

        if (issues.size() > 0) {
            Assert.fail("correct java delegate generates an issue");
        }
    }

    /**
     * Case: There are no technical attributes
     *
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws XPathExpressionException
     */
    @Test
    public void testNoTechnicalAttributes()
            throws XPathExpressionException, ParserConfigurationException, SAXException, IOException {
        final String PATH = BASE_PATH + "JavaDelegateCheckerTest_NoTechnicalAttributes.bpmn";
        checker = new JavaDelegateChecker(rule, new BpmnScanner(PATH));

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

        final Collection<ServiceTask> baseElements = modelInstance
                .getModelElementsByType(ServiceTask.class);

        final BpmnElement element = new BpmnElement(PATH, baseElements.iterator().next());

        final Collection<CheckerIssue> issues = checker.check(element);

        if (issues.size() != 1) {
            Assert.fail("collection with the issues is bigger or smaller as expected");
        } else {
            Assert.assertEquals("Task 'Service Task 1' with no java class name. (compare model: Details, Java Class)",
                    issues.iterator().next().getMessage());
        }
    }

    /**
     * Case: java delegate has not been set
     *
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws XPathExpressionException
     */
    @Test
    public void testNoJavaDelegateEntered()
            throws XPathExpressionException, ParserConfigurationException, SAXException, IOException {
        final String PATH = BASE_PATH + "JavaDelegateCheckerTest_NoJavaDelegateEntered.bpmn";
        checker = new JavaDelegateChecker(rule, new BpmnScanner(PATH));

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

        final Collection<ServiceTask> baseElements = modelInstance
                .getModelElementsByType(ServiceTask.class);

        final BpmnElement element = new BpmnElement(PATH, baseElements.iterator().next());

        final Collection<CheckerIssue> issues = checker.check(element);

        if (issues.size() != 1) {
            Assert.fail("collection with the issues is bigger or smaller as expected");
        } else {
            Assert.assertEquals("Task 'Service Task 1' with no java class name. (compare model: Details, Java Class)",
                    issues.iterator().next().getMessage());
        }
    }

    /**
     * Case: The path of the java delegate isn't correct
     *
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws XPathExpressionException
     */
    @Test
    public void testWrongJavaDelegatePath()
            throws XPathExpressionException, ParserConfigurationException, SAXException, IOException {
        final String PATH = BASE_PATH + "JavaDelegateCheckerTest_WrongJavaDelegatePath.bpmn";
        checker = new JavaDelegateChecker(rule, new BpmnScanner(PATH));

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

        final Collection<ServiceTask> baseElements = modelInstance
                .getModelElementsByType(ServiceTask.class);

        final BpmnElement element = new BpmnElement(PATH, baseElements.iterator().next());

        final Collection<CheckerIssue> issues = checker.check(element);

        if (issues.size() != 1) {
            Assert.fail("collection with the issues is bigger or smaller as expected");
        } else {
            Assert.assertEquals("Class 'TestDelegate2' in 'camunda:class' not found",
                    issues.iterator().next().getMessage());
        }
    }

    /**
     * Case: The java delegates implements no or a wrong interface
     *
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws XPathExpressionException
     */
    @Test
    public void testWrongJavaDelegateInterface()
            throws XPathExpressionException, ParserConfigurationException, SAXException, IOException {
        final String PATH = BASE_PATH + "JavaDelegateCheckerTest_WrongJavaDelegateInterface.bpmn";
        checker = new JavaDelegateChecker(rule, new BpmnScanner(PATH));

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

        final Collection<ServiceTask> baseElements = modelInstance
                .getModelElementsByType(ServiceTask.class);

        final BpmnElement element = new BpmnElement(PATH, baseElements.iterator().next());

        final Collection<CheckerIssue> issues = checker.check(element);

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
     *
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws XPathExpressionException
     */
    @Test
    public void testWrongJavaDelegateEntered()
            throws XPathExpressionException, ParserConfigurationException, SAXException, IOException {
        final String PATH = BASE_PATH + "JavaDelegateCheckerTest_WrongJavaDelegateEntered.bpmn";
        checker = new JavaDelegateChecker(rule, new BpmnScanner(PATH));

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

        final Collection<ServiceTask> baseElements = modelInstance
                .getModelElementsByType(ServiceTask.class);

        final BpmnElement element = new BpmnElement(PATH, baseElements.iterator().next());

        final Collection<CheckerIssue> issues = checker.check(element);

        if (issues.size() != 1) {
            Assert.fail("collection with the issues is bigger or smaller as expected");
        } else {
            Assert.assertEquals("Couldn't find correct beanmapping for delegate expression '${IncorrectBean}'",
                    issues.iterator().next().getMessage());
        }
    }

    /**
     * Case: incorrect JavaDelegateExpression reference
     *
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws XPathExpressionException
     */
    @Test
    public void testWrongJavaDelegateExpression()
            throws XPathExpressionException, ParserConfigurationException, SAXException, IOException {
        final String PATH = BASE_PATH + "JavaDelegateCheckerTest_WrongJavaDelegateExpression.bpmn";
        checker = new JavaDelegateChecker(rule, new BpmnScanner(PATH));

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

        final Collection<ServiceTask> baseElements = modelInstance
                .getModelElementsByType(ServiceTask.class);

        final BpmnElement element = new BpmnElement(PATH, baseElements.iterator().next());

        final Collection<CheckerIssue> issues = checker.check(element);

        if (issues.size() != 1) {
            Assert.fail("collection with the issues is bigger or smaller as expected");
        } else {
            Assert.assertEquals("Class 'TestDelegate2' in 'camunda:delegateExpression' not found",
                    issues.iterator().next().getMessage());
        }
    }

    /**
     * Case: incorrect JavaDelegateExpression reference
     *
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws XPathExpressionException
     */
    @Test
    public void testWrongClassReference()
            throws XPathExpressionException, ParserConfigurationException, SAXException, IOException {
        final String PATH = BASE_PATH + "JavaDelegateCheckerTest_WrongClassReference.bpmn";
        checker = new JavaDelegateChecker(rule, new BpmnScanner(PATH));

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

        final Collection<ServiceTask> baseElements = modelInstance
                .getModelElementsByType(ServiceTask.class);

        final BpmnElement element = new BpmnElement(PATH, baseElements.iterator().next());

        final Collection<CheckerIssue> issues = checker.check(element);

        if (issues.size() != 1) {
            Assert.fail("collection with the issues is bigger or smaller as expected");
        } else {
            Assert.assertEquals("Class '${testDelegate}' in 'camunda:class' not found",
                    issues.iterator().next().getMessage());
        }
    }

    /**
     * Case: implements the interface ActivityBehavior
     *
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws XPathExpressionException
     */
    @Test
    public void testInterfaceActivityBehavior()
            throws XPathExpressionException, ParserConfigurationException, SAXException, IOException {
        final String PATH = BASE_PATH + "JavaDelegateCheckerTest_InterfaceActivityBehavior.bpmn";
        checker = new JavaDelegateChecker(rule, new BpmnScanner(PATH));

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

        final Collection<ServiceTask> baseElements = modelInstance
                .getModelElementsByType(ServiceTask.class);

        final BpmnElement element = new BpmnElement(PATH, baseElements.iterator().next());

        final Collection<CheckerIssue> issues = checker.check(element);

        if (issues.size() != 1) {
            Assert.fail("collection with the issues is bigger or smaller as expected");
        } else {
            Assert.assertEquals(
                    "Class 'DelegateWithInterfaceActivityBehavior' in 'camunda:class' implements the interface ActivityBehavior, which is not a very good practice and should be avoided as much as possible",
                    issues.iterator().next().getMessage());
        }
    }
}
