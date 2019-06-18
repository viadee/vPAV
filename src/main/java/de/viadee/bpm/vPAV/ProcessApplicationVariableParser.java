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
package de.viadee.bpm.vPAV;

import de.viadee.bpm.vPAV.beans.BeanMappingGenerator;
import de.viadee.bpm.vPAV.config.model.RuleSet;
import de.viadee.bpm.vPAV.processing.BpmnModelDispatcher;
import de.viadee.bpm.vPAV.processing.ElementGraphBuilder;
import de.viadee.bpm.vPAV.processing.ProcessVariablesScanner;
import de.viadee.bpm.vPAV.processing.code.flow.BpmnElement;
import de.viadee.bpm.vPAV.processing.code.flow.FlowAnalysis;
import de.viadee.bpm.vPAV.processing.model.data.ProcessVariable;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.BaseElement;
import org.springframework.context.ApplicationContext;

import java.io.File;
import java.util.Collection;

public class ProcessApplicationVariableParser {

	public static Collection<ProcessVariable> parseProcessVariables(File modelFile, ApplicationContext ctx) {
		RuntimeConfig.getInstance().setApplicationContext(ctx);
		RuntimeConfig.getInstance().setBeanMapping(BeanMappingGenerator.generateBeanMappingFile(ctx));
		RuntimeConfig.getInstance().setClassLoader(ProcessApplicationValidator.class.getClassLoader());

		FileScanner fileScanner = new FileScanner(new RuleSet());
		ProcessVariablesScanner variableScanner = readOuterProcessVariables(fileScanner);

		BpmnScanner bpmnScanner = createScanner(modelFile);
		final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(modelFile);

		// hold bpmn elements
		final Collection<BaseElement> baseElements = modelInstance.getModelElementsByType(BaseElement.class);

		final ElementGraphBuilder graphBuilder = new ElementGraphBuilder(fileScanner.getDecisionRefToPathMap(),
				fileScanner.getProcessIdToPathMap(), variableScanner.getMessageIdToVariableMap(),
				variableScanner.getProcessIdToVariableMap(), bpmnScanner);

		final Collection<BpmnElement> bpmnElements = BpmnModelDispatcher.getBpmnElements(modelFile, baseElements,
				graphBuilder, new FlowAnalysis());

		return BpmnModelDispatcher.getProcessVariables(bpmnElements);
	}

	/**
	 * Scan process variables in external classes, which are not referenced from
	 * model
	 *
	 * @param fileScanner
	 *            FileScanner
	 */
	private static ProcessVariablesScanner readOuterProcessVariables(final FileScanner fileScanner) {
		ProcessVariablesScanner variableScanner = new ProcessVariablesScanner(
				fileScanner.getJavaResourcesFileInputStream());
		variableScanner.scanProcessVariables();
		return variableScanner;
	}

	/**
	 *
	 * @param processDefinition
	 *            Holds the path to the BPMN model
	 * @return BpmnScanner
	 */
	private static BpmnScanner createScanner(final File processDefinition) {
		// create BPMNScanner
		return new BpmnScanner(processDefinition.getPath());
	}
}
