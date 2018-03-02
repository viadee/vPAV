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
import java.util.HashMap;
import java.util.Map;

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
import de.viadee.bpm.vPAV.config.model.ModelConvention;
import de.viadee.bpm.vPAV.config.model.Rule;
import de.viadee.bpm.vPAV.config.model.Setting;
import de.viadee.bpm.vPAV.processing.model.data.BpmnElement;
import de.viadee.bpm.vPAV.processing.model.data.CheckerIssue;

/**
 * unit tests for class ExtensionChecker
 *
 */
public class ExtensionCheckerTest {

    private static final String BASE_PATH = "src/test/resources/";

    private static ExtensionChecker checker;

    private static ClassLoader cl;

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
     * Case: Extension Key-pair in task is correct
     *
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws XPathExpressionException
     */

    @Test
    public void testExtensionChecker_Correct()
            throws XPathExpressionException, ParserConfigurationException, SAXException, IOException {
        final String PATH = BASE_PATH + "ExtensionCheckerTest_Correct.bpmn";
        checker = new ExtensionChecker(createRule(), new BpmnScanner(PATH));

        final Collection<CheckerIssue> issues = new ArrayList<CheckerIssue>();

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

        final Collection<BaseElement> baseElements = modelInstance
                .getModelElementsByType(BaseElement.class);

        for (BaseElement event : baseElements) {
            final BpmnElement element = new BpmnElement(PATH, event);
            issues.addAll(checker.check(element));
        }

        if (issues.size() > 0) {
            Assert.fail("Correct value pair should not generate an issue");
        }
    }

    /**
     * Case: Extension Key-pair in task is wrong
     *
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws XPathExpressionException
     */

    @Test
    public void testExtensionChecker_Wrong()
            throws XPathExpressionException, ParserConfigurationException, SAXException, IOException {
        final String PATH = BASE_PATH + "ExtensionCheckerTest_Wrong.bpmn";
        checker = new ExtensionChecker(createRule(), new BpmnScanner(PATH));

        final Collection<CheckerIssue> issues = new ArrayList<CheckerIssue>();

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

        final Collection<BaseElement> baseElements = modelInstance
                .getModelElementsByType(BaseElement.class);

        for (BaseElement event : baseElements) {
            final BpmnElement element = new BpmnElement(PATH, event);
            issues.addAll(checker.check(element));
        }

        if (issues.size() != 1) {
            Assert.fail("Wrong value pair should generate an issue");
        }
    }

    /**
     * Case: Extension Key-pair in task is missing a value
     *
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws XPathExpressionException
     */

    @Test
    public void testExtensionChecker_NoValue()
            throws XPathExpressionException, ParserConfigurationException, SAXException, IOException {
        final String PATH = BASE_PATH + "ExtensionCheckerTest_NoValue.bpmn";
        checker = new ExtensionChecker(createRule(), new BpmnScanner(PATH));

        final Collection<CheckerIssue> issues = new ArrayList<CheckerIssue>();

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

        final Collection<BaseElement> baseElements = modelInstance
                .getModelElementsByType(BaseElement.class);

        for (BaseElement event : baseElements) {
            final BpmnElement element = new BpmnElement(PATH, event);
            issues.addAll(checker.check(element));
        }

        if (issues.size() != 1) {
            Assert.fail("Wrong value pair should generate an issue");
        }
    }

    /**
     * Case: Extension Key-pair in task is missing the key
     *
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws XPathExpressionException
     */

    @Test
    public void testExtensionChecker_NoKey()
            throws XPathExpressionException, ParserConfigurationException, SAXException, IOException {
        final String PATH = BASE_PATH + "ExtensionCheckerTest_NoKey.bpmn";
        checker = new ExtensionChecker(createRule(), new BpmnScanner(PATH));

        final Collection<CheckerIssue> issues = new ArrayList<CheckerIssue>();

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

        final Collection<BaseElement> baseElements = modelInstance
                .getModelElementsByType(BaseElement.class);

        for (BaseElement event : baseElements) {
            final BpmnElement element = new BpmnElement(PATH, event);
            issues.addAll(checker.check(element));
        }

        if (issues.size() != 2) {
            Assert.fail("Wrong value pair should generate an issue");
        }
    }

    /**
     * Case: Check extension Key-pair for specified task
     *
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws XPathExpressionException
     */

    @Test
    public void testExtensionChecker_WithId()
            throws XPathExpressionException, ParserConfigurationException, SAXException, IOException {
        final String PATH = BASE_PATH + "ExtensionCheckerTest_WithId.bpmn";
        checker = new ExtensionChecker(createRule2(), new BpmnScanner(PATH));

        final Collection<CheckerIssue> issues = new ArrayList<CheckerIssue>();

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

        final Collection<BaseElement> baseElements = modelInstance
                .getModelElementsByType(BaseElement.class);

        for (BaseElement event : baseElements) {
            final BpmnElement element = new BpmnElement(PATH, event);
            issues.addAll(checker.check(element));
        }

        if (issues.size() != 1) {
            Assert.fail("Wrong value pair should generate an issue");
        }
    }

    /**
     * Case: Check extension Key-pair for specified task
     *
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws XPathExpressionException
     */

    @Test
    public void testExtensionChecker_NoRequiredAttribute()
            throws XPathExpressionException, ParserConfigurationException, SAXException, IOException {
        final String PATH = BASE_PATH + "ExtensionCheckerTest_NoRequiredAttribute.bpmn";
        checker = new ExtensionChecker(createRule3(), new BpmnScanner(PATH));

        final Collection<CheckerIssue> issues = new ArrayList<CheckerIssue>();

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

        final Collection<BaseElement> baseElements = modelInstance
                .getModelElementsByType(BaseElement.class);

        for (BaseElement event : baseElements) {
            final BpmnElement element = new BpmnElement(PATH, event);
            issues.addAll(checker.check(element));
        }

        if (issues.size() != 3) {
            Assert.fail("Wrong value pair should generate an issue");
        }
    }

    /**
     * Creates rule configuration
     *
     * @return rule
     */
    private static Rule createRule() {

        final ArrayList<ModelConvention> modelConventions = createModelConventions();

        final Map<String, Setting> settings = new HashMap<String, Setting>();
        final Setting setting = new Setting("SETTING1", null, "ServiceTask", null, true, "\\d+");
        final Setting setting1 = new Setting("SETTING2", null, "ServiceTask", null, true, "\\d+");

        settings.put("SETTING1", setting);
        settings.put("SETTING2", setting1);

        final Rule rule = new Rule("ExtensionChecker", true, null, settings, null, modelConventions);

        return rule;
    }

    /**
     * Creates second rule configuration
     *
     * @return rule
     */
    private static Rule createRule2() {

        final ArrayList<ModelConvention> modelConventions = createModelConventions();

        final Map<String, Setting> settings = new HashMap<String, Setting>();
        final Setting setting = new Setting("SETTING1", null, null, "Task_26x8g8d", false, "\\d+");
        final Setting setting1 = new Setting("SETTING2", null, null, null, false, "\\d+");

        settings.put("SETTING1", setting);
        settings.put("SETTING2", setting1);

        final Rule rule = new Rule("ExtensionChecker", true, null, settings, null, modelConventions);

        return rule;
    }

    /**
     * Creates third rule configuration
     *
     * @return rule
     */
    private static Rule createRule3() {

        final ArrayList<ModelConvention> modelConventions = createModelConventions();

        final Map<String, Setting> settings = new HashMap<String, Setting>();
        final Setting setting = new Setting("SETTING1", null, "ServiceTask", null, false, "\\d+");
        final Setting setting1 = new Setting("SETTING2", null, "ServiceTask", null, true, "\\d+");

        settings.put("SETTING1", setting);
        settings.put("SETTING2", setting1);

        final Rule rule = new Rule("ExtensionChecker", true, null, settings, null, modelConventions);

        return rule;
    }

    /**
     * Creates model conventions
     *
     * @return modelConventions
     */
    private static ArrayList<ModelConvention> createModelConventions() {
        final ArrayList<ModelConvention> modelConventions = new ArrayList<ModelConvention>();
        final ModelConvention modelConvention1 = new ModelConvention("ServiceTask");
        final ModelConvention modelConvention2 = new ModelConvention("BusinessRuleTask");
        final ModelConvention modelConvention3 = new ModelConvention("SkriptTask");

        modelConventions.add(modelConvention1);
        modelConventions.add(modelConvention2);
        modelConventions.add(modelConvention3);
        return modelConventions;
    }

}
