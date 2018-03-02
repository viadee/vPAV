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

import java.util.HashMap;
import java.util.Map;

import de.viadee.bpm.vPAV.processing.model.data.BpmnElement;

/**
 * University of Washington, Computer Science and Engineering, Course 373, Winter 2011, Jessica Miller A utility class
 * that attaches "bookkeeping" information to a vertex. used when searching the graph for a path between two vertices
 */

public class VertexInfo {

    /** The vertex itself. */
    public BpmnElement v;

    /** A mark for whether this vertex has been visited. Useful for path searching. */
    public boolean visited;

    private Map<String, Void> visitedVariables;

    /**
     * Constructs information for the given vertex.
     *
     * @param v
     *            BpmnElement
     */

    public VertexInfo(final BpmnElement v) {
        this.v = v;
        this.visitedVariables = new HashMap<String, Void>();
        this.clear();
    }

    public void visitVariable(final String varName) {
        visitedVariables.put(varName, null);
    }

    public boolean variableVisited(final String varName) {
        return visitedVariables.containsKey(varName);
    }

    /** Resets the visited field. */
    public void clear() {
        this.visited = false;
    }
}