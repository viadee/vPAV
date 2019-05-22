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

import java.util.Iterator;
import java.util.LinkedHashMap;

public class ControlFlowGraph {

	private LinkedHashMap<String, Node> nodes;

	private LinkedHashMap<String, ProcessVariableOperation> operations;

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

//		for (Map.Entry<String, Node> entry : nodes.entrySet()) {
//            if (entry.getValue().getFilePath().equals(node.getFilePath())) {
//                node.setPredsIntraProcedural(entry.getKey());
//            }
//        }
//		this.nodes.forEach((key1, value) -> {
//			if (value.getParentElement().getBaseElement().getId().equals(node.getParentElement().getBaseElement().getId())) {
//				node.setPredsIntraProcedural(key1);
////				node.incrementDelegateCount();
//			}
//		});
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
        computePredecessorRelations();
		computeReachingDefinitions();
		computeLineByLine(element);
		extractAnomalies(element);
	}

    /**
     * Set predecessor/successor relations for blocks and initialize sets
     */
    void computePredecessorRelations() {
        nodes.values().forEach(node -> {
            this.operations.putAll(node.getOperations());
            node.setPreds();
            node.setSuccs();
            node.setOutUnused(new LinkedHashMap<>());
            node.setOutUsed(new LinkedHashMap<>());
        });
    }

    /**
     * Uses the approach from ALSU07 (Reaching Definitions) to compute data flow
     * anomalies across the CFG
     */
    private void computeReachingDefinitions() {
        boolean change = true;
        while (change) {
            change = false;
            for (Node node : nodes.values()) {
                // Calculate in-sets (intersection of predecessors)
                final LinkedHashMap<String, ProcessVariableOperation> inUsed = node.getInUsed();
                final LinkedHashMap<String, ProcessVariableOperation> inUnused = node
                        .getInUnused();
                for (Node pred : node.getNodePredecessors()) {
                    inUsed.putAll(pred.getOutUsed());
                    inUnused.putAll(pred.getOutUnused());
                }
                node.setInUsed(inUsed);
                node.setInUnused(inUnused);

                // Calculate out-sets for used definitions (transfer functions)
                final LinkedHashMap<String, ProcessVariableOperation> outUsed = new LinkedHashMap<>();
                outUsed.putAll(node.getUsed());
                outUsed.putAll(getSetDifference(node.getInUsed(), node.getKilled()));
                node.setOutUsed(outUsed);

                // Calculate out-sets for unused definitions (transfer functions)
                final LinkedHashMap<String, ProcessVariableOperation> outUnused = new LinkedHashMap<>(
                        node.getDefined());
                final LinkedHashMap<String, ProcessVariableOperation> tempIntersection = new LinkedHashMap<>();
                tempIntersection.putAll(getSetDifference(node.getInUnused(), node.getKilled()));
                tempIntersection.putAll(getSetDifference(tempIntersection, node.getUsed()));
                outUnused.putAll(tempIntersection);
                node.setOutUnused(outUnused);

                // Compare old values with new values and check for changes
                final LinkedHashMap<String, ProcessVariableOperation> oldOutUnused = node
                        .getOutUnused();
                final LinkedHashMap<String, ProcessVariableOperation> oldOutUsed = node
                        .getOutUsed();

                if (!oldOutUnused.equals(outUnused) || !oldOutUsed.equals(outUsed)) {
                    change = true;
                }
            }
        }
    }


    /**
	 * Finds anomalies inside blocks by checking statements unit by unit
     * @param element
     *            Current BpmnElement
	 */
	private void computeLineByLine(final BpmnElement element) {
		getNodes().values().forEach(node -> {
			if (node.getOperations().size() >= 2) {
				ProcessVariableOperation prev = null;
				for (ProcessVariableOperation operation : node.getOperations().values()) {
					if (prev == null) {
						prev = operation;
						continue;
					}
					checkAnomaly(element, operation, prev);
					prev = operation;
				}
			}
		});
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
//			nopRAnomalies(element, node);

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
        final LinkedHashMap<String, ProcessVariableOperation> ddAnomalies = new LinkedHashMap<>(
                getIntersection(node.getInUnused(), node.getDefined()));
        if (!ddAnomalies.isEmpty()) {
            ddAnomalies.forEach((k, v) -> element.addSourceCodeAnomaly(
                    new AnomalyContainer(v.getName(), Anomaly.DD, element.getBaseElement().getId(), v)));
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
        final LinkedHashMap<String, ProcessVariableOperation> duAnomalies = new LinkedHashMap<>(
                getIntersection(node.getInUnused(), node.getKilled()));
        if (!duAnomalies.isEmpty()) {
            duAnomalies.forEach((k, v) -> element.addSourceCodeAnomaly(
                    new AnomalyContainer(v.getName(), Anomaly.DU, element.getBaseElement().getId(), v)));
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
		final LinkedHashMap<String, ProcessVariableOperation> urAnomaliesTemp = new LinkedHashMap<>(
				node.getUsed());
		final LinkedHashMap<String, ProcessVariableOperation> urAnomalies = new LinkedHashMap<>(
				urAnomaliesTemp);

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

		if (!urAnomalies.isEmpty()) {
			urAnomalies.forEach((k, v) -> element.addSourceCodeAnomaly(
					new AnomalyContainer(v.getName(), Anomaly.UR, element.getBaseElement().getId(), v)));
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
        final LinkedHashMap<String, ProcessVariableOperation> uuAnomaliesTemp = new LinkedHashMap<>(
                node.getKilled());
        final LinkedHashMap<String, ProcessVariableOperation> uuAnomalies = new LinkedHashMap<>(
                uuAnomaliesTemp);

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
                if (Integer.parseInt(key) > Integer.parseInt(key2)) {
                    uuAnomalies.remove(key);
                }
            }
        }));


        if (!uuAnomalies.isEmpty()) {
            uuAnomalies.forEach((k, v) -> element.addSourceCodeAnomaly(
                    new AnomalyContainer(v.getName(), Anomaly.UU, element.getBaseElement().getId(), v)));
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
        final LinkedHashMap<String, ProcessVariableOperation> dNopAnomaliesTemp = new LinkedHashMap<>(
                node.getInUnused());
        dNopAnomaliesTemp.putAll(node.getInUsed());
        final LinkedHashMap<String, ProcessVariableOperation> dNopAnomalies = new LinkedHashMap<>(
                dNopAnomaliesTemp);

        dNopAnomaliesTemp.forEach((key, value) -> node.getDefined().forEach((key2, value2) -> {
            if (value.getName().equals(value2.getName())) {
                dNopAnomalies.remove(key);
            }
        }));

        dNopAnomaliesTemp.forEach((key, value) -> node.getKilled().forEach((key2, value2) -> {
            if (value.getName().equals(value2.getName())) {
                dNopAnomalies.remove(key);
            }
        }));

        dNopAnomaliesTemp.forEach((key, value) -> node.getUsed().forEach((key2, value2) -> {
            if (value.getName().equals(value2.getName())) {
                dNopAnomalies.remove(key);
            }
        }));

        if (!dNopAnomalies.isEmpty()) {
            dNopAnomalies.forEach((k, v) -> element.addSourceCodeAnomaly(
                    new AnomalyContainer(v.getName(), Anomaly.D, element.getBaseElement().getId(), v)));
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
	private LinkedHashMap<String, ProcessVariableOperation> getSetDifference(
			final LinkedHashMap<String, ProcessVariableOperation> mapOne,
			final LinkedHashMap<String, ProcessVariableOperation> mapTwo) {
		final LinkedHashMap<String, ProcessVariableOperation> setDifference = new LinkedHashMap<>(
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

	boolean hasNodes () { return !nodes.isEmpty(); }

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

	Node firstNode() {
		Iterator<Node> iterator = nodes.values().iterator();
		return iterator.next();
	}

	Node lastNode() {
		Iterator<Node> iterator = nodes.values().iterator();
		Node node = null;
		while (iterator.hasNext()) {
			node = iterator.next();
		}
		return node;
	}
}
