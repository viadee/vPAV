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

import de.viadee.bpm.vPAV.processing.model.data.Anomaly;
import de.viadee.bpm.vPAV.processing.model.data.AnomalyContainer;
import de.viadee.bpm.vPAV.processing.model.data.KnownElementFieldType;
import de.viadee.bpm.vPAV.processing.model.data.ProcessVariableOperation;
import de.viadee.bpm.vPAV.processing.model.graph.Graph;
import org.camunda.bpm.model.bpmn.instance.CallActivity;
import org.camunda.bpm.model.bpmn.instance.EndEvent;
import org.camunda.bpm.model.bpmn.instance.StartEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.stream.Stream;

import static de.viadee.bpm.vPAV.processing.model.data.VariableOperation.*;

public class FlowAnalysis {

	private LinkedHashMap<String, AnalysisElement> nodes;
	private LinkedHashMap<String, ProcessVariableOperation> scopedOperations;

	private int operationCounter = 0;

	public FlowAnalysis() {
		this.nodes = new LinkedHashMap<>();
		this.scopedOperations = new LinkedHashMap<>();
	}

	/**
	 * Given a collection of graphs, this method is the sole entrance to the
	 * analysis of the graphs. First process model and control flow graph of
	 * delegates are embedded to create a single graph. Then the graph is analyzed
	 * conservatively by following the Reaching Definition algorithm. For
	 * discovering data flow anomalies inside a single block, a sequential check on
	 * unit basis is performed. Lastly, anomalies are extracted and appended to the
	 * parent element (for visualization)
	 *
	 * @param graphCollection
	 *            Collection of graphs
	 */
	public void analyze(final Collection<Graph> graphCollection) {
		for (Graph graph : graphCollection) {
			embedControlFlowGraph(graph);
			computeReachingDefinitions();
			computeLineByLine();
			extractAnomalies();
		}
	}

	/**
	 * Embeds the control flow graphs of bpmn elements into the process model
	 *
	 * @param graph
	 *            Given Graph
	 */
	private void embedControlFlowGraph(final Graph graph) {
		// Add all elements on bpmn level
		graph.getVertexInfo().keySet().forEach(element -> {
			AnalysisElement analysisElement = new BpmnElementDecorator(element);
			analysisElement.clearPredecessors();
			graph.getAdjacencyListPredecessor(element)
					.forEach(value -> analysisElement.addPredecessor(new BpmnElementDecorator(value)));
			graph.getAdjacencyListSuccessor(element)
					.forEach(value -> analysisElement.addSuccessor(new BpmnElementDecorator(value)));
			this.nodes.put(analysisElement.getId(), analysisElement);
		});

		// Add all nodes on source code level and correct the pointers
		final LinkedHashMap<String, AnalysisElement> temp = new LinkedHashMap<>(nodes);
		final LinkedHashMap<String, AnalysisElement> cfgNodes = new LinkedHashMap<>();
		final ArrayList<String> ids = new ArrayList<>();
		temp.values().forEach(analysisElement -> {
			if (analysisElement.getControlFlowGraph().hasNodes()) {
				analysisElement.getControlFlowGraph().computePredecessorRelations();

				final Node firstNode = analysisElement.getControlFlowGraph().firstNode();
				final Node lastNode = analysisElement.getControlFlowGraph().lastNode();

				final LinkedHashMap<String, ProcessVariableOperation> inputVariables = new LinkedHashMap<>();
				final LinkedHashMap<String, ProcessVariableOperation> outputVariables = new LinkedHashMap<>();
				final LinkedHashMap<String, ProcessVariableOperation> initialVariables = new LinkedHashMap<>();
				analysisElement.getOperations().values().forEach(operation -> {
					if (operation.getFieldType().equals(KnownElementFieldType.InputParameter)) {
						inputVariables.put(operation.getId(), operation);
					} else if (operation.getFieldType().equals(KnownElementFieldType.OutputParameter)) {
						outputVariables.put(operation.getId(), operation);
					} else if (operation.getFieldType().equals(KnownElementFieldType.Initial)) {
						initialVariables.put(operation.getId(), operation);
					}
				});

				firstNode.setInUnused(inputVariables);
				lastNode.setOutUnused(outputVariables);

				// If we have initial operations, we cant have input mapping (restriction of
				// start event)
				// Set initial operations as input for the first block and later remove bpmn
				// element
				if (!initialVariables.isEmpty()) {
					firstNode.setInUnused(initialVariables);
				}

				// Replace element with first block
				analysisElement.getPredecessors().forEach(pred -> {
					pred.removeSuccessor(analysisElement.getId());
					pred.addSuccessor(new NodeDecorator(firstNode));
					firstNode.addPredecessor(pred);
				});

				// Replace element with last block
				analysisElement.getSuccessors().forEach(succ -> {
					succ.removePredecessor(analysisElement.getId());
					succ.addPredecessor(new NodeDecorator(lastNode));
				});

				// Set predecessor relation for blocks across delegates
				Iterator<Node> iterator = analysisElement.getControlFlowGraph().getNodes().values().iterator();
				Node predDelegate = null;
				while (iterator.hasNext()) {
					Node currNode = iterator.next();
					if (predDelegate == null) {
						predDelegate = currNode;
					} else {
						if (currNode.firstOperation() != null && predDelegate.lastOperation() != null && !currNode
								.firstOperation().getChapter().equals(predDelegate.lastOperation().getChapter())) {
							currNode.setPredsInterProcedural(predDelegate.getId());
							predDelegate = currNode;
						}
					}
				}

				// Remember id of elements to be removed
				analysisElement.getControlFlowGraph().getNodes().values()
						.forEach(node -> cfgNodes.put(node.getId(), node));
				ids.add(firstNode.getParentElement().getBaseElement().getId());
			} else {
				// In case we have start event that maps a message to a method
				final LinkedHashMap<String, ProcessVariableOperation> initialOperations = new LinkedHashMap<>();
				analysisElement.getOperations().values().forEach(operation -> {
					if (operation.getFieldType().equals(KnownElementFieldType.Initial)) {
						initialOperations.put(operation.getId(), operation);
					}
				});
				analysisElement.setInUsed(initialOperations);

				// In case we have a call activity, pass variable mappings
				final LinkedHashMap<String, ProcessVariableOperation> camundaIn = new LinkedHashMap<>();
				final LinkedHashMap<String, ProcessVariableOperation> camundaOut = new LinkedHashMap<>();
				final ArrayList<ProcessVariableOperation> operationList = new ArrayList<>();
				if (analysisElement.getBaseElement() instanceof CallActivity) {
					for (AnalysisElement succ : analysisElement.getSuccessors()) {
						if (succ.getBaseElement() instanceof StartEvent) {
							analysisElement.getOperations().values().forEach(operation -> {

								if (operation.getFieldType().equals(KnownElementFieldType.CamundaIn)) {
									camundaIn.put(operation.getId(), operation);
									operationList.add(operation);
								}
								if (operation.getFieldType().equals(KnownElementFieldType.CamundaOut)) {
									camundaOut.put(operation.getId(), operation);
									operationList.add(operation);
								}
							});
							succ.setInUnused(camundaIn);
						} else {
							analysisElement.removeSuccessor(succ.getId());
							succ.removePredecessor(analysisElement.getId());
						}
					}
					analysisElement.setOutUnused(camundaOut);
				}

				operationList.forEach(analysisElement::removeOperation);

				if (analysisElement.getBaseElement() instanceof StartEvent) {
					for (AnalysisElement pred : analysisElement.getPredecessors()) {
						if (pred.getBaseElement() instanceof EndEvent) {
							analysisElement.removePredecessor(pred.getId());
						}
					}
				}

				if (analysisElement.getBaseElement() instanceof EndEvent) {
					for (AnalysisElement succ : analysisElement.getSuccessors()) {
						if (succ.getBaseElement() instanceof StartEvent) {
							analysisElement.removeSuccessor(succ.getId());
						}
					}
				}
			}
		});

		temp.putAll(cfgNodes);
		nodes.putAll(temp);
		ids.forEach(id -> nodes.remove(id));
	}

	/**
	 * Uses the approach from ALSU07 (Reaching Definitions) to compute data flow
	 * anomalies across the embedded CFG
	 */
	private void computeReachingDefinitions() {
		boolean change = true;
		while (change) {
			change = false;
			for (AnalysisElement analysisElement : nodes.values()) {
				// Calculate in-sets (intersection of predecessors)
				final LinkedHashMap<String, ProcessVariableOperation> inUsed = analysisElement.getInUsed();
				final LinkedHashMap<String, ProcessVariableOperation> inUnused = analysisElement.getInUnused();
				for (AnalysisElement pred : analysisElement.getPredecessors()) {
					inUsed.putAll(pred.getOutUsed());
					inUnused.putAll(pred.getOutUnused());
				}
				analysisElement.setInUsed(inUsed);
				analysisElement.setInUnused(inUnused);

				// Get old values before calculating new values and later check for changes
				final LinkedHashMap<String, ProcessVariableOperation> oldOutUnused = analysisElement.getOutUnused();
				final LinkedHashMap<String, ProcessVariableOperation> oldOutUsed = analysisElement.getOutUsed();

				// Calculate out-sets for used definitions (transfer functions)
				final LinkedHashMap<String, ProcessVariableOperation> tempUnion = new LinkedHashMap<>();
				tempUnion.putAll(analysisElement.getInUsed());
				tempUnion.putAll(getIntersection(analysisElement.getInUnused(), analysisElement.getUsed()));
				tempUnion.putAll(getIntersection(analysisElement.getDefined(), analysisElement.getUsed()));
				final LinkedHashMap<String, ProcessVariableOperation> outUsed = new LinkedHashMap<>(
						getSetDifference(tempUnion, analysisElement.getKilled()));

				// Calculate out-sets for unused definitions (transfer functions)
				final LinkedHashMap<String, ProcessVariableOperation> tempUnion2 = new LinkedHashMap<>();
				tempUnion2.putAll(analysisElement.getDefined());
				tempUnion2.putAll(analysisElement.getInUnused());
				final LinkedHashMap<String, ProcessVariableOperation> tempIntersection = new LinkedHashMap<>(
						getSetDifference(tempUnion2, analysisElement.getKilled()));
				final LinkedHashMap<String, ProcessVariableOperation> tempIntersection2 = new LinkedHashMap<>(
						getSetDifference(tempIntersection, analysisElement.getUsed()));
				final LinkedHashMap<String, ProcessVariableOperation> tempIntersection3 = new LinkedHashMap<>(
						getSetDifference(tempIntersection2, analysisElement.getKilled()));
				final LinkedHashMap<String, ProcessVariableOperation> outUnused = new LinkedHashMap<>(
						tempIntersection3);

				// If the current element contains input mapping operations, remove from
				// outgoing sets due to scope (only locally accessible)
				final LinkedHashMap<String, ProcessVariableOperation> tempOutUnused = new LinkedHashMap<>(outUnused);
				final LinkedHashMap<String, ProcessVariableOperation> tempOutUsed = new LinkedHashMap<>(outUsed);
				analysisElement.getParentElement().getOperations().forEach((key, value) -> {
					if (value.getScopeId().equals(analysisElement.getParentElement().getId())) {
						tempOutUnused.forEach((key1, value1) -> {
							if (value1.getName().equals(value.getName())) {
								outUnused.remove(key1);
							}
						});
						tempOutUsed.forEach((key1, value1) -> {
							if (value1.getName().equals(value.getName())) {
								outUsed.remove(key1);
							}
						});
						scopedOperations.put(value.getName(), value);
					}
				});

				Stream<ProcessVariableOperation> operations = analysisElement.getOperations().values().stream()
						.filter(value -> scopedOperations.containsKey(value.getName()))
						.filter(operation -> operation.getOperation().equals(WRITE));
				operations.forEach(operation -> {
					scopedOperations.remove(operation.getName());
					outUnused.put(operation.getId(), operation);
				});

				analysisElement.setOutUsed(outUsed);
				analysisElement.setOutUnused(outUnused);

				if (!oldOutUnused.equals(outUnused) || !oldOutUsed.equals(outUsed)) {
					change = true;
				}
			}
		}
	}

	/**
	 * Finds anomalies inside blocks by checking statements unit by unit
	 */
	private void computeLineByLine() {
		nodes.values().forEach(analysisElement -> {
			if (analysisElement.getOperations().size() >= 2) {
				ProcessVariableOperation prev = null;
				for (ProcessVariableOperation operation : analysisElement.getOperations().values()) {
					if (prev == null) {
						prev = operation;
						continue;
					}
					checkAnomaly(operation.getElement(), operation, prev);
					prev = operation;
				}
			}
		});
	}

	/**
	 * Based on the calculated sets, extract the anomalies found on source code leve
	 *
	 */
	private void extractAnomalies() {
		nodes.values().forEach(node -> {
			ddAnomalies(node);

			duAnomalies(node);

			urAnomalies(node);

			uuAnomalies(node);
		});
	}

	/**
	 * Extract DD anomalies
	 *
	 * @param node
	 *            Current node
	 */
	private void ddAnomalies(final AnalysisElement node) {
		final LinkedHashMap<String, ProcessVariableOperation> ddAnomalies = new LinkedHashMap<>(
				getIntersection(node.getInUnused(), node.getDefined()));
		if (!ddAnomalies.isEmpty()) {
			ddAnomalies.forEach((k, v) -> node
					.addSourceCodeAnomaly(new AnomalyContainer(v.getName(), Anomaly.DD, node.getId(), v)));
		}
	}

	/**
	 * Extract DU anomalies
	 *
	 * @param node
	 *            Current node
	 */
	private void duAnomalies(final AnalysisElement node) {
		final LinkedHashMap<String, ProcessVariableOperation> duAnomalies = new LinkedHashMap<>(
				getIntersection(node.getInUnused(), node.getKilled()));
		if (!duAnomalies.isEmpty()) {
			duAnomalies.forEach((k, v) -> node
					.addSourceCodeAnomaly(new AnomalyContainer(v.getName(), Anomaly.DU, node.getId(), v)));
		}
	}

	/**
	 * Extract UR anomalies
	 *
	 * @param node
	 *            Current node
	 */
	private void urAnomalies(final AnalysisElement node) {
		final LinkedHashMap<String, ProcessVariableOperation> urAnomaliesTemp = new LinkedHashMap<>(node.getUsed());
		final LinkedHashMap<String, ProcessVariableOperation> urAnomalies = new LinkedHashMap<>(urAnomaliesTemp);

		urAnomaliesTemp.forEach((key, value) -> node.getInUnused().forEach((key2, value2) -> {
			if (value.getName().equals(value2.getName())) {
				urAnomalies.remove(key);
			}
		}));

		urAnomaliesTemp.forEach((key, value) -> node.getInUsed().forEach((key2, value2) -> {
			if (value.getName().equals(value2.getName())) {
				urAnomalies.remove(key);
			}
		}));

		urAnomaliesTemp.forEach((key, value) -> node.getDefined().forEach((key2, value2) -> {
			if (value.getName().equals(value2.getName())) {
				if (value.getIndex() > value2.getIndex()) {
					urAnomalies.remove(key);
				}
			}
		}));

		if (!urAnomalies.isEmpty()) {
			urAnomalies.forEach((k, v) -> node
					.addSourceCodeAnomaly(new AnomalyContainer(v.getName(), Anomaly.UR, node.getId(), v)));
		}
	}

	/**
	 * Extract UU anomalies
	 *
	 * @param node
	 *            Current node
	 */
	private void uuAnomalies(final AnalysisElement node) {
		final LinkedHashMap<String, ProcessVariableOperation> uuAnomaliesTemp = new LinkedHashMap<>(node.getKilled());
		final LinkedHashMap<String, ProcessVariableOperation> uuAnomalies = new LinkedHashMap<>(uuAnomaliesTemp);

		uuAnomaliesTemp.forEach((key, value) -> node.getInUnused().forEach((key2, value2) -> {
			if (value.getName().equals(value2.getName())) {
				uuAnomalies.remove(key);
			}
		}));

		uuAnomaliesTemp.forEach((key, value) -> node.getInUsed().forEach((key2, value2) -> {
			if (value.getName().equals(value2.getName())) {
				uuAnomalies.remove(key);
			}
		}));

		uuAnomaliesTemp.forEach((key, value) -> node.getDefined().forEach((key2, value2) -> {
			if (value.getName().equals(value2.getName())) {
				if (value.getIndex() > value2.getIndex()) {
					uuAnomalies.remove(key);
				}
			}
		}));

		if (!uuAnomalies.isEmpty()) {
			uuAnomalies.forEach((k, v) -> node
					.addSourceCodeAnomaly(new AnomalyContainer(v.getName(), Anomaly.UU, node.getId(), v)));
		}
	}

	/**
	 * Check for data-flow anomaly between current and previous variable operation
	 *
	 * @param element
	 *            Current BpmnElement
	 * @param curr
	 *            current operation
	 * @param prev
	 *            previous operation
	 */
	private void checkAnomaly(final BpmnElement element, final ProcessVariableOperation curr,
			final ProcessVariableOperation prev) {
		if (urSourceCode(prev, curr)) {
			element.addSourceCodeAnomaly(
					new AnomalyContainer(curr.getName(), Anomaly.UR, element.getBaseElement().getId(), curr));
		}
		if (ddSourceCode(prev, curr)) {
			element.addSourceCodeAnomaly(
					new AnomalyContainer(curr.getName(), Anomaly.DD, element.getBaseElement().getId(), curr));
		}
		if (duSourceCode(prev, curr)) {
			element.addSourceCodeAnomaly(
					new AnomalyContainer(curr.getName(), Anomaly.DU, element.getBaseElement().getId(), curr));
		}
		if (uuSourceCode(prev, curr)) {
			element.addSourceCodeAnomaly(
					new AnomalyContainer(curr.getName(), Anomaly.UU, element.getBaseElement().getId(), curr));
		}
	}

	/**
	 * UU anomaly: second last operation of PV is DELETE, last operation is DELETE
	 *
	 * @param prev
	 *            Previous ProcessVariable
	 * @param curr
	 *            Current ProcessVariable
	 * @return true/false
	 */
	private boolean uuSourceCode(ProcessVariableOperation prev, ProcessVariableOperation curr) {
		return curr.getOperation().equals(DELETE) && prev.getOperation().equals(DELETE);
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
		return curr.getOperation().equals(READ) && prev.getOperation().equals(DELETE);
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
		return curr.getOperation().equals(WRITE) && prev.getOperation().equals(WRITE);
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
		return curr.getOperation().equals(DELETE) && prev.getOperation().equals(WRITE);
	}

	/**
	 * Helper method to create the set difference of two given maps (based on
	 * variable names)
	 *
	 * @param mapOne
	 *            First map
	 * @param mapTwo
	 *            Second map
	 * @return Set difference of given maps
	 */
	private LinkedHashMap<String, ProcessVariableOperation> getSetDifference(
			final LinkedHashMap<String, ProcessVariableOperation> mapOne,
			final LinkedHashMap<String, ProcessVariableOperation> mapTwo) {
		final LinkedHashMap<String, ProcessVariableOperation> setDifference = new LinkedHashMap<>(mapOne);

		mapOne.forEach((key, value) -> mapTwo.forEach((key2, value2) -> {
			if (value.getName().equals(value2.getName())) {
				setDifference.remove(key);
			}
		}));
		return setDifference;
	}

	/**
	 * Helper method to create the intersection of two given maps
	 *
	 * @param mapOne
	 *            First map
	 * @param mapTwo
	 *            Second map
	 * @return Intersection of given maps
	 */
	private LinkedHashMap<String, ProcessVariableOperation> getIntersection(
			final LinkedHashMap<String, ProcessVariableOperation> mapOne,
			final LinkedHashMap<String, ProcessVariableOperation> mapTwo) {
		final LinkedHashMap<String, ProcessVariableOperation> intersection = new LinkedHashMap<>();

		mapOne.forEach((key, value) -> mapTwo.forEach((key2, value2) -> {
			if (value.getName().equals(value2.getName())) {
				intersection.put(key, value);
			}
		}));
		return intersection;
	}

	public LinkedHashMap<String, AnalysisElement> getNodes() {
		return nodes;
	}

	public int getOperationCounter() {
		return operationCounter;
	}

	public void incrementOperationCounter() {
		this.operationCounter++;
	}

}
