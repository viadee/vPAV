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

import de.viadee.bpm.vPAV.processing.model.data.ProcessVariableOperation;
import org.apache.commons.lang3.tuple.ImmutablePair;
import soot.toolkits.graph.Block;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Node {

	private ControlFlowGraph controlFlowGraph;

	private LinkedHashMap<Integer, ImmutablePair<BitSet, ProcessVariableOperation>> operations;
	private LinkedHashMap<Integer, ImmutablePair<BitSet, ProcessVariableOperation>> defined;
	private LinkedHashMap<Integer, ImmutablePair<BitSet, ProcessVariableOperation>> used;
	private LinkedHashMap<Integer, ImmutablePair<BitSet, ProcessVariableOperation>> killed;
	private LinkedHashMap<Integer, ImmutablePair<BitSet, ProcessVariableOperation>> inUsed;
	private LinkedHashMap<Integer, ImmutablePair<BitSet, ProcessVariableOperation>> inUnused;
	private LinkedHashMap<Integer, ImmutablePair<BitSet, ProcessVariableOperation>> outUsed;
	private LinkedHashMap<Integer, ImmutablePair<BitSet, ProcessVariableOperation>> outUnused;

	private Block block;

	private List<Node> predecessors;
	private List<Node> successor;
	private String id;

	public Node(final ControlFlowGraph controlFlowGraph, final Block block) {
		this.controlFlowGraph = controlFlowGraph;
		this.block = block;
		this.predecessors = new ArrayList<>();
		this.successor = new ArrayList<>();

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
		this.operations.put(controlFlowGraph.getDefCounter(),
				new ImmutablePair<>(new BitSet(controlFlowGraph.getDefCounter()), processVariableOperation));
		switch (processVariableOperation.getOperation()) {
		case WRITE:
			defined.put(controlFlowGraph.getDefCounter(),
					new ImmutablePair<>(new BitSet(controlFlowGraph.getDefCounter()), processVariableOperation));
			break;
		case READ:
			used.put(controlFlowGraph.getDefCounter(),
					new ImmutablePair<>(new BitSet(controlFlowGraph.getDefCounter()), processVariableOperation));
			break;
		case DELETE:
			killed.put(controlFlowGraph.getDefCounter(),
					new ImmutablePair<>(new BitSet(controlFlowGraph.getDefCounter()), processVariableOperation));
			break;
		}
		controlFlowGraph.incrementDefCounter();
	}

	/**
	 * Set the predecessor nodes of the current node
	 */
	void setPreds() {
		final Pattern blockPattern = Pattern.compile("(Block\\s#)(\\d)");
		final Pattern idPattern = Pattern.compile("(\\d\\.)*(\\d)");

		for (Block block : this.block.getPreds()) {
			Matcher blockMatcher = blockPattern.matcher(block.toShortString());
			createIds(idPattern, blockMatcher, true);
		}
	}

	/**
	 * Set the successor nodes of the current node
	 */
	void setSuccs() {
		final Pattern blockPattern = Pattern.compile("(Block\\s#)(\\d)");
		final Pattern idPattern = Pattern.compile("(\\d\\.)*(\\d)");

		for (Block block : this.block.getSuccs()) {
			Matcher blockMatcher = blockPattern.matcher(block.toShortString());
			createIds(idPattern, blockMatcher, false);
		}
	}

	/**
	 * Matches the ids and creates the references to successors and predecessors
	 *
	 * @param idPattern
	 *            Pattern for resolving the id
	 * @param blockMatcher
	 *            Pattern to extract the local id of a node's block
	 * @param pred
	 *            Boolean to indicate whether the current node is a predecessor or
	 *            successor
	 */
	private void createIds(final Pattern idPattern, final Matcher blockMatcher, final boolean pred) {
		if (blockMatcher.matches()) {
			String key = blockMatcher.group(2);
			Matcher idMatcher = idPattern.matcher(this.id);
			if (idMatcher.matches()) {
				String id = idMatcher.group();
				id = id.substring(0, id.length() - 1).concat(key);
				if (id.length() > 1) {
					if (pred) {
						this.predecessors
								.add(controlFlowGraph.getNodes().get(id.substring(0, id.length() - 1).concat(key)));
					} else {
						this.successor
								.add(controlFlowGraph.getNodes().get(id.substring(0, id.length() - 1).concat(key)));
					}
				} else {
					if (pred) {
						this.predecessors.add(controlFlowGraph.getNodes().get(id));
					} else {
						this.successor.add(controlFlowGraph.getNodes().get(id));
					}
				}
			}
		}
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public Block getBlock() {
		return block;
	}

	List<Node> getPredecessors() {
		return predecessors;
	}

	public LinkedHashMap<Integer, ImmutablePair<BitSet, ProcessVariableOperation>> getOperations() {
		return operations;
	}

	public void setOperations(LinkedHashMap<Integer, ImmutablePair<BitSet, ProcessVariableOperation>> operations) {
		this.operations = operations;
	}

	public LinkedHashMap<Integer, ImmutablePair<BitSet, ProcessVariableOperation>> getDefined() {
		return defined;
	}

	public void setDefined(LinkedHashMap<Integer, ImmutablePair<BitSet, ProcessVariableOperation>> defined) {
		this.defined = defined;
	}

	public LinkedHashMap<Integer, ImmutablePair<BitSet, ProcessVariableOperation>> getUsed() {
		return used;
	}

	public void setUsed(LinkedHashMap<Integer, ImmutablePair<BitSet, ProcessVariableOperation>> used) {
		this.used = used;
	}

	public LinkedHashMap<Integer, ImmutablePair<BitSet, ProcessVariableOperation>> getKilled() {
		return killed;
	}

	public void setKilled(LinkedHashMap<Integer, ImmutablePair<BitSet, ProcessVariableOperation>> killed) {
		this.killed = killed;
	}

	LinkedHashMap<Integer, ImmutablePair<BitSet, ProcessVariableOperation>> getInUsed() {
		return inUsed;
	}

	void setInUsed(LinkedHashMap<Integer, ImmutablePair<BitSet, ProcessVariableOperation>> inUsed) {
		this.inUsed = inUsed;
	}

	LinkedHashMap<Integer, ImmutablePair<BitSet, ProcessVariableOperation>> getInUnused() {
		return inUnused;
	}

	void setInUnused(LinkedHashMap<Integer, ImmutablePair<BitSet, ProcessVariableOperation>> inUnused) {
		this.inUnused = inUnused;
	}

	LinkedHashMap<Integer, ImmutablePair<BitSet, ProcessVariableOperation>> getOutUsed() {
		return outUsed;
	}

	void setOutUsed(LinkedHashMap<Integer, ImmutablePair<BitSet, ProcessVariableOperation>> outUsed) {
		this.outUsed = outUsed;
	}

	LinkedHashMap<Integer, ImmutablePair<BitSet, ProcessVariableOperation>> getOutUnused() {
		return outUnused;
	}

	void setOutUnused(LinkedHashMap<Integer, ImmutablePair<BitSet, ProcessVariableOperation>> outUnused) {
		this.outUnused = outUnused;
	}

}
