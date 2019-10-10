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
package de.viadee.bpm.vPAV.config.reader;

import de.viadee.bpm.vPAV.RuntimeConfig;
import de.viadee.bpm.vPAV.config.model.Rule;
import de.viadee.bpm.vPAV.config.model.RuleSet;
import de.viadee.bpm.vPAV.constants.ConfigConstants;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;

import static org.junit.Assert.*;

public class XmlConfigReaderTest {

    @BeforeClass
	public static void setup() throws MalformedURLException {
		final File file = new File(".");
		final String currentPath = file.toURI().toURL().toString();
		final URL classUrl = new URL(currentPath + "src/test/java");
		final URL[] classUrls = {classUrl};
        ClassLoader cl = new URLClassLoader(classUrls);
		RuntimeConfig.getInstance().setClassLoader(cl);
		RuntimeConfig.getInstance().setTest(true);
	}

	/**
	 * Test loading a correct config file
	 *
	 * @throws ConfigReaderException
	 *             Exception if config file could not be found
	 */
    @Test
	public void testLoadingCorrectXMLConfigFile() throws ConfigReaderException {
		// Given
		XmlConfigReader reader = new XmlConfigReader();

        // When
        RuleSet result = reader.read(ConfigConstants.RULESET);

        // Then
		assertFalse("No element rules could be read", result.getElementRules().isEmpty());
        for (Map.Entry<String, Map<String, Rule>> entry : result.getElementRules().entrySet()) {
            assertFalse("Rules of type " + entry.getKey() + " could not be read.", entry.getValue().isEmpty());
		}
		assertFalse("No model rules could be read", result.getModelRules().isEmpty());
		for (Map.Entry<String, Map<String, Rule>> entry : result.getModelRules().entrySet()) {
			assertFalse("Rules of type " + entry.getKey() + " could not be read.", entry.getValue().isEmpty());
		}
	}

    @Test
	public void testLoadingCorrectXMLConfigFileWithExternalChecker() throws ConfigReaderException {
		// Given
		XmlConfigReader reader = new XmlConfigReader();

		// When
		RuleSet result = reader.read("ruleSetWithExternalChecker.xml");

		// Then
		assertEquals("Not all element rules could be read.", 5, result.getElementRules().size());
		assertNotNull("External checker rule could not be loaded", result.getElementRules().get("TaskNamingConventionCheckerExtern"));
		assertFalse("No model rules could be read", result.getModelRules().isEmpty());
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

		// When
		try {
			RuleSet result = reader.read("non-existing.xml");
            assertNotNull("Exception expected, but no one was thrown.", result);
		} catch (ConfigReaderException e) {
			// load DefaultRuleSet
			RuleSet result = reader.read("ruleSetDefault.xml");
			Map<String, Map<String, Rule>> elementRules = result.getElementRules();

			// Default rules correct
			assertTrue("False Default ruleSet ",
					elementRules.get("JavaDelegateChecker").get("JavaDelegateChecker").isActive());
			assertTrue("False Default ruleSet ",
					elementRules.get("EmbeddedGroovyScriptChecker").get("EmbeddedGroovyScriptChecker").isActive());
			assertFalse("False Default ruleSet ", elementRules.get("VersioningChecker").get("VersioningChecker").isActive());
			assertFalse("False Default ruleSet ", elementRules.get("DmnTaskChecker").get("DmnTaskChecker").isActive());
			assertFalse("False Default ruleSet ", elementRules.get("ProcessVariablesNameConventionChecker")
					.get("ProcessVariablesNameConventionChecker").isActive());
			assertFalse("False Default ruleSet ",
					elementRules.get("TaskNamingConventionChecker").get("TaskNamingConventionChecker").isActive());

			Map<String, Map<String, Rule>> modelRules = result.getModelRules();
			assertTrue("False Default ruleSet ", modelRules.get("DataFlowChecker").get("DataFlowChecker").isActive());
			assertTrue("False Default ruleSet ", modelRules.get("ProcessVariablesModelChecker").get("ProcessVariablesModelChecker").isActive());
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
	 */
	@Test(expected = ConfigReaderException.class)
	public void testLoadingIncorrectXMLConfigFile() throws ConfigReaderException {
		// Given
		XmlConfigReader reader = new XmlConfigReader();

		// When Then
		reader.read("ruleSetIncorrect.xml");
	}
}
