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
package de.viadee.bpm.vPAV.processing.checker;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.ServiceTask;
import org.junit.BeforeClass;
import org.junit.Test;

import de.viadee.bpm.vPAV.RuntimeConfig;
import de.viadee.bpm.vPAV.config.model.Rule;
import de.viadee.bpm.vPAV.config.model.Setting;
import de.viadee.bpm.vPAV.processing.model.data.BpmnElement;
import de.viadee.bpm.vPAV.processing.model.data.CheckerIssue;

public class VersioningCheckerTest {

    private static final String BASE_PATH = "src/test/resources/";

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
     * Case: test versioning for java class
     */
    @Test
    public void testJavaClassVersioning() {
        final String PATH = BASE_PATH + "VersioningCheckerTest_JavaClassVersioning.bpmn";

        final Rule rule = new Rule("VersioningChecker", true, null, null, null);

        // Versions
        final Collection<String> resourcesNewestVersions = new ArrayList<String>();
        resourcesNewestVersions.add("de/test/TestDelegate_1_2.java");

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

        final Collection<ServiceTask> baseElements = modelInstance
                .getModelElementsByType(ServiceTask.class);

        final BpmnElement element = new BpmnElement(PATH, baseElements.iterator().next());

        final ElementChecker checker = new VersioningChecker(rule, resourcesNewestVersions);
        final Collection<CheckerIssue> issues = checker.check(element);
        assertEquals(1, issues.size());
    }

    /**
     * Case: test versioning for script
     */
    @Test
    public void testScriptVersioning() {
        final String PATH = BASE_PATH + "VersioningCheckerTest_ScriptVersioning.bpmn";

        final Rule rule = new Rule("VersioningChecker", true, null, null, null);

        // Versions
        final Collection<String> resourcesNewestVersions = new ArrayList<String>();
        resourcesNewestVersions.add("de/test/testScript_1_2.groovy");

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

        final Collection<ServiceTask> baseElements = modelInstance
                .getModelElementsByType(ServiceTask.class);

        final BpmnElement element = new BpmnElement(PATH, baseElements.iterator().next());

        final ElementChecker checker = new VersioningChecker(rule, resourcesNewestVersions);
        final Collection<CheckerIssue> issues = checker.check(element);
        assertEquals(1, issues.size());
    }

    /**
     * Case: test versioning for spring bean, with an outdated class reference
     */
    @Test
    public void testBeanVersioningWithOutdatedClass() {
        final String PATH = BASE_PATH + "VersioningCheckerTest_BeanVersioningOutdatedClass.bpmn";

        final Map<String, Setting> settings = new HashMap<String, Setting>();
        settings.put("versioningSchemaClass",
                new Setting("versioningSchemaClass", "([^_]*)_{1}([0-9][_][0-9]{1})\\.(java|groovy)"));

        final Rule rule = new Rule("VersioningChecker", true, settings, null, null);

        // Versions
        final Collection<String> resourcesNewestVersions = new ArrayList<String>();
        resourcesNewestVersions.add("de/test/TestDelegate_1_2.java");

        // Bean-Mapping
        final Map<String, String> beanMapping = new HashMap<String, String>();
        beanMapping.put("myBean_1_1", "de.test.TestDelegate_1_1");
        RuntimeConfig.getInstance().setBeanMapping(beanMapping);

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

        final Collection<ServiceTask> baseElements = modelInstance
                .getModelElementsByType(ServiceTask.class);

        final BpmnElement element = new BpmnElement(PATH, baseElements.iterator().next());

        final ElementChecker checker = new VersioningChecker(rule, resourcesNewestVersions);
        final Collection<CheckerIssue> issues = checker.check(element);
        assertEquals(1, issues.size());
    }
}
