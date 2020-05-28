/*
 * BSD 3-Clause License
 *
 * Copyright © 2020, viadee Unternehmensberatung AG
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

import de.viadee.bpm.vPAV.processing.model.data.AnomalyContainer;
import de.viadee.bpm.vPAV.processing.model.data.ElementChapter;
import de.viadee.bpm.vPAV.processing.model.data.KnownElementFieldType;
import de.viadee.bpm.vPAV.processing.model.data.ProcessVariableOperation;
import org.camunda.bpm.model.bpmn.instance.BaseElement;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BasicNode implements AnalysisElement{

    protected LinkedHashMap<String, ProcessVariableOperation> operations;

    protected LinkedHashMap<String, ProcessVariableOperation> defined;

    protected LinkedHashMap<String, ProcessVariableOperation> used;

    LinkedHashMap<String, ProcessVariableOperation> killed;

    LinkedHashMap<String, ProcessVariableOperation> inUsed;

    LinkedHashMap<String, ProcessVariableOperation> inUnused;

    LinkedHashMap<String, ProcessVariableOperation> outUsed;

    LinkedHashMap<String, ProcessVariableOperation> outUnused;

    protected BpmnElement parentElement;

    ElementChapter elementChapter;

    private KnownElementFieldType fieldType;

    LinkedHashMap<String, AnalysisElement> predecessors;

    LinkedHashMap<String, AnalysisElement> successors;

    protected String id;

    public BasicNode(final BpmnElement parentElement,
            final ElementChapter elementChapter, final KnownElementFieldType fieldType) {
        this.parentElement = parentElement;
        this.elementChapter = elementChapter;

        this.predecessors = new LinkedHashMap<>();
        this.successors = new LinkedHashMap<>();

        this.operations = new LinkedHashMap<>();
        this.defined = new LinkedHashMap<>();
        this.used = new LinkedHashMap<>();
        this.killed = new LinkedHashMap<>();
        this.inUsed = new LinkedHashMap<>();
        this.inUnused = new LinkedHashMap<>();
        this.outUsed = new LinkedHashMap<>();
        this.outUnused = new LinkedHashMap<>();
        this.fieldType = fieldType;
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
        processVariableOperation.setNode(this);
        final String variableOperationId = processVariableOperation.getId();
        this.operations.put(variableOperationId, processVariableOperation);
        switch (processVariableOperation.getOperation()) {
            case WRITE:
                defined.put(variableOperationId, processVariableOperation);
                break;
            case READ:
                used.put(variableOperationId, processVariableOperation);
                break;
            case DELETE:
                killed.put(variableOperationId, processVariableOperation);
                break;
        }
    }

    public void setId(final String id) {
        this.id = id;
    }

    @Override
    public ControlFlowGraph getControlFlowGraph() {
        return parentElement.getControlFlowGraph();
    }

    public String getId() {
        return id;
    }

    public LinkedHashMap<String, ProcessVariableOperation> getOperations() {
        return operations;
    }

    public void setOperations(LinkedHashMap<String, ProcessVariableOperation> operations) {
        this.operations = operations;
    }

    public LinkedHashMap<String, ProcessVariableOperation> getDefined() {
        return defined;
    }

    public void addSourceCodeAnomaly(AnomalyContainer anomalyContainer) {
        this.parentElement.addSourceCodeAnomaly(anomalyContainer);
    }

    @Override
    public void clearPredecessors() {
        this.predecessors.clear();
    }

    @Override
    public void removePredecessor(String predecessor) {
        this.predecessors.remove(predecessor);
    }

    @Override
    public void clearSuccessors() {
        this.successors.clear();
    }

    @Override
    public void removeSuccessor(String successor) {
        this.successors.remove(successor);
    }

    @Override
    public Map<BpmnElement, List<AnomalyContainer>> getAnomalies() {
        return getParentElement().getAnomalies();
    }

    @Override
    public BaseElement getBaseElement() {
        return this.parentElement.getBaseElement();
    }

    @Override
    public void setDefined(LinkedHashMap<String, ProcessVariableOperation> defined) {
        this.defined = defined;
    }

    @Override
    public void addDefined(LinkedHashMap<String, ProcessVariableOperation> defined) {
        this.defined.putAll(defined);
    }

    @Override
    public LinkedHashMap<String, ProcessVariableOperation> getUsed() {
        return used;
    }

    @Override
    public void setUsed(LinkedHashMap<String, ProcessVariableOperation> used) {
        this.used = used;
    }

    @Override
    public LinkedHashMap<String, ProcessVariableOperation> getKilled() {
        return killed;
    }

    @Override
    public LinkedHashMap<String, ProcessVariableOperation> getInUsed() {
        return inUsed;
    }

    @Override
    public void setInUsed(LinkedHashMap<String, ProcessVariableOperation> inUsed) {
        this.inUsed = inUsed;
    }

    @Override
    public LinkedHashMap<String, ProcessVariableOperation> getInUnused() {
        return inUnused;
    }

    @Override
    public void setInUnused(LinkedHashMap<String, ProcessVariableOperation> inUnused) {
        this.inUnused = inUnused;
    }

    @Override
    public LinkedHashMap<String, ProcessVariableOperation> getOutUsed() {
        return outUsed;
    }

    @Override
    public void setOutUsed(LinkedHashMap<String, ProcessVariableOperation> outUsed) {
        this.outUsed = outUsed;
    }

    @Override
    public LinkedHashMap<String, ProcessVariableOperation> getOutUnused() {
        return outUnused;
    }

    @Override
    public void setOutUnused(LinkedHashMap<String, ProcessVariableOperation> outUnused) {
        this.outUnused = outUnused;
    }

    @Override
    public BpmnElement getParentElement() {
        return parentElement;
    }

    @Override
    public void removeOperation(ProcessVariableOperation op) {
        this.operations.remove(op.getId());
    }

    @Override public String getGraphId() {
        return id;
    }

    @Override
    public void setPredecessors(LinkedHashMap<String, AnalysisElement> predecessors) {
        this.predecessors = predecessors;
    }

    @Override
    public void addPredecessor(AnalysisElement predecessor) {
        this.predecessors.put(predecessor.getId(), predecessor);
        predecessor.addSuccessor(this);
    }

    @Override
    public List<AnalysisElement> getPredecessors() {
        return new ArrayList<>(this.predecessors.values());
    }

    @Override
    public List<AnalysisElement> getSuccessors() {
        return new ArrayList<>(this.successors.values());
    }

    @Override
    public void addSuccessor(AnalysisElement successor) {
        this.successors.put(successor.getId(), successor);
    }

    @Override
    public void setSuccessors(LinkedHashMap<String, AnalysisElement> successors) {
        this.successors = successors;
    }

    public ElementChapter getElementChapter() {
        return elementChapter;
    }

    public KnownElementFieldType getFieldType() {
        return fieldType;
    }
}
