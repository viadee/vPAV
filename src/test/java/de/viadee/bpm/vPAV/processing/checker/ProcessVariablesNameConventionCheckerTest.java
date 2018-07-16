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

import static org.junit.Assert.assertEquals;

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

import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.BaseElement;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xml.sax.SAXException;

import de.viadee.bpm.vPAV.BpmnScanner;
import de.viadee.bpm.vPAV.RuntimeConfig;
import de.viadee.bpm.vPAV.config.model.ElementConvention;
import de.viadee.bpm.vPAV.config.model.ElementFieldTypes;
import de.viadee.bpm.vPAV.config.model.Rule;
import de.viadee.bpm.vPAV.processing.ProcessVariableReader;
import de.viadee.bpm.vPAV.processing.model.data.BpmnElement;
import de.viadee.bpm.vPAV.processing.model.data.CheckerIssue;
import de.viadee.bpm.vPAV.processing.model.data.ProcessVariableOperation;

/**
 * unit tests for ProcessVariablesNameConventionChecker
 *
 */
public class ProcessVariablesNameConventionCheckerTest {

    private static final String BASE_PATH = "src/test/resources/";

    private static ElementChecker checker;

    private static Map<String, String> beanMapping;

    private static ClassLoader cl;

    @BeforeClass
    public static void setup() throws MalformedURLException {
        RuntimeConfig.getInstance().setTest(true);
        beanMapping = new HashMap<String, String>();
        beanMapping.put("myBean", "de.viadee.bpm.vPAV.delegates.TestDelegate");
        RuntimeConfig.getInstance().setBeanMapping(beanMapping);
        checker = new ProcessVariablesNameConventionChecker(createRule(), null);
        final File file = new File(".");
        final String currentPath = file.toURI().toURL().toString();
        final URL classUrl = new URL(currentPath + "src/test/java/");
        final URL[] classUrls = { classUrl };
        cl = new URLClassLoader(classUrls);
        RuntimeConfig.getInstance().setClassLoader(cl);
        RuntimeConfig.getInstance().getResource("en_US");
    }

    @AfterClass
    public static void tearDown() {
        RuntimeConfig.getInstance().setTest(false);
    }

    /**
     * case: internal and external process variables follows the conventions
     * 
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     */
    @Test
    public void testCorrectProcessVariableNames() throws ParserConfigurationException, SAXException, IOException {
        final String PATH = BASE_PATH
                + "ProcessVariablesNameConventionCheckerTest_CorrectProcessVariablesNamingConvention.bpmn";

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

        final Collection<BaseElement> baseElements = modelInstance
                .getModelElementsByType(BaseElement.class);

        final Collection<CheckerIssue> issues = new ArrayList<CheckerIssue>();
        for (final BaseElement baseElement : baseElements) {
            final BpmnElement element = new BpmnElement(PATH, baseElement);
            Map<String, ProcessVariableOperation> variables = new ProcessVariableReader(null, new BpmnScanner(PATH))
                    .getVariablesFromElement(element);
            element.setProcessVariables(variables);

            issues.addAll(checker.check(element));
        }

        assertEquals(0, issues.size());
    }

    /**
     * case: recognise variables which are against the naming conventions (internal/external)
     * 
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     */
    @Test
    public void testWrongProcessVariableNames() throws ParserConfigurationException, SAXException, IOException {
        final String PATH = BASE_PATH
                + "ProcessVariablesNameConventionCheckerTest_WrongProcessVariablesNamingConvention.bpmn";

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

        final Collection<BaseElement> baseElements = modelInstance
                .getModelElementsByType(BaseElement.class);

        final Collection<CheckerIssue> issues = new ArrayList<CheckerIssue>();
        for (final BaseElement baseElement : baseElements) {
            final BpmnElement element = new BpmnElement(PATH, baseElement);
            Map<String, ProcessVariableOperation> variables = new ProcessVariableReader(null, new BpmnScanner(PATH))
                    .getVariablesFromElement(element);
            element.setProcessVariables(variables);

            issues.addAll(checker.check(element));
        }
        int externalConventions = 0;
        int internalConventions = 0;
        for (CheckerIssue issue : issues) {
            if (issue.getMessage().contains("external")) {
                externalConventions++;
            }
            if (issue.getMessage().contains("internal")) {
                internalConventions++;
            }
        }

        assertEquals(4, issues.size());
        assertEquals(1, internalConventions);
        assertEquals(3, externalConventions);
    }

    /**
     * Creates the configuration rule
     *
     * @return rule
     */
    private static Rule createRule() {

        final Collection<ElementConvention> elementConventions = new ArrayList<ElementConvention>();
        final Collection<String> fieldTypeNames = new ArrayList<String>();
        fieldTypeNames.add("Class");
        fieldTypeNames.add("ExternalScript");
        fieldTypeNames.add("DelegateExpression");

        final ElementFieldTypes internalTypes = new ElementFieldTypes(fieldTypeNames, true);

        final ElementConvention internalElementConvention = new ElementConvention("internal",
                internalTypes, null, "int_[a-zA-Z]+");

        final ElementFieldTypes externalTypes = new ElementFieldTypes(fieldTypeNames, false);

        final ElementConvention externalElementConvention = new ElementConvention("external",
                externalTypes, null, "ext_[a-zA-Z]+");
        elementConventions.add(internalElementConvention);
        elementConventions.add(externalElementConvention);

        final Rule rule = new Rule("ProcessVariablesNameConventionChecker", true, null, null,
                elementConventions, null);

        return rule;
    }
}
