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
package de.viadee.bpm.vPAV.processing.code.flow;

import de.viadee.bpm.vPAV.processing.model.data.AnomalyContainer;
import de.viadee.bpm.vPAV.processing.model.data.ProcessVariableOperation;
import org.camunda.bpm.model.bpmn.instance.BaseElement;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public interface AnalysisElement {

	ControlFlowGraph getControlFlowGraph();

	String getId();

	LinkedHashMap<String, ProcessVariableOperation> getOperations();

	void setPredecessors(LinkedHashMap<String, AnalysisElement> predecessors);

	void addPredecessor(AnalysisElement predecessor);

	List<AnalysisElement> getPredecessors();

	List<AnalysisElement> getSuccessors();

	void setSuccessors(LinkedHashMap<String, AnalysisElement> successors);

	void addSuccessor(AnalysisElement successor);

	LinkedHashMap<String, ProcessVariableOperation> getInUsed();

	LinkedHashMap<String, ProcessVariableOperation> getInUnused();

	LinkedHashMap<String, ProcessVariableOperation> getOutUsed();

	LinkedHashMap<String, ProcessVariableOperation> getOutUnused();

	void setInUsed(LinkedHashMap<String, ProcessVariableOperation> inUsed);

	void setInUnused(LinkedHashMap<String, ProcessVariableOperation> inUnused);

	void setOutUsed(LinkedHashMap<String, ProcessVariableOperation> outUsed);

	void setOutUnused(LinkedHashMap<String, ProcessVariableOperation> outUnused);

	LinkedHashMap<String, ProcessVariableOperation> getUsed();

	LinkedHashMap<String, ProcessVariableOperation> getKilled();

	LinkedHashMap<String, ProcessVariableOperation> getDefined();

	void setOperations(LinkedHashMap<String, ProcessVariableOperation> operations);

	void setUsed(LinkedHashMap<String, ProcessVariableOperation> used);

	void setDefined(LinkedHashMap<String, ProcessVariableOperation> defined);

	void addDefined(LinkedHashMap<String, ProcessVariableOperation> defined);

	void addSourceCodeAnomaly(AnomalyContainer anomalyContainer);

	void clearPredecessors();

	void removePredecessor(String predecessor);

	void clearSuccessors();

	void removeSuccessor(String successor);

	Map<BpmnElement, List<AnomalyContainer>> getAnomalies();

	BaseElement getBaseElement();

	BpmnElement getParentElement();

	void removeOperation(ProcessVariableOperation op);

}
