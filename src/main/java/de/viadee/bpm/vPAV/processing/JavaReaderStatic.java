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
import de.viadee.bpm.vPAV.FileScanner;
import de.viadee.bpm.vPAV.constants.BpmnConstants;
import de.viadee.bpm.vPAV.constants.CamundaMethodServices;
import de.viadee.bpm.vPAV.processing.code.flow.FlowGraph;
import de.viadee.bpm.vPAV.processing.code.flow.Node;
import de.viadee.bpm.vPAV.processing.model.data.*;
import org.camunda.bpm.engine.variable.VariableMap;
import soot.*;
import soot.jimple.*;
import soot.jimple.internal.*;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.options.Options;
import soot.toolkits.graph.Block;
import soot.toolkits.graph.BlockGraph;
import soot.toolkits.graph.ClassicCompleteBlockGraph;

import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JavaReaderStatic implements JavaReader {

	public static final Logger LOGGER = Logger.getLogger(JavaReaderStatic.class.getName());

	private String returnStmt;

	private List<Value> constructorArgs;

	/**
	 * Checks a java delegate for process variable references with static code
	 * analysis (read/write/delete).
	 *
	 * Constraints: names, which only could be determined at runtime, can't be
	 * analyzed. e.g. execution.setVariable(execution.getActivityId() + "-" +
	 * execution.getEventName(), true)
	 *
	 * @param fileScanner
	 *            FileScanner
	 * @param classFile
	 *            Name of the class
	 * @param element
	 *            Bpmn element
	 * @param chapter
	 *            ElementChapter
	 * @param fieldType
	 *            KnownElementFieldType
	 * @param scopeId
	 *            Scope of the element
	 * @return Map of process variables from the referenced delegate
	 */
	public ListMultimap<String, ProcessVariableOperation> getVariablesFromJavaDelegate(final FileScanner fileScanner,
			final String classFile, final BpmnElement element, final ElementChapter chapter,
			final KnownElementFieldType fieldType, final String scopeId, final FlowGraph flowGraph) {

		final ListMultimap<String, ProcessVariableOperation> variables = ArrayListMultimap.create();

		if (classFile != null && classFile.trim().length() > 0) {

			final String sootPath = FileScanner.getSootPath();

			System.setProperty("soot.class.path", sootPath);

			final Set<String> classPaths = fileScanner.getJavaResourcesFileInputStream();
			final ArrayList<String> delegateMethods = new ArrayList<>();
			delegateMethods.add("execute");
			delegateMethods.add("notify");
			delegateMethods.add("mapInputVariables");
			delegateMethods.add("mapOutputVariables");

			for (String delegateMethodName : delegateMethods) {
				variables.putAll(classFetcher(classPaths, classFile, delegateMethodName, classFile, element, chapter,
						fieldType, scopeId, flowGraph));
			}
		}
		return variables;
	}

	/**
	 *
	 * Retrieves variables from a class
	 *
	 * @param className
	 *            Name of the class that potentially declares process variables
	 * @param scanner
	 *            OuterProcessVariablesScanner
	 * @param element
	 *            BpmnElement
	 * @param resourceFilePath
	 *            Path of the BPMN model
	 * @return Map of process variable operations
	 */
	public ListMultimap<String, ProcessVariableOperation> getVariablesFromClass(String className,
			final ProcessVariablesScanner scanner, final BpmnElement element, final String resourceFilePath,
			final EntryPoint entryPoint) {

		final ListMultimap<String, ProcessVariableOperation> initialOperations = ArrayListMultimap.create();

		if (className != null && className.trim().length() > 0) {
			final String sootPath = FileScanner.getSootPath();
			System.setProperty("soot.class.path", sootPath);

			className = cleanString(className, true);

			Options.v().set_whole_program(true);
			Options.v().set_allow_phantom_refs(true);

			SootClass sootClass = Scene.v().forceResolve(className, SootClass.SIGNATURES);

			if (sootClass != null) {
				sootClass.setApplicationClass();
				Scene.v().loadNecessaryClasses();
				for (SootMethod method : sootClass.getMethods()) {
					if (method.getName().equals(entryPoint.getMethodName())) {
						final Body body = method.retrieveActiveBody();
						initialOperations.putAll(checkWriteAccess(body, element, resourceFilePath, entryPoint));
					}
				}
			}
		}
		return initialOperations;
	}

	/**
	 *
	 * Checks for WRITE operations on process variables
	 *
	 * @param body
	 *            Soot representation of a method's body
	 * @param element
	 *            BpmnElement
	 * @param resourceFilePath
	 *            Path of the BPMN model
	 * @return Map of process variable operations
	 */
	private ListMultimap<String, ProcessVariableOperation> checkWriteAccess(final Body body, final BpmnElement element,
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
	 *
	 * Check whether or not the second or third argument contain a reference to the
	 * variable map
	 *
	 * @param entry
	 *            Current entry
	 * @param assignment
	 *            Current assigned variable
	 * @param invoke
	 *            Current invocation
	 * @param expr
	 *            Current expression
	 * @return True/False based on whether the second or third argument refers to
	 *         the variable map
	 */
	private boolean checkArgBoxes(final EntryPoint entry, final String assignment, final String invoke,
			final JInterfaceInvokeExpr expr, final BpmnElement element) {
		if (expr.getMethodRef().getName().equals(entry.getEntryPoint())) {
			if (!assignment.isEmpty()) {
				if (element.getBaseElement().getElementType().getTypeName().equals(BpmnConstants.RECEIVE_TASK)) {
					String message = expr.getArgBox(0).getValue().toString().replaceAll("\"", "");
					if (message.equals(entry.getMessageName())) {
						return true;
					}
				} else {
					if (expr.getArgBox(1).getValue().toString().equals(invoke)) {
						return true;
					}
					if (expr.getArgBox(2).getValue().toString().equals(invoke)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 *
	 * Starting by the main JavaDelegate, statically analyses the classes
	 * implemented for the bpmn element.
	 *
	 * @param classPaths
	 *            Set of classes that is included in inter-procedural analysis
	 * @param className
	 *            Name of currently analysed class
	 * @param methodName
	 *            Name of currently analysed method
	 * @param classFile
	 *            Location path of class
	 * @param element
	 *            Bpmn element
	 * @param chapter
	 *            ElementChapter
	 * @param fieldType
	 *            KnownElementFieldType
	 * @param scopeId
	 *            Scope of the element
	 * @return Map of process variables for a given class
	 */
	public ListMultimap<String, ProcessVariableOperation> classFetcher(final Set<String> classPaths,
			final String className, final String methodName, final String classFile, final BpmnElement element,
			final ElementChapter chapter, final KnownElementFieldType fieldType, final String scopeId,
			final FlowGraph flowGraph) {

		ListMultimap<String, ProcessVariableOperation> processVariables = ArrayListMultimap.create();

		OutSetCFG outSet = new OutSetCFG(new ArrayList<>());

		List<Value> args = new ArrayList<>();

		classFetcherRecursive(classPaths, className, methodName, classFile, element, chapter, fieldType, scopeId,
				outSet, null, "", args, flowGraph);

		if (outSet.getAllProcessVariables().size() > 0) {
			processVariables.putAll(outSet.getAllProcessVariables());
			addAnomaliesFoundInSourceCode(element, outSet);
		}

		return processVariables;

	}

	/**
	 * Recursively follow call hierarchy and obtain method bodies
	 *
	 * @param classPaths
	 *            Set of classes that is included in inter-procedural analysis
	 * @param className
	 *            Name of currently analysed class
	 * @param methodName
	 *            Name of currently analysed method
	 * @param classFile
	 *            Location path of class
	 * @param element
	 *            Bpmn element
	 * @param chapter
	 *            ElementChapter
	 * @param fieldType
	 *            KnownElementFieldType
	 * @param scopeId
	 *            Scope of the element
	 * @param outSet
	 *            Callgraph information
	 * @param originalBlock
	 *            VariableBlock
	 * @param assignmentStmt
	 *            Assignment statement (left side)
	 * @param args
	 *            List of arguments
	 * @return OutSetCFG which contains data flow information
	 */
	public OutSetCFG classFetcherRecursive(final Set<String> classPaths, String className, final String methodName,
			final String classFile, final BpmnElement element, final ElementChapter chapter,
			final KnownElementFieldType fieldType, final String scopeId, OutSetCFG outSet,
			final VariableBlock originalBlock, final String assignmentStmt, final List<Value> args,
			final FlowGraph flowGraph) {

		className = cleanString(className, true);

		Options.v().set_whole_program(true);
		Options.v().set_allow_phantom_refs(true);

		SootClass sootClass = Scene.v().forceResolve(className, SootClass.SIGNATURES);

		if (sootClass != null) {

			sootClass.setApplicationClass();
			Scene.v().loadNecessaryClasses();

			// Retrieve the method and its body based on the used interface
			List<Type> parameterTypes = new ArrayList<>();
			RefType delegateExecutionType = RefType.v("org.camunda.bpm.engine.delegate.DelegateExecution");
			RefType delegateTaskType = RefType.v("org.camunda.bpm.engine.delegate.DelegateTask");
			RefType mapVariablesType = RefType.v("org.camunda.bpm.engine.variable.VariableMap");
			VoidType returnType = VoidType.v();

			switch (methodName) {
			case "execute":
				parameterTypes.add(delegateExecutionType);
				outSet = retrieveMethod(classPaths, methodName, classFile, element, chapter, fieldType, scopeId, outSet,
						originalBlock, sootClass, parameterTypes, returnType, assignmentStmt, args, flowGraph);
				break;
			case "notify":
				for (SootClass clazz : sootClass.getInterfaces()) {
					if (clazz.getName().equals("org.camunda.bpm.engine.delegate.TaskListener")) {
						parameterTypes.add(delegateTaskType);
					} else if (clazz.getName().equals("org.camunda.bpm.engine.delegate.ExecutionListener")) {
						parameterTypes.add(delegateExecutionType);
					}
				}
				outSet = retrieveMethod(classPaths, methodName, classFile, element, chapter, fieldType, scopeId, outSet,
						originalBlock, sootClass, parameterTypes, returnType, assignmentStmt, args, flowGraph);
				break;
			case "mapInputVariables":
				parameterTypes.add(delegateExecutionType);
				parameterTypes.add(mapVariablesType);
				outSet = retrieveMethod(classPaths, methodName, classFile, element, chapter, fieldType, scopeId, outSet,
						originalBlock, sootClass, parameterTypes, returnType, assignmentStmt, args, flowGraph);
				break;
			case "mapOutputVariables":
				parameterTypes.add(delegateExecutionType);
				parameterTypes.add(mapVariablesType);
				outSet = retrieveMethod(classPaths, methodName, classFile, element, chapter, fieldType, scopeId, outSet,
						originalBlock, sootClass, parameterTypes, returnType, assignmentStmt, args, flowGraph);
				break;
			default:
				outSet = retrieveCustomMethod(sootClass, classPaths, methodName, classFile, element, chapter, fieldType,
						scopeId, outSet, originalBlock, assignmentStmt, args, flowGraph);
				break;
			}

		} else {
			LOGGER.warning("Class " + classFile + " was not found by Soot");
		}

		return outSet;

	}

	/**
	 *
	 * Retrieve given camunda methods to obtain a Soot representation of said method
	 * to analyse its body
	 *
	 * @param classPaths
	 *            Set of classes that is included in inter-procedural analysis
	 * @param methodName
	 *            Name of currently analysed method
	 * @param classFile
	 *            Location path of class
	 * @param element
	 *            Bpmn element
	 * @param chapter
	 *            ElementChapter
	 * @param fieldType
	 *            KnownElementFieldType
	 * @param scopeId
	 *            Scope of the element
	 * @param outSet
	 *            Callgraph information
	 * @param originalBlock
	 *            VariableBlock
	 * @param sootClass
	 *            Soot representation of given class
	 * @param parameterTypes
	 *            Soot representation of parameters
	 * @param returnType
	 *            Soot Representation of return type
	 * @return OutSetCFG which contains data flow information
	 */
	private OutSetCFG retrieveMethod(final Set<String> classPaths, final String methodName, final String classFile,
			final BpmnElement element, final ElementChapter chapter, final KnownElementFieldType fieldType,
			final String scopeId, OutSetCFG outSet, final VariableBlock originalBlock, final SootClass sootClass,
			final List<Type> parameterTypes, final VoidType returnType, final String assignmentStmt,
			final List<Value> args, final FlowGraph flowGraph) {

		SootMethod method = sootClass.getMethodUnsafe(methodName, parameterTypes, returnType);

		if (method != null) {
			outSet = fetchMethodBody(classPaths, classFile, element, chapter, fieldType, scopeId, outSet, originalBlock,
					method, assignmentStmt, args, flowGraph);
		} else {
			method = sootClass.getMethodByNameUnsafe(methodName);
			if (method != null) {
				outSet = fetchMethodBody(classPaths, classFile, element, chapter, fieldType, scopeId, outSet,
						originalBlock, method, assignmentStmt, args, flowGraph);
			} else {
				LOGGER.warning("In class " + classFile + " - " + methodName + " method was not found by Soot");
			}
		}
		return outSet;
	}

	/**
	 *
	 * Retrieve given custom methods to obtain a Soot representation of said method
	 * to analyse its body
	 *
	 * @param classPaths
	 *            Set of classes that is included in inter-procedural analysis
	 * @param methodName
	 *            Name of currently analysed method
	 * @param classFile
	 *            Location path of class
	 * @param element
	 *            Bpmn element
	 * @param chapter
	 *            ElementChapter
	 * @param fieldType
	 *            KnownElementFieldType
	 * @param scopeId
	 *            Scope of the element
	 * @param outSet
	 *            Callgraph information
	 * @param originalBlock
	 *            VariableBlock
	 * @param sootClass
	 *            Soot representation of given class
	 * @return OutSetCFG which contains data flow information
	 */
	private OutSetCFG retrieveCustomMethod(final SootClass sootClass, final Set<String> classPaths,
			final String methodName, final String classFile, final BpmnElement element, final ElementChapter chapter,
			final KnownElementFieldType fieldType, final String scopeId, OutSetCFG outSet,
			final VariableBlock originalBlock, final String assignmentStmt, final List<Value> args,
			final FlowGraph flowGraph) {

		for (SootMethod method : sootClass.getMethods()) {
			if (method.getName().equals(methodName)) {
				outSet = fetchMethodBody(classPaths, classFile, element, chapter, fieldType, scopeId, outSet,
						originalBlock, method, assignmentStmt, args, flowGraph);
			}
		}
		return outSet;
	}

	/**
	 *
	 * Retrieve given custom methods to obtain a Soot representation of said method
	 * to analyse its body
	 *
	 * @param classPaths
	 *            Set of classes that is included in inter-procedural analysis
	 * @param classFile
	 *            Location path of class
	 * @param element
	 *            Bpmn element
	 * @param chapter
	 *            ElementChapter
	 * @param fieldType
	 *            KnownElementFieldType
	 * @param scopeId
	 *            Scope of the element
	 * @param outSet
	 *            Callgraph information
	 * @param originalBlock
	 *            VariableBlock
	 * @param method
	 *            Soot representation of a given method
	 * @return OutSetCFG which contains data flow information
	 */
	private OutSetCFG fetchMethodBody(final Set<String> classPaths, final String classFile, final BpmnElement element,
			final ElementChapter chapter, final KnownElementFieldType fieldType, final String scopeId, OutSetCFG outSet,
			final VariableBlock originalBlock, final SootMethod method, final String assignmentStmt,
			final List<Value> args, final FlowGraph flowGraph) {

		final Body body = method.retrieveActiveBody();

		BlockGraph graph = new ClassicCompleteBlockGraph(body);
		// Prepare call graph for inter-procedural recursive call
		List<SootMethod> entryPoints = new ArrayList<>();
		entryPoints.add(method);
		Scene.v().setEntryPoints(entryPoints);

		PackManager.v().getPack("cg").apply();
		CallGraph cg = Scene.v().getCallGraph();

		final List<Block> graphHeads = graph.getHeads();

		for (int i = 0; i < graphHeads.size(); i++) {
			outSet = graphIterator(classPaths, cg, graph, outSet, element, chapter, fieldType, classFile, scopeId,
					originalBlock, assignmentStmt, args, flowGraph);
		}

		return outSet;
	}

	/**
	 * Iterate through the control-flow graph with an iterative data-flow analysis
	 * logic
	 *
	 * @param classPaths
	 *            Set of classes that is included in inter-procedural analysis
	 * @param cg
	 *            Soot FlowGraph
	 * @param graph
	 *            Control Flow graph of method
	 * @param outSet
	 *            OUT set of CFG
	 * @param element
	 *            Bpmn element
	 * @param chapter
	 *            ElementChapter
	 * @param fieldType
	 *            KnownElementFieldType
	 * @param filePath
	 *            ResourceFilePath for ProcessVariableOperation
	 * @param scopeId
	 *            Scope of BpmnElement
	 * @param originalBlock
	 *            VariableBlock
	 *
	 * @return OutSetCFG which contains data flow information
	 */
	private OutSetCFG graphIterator(final Set<String> classPaths, final CallGraph cg, final BlockGraph graph,
			OutSetCFG outSet, final BpmnElement element, final ElementChapter chapter,
			final KnownElementFieldType fieldType, final String filePath, final String scopeId,
			VariableBlock originalBlock, final String assignmentStmt, final List<Value> args,
			final FlowGraph flowGraph) {

		for (Block block : graph.getBlocks()) {
			Node node = new Node(flowGraph, block);
			flowGraph.addNode(node);

			// Collect the functions Unit by Unit via the blockIterator
			final VariableBlock vb = blockIterator(classPaths, cg, block, outSet, element, chapter, fieldType, filePath,
					scopeId, originalBlock, assignmentStmt, args, flowGraph, node);

			// depending if outset already has that Block, only add variables,
			// if not, then add the whole vb

			if (outSet.getVariableBlock(vb.getBlock()) == null) {
				outSet.addVariableBlock(vb);
			}
		}

		return outSet;
	}

	/**
	 * Iterator through the source code line by line, collecting the
	 * ProcessVariables Camunda methods are interface invocations appearing either
	 * in Assign statement or Invoke statement Constraint: Only String constants can
	 * be precisely recognized.
	 *
	 * @param classPaths
	 *            Set of classes that is included in inter-procedural analysis
	 * @param cg
	 *            Soot FlowGraph
	 * @param block
	 *            Block from CFG
	 * @param outSet
	 *            OUT set of CFG
	 * @param element
	 *            BpmnElement
	 * @param chapter
	 *            ElementChapter
	 * @param fieldType
	 *            KnownElementFieldType
	 * @param filePath
	 *            ResourceFilePath for ProcessVariableOperation
	 * @param scopeId
	 *            Scope of BpmnElement
	 * @param variableBlock
	 *            VariableBlock
	 * @return VariableBlock
	 */
	private VariableBlock blockIterator(final Set<String> classPaths, final CallGraph cg, final Block block,
			final OutSetCFG outSet, final BpmnElement element, final ElementChapter chapter,
			final KnownElementFieldType fieldType, final String filePath, final String scopeId,
			VariableBlock variableBlock, String assignmentStmt, final List<Value> args, final FlowGraph flowGraph,
			final Node node) {
		if (variableBlock == null) {
			variableBlock = new VariableBlock(block, new ArrayList<>());
		}

		String paramName = "";
		int argsCounter = 0;
		int instanceFieldRef = Integer.MAX_VALUE;

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
				assignmentStmt = processInvokeStmt(classPaths, cg, outSet, element, chapter, fieldType, filePath,
						scopeId, variableBlock, assignmentStmt, flowGraph, node, paramName, argsCounter, unit);
			}
			if (unit instanceof AssignStmt) {
				// Method call with assignment to a variable
				if (((AssignStmt) unit).getRightOpBox().getValue() instanceof JVirtualInvokeExpr) {
					assignmentStmt = ((AssignStmt) unit).getLeftOpBox().getValue().toString();
					JVirtualInvokeExpr expr = (JVirtualInvokeExpr) ((AssignStmt) unit).getRightOpBox().getValue();
					checkInterProceduralCall(classPaths, cg, outSet, element, chapter, fieldType, scopeId,
							variableBlock, unit, assignmentStmt, expr.getArgs(), flowGraph);
					paramName = this.getReturnStmt();
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
							if (this.getConstructorArgs() != null) {
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

		return variableBlock;
	}

	/**
	 * Iterator through the source code line by line, collecting the
	 * ProcessVariables Camunda methods are interface invocations appearing either
	 * in Assign statement or Invoke statement Constraint: Only String constants can
	 * be precisely recognized.
	 *
	 * @param classPaths
	 *            Set of classes that is included in inter-procedural analysis
	 * @param cg
	 *            Soot FlowGraph
	 * @param outSet
	 *            OUT set of CFG
	 * @param element
	 *            BpmnElement
	 * @param chapter
	 *            ElementChapter
	 * @param fieldType
	 *            KnownElementFieldType
	 * @param filePath
	 *            ResourceFilePath for ProcessVariableOperation
	 * @param scopeId
	 *            Scope of BpmnElement
	 * @param variableBlock
	 *            VariableBlock
	 * @param flowGraph
	 *            Control Flow Graph
	 * @param node
	 *            Current node of the CFG
	 * @param paramName
	 *            Name of the parameter
	 * @param argsCounter
	 *            Counts the arguments in case of a method or constructor call
	 * @param unit
	 *            Current unit
	 * @return assignmentStmt
	 */
	private String processInvokeStmt(final Set<String> classPaths, final CallGraph cg, final OutSetCFG outSet,
			final BpmnElement element, final ElementChapter chapter, final KnownElementFieldType fieldType,
			final String filePath, final String scopeId, final VariableBlock variableBlock, String assignmentStmt,
			final FlowGraph flowGraph, final Node node, final String paramName, final int argsCounter,
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
					assignmentStmt, expr.getArgs(), flowGraph);
		}
		// Constructor call
		if (((InvokeStmt) unit).getInvokeExprBox().getValue() instanceof JSpecialInvokeExpr) {
			JSpecialInvokeExpr expr = (JSpecialInvokeExpr) ((InvokeStmt) unit).getInvokeExprBox().getValue();
			if (((InvokeStmt) unit).getInvokeExprBox().getValue().toString().contains("void <init>")) {
				this.setConstructorArgs(expr.getArgs());
				assignmentStmt = expr.getBaseBox().getValue().toString();
			} else {
				checkInterProceduralCall(classPaths, cg, outSet, element, chapter, fieldType, scopeId, variableBlock,
						unit, assignmentStmt, expr.getArgs(), flowGraph);
			}
		}
		return assignmentStmt;
	}

	/**
	 * @param classPaths
	 *            Set of classes that is included in inter-procedural analysis
	 * @param cg
	 *            Soot FlowGraph
	 * @param outSet
	 *            OUT set of CFG
	 * @param element
	 *            BpmnElement
	 * @param chapter
	 *            ElementChapter
	 * @param fieldType
	 *            KnownElementFieldType
	 * @param scopeId
	 *            Scope of BpmnElement
	 * @param variableBlock
	 *            VariableBlock
	 * @param unit
	 *            Current unit of code
	 * @param assignmentStmt
	 *            Left side of assignment statement
	 * @param args
	 *            List of arguments
	 */
	private void checkInterProceduralCall(final Set<String> classPaths, final CallGraph cg, final OutSetCFG outSet,
			final BpmnElement element, final ElementChapter chapter, final KnownElementFieldType fieldType,
			final String scopeId, final VariableBlock variableBlock, final Unit unit, final String assignmentStmt,
			final List<Value> args, final FlowGraph flowGraph) {

		final Iterator<Edge> sources = cg.edgesOutOf(unit);
		Edge src;
		while (sources.hasNext()) {
			src = sources.next();
			String methodName = src.tgt().getName();
			String className = src.tgt().getDeclaringClass().getName();
			className = cleanString(className, false);
			if (classPaths.contains(className) || className.contains("$")) {
				flowGraph.incrementRecursionCounter();
				flowGraph.resetInternalNodeCounter();
				flowGraph.setPriorLevel(flowGraph.getInternalNodeCounter());
				G.reset();
				classFetcherRecursive(classPaths, className, methodName, className, element, chapter, fieldType,
						scopeId, outSet, variableBlock, assignmentStmt, args, flowGraph);
				flowGraph.decrementRecursionCounter();
			}
		}

	}

	/**
	 *
	 * Special parsing of statements to find Process Variable operations.
	 *
	 * @param expr
	 *            Expression Unit from Statement
	 * @param variableBlock
	 *            current VariableBlock
	 * @param element
	 *            BpmnElement
	 * @param chapter
	 *            ElementChapter
	 * @param fieldType
	 *            KnownElementFieldType
	 * @param filePath
	 *            ResourceFilePath for ProcessVariableOperation
	 * @param scopeId
	 *            Scope of BpmnElement
	 */
	private void parseExpression(final JInterfaceInvokeExpr expr, final VariableBlock variableBlock,
			final BpmnElement element, final ElementChapter chapter, final KnownElementFieldType fieldType,
			final String filePath, final String scopeId, final String paramName, final Node node) {

		String functionName = expr.getMethodRef().getName();
		int numberOfArg = expr.getArgCount();
		String baseBox = expr.getBaseBox().getValue().getType().toString();

		CamundaProcessVariableFunctions foundMethod = CamundaProcessVariableFunctions
				.findByNameAndNumberOfBoxes(functionName, baseBox, numberOfArg);

		if (foundMethod != null) {
			int location = foundMethod.getLocation() - 1;
			VariableOperation type = foundMethod.getOperationType();
			if (expr.getArgBox(location).getValue() instanceof StringConstant) {
				StringConstant variableName = (StringConstant) expr.getArgBox(location).getValue();
				String name = variableName.value.replaceAll("\"", "");
				node.addOperation(
						new ProcessVariableOperation(name, element, chapter, fieldType, filePath, type, scopeId));
				variableBlock.addProcessVariable(
						new ProcessVariableOperation(name, element, chapter, fieldType, filePath, type, scopeId));

			} else if (!paramName.isEmpty()) {
				node.addOperation(new ProcessVariableOperation(paramName.replaceAll("\"", ""), element, chapter,
						fieldType, filePath, type, scopeId));
				variableBlock.addProcessVariable(new ProcessVariableOperation(paramName.replaceAll("\"", ""), element,
						chapter, fieldType, filePath, type, scopeId));
			} else {
				// TODO: Warnmeldung mit PV operation
			}
		}
	}

	/**
	 *
	 * Parsing of initially discovered statements to find Process Variable
	 * operations.
	 *
	 * @param expr
	 *            Expression Unit from Statement
	 * @param element
	 *            Current BPMN Element
	 * @param resourceFilePath
	 *            Filepath of model
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
								element.getBaseElement().getScope().getAttributeValue(BpmnConstants.ATTR_ID)));
			}
		}
		return initialOperations;
	}

	/**
	 *
	 * Find anomalies inside a Bpmn element as well.
	 *
	 * @param element
	 *            BpmnElement
	 * @param outSet
	 *            OUT set of CFG
	 */
	private void addAnomaliesFoundInSourceCode(final BpmnElement element, final OutSetCFG outSet) {
		for (VariableBlock vb : outSet.getAllVariableBlocks()) {
			if (vb.getBlock().getIndexInMethod() == 0) {
				addAnomaliesFoundInPathsRecursive(element, vb.getBlock(), new LinkedList<>(), outSet,
						new LinkedList<>(), "");
			}
		}
	}

	/**
	 *
	 * Build LinkedList of ProcessVariableOperations recursively based on CFG paths.
	 *
	 * @param element
	 *            BpmnElement
	 * @param currentBlock
	 *            Block from CFG
	 * @param currentPath
	 *            List of so far visited Blocks
	 * @param outSet
	 *            OUT set of CFG
	 * @param predecessorVariablesList
	 *            Chain of Process Variables along the path
	 * @param edge
	 *            Current edge between two Blocks
	 */
	private void addAnomaliesFoundInPathsRecursive(final BpmnElement element, final Block currentBlock,
			final LinkedList<String> currentPath, final OutSetCFG outSet,
			final LinkedList<ProcessVariableOperation> predecessorVariablesList, final String edge) {

		// List<AnomalyContainer> foundAnomalies = new ArrayList<AnomalyContainer>();
		if (edge != null && !edge.equals("")) {
			currentPath.add(edge);
		}

		// get the VariableBlock
		VariableBlock variableBlock = outSet.getVariableBlock(currentBlock);

		// set IN + this Variables as OUT
		LinkedList<ProcessVariableOperation> usedVariables = new LinkedList<>();
		usedVariables.addAll(variableBlock.getAllProcessVariables());

		// prepare for statement by statement comparison
		Set<ProcessVariableOperation> pvSet = new HashSet<>(usedVariables);
		for (ProcessVariableOperation pV : pvSet) {
			// create unique lists for operations on same variable
			LinkedList<ProcessVariableOperation> innerList = new LinkedList<>();
			for (ProcessVariableOperation variable : usedVariables) {
				if (pV.getName().equals(variable.getName())) {
					innerList.add(variable);
				}
			}
			checkStatementByStatement(element, innerList);
		}

		// Based on last appearance of Variable decide on UR, DD, DU anomaly
		for (ProcessVariableOperation variable : usedVariables) {
			if (predecessorVariablesList.lastIndexOf(variable) >= 0) {
				ProcessVariableOperation lastAppearance = predecessorVariablesList
						.get(predecessorVariablesList.lastIndexOf(variable));
				checkAnomaly(element, variable, lastAppearance);
			}
		}

		// Prepare new chain of Variables including this Block's variables for
		// successors
		predecessorVariablesList.addAll(usedVariables);

		List<Block> successors = currentBlock.getSuccs();
		for (Block successor : successors) {
			String newEdge = currentBlock.toShortString() + successor.toShortString();
			int occurrence = Collections.frequency(currentPath, newEdge);
			if (occurrence < 2) {
				addAnomaliesFoundInPathsRecursive(element, successor, currentPath, outSet, predecessorVariablesList,
						newEdge);
			}
		}

		if (!currentPath.isEmpty()) {
			currentPath.removeLast();
		}

		for (ProcessVariableOperation pv : variableBlock.getAllProcessVariables()) {
			predecessorVariablesList.removeLastOccurrence(pv);
		}

	}

	/**
	 * Checks for anomalies occurring between elements
	 *
	 * @param element
	 *            Current BpmnElement
	 * @param innerList
	 *            List of all variable operations (same name)
	 */
	private void checkStatementByStatement(final BpmnElement element,
			final LinkedList<ProcessVariableOperation> innerList) {

		if (innerList.size() >= 2) {
			ProcessVariableOperation prev = null;
			for (ProcessVariableOperation curr : innerList) {
				if (prev == null) {
					prev = curr;
					continue;
				}
				checkAnomaly(element, curr, prev);
			}
		}

	}

	/**
	 * Check for dataflow anomaly between current and previous variable operation
	 *
	 * @param element
	 *            Current BpmnElement
	 * @param curr
	 *            current operation
	 * @param last
	 *            previous operation
	 */
	private void checkAnomaly(final BpmnElement element, ProcessVariableOperation curr, ProcessVariableOperation last) {
		if (urSourceCode(last, curr)) {
			element.addSourceCodeAnomaly(
					new AnomalyContainer(curr.getName(), Anomaly.UR, element.getBaseElement().getId(), curr));
		}

		if (ddSourceCode(last, curr)) {
			element.addSourceCodeAnomaly(
					new AnomalyContainer(curr.getName(), Anomaly.DD, element.getBaseElement().getId(), curr));
		}

		if (duSourceCode(last, curr)) {
			element.addSourceCodeAnomaly(
					new AnomalyContainer(curr.getName(), Anomaly.DU, element.getBaseElement().getId(), curr));
		}
	}

	/**
	 * UR anomaly: second last operation of PV is DELETE, last operation is READ
	 *
	 * @param prev
	 *            Previous ProcessVariable
	 * @param curr
	 *            Current ProcessVariable
	 * @return true/false
	 */
	private boolean urSourceCode(final ProcessVariableOperation prev, final ProcessVariableOperation curr) {
		return curr.getOperation().equals(VariableOperation.READ)
				&& prev.getOperation().equals(VariableOperation.DELETE);
	}

	/**
	 * DD anomaly: second last operation of PV is DEFINE, last operation is DELETE
	 *
	 * @param prev
	 *            Previous ProcessVariable
	 * @param curr
	 *            Current ProcessVariable
	 * @return true/false
	 */
	private boolean ddSourceCode(final ProcessVariableOperation prev, final ProcessVariableOperation curr) {
		return curr.getOperation().equals(VariableOperation.WRITE)
				&& prev.getOperation().equals(VariableOperation.WRITE);
	}

	/**
	 * DU anomaly: second last operation of PV is DEFINE, last operation is DELETE
	 *
	 * @param prev
	 *            Previous ProcessVariable
	 * @param curr
	 *            Current ProcessVariable
	 * @return true/false
	 */
	private boolean duSourceCode(final ProcessVariableOperation prev, final ProcessVariableOperation curr) {
		return curr.getOperation().equals(VariableOperation.DELETE)
				&& prev.getOperation().equals(VariableOperation.WRITE);
	}

	/**
	 * Strips unnecessary characters and returns cleaned name
	 *
	 * @param className
	 *            Classname to be stripped of unused chars
	 * @return cleaned String
	 */
	private String cleanString(String className, boolean dot) {
		className = ProcessVariablesScanner.cleanString(className, dot);
		return className;
	}

	private String getReturnStmt() {
		return returnStmt;
	}

	private void setReturnStmt(final String returnStmt) {
		this.returnStmt = returnStmt;
	}

	private List<Value> getConstructorArgs() {
		return constructorArgs;

	}

	private void setConstructorArgs(final List<Value> args) {
		this.constructorArgs = args;
	}

}
