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
package de.viadee.bpm.vpav.processing.checker;

import de.viadee.bpm.vpav.IssueService;
import de.viadee.bpm.vpav.RuntimeConfig;
import de.viadee.bpm.vpav.config.model.Rule;
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

/**
 * unit tests for class SignalEventChecker
 *
 */
public class SignalEventCheckerTest {

    private static final String BASE_PATH = "src/test/resources/processing/checker/";

    private static SignalEventChecker checker;

    private final Rule rule = new Rule("SignalEventChecker", true, null, null, null, null);

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
     * Case: Tasks without Expressions
     *
     */
    @Test
    public void testCorrectModel() {
        final String PATH = BASE_PATH + "SignalEventChecker_Correct.bpmn";
        checker = new SignalEventChecker(rule);

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

        final Collection<BaseElement> baseElements = modelInstance.getModelElementsByType(BaseElement.class);

        for (BaseElement baseElement : baseElements) {
            final BpmnElement element = new BpmnElement(PATH, baseElement, new ControlFlowGraph(), new FlowAnalysis());
            checker.check(element);
        }

        if (IssueService.getInstance().getIssues().size() > 0) {
            Assert.fail("correct model generates an issue");
        }
    }

    /**
     * Case: Multiple SignalStartEvents with the same signal name
     *
     */
    @Test
    public void testWrongModel() {
        final String PATH = BASE_PATH + "SignalEventChecker_Wrong.bpmn";
        checker = new SignalEventChecker(rule);

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

        final Collection<BaseElement> baseElements = modelInstance.getModelElementsByType(BaseElement.class);

        for (BaseElement baseElement : baseElements) {
            final BpmnElement element = new BpmnElement(PATH, baseElement, new ControlFlowGraph(), new FlowAnalysis());
            checker.check(element);
        }

        if (IssueService.getInstance().getIssues().size() != 1) {
            Assert.fail("incorrect model should generate an issue");
        }
    }

    @Before
    public void clearIssues() {
        IssueService.getInstance().clear();
    }
}
