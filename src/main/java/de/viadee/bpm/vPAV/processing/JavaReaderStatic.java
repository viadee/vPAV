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
import de.viadee.bpm.vPAV.processing.code.flow.*;
import de.viadee.bpm.vPAV.processing.model.data.*;
import org.camunda.bpm.model.bpmn.impl.BpmnModelConstants;
import soot.*;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.options.Options;
import soot.toolkits.graph.Block;
import soot.toolkits.graph.BlockGraph;
import soot.toolkits.graph.ClassicCompleteBlockGraph;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

public class JavaReaderStatic {

    private static final Logger LOGGER = Logger.getLogger(JavaReaderStatic.class.getName());

    private VariablesExtractor variablesExtractor;

    public JavaReaderStatic() {
        this.setupSoot();
        variablesExtractor = new VariablesExtractor(this);
    }

    /**
     * Checks a java delegate for process variable references with static code
     * analysis (read/write/delete).
     * <p>
     * Constraints: names, which only could be determined at runtime, can't be
     * analyzed. e.g. execution.setVariable(execution.getActivityId() + "-" +
     * execution.getEventName(), true)
     *
     * @param fileScanner      FileScanner
     * @param classFile        Name of the class
     * @param element          Bpmn element
     * @param chapter          ElementChapter
     * @param fieldType        KnownElementFieldType
     * @param scopeId          Scope of the element
     * @return Map of process variables from the referenced delegate
     */
    ListMultimap<String, ProcessVariableOperation> getVariablesFromJavaDelegate(final FileScanner fileScanner,
            final String classFile, final BpmnElement element, final ElementChapter chapter,
            final KnownElementFieldType fieldType, final String scopeId,  AnalysisElement[] predecessor) {

        final ListMultimap<String, ProcessVariableOperation> variables = ArrayListMultimap.create();

        if (classFile != null && classFile.trim().length() > 0) {

            final String sootPath = FileScanner.getSootPath();

            System.setProperty("soot.class.path", sootPath);

            final Set<String> classPaths = fileScanner.getJavaResourcesFileInputStream();

            if (element.getBaseElement().getAttributeValueNs(BpmnModelConstants.CAMUNDA_NS,
                    BpmnConstants.ATTR_VAR_MAPPING_CLASS) != null
                    || element.getBaseElement().getAttributeValueNs(BpmnModelConstants.CAMUNDA_NS,
                    BpmnConstants.ATTR_VAR_MAPPING_DELEGATE) != null) {
                // Delegate Variable Mapping
                variables.putAll(classFetcher(classPaths, classFile, "mapInputVariables", classFile, element,
                        ElementChapter.InputImplementation, fieldType, scopeId, predecessor));
                variables.putAll(classFetcher(classPaths, classFile, "mapOutputVariables", classFile, element,
                        ElementChapter.OutputImplementation, fieldType, scopeId, predecessor));
            } else {
                // Java Delegate or Listener
                SootClass sootClass = Scene.v().forceResolve(cleanString(classFile, true), SootClass.SIGNATURES);
                if (sootClass.declaresMethodByName("notify")) {
                    variables.putAll(classFetcher(classPaths, classFile, "notify", classFile, element, chapter,
                            fieldType,
                            scopeId, predecessor));
                } else if (sootClass.declaresMethodByName("execute")) {
                    variables.putAll(classFetcher(classPaths, classFile, "execute", classFile, element, chapter,
                            fieldType,
                            scopeId, predecessor));
                } else {
                    LOGGER.warning("No supported (execute/notify) method in " + classFile + " found.");
                }
            }
        }
        return variables;
    }

    /**
     * Retrieves variables from a class
     *
     * @param className        Name of the class that potentially declares process variables
     * @param element          BpmnElement
     * @param resourceFilePath Path of the BPMN model
     * @param entryPoint       Current entry point
     * @return Map of process variable operations
     */
    ListMultimap<String, ProcessVariableOperation> getVariablesFromClass(String className, final BpmnElement element,
            final String resourceFilePath, final EntryPoint entryPoint) {

        final ListMultimap<String, ProcessVariableOperation> initialOperations = ArrayListMultimap.create();

        if (className != null && className.trim().length() > 0) {
            className = cleanString(className, true);
            SootClass sootClass = Scene.v().forceResolve(className, SootClass.SIGNATURES);

            if (sootClass != null) {
                sootClass.setApplicationClass();
                Scene.v().loadNecessaryClasses();
                for (SootMethod method : sootClass.getMethods()) {
                    if (method.getName().equals(entryPoint.getMethodName())) {
                        final Body body = method.retrieveActiveBody();
                        initialOperations.putAll(variablesExtractor
                                .checkWriteAccess(body, element, resourceFilePath, entryPoint));
                    }
                }
            }
        }
        return initialOperations;
    }

    /**
     * Starting by the main JavaDelegate, statically analyses the classes
     * implemented for the bpmn element.
     *
     * @param classPaths       Set of classes that is included in inter-procedural analysis
     * @param className        Name of currently analysed class
     * @param methodName       Name of currently analysed method
     * @param classFile        Location path of class
     * @param element          Bpmn element
     * @param chapter          ElementChapter
     * @param fieldType        KnownElementFieldType
     * @param scopeId          Scope of the element
     * @return Map of process variables for a given class
     */
    public ListMultimap<String, ProcessVariableOperation> classFetcher(final Set<String> classPaths,
            final String className, final String methodName, final String classFile, final BpmnElement element,
            final ElementChapter chapter, final KnownElementFieldType fieldType, final String scopeId,
            AnalysisElement[] predecessor) {

        ListMultimap<String, ProcessVariableOperation> processVariables = ArrayListMultimap.create();

        OutSetCFG outSet = new OutSetCFG(new ArrayList<>());

        List<Value> args = new ArrayList<>();

        variablesExtractor.resetMethodStackTrace();
        classFetcherRecursive(classPaths, className, methodName, classFile, element, chapter, fieldType, scopeId,
                outSet, null, "", args,null, predecessor);

        if (outSet.getAllProcessVariables().size() > 0) {
            processVariables.putAll(outSet.getAllProcessVariables());
        }

        return processVariables;
    }

    private SootClass setupSootClass(String className) {
        className = cleanString(className, true);
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

    private List<Type> prepareSootAndFetchedObjects(final String methodName,
            final SootClass sootClass) {
        List<Type> parameterTypes = new ArrayList<>();

        // Retrieve the method and its body based on the used interface
        RefType delegateExecutionType = RefType.v("org.camunda.bpm.engine.delegate.DelegateExecution");
        RefType activityExecutionType = RefType.v("org.camunda.bpm.engine.impl.pvm.delegate.ActivityExecution");
        RefType delegateTaskType = RefType.v("org.camunda.bpm.engine.delegate.DelegateTask");
        RefType mapVariablesType = RefType.v("org.camunda.bpm.engine.variable.VariableMap");

        switch (methodName) {
            case "execute":
                for (SootClass clazz : sootClass.getInterfaces()) {
                    if (clazz.getName()
                            .equals("org.camunda.bpm.engine.impl.bpmn.behavior.AbstractBpmnActivityBehavior")) {
                        parameterTypes.add(activityExecutionType);
                    } else if (clazz.getName().equals("org.camunda.bpm.engine.delegate.JavaDelegate")) {
                        parameterTypes.add(delegateExecutionType);
                    }
                }
                break;
            case "notify":
                for (SootClass clazz : sootClass.getInterfaces()) {
                    if (clazz.getName().equals("org.camunda.bpm.engine.delegate.TaskListener")) {
                        parameterTypes.add(delegateTaskType);
                    } else if (clazz.getName().equals("org.camunda.bpm.engine.delegate.ExecutionListener")) {
                        parameterTypes.add(delegateExecutionType);
                    }
                }
                break;
            case "mapInputVariables":
            case "mapOutputVariables":
                parameterTypes.add(delegateExecutionType);
                parameterTypes.add(mapVariablesType);
                break;
        }

        return parameterTypes;
    }

    /**
     * Recursively follow call hierarchy and obtain method bodies
     *
     * @param classPaths       Set of classes that is included in inter-procedural analysis
     * @param className        Name of currently analysed class
     * @param methodName       Name of currently analysed method
     * @param classFile        Location path of class
     * @param element          Bpmn element
     * @param chapter          ElementChapter
     * @param fieldType        KnownElementFieldType
     * @param scopeId          Scope of the element
     * @param outSet           Callgraph information
     * @param originalBlock    VariableBlock
     * @param assignmentStmt   Assignment statement (left side)
     * @param args             List of arguments
     */
    void classFetcherRecursive(final Set<String> classPaths, String className, final String methodName,
            final String classFile, final BpmnElement element, final ElementChapter chapter,
            final KnownElementFieldType fieldType, final String scopeId, OutSetCFG outSet,
            final VariableBlock originalBlock, final String assignmentStmt, final List<Value> args,
            SootMethod sootMethod, final AnalysisElement[] predecessor) {

        SootClass sootClass = setupSootClass(className);

        if (sootClass != null) {
            List<Type> parameterTypes = prepareSootAndFetchedObjects(methodName, sootClass);
            List<SootMethod> toFetchedMethods = new ArrayList<>();
            if (parameterTypes.size() > 0) {
                // Replace retrieveCustomMethod()
                if (sootMethod == null) {
                    toFetchedMethods = sootClass.getMethods();
                } else {
                    toFetchedMethods.add(sootMethod);
                }
            } else {
                // Replace retrieveMethod()
                sootMethod = getSootMethod(sootClass, methodName, parameterTypes, VoidType.v());
                toFetchedMethods.add(sootMethod);
            }
            for (SootMethod method : toFetchedMethods) {
                if (method != null) {
                    if (method.getName().equals(methodName)) {

                        // check if method is recursive and was already two times called
                        if (!variablesExtractor.visitMethod(method)) {
                            return;
                        }

                        // Replace fetchMethodBody
                        BlockGraph graph = getBlockGraph(method);
                        List<Block> graphHeads = graph.getHeads();

                        for(Block block: graphHeads) {
                            outSet = blockIterator(classPaths, Scene.v().getCallGraph(), graph, block, outSet, element,
                                    chapter,
                                    fieldType, classFile, scopeId,
                                    originalBlock, assignmentStmt, args, predecessor);
                        }
                        variablesExtractor.leaveMethod(method);
                    }
                }
            }
        }
    }

    private SootMethod getSootMethod(final SootClass sootClass, final String methodName,
            final List<Type> parameterTypes, final VoidType returnType) {
        SootMethod method = sootClass.getMethodUnsafe(methodName, parameterTypes, returnType);
        if (method == null) {
            method = sootClass.getMethodByNameUnsafe(methodName);
            if (method == null) {
                LOGGER.warning(
                        "In class " + sootClass.getName() + " - " + methodName + " method was not found by Soot");
            }
        }
        return method;
    }

    private BlockGraph getBlockGraph(final SootMethod method) {

        final Body body = method.retrieveActiveBody();

        BlockGraph graph = new ClassicCompleteBlockGraph(body);
        // Prepare call graph for inter-procedural recursive call
        List<SootMethod> entryPoints = new ArrayList<>();
        entryPoints.add(method);
        Scene.v().setEntryPoints(entryPoints);

        PackManager.v().getPack("cg").apply();

        return graph;
    }

    /**
     * Iterate through the control-flow graph with an iterative data-flow analysis
     * logic
     *
     * @param classPaths    Set of classes that is included in inter-procedural analysis
     * @param cg            Soot ControlFlowGraph
     * @param graph         Control Flow graph of method
     * @param outSet        OUT set of CFG
     * @param element       Bpmn element
     * @param chapter       ElementChapter
     * @param fieldType     KnownElementFieldType
     * @param filePath      ResourceFilePath for ProcessVariableOperation
     * @param scopeId       Scope of BpmnElement
     * @param originalBlock VariableBlock
     * @return OutSetCFG which contains data flow information
     */
    private OutSetCFG blockIterator(final Set<String> classPaths, final CallGraph cg, final BlockGraph graph,
            final Block block,
            OutSetCFG outSet, final BpmnElement element, final ElementChapter chapter,
            final KnownElementFieldType fieldType, final String filePath, final String scopeId,
            VariableBlock originalBlock, final String assignmentStmt, final List<Value> args,
            final AnalysisElement[] predecessor) {
        // Collect the functions Unit by Unit via the blockIterator
        final VariableBlock vb = variablesExtractor
                .blockIterator(classPaths, cg, block, outSet, element, chapter, fieldType, filePath,
                        scopeId, originalBlock, assignmentStmt, args, predecessor);

        // depending if outset already has that Block, only add variables,
        // if not, then add the whole vb
        if (outSet.getVariableBlock(vb.getBlock()) == null) {
            outSet.addVariableBlock(vb);
        }

        return outSet;
    }

    /**
     * Strips unnecessary characters and returns cleaned name
     *
     * @param className Classname to be stripped of unused chars
     * @return cleaned String
     */
    private String cleanString(String className, boolean dot) {
        className = ProcessVariablesScanner.cleanString(className, dot);
        return className;
    }

    private void addNodeAndClearPredecessors(AbstractNode node, ControlFlowGraph cg, LinkedHashMap<String, AnalysisElement>  predecessors) {
        cg.addNode(node);
        node.setPredecessors(new LinkedHashMap<>(predecessors));
        predecessors.clear();
        predecessors.put(node.getId(), node);
    }

    private void setupSoot() {
        final String sootPath = FileScanner.getSootPath();
        System.setProperty("soot.class.path", sootPath);
        Options.v().set_whole_program(true);
        Options.v().set_allow_phantom_refs(true);
        ArrayList<String> excludedClasses = new ArrayList<>();
        excludedClasses.add("java.*");
        excludedClasses.add("sun.*");
        excludedClasses.add("jdk.*");
        excludedClasses.add("javax.*");
        Options.v().set_exclude(excludedClasses);
        Options.v().set_no_bodies_for_excluded(true);
        Scene.v().extendSootClassPath(Scene.v().defaultClassPath());
    }
}
