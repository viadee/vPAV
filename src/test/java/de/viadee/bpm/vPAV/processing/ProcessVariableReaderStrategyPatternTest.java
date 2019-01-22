/**
 * BSD 3-Clause License
 *
 * Copyright © 2018, viadee Unternehmensberatung AG
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
package de.viadee.bpm.vPAV.processing;

import de.viadee.bpm.vPAV.FileScanner;
import de.viadee.bpm.vPAV.ProcessApplicationValidator;
import de.viadee.bpm.vPAV.RuntimeConfig;
import de.viadee.bpm.vPAV.constants.ConfigConstants;
import de.viadee.bpm.vPAV.processing.model.data.ProcessVariableOperation;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class ProcessVariableReaderStrategyPatternTest {

    private static ClassLoader cl;

    @BeforeClass
    public static void setup() throws MalformedURLException {
        RuntimeConfig.getInstance().setTest(true);
        final File file = new File(".");
        final String currentPath = file.toURI().toURL().toString();
        final URL classUrl = new URL(currentPath + "src/test/java/");
        final URL resourcesUrl = new URL(currentPath + "src/test/resources/");
        final URL[] classUrls = { classUrl, resourcesUrl };
        cl = new URLClassLoader(classUrls);
        RuntimeConfig.getInstance().setClassLoader(cl);
    }

    @AfterClass
    public static void tearDown() {
        RuntimeConfig.getInstance().setTest(false);
    }

    @Test()
    public void testStrategyPatternProcessVariableReaderStatic() {

    	final FileScanner fileScanner = new FileScanner(new HashMap<>(), ConfigConstants.TEST_JAVAPATH);
        boolean isStatic = true;

        final JavaReaderContext pvc = new JavaReaderContext();
        if (isStatic) {
            pvc.setJavaReadingStrategy(new JavaReaderStatic());
        } else {
            pvc.setJavaReadingStrategy(new JavaReaderRegex());
        }

        final Map<String, ProcessVariableOperation> variables = pvc
                .readJavaDelegate(fileScanner, "de.viadee.bpm.vPAV.delegates.TestDelegateStatic", null, null, null, null);

        assertEquals("Static reader should not find 4 variables since one of them is in a comment", 3,
                variables.size());

    }

    @Test()
    public void testStrategyPatternProcessVariableReaderRegex() {
    	final FileScanner fileScanner = new FileScanner(new HashMap<>(), ConfigConstants.TEST_JAVAPATH);
        boolean isStatic = false;        

        final JavaReaderContext pvc = new JavaReaderContext();
        if (isStatic) {
            pvc.setJavaReadingStrategy(new JavaReaderStatic());
        } else {
            pvc.setJavaReadingStrategy(new JavaReaderRegex());
        }

        final Map<String, ProcessVariableOperation> variables = pvc
                .readJavaDelegate(fileScanner, "de.viadee.bpm.vPAV.delegates.TestDelegateStatic", null, null, null, null);

        assertEquals("RegEx reader should find 4 variables including a false positive in a comment", 4,
                variables.size());

    }

}
