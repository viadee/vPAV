package de.viadee.bpm.vPAV.processing.dataflow;

import java.util.ArrayList;
import java.util.List;

public class ProcessVariable {

    private final String name;
    private final List<de.viadee.bpm.vPAV.processing.model.data.ProcessVariable> operations;
    private final List<de.viadee.bpm.vPAV.processing.model.data.ProcessVariable> writes;
    private final List<de.viadee.bpm.vPAV.processing.model.data.ProcessVariable> reads;
    private final List<de.viadee.bpm.vPAV.processing.model.data.ProcessVariable> deletes;
    private final List<de.viadee.bpm.vPAV.processing.model.data.ProcessVariable> definitions;

    public ProcessVariable(String name) {
        this.name = name;
        this.operations = new ArrayList<>();
        this.writes = new ArrayList<>();
        this.reads = new ArrayList<>();
        this.deletes = new ArrayList<>();
        this.definitions = new ArrayList<>();
    }

    public void addDefinition(de.viadee.bpm.vPAV.processing.model.data.ProcessVariable operation) {
        operations.add(operation);
        writes.add(operation);
        definitions.add(operation);
    }

    public void addWrite(de.viadee.bpm.vPAV.processing.model.data.ProcessVariable operation) {
        operations.add(operation);
        writes.add(operation);
    }

    public void addRead(de.viadee.bpm.vPAV.processing.model.data.ProcessVariable operation) {
        operations.add(operation);
        reads.add(operation);
    }

    public void addDelete(de.viadee.bpm.vPAV.processing.model.data.ProcessVariable operation) {
        operations.add(operation);
        deletes.add(operation);
    }

    public String getName() {
        return name;
    }

    public List<de.viadee.bpm.vPAV.processing.model.data.ProcessVariable> getWrites() {
        return writes;
    }

    public List<de.viadee.bpm.vPAV.processing.model.data.ProcessVariable> getReads() {
        return reads;
    }

    public List<de.viadee.bpm.vPAV.processing.model.data.ProcessVariable> getDeletes() {
        return deletes;
    }

    public List<de.viadee.bpm.vPAV.processing.model.data.ProcessVariable> getDefinitions() {
        return definitions;
    }
}
