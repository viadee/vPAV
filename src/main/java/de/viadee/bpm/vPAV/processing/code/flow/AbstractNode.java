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
import de.viadee.bpm.vPAV.processing.model.data.ElementChapter;
import de.viadee.bpm.vPAV.processing.model.data.ProcessVariableOperation;
import org.camunda.bpm.model.bpmn.instance.BaseElement;
import soot.toolkits.graph.Block;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public abstract class AbstractNode implements AnalysisElement {

	protected ControlFlowGraph controlFlowGraph;

	protected LinkedHashMap<String, ProcessVariableOperation> operations;
	protected LinkedHashMap<String, ProcessVariableOperation> defined;
	protected LinkedHashMap<String, ProcessVariableOperation> used;
	protected LinkedHashMap<String, ProcessVariableOperation> killed;
	protected LinkedHashMap<String, ProcessVariableOperation> inUsed;
	protected LinkedHashMap<String, ProcessVariableOperation> inUnused;
	protected LinkedHashMap<String, ProcessVariableOperation> outUsed;
	protected LinkedHashMap<String, ProcessVariableOperation> outUnused;

	protected BpmnElement parentElement;
	protected ElementChapter elementChapter;

	protected LinkedHashMap<String, AnalysisElement> predecessors;
	protected LinkedHashMap<String, AnalysisElement> successors;

	protected String id;

	AbstractNode(final ControlFlowGraph controlFlowGraph, final BpmnElement parentElement,
				 final ElementChapter elementChapter) {
		this.controlFlowGraph = controlFlowGraph;
		this.parentElement = parentElement;
		this.elementChapter = elementChapter;

		this.predecessors = new LinkedHashMap<>();
		this.successors = new LinkedHashMap<>();

		this.operations = new LinkedHashMap<>();
		this.defined = new LinkedHashMap<>();
		this.used = new LinkedHashMap<>();
		this.killed = new LinkedHashMap<>();
		this.inUsed = new LinkedHashMap<>();
		this.inUnused = new LinkedHashMap<>();
		this.outUsed = new LinkedHashMap<>();
		this.outUnused = new LinkedHashMap<>();
	}

	/**
	 * Adds an operation to the list of operations (used for line by line checking)
	 * Based on operation type adds the operation to the set of corresponding
	 * operations
	 *
	 * @param processVariableOperation
	 *            Current process variable operation
	 */
	public void addOperation(final ProcessVariableOperation processVariableOperation) {
		final String id = String.valueOf(parentElement.getFlowAnalysis().getOperationCounter());
		this.operations.put(processVariableOperation.getId(), processVariableOperation);
		switch (processVariableOperation.getOperation()) {
		case WRITE:
			defined.put(id, processVariableOperation);
			break;
		case READ:
			used.put(id, processVariableOperation);
			break;
		case DELETE:
			killed.put(id, processVariableOperation);
			break;
		}
	}

	/**
	 * Set the predecessor nodes of the current node for interprocedural methods
	 * 
	 * @param key
	 *            Key of node
	 */
	void setPredsInterProcedural(final String key) {
		AnalysisElement ae = controlFlowGraph.getNodes().get(key);
		this.predecessors.put(ae.getId(), ae);
	}

	/**
	 * Matches the ids and creates the references to nodeSuccessors and
	 * nodePredecessors
	 *
	 * @param idPattern
	 *            Pattern for resolving the id
	 * @param blockMatcher
	 *            Pattern to extract the local id of a node's block
	 * @param pred
	 *            Boolean to indicate whether the current node is a predecessor or
	 *            successor
	 */
	protected void createIds(final Pattern idPattern, final Matcher blockMatcher, final boolean pred) {
		if (blockMatcher.matches()) {
			String key = blockMatcher.group(2);
			Matcher idMatcher = idPattern.matcher(this.id);
			if (idMatcher.matches()) {
				String id = idMatcher.group();
				id = id.substring(0, id.length() - 1).concat(key);
				AnalysisElement ae = controlFlowGraph.getNodes().get(id);
				if (pred) {
					this.addPredecessor(ae);
				} else {
					this.addSuccessor(ae);
				}
			}
		}
	}

	abstract void setPreds();

	abstract void setSuccs();

	@Override
	public ControlFlowGraph getControlFlowGraph() {
		return null;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public LinkedHashMap<String, ProcessVariableOperation> getOperations() {
		return operations;
	}

	public void setOperations(LinkedHashMap<String, ProcessVariableOperation> operations) {
		this.operations = operations;
	}

	public LinkedHashMap<String, ProcessVariableOperation> getDefined() {
		return defined;
	}

	@Override
	public void addSourceCodeAnomaly(AnomalyContainer anomalyContainer) {
		this.parentElement.addSourceCodeAnomaly(anomalyContainer);
	}

	@Override
	public void clearPredecessors() {
		this.predecessors.clear();
	}

	@Override
	public void removePredecessor(String predecessor) {
		this.predecessors.remove(predecessor);
	}

	@Override
	public void clearSuccessors() { this.successors.clear(); }

	@Override
	public void removeSuccessor(String successor) {
		this.successors.remove(successor);
	}

	@Override
	public Map<BpmnElement, List<AnomalyContainer>> getAnomalies() {
		return getParentElement().getAnomalies();
	}

	@Override
	public BaseElement getBaseElement() {
		return this.parentElement.getBaseElement();
	}

	public void setDefined(LinkedHashMap<String, ProcessVariableOperation> defined) {
		this.defined = defined;
	}

	@Override
	public void addDefined(LinkedHashMap<String, ProcessVariableOperation> defined) {
		this.defined.putAll(defined);
	}

	public LinkedHashMap<String, ProcessVariableOperation> getUsed() {
		return used;
	}

	public void setUsed(LinkedHashMap<String, ProcessVariableOperation> used) {
		this.used = used;
	}

	public LinkedHashMap<String, ProcessVariableOperation> getKilled() {
		return killed;
	}

	public void setKilled(LinkedHashMap<String, ProcessVariableOperation> killed) {
		this.killed = killed;
	}

	public LinkedHashMap<String, ProcessVariableOperation> getInUsed() {
		return inUsed;
	}

	public void setInUsed(LinkedHashMap<String, ProcessVariableOperation> inUsed) {
		this.inUsed = inUsed;
	}

	public LinkedHashMap<String, ProcessVariableOperation> getInUnused() {
		return inUnused;
	}

	public void setInUnused(LinkedHashMap<String, ProcessVariableOperation> inUnused) {
		this.inUnused = inUnused;
	}

	public LinkedHashMap<String, ProcessVariableOperation> getOutUsed() {
		return outUsed;
	}

	public void setOutUsed(LinkedHashMap<String, ProcessVariableOperation> outUsed) {
		this.outUsed = outUsed;
	}

	public LinkedHashMap<String, ProcessVariableOperation> getOutUnused() {
		return outUnused;
	}

	@Override
	public void setOutUnused(LinkedHashMap<String, ProcessVariableOperation> outUnused) {
		this.outUnused = outUnused;
	}

	public BpmnElement getParentElement() {
		return parentElement;
	}

	@Override
	public void removeOperation(ProcessVariableOperation op) {
		this.operations.remove(op.getId());
	}

	@Override
	public void setPredecessors(LinkedHashMap<String, AnalysisElement> predecessors) {
		this.predecessors = predecessors;
	}

	@Override
	public void addPredecessor(AnalysisElement predecessor) {
		this.predecessors.put(predecessor.getId(), predecessor);
	}

	@Override
	public List<AnalysisElement> getPredecessors() {
		return this.predecessors.values().stream().map(NodeDecorator::new).collect(Collectors.toList());
	}

	@Override
	public List<AnalysisElement> getSuccessors() {
		return this.successors.values().stream().map(NodeDecorator::new).collect(Collectors.toList());
	}

	@Override
	public void setSuccessors(LinkedHashMap<String, AnalysisElement> successors) {
		this.successors = successors;
	}

	@Override
	public void addSuccessor(AnalysisElement successor) {
		this.successors.put(successor.getId(), successor);
	}

	ElementChapter getElementChapter() {
		return elementChapter;
	}
}
