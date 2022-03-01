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
package de.viadee.bpm.vpav.processing;

import de.viadee.bpm.vpav.FileScanner;
import de.viadee.bpm.vpav.RuntimeConfig;
import de.viadee.bpm.vpav.constants.CamundaMethodServices;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import soot.Scene;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class EntryPointScannerTest {

    private static EntryPointScanner scanner;

    @BeforeClass
    public static void setup() {
        RuntimeConfig.getInstance().setTest(true);
        FileScanner.setupSootClassPaths(new LinkedList<>());
        JavaReaderStatic.setupSoot();
        Scene.v().loadNecessaryClasses();
        String currentPath = (new File(".")).toURI().getPath();
        Scene.v().extendSootClassPath(currentPath + "src/test/java");
        Scene.v().defaultClassPath();

        final Set<String> javaResources = new HashSet<>();
        javaResources.add("de/viadee/bpm/vpav/processing/EntryPointRuntimeService");
        scanner = new EntryPointScanner(javaResources);
        scanner.scanProcessVariables();
    }

    @Test
    public void testFindVariablesMap() {
        List<EntryPoint> entryPoints = scanner.getEntryPoints().stream()
                .filter(ep -> ep.getMethodName().equals("startProcessWithVariables")).collect(
                        Collectors.toList());
        Assert.assertEquals("One entry point should be found.", 1, entryPoints.size());
        Assert.assertEquals("One variable should be passed on start.", 1,
                entryPoints.get(0).getProcessVariables().size());
        Assert.assertEquals("The variable 'anotherVariable' should have been found.", "anotherVariable",
                entryPoints.get(0).getProcessVariables().iterator().next());
    }

    @Test
    public void testEntryPointMethod() {
        List<EntryPoint> entryPoints = scanner.getEntryPoints().stream()
                .filter(ep -> ep.getMethodName().equals("startProcess")).collect(
                        Collectors.toList());
        Assert.assertEquals(4, entryPoints.size());
        for (EntryPoint ep : entryPoints) {
            Assert.assertEquals("de.viadee.bpm.vpav.processing.EntryPointRuntimeService", ep.getClassName());
            Assert.assertEquals("startProcess", ep.getMethodName());
        }

        Assert.assertEquals(CamundaMethodServices.START_PROCESS_INSTANCE_BY_ID, entryPoints.get(0).getEntryPointName());

        Assert.assertEquals(CamundaMethodServices.START_PROCESS_INSTANCE_BY_KEY,
                entryPoints.get(1).getEntryPointName());
        Assert.assertEquals("myKey", entryPoints.get(1).getProcessDefinitionKey());

        Assert.assertEquals(CamundaMethodServices.START_PROCESS_INSTANCE_BY_MESSAGE,
                entryPoints.get(2).getEntryPointName());
        Assert.assertEquals("myMessage", entryPoints.get(2).getMessageName());

        Assert.assertEquals(CamundaMethodServices.START_PROCESS_INSTANCE_BY_MESSAGE_AND_PROCESS_DEF,
                entryPoints.get(3).getEntryPointName());
        Assert.assertEquals("myMessage2", entryPoints.get(3).getMessageName());
    }

    @Test
    public void startWithProcessInstantiationBuilder() {
        List<EntryPoint> entryPoints = scanner.getEntryPoints().stream()
                .filter(ep -> ep.getMethodName().equals("startWithProcessInstantiationBuilder")).collect(
                        Collectors.toList());
        Assert.assertEquals("One entry point should be found.", 1, entryPoints.size());
        Assert.assertEquals("processKey", entryPoints.get(0).getProcessDefinitionKey());
        Assert.assertEquals("Three variable should be passed on start.", 3,
                entryPoints.get(0).getProcessVariables().size());
        Iterator<String> iter = entryPoints.get(0).getProcessVariables().iterator();
        Assert.assertEquals("The variable 'var' should have been found.", "var",
                iter.next());
        Assert.assertEquals("The variable 'mapVariable' should have been found.", "mapVariable",
                iter.next());
        Assert.assertEquals("The variable 'mapVariable2' should have been found.", "mapVariable2",
                iter.next());
    }

    @Test
    public void testCamundaVariableMap() {
        List<EntryPoint> entryPoints = scanner.getEntryPoints().stream()
                .filter(ep -> ep.getMethodName().equals("withVariableMap")).collect(
                        Collectors.toList());
        Assert.assertEquals(1, entryPoints.size());

        Assert.assertEquals(CamundaMethodServices.START_PROCESS_INSTANCE_BY_KEY,
                entryPoints.get(0).getEntryPointName());
        Assert.assertEquals(1, entryPoints.get(0).getProcessVariables().size());
        Assert.assertEquals("variable_camunda", entryPoints.get(0).getProcessVariables().iterator().next());
    }
}
