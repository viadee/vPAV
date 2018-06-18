package de.viadee.bpm.vPAV.processing.dataflow;

public class ConstrainedProcessVariableSetImpl implements ConstrainedProcessVariableSet {

    private final RuleBuilder builder;

    ConstrainedProcessVariableSetImpl(RuleBuilder builder) {
        this.builder = builder;
    }

    @Override
    public ProcessVariableConstraintBuilder orThat() {
        return new ProcessVariableConstraintBuilderImpl(builder::orThat);
    }

    @Override
    public ProcessVariableConstraintBuilder andThat() {
        return new ProcessVariableConstraintBuilderImpl(builder::andThat);
    }

    @Override
    public ConstrainedProcessVariableSet orThat(Constraint<ProcessVariable> constraint) {
        builder.orThat(constraint);
        return this;
    }

    @Override
    public ConstrainedProcessVariableSet andThat(Constraint<ProcessVariable> constraint) {
        builder.orThat(constraint);
        return this;
    }

    @Override
    public RuleBuilder should() {
        return null;
    }

    @Override
    public RuleBuilder should(Condition condition) {
        return builder.should(condition);
    }
}
