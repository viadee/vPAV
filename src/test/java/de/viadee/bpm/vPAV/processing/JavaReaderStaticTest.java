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
package de.viadee.bpm.vPAV.processing;

import de.viadee.bpm.vPAV.FileScanner;
import de.viadee.bpm.vPAV.RuntimeConfig;
import de.viadee.bpm.vPAV.constants.BpmnConstants;
import de.viadee.bpm.vPAV.processing.code.flow.*;
import de.viadee.bpm.vPAV.processing.model.data.ElementChapter;
import de.viadee.bpm.vPAV.processing.model.data.KnownElementFieldType;
import org.camunda.bpm.model.bpmn.impl.BpmnModelConstants;
import org.camunda.bpm.model.bpmn.instance.BaseElement;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import soot.Scene;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JavaReaderStaticTest {

    private JavaReaderStatic reader;

    @BeforeClass
    public static void setupSoot() {
        RuntimeConfig.getInstance().setTest(true);
        FileScanner.setupSootClassPaths(new LinkedList<>());
        JavaReaderStatic.setupSoot();
        Scene.v().loadNecessaryClasses();
    }

    @Before
    public void setup() {
        reader = new JavaReaderStatic();
    }

    @Test
    public void testGetVariablesFromJavaDelegate() {
        // Test Java Delegate
        BaseElement baseElement = mock(BaseElement.class);
        when(baseElement.getId()).thenReturn("ServiceTask");
        ControlFlowGraph cfg = new ControlFlowGraph();
        BpmnElement element = new BpmnElement("", baseElement, cfg, new FlowAnalysis());
        BasicNode predecessor = new BasicNode(element, ElementChapter.General, KnownElementFieldType.UserDefined);
        BasicNode[] pred = new BasicNode[] { predecessor };

        reader.getVariablesFromJavaDelegate("de.viadee.bpm.vPAV.delegates.SimpleDelegate",
                element, ElementChapter.Implementation, KnownElementFieldType.Class, pred);

        Assert.assertEquals(1, predecessor.getSuccessors().size());
        Assert.assertEquals("ServiceTask__3", pred[0].getId());
        Assert.assertEquals(5, cfg.getOperations().size());
        HashSet<String> variables = cfg.getVariablesOfOperations();
        Assert.assertTrue(variables.contains("variableOne"));
        Assert.assertTrue(variables.contains("variableTwo"));
        Assert.assertTrue(variables.contains("variableThree"));
        Assert.assertTrue(variables.contains("myVariable"));

        // Test Delegate Variable Mapping
        cfg = new ControlFlowGraph();
        element = new BpmnElement("", baseElement, cfg, new FlowAnalysis());
        predecessor = new BasicNode(element, ElementChapter.General, KnownElementFieldType.UserDefined);
        pred[0] = predecessor;
        when(baseElement.getAttributeValueNs(BpmnModelConstants.CAMUNDA_NS,
                BpmnConstants.ATTR_VAR_MAPPING_DELEGATE)).thenReturn("something");
        reader.getVariablesFromJavaDelegate("de.viadee.bpm.vPAV.delegates.DelegatedVarMapping",
                element, ElementChapter.Implementation, KnownElementFieldType.Class, pred);

        Assert.assertEquals(1, predecessor.getSuccessors().size());
        Assert.assertEquals("ServiceTask__1", pred[0].getId());
        Assert.assertEquals(3, cfg.getOperations().size());
        variables = cfg.getVariablesOfOperations();
        Assert.assertTrue(variables.contains("inMapping"));
        Assert.assertTrue(variables.contains("outMapping"));
    }

    @Test
    public void testGetVariablesFromClass() {
        BaseElement baseElement = mock(BaseElement.class);
        when(baseElement.getId()).thenReturn("ServiceTask");
        final BpmnElement element = new BpmnElement("", baseElement, new ControlFlowGraph(),
                new FlowAnalysis());

        final EntryPoint entry = new EntryPoint(
                "de.viadee.bpm.vPAV.delegates.TestDelegateStaticInitialProcessVariables.java", "startProcess",
                "schadensmeldungKfzGlasbruch", "startProcessInstanceByMessage", null);

        new JavaReaderStatic()
                .getVariablesFromClass("de.viadee.bpm.vPAV.delegates.TestDelegateStaticInitialProcessVariables",
                        element, ElementChapter.Implementation, KnownElementFieldType.Class, entry, new BasicNode[1]);

        assertEquals(3, element.getControlFlowGraph().getOperations().size());
        HashSet<String> variables = element.getControlFlowGraph().getVariablesOfOperations();
        assertTrue(variables.contains("kunde"));
        assertTrue(variables.contains("vsnr"));
        assertTrue(variables.contains("kfz"));
    }
}
