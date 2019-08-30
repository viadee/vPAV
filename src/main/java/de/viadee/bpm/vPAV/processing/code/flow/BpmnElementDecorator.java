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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.camunda.bpm.model.bpmn.instance.BaseElement;

import com.google.common.collect.ListMultimap;

import de.viadee.bpm.vPAV.processing.model.data.AnomalyContainer;
import de.viadee.bpm.vPAV.processing.model.data.ProcessVariableOperation;

public class BpmnElementDecorator implements AnalysisElement, Cloneable {

	private AnalysisElement decoratedBpmnElement;

	BpmnElementDecorator(final AnalysisElement element) {
		this.decoratedBpmnElement = element;
	}

	@Override
	public ControlFlowGraph getControlFlowGraph() {
		return decoratedBpmnElement.getControlFlowGraph();
	}

	@Override
	public String getId() {
		return decoratedBpmnElement.getId();
	}

	@Override
	public LinkedHashMap<String, ProcessVariableOperation> getOperations() {
		return decoratedBpmnElement.getOperations();
	}

	@Override
	public void setPredecessors(LinkedHashMap<String, AnalysisElement> predecessors) {
		decoratedBpmnElement.setPredecessors(predecessors);
	}

	@Override
	public void addPredecessor(AnalysisElement predecessor) {
		decoratedBpmnElement.addPredecessor(predecessor);
	}

	@Override
	public List<AnalysisElement> getPredecessors() {
		return decoratedBpmnElement.getPredecessors();
	}

	@Override
	public List<AnalysisElement> getSuccessors() {
		return decoratedBpmnElement.getSuccessors();
	}

	@Override
	public void setSuccessors(LinkedHashMap<String, AnalysisElement> successors) {
		this.decoratedBpmnElement.setSuccessors(successors);
	}

	@Override
	public void addSuccessor(AnalysisElement successor) {
		this.decoratedBpmnElement.addSuccessor(successor);
	}

	@Override
	public LinkedHashMap<String, ProcessVariableOperation> getInUsed() {
		return this.decoratedBpmnElement.getInUsed();
	}

	@Override
	public LinkedHashMap<String, ProcessVariableOperation> getInUnused() {
		return this.decoratedBpmnElement.getInUnused();
	}

	@Override
	public LinkedHashMap<String, ProcessVariableOperation> getOutUsed() {
		return this.decoratedBpmnElement.getOutUsed();
	}

	@Override
	public LinkedHashMap<String, ProcessVariableOperation> getOutUnused() {
		return this.decoratedBpmnElement.getOutUnused();
	}

	@Override
	public void setInUsed(LinkedHashMap<String, ProcessVariableOperation> inUsed) {
		this.decoratedBpmnElement.setInUsed(inUsed);
	}

	@Override
	public void setInUnused(LinkedHashMap<String, ProcessVariableOperation> inUnused) {
		this.decoratedBpmnElement.setInUnused(inUnused);
	}

	@Override
	public void setOutUsed(LinkedHashMap<String, ProcessVariableOperation> outUsed) {
		this.decoratedBpmnElement.setOutUsed(outUsed);
	}

	@Override
	public void setOutUnused(LinkedHashMap<String, ProcessVariableOperation> outUnused) {
		this.decoratedBpmnElement.setOutUnused(outUnused);
	}

	@Override
	public LinkedHashMap<String, ProcessVariableOperation> getUsed() {
		return this.decoratedBpmnElement.getUsed();
	}

	@Override
	public LinkedHashMap<String, ProcessVariableOperation> getKilled() {
		return this.decoratedBpmnElement.getKilled();
	}

	@Override
	public LinkedHashMap<String, ProcessVariableOperation> getDefined() {
		return this.decoratedBpmnElement.getDefined();
	}

    @Override
    public void setOperations(LinkedHashMap<String, ProcessVariableOperation> operations) {
        this.decoratedBpmnElement.setOperations(operations);
    }

    @Override
    public void setUsed(LinkedHashMap<String, ProcessVariableOperation> used) {
        this.decoratedBpmnElement.setUsed(used);
    }

    @Override
    public void setDefined(LinkedHashMap<String, ProcessVariableOperation> defined) {
        this.decoratedBpmnElement.setDefined(defined);
    }

	@Override
	public void addDefined(LinkedHashMap<String, ProcessVariableOperation> defined) {
		this.decoratedBpmnElement.addDefined(defined);
	}

	@Override
	public void addSourceCodeAnomaly(AnomalyContainer anomalyContainer) {
		this.decoratedBpmnElement.addSourceCodeAnomaly(anomalyContainer);
	}

	@Override
	public void clearPredecessors() {
		this.decoratedBpmnElement.clearPredecessors();
	}

	@Override
	public void removePredecessor(String predecessor) {
		this.decoratedBpmnElement.removePredecessor(predecessor);
	}

	@Override
	public void clearSuccessors() {
		this.decoratedBpmnElement.clearSuccessors();
	}

	@Override
	public void removeSuccessor(String successor) {
		this.decoratedBpmnElement.removeSuccessor(successor);
	}

	@Override
	public Map<BpmnElement, List<AnomalyContainer>> getAnomalies() {
		return decoratedBpmnElement.getAnomalies();
	}

	@Override
	public BaseElement getBaseElement() {
		return decoratedBpmnElement.getBaseElement();
	}

	@Override
	public BpmnElement getParentElement() {
		return decoratedBpmnElement.getParentElement();
	}

	@Override
	public void removeOperation(ProcessVariableOperation op) {
		this.decoratedBpmnElement.removeOperation(op);
	}

	public ListMultimap<String, ProcessVariableOperation> getProcessVariables() {
		return ((BpmnElement) decoratedBpmnElement).getProcessVariables();
	}
}
