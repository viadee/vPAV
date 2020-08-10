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
import de.viadee.bpm.vPAV.SootResolverSimplified;
import de.viadee.bpm.vPAV.constants.CamundaMethodServices;
import de.viadee.bpm.vPAV.processing.code.flow.MapVariable;
import de.viadee.bpm.vPAV.processing.model.data.CamundaEntryPointFunctions;
import soot.*;
import soot.jimple.AssignStmt;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.internal.JInterfaceInvokeExpr;
import soot.options.Options;
import soot.toolkits.graph.Block;

import java.util.*;

public class EntryPointScanner extends ObjectReaderReceiver {

    private Set<String> javaResources;

    private Map<String, Collection<String>> messageIdToVariableMap = new HashMap<>();

    private Map<String, Collection<String>> processIdToVariableMap = new HashMap<>();

    private Set<String> camundaProcessEntryPoints = new HashSet<>();

    private List<EntryPoint> entryPoints = new ArrayList<>();

    private List<EntryPoint> intermediateEntryPoints = new ArrayList<>();

    private Set<String> processIds = new HashSet<>();

    public EntryPointScanner(final Set<String> javaResources) {
        this.javaResources = javaResources;
        camundaProcessEntryPoints.add(CamundaMethodServices.START_PROCESS_INSTANCE_BY_ID);
        camundaProcessEntryPoints.add(CamundaMethodServices.START_PROCESS_INSTANCE_BY_KEY);
        camundaProcessEntryPoints.add(CamundaMethodServices.START_PROCESS_INSTANCE_BY_MESSAGE);
        camundaProcessEntryPoints.add(CamundaMethodServices.START_PROCESS_INSTANCE_BY_MESSAGE_AND_PROCESS_DEF);
        camundaProcessEntryPoints.add(CamundaMethodServices.CORRELATE_MESSAGE);
    }

    /**
     * scan java resources for variables and retrieve important information such as message ids and entrypoints
     */
    public void scanProcessVariables() {
        for (final String filePath : javaResources) {
            if (!filePath.startsWith("javax")) {
                // TODO why do we create these sets and never use them?
                final Set<String> messageIds = new HashSet<>();
                final Set<String> processIds = new HashSet<>();
                retrieveMethod(filePath, messageIds, processIds);
            }
        }
    }

    /**
     * Retrieve the method name which contains the entrypoint (e.g. "startProcessByXYZ")
     *
     * @param filePath   fully qualified path to the java class
     * @param messageIds Set of messageIds (used to retrieve variable manipulation later on)
     * @param processIds Set of processIds (used to retrieve variable manipulation later on)
     */
    private void retrieveMethod(final String filePath, final Set<String> messageIds, final Set<String> processIds) {
        SootClass sootClass = Scene.v().forceResolve(cleanString(filePath), SootClass.SIGNATURES);

        if (sootClass != null && !sootClass.isInterface()) {
            sootClass.setApplicationClass();
            Scene.v().loadNecessaryClasses();
            for (SootMethod method : sootClass.getMethods()) {
                if (!method.isPhantom() && !method.isAbstract()) {
                    final Body body = method.retrieveActiveBody();
                    ObjectReader objectReader = new ObjectReader(this, sootClass);
                    Block block = SootResolverSimplified.getBlockFromMethod(method);
                    objectReader.processBlock(block, new ArrayList<>(), new ArrayList<>(), null);

     /*               for (String entryPoint : camundaProcessEntryPoints) {
                        if (body.toString().contains(entryPoint)) {
                            final PatchingChain<Unit> pc = body.getUnits();
                            for (Unit unit : pc) {
                                if (unit instanceof AssignStmt) {
                                    final String rightBox = ((AssignStmt) unit).getRightOpBox().getValue().toString();
                                    if (rightBox.contains(entryPoint) && ((AssignStmt) unit).getRightOpBox()
                                            .getValue() instanceof JInterfaceInvokeExpr) {
                                        final JInterfaceInvokeExpr expr = (JInterfaceInvokeExpr) ((AssignStmt) unit)
                                                .getRightOpBox().getValue();
                                        checkExpression(filePath, messageIds, method, entryPoint, expr);
                                    }
                                }
                                if (unit instanceof InvokeStmt) {
                                    final String rightBox = ((InvokeStmt) unit).getInvokeExprBox().getValue()
                                            .toString();
                                    if (rightBox.contains(entryPoint) && ((InvokeStmt) unit).getInvokeExprBox()
                                            .getValue() instanceof JInterfaceInvokeExpr) {
                                        final JInterfaceInvokeExpr expr = (JInterfaceInvokeExpr) ((InvokeStmt) unit)
                                                .getInvokeExprBox().getValue();
                                        checkExpression(filePath, messageIds, method, entryPoint, expr);
                                    }
                                }
                            }
                        }
                        if (body.toString().contains(CamundaMethodServices.CORRELATE_MESSAGE)) {
                            processIds.add(entryPoint);
                        }
                    } */
                }
            }
        }
    }

    /**
     * Checks the current expression and creates a new entrypoint
     *
     * @param filePath   Current filePath of the model
     * @param messageIds List of message ids
     * @param method     Current method
     * @param entryPoint Current entryPoint
     * @param expr       Current expression
     */
    private void checkExpression(final String filePath, final Set<String> messageIds, final SootMethod method,
            final String entryPoint, final JInterfaceInvokeExpr expr) {
        if (expr != null) {
            final String ex = expr.getArgBox(0).getValue().toString();
            if (entryPoint.equals(CamundaMethodServices.CORRELATE_MESSAGE)) {
                intermediateEntryPoints
                        .add(new EntryPoint(filePath, method.getName(), ex.replaceAll("\"", ""), entryPoint));
            } else {
                messageIds.add(entryPoint);
                entryPoints.add(new EntryPoint(filePath, method.getName(), ex.replaceAll("\"", ""), entryPoint));
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
     * get list of intermediate entrypoints (process message, method) where process variables have been found
     *
     * @return returns list of locations
     */
    public List<EntryPoint> getIntermediateEntryPoints() {
        return intermediateEntryPoints;
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

    public void addEntryPoint(CamundaEntryPointFunctions function, String className, InvokeExpr expr, List<Object> args) {
        String methodName = expr.getMethod().getName();
        String messageName = "";

        if(function.isWithMessage()) {
            messageName = (String) args.get(0);
        }

        if (expr.getArgCount() > 1) {
            // Variables might be passed
            for(Object o: args) {
                if(o instanceof MapVariable) {
                    Set<String> variables = ((MapVariable) o).getValues().keySet();
                    EntryPoint ep = new EntryPoint(className, methodName, messageName, methodName, variables);
                    this.entryPoints.add(ep);
                    return;
                }
            }
        }

        EntryPoint ep = new EntryPoint(className, methodName, messageName, methodName);

        if(function.equals(CamundaEntryPointFunctions.CreateMessageCorrelation)) {
            processIds.add(function.getName());
        }
        else {
            this.entryPoints.add(ep);
        }
    }
}
