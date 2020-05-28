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
package de.viadee.bpm.vPAV.processing.checker;

import de.viadee.bpm.vPAV.IssueService;
import de.viadee.bpm.vPAV.RuntimeConfig;
import de.viadee.bpm.vPAV.config.model.ModelConvention;
import de.viadee.bpm.vPAV.config.model.Rule;
import de.viadee.bpm.vPAV.config.model.Setting;
import de.viadee.bpm.vPAV.processing.code.flow.BpmnElement;
import de.viadee.bpm.vPAV.processing.code.flow.ControlFlowGraph;
import de.viadee.bpm.vPAV.processing.code.flow.FlowAnalysis;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.BaseElement;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * unit tests for class ExtensionChecker
 *
 */
public class ExtensionCheckerTest {

    private static final String BASE_PATH = "src/test/resources/";

    private static ExtensionChecker checker;

    @BeforeClass
    public static void setup() throws MalformedURLException {

        final File file = new File(".");
        final String currentPath = file.toURI().toURL().toString();
        final URL classUrl = new URL(currentPath + "src/test/java");
        final URL[] classUrls = { classUrl };
        ClassLoader cl = new URLClassLoader(classUrls);
        RuntimeConfig.getInstance().setClassLoader(cl);
        RuntimeConfig.getInstance().getResource("en_US");
    }

    /**
     * Case: Extension Key-pair in task is correct
     */

    @Test
    public void testExtensionChecker_Correct() {
        final String PATH = BASE_PATH + "ExtensionCheckerTest_Correct.bpmn";
        checker = new ExtensionChecker(createRule());

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

        final Collection<BaseElement> baseElements = modelInstance.getModelElementsByType(BaseElement.class);

        for (BaseElement event : baseElements) {
            final BpmnElement element = new BpmnElement(PATH, event, new ControlFlowGraph(), new FlowAnalysis());
            checker.check(element);
        }

        if (IssueService.getInstance().getIssues().size() > 0) {
            Assert.fail("Correct value pair should not generate an issue");
        }
    }

    /**
     * Case: Extension Key-pair in task is wrong
     */

    @Test
    public void testExtensionChecker_Wrong() {
        final String PATH = BASE_PATH + "ExtensionCheckerTest_Wrong.bpmn";
        checker = new ExtensionChecker(createRule());

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

        final Collection<BaseElement> baseElements = modelInstance.getModelElementsByType(BaseElement.class);

        for (BaseElement event : baseElements) {
            final BpmnElement element = new BpmnElement(PATH, event, new ControlFlowGraph(), new FlowAnalysis());
            checker.check(element);
        }

        if (IssueService.getInstance().getIssues().size() != 1) {
            Assert.fail("Wrong value pair should generate an issue");
        }
    }

    /**
     * Case: Extension Key-pair in task is missing a value
     */

    @Test
    public void testExtensionChecker_NoValue() {
        final String PATH = BASE_PATH + "ExtensionCheckerTest_NoValue.bpmn";
        checker = new ExtensionChecker(createRule());

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

        final Collection<BaseElement> baseElements = modelInstance.getModelElementsByType(BaseElement.class);

        for (BaseElement event : baseElements) {
            final BpmnElement element = new BpmnElement(PATH, event, new ControlFlowGraph(), new FlowAnalysis());
            checker.check(element);
        }

        if (IssueService.getInstance().getIssues().size() != 1) {
            Assert.fail("Wrong value pair should generate an issue");
        }
    }

    /**
     * Case: Extension Key-pair in task is missing the key
     */

    @Test
    public void testExtensionChecker_NoKey() {
        final String PATH = BASE_PATH + "ExtensionCheckerTest_NoKey.bpmn";
        checker = new ExtensionChecker(createRule());

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

        final Collection<BaseElement> baseElements = modelInstance.getModelElementsByType(BaseElement.class);

        for (BaseElement event : baseElements) {
            final BpmnElement element = new BpmnElement(PATH, event, new ControlFlowGraph(), new FlowAnalysis());
            checker.check(element);
        }

        if (IssueService.getInstance().getIssues().size() != 2) {
            Assert.fail("Wrong value pair should generate an issue");
        }
    }

    /**
     * Case: Check extension Key-pair for specified task
     */

    @Test
    public void testExtensionChecker_WithId() {
        final String PATH = BASE_PATH + "ExtensionCheckerTest_WithId.bpmn";
        checker = new ExtensionChecker(createRule2());

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

        final Collection<BaseElement> baseElements = modelInstance.getModelElementsByType(BaseElement.class);

        for (BaseElement event : baseElements) {
            final BpmnElement element = new BpmnElement(PATH, event, new ControlFlowGraph(), new FlowAnalysis());
            checker.check(element);
        }

        if (IssueService.getInstance().getIssues().size() != 1) {
            Assert.fail("Wrong value pair should generate an issue");
        }
    }

    /**
     * Case: Check extension Key-pair for specified task
     */

    @Test
    public void testExtensionChecker_NoRequiredAttribute() {
        final String PATH = BASE_PATH + "ExtensionCheckerTest_NoRequiredAttribute.bpmn";
        checker = new ExtensionChecker(createRule3());

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

        final Collection<BaseElement> baseElements = modelInstance.getModelElementsByType(BaseElement.class);

        for (BaseElement event : baseElements) {
            final BpmnElement element = new BpmnElement(PATH, event, new ControlFlowGraph(), new FlowAnalysis());
            checker.check(element);
        }

        if (IssueService.getInstance().getIssues().size() != 3) {
            Assert.fail("Wrong value pair should generate an issue");
        }
    }

    /**
     * Creates rule configuration
     *
     * @return rule
     */
    private static Rule createRule() {

        final ArrayList<ModelConvention> modelConventions = createModelConventions();

        final Map<String, Setting> settings = new HashMap<>();
        final Setting setting = new Setting("SETTING1", null, "ServiceTask", null, true, "\\d+");
        final Setting setting1 = new Setting("SETTING2", null, "ServiceTask", null, true, "\\d+");

        settings.put("SETTING1", setting);
        settings.put("SETTING2", setting1);

        return new Rule("ExtensionChecker", true, null, settings, null, modelConventions);
    }

    /**
     * Creates second rule configuration
     *
     * @return rule
     */
    private static Rule createRule2() {

        final ArrayList<ModelConvention> modelConventions = createModelConventions();

        final Map<String, Setting> settings = new HashMap<>();
        final Setting setting = new Setting("SETTING1", null, null, "Task_26x8g8d", false, "\\d+");
        final Setting setting1 = new Setting("SETTING2", null, null, null, false, "\\d+");

        settings.put("SETTING1", setting);
        settings.put("SETTING2", setting1);

        return new Rule("ExtensionChecker", true, null, settings, null, modelConventions);
    }

    /**
     * Creates third rule configuration
     *
     * @return rule
     */
    private static Rule createRule3() {

        final ArrayList<ModelConvention> modelConventions = createModelConventions();

        final Map<String, Setting> settings = new HashMap<>();
        final Setting setting = new Setting("SETTING1", null, "ServiceTask", null, false, "\\d+");
        final Setting setting1 = new Setting("SETTING2", null, "ServiceTask", null, true, "\\d+");

        settings.put("SETTING1", setting);
        settings.put("SETTING2", setting1);

        return new Rule("ExtensionChecker", true, null, settings, null, modelConventions);
    }

    /**
     * Creates model conventions
     *
     * @return modelConventions
     */
    private static ArrayList<ModelConvention> createModelConventions() {
        final ArrayList<ModelConvention> modelConventions = new ArrayList<>();
        final ModelConvention modelConvention1 = new ModelConvention("ServiceTask");
        final ModelConvention modelConvention2 = new ModelConvention("BusinessRuleTask");
        final ModelConvention modelConvention3 = new ModelConvention("SkriptTask");

        modelConventions.add(modelConvention1);
        modelConventions.add(modelConvention2);
        modelConventions.add(modelConvention3);
        return modelConventions;
    }

    @Before
    public void clearIssues() {
        IssueService.getInstance().clear();
    }

}
