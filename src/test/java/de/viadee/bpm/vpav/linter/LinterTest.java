/*
 * BSD 3-Clause License
 *
 * Copyright © 2022, viadee Unternehmensberatung AG
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
package de.viadee.bpm.vpav.linter;

import de.viadee.bpm.vpav.IssueService;
import de.viadee.bpm.vpav.RuntimeConfig;
import de.viadee.bpm.vpav.config.model.Rule;
import de.viadee.bpm.vpav.config.model.Setting;
import de.viadee.bpm.vpav.processing.checker.LinterChecker;
import de.viadee.bpm.vpav.processing.code.flow.BpmnElement;
import de.viadee.bpm.vpav.processing.code.flow.ControlFlowGraph;
import de.viadee.bpm.vpav.processing.code.flow.FlowAnalysis;
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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class LinterTest {

    private static final String BASE_PATH = "src/test/resources/linter/";

    private static LinterChecker checker;

    private final Rule rule = createRule();

    @BeforeClass
    public static void setup() throws MalformedURLException {
        final File file = new File(".");
        final String currentPath = file.toURI().toURL().toString();
        final URL classUrl = new URL(currentPath + "src/test/java");
        final URL[] classUrls = { classUrl };
        ClassLoader cl = new URLClassLoader(classUrls);
        RuntimeConfig.getInstance().setClassLoader(cl);
        RuntimeConfig.getInstance().setResource("en_US");
    }

    /**
     * Case: Incorrect model
     */
    @Test
    public void testConditionalFlowsIncorrect() {
        final String PATH = BASE_PATH + "LinterChecker_ConditionalFlowsIncorrect.bpmn";
        checker = new LinterChecker(rule);

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

        final Collection<BaseElement> baseElements = modelInstance.getModelElementsByType(BaseElement.class);

        for (BaseElement event : baseElements) {
            final BpmnElement element = new BpmnElement(PATH, event, new ControlFlowGraph(), new FlowAnalysis());
            checker.check(element);
        }

        if (IssueService.getInstance().getIssues().size() == 0) {
            Assert.fail("Incorrect model should generate an issue");
        }
    }

    /**
     * Case: Correct model
     */
    @Test
    public void testConditionalFlowsCorrect() {
        final String PATH = BASE_PATH + "LinterChecker_ConditionalFlowsCorrect.bpmn";
        checker = new LinterChecker(rule);

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

        final Collection<BaseElement> baseElements = modelInstance.getModelElementsByType(BaseElement.class);

        for (BaseElement event : baseElements) {
            final BpmnElement element = new BpmnElement(PATH, event, new ControlFlowGraph(), new FlowAnalysis());
            checker.check(element);
        }

        if (IssueService.getInstance().getIssues().size() > 0) {
            Assert.fail("Correct model should not generate an issue");
        }
    }

    /**
     * Creates rule configuration
     *
     * @return rule
     */
    private static Rule createRule() {
        final Map<String, Setting> settings = new HashMap<>();
        final Setting setting = new Setting("Conditional Flows", null, null, null, true, null);

        settings.put("SETTING1", setting);

        return new Rule("LinterChecker", true, null, settings, null, null);
    }

    @Before
    public void clearIssues() {
        IssueService.getInstance().clear();
    }
}
