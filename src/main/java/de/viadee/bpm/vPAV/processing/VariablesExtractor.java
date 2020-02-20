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

    private JavaReaderStatic javaReaderStatic;

    private HashMap<SootMethod, Integer> methodStackTrace;

    private HashSet<Integer> processedBlocks;

    private HashMap<String, ObjectVariable> objectVariables;

    private String returnStmt;

    VariablesExtractor(JavaReaderStatic reader) {
        this.javaReaderStatic = reader;
        this.methodStackTrace = new HashMap<>();
        this.processedBlocks = new HashSet<>();
        this.objectVariables = new HashMap<>();
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
            final AnalysisElement[] predecessor, final String thisObject) {
        if (variableBlock == null) {
            variableBlock = new VariableBlock(block, new ArrayList<>());
        }

        HashMap<String, StringVariable> localStringVariables = new HashMap<>();
        String paramName = "";
        final ControlFlowGraph controlFlowGraph = element.getControlFlowGraph();
        Node node = new Node(element, block, chapter);

        final Iterator<Unit> unitIt = block.iterator();

        Unit unit;
        while (unitIt.hasNext()) {
            unit = unitIt.next();
            if (!checkExclusion(unit)) {
                continue;
            }
            // e. g. r0 := @this: de.viadee.bpm.vPAV.delegates.TestDelegate
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
            // e.g. interfaceinvoke r1. ... (Method call)
            if (unit instanceof InvokeStmt) {
                try {
                    // To improve performance, node splitting should be only done if called method can access delegate
                    // execution object
                    // We do not know it the node must be splitted, so we just do it in most cases
                    if (((JInvokeStmt) unit).getInvokeExpr() instanceof JStaticInvokeExpr) {
                        JStaticInvokeExpr expr = (JStaticInvokeExpr) ((InvokeStmt) unit).getInvokeExpr();
                        checkInterProceduralCall(classPaths, cg, outSet, element, chapter, fieldType, scopeId,
                                variableBlock,
                                unit, assignmentStmt, expr.getArgs(), Statement.INVOKE, predecessor,
                                expr.getMethod().getParameterTypes(), null);
                    } else {
                        // Operation on DelegateExecution object is called
                        if (((InstanceInvokeExpr) ((JInvokeStmt) unit).getInvokeExpr()).getBase().getType().toString()
                                .equals(CamundaMethodServices.DELEGATE)) {
                            parseInterfaceInvokeExpression((JInterfaceInvokeExpr) ((JInvokeStmt) unit).getInvokeExpr(),
                                    variableBlock, element, chapter, fieldType, filePath, scopeId,
                                    node, unit, classPaths, cg,
                                    outSet, assignmentStmt, predecessor, Statement.INVOKE, localStringVariables);
                        }
                        // Node has to be splitted
                        else if (node.getOperations().size() > 0) {
                            predecessor[0] = addNodeAndGetNewPredecessor(node, controlFlowGraph, predecessor[0]);
                            Node newSectionNode = (Node) node.clone();
                            assignmentStmt = processInvokeStmt(classPaths, cg, outSet, element, chapter, fieldType,
                                    filePath, scopeId, variableBlock, assignmentStmt, node,
                                    unit, predecessor, localStringVariables);
                            paramName = returnStmt;
                            node = newSectionNode;
                        } else {
                            assignmentStmt = processInvokeStmt(classPaths, cg, outSet, element, chapter, fieldType,
                                    filePath, scopeId, variableBlock, assignmentStmt, node,
                                    unit, predecessor, localStringVariables);
                        }
                    }
                } catch (CloneNotSupportedException e) {
                    e.printStackTrace();
                }
            }
            // e. g. $r2 = staticinvoke ... (Assignment)
            if (unit instanceof AssignStmt) {
                // TODO wann können die temporären von soot erstellten $r variablen gelöscht werden?
                // Local String Variable is updated by directly assigning a new String constant
                if (((AssignStmt) unit).getLeftOpBox().getValue() instanceof JimpleLocal && ((AssignStmt) unit)
                        .getLeftOpBox().getValue()
                        .getType().equals(RefType.v("java.lang.String")) &&
                        ((AssignStmt) unit).getRightOpBox().getValue() instanceof StringConstant
                ) {
                    String varIdentifier = ((AssignStmt) unit).getLeftOpBox().getValue().toString();
                    String value = ((StringConstant) ((AssignStmt) unit).getRightOpBox().getValue()).value;
                    updateStringVariable(localStringVariables, varIdentifier, value);
                }
                // Local variable gets value of object field
                //              else if (((AssignStmt) unit).getLeftOpBox().getValue() instanceof JimpleLocal &&
                //                    ((AssignStmt) unit).getRightOpBox().getValue() instanceof JInstanceFieldRef) {

                //          }
                // Object String Variable is updated by directly assigning a new String constanct
                else if (((AssignStmt) unit)
                        .getLeftOpBox().getValue()
                        .getType().equals(RefType.v("java.lang.String")) &&
                        ((AssignStmt) unit).getRightOpBox().getValue() instanceof StringConstant) {
                    updateObjectStringVariable(unit);
                }
                // Renaming of variables
                else if (((AssignStmt) unit).getLeftOpBox().getValue() instanceof JimpleLocal && ((AssignStmt) unit)
                        .getRightOpBox().getValue() instanceof JimpleLocal) {
                    String varIdentifierRight = ((JimpleLocal) ((AssignStmt) unit).getRightOpBox().getValue())
                            .getName();
                    if (objectVariables.containsKey(varIdentifierRight)) {
                        String varIdentifierLeft = ((JimpleLocal) ((AssignStmt) unit).getLeftOpBox().getValue())
                                .getName();
                        objectVariables.put(varIdentifierLeft, objectVariables.get(varIdentifierRight));
                        // Temporary variable can be deleted
                        if (varIdentifierRight.startsWith("$")) {
                            objectVariables.remove(varIdentifierRight);
                        }
                    }
                }
                // Process method call because other objects might be manipulated
                else {
                    // Method call with assignment to a variable
                    if (((AssignStmt) unit).getRightOpBox().getValue() instanceof JVirtualInvokeExpr) {
                        assignmentStmt = ((AssignStmt) unit).getLeftOpBox().getValue().toString();
                        JVirtualInvokeExpr expr = (JVirtualInvokeExpr) ((AssignStmt) unit).getRightOpBox().getValue();

                        try {
                            if (node.getOperations().size() > 0) {
                                predecessor[0] = addNodeAndGetNewPredecessor(node, controlFlowGraph, predecessor[0]);
                            }
                            // Split node
                            Node newSectionNode = (Node) node.clone();
                            checkInterProceduralCall(classPaths, cg, outSet, element, chapter, fieldType, scopeId,
                                    variableBlock, unit, assignmentStmt, expr.getArgs(), Statement.ASSIGNMENT,
                                    predecessor,
                                    expr.getMethod().getParameterTypes(), null);
                            paramName = returnStmt;
                            node = newSectionNode;

                        } catch (CloneNotSupportedException e) {
                            e.printStackTrace();
                        }
                    }
                    // Method call of implemented interface method with assignment to a variable
                    if (((AssignStmt) unit).getRightOpBox().getValue() instanceof JInterfaceInvokeExpr) {
                        JInterfaceInvokeExpr expr = (JInterfaceInvokeExpr) ((AssignStmt) unit).getRightOpBox()
                                .getValue();
                        if (expr != null) {
                            parseInterfaceInvokeExpression(expr, variableBlock, element, chapter, fieldType, filePath,
                                    scopeId, node, unit, classPaths, cg, outSet, assignmentStmt, predecessor,
                                    Statement.ASSIGNMENT, localStringVariables);
                        }
                    }
                    // Method call of private method with assignment to a variable
                    if (((AssignStmt) unit).getRightOpBox().getValue() instanceof JSpecialInvokeExpr) {
                        JSpecialInvokeExpr expr = (JSpecialInvokeExpr) ((AssignStmt) unit).getRightOpBox().getValue();
                        if (expr != null) {
                            parseSpecialInvokeExpression(expr, variableBlock, element, chapter, fieldType, filePath,
                                    scopeId, paramName, node, unit, classPaths, cg, outSet, assignmentStmt, predecessor
                            );
                        }
                    }

                    // Instance fields
                    if (((AssignStmt) unit).getRightOpBox().getValue() instanceof JInstanceFieldRef) {
                        // Extract name
                        int spaceIdx = ((AssignStmt) unit).getRightOpBox().getValue().toString().lastIndexOf(" ");
                        int gtsIdx = ((AssignStmt) unit).getRightOpBox().getValue().toString().lastIndexOf(">");
                        String objIdentifier = ((JInstanceFieldRef) ((AssignStmt) unit).getRightOpBox().getValue())
                                .getBase()
                                .toString();
                        String varIdentifier = ((AssignStmt) unit).getLeftOpBox().getValue().toString();
                        String varName = ((AssignStmt) unit).getRightOpBox().getValue().toString()
                                .substring(spaceIdx, gtsIdx);
                        ObjectVariable objVar = objectVariables.get(objIdentifier);

                        if (objVar == null) {
                            if (objIdentifier.equals("r0")) {
                                objVar = objectVariables.get(thisObject);
                            }
                            // TODO
                        }
                        if (objVar != null) {
                            StringVariable stringVar = objVar.getStringField(varName);
                            if (stringVar == null) {
                                // TODO
                            } else {
                                updateStringVariable(localStringVariables, varIdentifier, stringVar.getValue());
                            }
                        }
                    }
                    // Assignment of new object
                    if (((AssignStmt) unit).getRightOpBox().getValue() instanceof JNewExpr) {
                        // Here variable is created, constructor is processed in next statement
                        objectVariables.put(((AssignStmt) unit).getLeftOpBox().getValue().toString(), null);
                    }
                }

                // TODO array variable
                // TODO test variable resolving if local variable is on right side

            }
        }

        if (node.getOperations().size() > 0) {
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
                                scopeId, null, assignmentStmt, args, newPredecessor, "");

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
                if (predecessor[0] != null) {
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
        }

        return variableBlock;
    }

    private void updateObjectStringVariable(Unit unit) {
        // Extract name
        int spaceIdx = ((AssignStmt) unit).getLeftOpBox().getValue().toString().lastIndexOf(" ");
        int gtsIdx = ((AssignStmt) unit).getLeftOpBox().getValue().toString().lastIndexOf(">");
        String objIdentifier = ((JInstanceFieldRef) ((AssignStmt) unit).getLeftOpBox().getValue()).getBase()
                .toString();
        String varName = ((AssignStmt) unit).getLeftOpBox().getValue().toString()
                .substring(spaceIdx, gtsIdx);
        String value = ((StringConstant) ((AssignStmt) unit).getRightOpBox().getValue()).value;
        ObjectVariable objVar = objectVariables.get(objIdentifier);
        if (objVar == null) {
            objVar = new ObjectVariable();
            objVar.updateStringField(varName, value);
            objectVariables.put(objIdentifier, objVar);
        } else {
            objVar.updateStringField(varName, value);
        }
    }

    private void updateStringVariable(HashMap<String, StringVariable> localStringVariables, String varIdentifier,
            String value) {
        if (localStringVariables.containsKey(varIdentifier)) {
            StringVariable var = localStringVariables.get(varIdentifier);
            var.setValue(value);
        } else {
            StringVariable var = new StringVariable(value);
            localStringVariables.put(varIdentifier, var);
        }
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
                    unit, assignmentStmt, expr.getArgs(), Statement.ASSIGNMENT, predecessor,
                    expr.getMethod().getParameterTypes(), null);
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
            final String filePath, String scopeId, final Node node, final Unit unit,
            final Set<String> classPaths, final CallGraph cg, final OutSetCFG outSet, final String assignmentStmt,
            final AnalysisElement[] predecessor, final Statement statement,
            HashMap<String, StringVariable> localStringVariables) {
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
            } else if (expr.getArgBox(location).getValue() instanceof JimpleLocal) {
                // Local variable or object field (is transferred to local variable in soot) is used
                String variableIdentifier = expr.getArgBox(location).getValue().toString();
                StringVariable var = localStringVariables.get(variableIdentifier);
                if (var != null) {
                    node.addOperation(
                            new ProcessVariableOperation(var.getValue(), element, chapter, fieldType, filePath, type,
                                    scopeId, element.getFlowAnalysis().getOperationCounter()));
                    variableBlock.addProcessVariable(
                            new ProcessVariableOperation(var.getValue(), element, chapter, fieldType,
                                    filePath, type, scopeId, element.getFlowAnalysis().getOperationCounter()));
                } else {
                    // TODO
                }
            } else {
                IssueWriter.createIssue(new Rule("ProcessVariablesModelChecker", true, null, null, null, null),
                        //$NON-NLS-1$
                        CriticalityEnum.WARNING, filePath, element,
                        String.format(Messages.getString("ProcessVariablesModelChecker.4"),
                                CheckName.checkName(element.getBaseElement()), chapter, fieldType.getDescription()));
            }
        } else {
            checkInterProceduralCall(classPaths, cg, outSet, element, chapter, fieldType, scopeId, variableBlock,
                    unit, assignmentStmt, expr.getArgs(), statement, predecessor, expr.getMethod().getParameterTypes(),
                    null);
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
     * @param unit          Current unit
     * @return assignmentStmt
     */
    private String processInvokeStmt(final Set<String> classPaths, final CallGraph cg, final OutSetCFG outSet,
            final BpmnElement element, final ElementChapter chapter, final KnownElementFieldType fieldType,
            final String filePath, final String scopeId, final VariableBlock variableBlock, String assignmentStmt,
            final Node node,
            final Unit unit, final AnalysisElement[] predecessor,
            final HashMap<String, StringVariable> localStringVariables) {
        // Method call of implemented interface method without prior assignment
        if (((InvokeStmt) unit).getInvokeExprBox().getValue() instanceof JInterfaceInvokeExpr) {
            JInterfaceInvokeExpr expr = (JInterfaceInvokeExpr) ((InvokeStmt) unit).getInvokeExprBox().getValue();
            if (expr != null) {
                parseInterfaceInvokeExpression(expr, variableBlock, element, chapter, fieldType, filePath, scopeId,
                        node, unit, classPaths, cg, outSet, assignmentStmt, predecessor,
                        Statement.INVOKE, null);
            }
        }
        // Method call without prior assignment
        else if (((InvokeStmt) unit).getInvokeExprBox().getValue() instanceof JVirtualInvokeExpr) {
            JVirtualInvokeExpr expr = (JVirtualInvokeExpr) ((InvokeStmt) unit).getInvokeExprBox().getValue();
            checkInterProceduralCall(classPaths, cg, outSet, element, chapter, fieldType, scopeId, variableBlock,
                    unit, assignmentStmt, expr.getArgs(), Statement.INVOKE, predecessor,
                    expr.getMethod().getParameterTypes(), expr.getBase().toString());
        }
        // Constructor call
        else if (((InvokeStmt) unit).getInvokeExprBox().getValue() instanceof JSpecialInvokeExpr) {
            JSpecialInvokeExpr expr = (JSpecialInvokeExpr) ((InvokeStmt) unit).getInvokeExprBox().getValue();
            List<Value> args = expr.getArgs();
            for (int i = 0; i < args.size(); i++) {
                Value val = args.get(i);
                // Pass String variables as value not reference
                if (val instanceof JimpleLocal) {
                    if (val.getType().toString().equals("java.lang.String")) {
                        args.set(i,
                                StringConstant.v(localStringVariables.get(((JimpleLocal) val).getName()).getValue()));
                    }
                }
            }

            ObjectVariable obj = checkInterProceduralCall(classPaths, cg, outSet, element, chapter, fieldType, scopeId,
                    variableBlock, unit, assignmentStmt, args, Statement.INVOKE, predecessor,
                    expr.getMethod().getParameterTypes(), null);
            String varIdentifier = ((JSpecialInvokeExpr) ((InvokeStmt) unit).getInvokeExprBox().getValue()).getBase()
                    .toString();
            if (obj != null) {
                objectVariables.put(varIdentifier, obj);
            }
        }
        return assignmentStmt;
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
    private ObjectVariable checkInterProceduralCall(final Set<String> classPaths, final CallGraph cg,
            final OutSetCFG outSet,
            final BpmnElement element, final ElementChapter chapter, final KnownElementFieldType fieldType,
            final String scopeId, final VariableBlock variableBlock, final Unit unit, final String assignmentStmt,
            final List<Value> args, final Statement statement, AnalysisElement[] predecessor,
            List<Type> parameterTypes, final String thisObject) {

        ObjectVariable obj = null;
        final ControlFlowGraph controlFlowGraph = element.getControlFlowGraph();
        final Iterator<Edge> sources = cg.edgesOutOf(unit);
        Edge src;
        while (sources.hasNext()) {
            src = sources.next();
            String methodName = src.tgt().getName();
            String className = src.tgt().getDeclaringClass().getName();
            className = ProcessVariablesScanner.cleanString(className, false);
            if (classPaths.contains(className)) {
                SootMethod sootMethod;
                if (Statement.INVOKE.equals(statement)) {
                    sootMethod = ((JInvokeStmt) unit).getInvokeExpr().getMethodRef().resolve();
                } else if (Statement.ASSIGNMENT.equals(statement)) {
                    sootMethod = ((JAssignStmt) unit).getInvokeExpr().getMethodRef().resolve();
                } else {
                    sootMethod = null;
                }

                controlFlowGraph.incrementRecursionCounter();
                controlFlowGraph.addPriorLevel(controlFlowGraph.getPriorLevel());
                controlFlowGraph.resetInternalNodeCounter();

                obj = javaReaderStatic
                        .classFetcherRecursive(classPaths, className, methodName, className, element, chapter,
                                fieldType, scopeId, outSet, variableBlock, assignmentStmt, args, sootMethod,
                                predecessor,
                                parameterTypes, thisObject);
                controlFlowGraph.removePriorLevel();
                controlFlowGraph.decrementRecursionCounter();
                controlFlowGraph.setInternalNodeCounter(controlFlowGraph.getPriorLevel());
            }
        }
        return obj;
    }

    private void setReturnStmt(final String returnStmt) {
        this.returnStmt = returnStmt;
    }

    void resetMethodStackTrace() {
        this.methodStackTrace.clear();
    }

    boolean visitMethod(SootMethod sootMethod) {
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

    void leaveMethod(SootMethod sootMethod) {
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
