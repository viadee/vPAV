package de.viadee.bpm.vPAV.processing.dataflow;

public interface ConstrainedProcessVariableSet {
    ProcessVariablePredicateBuilder<ConstrainedProcessVariableSet> orThatAre();
    ProcessVariablePredicateBuilder<ConstrainedProcessVariableSet> andThatAre();
    ConstrainedProcessVariableSet orThatAre(DescribedPredicate<ProcessVariable> constraint);
    ConstrainedProcessVariableSet andThatAre(DescribedPredicate<ProcessVariable> constraint);
    ProcessVariablePredicateBuilder<ConditionedSet> shouldBe();
    ConditionedSet shouldBe(DescribedPredicate<ProcessVariable> condition);
}
