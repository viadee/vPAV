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

	private LinkedHashMap<Integer, ImmutablePair<BitSet, ProcessVariableOperation>> def;
	private LinkedHashMap<Integer, ImmutablePair<BitSet, ProcessVariableOperation>> use;
	private LinkedHashMap<Integer, ImmutablePair<BitSet, ProcessVariableOperation>> kill;

	private Block block;

	private List<Node> preds;
	private List<Node> succs;
	private String id;

	private BitSet defined;
	private BitSet used;
	private BitSet killed;

	private BitSet inUnused;
	private BitSet inUsed;

	private BitSet outUnused;
	private BitSet outUsed;


	public Node(final ControlFlowGraph controlFlowGraph, final Block block) {
		this.controlFlowGraph = controlFlowGraph;
		this.block = block;
		this.preds = new ArrayList<>();
		this.succs = new ArrayList<>();

		this.operations = new LinkedHashMap<>();

		this.defined = new BitSet();
		this.used = new BitSet();
		this.killed = new BitSet();

		this.inUnused = new BitSet();
		this.inUsed = new BitSet();

		this.outUnused = new BitSet();
		this.outUsed = new BitSet();

		this.def = new LinkedHashMap<>();
		this.use = new LinkedHashMap<>();
		this.kill = new LinkedHashMap<>();
	}

	// Adds an operation to the list of operations (used for line by line checking)
	// Based on operation type adds the operation to the set of corresponding
	// operations
	public void addOperation(final ProcessVariableOperation processVariableOperation) {
		this.operations.put(controlFlowGraph.getDefCounter(),
				new ImmutablePair<>(new BitSet(controlFlowGraph.getDefCounter()), processVariableOperation));
		switch (processVariableOperation.getOperation()) {
		case WRITE:
			defined.set(controlFlowGraph.getDefCounter());
			def.put(controlFlowGraph.getDefCounter(), new ImmutablePair<>(new BitSet(controlFlowGraph.getDefCounter()), processVariableOperation));
			printBits(defined);
			break;
		case READ:
			used.set(controlFlowGraph.getDefCounter());
			use.put(controlFlowGraph.getDefCounter(), new ImmutablePair<>(new BitSet(controlFlowGraph.getDefCounter()), processVariableOperation));
			printBits(used);
			break;
		case DELETE:
			killed.set(controlFlowGraph.getDefCounter());
			kill.put(controlFlowGraph.getDefCounter(), new ImmutablePair<>(new BitSet(controlFlowGraph.getDefCounter()), processVariableOperation));
			printBits(killed);
			break;
		}
		controlFlowGraph.incrementDefCounter();
	}

	void printBits(BitSet b) {
		for (int i = 0; i < b.size(); i++) {
			System.out.print(b.get(i) ? "1" : "0");
		}
		System.out.println();
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
						this.preds.add(controlFlowGraph.getNodes().get(id.substring(0, id.length() - 1).concat(key)));
					} else {
						this.succs.add(controlFlowGraph.getNodes().get(id.substring(0, id.length() - 1).concat(key)));
					}
				} else {
					if (pred) {
						this.preds.add(controlFlowGraph.getNodes().get(id));
					} else {
						this.succs.add(controlFlowGraph.getNodes().get(id));
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

	List<Node> getPreds() {
		return preds;
	}

	List<Node> getSuccs() {
		return succs;
	}

	BitSet getDefined() {
		return defined;
	}

	public BitSet getUsed() {
		return used;
	}

	BitSet getKilled() {
		return killed;
	}

	BitSet getInUnused() {
		return inUnused;
	}

	void setInUnused(BitSet inUnused) {
		this.inUnused = inUnused;
	}

	BitSet getInUsed() {
		return inUsed;
	}

	void setInUsed(BitSet inUsed) {
		this.inUsed = inUsed;
	}

	BitSet getOutUnused() {
		return outUnused;
	}

	void setOutUnused(BitSet outUnused) {
		this.outUnused = outUnused;
	}

	BitSet getOutUsed() {
		return outUsed;
	}

	void setOutUsed(BitSet outUsed) {
		this.outUsed = outUsed;
	}

	public LinkedHashMap<Integer, ImmutablePair<BitSet, ProcessVariableOperation>> getOperations() {
		return operations;
	}

	public void setKill(LinkedHashMap<Integer, ImmutablePair<BitSet, ProcessVariableOperation>> kill) {
		this.kill.putAll(kill);
	}

	public LinkedHashMap<Integer, ImmutablePair<BitSet, ProcessVariableOperation>> getDef() {
		return def;
	}
}
