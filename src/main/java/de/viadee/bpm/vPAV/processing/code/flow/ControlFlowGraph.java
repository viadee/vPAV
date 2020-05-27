/*
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

import java.util.*;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import de.viadee.bpm.vPAV.processing.model.data.ElementChapter;
import de.viadee.bpm.vPAV.processing.model.data.ProcessVariableOperation;
import fj.Hash;

public class ControlFlowGraph {

    private LinkedHashMap<String, BasicNode> nodes;

    private int nodeCounter;

    public ControlFlowGraph() {
        nodes = new LinkedHashMap<>();
        nodeCounter = -1;
    }

    /**
     * Adds a node to the current CFG
     *
     * @param node
     *            Node to be added to the control flow graph
     */
    public void addNode(final BasicNode node) {
        String key = createHierarchy(node);
        node.setId(key);
        this.nodes.put(key, node);
    }

    public void addNodeWithoutNewId(final BasicNode node) {
        this.nodes.put(node.getId(), node);
    }

    /**
     *
     * Helper method to create ids based on hierarchy, e.g.
     *
     * 0 --> 1 --> 2 --> 3 --> 3.0 --> 3.0.0 --> 3.0.1 --> 3.0.2
     *
     * @param node
     *            Current node
     * @return Id of node
     */
    private String createHierarchy(final BasicNode node) {
        StringBuilder key = new StringBuilder();
        key.append(node.getParentElement().getBaseElement().getId()).append("__");
        nodeCounter++;
        key.append(nodeCounter);
        return key.toString();
    }

    boolean hasNodes() {
        return !nodes.isEmpty();
    }

    public LinkedHashMap<String, BasicNode> getNodes() {
        return nodes;
    }

    public BasicNode firstNode() {
        Iterator<BasicNode> iterator = nodes.values().iterator();
        return iterator.next();
    }

    public BasicNode lastNode() {
        Iterator<BasicNode> iterator = nodes.values().iterator();
        BasicNode node = null;
        while (iterator.hasNext()) {
            node = iterator.next();
        }
        return node;
    }

    public ListMultimap<String, ProcessVariableOperation> getOperations() {
        ListMultimap<String, ProcessVariableOperation> operations = ArrayListMultimap.create();
        for (BasicNode node : nodes.values()) {
            for (Map.Entry<String, ProcessVariableOperation> entry : node.getOperations().entrySet()) {
                operations.put(entry.getKey(), entry.getValue());
            }
        }
        return operations;
    }

    // Only used for tests
    public HashSet<String> getVariablesOfOperations() {
        HashSet<String> variableNames = new HashSet<>();
        ListMultimap<String, ProcessVariableOperation> operations = getOperations();
        for (ProcessVariableOperation pvo : operations.values()) {
            variableNames.add(pvo.getName());
        }
        return variableNames;
    }

    public void removeNode(BasicNode node) {
        nodes.remove(node.getId());
    }
}
