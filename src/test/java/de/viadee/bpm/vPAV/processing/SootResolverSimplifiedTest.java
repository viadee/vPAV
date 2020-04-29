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
package de.viadee.bpm.vPAV.processing;

import de.viadee.bpm.vPAV.FileScanner;
import de.viadee.bpm.vPAV.RuntimeConfig;
import de.viadee.bpm.vPAV.SootResolverSimplified;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.toolkits.graph.Block;

import java.io.File;
import java.util.LinkedList;

public class SootResolverSimplifiedTest {

    @BeforeClass
    public static void setupSoot() {
        RuntimeConfig.getInstance().setTest(true);
        FileScanner.setupSootClassPaths(new LinkedList<>());
        new JavaReaderStatic().setupSoot();
        Scene.v().loadNecessaryClasses();
        String currentPath = (new File(".")).toURI().getPath();
        Scene.v().extendSootClassPath(currentPath + "src/test/java");
        Scene.v().defaultClassPath();
    }

    @Test
    public void testGetBlockFromMethod() {
        SootClass sc = Scene.v().forceResolve("de.viadee.bpm.vPAV.processing.SimpleObject", SootClass.SIGNATURES);
        SootMethod method = sc.getMethodByName("method");
        Block block = SootResolverSimplified.getBlockFromMethod(method);
        Assert.assertEquals(3, block.getBody().getUnits().size());
    }

    @Test
    public void testGetParametersForDefaultMethods() {
        // Test execute
        Assert.assertEquals(RefType.v("org.camunda.bpm.engine.delegate.DelegateExecution"),
                SootResolverSimplified.getParametersForDefaultMethods("execute").get(0));
        Assert.assertEquals(1, SootResolverSimplified.getParametersForDefaultMethods("execute").size());

        // Test notify
        Assert.assertEquals(RefType.v("org.camunda.bpm.engine.delegate.DelegateExecution"),
                SootResolverSimplified.getParametersForDefaultMethods("notify").get(0));
        Assert.assertEquals(1, SootResolverSimplified.getParametersForDefaultMethods("execute").size());

        // Test mapInputVariables
        Assert.assertEquals(RefType.v("org.camunda.bpm.engine.delegate.DelegateExecution"),
                SootResolverSimplified.getParametersForDefaultMethods("mapInputVariables").get(0));
        Assert.assertEquals(RefType.v("org.camunda.bpm.engine.variable.VariableMap"),
                SootResolverSimplified.getParametersForDefaultMethods("mapInputVariables").get(1));
        Assert.assertEquals(2, SootResolverSimplified.getParametersForDefaultMethods("mapInputVariables").size());

        // Test mapOutputVariables
        Assert.assertEquals(RefType.v("org.camunda.bpm.engine.delegate.DelegateExecution"),
                SootResolverSimplified.getParametersForDefaultMethods("mapOutputVariables").get(0));
        Assert.assertEquals(RefType.v("org.camunda.bpm.engine.delegate.VariableScope"),
                SootResolverSimplified.getParametersForDefaultMethods("mapOutputVariables").get(1));
        Assert.assertEquals(2, SootResolverSimplified.getParametersForDefaultMethods("mapOutputVariables").size());
    }
}
