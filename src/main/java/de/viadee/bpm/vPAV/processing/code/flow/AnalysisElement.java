package de.viadee.bpm.vPAV.processing.code.flow;

import de.viadee.bpm.vPAV.processing.model.data.AnomalyContainer;
import de.viadee.bpm.vPAV.processing.model.data.ProcessVariableOperation;

import java.util.LinkedHashMap;
import java.util.List;

public interface AnalysisElement {

    ControlFlowGraph getControlFlowGraph();

    String getId();

    LinkedHashMap<String, ProcessVariableOperation> getOperations();

    void setPredecessors(LinkedHashMap<String, AnalysisElement> predecessors);

    void addPredecessor(AnalysisElement predecessor);

    List<AnalysisElement> getPredecessors();

    List<AnalysisElement> getSuccessors();

    void setSuccessors(LinkedHashMap<String, AnalysisElement> successors);

    void addSuccessor(AnalysisElement successor);

    LinkedHashMap<String,ProcessVariableOperation> getInUsed();

    LinkedHashMap<String,ProcessVariableOperation> getInUnused();

    LinkedHashMap<String,ProcessVariableOperation> getOutUsed();

    LinkedHashMap<String,ProcessVariableOperation> getOutUnused();

    void setInUsed(LinkedHashMap<String,ProcessVariableOperation> inUsedB);

    void setInUnused(LinkedHashMap<String,ProcessVariableOperation> inUnusedB);

    void setOutUsed(LinkedHashMap<String,ProcessVariableOperation> outUsed);

    void setOutUnused(LinkedHashMap<String,ProcessVariableOperation> outUnused);

    LinkedHashMap<String,ProcessVariableOperation> getUsed();

    LinkedHashMap<String,ProcessVariableOperation> getKilled();

    LinkedHashMap<String,ProcessVariableOperation> getDefined();

    void addSourceCodeAnomaly(AnomalyContainer anomalyContainer);

    void clearPredecessors();

    void removePredecessor(String predecessor);
}
