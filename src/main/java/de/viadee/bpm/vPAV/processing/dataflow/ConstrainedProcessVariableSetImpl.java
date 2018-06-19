package de.viadee.bpm.vPAV.processing.dataflow;

class ConstrainedProcessVariableSetImpl implements ConstrainedProcessVariableSet {

    private final RuleBuilder builder;

    ConstrainedProcessVariableSetImpl(RuleBuilder builder) {
        this.builder = builder;
    }

    @Override
    public ProcessVariablePredicateBuilder<ConstrainedProcessVariableSet> orThatAre() {
        return new ProcessVariablePredicateBuilderImpl<>(builder::orThatAre);
    }

    @Override
    public ProcessVariablePredicateBuilder<ConstrainedProcessVariableSet> andThatAre() {
        return new ProcessVariablePredicateBuilderImpl<>(builder::andThatAre);
    }

    @Override
    public ConstrainedProcessVariableSet orThatAre(DescribedPredicate<ProcessVariable> constraint) {
        builder.orThatAre(constraint);
        return this;
    }

    @Override
    public ConstrainedProcessVariableSet andThatAre(DescribedPredicate<ProcessVariable> constraint) {
        builder.orThatAre(constraint);
        return this;
    }

    @Override
    public ProcessVariablePredicateBuilder<ConditionedSet> shouldBe() {
        return builder.shouldBe();
    }

    @Override
    public ConditionedSet shouldBe(DescribedPredicate<ProcessVariable> condition) {
        return builder.shouldBe(condition);
    }
}
