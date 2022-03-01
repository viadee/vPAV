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

import de.viadee.bpm.vpav.beans.BeanMappingGenerator;
import de.viadee.bpm.vpav.config.model.RuleSet;
import de.viadee.bpm.vpav.processing.BpmnModelDispatcher;
import de.viadee.bpm.vpav.processing.ElementGraphBuilder;
import de.viadee.bpm.vpav.processing.EntryPointScanner;
import de.viadee.bpm.vpav.processing.code.flow.FlowAnalysis;
import de.viadee.bpm.vpav.processing.model.data.ProcessVariable;
import de.viadee.bpm.vpav.processing.model.graph.Graph;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.springframework.context.ApplicationContext;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Is used for the Data Flow Validation Language.
 */
public class ProcessApplicationVariableParser {

    public static Collection<ProcessVariable> parseProcessVariables(File modelFile, ApplicationContext ctx) {
        RuntimeConfig.getInstance().setApplicationContext(ctx);
        RuntimeConfig.getInstance().setBeanMapping(BeanMappingGenerator.generateBeanMappingFile(ctx));
        RuntimeConfig.getInstance().setClassLoader(ProcessApplicationValidator.class.getClassLoader());

        // Retrieve BPMN elements
        FileScanner fileScanner = new FileScanner(new RuleSet());
        EntryPointScanner variableScanner = readOuterProcessVariables(fileScanner);
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(modelFile);

        final ElementGraphBuilder graphBuilder = new ElementGraphBuilder(fileScanner.getDecisionRefToPathMap(),
                fileScanner.getProcessIdToPathMap(), variableScanner.getMessageIdToVariableMap(),
                variableScanner.getProcessIdToVariableMap());

        // create data flow graphs for bpmn model including creation of process variables
        Collection<Graph> graphCollection = graphBuilder.createProcessGraph(fileScanner, modelInstance,
                modelFile.getPath(), new ArrayList<>(), variableScanner, new FlowAnalysis());

        return BpmnModelDispatcher.getProcessVariables(graphCollection.iterator().next().getVertices());
    }

    /**
     * Scan process variables in external classes, which are not referenced from
     * model
     *
     * @param fileScanner FileScanner
     */
    private static EntryPointScanner readOuterProcessVariables(final FileScanner fileScanner) {
        EntryPointScanner variableScanner = new EntryPointScanner(
                fileScanner.getJavaResourcesFileInputStream());
        variableScanner.scanProcessVariables();
        return variableScanner;
    }
}
