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
package de.viadee.bpm.vPAV.constants;

import de.viadee.bpm.vPAV.FileScanner;
import de.viadee.bpm.vPAV.RuntimeConfig;
import de.viadee.bpm.vPAV.config.model.RuleSet;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

public class ConfigConstantsTest {

    private static ClassLoader cl;

    @BeforeClass
    public static void setup() throws MalformedURLException {
        final File file = new File(".");
        final String currentPath = file.toURI().toURL().toString();
        final URL classUrl = new URL(currentPath + "src/test/java");
        final URL[] classUrls = {classUrl};
        cl = new URLClassLoader(classUrls);
        RuntimeConfig.getInstance().setClassLoader(cl);
        RuntimeConfig.getInstance().setTest(true);
    }

    @After
    public void resetConfigConstants() {
        ConfigConstants.getInstance().setProperties(new Properties());
    }

    @Test
    public void testCustomBasePathExists() {
        // Set custom basepath.
        Properties myProperties = new Properties();
        myProperties.put("basepath", "src/test/resources/ConfigConstantsTest/");
        ConfigConstants.getInstance().setProperties(myProperties);
        Assert.assertEquals("BasePath could not be successfully injected", "src/test/resources/ConfigConstantsTest/",
                ConfigConstants.getInstance().getBasepath());

        // Create file scanner.
        RuleSet ruleSet = new RuleSet();
        FileScanner fileScanner = new FileScanner(ruleSet);
        Map<String, String> map = fileScanner.getProcessIdToPathMap();
        Assert.assertFalse("BasePath was changed but BPMN model was not found.", map.isEmpty());
        Assert.assertEquals("Wrong bpmn file was found.", "ConfigConstantsTest.bpmn", map.get("Process_1"));
    }

    @Test
    public void testCreateOutputHtmlPropertyExists() {
        Properties myProperties = new Properties();
        myProperties.put("outputhtml", false);
        ConfigConstants.getInstance().setProperties(myProperties);
        Assert.assertTrue("Output Html property should be false.", ConfigConstants.getInstance().isHtmlOutputEnabled());
    }

    @Test
    public void testCreateOutputHtmlPropertyNotExists() {
        Assert.assertTrue("Output Html property should be true because it is not set.", ConfigConstants.getInstance().isHtmlOutputEnabled());
    }

    @Test
    public void testLanguagePropertyExists() {
        Properties myProperties = new Properties();
        myProperties.put("language", "en");
        ConfigConstants.getInstance().setProperties(myProperties);
        Assert.assertEquals("Language was not correctly loaded.", "en", ConfigConstants.getInstance().getLanguage());
    }

    @Test
    public void testLanguagePropertyNotExists() {
        String expected;
        if (Locale.getDefault().toString().equals("de_DE")) {
            expected = "de";
        } else {
            expected = "en";
        }

        Assert.assertEquals("Default language was not used.", expected, ConfigConstants.getInstance().getLanguage());
    }
}
