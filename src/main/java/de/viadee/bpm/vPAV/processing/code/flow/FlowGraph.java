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

public class FlowGraph {

	private BpmnElement element;

	private LinkedHashMap<String, Node> nodes;

	private int internalNodeCounter;

	private int recursionCounter;

	private int nodeCounter;

	private int defCounter;

	private int priorLevel;

	public FlowGraph(final BpmnElement element) {
		this.element = element;
		nodes = new LinkedHashMap<>();
		defCounter = 0;
		nodeCounter = -1;
		internalNodeCounter = 0;
		recursionCounter = 0;
		priorLevel = 0;
	}

	/**
	 *
	 * @param node
	 *            Node to be added to the control flow graph
	 */
	public void addNode(final Node node) {
		String key = createHierarchy();
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
	private String createHierarchy() {
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
				} else {
					key.append(priorLevel);
				}
			}
			internalNodeCounter++;
		}
		return key.toString();
	}

	public void analyze() {
		computeLineByLine();
		// computeInAndOutSets();
	}

	private void computeLineByLine() {
		for (final Node node : getNodes().values()) {
			if (node.getOperations().size() >= 2) {
				ProcessVariableOperation prev = null;
				for (ImmutablePair operation : node.getOperations().values()) {
					ProcessVariableOperation curr = (ProcessVariableOperation) operation.getRight();
					if (prev == null) {
						prev = curr;
						continue;
					}
					checkAnomaly(element, curr, prev);
				}
			}
		}
	}

	private void checkAnomaly(final BpmnElement element, ProcessVariableOperation curr, ProcessVariableOperation prev) {
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

	public void computeInAndOutSets() {
        // Set predecessor/successor relations and initialize sets
        for (Node node : nodes.values()) {
            node.setPreds();
            node.setSuccs();
            node.setOutUnused(new BitSet());
            node.setOutUsed(new BitSet());
        }

        boolean change = true;
        while (change) {
            change = false;
            for (Node node : nodes.values()) {
                // Calculate in-sets (intersection of predecessors)
                BitSet inUnused = node.getInUnused();
                BitSet inUsed = node.getInUsed();
                for (Node pred : node.getPreds()) {
                    inUnused.and(pred.getOutUnused());
                    inUsed.and(pred.getOutUsed());
                }
                node.setInUnused(inUnused);
                node.setInUsed(inUsed);

                // Calculate out-sets for used definitions (transfer functions)
                BitSet oldOutUsed = node.getOutUsed();
                BitSet newOutUsed = node.getUsed();
                BitSet inUsedTemp = node.getInUsed();
                inUsedTemp.andNot(node.getKilled());
                newOutUsed.or(inUsedTemp);

                node.printBits(newOutUsed);

                // Calculate out-sets for unused definitions (transfer functions)
                BitSet oldOutUnused = node.getOutUnused();
                BitSet newOutUnused = node.getDefined();
                BitSet inUnusedTemp = node.getInUnused();
                inUnusedTemp.andNot(node.getKilled());
                inUnusedTemp.andNot(node.getUsed());
                newOutUnused.or(inUnusedTemp);

                node.printBits(newOutUnused);

                if (!oldOutUnused.equals(newOutUnused) || !oldOutUsed.equals(newOutUsed)) {
                    change = true;
                }
            }
        }
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
}
