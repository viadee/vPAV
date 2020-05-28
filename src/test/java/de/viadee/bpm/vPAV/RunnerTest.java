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
package de.viadee.bpm.vPAV;

import de.viadee.bpm.vPAV.config.model.Rule;
import de.viadee.bpm.vPAV.config.model.RuleSet;
import de.viadee.bpm.vPAV.config.reader.ConfigReaderException;
import de.viadee.bpm.vPAV.config.reader.XmlConfigReader;
import de.viadee.bpm.vPAV.constants.ConfigConstants;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.*;

public class RunnerTest {

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
     * Test if parent and child ruleset are merged correctly.
     *
     * @throws ConfigReaderException
     */
    @Test
    public void testMergeRuleSet() throws ConfigReaderException {
        XmlConfigReader reader = new XmlConfigReader();

        RuleSet childRuleSet = reader.read("ruleSets/ruleSetChild.xml");
        RuleSet parentRuleSet = reader.read("ruleSets/parentRuleSet.xml");

        Runner runner = new Runner();
        RuleSet rules = runner.mergeRuleSet(parentRuleSet, childRuleSet);
        Map<String, Map<String, Rule>> elementRules = rules.getElementRules();
        Map<String, Map<String, Rule>> modelRules = rules.getModelRules();
        assertFalse("No rules could be read", elementRules.isEmpty());

        // Check if inheritance worked correctly.
        assertEquals("Number of total element rules is wrong.", 6, elementRules.size());
        assertEquals("Merging of MessageEventChecker rules did not work.", 2,
                elementRules.get("MessageEventChecker").size());
        assertFalse("Child rule of MessageEventChecker was not loaded correctly.",
                elementRules.get("MessageEventChecker").get("messageFalse").isActive());
        assertTrue("Parent rule of MessageEventChecker was not loaded correctly.",
                elementRules.get("MessageEventChecker").get("messageTrue").isActive());
        assertFalse("OverlapChecker rule was not overridden.",
                elementRules.get("OverlapChecker").get("OverlapChecker").isActive());
        assertEquals("Merging of ExtensionChecker rules did not work.", 2, elementRules.get("ExtensionChecker").size());
        assertTrue("Child rule of ExtensionChecker was not loaded.",
                elementRules.get("ExtensionChecker").containsKey("extensionChild"));
        assertTrue("Parent rule of ExtensionChecker was not loaded.",
                elementRules.get("ExtensionChecker").containsKey("ExtensionChecker"));
        assertEquals("TimerExpressionChecker was not correctly merged.", 1,
                elementRules.get("TimerExpressionChecker").size());
        assertEquals("NoScriptChecker rule was not loaded from parent.", 1, elementRules.get("NoScriptChecker").size());
        assertEquals("The two XorConventionCheckers defined in the child were not loaded.", 2,
                elementRules.get("XorConventionChecker").size());
        assertEquals("Number of total model rules is wrong.", 2, modelRules.size());
        assertEquals("Merging of DataFlowChecker rules did not work.", 1, modelRules.get("DataFlowChecker").size());
        assertEquals("Merging of ProcessVariablesModelChecker rules did not work.", 1,
                modelRules.get("ProcessVariablesModelChecker").size());
    }

    @Test
    public void testReadConfig() {
        RuntimeConfig.getInstance().setTest(true);
        Properties properties = new Properties();
        properties.put("ruleSet", "ruleSets/ruleSetChild.xml");
        properties.put("parentRuleSet", "ruleSets/parentRuleSet.xml");
        ConfigConstants.getInstance().setProperties(properties);

        Runner runner = new Runner();
        RuleSet rules = runner.readConfig();

        Map<String, Map<String, Rule>> elementRules = rules.getElementRules();
        assertFalse("No rules could be read", elementRules.isEmpty());
        assertEquals("Number of total element rules is wrong.", 6, elementRules.size());
        assertEquals("Merging of MessageEventChecker rules did not work.", 2,
                elementRules.get("MessageEventChecker").size());
        assertTrue("Parent rule of ExtensionChecker was not loaded.",
                elementRules.get("ExtensionChecker").containsKey("ExtensionChecker"));
        assertEquals("NoScriptChecker rule was not loaded from parent.", 1, elementRules.get("NoScriptChecker").size());
    }
}
