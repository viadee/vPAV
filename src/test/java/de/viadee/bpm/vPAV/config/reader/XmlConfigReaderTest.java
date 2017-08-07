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
package de.viadee.bpm.vPAV.config.reader;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Map;

import org.junit.Test;

import de.viadee.bpm.vPAV.ConstantsConfig;
import de.viadee.bpm.vPAV.config.model.Rule;

public class XmlConfigReaderTest {

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
        Map<String, Rule> result = reader.read(new File(ConstantsConfig.RULESET));

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

        // When
        Map<String, Rule> result = reader.read(new File("non-existing.xml"));

        // Then
        // DefaultXML correctly read
        assertFalse("No rules could be read - no defaults are returned", result.isEmpty());
        // Default rules correct
        assertTrue("False Default ruleSet ", result.get("JavaDelegateChecker").isActive());
        assertTrue("False Default ruleSet ", result.get("EmbeddedGroovyScriptChecker").isActive());
        assertTrue("False Default ruleSet ", result.get("VersioningChecker").isActive());
        assertFalse("False Default ruleSet ", result.get("DmnTaskChecker").isActive());
        assertFalse("False Default ruleSet ", result.get("ProcessVariablesModelChecker").isActive());
        assertFalse("False Default ruleSet ", result.get("ProcessVariablesNameConventionChecker").isActive());
        assertFalse("False Default ruleSet ", result.get("TaskNamingConventionChecker").isActive());

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
        reader.read(new File("src/test/resources/ruleSetIncorrectName.xml"));

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
        reader.read(new File("src/test/resources/ruleSetIncorrect.xml"));

    }

    /**
     * Test loading a config file with incorrect RegEx
     *
     *
     */
    @Test(expected = ConfigReaderException.class)
    public void testLoadingIncorrectRegExXMLConfigFile() throws ConfigReaderException {
        // Given
        XmlConfigReader reader = new XmlConfigReader();

        reader.read(new File("src/test/resources/ruleSetIncorrectRegEx.xml"));
    }
}
