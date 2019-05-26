package de.viadee.bpm.vPAV.processing.code.flow;

import de.viadee.bpm.vPAV.processing.model.data.AnomalyContainer;
import de.viadee.bpm.vPAV.processing.model.data.ProcessVariableOperation;

import java.util.LinkedHashMap;
import java.util.List;

public class NodeDecorator implements AnalysisElement {

    private AnalysisElement decoratedNode;

    public NodeDecorator(final AnalysisElement node) {
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


}
