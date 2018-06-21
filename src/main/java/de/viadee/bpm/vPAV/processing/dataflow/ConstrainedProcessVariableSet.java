package de.viadee.bpm.vPAV.processing.dataflow;

public interface ConstrainedProcessVariableSet {
    ProcessVariablePredicateBuilder<ConstrainedProcessVariableSet> orThatAre();
    ProcessVariablePredicateBuilder<ConstrainedProcessVariableSet> andThatAre();
    ConstrainedProcessVariableSet orThatAre(DescribedPredicateEvaluator<ProcessVariable> constraint);
    ConstrainedProcessVariableSet andThatAre(DescribedPredicateEvaluator<ProcessVariable> constraint);
    ProcessVariablePredicateBuilder<ConditionedSet> shouldBe();
    ConditionedSet shouldBe(DescribedPredicateEvaluator<ProcessVariable> condition);
}
