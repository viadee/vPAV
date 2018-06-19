package de.viadee.bpm.vPAV.processing.dataflow;

import java.util.Collection;

class ConditionedSetImpl implements ConditionedSet {

    private final RuleBuilder ruleBuilder;

    public ConditionedSetImpl(RuleBuilder ruleBuilder) {
        this.ruleBuilder = ruleBuilder;
    }

    @Override
    public ProcessVariablePredicateBuilder<ConditionedSet> andShouldBe() {
        return new ProcessVariablePredicateBuilderImpl<>(ruleBuilder::andShouldBe);
    }

    @Override
    public ProcessVariablePredicateBuilder<ConditionedSet> orShouldBe() {
        return new ProcessVariablePredicateBuilderImpl<>(ruleBuilder::orShouldBe);
    }

    @Override
    public ConditionedSet andShouldBe(DescribedPredicate<ProcessVariable> condition) {
        return ruleBuilder.andShouldBe(condition);
    }

    @Override
    public ConditionedSet orShouldBe(DescribedPredicate<ProcessVariable> condition) {
        return ruleBuilder.orShouldBe(condition);
    }

    @Override
    public void check(Collection<ProcessVariable> variables) {
        ruleBuilder.check(variables);
    }
}
