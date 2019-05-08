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

import de.viadee.bpm.vPAV.processing.model.data.*;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.BitSet;
import java.util.LinkedHashMap;

public class ControlFlowGraph {

	private LinkedHashMap<String, Node> nodes;

	private LinkedHashMap<Integer, ImmutablePair<BitSet, ProcessVariableOperation>> operations;

	private int internalNodeCounter;

	private int recursionCounter;

	private int nodeCounter;

	private int defCounter;

	private int priorLevel;

	public ControlFlowGraph() {
		nodes = new LinkedHashMap<>();
		this.operations = new LinkedHashMap<>();
		defCounter = 0;
		nodeCounter = -1;
		internalNodeCounter = 0;
		recursionCounter = 0;
		priorLevel = 0;
	}

	/**
	 * Adds a node to the current CFG
	 *
	 * @param node
	 *            Node to be added to the control flow graph
	 */
	public void addNode(final Node node) {
		String key = createHierarchy(node);
		node.setId(key);
		this.nodes.put(key, node);
	}

	/**
	 *
	 * Helper method to create ids based on hierarchy, e.g.
	 *
	 * 0 --> 1 --> 2 --> 3 --> 3.0 --> 3.0.0 --> 3.0.1 --> 3.0.2
	 * 
	 * @return Id of node
	 */
	private String createHierarchy(final Node node) {
		StringBuilder key = new StringBuilder();
		if (recursionCounter == 0) {
			nodeCounter++;
			key.append(nodeCounter);
		} else {
			key.append(nodeCounter);
			for (int i = 1; i <= recursionCounter; i++) {
				key.append(".");
				if (i == recursionCounter) {
					key.append(internalNodeCounter);
					String predKey = key.toString();
					if (internalNodeCounter == 0) {
						node.setPredsIntraProcedural(predKey.substring(0, predKey.length() - 2));
					}
				} else {
					key.append(priorLevel);
				}
			}
			internalNodeCounter++;
		}
		return key.toString();
	}

	/**
	 * Method used to start the data flow analysis by checking the found process
	 * variable operations. First checks for anomalies inside each block (i.e. unit
	 * by unit), subsequently calculates the reaching definitions
	 *
	 * @param element
	 *            Current BpmnElement
	 */
	public void analyze(final BpmnElement element) {
		computeReachingDefinitions();
		computeLineByLine(element);
		extractAnomalies(element);
	}
	/**
	 * Finds anomalies inside blocks by checking statements unit by unit
     * @param element
     *            Current BpmnElement
	 */
	private void computeLineByLine(final BpmnElement element) {
		for (final Node node : getNodes().values()) {
			if (node.getOperations().size() >= 2) {
				ProcessVariableOperation prev = null;
				for (ImmutablePair<BitSet, ProcessVariableOperation> operation : node.getOperations().values()) {
					ProcessVariableOperation curr = operation.getRight();
					if (prev == null) {
						prev = curr;
						continue;
					}
					checkAnomaly(element, curr, prev);
					prev = curr;
				}
			}
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
		return curr.getOperation().equals(VariableOperation.DELETE)
				&& prev.getOperation().equals(VariableOperation.DELETE);
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
		return curr.getOperation().equals(VariableOperation.READ)
				&& prev.getOperation().equals(VariableOperation.DELETE);
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
		return curr.getOperation().equals(VariableOperation.WRITE)
				&& prev.getOperation().equals(VariableOperation.WRITE);
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
		return curr.getOperation().equals(VariableOperation.DELETE)
				&& prev.getOperation().equals(VariableOperation.WRITE);
	}

	/**
	 * Uses the approach from ALSU07 (Reaching Definitions) to compute data flow
	 * anomalies across the CFG
	 */
	private void computeReachingDefinitions() {
		// Set predecessor/successor relations and initialize sets
		nodes.values().forEach(node -> {
			this.operations.putAll(node.getOperations());
			node.setPreds();
			node.setSuccs();
			node.setOutUnused(new LinkedHashMap<>());
			node.setOutUsed(new LinkedHashMap<>());
		});

		boolean change = true;
		while (change) {
			change = false;
			for (Node node : nodes.values()) {
				// Calculate in-sets (intersection of predecessors)
				final LinkedHashMap<Integer, ImmutablePair<BitSet, ProcessVariableOperation>> inUsed = node.getInUsed();
				final LinkedHashMap<Integer, ImmutablePair<BitSet, ProcessVariableOperation>> inUnused = node
						.getInUnused();
				for (Node pred : node.getPredecessors()) {
					inUsed.putAll(pred.getOutUsed());
					inUnused.putAll(pred.getOutUnused());
				}
				node.setInUsed(inUsed);
				node.setInUnused(inUnused);

				// Calculate out-sets for used definitions (transfer functions)
				final LinkedHashMap<Integer, ImmutablePair<BitSet, ProcessVariableOperation>> outUsed = new LinkedHashMap<>();
				outUsed.putAll(node.getUsed());
				outUsed.putAll(getSetDifference(node.getInUsed(), node.getKilled()));
				node.setOutUsed(outUsed);

				// Calculate out-sets for unused definitions (transfer functions)
				final LinkedHashMap<Integer, ImmutablePair<BitSet, ProcessVariableOperation>> outUnused = new LinkedHashMap<>(
						node.getDefined());
				final LinkedHashMap<Integer, ImmutablePair<BitSet, ProcessVariableOperation>> tempIntersection = new LinkedHashMap<>();
				tempIntersection.putAll(getSetDifference(node.getInUnused(), node.getKilled()));
				tempIntersection.putAll(getSetDifference(tempIntersection, node.getUsed()));
				outUnused.putAll(tempIntersection);
				node.setOutUnused(outUnused);

				// Compare old values with new values and check for changes
				final LinkedHashMap<Integer, ImmutablePair<BitSet, ProcessVariableOperation>> oldOutUnused = node
						.getOutUnused();
				final LinkedHashMap<Integer, ImmutablePair<BitSet, ProcessVariableOperation>> oldOutUsed = node
						.getOutUsed();

				if (!oldOutUnused.equals(outUnused) || !oldOutUsed.equals(outUsed)) {
					change = true;
				}
			}
		}
	}

	/**
	 * Based on the calculated sets, extract the anomalies found on source code
	 * level
	 * 
	 * @param element
	 *            Current BpmnElement
	 */
	private void extractAnomalies(final BpmnElement element) {
		nodes.values().forEach(node -> {
			// DD (inUnused U defined)
			ddAnomalies(element, node);

			// DU (inUnused U killed)
			duAnomalies(element, node);

			// UR (used - inUnused - inUsed - defined)
			urAnomalies(element, node);

			// UU ()
			uuAnomalies(element, node);

            // -R ()
			nopRAnomalies(element, node);

			// D- (inUnused U inUsed - (defined - killed - used))
//			dNopAnomalies(element, node);

		});
	}

    /**
     * Extract DD anomalies
     *
     * @param element
     *            Current BpmnElement
     * @param node
     *            Current node
     */
    private void ddAnomalies(final BpmnElement element, final Node node) {
        final LinkedHashMap<Integer, ImmutablePair<BitSet, ProcessVariableOperation>> ddAnomalies = new LinkedHashMap<>(
                getIntersection(node.getInUnused(), node.getDefined()));
        if (!ddAnomalies.isEmpty()) {
            ddAnomalies.forEach((k, v) -> element.addSourceCodeAnomaly(
                    new AnomalyContainer(v.right.getName(), Anomaly.DD, element.getBaseElement().getId(), v.right)));
        }
    }

    /**
     * Extract DU anomalies
     *
     * @param element
     *            Current BpmnElement
     * @param node
     *            Current node
     */
    private void duAnomalies(final BpmnElement element, final Node node) {
        final LinkedHashMap<Integer, ImmutablePair<BitSet, ProcessVariableOperation>> duAnomalies = new LinkedHashMap<>(
                getIntersection(node.getInUnused(), node.getKilled()));
        if (!duAnomalies.isEmpty()) {
            duAnomalies.forEach((k, v) -> element.addSourceCodeAnomaly(
                    new AnomalyContainer(v.right.getName(), Anomaly.DU, element.getBaseElement().getId(), v.right)));
        }
    }

	/**
	 * Extract UR anomalies
	 *
	 * @param element
	 *            Current BpmnElement
	 * @param node
	 *            Current node
	 */
	private void urAnomalies(final BpmnElement element, final Node node) {
		final LinkedHashMap<Integer, ImmutablePair<BitSet, ProcessVariableOperation>> urAnomaliesTemp = new LinkedHashMap<>(
				node.getUsed());
		final LinkedHashMap<Integer, ImmutablePair<BitSet, ProcessVariableOperation>> urAnomalies = new LinkedHashMap<>(
				urAnomaliesTemp);

		urAnomaliesTemp.forEach((key, value) -> node.getInUnused().forEach((key2, value2) -> {
			if (value.getRight().getName().equals(value2.getRight().getName())) {
				urAnomalies.remove(key);
			}
		}));

		urAnomaliesTemp.forEach((key, value) -> node.getInUsed().forEach((key2, value2) -> {
			if (value.getRight().getName().equals(value2.getRight().getName())) {
				urAnomalies.remove(key);
			}
		}));

		urAnomaliesTemp.forEach((key, value) -> node.getDefined().forEach((key2, value2) -> {
			if (value.getRight().getName().equals(value2.getRight().getName())) {
				if (key < key2) {
					urAnomalies.remove(key);
				}
			}
		}));

		if (!urAnomalies.isEmpty()) {
			urAnomalies.forEach((k, v) -> element.addSourceCodeAnomaly(
					new AnomalyContainer(v.right.getName(), Anomaly.UR, element.getBaseElement().getId(), v.right)));
		}
	}

    /**
     * Extract UU anomalies
     *
     * @param element
     *            Current BpmnElement
     * @param node
     *            Current node
     */
    private void uuAnomalies(BpmnElement element, Node node) {
        final LinkedHashMap<Integer, ImmutablePair<BitSet, ProcessVariableOperation>> urAnomaliesTemp = new LinkedHashMap<>(
                node.getKilled());
        final LinkedHashMap<Integer, ImmutablePair<BitSet, ProcessVariableOperation>> uuAnomalies = new LinkedHashMap<>(
                urAnomaliesTemp);

        urAnomaliesTemp.forEach((key, value) -> node.getInUnused().forEach((key2, value2) -> {
            if (value.getRight().getName().equals(value2.getRight().getName())) {
                uuAnomalies.remove(key);
            }
        }));

        urAnomaliesTemp.forEach((key, value) -> node.getInUsed().forEach((key2, value2) -> {
            if (value.getRight().getName().equals(value2.getRight().getName())) {
                uuAnomalies.remove(key);
            }
        }));

        urAnomaliesTemp.forEach((key, value) -> node.getDefined().forEach((key2, value2) -> {
            if (value.getRight().getName().equals(value2.getRight().getName())) {
                if (key < key2) {
                    uuAnomalies.remove(key);
                }
            }
        }));


        if (!uuAnomalies.isEmpty()) {
            uuAnomalies.forEach((k, v) -> element.addSourceCodeAnomaly(
                    new AnomalyContainer(v.right.getName(), Anomaly.UU, element.getBaseElement().getId(), v.right)));
        }

    }

    /**
     * Extract -R anomalies
     *
     * @param element
     *            Current BpmnElement
     * @param node
     *            Current node
     */
    private void nopRAnomalies(BpmnElement element, Node node) {

    }

    /**
     * Extract D- anomalies
     *
     * @param element
     *            Current BpmnElement
     * @param node
     *            Current node
     */
    private void dNopAnomalies(final BpmnElement element, final Node node) {
        final LinkedHashMap<Integer, ImmutablePair<BitSet, ProcessVariableOperation>> dNopAnomaliesTemp = new LinkedHashMap<>(
                node.getInUnused());
        dNopAnomaliesTemp.putAll(node.getInUsed());
        final LinkedHashMap<Integer, ImmutablePair<BitSet, ProcessVariableOperation>> dNopAnomalies = new LinkedHashMap<>(
                dNopAnomaliesTemp);

        dNopAnomaliesTemp.forEach((key, value) -> node.getDefined().forEach((key2, value2) -> {
            if (value.getRight().getName().equals(value2.getRight().getName())) {
                dNopAnomalies.remove(key);
            }
        }));

        dNopAnomaliesTemp.forEach((key, value) -> node.getKilled().forEach((key2, value2) -> {
            if (value.getRight().getName().equals(value2.getRight().getName())) {
                dNopAnomalies.remove(key);
            }
        }));

        dNopAnomaliesTemp.forEach((key, value) -> node.getUsed().forEach((key2, value2) -> {
            if (value.getRight().getName().equals(value2.getRight().getName())) {
                dNopAnomalies.remove(key);
            }
        }));

        if (!dNopAnomalies.isEmpty()) {
            dNopAnomalies.forEach((k, v) -> element.addSourceCodeAnomaly(
                    new AnomalyContainer(v.right.getName(), Anomaly.D, element.getBaseElement().getId(), v.right)));
        }
    }

	/**
	 * Helper method to create the set difference of two given maps (based on variable names)
	 *
	 * @param mapOne
	 *            First map
	 * @param mapTwo
	 *            Second map
	 * @return Set difference of given maps
	 */
	private LinkedHashMap<Integer, ImmutablePair<BitSet, ProcessVariableOperation>> getSetDifference(
			final LinkedHashMap<Integer, ImmutablePair<BitSet, ProcessVariableOperation>> mapOne,
			final LinkedHashMap<Integer, ImmutablePair<BitSet, ProcessVariableOperation>> mapTwo) {
		final LinkedHashMap<Integer, ImmutablePair<BitSet, ProcessVariableOperation>> setDifference = new LinkedHashMap<>(
				mapOne);

		mapOne.keySet().forEach(key -> {
			if (mapTwo.containsKey(key)) {
				setDifference.remove(key);
			}
		});
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
	private LinkedHashMap<Integer, ImmutablePair<BitSet, ProcessVariableOperation>> getIntersection(
			final LinkedHashMap<Integer, ImmutablePair<BitSet, ProcessVariableOperation>> mapOne,
			final LinkedHashMap<Integer, ImmutablePair<BitSet, ProcessVariableOperation>> mapTwo) {
		final LinkedHashMap<Integer, ImmutablePair<BitSet, ProcessVariableOperation>> intersection = new LinkedHashMap<>();

        mapOne.forEach((key, value) -> mapTwo.forEach((key2, value2) -> {
            if (value.getRight().getName().equals(value2.getRight().getName())) {
                intersection.put(key, value);
            }
        }));
		return intersection;
	}

	int getDefCounter() {
		return defCounter;
	}

	void incrementDefCounter() {
		this.defCounter++;
	}

	public void incrementRecursionCounter() {
		this.recursionCounter++;
	}

	public void decrementRecursionCounter() {
		this.recursionCounter--;
	}

	public void resetInternalNodeCounter() {
		this.internalNodeCounter = 0;
	}

	public int getInternalNodeCounter() {
		return internalNodeCounter;
	}

	public void setPriorLevel(int priorLevel) {
		this.priorLevel = priorLevel;
	}

	public LinkedHashMap<String, Node> getNodes() {
		return nodes;
	}

	public int getPriorLevel() {
		return priorLevel;
	}

	public void setInternalNodeCounter(int internalNodeCounter) {
		this.internalNodeCounter = internalNodeCounter;
	}
}
