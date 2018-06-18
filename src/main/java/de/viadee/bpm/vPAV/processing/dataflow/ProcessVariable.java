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
}
