/*
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
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import soot.*;
import soot.jimple.internal.JimpleLocal;
import soot.toolkits.graph.Block;

import java.util.*;

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
    public void testStartBlockProcessing() {
        BaseElement baseElement = mock(BaseElement.class);
        when(baseElement.getId()).thenReturn("ServiceTask");
        SootClass sc = Scene.v().forceResolve("de.viadee.bpm.vPAV.delegates.SimpleDelegate", SootClass.SIGNATURES);
        SootMethod method = sc.getMethodByName("execute");
        ControlFlowGraph cfg = new ControlFlowGraph();
        ProcessVariablesCreator vr = new ProcessVariablesCreator(
                new BpmnElement("", baseElement, cfg, new FlowAnalysis()),
                null, null, new BasicNode[1]);
        ArrayList<Value> args = new ArrayList<>();
        args.add(new JimpleLocal("r1", RefType.v(CamundaMethodServices.DELEGATE)));
        vr.startBlockProcessing(SootResolverSimplified.getBlockFromMethod(method), args);

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
        Assert.assertEquals("variableOneChanged", node4.getKilled().get("variableOneChanged_4").getName());
    }

    @Test
    public void testHandleProcessVariableManipulation() {
        BaseElement baseElement = mock(BaseElement.class);
        when(baseElement.getId()).thenReturn("ServiceTask");
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

        ProcessVariablesCreator vr = new ProcessVariablesCreator(
                new BpmnElement("", baseElement, cfg, new FlowAnalysis()), null,
                null, new BasicNode[1]);
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
        ControlFlowGraph cfg = runBlockProcessingOnControlFlowObject("methodWithIfElse");

        Assert.assertEquals(4, cfg.getNodes().size());
        Iterator<BasicNode> iterator = cfg.getNodes().values().iterator();
        BasicNode startNode = iterator.next();
        BasicNode ifNode = iterator.next();
        BasicNode lastNode = iterator.next();
        BasicNode elseNode = iterator.next();

        Assert.assertEquals(VariableOperation.READ, lastNode.getOperations().values().iterator().next().getOperation());
        Assert.assertEquals(2, lastNode.getPredecessors().size());
        Assert.assertEquals(VariableOperation.WRITE, ifNode.getOperations().values().iterator().next().getOperation());
        Assert.assertEquals(VariableOperation.DELETE,
                elseNode.getOperations().values().iterator().next().getOperation());
        Assert.assertEquals(1, ifNode.getPredecessors().size());
        Assert.assertEquals(1, elseNode.getPredecessors().size());
        Assert.assertSame(ifNode.getPredecessors().get(0), elseNode.getPredecessors().get(0));
        Assert.assertEquals(VariableOperation.READ,
                startNode.getOperations().values().iterator().next().getOperation());
    }

    @Test
    public void testBlockWithLoop() {
        ControlFlowGraph cfg = runBlockProcessingOnControlFlowObject("methodWithLoop");

        Assert.assertEquals(2, cfg.getNodes().size());
        Iterator<BasicNode> iterator = cfg.getNodes().values().iterator();
        BasicNode loopNode = iterator.next();
        BasicNode afterLoopNode = iterator.next();

        Assert.assertEquals(1, loopNode.getPredecessors().size());
        Assert.assertSame(loopNode, loopNode.getPredecessors().get(0));
        Assert.assertEquals(VariableOperation.READ, loopNode.getOperations().values().iterator().next().getOperation());
        Assert.assertEquals(1, afterLoopNode.getPredecessors().size());
        Assert.assertSame(loopNode, afterLoopNode.getPredecessors().get(0));
        Assert.assertEquals(VariableOperation.DELETE,
                afterLoopNode.getOperations().values().iterator().next().getOperation());
    }

    @Test
    public void testBlockWithNestedControls() {
        ControlFlowGraph cfg = runBlockProcessingOnControlFlowObject("methodWithNestedControls");

        Assert.assertEquals(4, cfg.getNodes().size());
        Iterator<BasicNode> iterator = cfg.getNodes().values().iterator();
        BasicNode ifNode = iterator.next();
        BasicNode loopNode = iterator.next();
        BasicNode elseNode = iterator.next();
        BasicNode afterLoopNode = iterator.next();

        Assert.assertEquals(1, ifNode.getPredecessors().size());
        Assert.assertEquals(1, elseNode.getPredecessors().size());
        Assert.assertEquals(2, loopNode.getPredecessors().size());
        Assert.assertEquals(1, afterLoopNode.getPredecessors().size());
        Assert.assertSame(loopNode, ifNode.getPredecessors().get(0));
        Assert.assertSame(loopNode, elseNode.getPredecessors().get(0));
        Assert.assertSame(loopNode, afterLoopNode.getPredecessors().get(0));
        Assert.assertSame(ifNode, loopNode.getPredecessors().get(0));
        Assert.assertSame(elseNode, loopNode.getPredecessors().get(1));
    }

    @Test
    public void testBlockWithRecursion() {
        ControlFlowGraph cfg = runBlockProcessingOnControlFlowObject("methodWithRecursion");

        Assert.assertEquals(2, cfg.getNodes().size());
        Iterator<BasicNode> iterator = cfg.getNodes().values().iterator();
        BasicNode beforeNode = iterator.next();
        BasicNode afterNode = iterator.next();

        Assert.assertEquals(1, beforeNode.getPredecessors().size());
        Assert.assertSame(beforeNode, beforeNode.getPredecessors().iterator().next());
        Assert.assertEquals(1, afterNode.getPredecessors().size());
        Assert.assertSame(beforeNode, afterNode.getPredecessors().iterator().next());
    }

    @Test
    public void testAddNodeIfNotExisting() {
        ControlFlowGraph cfg = new ControlFlowGraph();
        BaseElement baseElement = mock(BaseElement.class);
        when(baseElement.getId()).thenReturn("ServiceTask");
        ProcessVariablesCreator vr = new ProcessVariablesCreator(
                new BpmnElement("", baseElement, cfg, new FlowAnalysis()),
                null, null, "Process_1");
        Block block = mock(Block.class);
        Block anotherBlock = mock(Block.class);

        // Add first block
        vr.addNodeIfNotExisting(block);
        Assert.assertEquals(1, vr.getNodes().size());

        // Adding block again shouldn´t create new node
        vr.addNodeIfNotExisting(block);
        Assert.assertEquals(1, vr.getNodes().size());

        // Add another block
        vr.addNodeIfNotExisting(anotherBlock);
        Assert.assertEquals(2, vr.getNodes().size());

        // Adding first block again should now create new node
        vr.addNodeIfNotExisting(block);
        Assert.assertEquals(3, vr.getNodes().size());
    }

    @Test
    public void testCleanEmptyNodes() {
        ControlFlowGraph cfg = new ControlFlowGraph();
        BaseElement baseElement = mock(BaseElement.class);
        when(baseElement.getId()).thenReturn("ServiceTask");
        BpmnElement element = new BpmnElement("", baseElement, cfg, new FlowAnalysis());
        ProcessVariablesCreator vr = new ProcessVariablesCreator(element,
                null, null, "Process_1");

        // Create block and nodes
        ProcessVariableOperation pvo = mock(ProcessVariableOperation.class);
        when(pvo.getOperation()).thenReturn(VariableOperation.WRITE);
        Block block = mock(Block.class);

        Node firstNode = new Node(element, block, null, null);
        firstNode.addOperation(pvo);
        Node secondNode = new Node(element, block, null, null);
        secondNode.addOperation(pvo);
        Node thirdNode = new Node(element, block, null, null);
        Node fourthNode = new Node(element, block, null, null);
        fourthNode.addOperation(pvo);
        Node fifthNode = new Node(element, block, null, null);
        fifthNode.addOperation(pvo);
        Node sixthNode = new Node(element, block, null, null);
        sixthNode.addOperation(pvo);

        vr.getNodes().add(firstNode);
        cfg.addNode(firstNode);
        vr.getNodes().add(secondNode);
        cfg.addNode(secondNode);
        vr.getNodes().add(thirdNode);
        cfg.addNode(thirdNode);
        vr.getNodes().add(fourthNode);
        cfg.addNode(fourthNode);
        vr.getNodes().add(fifthNode);
        cfg.addNode(fifthNode);
        vr.getNodes().add(sixthNode);
        cfg.addNode(sixthNode);

        // Create relationships between nodes
        thirdNode.addPredecessor(firstNode);
        thirdNode.addPredecessor(secondNode);
        fourthNode.addPredecessor(thirdNode);
        fifthNode.addPredecessor(thirdNode);
        sixthNode.addPredecessor(fourthNode);
        sixthNode.addPredecessor(fifthNode);

        // Clean empty nodes (only node three is empty)
        Assert.assertEquals(6, vr.getNodes().size());
        vr.cleanEmptyNodes();

        Assert.assertEquals(5, cfg.getNodes().size());
        Assert.assertEquals(2, firstNode.getSuccessors().size());
        Assert.assertEquals(2, secondNode.getSuccessors().size());
        Assert.assertEquals(2, sixthNode.getPredecessors().size());

        Assert.assertSame(fourthNode, firstNode.getSuccessors().get(0));
        Assert.assertSame(fifthNode, firstNode.getSuccessors().get(1));
        Assert.assertSame(fourthNode, secondNode.getSuccessors().get(0));
        Assert.assertSame(fifthNode, secondNode.getSuccessors().get(1));

        Assert.assertSame(firstNode, fourthNode.getPredecessors().get(0));
        Assert.assertSame(secondNode, fourthNode.getPredecessors().get(1));
        Assert.assertSame(firstNode, fifthNode.getPredecessors().get(0));
        Assert.assertSame(secondNode, fifthNode.getPredecessors().get(1));
    }

    private ControlFlowGraph runBlockProcessingOnControlFlowObject(String methodName) {
        SootClass sc = Scene.v().forceResolve("de.viadee.bpm.vPAV.processing.ControlFlowObject", SootClass.SIGNATURES);
        SootMethod method = sc.getMethodByName(methodName);
        ControlFlowGraph cfg = new ControlFlowGraph();
        BaseElement baseElement = mock(BaseElement.class);
        when(baseElement.getId()).thenReturn("ServiceTask");
        ProcessVariablesCreator vr = new ProcessVariablesCreator(
                new BpmnElement("", baseElement, cfg, new FlowAnalysis()),
                null, null, "Process_1");
        ArrayList<Value> args = new ArrayList<>();
        args.add(new JimpleLocal("r1", RefType.v(CamundaMethodServices.DELEGATE)));
        vr.startBlockProcessing(SootResolverSimplified.getBlockFromMethod(method), args);
        return cfg;
    }

    @Before
    public void clear() {
        IssueService.getInstance().clear();
        ProcessVariableOperation.resetIdCounter();
    }
}
