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

import de.viadee.bpm.vPAV.SootResolverSimplified;
import de.viadee.bpm.vPAV.constants.CamundaMethodServices;
import de.viadee.bpm.vPAV.processing.code.flow.MapVariable;
import de.viadee.bpm.vPAV.processing.model.data.CamundaEntryPointFunctions;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.jimple.InvokeExpr;
import soot.jimple.internal.JInterfaceInvokeExpr;
import soot.toolkits.graph.Block;

import java.util.*;

import static de.viadee.bpm.vPAV.SootResolverSimplified.fixClassPathForSoot;

public class EntryPointScanner extends ObjectReaderReceiver {

    private Set<String> javaResources;

    private Map<String, Collection<String>> messageIdToVariableMap = new HashMap<>();

    private Map<String, Collection<String>> processIdToVariableMap = new HashMap<>();

    private List<EntryPoint> entryPoints = new ArrayList<>();

    public EntryPointScanner(final Set<String> javaResources) {
        this.javaResources = javaResources;
     }

    /**
     * scan java resources for variables and retrieve important information such as message ids and entrypoints
     */
    public void scanProcessVariables() {
        for (final String filePath : javaResources) {
            if (!filePath.startsWith("javax")) {
                retrieveMethod(filePath);
            }
        }
    }

    /**
     * Retrieve the method name which contains the entrypoint (e.g. "startProcessByXYZ")
     *
     * @param filePath fully qualified path to the java class
     */
    private void retrieveMethod(final String filePath) {
        SootClass sootClass = Scene.v().forceResolve(fixClassPathForSoot(cleanString(filePath)), SootClass.SIGNATURES);

        if (sootClass != null && !sootClass.isInterface()) {
            sootClass.setApplicationClass();
            Scene.v().loadNecessaryClasses();
            for (SootMethod method : sootClass.getMethods()) {
                if (!method.isPhantom() && !method.isAbstract()) {
                    ObjectReader objectReader = new ObjectReader(this, sootClass, method.getName());
                    Block block = SootResolverSimplified.getBlockFromMethod(method);
                    objectReader.processBlock(block, new ArrayList<>(), new ArrayList<>(), null);
                }
            }
        }
    }

    /**
     * Strips unnecessary characters and returns cleaned name
     *
     * @param className Classname to be stripped of unused chars
     * @return cleaned String
     */
    public static String cleanString(String className) {
        if (System.getProperty("os.name").startsWith("Windows")) {
            return className.replace("\\", ".")
                    .replace("/", ".").replace(".class", "");
        } else {
            return className.replace("/", ".").replace(".class",
                    "").replace(".java", "");
        }
    }

    /**
     * get list of entrypoints (process message, method) where process variables have been found
     *
     * @return returns list of locations
     */
    public List<EntryPoint> getEntryPoints() {
        return entryPoints;
    }

    /**
     * get mapping for message id
     *
     * @return messageIdToVariableMap returns messageIdToVariableMap
     */
    public Map<String, Collection<String>> getMessageIdToVariableMap() {
        return messageIdToVariableMap;
    }

    /**
     * get mapping for process id
     *
     * @return processIdToVariableMap returns processIdToVariableMap
     */
    public Map<String, Collection<String>> getProcessIdToVariableMap() {
        return processIdToVariableMap;
    }

    public void addEntryPoint(CamundaEntryPointFunctions function, String className, String methodName, InvokeExpr expr,
            List<Object> args) {
        String entryPointName = expr.getMethod().getName();
        String messageName = "";
        String processDefinitionKey = null;

        if (function.isWithMessage()) {
            messageName = (String) args.get(0);
            // TODO Check expresssion

        }

        if (function.equals(CamundaEntryPointFunctions.StartProcessInstanceByKey)) {
            processDefinitionKey = (String) args.get(0);
        }

        if (expr.getArgCount() > 1) {
            // Variables might be passed
            for (Object o : args) {
                if (o instanceof MapVariable) {
                    Set<String> variables = ((MapVariable) o).getValues().keySet();
                    EntryPoint ep = new EntryPoint(className, methodName, messageName, entryPointName, processDefinitionKey,
                            variables);
                    this.entryPoints.add(ep);
                    return;
                }
            }
        }

        EntryPoint ep = new EntryPoint(className, methodName, messageName, entryPointName, processDefinitionKey);

        this.entryPoints.add(ep);
    }
}
