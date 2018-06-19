package de.viadee.bpm.vPAV.processing.dataflow;

public interface ConditionedSet extends DataFlowRule {
    ProcessVariablePredicateBuilder<ConditionedSet> andShouldBe();
    ProcessVariablePredicateBuilder<ConditionedSet> orShouldBe();
    ConditionedSet andShouldBe(DescribedPredicate<ProcessVariable> condition);
    ConditionedSet orShouldBe(DescribedPredicate<ProcessVariable> condition);
}
