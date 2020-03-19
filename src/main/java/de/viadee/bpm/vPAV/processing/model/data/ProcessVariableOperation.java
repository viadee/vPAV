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

import de.viadee.bpm.vPAV.processing.code.flow.BasicNode;
import de.viadee.bpm.vPAV.processing.code.flow.BpmnElement;
import de.viadee.bpm.vPAV.processing.code.flow.Node;

/**
 * Represents a process variable operation with some meaningful information.
 *
 */
public class ProcessVariableOperation {

    private static int id_counter;

    private String id;

    private String name;

    private BasicNode node;

    private VariableOperation operation;

    private String scopeId;

    private String resourceFilePath;

    private int index;

    private int flowOperationIndex;

    private KnownElementFieldType fieldType;

    public ProcessVariableOperation(final String name,
            final VariableOperation operation,
            final String scopeId) {
        super();
        this.name = name;
        this.operation = operation;
        this.scopeId = scopeId;
        this.id = createId();
    }

    public ProcessVariableOperation(final String name,
            final VariableOperation operation,
            final KnownElementFieldType fieldType,
            final String scopeId) {
        super();
        this.name = name;
        this.operation = operation;
        this.fieldType = fieldType;
        this.scopeId = scopeId;
        this.id = createId();
    }

    public ProcessVariableOperation(final String name, final BpmnElement element,
            final String resourceFilePath, final VariableOperation operation,
            final String scopeId, final int index, final Node node) {
        super();
        this.name = name;
        this.resourceFilePath = resourceFilePath;
        this.operation = operation;
        this.scopeId = scopeId;
        this.index = index;
        this.node = node;
        this.id = createId();
        element.getFlowAnalysis().incrementOperationCounter();
        this.flowOperationIndex = element.getFlowAnalysis().getOperationCounter();
    }

    public void setNode(BasicNode node) {
        this.node = node;
    }

    private String createId() {
        return name + "_" + id_counter++;
    }

    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }

    public String getResourceFilePath() {
        return resourceFilePath;
    }

    public BpmnElement getElement() {
        return node.getParentElement();
    }

    public ElementChapter getChapter() {
        return node.getElementChapter();
    }

    public KnownElementFieldType getFieldType() {
        if (fieldType == null) {
            return node.getFieldType();
        } else {
            return fieldType;
        }
    }

    public VariableOperation getOperation() {
        return operation;
    }

    public String getScopeId() {
        return scopeId;
    }

    public int getIndex() {
        return index;
    }

    public String toString() {
        return name + " [" + getElement().getProcessDefinition() + ", " + getElement().getBaseElement().getId()
                + ", Scope: "
                + scopeId + ", " + getChapter().name() + ", " + getFieldType().getDescription() + ", "
                + resourceFilePath + "]";
    }

    @Override
    public boolean equals(final Object o) {
        if (o instanceof ProcessVariableOperation) {
            final ProcessVariableOperation p = (ProcessVariableOperation) o;
            return name.equals(p.getName());
        }
        return false;
    }

    public int getFlowOperationIndex() {
        return flowOperationIndex;
    }

    public void initializeOperation(final BpmnElement element) {
        // TODO when and where is this used?
        //  this.element = element;
        element.getFlowAnalysis().incrementOperationCounter();
        this.flowOperationIndex = element.getFlowAnalysis().getOperationCounter();
    }

    public void setIndex(int operationCounter) {
        index = operationCounter;
    }
}
