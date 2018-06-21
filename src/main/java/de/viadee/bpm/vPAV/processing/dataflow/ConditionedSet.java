package de.viadee.bpm.vPAV.processing.dataflow;

public interface ConditionedSet extends DataFlowRule {
    ProcessVariablePredicateBuilder<ConditionedSet> andShouldBe();
    ProcessVariablePredicateBuilder<ConditionedSet> orShouldBe();
    ConditionedSet andShouldBe(DescribedPredicateEvaluator<ProcessVariable> condition);
    ConditionedSet orShouldBe(DescribedPredicateEvaluator<ProcessVariable> condition);
}
