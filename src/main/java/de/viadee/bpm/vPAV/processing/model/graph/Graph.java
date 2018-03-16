/**
 * BSD 3-Clause License
 *
 * Copyright © 2018, viadee Unternehmensberatung GmbH
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
package de.viadee.bpm.vPAV.processing.model.graph;

/**
 * University of Washington, Computer Science & Engineering, Course 373, Winter 2011, Jessica Miller
 *
 * A class for a directed graph. Implemented by an adjacency list representation of a graph.
 */
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.camunda.bpm.model.bpmn.impl.BpmnModelConstants;

import de.viadee.bpm.vPAV.constants.BpmnConstants;
import de.viadee.bpm.vPAV.processing.model.data.Anomaly;
import de.viadee.bpm.vPAV.processing.model.data.AnomalyContainer;
import de.viadee.bpm.vPAV.processing.model.data.BpmnElement;
import de.viadee.bpm.vPAV.processing.model.data.InOutState;

public class Graph implements IGraph {

    private String processId;

    private Map<BpmnElement, List<Edge>> adjacencyListSucessor; // [vertices] -> [edge]

    private Map<BpmnElement, List<Edge>> adjacencyListPredecessor; // [vertices] -> [edge]

    private Map<BpmnElement, VertexInfo> vertexInfo; // [vertex] -> [info]

    private Collection<BpmnElement> startNodes = new ArrayList<BpmnElement>();

    private Collection<BpmnElement> endNodes = new ArrayList<BpmnElement>();

    public Graph(final String processId) {
        this.processId = processId;
        this.adjacencyListSucessor = new HashMap<BpmnElement, List<Edge>>();
        this.adjacencyListPredecessor = new HashMap<BpmnElement, List<Edge>>();
        this.vertexInfo = new HashMap<BpmnElement, VertexInfo>();
    }

    @Override
    public String getProcessId() {
        return processId;
    }

    @Override
    public void addStartNode(final BpmnElement node) {
        startNodes.add(node);
    }

    @Override
    public Collection<BpmnElement> getStartNodes() {
        return startNodes;
    }

    @Override
    public void addEndNode(final BpmnElement node) {
        endNodes.add(node);
    }

    @Override
    public Collection<BpmnElement> getEndNodes() {
        return endNodes;
    }

    @Override
    public void addVertex(BpmnElement v) {
        if (v == null) {
            throw new IllegalArgumentException("null");
        }

        adjacencyListSucessor.put(v, new ArrayList<Edge>());
        adjacencyListPredecessor.put(v, new ArrayList<Edge>());
        vertexInfo.put(v, new VertexInfo(v));
    }

    @Override
    public Collection<BpmnElement> getVertices() {
        return vertexInfo.keySet();
    }

    @Override
    public Collection<List<Edge>> getEdges() {
        return adjacencyListSucessor.values();
    }

    @Override
    public void addEdge(BpmnElement from, BpmnElement to, int weight) {
        // add successor
        List<Edge> edgeSucessorList = adjacencyListSucessor.get(from);
        if (edgeSucessorList == null) {
            throw new IllegalArgumentException("source vertex not in graph");
        }

        Edge newSucessorEdge = new Edge(from, to, weight);
        edgeSucessorList.add(newSucessorEdge);

        // add predecessor
        List<Edge> edgePredecessorList = adjacencyListPredecessor.get(to);
        if (edgePredecessorList == null) {
            throw new IllegalArgumentException("source vertex not in graph");
        }

        Edge newPredecessorEdge = new Edge(to, from, weight);
        edgePredecessorList.add(newPredecessorEdge);
    }

    @Override
    public void removeEdge(BpmnElement from, BpmnElement to) {
        final List<Edge> edgeSucessorList = adjacencyListSucessor.get(from);
        Edge foundEdge = null;
        for (final Edge e : edgeSucessorList) {
            if (e.getFrom().toString().equals(from.toString()) && e.getTo().toString().equals(to.toString())) {
                // delete
                foundEdge = e;
            }
        }
        edgeSucessorList.remove(foundEdge);

        final List<Edge> edgePredecessorList = adjacencyListPredecessor.get(to);
        foundEdge = null;
        for (final Edge e : edgePredecessorList) {
            if (e.getTo().toString().equals(from.toString()) && e.getFrom().toString().equals(to.toString())) {
                // delete
                foundEdge = e;
            }
        }
        edgePredecessorList.remove(foundEdge);
    }

    @Override
    public boolean hasEdge(BpmnElement from, BpmnElement to) {
        return getEdge(from, to) != null;
    }

    @Override
    public Edge getEdge(BpmnElement from, BpmnElement to) {
        List<Edge> edgeList = adjacencyListSucessor.get(from);
        if (edgeList == null) {
            throw new IllegalArgumentException("source vertex not in graph");
        }

        for (Edge e : edgeList) {
            if (e.getTo().equals(to)) {
                return e;
            }
        }

        return null;
    }

    /**
     * set anomaly information on data flow graph
     *
     */
    @Override
    public void setAnomalyInformation(final BpmnElement source) {
        setAnomalyInformationRecursive(source, new LinkedList<BpmnElement>());
    }

    /**
     * set anomaly information recursive on data flow graph (forward)
     *
     * @param startNode
     * @param currentPath
     */
    private void setAnomalyInformationRecursive(final BpmnElement startNode,
            final LinkedList<BpmnElement> currentPath) {

        currentPath.add(startNode);

        final boolean isGateway = startNode.getBaseElement().getElementType().getBaseType()
                .getTypeName().equals(BpmnModelConstants.BPMN_ELEMENT_GATEWAY);
        final boolean isNodeParallelGateway = startNode.getBaseElement().getElementType().getTypeName()
                .equals(BpmnModelConstants.BPMN_ELEMENT_PARALLEL_GATEWAY);
        final boolean isEndEvent = startNode.getBaseElement().getElementType().getTypeName()
                .equals(BpmnConstants.ENDEVENT)
                && startNode.getBaseElement().getParentElement().getElementType().getTypeName()
                        .equals(BpmnConstants.PROCESS);

        final List<Edge> predecessorEdges = this.adjacencyListPredecessor.get(startNode);
        Map<String, InOutState> outSuccessors = new HashMap<String, InOutState>();
        if (predecessorEdges != null) {
            for (final Edge t : predecessorEdges) {
                if (isGateway) {
                    if (isNodeParallelGateway) {
                        // If the node is a parallel gateway, take all predecessor variables.
                        // If variables are identical, take the variable with the following precedence
                        // 1) DELETED
                        // 2) READ
                        // 3) DEFINED
                        if (outSuccessors.isEmpty()) {
                            outSuccessors.putAll(t.getTo().getOut());
                        } else {
                            outSuccessors.putAll(unionWithStatePrecedence(outSuccessors, t.getTo().getOut()));
                        }
                    } else {
                        // If the node is an other gateway, take the intersection of all predecessor variables.
                        // Follow the precedence rule (look above)
                        if (outSuccessors.isEmpty()) {
                            outSuccessors.putAll(t.getTo().getOut());
                        } else {
                            outSuccessors.putAll(intersection(outSuccessors, t.getTo().getOut()));
                        }
                    }
                } else {
                    outSuccessors.putAll(t.getTo().getOut());
                }
            }
        }

        startNode.setIn(outSuccessors);
        if (!isEndEvent) {
            // end element has not an out set
            startNode.setOut();
        }

        if (startNode.getBaseElement() != null) {
            // save the path, if the the search has reached the begin of the process
            if (isEndEvent) {
                currentPath.remove(startNode);
                return;
            }
        }

        final List<Edge> edges = this.adjacencyListSucessor.get(startNode);

        for (final Edge t : edges) {
            int occurrences = Collections.frequency(currentPath, t.getTo());
            if (occurrences < 2) { // case iterations n=1 and n=2 for loops
                setAnomalyInformationRecursive(t.getTo(), currentPath);
            }
        }

        currentPath.remove(startNode);
    }

    /**
     * get nodes with data flow anomalies
     */
    @Override
    public Map<BpmnElement, List<AnomalyContainer>> getNodesWithAnomalies() {

        final Map<BpmnElement, List<AnomalyContainer>> anomalies = new HashMap<BpmnElement, List<AnomalyContainer>>();
        for (final BpmnElement node : adjacencyListSucessor.keySet()) {
            anomalies.putAll(node.getAnomalies());
        }
        return anomalies;
    }

    /**
     * search all paths with variables, which has not been set
     *
     * source: http://codereview.stackexchange.com/questions/45678/find-all-paths-from-source-to-destination
     */
    @Override
    public List<Path> getAllInvalidPaths(final BpmnElement source, final AnomalyContainer anomaly) {
        final List<Path> paths = getAllInvalidPathsRecursive(source, anomaly,
                new LinkedList<BpmnElement>());
        return paths;
    }

    /**
     * search all paths with variables, which has not been set (backward)
     *
     * source: http://codereview.stackexchange.com/questions/45678/find-all-paths-from-source-to-destination
     *
     * @param startNode
     * @param varName
     * @param currentPath
     * @param maxSize
     * @return paths
     */
    private List<Path> getAllInvalidPathsRecursive(final BpmnElement startNode,
            final AnomalyContainer anomaly, final LinkedList<BpmnElement> currentPath) {

        final List<Path> invalidPaths = new ArrayList<Path>();

        currentPath.add(startNode);

        final List<Edge> edges = this.adjacencyListPredecessor.get(startNode);

        final Map<String, InOutState> in = startNode.getIn();
        final Map<String, InOutState> out = startNode.getOut();

        final List<Path> returnPathsUrAnomaly = exitConditionUrAnomaly(startNode, anomaly, currentPath,
                invalidPaths, in, out);
        final List<Path> returnPathsDdDuAnomaly = exitConditionDdDuAnomaly(startNode, anomaly,
                currentPath, invalidPaths, in);

        if (anomaly.getAnomaly() == Anomaly.UR && !in.containsKey(anomaly.getName())
                && out.containsKey(anomaly.getName())) {
            return invalidPaths;
        } else if (returnPathsUrAnomaly != null) {
            return returnPathsUrAnomaly;
        } else if (returnPathsDdDuAnomaly != null) {
            return returnPathsDdDuAnomaly;
        }

        for (final Edge t : edges) {
            if (!currentPath.contains(t.getTo()) || t.getTo() == anomaly.getVariable().getElement()) {
                invalidPaths.addAll(getAllInvalidPathsRecursive(t.getTo(), anomaly, currentPath));
            }
        }

        currentPath.remove(startNode);

        return invalidPaths;
    }

    /**
     * exit condition for path finding (ur anomaly)
     *
     * @param startNode
     * @param anomaly
     * @param currentPath
     * @param invalidPaths
     * @param in
     * @param out
     */
    private List<Path> exitConditionUrAnomaly(final BpmnElement startNode,
            final AnomalyContainer anomaly, final LinkedList<BpmnElement> currentPath,
            final List<Path> invalidPaths, final Map<String, InOutState> in,
            final Map<String, InOutState> out) {

        // go back to the node, where the variable was deleted
        // or go back to the start
        if (anomaly.getAnomaly() == Anomaly.UR && (variableDeleted(anomaly, in, out)
                || ((startNode.getBaseElement().getElementType().getTypeName().equals(BpmnConstants.STARTEVENT)
                        && startNode.getBaseElement().getParentElement().getElementType().getTypeName()
                                .equals(BpmnConstants.PROCESS))))) {

            final List<BpmnElement> newPath = new ArrayList<BpmnElement>(currentPath);
            invalidPaths.add(new Path(newPath));

            currentPath.remove(startNode);
            return invalidPaths;
        }
        return null;
    }

    /**
     * is variable deleted
     *
     * @param anomaly
     * @param in
     * @param out
     * @return
     */
    private boolean variableDeleted(final AnomalyContainer anomaly, final Map<String, InOutState> in,
            final Map<String, InOutState> out) {

        return ((in.containsKey(anomaly.getName()) && in.get(anomaly.getName()) != InOutState.DELETED))
                && (out.containsKey(anomaly.getName()) && out.get(anomaly.getName()) == InOutState.DELETED);
    }

    /**
     * exit condition for path finding (du / dd anomaly)
     *
     * @param startNode
     * @param anomaly
     * @param currentPath
     * @param invalidPaths
     */
    private List<Path> exitConditionDdDuAnomaly(final BpmnElement startNode,
            final AnomalyContainer anomaly, final LinkedList<BpmnElement> currentPath,
            final List<Path> invalidPaths, Map<String, InOutState> in) {

        // go back to the node where the element is defined
        // skip the startpoint
        if (startNode.defined().containsKey(anomaly.getName())
                && (anomaly.getAnomaly() == Anomaly.DD || anomaly.getAnomaly() == Anomaly.DU)
                && currentPath.size() > 1) {
            final List<BpmnElement> newPath = new ArrayList<BpmnElement>(currentPath);
            invalidPaths.add(new Path(newPath));

            currentPath.remove(startNode);
            return invalidPaths;
        }
        return null;
    }

    @Override
    public String toString() {
        Set<BpmnElement> keys = adjacencyListSucessor.keySet();
        String str = "digraph G {\n";

        for (BpmnElement v : keys) {
            str += " ";

            List<Edge> edgeList = adjacencyListSucessor.get(v);

            for (Edge edge : edgeList) {
                str += edge;
                str += "\n";
            }
        }
        str += "}";
        return str;
    }

    protected final void clearVertexInfo() {
        for (VertexInfo info : this.vertexInfo.values()) {
            info.clear();
        }
    }

    /**
     * generate intersection for variable maps and remind precedence rule for variable states
     *
     * @param mapA
     *            mapA
     * @param mapB
     *            mapB
     * @return intersection
     */
    public static Map<String, InOutState> intersection(final Map<String, InOutState> mapA,
            final Map<String, InOutState> mapB) {
        final Map<String, InOutState> intersectionMap = new HashMap<String, InOutState>();
        final Set<String> variables = new HashSet<String>();
        variables.addAll(mapA.keySet());
        variables.addAll(mapB.keySet());
        for (final String varName : variables) {
            if (mapA.containsKey(varName) && mapB.containsKey(varName)) {
                final InOutState state1 = mapA.get(varName);
                final InOutState state2 = mapB.get(varName);

                final InOutState intersectionElement = getStatePrecedence(state1, state2);
                intersectionMap.put(varName, intersectionElement);
            } else {
                mapA.remove(varName);
                mapB.remove(varName);
            }
        }
        return intersectionMap;
    }

    /**
     * get union and remind precedence rule for variable states
     *
     * @param mapA
     *            mapA
     * @param mapB
     *            mapB
     * @return union
     */
    public static Map<String, InOutState> unionWithStatePrecedence(final Map<String, InOutState> mapA,
            final Map<String, InOutState> mapB) {

        final Map<String, InOutState> unionMap = new HashMap<String, InOutState>();
        unionMap.putAll(mapA);
        unionMap.putAll(mapB);
        unionMap.putAll(intersection(mapA, mapB));

        return unionMap;

    }

    /**
     * precedence rule for variable states
     *
     * 1) delete 2) read 3) define
     *
     * @param element1
     * @param element2
     * @return
     */
    private static InOutState getStatePrecedence(final InOutState state1, final InOutState state2) {
        if (state1 == InOutState.DELETED || state2 == InOutState.DELETED
                || (state1 == InOutState.DELETED && state2 == InOutState.DELETED)) {
            return InOutState.DELETED;
        } else if (state1 == InOutState.READ || state2 == InOutState.READ
                || (state1 == InOutState.READ && state2 == InOutState.READ)) {
            return InOutState.READ;
        }
        return InOutState.DEFINED;
    }
}
