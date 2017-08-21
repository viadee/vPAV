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
import org.camunda.bpm.model.bpmn.instance.ExclusiveGateway;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xml.sax.SAXException;

import de.viadee.bpm.vPAV.RuntimeConfig;
import de.viadee.bpm.vPAV.config.model.ElementConvention;
import de.viadee.bpm.vPAV.config.model.Rule;
import de.viadee.bpm.vPAV.processing.model.data.BpmnElement;
import de.viadee.bpm.vPAV.processing.model.data.CheckerIssue;

/**
 * Unit Tests for DmnTaskChecker
 *
 */
public class XorNamingConventionCheckerTest {

    private static final String BASE_PATH = "src/test/resources/";

    private static XorNamingConventionChecker checker;

    private static ClassLoader cl;

    private final Rule rule = createRule();

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
     * Case: XOR gateway with correct naming convention and XOR join that should not be checked
     *
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws XPathExpressionException
     *
     */

    @Test
    public void testOutgoingXor()
            throws XPathExpressionException, ParserConfigurationException, SAXException, IOException {
        final String PATH = BASE_PATH + "XorNamingConventionChecker_outgoingXor.bpmn";
        checker = new XorNamingConventionChecker(rule, PATH);

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

        final Collection<ExclusiveGateway> baseElements = modelInstance
                .getModelElementsByType(ExclusiveGateway.class);

        final BpmnElement element = new BpmnElement(PATH, baseElements.iterator().next());

        final Collection<CheckerIssue> issues = checker.check(element);

        if (issues.size() > 0) {
            Assert.fail("correct naming convention should not generate an issue");
        }
    }

    /**
     * Case: XOR gateway with correct naming convention
     *
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws XPathExpressionException
     *
     */

    @Test
    public void testCorrectXor()
            throws XPathExpressionException, ParserConfigurationException, SAXException, IOException {
        final String PATH = BASE_PATH + "XorNamingConventionChecker_correct.bpmn";
        checker = new XorNamingConventionChecker(rule, PATH);

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

        final Collection<ExclusiveGateway> baseElements = modelInstance
                .getModelElementsByType(ExclusiveGateway.class);

        final BpmnElement element = new BpmnElement(PATH, baseElements.iterator().next());

        final Collection<CheckerIssue> issues = checker.check(element);

        if (issues.size() > 0) {
            Assert.fail("correct naming convention should not generate an issue");
        }
    }

    /**
     * Case: XOR gateway with wrong naming convention
     *
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws XPathExpressionException
     *
     */

    @Test
    public void testFalseXor()
            throws XPathExpressionException, ParserConfigurationException, SAXException, IOException {
        final String PATH = BASE_PATH + "XorNamingConventionChecker_false.bpmn";
        checker = new XorNamingConventionChecker(rule, PATH);

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

        final Collection<ExclusiveGateway> baseElements = modelInstance
                .getModelElementsByType(ExclusiveGateway.class);

        final BpmnElement element = new BpmnElement(PATH, baseElements.iterator().next());

        final Collection<CheckerIssue> issues = checker.check(element);

        if (issues.size() != 1) {
            Assert.fail("wrong naming convention should generate an issue");
        }
    }

    /**
     * Case: XOR gateway with correctly named outgoing edges
     *
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws XPathExpressionException
     *
     */

    @Test
    public void testOutgoingEdgesCorrect()
            throws XPathExpressionException, ParserConfigurationException, SAXException, IOException {
        final String PATH = BASE_PATH + "XorNamingConventionChecker_outgoingEdgesCorrect.bpmn";
        checker = new XorNamingConventionChecker(rule, PATH);

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

        final Collection<ExclusiveGateway> baseElements = modelInstance
                .getModelElementsByType(ExclusiveGateway.class);

        final BpmnElement element = new BpmnElement(PATH, baseElements.iterator().next());

        final Collection<CheckerIssue> issues = checker.check(element);

        if (issues.size() > 1) {
            Assert.fail("correct naming convention should not generate an issue");
        }
    }

    /**
     * Case: XOR gateway with wrongly named outgoing edges
     *
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws XPathExpressionException
     *
     */

    @Test
    public void testOutgoingEdgesFalse()
            throws XPathExpressionException, ParserConfigurationException, SAXException, IOException {
        final String PATH = BASE_PATH + "XorNamingConventionChecker_outgoingEdgesFalse.bpmn";
        checker = new XorNamingConventionChecker(rule, PATH);

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

        final Collection<ExclusiveGateway> baseElements = modelInstance
                .getModelElementsByType(ExclusiveGateway.class);

        final BpmnElement element = new BpmnElement(PATH, baseElements.iterator().next());

        final Collection<CheckerIssue> issues = checker.check(element);

        if (issues.size() != 1) {
            Assert.fail("wrong naming convention of edges should generate an issue");
        }
    }

    /**
     * Creates rule configuration
     *
     * @return rule
     */
    private static Rule createRule() {

        final Collection<ElementConvention> elementConventions = new ArrayList<ElementConvention>();

        final ElementConvention elementConvention = new ElementConvention("convention", null,
                "[A-ZÄÖÜ][a-zäöü]*\\?{1}");
        elementConventions.add(elementConvention);

        final Rule rule = new Rule("XorNamingConventionChecker", true, null, elementConventions, null);

        return rule;
    }

}