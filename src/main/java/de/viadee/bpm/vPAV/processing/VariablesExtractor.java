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
package de.viadee.bpm.vPAV.processing;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import de.viadee.bpm.vPAV.Messages;
import de.viadee.bpm.vPAV.config.model.Rule;
import de.viadee.bpm.vPAV.constants.BpmnConstants;
import de.viadee.bpm.vPAV.constants.CamundaMethodServices;
import de.viadee.bpm.vPAV.output.IssueWriter;
import de.viadee.bpm.vPAV.processing.code.flow.BpmnElement;
import de.viadee.bpm.vPAV.processing.code.flow.ControlFlowGraph;
import de.viadee.bpm.vPAV.processing.code.flow.Node;
import de.viadee.bpm.vPAV.processing.model.data.*;
import org.camunda.bpm.engine.variable.VariableMap;
import org.camunda.bpm.model.bpmn.instance.CallActivity;
import soot.*;
import soot.jimple.*;
import soot.jimple.internal.*;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.toolkits.graph.Block;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class VariablesExtractor {
    private List<Value> constructorArgs;
    private JavaReaderStatic javaReaderStatic;
    private String returnStmt;

    VariablesExtractor(JavaReaderStatic reader) {
        this.javaReaderStatic = reader;
    }

    /**
     * Check whether or not the second or third argument contain a reference to the
     * variable map
     *
     * @param entry      Current entry
     * @param assignment Current assigned variable
     * @param invoke     Current invocation
     * @param expr       Current expression
     * @return True/False based on whether the second or third argument refers to
     * the variable map
     */
    private boolean checkArgBoxes(final EntryPoint entry, final String assignment, final String invoke,
                                  final JInterfaceInvokeExpr expr, final BpmnElement element) {
        if (expr.getMethodRef().getName().equals(entry.getEntryPoint())) {
            if (!assignment.isEmpty()) {
                if (element.getBaseElement().getElementType().getTypeName().equals(BpmnConstants.RECEIVE_TASK)) {
                    String message = expr.getArgBox(0).getValue().toString().replaceAll("\"", "");
                    return message.equals(entry.getMessageName());
                } else {
                    for (Value value : expr.getArgs()) {
                        if (value.toString().equals(invoke)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Checks for WRITE operations on process variables
     *
     * @param body             Soot representation of a method's body
     * @param element          BpmnElement
     * @param resourceFilePath Path of the BPMN model
     * @return Map of process variable operations
     */
    ListMultimap<String, ProcessVariableOperation> checkWriteAccess(final Body body, final BpmnElement element,
                                                                    final String resourceFilePath, final EntryPoint entryPoint) {

        final ListMultimap<String, ProcessVariableOperation> initialOperations = ArrayListMultimap.create();

        if (body.getMethod().getName().equals(entryPoint.getMethodName())) {
            final PatchingChain<Unit> pc = body.getUnits();
            String assignment = "";
            String invoke = "";

            for (Unit unit : pc) {
                if (unit instanceof AssignStmt) {
                    final String rightBox = ((AssignStmt) unit).getRightOpBox().getValue().toString();
                    final String leftBox = ((AssignStmt) unit).getLeftOpBox().getValue().toString();

                    if (rightBox.contains(CamundaMethodServices.VARIABLE_MAP + " createVariables()")) {
                        assignment = leftBox;
                    }

                    if (rightBox.contains(entryPoint.getEntryPoint()) && rightBox.contains(invoke)) {
                        return initialOperations;
                    }

                    if (((AssignStmt) unit).getRightOpBox().getValue() instanceof JInterfaceInvokeExpr) {
                        final JInterfaceInvokeExpr expr = (JInterfaceInvokeExpr) ((AssignStmt) unit).getRightOpBox()
                                .getValue();
                        if (expr != null) {
                            if (expr.getMethodRef().getDeclaringClass().equals(
                                    Scene.v().forceResolve(VariableMap.class.getName(), SootClass.SIGNATURES))) {
                                initialOperations.putAll(parseInitialExpression(expr, element, resourceFilePath));
                                invoke = leftBox;
                            }
                            if (checkArgBoxes(entryPoint, assignment, invoke, expr, element))
                                return initialOperations;
                        }
                    }
                }
                if (unit instanceof InvokeStmt) {
                    if (((InvokeStmt) unit).getInvokeExprBox().getValue() instanceof JInterfaceInvokeExpr) {
                        final JInterfaceInvokeExpr expr = (JInterfaceInvokeExpr) ((InvokeStmt) unit).getInvokeExprBox()
                                .getValue();
                        if (expr != null) {
                            if (expr.getMethodRef().getDeclaringClass()
                                    .equals(Scene.v().forceResolve(Map.class.getName(), SootClass.SIGNATURES))) {
                                initialOperations.putAll(parseInitialExpression(expr, element, resourceFilePath));
                            }
                            if (checkArgBoxes(entryPoint, assignment, invoke, expr, element))
                                return initialOperations;
                        }
                    }
                }
            }
        }

        return initialOperations;
    }

    /**
     * Iterator through the source code line by line, collecting the
     * ProcessVariables Camunda methods are interface invocations appearing either
     * in Assign statement or Invoke statement Constraint: Only String constants can
     * be precisely recognized.
     *
     * @param classPaths    Set of classes that is included in inter-procedural analysis
     * @param cg            Soot ControlFlowGraph
     * @param block         Block from CFG
     * @param outSet        OUT set of CFG
     * @param element       BpmnElement
     * @param chapter       ElementChapter
     * @param fieldType     KnownElementFieldType
     * @param filePath      ResourceFilePath for ProcessVariableOperation
     * @param scopeId       Scope of BpmnElement
     * @param variableBlock VariableBlock
     * @return VariableBlock
     */
    VariableBlock blockIterator(final Set<String> classPaths, final CallGraph cg, final Block block,
                                final OutSetCFG outSet, final BpmnElement element, final ElementChapter chapter,
                                final KnownElementFieldType fieldType, final String filePath, final String scopeId,
                                VariableBlock variableBlock, String assignmentStmt, final List<Value> args,
                                final ControlFlowGraph controlFlowGraph, Node node) {
        if (variableBlock == null) {
            variableBlock = new VariableBlock(block, new ArrayList<>());
        }

        String paramName = "";
        int argsCounter = 0;
        int instanceFieldRef = Integer.MAX_VALUE;
        boolean nodeSaved = true;

        final Iterator<Unit> unitIt = block.iterator();

        Unit unit;
        while (unitIt.hasNext()) {
            unit = unitIt.next();
            if (unit instanceof IdentityStmt) {
                // IdentityStatement, used to find index of method's arguments and name, if
                // possible (has to be a String)
                if (((IdentityStmt) unit).getRightOp() instanceof ParameterRef) {
                    if (((IdentityStmt) unit).getRightOp().getType().toString().equals("java.lang.String")) {
                        final Pattern pattern = Pattern.compile("(@parameter(.):)(.*)");
                        String argument = ((IdentityStmt) unit).getRightOpBox().getValue().toString();
                        Matcher matcher = pattern.matcher(argument);
                        if (matcher.matches()) {
                            Value val = args.get(Integer.parseInt(matcher.group(2)));
                            paramName = val.toString().replace("\"", "");
                            assignmentStmt = ((JIdentityStmt) unit).getLeftOpBox().getValue().toString();
                        }
                    }
                }
            }
            if (unit instanceof ReturnStmt) {
                // Return statement
                if (((JReturnStmt) unit).getOpBox().getValue().toString().equals(assignmentStmt)) {
                    this.setReturnStmt(paramName);
                }
            }
            if (unit instanceof InvokeStmt) {
                try {
                    boolean passesDelegateExecution = false;
                    // Split node only if DelegateExecution object (which is used for manipulating variables) is passed
                    InvokeExpr calledMethod = (InvokeExpr) ((InvokeStmt) unit).getInvokeExprBox().getValue();
                    for (Type parameter : calledMethod.getMethodRef().getParameterTypes()) {
                        if (((RefType) parameter).getClassName().equals("org.camunda.bpm.engine.delegate.DelegateExecution")) {
                            passesDelegateExecution = true;
                            break;
                        }
                    }
                    if (passesDelegateExecution) {
                        // Node must be splitted
                        if (!nodeSaved) {
                            controlFlowGraph.addNode(node);
                        }
                        // Split node
                        Node newSectionNode = (Node) node.clone();
                        assignmentStmt = processInvokeStmt(classPaths, cg, outSet, element, chapter, fieldType, filePath,
                                scopeId, variableBlock, assignmentStmt, controlFlowGraph, node, paramName, argsCounter, unit);
                        paramName = returnStmt;
                        node = newSectionNode;
                        nodeSaved = false;
                    } else {
                        assignmentStmt = processInvokeStmt(classPaths, cg, outSet, element, chapter, fieldType, filePath,
                                scopeId, variableBlock, assignmentStmt, controlFlowGraph, node, paramName, argsCounter, unit);
                    }
                } catch (CloneNotSupportedException e) {
                    e.printStackTrace();
                }
            }
            if (unit instanceof AssignStmt) {
                // Method call with assignment to a variable
                if (((AssignStmt) unit).getRightOpBox().getValue() instanceof JVirtualInvokeExpr) {
                    assignmentStmt = ((AssignStmt) unit).getLeftOpBox().getValue().toString();
                    JVirtualInvokeExpr expr = (JVirtualInvokeExpr) ((AssignStmt) unit).getRightOpBox().getValue();

                    try {
                        if (!nodeSaved) {
                            controlFlowGraph.addNode(node);
                        }
                        // Split node
                        Node newSectionNode = (Node) node.clone();
                        checkInterProceduralCall(classPaths, cg, outSet, element, chapter, fieldType, scopeId,
                                variableBlock, unit, assignmentStmt, expr.getArgs(), controlFlowGraph, false);
                        paramName = returnStmt;
                        node = newSectionNode;
                        nodeSaved = false;

                    } catch (CloneNotSupportedException e) {
                        e.printStackTrace();
                    }
                }
                // Method call of implemented interface method with assignment to a variable
                if (((AssignStmt) unit).getRightOpBox().getValue() instanceof JInterfaceInvokeExpr) {
                    JInterfaceInvokeExpr expr = (JInterfaceInvokeExpr) ((AssignStmt) unit).getRightOpBox().getValue();
                    if (expr != null) {
                        parseExpression(expr, variableBlock, element, chapter, fieldType, filePath, scopeId, paramName,
                                node);
                    }
                }
                // Instance fields
                if (((AssignStmt) unit).getRightOpBox().getValue() instanceof JInstanceFieldRef) {
                    final Pattern pattern = Pattern.compile("(\\$r(\\d))");
                    String argument = ((AssignStmt) unit).getLeftOpBox().getValue().toString();
                    Matcher matcher = pattern.matcher(argument);
                    if (matcher.matches()) {
                        if (instanceFieldRef > Integer.parseInt(matcher.group(2))) {
                            instanceFieldRef = Integer.parseInt(matcher.group(2));
                            assignmentStmt = argument;
                            if (this.getConstructorArgs() != null && !this.getConstructorArgs().isEmpty()) {
                                argsCounter++;
                                paramName = this.getConstructorArgs().get(argsCounter - 1).toString();
                            }
                        } else {
                            assignmentStmt = argument;
                        }
                    }
                }
            }
        }

        if (!nodeSaved && node.getOperations().size() > 0) {
            controlFlowGraph.addNode(node);
        }

        return variableBlock;
    }


    /**
     * Special parsing of statements to find Process Variable operations.
     *
     * @param expr          Expression Unit from Statement
     * @param variableBlock current VariableBlock
     * @param element       BpmnElement
     * @param chapter       ElementChapter
     * @param fieldType     KnownElementFieldType
     * @param filePath      ResourceFilePath for ProcessVariableOperation
     * @param scopeId       Scope of BpmnElement
     */
    private void parseExpression(final JInterfaceInvokeExpr expr, final VariableBlock variableBlock,
                                 final BpmnElement element, final ElementChapter chapter, final KnownElementFieldType fieldType,
                                 final String filePath, String scopeId, final String paramName, final Node node) {

        String functionName = expr.getMethodRef().getName();
        int numberOfArg = expr.getArgCount();
        String baseBox = expr.getBaseBox().getValue().getType().toString();

        CamundaProcessVariableFunctions foundMethod = CamundaProcessVariableFunctions
                .findByNameAndNumberOfBoxes(functionName, baseBox, numberOfArg);

        if (foundMethod != null) {
            int location = foundMethod.getLocation() - 1;
            VariableOperation type = foundMethod.getOperationType();

            // Check if method call is to variable map for delegate variable mappings
            if (expr.getMethodRef().getDeclaringClass().getName()
                    .equals("org.camunda.bpm.engine.variable.VariableMap")) {
                // If so, scope id is the id of the child process
                scopeId = ((CallActivity) element.getBaseElement()).getCalledElement();
            }

            if (expr.getArgBox(location).getValue() instanceof StringConstant) {
                StringConstant variableName = (StringConstant) expr.getArgBox(location).getValue();
                String name = variableName.value.replaceAll("\"", "");
                node.addOperation(new ProcessVariableOperation(name, element, chapter, fieldType, filePath, type,
                        scopeId, element.getFlowAnalysis().getOperationCounter()));
                variableBlock.addProcessVariable(new ProcessVariableOperation(name, element, chapter, fieldType,
                        filePath, type, scopeId, element.getFlowAnalysis().getOperationCounter()));

            } else if (!paramName.isEmpty()) {
                node.addOperation(new ProcessVariableOperation(paramName.replaceAll("\"", ""), element, chapter,
                        fieldType, filePath, type, scopeId, element.getFlowAnalysis().getOperationCounter()));
                variableBlock.addProcessVariable(new ProcessVariableOperation(paramName.replaceAll("\"", ""), element,
                        chapter, fieldType, filePath, type, scopeId, element.getFlowAnalysis().getOperationCounter()));
            } else {
                IssueWriter.createIssue(new Rule("ProcessVariablesModelChecker", true, null, null, null, null), //$NON-NLS-1$
                        CriticalityEnum.WARNING, filePath, element,
                        String.format(Messages.getString("ProcessVariablesModelChecker.4"),
                                CheckName.checkName(element.getBaseElement()), chapter, fieldType.getDescription()));
            }
        }
    }


    /**
     * Parsing of initially discovered statements to find Process Variable
     * operations.
     *
     * @param expr             Expression Unit from Statement
     * @param element          Current BPMN Element
     * @param resourceFilePath Filepath of model
     * @return inital operations
     */
    private ListMultimap<String, ProcessVariableOperation> parseInitialExpression(final JInterfaceInvokeExpr expr,
                                                                                  final BpmnElement element, final String resourceFilePath) {

        final ListMultimap<String, ProcessVariableOperation> initialOperations = ArrayListMultimap.create();

        final String functionName = expr.getMethodRef().getName();
        final int numberOfArg = expr.getArgCount();
        final String baseBox = expr.getBaseBox().getValue().getType().toString();

        final CamundaProcessVariableFunctions foundMethod = CamundaProcessVariableFunctions
                .findByNameAndNumberOfBoxes(functionName, baseBox, numberOfArg);

        if (foundMethod != null) {
            final int location = foundMethod.getLocation() - 1;
            final VariableOperation type = foundMethod.getOperationType();
            if (expr.getArgBox(location).getValue() instanceof StringConstant) {
                final StringConstant variableName = (StringConstant) expr.getArgBox(location).getValue();
                final String name = variableName.value.replaceAll("\"", "");
                initialOperations.put(name,
                        new ProcessVariableOperation(name, element, ElementChapter.Code, KnownElementFieldType.Initial,
                                resourceFilePath, type,
                                element.getBaseElement().getScope().getAttributeValue(BpmnConstants.ATTR_ID),
                                element.getFlowAnalysis().getOperationCounter()));
            }
        }
        return initialOperations;
    }

    /**
     * Iterator through the source code line by line, collecting the
     * ProcessVariables Camunda methods are interface invocations appearing either
     * in Assign statement or Invoke statement Constraint: Only String constants can
     * be precisely recognized.
     *
     * @param classPaths       Set of classes that is included in inter-procedural analysis
     * @param cg               Soot ControlFlowGraph
     * @param outSet           OUT set of CFG
     * @param element          BpmnElement
     * @param chapter          ElementChapter
     * @param fieldType        KnownElementFieldType
     * @param filePath         ResourceFilePath for ProcessVariableOperation
     * @param scopeId          Scope of BpmnElement
     * @param variableBlock    VariableBlock
     * @param controlFlowGraph Control Flow Graph
     * @param node             Current node of the CFG
     * @param paramName        Name of the parameter
     * @param argsCounter      Counts the arguments in case of a method or constructor call
     * @param unit             Current unit
     * @return assignmentStmt
     */
    private String processInvokeStmt(final Set<String> classPaths, final CallGraph cg, final OutSetCFG outSet,
                                     final BpmnElement element, final ElementChapter chapter, final KnownElementFieldType fieldType,
                                     final String filePath, final String scopeId, final VariableBlock variableBlock, String assignmentStmt,
                                     final ControlFlowGraph controlFlowGraph, final Node node, final String paramName, final int argsCounter,
                                     final Unit unit) {
        // Method call of implemented interface method without prior assignment
        if (((InvokeStmt) unit).getInvokeExprBox().getValue() instanceof JInterfaceInvokeExpr) {
            JInterfaceInvokeExpr expr = (JInterfaceInvokeExpr) ((InvokeStmt) unit).getInvokeExprBox().getValue();
            if (expr != null) {
                if (argsCounter > 0) {
                    parseExpression(expr, variableBlock, element, chapter, fieldType, filePath, scopeId,
                            this.getConstructorArgs().get(argsCounter - 1).toString(), node);
                } else {
                    parseExpression(expr, variableBlock, element, chapter, fieldType, filePath, scopeId, paramName,
                            node);
                }
            }
        }
        // Method call without prior assignment
        if (((InvokeStmt) unit).getInvokeExprBox().getValue() instanceof JVirtualInvokeExpr) {
            JVirtualInvokeExpr expr = (JVirtualInvokeExpr) ((InvokeStmt) unit).getInvokeExprBox().getValue();
            checkInterProceduralCall(classPaths, cg, outSet, element, chapter, fieldType, scopeId, variableBlock, unit,
                    assignmentStmt, expr.getArgs(), controlFlowGraph, true);
        }
        // Constructor call
        if (((InvokeStmt) unit).getInvokeExprBox().getValue() instanceof JSpecialInvokeExpr) {
            JSpecialInvokeExpr expr = (JSpecialInvokeExpr) ((InvokeStmt) unit).getInvokeExprBox().getValue();
            if (((InvokeStmt) unit).getInvokeExprBox().getValue().toString().contains("void <init>")) {
                this.setConstructorArgs(expr.getArgs());
                assignmentStmt = expr.getBaseBox().getValue().toString();
            } else {
                checkInterProceduralCall(classPaths, cg, outSet, element, chapter, fieldType, scopeId, variableBlock,
                        unit, assignmentStmt, expr.getArgs(), controlFlowGraph, true);
            }
        }
        return assignmentStmt;
    }

    private List<Value> getConstructorArgs() {
        return constructorArgs;

    }

    /**
     * @param classPaths     Set of classes that is included in inter-procedural analysis
     * @param cg             Soot ControlFlowGraph
     * @param outSet         OUT set of CFG
     * @param element        BpmnElement
     * @param chapter        ElementChapter
     * @param fieldType      KnownElementFieldType
     * @param scopeId        Scope of BpmnElement
     * @param variableBlock  VariableBlock
     * @param unit           Current unit of code
     * @param assignmentStmt Left side of assignment statement
     * @param args           List of arguments
     */
    private void checkInterProceduralCall(final Set<String> classPaths, final CallGraph cg, final OutSetCFG outSet,
                                          final BpmnElement element, final ElementChapter chapter, final KnownElementFieldType fieldType,
                                          final String scopeId, final VariableBlock variableBlock, final Unit unit, final String assignmentStmt,
                                          final List<Value> args, final ControlFlowGraph controlFlowGraph, final boolean isInvoke) {

        final Iterator<Edge> sources = cg.edgesOutOf(unit);
        Edge src;
        while (sources.hasNext()) {
            src = sources.next();
            String methodName = src.tgt().getName();
            String className = src.tgt().getDeclaringClass().getName();
            className = ProcessVariablesScanner.cleanString(className, false);
            if (classPaths.contains(className)) {

                SootMethod sootMethod;
                if (isInvoke && !className.contains("$")) {
                    sootMethod = ((JInvokeStmt) unit).getInvokeExpr().getMethodRef().resolve();
                } else if (!isInvoke && !className.contains("$")) {
                    sootMethod = ((JAssignStmt) unit).getInvokeExpr().getMethodRef().resolve();
                } else {
                    sootMethod = null;
                }

                controlFlowGraph.incrementRecursionCounter();
                controlFlowGraph.addPriorLevel(controlFlowGraph.getPriorLevel());
                controlFlowGraph.resetInternalNodeCounter();
                javaReaderStatic.classFetcherRecursive(classPaths, className, methodName, className, element, chapter, fieldType,
                        scopeId, outSet, variableBlock, assignmentStmt, args, controlFlowGraph, sootMethod);
                controlFlowGraph.removePriorLevel();
                controlFlowGraph.decrementRecursionCounter();
                controlFlowGraph.setInternalNodeCounter(controlFlowGraph.getPriorLevel());
            }
        }
    }

    private void setConstructorArgs(final List<Value> args) {
        this.constructorArgs = args;
    }

    private void setReturnStmt(final String returnStmt) {
        this.returnStmt = returnStmt;
    }
}
