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
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.BaseElement;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xml.sax.SAXException;

import de.viadee.bpm.vPAV.BpmnScanner;
import de.viadee.bpm.vPAV.RuntimeConfig;
import de.viadee.bpm.vPAV.config.model.Rule;
import de.viadee.bpm.vPAV.processing.model.data.BpmnElement;
import de.viadee.bpm.vPAV.processing.model.data.CheckerIssue;

public class MessageEventCheckerTest {

    private static final String BASE_PATH = "src/test/resources/";

    private static MessageEventChecker checker;

    private static ClassLoader cl;

    private final Rule rule = new Rule("MessageEventChecker", true, null, null, null, null);

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
     * Case: StartEvent has been set with correct message
     *
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws XPathExpressionException
     */
    @Test
    public void testStartEvent() throws ParserConfigurationException, SAXException, IOException {
        final String PATH = BASE_PATH + "MessageEventChecker_testStartEvent.bpmn";
        checker = new MessageEventChecker(rule, new BpmnScanner(PATH));

        // parse bpmn model
        final Collection<CheckerIssue> issues = new ArrayList<CheckerIssue>();

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

        final Collection<BaseElement> baseElements = modelInstance
                .getModelElementsByType(BaseElement.class);

        for (BaseElement baseElement : baseElements) {
            final BpmnElement element = new BpmnElement(PATH, baseElement);
            issues.addAll(checker.check(element));
        }

        if (issues.size() > 0) {
            Assert.fail("correct model generates an issue");
        }
    }

    /**
     * Case: StartEvent has been set with message and expression
     *
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws XPathExpressionException
     */
    @Test
    public void testStartEventWithExpression() throws ParserConfigurationException, SAXException, IOException {
        final String PATH = BASE_PATH + "MessageEventChecker_testStartEventWithExpression.bpmn";
        checker = new MessageEventChecker(rule, new BpmnScanner(PATH));

        // parse bpmn model
        final Collection<CheckerIssue> issues = new ArrayList<CheckerIssue>();

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

        final Collection<BaseElement> baseElements = modelInstance
                .getModelElementsByType(BaseElement.class);

        for (BaseElement baseElement : baseElements) {
            final BpmnElement element = new BpmnElement(PATH, baseElement);
            issues.addAll(checker.check(element));
        }

        if (issues.size() != 1) {
            Assert.fail("model should generate an issue");
        }
    }

    /**
     * Case: EndEvent has been set with wrong message
     *
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws XPathExpressionException
     */
    @Test
    public void testEndEvent() throws ParserConfigurationException, SAXException, IOException {
        final String PATH = BASE_PATH + "MessageEventChecker_testEndEvent.bpmn";
        checker = new MessageEventChecker(rule, new BpmnScanner(PATH));

        // parse bpmn model
        final Collection<CheckerIssue> issues = new ArrayList<CheckerIssue>();

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

        final Collection<BaseElement> baseElements = modelInstance
                .getModelElementsByType(BaseElement.class);

        for (BaseElement baseElement : baseElements) {
            final BpmnElement element = new BpmnElement(PATH, baseElement);
            issues.addAll(checker.check(element));
        }

        if (issues.size() != 1) {
            Assert.fail("collection with the issues is bigger or smaller as expected");
        }
    }

    /**
     * Case: Mixed events with several faults
     *
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws XPathExpressionException
     */
    @Test
    public void testMixedEvents() throws ParserConfigurationException, SAXException, IOException {
        final String PATH = BASE_PATH + "MessageEventChecker_testMixedEvents.bpmn";
        checker = new MessageEventChecker(rule, new BpmnScanner(PATH));

        // parse bpmn model
        final Collection<CheckerIssue> issues = new ArrayList<CheckerIssue>();

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

        final Collection<BaseElement> baseElements = modelInstance
                .getModelElementsByType(BaseElement.class);

        for (BaseElement baseElement : baseElements) {
            final BpmnElement element = new BpmnElement(PATH, baseElement);
            issues.addAll(checker.check(element));
        }

        if (issues.size() != 1) {
            Assert.fail("collection with the issues is bigger or smaller as expected");
        }
    }

}
