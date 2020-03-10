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
package de.viadee.bpm.vPAV;

import de.viadee.bpm.vPAV.constants.CamundaMethodServices;
import de.viadee.bpm.vPAV.processing.JavaReaderStatic;
import de.viadee.bpm.vPAV.processing.code.flow.*;
import de.viadee.bpm.vPAV.processing.model.data.ProcessVariableOperation;
import de.viadee.bpm.vPAV.processing.model.data.VariableOperation;
import org.camunda.bpm.model.bpmn.instance.BaseElement;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import soot.*;
import soot.jimple.internal.JimpleLocal;
import soot.toolkits.graph.Block;

import java.util.*;

import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProcessVariablesCreatorTest {

    @BeforeClass
    public static void setupSoot() {
        RuntimeConfig.getInstance().setTest(true);
        // TODO rework the whole soot setup + filescanner because static and runtime is mixed up
        FileScanner.setupSootClassPaths(new LinkedList<>());
        new JavaReaderStatic().setupSoot();
        Scene.v().loadNecessaryClasses();
    }

    @Test
    public void testBlockIterator() {
        // de.viadee.bpm.vPAV.delegates.SimpleDelegate
        SootClass sc = Scene.v().forceResolve("de.viadee.bpm.vPAV.delegates.SimpleDelegate", SootClass.SIGNATURES);
        SootMethod method = sc.getMethodByName("execute");
        ControlFlowGraph cfg = new ControlFlowGraph();
        ProcessVariablesCreator vr = new ProcessVariablesCreator(
                new BpmnElement("", null, cfg, null),
                null, null);
        ArrayList<Value> args = new ArrayList<>();
        // TODO check if jimple local is really the correct parameter type
        args.add(new JimpleLocal("r1", RefType.v(CamundaMethodServices.DELEGATE)));
        vr.blockIterator(SootResolverSimplified.getBlockFromMethod(method), args);

        // Three methods, method 1 interrupted by method calls = four nodes
        Collection<BasicNode> nodes = cfg.getNodes().values();
        Assert.assertEquals(4, nodes.size());
        Iterator<BasicNode> iterator = nodes.iterator();
        Node node1 = (Node) iterator.next();
        Node node2 = (Node) iterator.next();
        Node node3 = (Node) iterator.next();
        Node node4 = (Node) iterator.next();
        Assert.assertEquals(2, node1.getOperations().size());
        Assert.assertEquals(1, node2.getOperations().size());
        Assert.assertEquals(1, node3.getOperations().size());
        Assert.assertEquals(1, node4.getOperations().size());
        Assert.assertSame(node1.getBlock(), node4.getBlock());
        Assert.assertEquals("variableOneChanged", node4.getKilled().get("0").getName());
    }

    @Test
    public void testHandleProcessVariableManipulation() {
        ControlFlowGraph cfg = new ControlFlowGraph();
        // Test that new nodes are created when the block changes
        Block blockOne = mock(Block.class);
        Block blockTwo = mock(Block.class);
        ProcessVariableOperation blockOneOpOne = new ProcessVariableOperation("var1",
                VariableOperation.WRITE, null);
        ProcessVariableOperation blockOneOpTwo = new ProcessVariableOperation("var2",
                VariableOperation.WRITE, null);
        ProcessVariableOperation blockTwoOpOne = new ProcessVariableOperation("var3",
                VariableOperation.WRITE, null);

        ProcessVariablesCreator vr = new ProcessVariablesCreator(new BpmnElement("", null, cfg, null), null, null);
        vr.handleProcessVariableManipulation(blockOne, blockOneOpOne);
        vr.handleProcessVariableManipulation(blockOne, blockOneOpTwo);
        vr.handleProcessVariableManipulation(blockTwo, blockTwoOpOne);

        Collection<BasicNode> nodes = cfg.getNodes().values();
        Iterator<BasicNode> iterator = nodes.iterator();
        Assert.assertEquals(2, nodes.size());
        Assert.assertEquals(2, iterator.next().getOperations().size());
        Assert.assertEquals(1, iterator.next().getOperations().size());
    }

    @Test
    public void testBlockWithIf() {
        SootClass sc = Scene.v().forceResolve("de.viadee.bpm.vPAV.processing.SimpleObject", SootClass.SIGNATURES);
        SootMethod method = sc.getMethodByName("methodWithIf");
        ControlFlowGraph cfg = new ControlFlowGraph();
        BaseElement baseElement = mock(BaseElement.class);
        when(baseElement.getId()).thenReturn("ServiceTask");
        ProcessVariablesCreator vr = new ProcessVariablesCreator(
                new BpmnElement("", baseElement, cfg, null),
                null, null, "Process_1");
        ArrayList<Value> args = new ArrayList<>();
        // TODO check if jimple local is really the correct parameter type
        args.add(new JimpleLocal("r1", RefType.v(CamundaMethodServices.DELEGATE)));
        vr.blockIterator(SootResolverSimplified.getBlockFromMethod(method), args);

        Assert.assertEquals(4, cfg.getNodes().size());
        Assert.assertEquals(VariableOperation.READ, vr.lastNode().getOperations().values().iterator().next().getOperation());
        Assert.assertEquals(2, vr.lastNode().getPredecessors().size());
        AnalysisElement ifNode = vr.lastNode().getPredecessors().get(0);
        AnalysisElement elseNode = vr.lastNode().getPredecessors().get(1);
        Assert.assertEquals(VariableOperation.WRITE, ifNode.getOperations().values().iterator().next().getOperation());
        Assert.assertEquals(VariableOperation.DELETE, elseNode.getOperations().values().iterator().next().getOperation());
        Assert.assertEquals(1, ifNode.getPredecessors().size());
        Assert.assertEquals(1, elseNode.getPredecessors().size());
        Assert.assertSame(ifNode.getPredecessors().get(0), elseNode.getPredecessors().get(0));
        AnalysisElement startNode = elseNode.getPredecessors().get(0);
        Assert.assertEquals(VariableOperation.READ, startNode.getOperations().values().iterator().next().getOperation());
    }
}
