package de.viadee.bpm.vPAV.processing.dataflow;

public interface ConstraintBuilder<T> {
    ConstraintBuilder<T> that();
    ConstraintBuilder<T> that(Constraint<T> constraint);

    // needed so that we can transition to condition builder
    ConditionBuilder should();
    ConditionBuilder should(Condition condition);
}
