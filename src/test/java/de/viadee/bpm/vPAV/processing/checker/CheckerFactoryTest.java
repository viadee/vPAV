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

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xml.sax.SAXException;

import de.viadee.bpm.vPAV.BpmnScanner;
import de.viadee.bpm.vPAV.RuntimeConfig;
import de.viadee.bpm.vPAV.config.model.Rule;
import de.viadee.bpm.vPAV.config.model.Setting;
import de.viadee.bpm.vPAV.processing.ConfigItemNotFoundException;

/**
 * unit tests for class CheckerFactoryTest
 *
 */
public class CheckerFactoryTest {

    private static final String BASE_PATH = "src/test/resources/";

    private static ClassLoader cl;

    private final static Setting setting = new Setting("WrongChecker", null, null, null, false,
            "de.viadee.vPAV_checker_plugin");

    private static Map<String, Setting> settings = new HashMap<String, Setting>();

    private static Map<String, Rule> rules = new HashMap<String, Rule>();

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
     *
     * @throws ConfigItemNotFoundException
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     */
    @Test
    public void testCorrectInternalChecker()
            throws ConfigItemNotFoundException, ParserConfigurationException, SAXException, IOException {
        Rule rule = new Rule("JavaDelegateChecker", true, null, null, null, null);
        rules.put("JavaDelegateChecker", rule);
        CheckerFactory checkerFactory = new CheckerFactory();

        Collection<ElementChecker> cElChecker = checkerFactory.createCheckerInstances(rules, null,
                new BpmnScanner(PATH));

        assertTrue("Collection of Checker should not be empty", !cElChecker.isEmpty());
    }

    /**
     * Test wrong internal checker
     *
     * @throws ConfigItemNotFoundException
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     */
    @Test
    public void testIncorrectInternalChecker()
            throws ConfigItemNotFoundException, ParserConfigurationException, SAXException, IOException {
        Rule rule = new Rule("WrongChecker", true, null, null, null, null);
        rules.put("WrongChecker", rule);
        CheckerFactory checkerFactory = new CheckerFactory();
        
        Collection<ElementChecker> cElChecker = checkerFactory.createCheckerInstances(rules, null,
                new BpmnScanner(PATH));

        assertTrue("Collection of Checker should be empty", cElChecker.isEmpty());
    }

    /**
     * Test wrong external Checker
     *
     * @throws ConfigItemNotFoundException
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     */
    @Test
    public void testIncorrectExternalChecker()
            throws ConfigItemNotFoundException, ParserConfigurationException, SAXException, IOException {
        settings.put("external_Location", setting);
        Rule rule = new Rule("WrongChecker", true, null, settings, null, null);
        rules.put("WrongChecker", rule);
        CheckerFactory checkerFactory = new CheckerFactory();
        
        Collection<ElementChecker> cElChecker = checkerFactory.createCheckerInstances(rules, null,
                new BpmnScanner(PATH));

        assertTrue("Collection of Checker should be empty", cElChecker.isEmpty());
    }

    @After
    public void clearLists() {
        rules.clear();
        settings.clear();
    }
}
