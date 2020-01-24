/**
 * BSD 3-Clause License
 * <p>
 * Copyright © 2019, viadee Unternehmensberatung AG
 * All rights reserved.
 * <p>
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * <p>
 * * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 * <p>
 * * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * <p>
 * * Neither the name of the copyright holder nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * <p>
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
import de.viadee.bpm.vPAV.processing.code.flow.*;
import de.viadee.bpm.vPAV.processing.code.flow.statement.Statement;
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

    private HashMap<SootMethod, Integer> methodStackTrace;

    private HashSet<Integer> processedBlocks;

    private String returnStmt;

    VariablesExtractor(JavaReaderStatic reader) {
        this.javaReaderStatic = reader;
        this.methodStackTrace = new HashMap<>();
        this.processedBlocks = new HashSet<>();
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
            final AnalysisElement[] predecessor) {
        if (variableBlock == null) {
            variableBlock = new VariableBlock(block, new ArrayList<>());
        }

        String paramName = "";
        int argsCounter = 0;
        int instanceFieldRef = Integer.MAX_VALUE;
        boolean nodeSaved = false;
        final ControlFlowGraph controlFlowGraph = element.getControlFlowGraph();
        Node node = new Node(element, block, chapter);

        final Iterator<Unit> unitIt = block.iterator();

        Unit unit;
        while (unitIt.hasNext()) {
            unit = unitIt.next();
            if (!checkExclusion(unit)) {
                continue;
            }
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
                        try {
                            if (parameter instanceof RefType) {
                                if (((RefType) parameter).getClassName()
                                        .equals(CamundaMethodServices.DELEGATE)) {
                                    passesDelegateExecution = true;
                                    break;
                                }
                            }
                        } catch (ClassCastException ignored) {
                        }
                    }
                    if (passesDelegateExecution) {
                        // Node must be splitted
                        if (!nodeSaved && node.getOperations().size() > 0) {
                            predecessor[0] = addNodeAndGetNewPredecessor(node, controlFlowGraph, predecessor[0]);
                        }
                        // TODO maybe say increase hierachy if we would like to keep it
                        // Split node
                        Node newSectionNode = (Node) node.clone();
                        assignmentStmt = processInvokeStmt(classPaths, cg, outSet, element, chapter, fieldType,
                                filePath, scopeId, variableBlock, assignmentStmt, node, paramName, argsCounter,
                                unit, predecessor);
                        paramName = returnStmt;
                        node = newSectionNode;
                        nodeSaved = false;
                    } else {
                        assignmentStmt = processInvokeStmt(classPaths, cg, outSet, element, chapter, fieldType,
                                filePath, scopeId, variableBlock, assignmentStmt, node, paramName, argsCounter,
                                unit, predecessor);
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
                        if (!nodeSaved && node.getOperations().size() > 0) {
                            predecessor[0] = addNodeAndGetNewPredecessor(node, controlFlowGraph, predecessor[0]);
                        }
                        // Split node
                        Node newSectionNode = (Node) node.clone();
                        checkInterProceduralCall(classPaths, cg, outSet, element, chapter, fieldType, scopeId,
                                variableBlock, unit, assignmentStmt, expr.getArgs(), Statement.ASSIGNMENT, predecessor,
                                expr.getMethod().getParameterTypes());
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
                        parseInterfaceInvokeExpression(expr, variableBlock, element, chapter, fieldType, filePath,
                                scopeId, paramName, node, unit, classPaths, cg, outSet, assignmentStmt, predecessor);
                    }
                }
                // Method call of private method with assignment to a variable
                if (((AssignStmt) unit).getRightOpBox().getValue() instanceof JSpecialInvokeExpr) {
                    JSpecialInvokeExpr expr = (JSpecialInvokeExpr) ((AssignStmt) unit).getRightOpBox().getValue();
                    if (expr != null) {
                        parseSpecialInvokeExpression(expr, variableBlock, element, chapter, fieldType, filePath,
                                scopeId, paramName, node, unit, classPaths, cg, outSet, assignmentStmt, predecessor);
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

        if ((!nodeSaved && node.getOperations().size() > 0)) {
            predecessor[0] = addNodeAndGetNewPredecessor(node, controlFlowGraph, predecessor[0]);
        }

        // Process successors
        for (Block succ : block.getSuccs()) {
            AnalysisElement[] newPredecessor = new AnalysisElement[] { predecessor[0] };

            // Process block only if not yet processed
            if (!processedBlocks.contains(getCustomHashForBlock(succ))) {
                processedBlocks.add(getCustomHashForBlock(succ));
                // Collect the functions Unit by Unit via the blockIterator
                final VariableBlock vb2 = this
                        .blockIterator(classPaths, cg, succ, outSet, element, chapter, fieldType, filePath,
                                scopeId, null, assignmentStmt, args, newPredecessor);

                // depending if outset already has that Block, only add variables,
                // if not, then add the whole vb
                if (outSet.getVariableBlock(vb2.getBlock()) == null) {
                    outSet.addVariableBlock(vb2);
                }
            } else {
                // Find node and add predecessor
                Node n = null;
                int succHash = getCustomHashForBlock(succ);
                for (AbstractNode tempNode : element.getControlFlowGraph().getNodes().values()) {
                    if (tempNode instanceof Node) {
                        if (getCustomHashForBlock(((Node) tempNode).getBlock()) == succHash) {
                            n = (Node) tempNode;
                            break;
                        }
                    }
                }
                if (n == null) {
                    // Happens when block has a successor that is not included in the control flow graph because
                    // is does not contain any operations
                    // The successors of the successor have to be found and added
                    // TODO add test case
                    if (element.getControlFlowGraph().getNodes().size() > 0) {
                        ArrayList<Integer> visitedBlocks = new ArrayList<>();
                        visitedBlocks.add(getCustomHashForBlock(block));
                        addPredecessorToSuccessors(succ, predecessor[0], element, visitedBlocks);
                    }
                } else {
                    n.addPredecessor(predecessor[0]);
                }
            }
        }

        return variableBlock;
    }

    private boolean checkExclusion(Unit unit) {
        if (unit instanceof JAssignStmt) {
            if (((JAssignStmt) unit).getRightOp().getType() instanceof RefType) {
                return !((RefType) ((JAssignStmt) unit).getRightOp().getType()).getClassName()
                        .equals("org.slf4j.Logger");
            }
        }
        if (unit instanceof JInvokeStmt) {
            return !((JInvokeStmt) unit).getInvokeExpr().getMethod().getDeclaringClass().toString()
                    .equals("org.slf4j.Logger");
        }
        return true;
    }

    /**
     * Special parsing of statements to find Process Variable operations.
     *
     * @param expr          Expression Unit from Statement (private access modifier)
     * @param variableBlock current VariableBlock
     * @param element       BpmnElement
     * @param chapter       ElementChapter
     * @param fieldType     KnownElementFieldType
     * @param filePath      ResourceFilePath for ProcessVariableOperation
     * @param scopeId       Scope of BpmnElement
     */
    private void parseSpecialInvokeExpression(final JSpecialInvokeExpr expr, final VariableBlock variableBlock,
            final BpmnElement element, final ElementChapter chapter, final KnownElementFieldType fieldType,
            final String filePath, String scopeId, final String paramName, final Node node, final Unit unit,
            final Set<String> classPaths, final CallGraph cg, final OutSetCFG outSet, final String assignmentStmt,
            final AnalysisElement[] predecessor) {
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
                    .equals(CamundaMethodServices.VARIABLE_MAP)) {
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
                IssueWriter.createIssue(new Rule("ProcessVariablesModelChecker", true, null, null, null, null),
                        //$NON-NLS-1$
                        CriticalityEnum.WARNING, filePath, element,
                        String.format(Messages.getString("ProcessVariablesModelChecker.4"),
                                CheckName.checkName(element.getBaseElement()), chapter, fieldType.getDescription()));
            }
        } else {
            checkInterProceduralCall(classPaths, cg, outSet, element, chapter, fieldType, scopeId, variableBlock,
                    unit, assignmentStmt, expr.getArgs(), Statement.ASSIGNMENT_INVOKE, predecessor, expr.getMethod().getParameterTypes());
        }
    }

    /**
     * Special parsing of statements to find Process Variable operations.
     *
     * @param expr          Expression Unit from Statement (public access modifier)
     * @param variableBlock current VariableBlock
     * @param element       BpmnElement
     * @param chapter       ElementChapter
     * @param fieldType     KnownElementFieldType
     * @param filePath      ResourceFilePath for ProcessVariableOperation
     * @param scopeId       Scope of BpmnElement
     */
    private void parseInterfaceInvokeExpression(final JInterfaceInvokeExpr expr, final VariableBlock variableBlock,
            final BpmnElement element, final ElementChapter chapter, final KnownElementFieldType fieldType,
            final String filePath, String scopeId, final String paramName, final Node node, final Unit unit,
            final Set<String> classPaths, final CallGraph cg, final OutSetCFG outSet, final String assignmentStmt,
            final AnalysisElement[] predecessor) {
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
                    .equals(CamundaMethodServices.VARIABLE_MAP)) {
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
                IssueWriter.createIssue(new Rule("ProcessVariablesModelChecker", true, null, null, null, null),
                        //$NON-NLS-1$
                        CriticalityEnum.WARNING, filePath, element,
                        String.format(Messages.getString("ProcessVariablesModelChecker.4"),
                                CheckName.checkName(element.getBaseElement()), chapter, fieldType.getDescription()));
            }
        } else {
            checkInterProceduralCall(classPaths, cg, outSet, element, chapter, fieldType, scopeId, variableBlock,
                    unit, assignmentStmt, expr.getArgs(), Statement.INVOKE, predecessor, expr.getMethod().getParameterTypes());
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
     * @param classPaths    Set of classes that is included in inter-procedural analysis
     * @param cg            Soot ControlFlowGraph
     * @param outSet        OUT set of CFG
     * @param element       BpmnElement
     * @param chapter       ElementChapter
     * @param fieldType     KnownElementFieldType
     * @param filePath      ResourceFilePath for ProcessVariableOperation
     * @param scopeId       Scope of BpmnElement
     * @param variableBlock VariableBlock
     * @param node          Current node of the CFG
     * @param paramName     Name of the parameter
     * @param argsCounter   Counts the arguments in case of a method or constructor call
     * @param unit          Current unit
     * @return assignmentStmt
     */
    private String processInvokeStmt(final Set<String> classPaths, final CallGraph cg, final OutSetCFG outSet,
            final BpmnElement element, final ElementChapter chapter, final KnownElementFieldType fieldType,
            final String filePath, final String scopeId, final VariableBlock variableBlock, String assignmentStmt,
            final Node node, final String paramName, final int argsCounter,
            final Unit unit, final AnalysisElement[] predecessor) {
        // Method call of implemented interface method without prior assignment
        if (((InvokeStmt) unit).getInvokeExprBox().getValue() instanceof JInterfaceInvokeExpr) {
            JInterfaceInvokeExpr expr = (JInterfaceInvokeExpr) ((InvokeStmt) unit).getInvokeExprBox().getValue();
            if (expr != null) {
                if (argsCounter > 0) {
                    parseInterfaceInvokeExpression(expr, variableBlock, element, chapter, fieldType, filePath, scopeId,
                            this.getConstructorArgs().get(argsCounter - 1).toString(), node, unit, classPaths, cg,
                            outSet, assignmentStmt, predecessor);
                } else {
                    parseInterfaceInvokeExpression(expr, variableBlock, element, chapter, fieldType, filePath, scopeId,
                            paramName, node, unit, classPaths, cg, outSet, assignmentStmt, predecessor);
                }
            }
        }
        // Method call without prior assignment
        else if (((InvokeStmt) unit).getInvokeExprBox().getValue() instanceof JVirtualInvokeExpr) {
            JVirtualInvokeExpr expr = (JVirtualInvokeExpr) ((InvokeStmt) unit).getInvokeExprBox().getValue();
            checkInterProceduralCall(classPaths, cg, outSet, element, chapter, fieldType, scopeId, variableBlock,
                    unit, assignmentStmt, expr.getArgs(), Statement.INVOKE, predecessor, expr.getMethod().getParameterTypes());
        }
        // Constructor call
        else if (((InvokeStmt) unit).getInvokeExprBox().getValue() instanceof JSpecialInvokeExpr) {
            JSpecialInvokeExpr expr = (JSpecialInvokeExpr) ((InvokeStmt) unit).getInvokeExprBox().getValue();
            if (((InvokeStmt) unit).getInvokeExprBox().getValue().toString().contains("void <init>")) {
                this.setConstructorArgs(expr.getArgs());
                assignmentStmt = expr.getBaseBox().getValue().toString();
            } else {
                checkInterProceduralCall(classPaths, cg, outSet, element, chapter, fieldType, scopeId,
                        variableBlock, unit, assignmentStmt, expr.getArgs(), Statement.INVOKE, predecessor,
                        expr.getMethod().getParameterTypes());
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
            final List<Value> args, final Statement statement, AnalysisElement[] predecessor,
            List<Type> parameterTypes) {

        final ControlFlowGraph controlFlowGraph = element.getControlFlowGraph();
        final Iterator<Edge> sources = cg.edgesOutOf(unit);
        Edge src;
        while (sources.hasNext()) {
            src = sources.next();
            String methodName = src.tgt().getName();
            String className = src.tgt().getDeclaringClass().getName();
            className = ProcessVariablesScanner.cleanString(className, false);
            if (classPaths.contains(className) && !className.contains("$")) {
                SootMethod sootMethod;
                if (Statement.INVOKE.equals(statement)) {
                    sootMethod = ((JInvokeStmt) unit).getInvokeExpr().getMethodRef().resolve();
                } else if (Statement.ASSIGNMENT.equals(statement) || Statement.ASSIGNMENT_INVOKE.equals(statement)) {
                    sootMethod = ((JAssignStmt) unit).getInvokeExpr().getMethodRef().resolve();
                } else {
                    sootMethod = null;
                }

                controlFlowGraph.incrementRecursionCounter();
                controlFlowGraph.addPriorLevel(controlFlowGraph.getPriorLevel());
                controlFlowGraph.resetInternalNodeCounter();

                javaReaderStatic.classFetcherRecursive(classPaths, className, methodName, className, element, chapter,
                        fieldType, scopeId, outSet, variableBlock, assignmentStmt, args, sootMethod, predecessor,
                        parameterTypes);
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

    public void resetMethodStackTrace() {
        this.methodStackTrace.clear();
    }

    public boolean visitMethod(SootMethod sootMethod) {
        if (methodStackTrace.containsKey(sootMethod)) {
            int num = methodStackTrace.get(sootMethod);
            if (num >= 2) {
                // break recursion
                return false;
            } else {
                // increase method counter
                methodStackTrace.put(sootMethod, num + 1);
            }

        } else {
            // add method
            methodStackTrace.put(sootMethod, 1);
        }
        return true;
    }

    public void leaveMethod(SootMethod sootMethod) {
        int num = methodStackTrace.get(sootMethod);
        methodStackTrace.put(sootMethod, num - 1);
    }

    private AbstractNode addNodeAndGetNewPredecessor(AbstractNode node, ControlFlowGraph cg,
            AnalysisElement predecessor) {
        cg.addNode(node);
        if (predecessor != null) {
            node.addPredecessor(predecessor);
        }

        return node;
    }

    private int getCustomHashForBlock(Block block) {
        // Ignore successors and predecessors when calculating hash
        return Objects.hash(block.getHead().hashCode(), block.getTail().hashCode(), block.getBody().hashCode(),
                block.getIndexInMethod());
    }

    private void addPredecessorToSuccessors(Block block, AnalysisElement pred, AnalysisElement element,
            ArrayList<Integer> visitedBlocks) {
        Node n = null;
        int succHash;
        for (Block succ : block.getSuccs()) {
            succHash = getCustomHashForBlock(succ);
            if (visitedBlocks.contains(succHash))
                continue;
            visitedBlocks.add(succHash);
            for (AbstractNode tempNode : element.getControlFlowGraph().getNodes().values()) {
                if (tempNode instanceof Node) {
                    if (getCustomHashForBlock(((Node) tempNode).getBlock()) == succHash) {
                        n = (Node) tempNode;
                        n.addPredecessor(pred);
                        break;
                    }
                }
            }
            if (n == null) {
                addPredecessorToSuccessors(succ, pred, element, visitedBlocks);
            }
        }
    }
}
