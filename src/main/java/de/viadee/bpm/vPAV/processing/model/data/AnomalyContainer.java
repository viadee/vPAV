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
package de.viadee.bpm.vPAV.processing.model.data;

import java.util.Objects;

public class AnomalyContainer {

    private String name;

    private Anomaly anomaly;

    private String elementId;

    private String elementName;

    private String nodeId;

    private ProcessVariableOperation variable;

    public AnomalyContainer(final String name, final Anomaly anomaly, final String elementId, final String elementName,
                            final ProcessVariableOperation variable) {
        this(name, anomaly, elementId, elementId, elementName, variable);
    }

    public AnomalyContainer(final String name, final Anomaly anomaly, final String nodeId, final String elementId, final String elementName,
                            final ProcessVariableOperation variable) {
        this.name = name;
        this.anomaly = anomaly;
        this.elementId = elementId;
        this.elementName = elementName;
        this.variable = variable;
        this.nodeId = nodeId;
    }

    public String getName() {
        return name;
    }

    public Anomaly getAnomaly() {
        return anomaly;
    }

    public String getElementId() {
        return elementId;
    }

    public String getElementName() {
        return elementName;
    }

    public String getNodeId() {
        return nodeId;
    }

    public ProcessVariableOperation getVariable() {
        return variable;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof AnomalyContainer) {
            final AnomalyContainer anomalyContainer = (AnomalyContainer) obj;
            if (this.name.equals(anomalyContainer.getName())
                    && this.anomaly == anomalyContainer.getAnomaly()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        if (variable != null) {
            return Objects.hash(name.hashCode(), anomaly.toString().hashCode(), elementId.hashCode(), nodeId, variable.getIndex());
        } else {
            return Objects.hash(name.hashCode(), anomaly.toString().hashCode(), elementId.hashCode(), nodeId);
        }
    }

    @Override
    public String toString() {
        return name + "(" + elementId + ", " + anomaly + ")";
    }
}
