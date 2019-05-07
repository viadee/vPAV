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
package de.viadee.bpm.vPAV.processing.model.data;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import org.camunda.bpm.model.bpmn.instance.BaseElement;

import java.util.*;

/**
 * Represents an BPMN element
 *
 */
public class BpmnElement {

	private String processDefinition;

	private BaseElement baseElement;

	private Map<String, InOutState> used = new HashMap<String, InOutState>();

	private Map<String, InOutState> defined = new HashMap<String, InOutState>();

	private Map<String, InOutState> in = new HashMap<String, InOutState>();

	private Map<String, InOutState> out = new HashMap<String, InOutState>();

	/* in interface for call activity */
	private Collection<String> inCa;

	/* out interface for call activity */
	private Collection<String> outCa;

	private ListMultimap<String, ProcessVariableOperation> processVariables;

	// collecting anomalies found on Java code level
	private List<AnomalyContainer> sourceCodeAnomalies = new ArrayList<AnomalyContainer>();

	public BpmnElement(final String processDefinition, final BaseElement element) {
		this.processDefinition = processDefinition;
		this.baseElement = element;
		this.processVariables = ArrayListMultimap.create();
	}

	public String getProcessDefinition() {
		return processDefinition;
	}

	public BaseElement getBaseElement() {
		return baseElement;
	}

	public ListMultimap<String, ProcessVariableOperation> getProcessVariables() {
		return processVariables;
	}

	public void setProcessVariables(final ListMultimap<String, ProcessVariableOperation> variables) {
		this.processVariables = variables;
	}

	public void setProcessVariable(final String variableName, final ProcessVariableOperation variableObject) {
		processVariables.put(variableName, variableObject);
	}

	public void addSourceCodeAnomaly(AnomalyContainer anomaly) {
		sourceCodeAnomalies.add(anomaly);
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
		if (this.used.isEmpty()) {
			for (final ProcessVariableOperation var : processVariables.values()) {
				if (var.getOperation() == VariableOperation.READ) {
					used.put(var.getName(), InOutState.READ);
				}
			}
		}
		return used;
	}

	public Map<String, InOutState> defined() {
		if (this.defined.isEmpty()) {
			for (final ProcessVariableOperation var : processVariables.values()) {
				if (var.getOperation() == VariableOperation.WRITE) {
					defined.put(var.getName(), InOutState.DEFINED);
				}
			}
		}
		return defined;
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
		if (in.containsKey(varName) && in.get(varName) == InOutState.DEFINED && out.containsKey(varName)
				&& out.get(varName) == InOutState.DELETED) {
			return true;
		}
		return false;
	}

	public boolean dd(final String varName) {
		if (in.containsKey(varName) && in.get(varName) == InOutState.DEFINED && defined().containsKey(varName)) {
			return true;
		}
		return false;
	}

	public Map<BpmnElement, List<AnomalyContainer>> getAnomalies() {
		final Map<BpmnElement, List<AnomalyContainer>> anomalyMap = new HashMap<>();
		anomalyMap.put(this, this.sourceCodeAnomalies);
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
}
