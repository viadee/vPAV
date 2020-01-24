/**
 * BSD 3-Clause License
 * <p>
 * Copyright © 2019, viadee Unternehmensberatung AG
 * All rights reserved.
 * <p>
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * <p>
 * * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 * <p>
 * * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * <p>
 * * Neither the name of the copyright holder nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * <p>
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

import de.viadee.bpm.vPAV.BpmnScanner;
import de.viadee.bpm.vPAV.IssueService;
import de.viadee.bpm.vPAV.RuntimeConfig;
import de.viadee.bpm.vPAV.config.model.ElementConvention;
import de.viadee.bpm.vPAV.config.model.Rule;
import de.viadee.bpm.vPAV.config.model.Setting;
import de.viadee.bpm.vPAV.processing.code.flow.BpmnElement;
import de.viadee.bpm.vPAV.processing.code.flow.ControlFlowGraph;
import de.viadee.bpm.vPAV.processing.code.flow.FlowAnalysis;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.ExclusiveGateway;
import org.junit.After;
import org.junit.Assert;
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
 * Unit Tests for DmnTaskChecker
 */
public class XorConventionCheckerTest {

    private static final String BASE_PATH = "src/test/resources/";

    private static XorConventionChecker checker;

    private static ClassLoader cl;

    private final Rule rule = createRule();

    private final Rule ruleDefault = createRuleDefault();

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
     * Case: XOR gateway with correct naming convention and XOR join that should not
     * be checked
     */

    @Test
    public void testOutgoingXor() {
        final String PATH = BASE_PATH + "XorConventionChecker_outgoingXor.bpmn";
        checker = new XorConventionChecker(rule, new BpmnScanner(PATH));

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

        final Collection<ExclusiveGateway> baseElements = modelInstance.getModelElementsByType(ExclusiveGateway.class);

        final BpmnElement element = new BpmnElement(PATH, baseElements.iterator().next(), new ControlFlowGraph(),
                new FlowAnalysis());

        checker.check(element);

        if (IssueService.getInstance().getIssues().size() > 0) {
            Assert.fail("correct naming convention should not generate an issue");
        }
    }

    /**
     * Case: XOR gateway with correct naming convention
     */

    @Test
    public void testCorrectXor() {
        final String PATH = BASE_PATH + "XorConventionChecker.bpmn";
        checker = new XorConventionChecker(rule, new BpmnScanner(PATH));

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

        final Collection<ExclusiveGateway> baseElements = modelInstance.getModelElementsByType(ExclusiveGateway.class);

        final BpmnElement element = new BpmnElement(PATH, baseElements.iterator().next(), new ControlFlowGraph(),
                new FlowAnalysis());

        checker.check(element);

        if (IssueService.getInstance().getIssues().size() > 0) {
            Assert.fail("correct naming convention should not generate an issue");
        }
    }

    /**
     * Case: XOR gateway with wrong naming convention
     */

    @Test
    public void testFalseXor() {
        final String PATH = BASE_PATH + "XorConventionChecker.bpmn";
        checker = new XorConventionChecker(rule, new BpmnScanner(PATH));

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

        // Create issue: name of XOR gate is not set
        ExclusiveGateway myExclusiveGateway = modelInstance.getModelElementById("MyExclusiveGateway");
        myExclusiveGateway.setName("");

        final Collection<ExclusiveGateway> baseElements = modelInstance.getModelElementsByType(ExclusiveGateway.class);
        final BpmnElement element = new BpmnElement(PATH, baseElements.iterator().next(), new ControlFlowGraph(),
                new FlowAnalysis());

        checker.check(element);

        if (IssueService.getInstance().getIssues().size() != 1) {
            Assert.fail("wrong naming convention should generate an issue");
        }
    }

    /**
     * Case: XOR gateway with correctly named outgoing edges
     */
    @Test
    public void testOutgoingEdgesCorrect() {
        final String PATH = BASE_PATH + "XorConventionChecker.bpmn";
        checker = new XorConventionChecker(rule, new BpmnScanner(PATH));

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

        final Collection<ExclusiveGateway> baseElements = modelInstance.getModelElementsByType(ExclusiveGateway.class);

        final BpmnElement element = new BpmnElement(PATH, baseElements.iterator().next(), new ControlFlowGraph(),
                new FlowAnalysis());

        checker.check(element);

        if (IssueService.getInstance().getIssues().size() > 1) {
            Assert.fail("correct naming convention should not generate an issue");
        }
    }

    /**
     * Case: XOR gateway with wrongly named outgoing edges
     */
    @Test
    public void testOutgoingEdgesFalse() {
        final String PATH = BASE_PATH + "XorConventionChecker_outgoingEdgesFalse.bpmn";
        checker = new XorConventionChecker(rule, new BpmnScanner(PATH));

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

        final Collection<ExclusiveGateway> baseElements = modelInstance.getModelElementsByType(ExclusiveGateway.class);

        final BpmnElement element = new BpmnElement(PATH, baseElements.iterator().next(), new ControlFlowGraph(),
                new FlowAnalysis());

        checker.check(element);

        if (IssueService.getInstance().getIssues().size() != 1) {
            Assert.fail("wrong naming convention of edges should generate an issue");
        }
    }

    /**
     * Creates rule configuration
     *
     * @return rule
     */
    private static Rule createRule() {

        final Collection<ElementConvention> elementConventions = new ArrayList<>();

        final ElementConvention elementConvention = new ElementConvention("convention", null, null,
                "[A-ZÄÖÜ][a-zäöü]*\\?{1}");

        final ElementConvention elementConvention2 = new ElementConvention("convention2", null, null,
                "[A-ZÄÖÜ][a-zäöü]*");
        elementConventions.add(elementConvention);
        elementConventions.add(elementConvention2);

        return new Rule("XorConventionChecker", true, null, null, elementConventions, null);
    }

    /**
     * Creates rule configuration
     *
     * @return rule
     */
    private static Rule createRuleDefault() {

        final Collection<ElementConvention> elementConventions = new ArrayList<>();

        final ElementConvention elementConvention = new ElementConvention("convention", null, null,
                "[A-ZÄÖÜ][a-zäöü]*\\?{1}");

        final ElementConvention elementConvention2 = new ElementConvention("convention2", null, null,
                "[A-ZÄÖÜ][a-zäöü]*");
        elementConventions.add(elementConvention);
        elementConventions.add(elementConvention2);

        Setting s = new Setting("requiredDefault", null, null, null, false, "true");
        final Map<String, Setting> settings = new HashMap<>();
        settings.put("requiredDefault", s);

        return new Rule("XorConventionChecker", true, null, settings, elementConventions, null);
    }

    /**
     * Case: XOR gateway with no default Path
     */
    @Test
    public void testDefaultPath() {
        final String PATH = BASE_PATH + "XorConventionChecker.bpmn";
        checker = new XorConventionChecker(ruleDefault, new BpmnScanner(PATH));

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

        final Collection<ExclusiveGateway> baseElements = modelInstance.getModelElementsByType(ExclusiveGateway.class);

        final BpmnElement element = new BpmnElement(PATH, baseElements.iterator().next(), new ControlFlowGraph(),
                new FlowAnalysis());

        checker.check(element);

        if (IssueService.getInstance().getIssues().size() != 1) {
            Assert.fail("no default path should generate an issue");
        }
    }

    /**
     * Case: XOR gateway with correct default Path
     */
    @Test
    public void testCorrectDefaultPath() {
        final String PATH = BASE_PATH + "XorConventionChecker.bpmn";
        checker = new XorConventionChecker(ruleDefault, new BpmnScanner(PATH));

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(new File(PATH));

        // Set default path
        ExclusiveGateway myExclusiveGateway = modelInstance.getModelElementById("MyExclusiveGateway");
        myExclusiveGateway.setDefault(modelInstance.getModelElementById("SequenceFlow_0zux6bg"));

        final Collection<ExclusiveGateway> baseElements = modelInstance.getModelElementsByType(ExclusiveGateway.class);

        final BpmnElement element = new BpmnElement(PATH, baseElements.iterator().next(), new ControlFlowGraph(),
                new FlowAnalysis());

        checker.check(element);

        if (IssueService.getInstance().getIssues().size() != 0) {
            Assert.fail("no default path should generate an issue");
        }
    }

    @After
    public void clearIssues() {
        IssueService.getInstance().clear();
    }
}
