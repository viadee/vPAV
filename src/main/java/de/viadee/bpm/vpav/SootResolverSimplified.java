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
package de.viadee.bpm.vpav;

import de.viadee.bpm.vpav.constants.CamundaMethodServices;
import de.viadee.bpm.vpav.constants.ConfigConstants;
import de.viadee.bpm.vpav.processing.EntryPointScanner;
import soot.*;
import soot.toolkits.graph.Block;
import soot.toolkits.graph.BlockGraph;
import soot.toolkits.graph.ClassicCompleteBlockGraph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import static de.viadee.bpm.vpav.constants.CamundaMethodServices.NOTIFY;

public class SootResolverSimplified {

    private SootResolverSimplified() {

    }

    private static final List<String> defaultMethods = Arrays
            .asList(CamundaMethodServices.EXECUTE, NOTIFY, "mapInputVariables", "mapOutputVariables");

    private static final Logger LOGGER = Logger.getLogger(SootResolverSimplified.class.getName());

    public static Block getBlockFromClass(SootClass sootClass, String methodName, List<Type> parameterTypes,
            Type returnType) {
        if (sootClass != null) {
            if (defaultMethods.contains(methodName)) {
                // Create parameter types for entry point methods
                parameterTypes = getParametersForDefaultMethods(methodName);
                returnType = VoidType.v();
            }

            SootMethod sootMethod = getSootMethod(sootClass, methodName, parameterTypes, returnType);
            return getBlockFromMethod(sootMethod);
        }

        return null;
    }

    public static SootMethod getSootMethod(final SootClass sootClass, final String methodName,
            List<Type> parameterTypes, final Type returnType) {
        SootMethod method = sootClass.getMethodUnsafe(methodName, parameterTypes, returnType);

        if (methodName.equals(CamundaMethodServices.EXECUTE) && method == null) {
            parameterTypes.remove(0);
            parameterTypes.add(CamundaMethodServices.ACTIVITY_EXECUTION_TYPE);
            method = sootClass.getMethodUnsafe(methodName, parameterTypes, returnType);
        }
        if (methodName.equals(NOTIFY) && method == null) {
            parameterTypes.remove(0);
            parameterTypes.add(CamundaMethodServices.DELEGATE_TASK_TYPE);
            method = sootClass.getMethodUnsafe(methodName, parameterTypes, returnType);
        }

        if (method == null) {
            LOGGER.warning(
                    String.format("In class %s - %s method was not found by Soot with parameters.", sootClass.getName(),
                            methodName));

            method = sootClass.getMethodByNameUnsafe(methodName);
            if (method == null) {
                LOGGER.warning(
                        String.format("In class %s - %s method was not found by Soot", sootClass.getName(),
                                methodName));
            }
        }
        return method;
    }

    public static Block getBlockFromMethod(SootMethod method) {
        try {
            if (!method.isPhantom()) {
                Body body = method.retrieveActiveBody();
                BlockGraph graph = new ClassicCompleteBlockGraph(body);
                List<Block> graphHeads = graph.getHeads();
                assert (graphHeads.size() == 1);

                return graphHeads.get(0);
            }
        } catch (Exception e) {
            LOGGER.warning(method.getName() + " could not be resolved and was skipped.");
            return null;
        }
        return null;
    }

    public static SootClass setupSootClass(String className) {
        className = EntryPointScanner.cleanString(className);
        SootClass sootClass = Scene.v().forceResolve(fixClassPathForSoot(className), SootClass.SIGNATURES);
        if (sootClass != null) {
            sootClass.setApplicationClass();
            Scene.v().loadNecessaryClasses();
            return sootClass;
        } else {
            LOGGER.warning(String.format("Class %s was not found by Soot", className));
            return null;
        }
    }

    public static List<Type> getParametersForDefaultMethods(final String methodName) {
        List<Type> parameterTypes = new ArrayList<>();

        // Retrieve the method and its body based on the used interface
        RefType delegateExecutionType = CamundaMethodServices.DELEGATE_EXECUTION_TYPE;
        RefType mapVariablesType = CamundaMethodServices.MAP_VARIABLES_TYPE;
        RefType variableScopeType = CamundaMethodServices.VARIABLE_SCOPE_TYPE;

        switch (methodName) {
            case "execute":
            case NOTIFY:
                parameterTypes.add(delegateExecutionType);
                break;
            case "mapInputVariables":
                parameterTypes.add(delegateExecutionType);
                parameterTypes.add(mapVariablesType);
                break;
            case "mapOutputVariables":
                parameterTypes.add(delegateExecutionType);
                parameterTypes.add(variableScopeType);
                break;
        }

        return parameterTypes;
    }

    public static String fixClassPathForSoot(String classFile) {
        // Trim class path because soot depends on the scan path
        int scanpathLength = RuntimeConfig.getInstance().getScanPath().length();
        String className = classFile.replaceAll("\\.", "/");
        if ((ConfigConstants.TARGET_TEST_PATH + className)
                .startsWith(RuntimeConfig.getInstance().getScanPath())) {
            classFile = classFile
                    .substring(scanpathLength - ConfigConstants.TARGET_TEST_PATH.length());
        } else if ((ConfigConstants.TARGET_CLASS_FOLDER + className)
                .startsWith(RuntimeConfig.getInstance().getScanPath())) {
            classFile = classFile
                    .substring(
                            scanpathLength - ConfigConstants.TARGET_CLASS_FOLDER.length());
        } else if (className.startsWith(RuntimeConfig.getInstance().getScanPath())) {
            classFile = classFile
                    .substring(scanpathLength);
        }

        return classFile;
    }

}
