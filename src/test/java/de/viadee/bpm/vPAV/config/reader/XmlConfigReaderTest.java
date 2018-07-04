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
package de.viadee.bpm.vPAV.config.reader;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;

import de.viadee.bpm.vPAV.RuntimeConfig;
import de.viadee.bpm.vPAV.config.model.Rule;
import de.viadee.bpm.vPAV.config.model.Setting;
import de.viadee.bpm.vPAV.constants.BpmnConstants;
import de.viadee.bpm.vPAV.constants.ConfigConstants;

public class XmlConfigReaderTest {

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
     * Test loading a correct config file
     *
     * @throws ConfigReaderException
     */
    @Test()
    public void testLoadingCorrectXMLConfigFile() throws ConfigReaderException {
        // Given
        XmlConfigReader reader = new XmlConfigReader();

        // When
        Map<String, Rule> result = reader.read(ConfigConstants.RULESET);

        // Then
        assertFalse("No rules could be read", result.isEmpty());
    }

    /**
     * Test loading a non-existing config file and check for defaults
     *
     * @throws ConfigReaderException
     */
    @Test()
    public void testLoadingNonExistingXMLConfigFile() throws ConfigReaderException {
        // Given
        XmlConfigReader reader = new XmlConfigReader();
        Map<String, Rule> result = null;

        // When
        try {
            result = reader.read("non-existing.xml");
            assertTrue("Exception expected, but no one was thrown.", result != null);
        } catch (ConfigReaderException e) {
            // load DefaultRuleSet
            result = reader.read("ruleSetDefault.xml");

            // Default rules correct
            assertTrue("False Default ruleSet ", result.get("JavaDelegateChecker").isActive());
            assertTrue("False Default ruleSet ", result.get("EmbeddedGroovyScriptChecker").isActive());
            assertFalse("False Default ruleSet ", result.get("VersioningChecker").isActive());
            assertFalse("False Default ruleSet ", result.get("DmnTaskChecker").isActive());
//            assertFalse("False Default ruleSet ", result.get("ProcessVariablesModelChecker").isActive());
            assertFalse("False Default ruleSet ", result.get("ProcessVariablesNameConventionChecker").isActive());
            assertFalse("False Default ruleSet ", result.get("TaskNamingConventionChecker").isActive());
        }
    }

    /**
     * Test loading an incorrect config file (rulename empty)
     *
     *
     */
    @Test(expected = ConfigReaderException.class)
    public void testLoadingIncorrectXMLNameConfigFile() throws ConfigReaderException {
        // Given
        XmlConfigReader reader = new XmlConfigReader();

        // When Then
        reader.read("ruleSetIncorrectName.xml");

    }

    /**
     * Test loading an incorrect config file (no xml)
     *
     *
     */
    @Test(expected = ConfigReaderException.class)
    public void testLoadingIncorrectXMLConfigFile() throws ConfigReaderException {
        // Given
        XmlConfigReader reader = new XmlConfigReader();

        // When Then
        reader.read("ruleSetIncorrect.xml");

    }

    @Test()
    public void testBooleanForTypeOfProcessVariableModelReader() throws ConfigReaderException {

        // Given
        XmlConfigReader reader = new XmlConfigReader();

        // When
        Map<String, Rule> result = reader.read(ConfigConstants.RULESETDEFAULT);

        // Then
        boolean isStatic = false;
        Rule rule = result.get(BpmnConstants.PROCESS_VARIABLE_MODEL_CHECKER);
        if (rule != null) {
            Setting setting = rule.getSettings().get(ConfigConstants.USE_STATIC_ANALYSIS_BOOLEAN);
            if (setting != null) {
                String value = setting.getValue();
                if (setting.getValue().equals("true")) {
                    isStatic = true;
                }
            }
        }
        assertTrue(isStatic);

    }

}
