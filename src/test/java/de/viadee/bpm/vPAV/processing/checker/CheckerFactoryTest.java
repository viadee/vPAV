/**
 * BSD 3-Clause License
 *
 * Copyright © 2019, viadee Unternehmensberatung AG
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

import de.viadee.bpm.vPAV.BpmnScanner;
import de.viadee.bpm.vPAV.RuntimeConfig;
import de.viadee.bpm.vPAV.config.model.Rule;
import de.viadee.bpm.vPAV.config.model.RuleSet;
import de.viadee.bpm.vPAV.config.model.Setting;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertTrue;

/**
 * unit tests for class CheckerFactoryTest
 *
 */
public class CheckerFactoryTest {

    private static final String BASE_PATH = "src/test/resources/";

    private static ClassLoader cl;

    private final static Setting setting = new Setting("WrongChecker", null, null, null, false,
            "de.viadee.vPAV_checker_plugin");

    private static Map<String, Setting> settings = new HashMap<>();

    private final String PATH = BASE_PATH + "CheckerFactoryTest.bpmn";

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
     * Test correct viadee checker 'JavaDelegateChecker'
     */
    @Test
    public void testCorrectInternalChecker()
            throws ParserConfigurationException, SAXException, IOException {
        RuleSet rules = new RuleSet();
        Rule rule = new Rule("JavaDelegateChecker", true, null, null, null, null);
        HashMap<String, Rule> delegateRules = new HashMap<>();
        delegateRules.put("JavaDelegateChecker", rule);
        rules.getElementRules().put("JavaDelegateChecker", delegateRules);
        rule = new Rule("DataFlowChecker", true, null, null, null, null);
        HashMap<String, Rule> dataFlowRules = new HashMap<>();
        dataFlowRules.put("DataFlowChecker", rule);
        rules.getModelRules().put("DataFlowChecker", dataFlowRules);

        CheckerFactory checkerFactory = new CheckerFactory();

        Collection[] checkers = checkerFactory.createCheckerInstances(rules, null,
                new BpmnScanner(PATH), null, null, null, null);

        assertTrue("Collection of Element Checker should not be empty", !checkers[0].isEmpty());
        assertTrue("Collection of Model Checker should not be empty", !checkers[1].isEmpty());
    }

    /**
     * Test wrong internal checker
     */
    @Test
    public void testIncorrectInternalChecker()
            throws ParserConfigurationException, SAXException, IOException {
        RuleSet rules = new RuleSet();
        Rule rule = new Rule("WrongChecker", true, null, null, null, null);
        HashMap<String, Rule> wrongCheckerRules = new HashMap<>();
        wrongCheckerRules.put("WrongChecker", rule);
        rules.getElementRules().put("WrongChecker", wrongCheckerRules);
        CheckerFactory checkerFactory = new CheckerFactory();

        Collection[] checkers = checkerFactory.createCheckerInstances(rules, null,
                new BpmnScanner(PATH), null, null, null, null);

        assertTrue("Collection of Element Checker should be empty", checkers[0].isEmpty());
        assertTrue("Collection of Model Checker should be empty", checkers[1].isEmpty());
    }

    /**
     * Test wrong external Checker
     *
     */
    @Test
    public void testIncorrectExternalChecker()
            throws ParserConfigurationException, SAXException, IOException {
        RuleSet rules = new RuleSet();
        settings.put("external_Location", setting);
        Rule rule = new Rule("WrongChecker", true, null, settings, null, null);
        HashMap<String, Rule> wrongCheckerRules = new HashMap<>();
        wrongCheckerRules.put("WrongChecker", rule);
        rules.getElementRules().put("WrongChecker", wrongCheckerRules);
        CheckerFactory checkerFactory = new CheckerFactory();

        Collection[] checkers = checkerFactory.createCheckerInstances(rules, null,
                new BpmnScanner(PATH), null, null, null, null);

        assertTrue("Collection of Checker should be empty", checkers[0].isEmpty());
        assertTrue("Collection of Model Checker should be empty", checkers[1].isEmpty());
    }

    /**
     * Tests whether multiple definitions of a singleton checker are correctly handled.
     */
    @Test
    public void testMultipleRulesOfSingletonChecker() {
        RuleSet rules = new RuleSet();
        // Given rule set
        Rule ruleMessageEvent = new Rule("MessageEventChecker", true, null, null, null, null);
        Rule ruleJavaDelegate = new Rule("JavaDelegateChecker", true, null, null, null, null);
        Rule ruleJavaDelegate2 = new Rule("JavaDelegateChecker", true, null, null, null, null);
        Rule ruleVersioningChecker = new Rule("VersioningChecker", true, null, null, null, null);
        Rule ruleVersioningChecker2 = new Rule("VersioningChecker", true, null, null, null, null);

        Map<String, Rule> ruleMapMessageEvent = new HashMap<>();
        Map<String, Rule> ruleMapJavaDelegate = new HashMap<>();
        Map<String, Rule> ruleMapVersioningChecker = new HashMap<>();

        ruleMapMessageEvent.put("MessageEvent", ruleMessageEvent);
        rules.getElementRules().put("MessageEventChecker", ruleMapMessageEvent);

        ruleMapJavaDelegate.put("JavaDelegate", ruleJavaDelegate);
        ruleMapJavaDelegate.put("JavaDelegate2", ruleJavaDelegate2);
        rules.getElementRules().put("JavaDelegateChecker", ruleMapJavaDelegate);

        ruleMapVersioningChecker.put("Versioning", ruleVersioningChecker);
        ruleMapVersioningChecker.put("Versioning2", ruleVersioningChecker2);
        rules.getElementRules().put("VersioningChecker", ruleMapVersioningChecker);

        // When
        CheckerFactory checkerFactory = new CheckerFactory();
        Collection[] checkers = checkerFactory.createCheckerInstances(rules, null,
                new BpmnScanner(PATH), null, null, null, null);

        // Then
        Assert.assertEquals("Wrong number of checkers was created.", 4, checkers[0].size());
        Assert.assertEquals("Duplicated versioning checker rule was not added to incorrect checker map.", 1, checkerFactory.getIncorrectCheckers().size());
    }

    @After
    public void clearLists() {
        settings.clear();
    }
}
