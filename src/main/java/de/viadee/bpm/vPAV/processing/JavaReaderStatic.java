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
package de.viadee.bpm.vPAV.processing;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import de.viadee.bpm.vPAV.FileScanner;
import de.viadee.bpm.vPAV.ProcessVariablesCreator;
import de.viadee.bpm.vPAV.SootResolverSimplified;
import de.viadee.bpm.vPAV.constants.BpmnConstants;
import de.viadee.bpm.vPAV.processing.code.flow.BasicNode;
import de.viadee.bpm.vPAV.processing.code.flow.BpmnElement;
import de.viadee.bpm.vPAV.processing.model.data.*;
import org.camunda.bpm.model.bpmn.impl.BpmnModelConstants;
import soot.*;
import soot.options.Options;
import soot.toolkits.graph.Block;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Logger;

public class JavaReaderStatic {

    private static final Logger LOGGER = Logger.getLogger(JavaReaderStatic.class.getName());

    public JavaReaderStatic() {
        this.setupSoot();
    }

    /**
     * Checks a java delegate for process variable references with static code
     * analysis (read/write/delete).
     *
     * @param classFile   Name of the class
     * @param element     Bpmn element
     * @param chapter     ElementChapter
     * @param fieldType   KnownElementFieldType
     */
    void getVariablesFromJavaDelegate(final String classFile, final BpmnElement element, final ElementChapter chapter,
            final KnownElementFieldType fieldType, BasicNode[] predecessor) {

        if (classFile != null && classFile.trim().length() > 0) {

            final String sootPath = FileScanner.getSootPath();
            System.setProperty("soot.class.path", sootPath);

            if (element.getBaseElement().getAttributeValueNs(BpmnModelConstants.CAMUNDA_NS,
                    BpmnConstants.ATTR_VAR_MAPPING_CLASS) != null
                    || element.getBaseElement().getAttributeValueNs(BpmnModelConstants.CAMUNDA_NS,
                    BpmnConstants.ATTR_VAR_MAPPING_DELEGATE) != null) {
                // Delegate Variable Mapping
                classFetcherNew(classFile, "mapInputVariables", element,
                        ElementChapter.InputImplementation, fieldType, predecessor);

                classFetcherNew(classFile, "mapOutputVariables", element,
                        ElementChapter.OutputImplementation, fieldType, predecessor);
            } else {
                // Java Delegate or Listener
                SootClass sootClass = Scene.v()
                        .forceResolve(ProcessVariablesScanner.cleanString(classFile, true), SootClass.SIGNATURES);
                if (sootClass.declaresMethodByName("notify")) {
                    classFetcherNew(classFile, "notify", element, chapter, fieldType, predecessor);
                } else if (sootClass.declaresMethodByName("execute")) {
                    classFetcherNew(classFile, "execute", element, chapter, fieldType, predecessor);
                } else {
                    LOGGER.warning("No supported (execute/notify) method in " + classFile + " found.");
                }
            }
        }
    }

    /**
     * Retrieves variables from a class
     *
     * @param className        Name of the class that potentially declares process variables
     * @param element          BpmnElement
     * @param entryPoint       Current entry point
     * @return Map of process variable operations
     */
    ListMultimap<String, ProcessVariableOperation> getVariablesFromClass(String className, final BpmnElement element,
            final EntryPoint entryPoint, BasicNode[] predecessor) {

        final ListMultimap<String, ProcessVariableOperation> initialOperations = ArrayListMultimap.create();

        if (className != null && className.trim().length() > 0) {
            className = ProcessVariablesScanner.cleanString(className, true);
            SootClass sootClass = Scene.v().forceResolve(className, SootClass.SIGNATURES);

            if (sootClass != null) {
                sootClass.setApplicationClass();
                Scene.v().loadNecessaryClasses();
                for (SootMethod method : sootClass.getMethods()) {
                    if (method.getName().equals(entryPoint.getMethodName())) {
                        Block block = SootResolverSimplified.getBlockFromMethod(method);
                        ProcessVariablesCreator pvc = new ProcessVariablesCreator(element,
                                ElementChapter.Implementation, KnownElementFieldType.Class,
                                predecessor);
                        pvc.startBlockProcessing(block, new ArrayList<>());
                    }
                }
            }
        }
        return initialOperations;
    }

    private void classFetcherNew(final String className,
            final String methodName, final BpmnElement element,
            final ElementChapter chapter, final KnownElementFieldType fieldType, BasicNode[] predecessor) {

        Block block = SootResolverSimplified.getBlockFromClass(className, methodName, null, null);
        ProcessVariablesCreator processVariablesCreator = new ProcessVariablesCreator(element, chapter, fieldType,
                predecessor);
        BasicNode lastNode = processVariablesCreator
                .startBlockProcessing(block, SootResolverSimplified.getParameterValuesForDefaultMethods(methodName));
        if (lastNode != null) {
            predecessor[0] = lastNode;
        }
    }

    public void setupSoot() {
        final String sootPath = FileScanner.getSootPath();
        System.setProperty("soot.class.path", sootPath);
        Options.v().set_whole_program(true);
        Options.v().set_allow_phantom_refs(true);
        String[] exClasses = new String[] { "java.*", "sun.*", "jdk.*", "javax.*" };
        Options.v().set_exclude(Arrays.asList(exClasses));
        Options.v().set_no_bodies_for_excluded(true);
        Scene.v().extendSootClassPath(Scene.v().defaultClassPath());
    }
}
