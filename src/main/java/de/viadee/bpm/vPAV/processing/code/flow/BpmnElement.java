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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import de.viadee.bpm.vPAV.processing.model.data.AnomalyContainer;
import de.viadee.bpm.vPAV.processing.model.data.InOutState;
import de.viadee.bpm.vPAV.processing.model.data.ProcessVariableOperation;
import de.viadee.bpm.vPAV.processing.model.data.VariableOperation;
import org.camunda.bpm.model.bpmn.instance.BaseElement;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents an BPMN element
 *
 */
public class BpmnElement implements AnalysisElement {

	private String processDefinition;

	private BaseElement baseElement;

	private ControlFlowGraph controlFlowGraph;

	private LinkedHashMap<String, ProcessVariableOperation> operations;
	private LinkedHashMap<String, ProcessVariableOperation> defined;
	private LinkedHashMap<String, ProcessVariableOperation> used;
	private LinkedHashMap<String, ProcessVariableOperation> killed;
	private LinkedHashMap<String, ProcessVariableOperation> inUsed;
	private LinkedHashMap<String, ProcessVariableOperation> inUnused;
	private LinkedHashMap<String, ProcessVariableOperation> outUsed;
	private LinkedHashMap<String, ProcessVariableOperation> outUnused;

	private LinkedHashMap<String, AnalysisElement> predecessors;
	private LinkedHashMap<String, AnalysisElement> successors;

	public BpmnElement(final String processDefinition, final BaseElement element,
			final ControlFlowGraph controlFlowGraph) {
		this.processDefinition = processDefinition;
		this.baseElement = element;
		this.controlFlowGraph = controlFlowGraph;
		this.operations = new LinkedHashMap<>();

		this.predecessors = new LinkedHashMap<>();
		this.successors = new LinkedHashMap<>();

		this.operations = new LinkedHashMap<>();
		this.processVariables = ArrayListMultimap.create();
		this.defined = new LinkedHashMap<>();
		this.used = new LinkedHashMap<>();
		this.killed = new LinkedHashMap<>();
		this.inUsed = new LinkedHashMap<>();
		this.inUnused = new LinkedHashMap<>();
		this.outUsed = new LinkedHashMap<>();
		this.outUnused = new LinkedHashMap<>();
	}

	public ControlFlowGraph getControlFlowGraph() {
		return controlFlowGraph;
	}

	/**
	 * Sets the process variables of this element
	 *
	 * @param variables
	 *            Collection of variables
	 */
	public void setProcessVariables(final ListMultimap<String, ProcessVariableOperation> variables) {
		variables.entries().forEach(e -> addOperation(e.getValue()));
		this.processVariables.putAll(variables);
	}

	/**
	 * Puts process variable operations into correct sets
	 *
	 * @param processVariableOperation
	 *            Current operation
	 */
	private void addOperation(final ProcessVariableOperation processVariableOperation) {
		this.operations.put(processVariableOperation.getId(), processVariableOperation);
		switch (processVariableOperation.getOperation()) {
		case WRITE:
			defined.put(processVariableOperation.getId(), processVariableOperation);
			break;
		case READ:
			used.put(processVariableOperation.getId(), processVariableOperation);
			break;
		case DELETE:
			killed.put(processVariableOperation.getId(), processVariableOperation);
			break;
		}
	}

	private Map<String, InOutState> used_old = new HashMap<String, InOutState>();

	private Map<String, InOutState> defined_old = new HashMap<String, InOutState>();

	private Map<String, InOutState> in = new HashMap<String, InOutState>();

	private Map<String, InOutState> out = new HashMap<String, InOutState>();

	/* in interface for call activity */
	private Collection<String> inCa;

	/* out interface for call activity */
	private Collection<String> outCa;

	private ListMultimap<String, ProcessVariableOperation> processVariables;

	public List<AnomalyContainer> getSourceCodeAnomalies() {
		return sourceCodeAnomalies;
	}

	// collecting anomalies found on Java code level
	private List<AnomalyContainer> sourceCodeAnomalies = new ArrayList<>();

	public String getProcessDefinition() {
		return processDefinition;
	}

	public BaseElement getBaseElement() {
		return baseElement;
	}

	public ListMultimap<String, ProcessVariableOperation> getProcessVariables() {
		return processVariables;
	}

	public void setProcessVariable(final String variableName, final ProcessVariableOperation variableObject) {
		processVariables.put(variableName, variableObject);
	}

	public void addSourceCodeAnomaly(AnomalyContainer anomaly) {
		sourceCodeAnomalies.add(anomaly);
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
	public int hashCode() {
		return baseElement.getId().hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof BpmnElement && this.hashCode() == o.hashCode()) {
			return true;
		}
		return false;
	}

	@Override
	public String toString() {
		return baseElement.getId();
	}

	public Map<String, InOutState> getIn() {
		return in;
	}

	public Map<String, InOutState> getOut() {
		return out;
	}

	public void setIn(final Map<String, InOutState> outPredecessor) {
		this.in = outPredecessor;
		// TODO: call activity (create own method)
		if (inCa != null) {
			final Collection<String> removeCandidates = new ArrayList<String>();
			for (final String variable : in.keySet()) {
				if (!inCa.contains(variable)) {
					removeCandidates.add(variable);
				}
			}
			for (final String var : removeCandidates) {
				in.remove(var);
			}
		}
	}

	public void setOut() {
		out.putAll(defined());
		changeStatusToRead(in);
		out.putAll(killed());
		// TODO: call activity (create own method)
		if (outCa != null) {
			final Collection<String> removeCandidates = new ArrayList<String>();
			for (final String variable : out.keySet()) {
				if (!outCa.contains(variable)) {
					removeCandidates.add(variable);
				} else {
					final InOutState state = out.get(variable);
					if (state == InOutState.DELETED) {
						removeCandidates.add(variable);
					}
				}
			}
			for (final String var : removeCandidates) {
				out.remove(var);
			}
		}
	}

	private Map<String, InOutState> used() {
		if (this.used_old.isEmpty()) {
			for (final ProcessVariableOperation var : processVariables.values()) {
				if (var.getOperation() == VariableOperation.READ) {
					used_old.put(var.getName(), InOutState.READ);
				}
			}
		}
		return used_old;
	}

	public Map<String, InOutState> defined() {
		if (this.defined_old.isEmpty()) {
			for (final ProcessVariableOperation var : processVariables.values()) {
				if (var.getOperation() == VariableOperation.WRITE) {
					defined_old.put(var.getName(), InOutState.DEFINED);
				}
			}
		}
		return defined_old;
	}

	private Map<String, InOutState> killed() {
		final Map<String, InOutState> killedVariables = new HashMap<String, InOutState>();
		for (final ProcessVariableOperation var : processVariables.values()) {
			if (var.getOperation() == VariableOperation.DELETE) {
				killedVariables.put(var.getName(), InOutState.DELETED);
			}
		}
		return killedVariables;
	}

	public void setInCa(final Collection<String> in) {
		this.inCa = in;
	}

	public void setOutCa(final Collection<String> out) {
		this.outCa = out;
	}

	public boolean ur(final String varName) {
		if ((in.containsKey(varName) == false
				|| (in.containsKey(varName) == true && in.get(varName) == InOutState.DELETED))
				&& used().containsKey(varName)) {
			return true;
		}
		return false;
	}

	public boolean du(final String varName) {
		return in.containsKey(varName) && in.get(varName) == InOutState.DEFINED && out.containsKey(varName)
				&& out.get(varName) == InOutState.DELETED;
	}

	public boolean dd(final String varName) {
		return in.containsKey(varName) && in.get(varName) == InOutState.DEFINED && defined().containsKey(varName);
	}

	public Map<BpmnElement, List<AnomalyContainer>> getAnomalies() {
		final Map<BpmnElement, List<AnomalyContainer>> anomalyMap = new HashMap<>();
		anomalyMap.put(this, getSourceCodeAnomalies());
		return anomalyMap;
	}

	private void changeStatusToRead(final Map<String, InOutState> inVariables) {
		for (final String varName : inVariables.keySet()) {
			if (used().containsKey(varName)) {
				out.put(varName, InOutState.READ);
			} else {
				out.put(varName, inVariables.get(varName));
			}
		}
	}

	public LinkedHashMap<String, ProcessVariableOperation> getInUsed() {
		return inUsed;
	}

	public LinkedHashMap<String, ProcessVariableOperation> getInUnused() {
		return inUnused;
	}

	public LinkedHashMap<String, ProcessVariableOperation> getOutUsed() {
		return outUsed;
	}

	public LinkedHashMap<String, ProcessVariableOperation> getOutUnused() {
		return outUnused;
	}

	public void setInUsed(LinkedHashMap<String, ProcessVariableOperation> inUsedB) {
		this.inUsed = inUsedB;
	}

	public void setInUnused(LinkedHashMap<String, ProcessVariableOperation> inUnusedB) {
		this.inUnused = inUnusedB;
	}

	public void setOutUsed(LinkedHashMap<String, ProcessVariableOperation> outUsed) {
		this.outUsed = outUsed;
	}

	public void setOutUnused(LinkedHashMap<String, ProcessVariableOperation> outUnused) {
		this.outUnused = outUnused;
	}

	public LinkedHashMap<String, ProcessVariableOperation> getUsed() {
		return used;
	}

	public LinkedHashMap<String, ProcessVariableOperation> getKilled() {
		return killed;
	}

	public LinkedHashMap<String, ProcessVariableOperation> getDefined() {
		return defined;
	}

	@Override
	public void setDefined(LinkedHashMap<String, ProcessVariableOperation> defined) {
		this.defined = defined;
	}

	@Override
	public String getId() {
		return this.getBaseElement().getId();
	}

	@Override
	public LinkedHashMap<String, ProcessVariableOperation> getOperations() {
		return operations;
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
		return this.predecessors.values().stream().map(BpmnElementDecorator::new).collect(Collectors.toList());
	}

	@Override
	public List<AnalysisElement> getSuccessors() {
		return this.successors.values().stream().map(BpmnElementDecorator::new).collect(Collectors.toList());
	}

	@Override
	public void setSuccessors(LinkedHashMap<String, AnalysisElement> successors) {
		this.successors = successors;
	}

	@Override
	public void addSuccessor(AnalysisElement successor) {
		this.successors.put(successor.getId(), successor);
	}
}
