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
import de.viadee.bpm.vPAV.constants.ConfigConstants;
import de.viadee.bpm.vPAV.processing.JavaReaderStatic;
import de.viadee.bpm.vPAV.processing.ProcessVariablesScanner;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import soot.*;
import soot.jimple.internal.JimpleLocal;
import soot.toolkits.graph.Block;
import soot.toolkits.graph.BlockGraph;
import soot.toolkits.graph.ClassicCompleteBlockGraph;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class SootResolverSimplified {

    private static List<String> defaultMethods = Arrays
            .asList("execute", "notify", "mapInputVariables", "mapOutputVariables");

    private static final Logger LOGGER = Logger.getLogger(SootResolverSimplified.class.getName());

    public static Block getBlockFromClass(String className, String methodName, List<Type> parameterTypes,
            Type returnType) {
        SootClass sootClass = setupSootClass(className);
        if (sootClass != null) {
            if (defaultMethods.contains(methodName)) {
                // Create parameter types for entry point methods
                parameterTypes = getParametersForDefaultMethods(methodName);
                returnType = VoidType.v();
            }

            SootMethod sootMethod = getSootMethod(sootClass, methodName, parameterTypes, returnType);
            return getBlockFromMethod(sootMethod);
        }
        // TODO handle this case;
        assert false;
        return null;
    }

    private static SootMethod getSootMethod(final SootClass sootClass, final String methodName,
            List<Type> parameterTypes, final Type returnType) {
        SootMethod method = sootClass.getMethodUnsafe(methodName, parameterTypes, returnType);

        if (methodName.equals("execute") && method == null) {
            parameterTypes.remove(0);
            parameterTypes.add(CamundaMethodServices.ACTIVITY_EXECUTION_TYPE);
            method = sootClass.getMethodUnsafe(methodName, parameterTypes, returnType);
        }
        if (methodName.equals("notify") && method == null) {
            parameterTypes.remove(0);
            parameterTypes.add(CamundaMethodServices.DELEGATE_TASK_TYPE);
            method = sootClass.getMethodUnsafe(methodName, parameterTypes, returnType);
        }

        if (method == null) {
            LOGGER.warning(
                    "In class " + sootClass.getName() + " - " + methodName
                            + " method was not found by Soot with parameters.");

            method = sootClass.getMethodByNameUnsafe(methodName);
            if (method == null) {
                LOGGER.warning(
                        "In class " + sootClass.getName() + " - " + methodName + " method was not found by Soot");
            }
        }
        return method;
    }

    public static Block getBlockFromMethod(SootMethod method) {
        if (!method.isPhantom()) {
            Body body = method.retrieveActiveBody();
            BlockGraph graph = new ClassicCompleteBlockGraph(body);
            List<Block> graphHeads = graph.getHeads();
            assert (graphHeads.size() == 1);

            return graphHeads.get(0);
        }
        return null;
    }

    private static SootClass setupSootClass(String className) {
        className = ProcessVariablesScanner.cleanString(className, true);
        SootClass sootClass = Scene.v().forceResolve(className, SootClass.SIGNATURES);
        if (sootClass != null) {
            sootClass.setApplicationClass();
            Scene.v().loadNecessaryClasses();
            return sootClass;
        } else {
            LOGGER.warning("Class " + className + " was not found by Soot");
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
            case "notify":
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

    // Add test
    public static List<Value> getParameterValuesForDefaultMethods(final String methodName) {
        List<Value> parameters = new ArrayList<>();

        // Retrieve the method and its body based on the used interface
        RefType delegateExecutionType = CamundaMethodServices.DELEGATE_EXECUTION_TYPE;
        RefType mapVariablesType = CamundaMethodServices.MAP_VARIABLES_TYPE;
        RefType variableScopeType = CamundaMethodServices.VARIABLE_SCOPE_TYPE;

        switch (methodName) {
            case "execute":
            case "notify":
                parameters.add(new JimpleLocal("del_ex", delegateExecutionType));
                break;
            case "mapInputVariables":
                parameters.add(new JimpleLocal("del_ex", delegateExecutionType));
                parameters.add(new JimpleLocal("var_map", mapVariablesType));
                break;
            case "mapOutputVariables":
                parameters.add(new JimpleLocal("del_ex", delegateExecutionType));
                parameters.add(new JimpleLocal("var_scope", variableScopeType));
                break;
        }

        return parameters;
    }

}
