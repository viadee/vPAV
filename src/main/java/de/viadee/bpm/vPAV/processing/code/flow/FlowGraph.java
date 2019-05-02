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

public class FlowGraph {

	public LinkedHashMap<String, Node> getNodes() {
		return nodes;
	}

	private LinkedHashMap<String, Node> nodes;

	private int internalNodeCounter;

	private int recursionCounter;

	private int nodeCounter;

	private int defCounter;

	private int priorLevel;

	public FlowGraph() {
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

	public void computeInAndOutSets() {
		createCFG();
		// for (final Node node : nodes) {
		// node.initOutUnused();
		// node.initOutUsed();
		// }
		// boolean change = true;
		// while (change) {
		// change = false;
		// for (final Node node : nodes) {
		// BitSet oldOutUnused = node.getOutUnused();
		// BitSet oldOutUsed = node.getOutUsed();
		// node.setInUnused();
		// node.setInUsed();
		// node.setOutUnused();
		// node.setOutUsed();
		// // IN(unused)[B] = (intersection of pred) OUT(unused)[P]
		// // IN(used)[B] = (intersection of pred) OUT(used)[P]
		// // OUT(unused)[B] = defined U (IN(unused) - killed - used)
		// // OUT(used)[B] = used U (in(used) - killed)
		// if (!oldOutUnused.equals(node.getOutUnused()) ||
		// !oldOutUsed.equals(node.getOutUsed())) {
		// change = true;
		// }
		// }
		// }

		// OUT[Entry] = {}
		// For (each basic block B other than ENTRY) {
		// OUT[B] = {}
		// }
		// change = true;
		// while (change) {
		// change = false;
		// for (each basic block B other than ENTRY) {
		// IN(unused)[B] = (intersection of pred) OUT(unused)[P]
		// IN(used)[B] = (intersection of pred) OUT(used)[P]
		// OUT(unused)[B] = defined U (IN(unused) - killed - used)
		// OUT(used)[B] = used U (in(used) - killed)
		// }
		// }
	}

	/**
	 * Constructs the links between nodes, which are needed to subsequently
	 * calculate the sets
	 */
	private void createCFG() {
		for (Node node : nodes.values()) {
			node.setPreds();
			node.setSuccs();
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
}
