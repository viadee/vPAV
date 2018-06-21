package de.viadee.bpm.vPAV.processing.dataflow;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

class OperationBasedPredicateBuilderImpl<T> implements OperationBasedPredicateBuilder<T> {

    private final Function<DescribedPredicateEvaluator<ProcessVariable>, T> conditionSetter;
    private final Function<ProcessVariable, List<de.viadee.bpm.vPAV.processing.model.data.ProcessVariable>> operationProvider;
    private final String operationDescription;

    OperationBasedPredicateBuilderImpl(
            Function<DescribedPredicateEvaluator<ProcessVariable>, T> conditionSetter,
            Function<ProcessVariable, List<de.viadee.bpm.vPAV.processing.model.data.ProcessVariable>> operationProvider,
            String operationDescription) {
        this.conditionSetter = conditionSetter;
        this.operationProvider = operationProvider;
        this.operationDescription = operationDescription;
    }

    @Override
    public T exactly(int n) {
        final String times = n == 1 ? "time" : "times";
        final Function<ProcessVariable, EvaluationResult> evaluator = p -> {
            int operationsCount = operationProvider.apply(p).size();
            return operationsCount == n ?
                    EvaluationResult.forSuccess() :
                    EvaluationResult.forViolation(String.format("needed to be %s exactly %s %s but was %s",
                            operationDescription, n, times, operationsCount));
        };
        final String description = String.format("%s exactly %s %s", operationDescription, n, times);
        return conditionSetter.apply(new DescribedPredicateEvaluator<>(evaluator, description));
    }

    @Override
    public T atLeast(int n) {
        final String times = n == 1 ? "time" : "times";
        final Function<ProcessVariable, EvaluationResult> evaluator = p -> {
            int operationsCount = operationProvider.apply(p).size();
            return operationsCount >= n ?
                    EvaluationResult.forSuccess() :
                    EvaluationResult.forViolation(String.format("needed to be %s at least %s %s but was %s",
                            operationDescription, n, times, operationsCount));
        };
        final String description = String.format("%s at least %s %s", operationDescription, n, times);
        return conditionSetter.apply(new DescribedPredicateEvaluator<>(evaluator, description));
    }

    @Override
    public T atMost(int n) {
        final String times = n == 1 ? "time" : "times";
        final Function<ProcessVariable, EvaluationResult> evaluator = p -> {
            int operationsCount = operationProvider.apply(p).size();
            return operationsCount <= n ?
                    EvaluationResult.forSuccess() :
                    EvaluationResult.forViolation(String.format("needed to be %s at most %s %s but was %s",
                            operationDescription, n, times, operationsCount));
        };
        final String description = String.format("%s at most %s %s", operationDescription, n, times);
        return conditionSetter.apply(new DescribedPredicateEvaluator<>(evaluator, description));
    }
}
