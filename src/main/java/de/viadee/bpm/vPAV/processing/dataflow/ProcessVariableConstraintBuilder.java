package de.viadee.bpm.vPAV.processing.dataflow;

public interface ProcessVariableConstraintBuilder {
    ConstrainedProcessVariableSet areDefinedByServiceTasks();
    ConstrainedProcessVariableSet havePrefix(String prefix);
}
