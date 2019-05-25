package de.viadee.bpm.vPAV.processing.code.flow;

import de.viadee.bpm.vPAV.processing.model.data.AnomalyContainer;
import de.viadee.bpm.vPAV.processing.model.data.ProcessVariableOperation;

import java.util.LinkedHashMap;
import java.util.List;

public class BpmnElementDecorator implements AnalysisElement {

    private AnalysisElement decoratedBpmnElement;

    BpmnElementDecorator(final AnalysisElement element) {
        this.decoratedBpmnElement = element;
    }

    @Override
    public ControlFlowGraph getControlFlowGraph() {
        return decoratedBpmnElement.getControlFlowGraph();
    }

    @Override
    public String getId() {
        return decoratedBpmnElement.getId();
    }

    @Override
    public void setPredecessors(LinkedHashMap<String, AnalysisElement> predecessors) {
    	decoratedBpmnElement.setPredecessors(predecessors);
    }

    @Override
    public void addPredecessor(AnalysisElement predecessor) {
        decoratedBpmnElement.addPredecessor(predecessor);
    }

    @Override
    public List<AnalysisElement> getPredecessors() {
        return decoratedBpmnElement.getPredecessors();
    }

    @Override
    public List<AnalysisElement> getSuccessors() {
        return decoratedBpmnElement.getSuccessors();
    }

    @Override
    public void setSuccessors(LinkedHashMap<String, AnalysisElement> successors) {
        this.decoratedBpmnElement.setSuccessors(successors);
    }

    @Override
    public void addSuccessor(AnalysisElement successor) {
        this.decoratedBpmnElement.addSuccessor(successor);
    }

    @Override
    public LinkedHashMap<String, ProcessVariableOperation> getInUsed() {
        return this.decoratedBpmnElement.getInUsed();
    }

    @Override
    public LinkedHashMap<String, ProcessVariableOperation> getInUnused() {
        return this.decoratedBpmnElement.getInUnused();
    }

    @Override
    public LinkedHashMap<String, ProcessVariableOperation> getOutUsed() {
        return this.decoratedBpmnElement.getOutUsed();
    }

    @Override
    public LinkedHashMap<String, ProcessVariableOperation> getOutUnused() {
        return this.decoratedBpmnElement.getOutUnused();
    }

    @Override
    public void setInUsed(LinkedHashMap<String, ProcessVariableOperation> inUsed) {
        this.decoratedBpmnElement.setInUsed(inUsed);
    }

    @Override
    public void setInUnused(LinkedHashMap<String, ProcessVariableOperation> inUnused) {
        this.decoratedBpmnElement.setInUnused(inUnused);
    }

    @Override
    public void setOutUsed(LinkedHashMap<String, ProcessVariableOperation> outUsed) {
        this.decoratedBpmnElement.setOutUsed(outUsed);
    }

    @Override
    public void setOutUnused(LinkedHashMap<String, ProcessVariableOperation> outUnused) {
        this.decoratedBpmnElement.setOutUnused(outUnused);
    }

    @Override
    public LinkedHashMap<String, ProcessVariableOperation> getUsed() {
        return this.decoratedBpmnElement.getUsed();
    }

    @Override
    public LinkedHashMap<String, ProcessVariableOperation> getKilled() {
        return this.decoratedBpmnElement.getKilled();
    }

    @Override
    public LinkedHashMap<String, ProcessVariableOperation> getDefined() {
        return this.decoratedBpmnElement.getDefined();
    }

    @Override
    public void addSourceCodeAnomaly(AnomalyContainer anomalyContainer) {
        this.decoratedBpmnElement.addSourceCodeAnomaly(anomalyContainer);
    }

    @Override
    public void clearPredecessors() {
        this.decoratedBpmnElement.clearPredecessors();
    }

    @Override
    public void removePredecessor(String predecessor) {
        this.decoratedBpmnElement.removePredecessor(predecessor);
    }
}
