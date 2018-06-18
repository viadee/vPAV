package de.viadee.bpm.vPAV.processing.dataflow;

public interface ConstrainedProcessVariableSet {
    ProcessVariableConstraintBuilder orThat();
    ProcessVariableConstraintBuilder andThat();
    ConstrainedProcessVariableSet orThat(Constraint<ProcessVariable> constraint);
    ConstrainedProcessVariableSet andThat(Constraint<ProcessVariable> constraint);
    RuleBuilder should();
    RuleBuilder should(Condition condition);
}
