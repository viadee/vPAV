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

import de.viadee.bpm.vPAV.processing.JavaReaderStatic;
import de.viadee.bpm.vPAV.processing.code.flow.Node;
import de.viadee.bpm.vPAV.processing.model.data.ProcessVariableOperation;
import de.viadee.bpm.vPAV.processing.model.data.VariableOperation;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import soot.*;
import soot.jimple.internal.JimpleLocal;
import soot.toolkits.graph.Block;

import java.util.ArrayList;
import java.util.LinkedList;

import static org.mockito.Mockito.mock;

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
        ProcessVariablesCreator vr = new ProcessVariablesCreator(null, null, null);
        ArrayList<Value> args = new ArrayList<>();
        // TODO check if jimple local is really the correct parameter type
        args.add(new JimpleLocal("r1", RefType.v("org.camunda.bpm.engine.delegate.DelegateExecution")));
        vr.blockIterator(SootResolverSimplified.getBlockFromMethod(method), args);

        ArrayList<Node> nodes = vr.getNodes();
        // Three methods, method 1 interrupted by method calls = four nodes
        Assert.assertEquals(4, nodes.size());
        Assert.assertEquals(2, nodes.get(0).getOperations().size());
        Assert.assertEquals(1, nodes.get(1).getOperations().size());
        Assert.assertEquals(1, nodes.get(2).getOperations().size());
        Assert.assertEquals(1, nodes.get(3).getOperations().size());
        Assert.assertSame(nodes.get(0).getBlock(), nodes.get(3).getBlock());
        Assert.assertEquals("variableOneChanged", nodes.get(3).getKilled().get("0").getName());
    }

    @Test
    public void testHandleProcessVariableManipulation() {
        // Test that new nodes are created when the block changes
        Block blockOne = mock(Block.class);
        Block blockTwo = mock(Block.class);
        ProcessVariableOperation blockOneOpOne = new ProcessVariableOperation("var1",
                VariableOperation.WRITE, null);
        ProcessVariableOperation blockOneOpTwo = new ProcessVariableOperation("var2",
                VariableOperation.WRITE, null);
        ProcessVariableOperation blockTwoOpOne = new ProcessVariableOperation("var3",
                VariableOperation.WRITE, null);

        ProcessVariablesCreator vr = new ProcessVariablesCreator(null, null, null);
        vr.handleProcessVariableManipulation(blockOne, blockOneOpOne);
        vr.handleProcessVariableManipulation(blockOne, blockOneOpTwo);
        vr.handleProcessVariableManipulation(blockTwo, blockTwoOpOne);

        Assert.assertEquals(2, vr.getNodes().size());
        Assert.assertEquals(2, vr.getNodes().get(0).getOperations().size());
        Assert.assertEquals(1, vr.getNodes().get(1).getOperations().size());
    }
}
