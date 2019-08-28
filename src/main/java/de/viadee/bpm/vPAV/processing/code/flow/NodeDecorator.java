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
import java.util.List;
import java.util.Map;

import org.camunda.bpm.model.bpmn.instance.BaseElement;

import de.viadee.bpm.vPAV.processing.model.data.AnomalyContainer;
import de.viadee.bpm.vPAV.processing.model.data.ProcessVariableOperation;

public class NodeDecorator implements AnalysisElement {

    private AnalysisElement decoratedNode;

    NodeDecorator(final AnalysisElement node) {
        this.decoratedNode = node;
    }

    @Override
    public ControlFlowGraph getControlFlowGraph() {
        return null;
    }

    @Override
    public String getId() {
        return decoratedNode.getId();
    }

    @Override
    public LinkedHashMap<String, ProcessVariableOperation> getOperations() {
        return decoratedNode.getOperations();
    }

    @Override
    public void setPredecessors(LinkedHashMap<String, AnalysisElement> predecessors) {
        decoratedNode.setPredecessors(predecessors);
    }

    @Override
    public void addPredecessor(AnalysisElement predecessor) {
        decoratedNode.addPredecessor(predecessor);
    }

    @Override
    public List<AnalysisElement> getPredecessors() {
        return decoratedNode.getPredecessors();
    }

    @Override
    public List<AnalysisElement> getSuccessors() {
        return decoratedNode.getSuccessors();
    }

    @Override
    public void setSuccessors(LinkedHashMap<String, AnalysisElement> successors) {
        decoratedNode.setSuccessors(successors);
    }

    @Override
    public void addSuccessor(AnalysisElement successor) {
        decoratedNode.addSuccessor(successor);
    }

    @Override
    public LinkedHashMap<String, ProcessVariableOperation> getInUsed() {
        return decoratedNode.getInUsed();
    }

    @Override
    public LinkedHashMap<String, ProcessVariableOperation> getInUnused() {
        return decoratedNode.getInUnused();
    }

    @Override
    public LinkedHashMap<String, ProcessVariableOperation> getOutUsed() {
        return decoratedNode.getOutUsed();
    }

    @Override
    public LinkedHashMap<String, ProcessVariableOperation> getOutUnused() {
        return decoratedNode.getOutUnused();
    }

    @Override
    public void setInUsed(LinkedHashMap<String, ProcessVariableOperation> inUsed) {
        decoratedNode.setInUsed(inUsed);
    }

    @Override
    public void setInUnused(LinkedHashMap<String, ProcessVariableOperation> inUnused) {
        decoratedNode.setInUnused(inUnused);
    }

    @Override
    public void setOutUsed(LinkedHashMap<String, ProcessVariableOperation> outUsed) {
        decoratedNode.setOutUsed(outUsed);
    }

    @Override
    public void setOutUnused(LinkedHashMap<String, ProcessVariableOperation> outUnused) {
        decoratedNode.setOutUnused(outUnused);
    }

    @Override
    public LinkedHashMap<String, ProcessVariableOperation> getUsed() {
        return decoratedNode.getUsed();
    }

    @Override
    public LinkedHashMap<String, ProcessVariableOperation> getKilled() {
        return decoratedNode.getKilled();
    }

    @Override
    public LinkedHashMap<String, ProcessVariableOperation> getDefined() {
        return decoratedNode.getDefined();
    }

    @Override
    public void setOperations(LinkedHashMap<String, ProcessVariableOperation> operations) {
        decoratedNode.setOperations(operations);
    }

    @Override
    public void setUsed(LinkedHashMap<String, ProcessVariableOperation> used) {
        decoratedNode.setUsed(used);
    }

    @Override
    public void setDefined(LinkedHashMap<String, ProcessVariableOperation> defined) {
        decoratedNode.setDefined(defined);
    }

    @Override
    public void addDefined(LinkedHashMap<String, ProcessVariableOperation> defined) {
        this.decoratedNode.addDefined(defined);
    }

    @Override
    public void addSourceCodeAnomaly(AnomalyContainer anomalyContainer) {
        this.decoratedNode.addSourceCodeAnomaly(anomalyContainer);
    }

    @Override
    public void clearPredecessors() {
        this.decoratedNode.clearPredecessors();
    }

    @Override
    public void removePredecessor(String predecessor) {
        this.decoratedNode.removePredecessor(predecessor);
    }

    @Override
    public void clearSuccessors() {
        this.decoratedNode.clearSuccessors();
    }

    @Override
    public void removeSuccessor(String successor) {
        this.decoratedNode.removeSuccessor(successor);
    }

    @Override
    public Map<BpmnElement, List<AnomalyContainer>> getAnomalies() {
        return decoratedNode.getAnomalies();
    }

    @Override
    public BaseElement getBaseElement() {
        return decoratedNode.getBaseElement();
    }

    @Override
    public BpmnElement getParentElement() {
        return decoratedNode.getParentElement();
    }

    @Override
    public void removeOperation(ProcessVariableOperation op) {
        this.decoratedNode.removeOperation(op);
    }

}
