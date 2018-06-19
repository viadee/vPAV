package de.viadee.bpm.vPAV.processing.dataflow;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

class OperationBasedPredicateBuilderImpl<T> implements OperationBasedPredicateBuilder<T> {

    private final Function<DescribedPredicate<ProcessVariable>, T> conditionSetter;
    private final Function<ProcessVariable, List<de.viadee.bpm.vPAV.processing.model.data.ProcessVariable>> operationProvider;
    private final String operationDescription;

    OperationBasedPredicateBuilderImpl(
            Function<DescribedPredicate<ProcessVariable>, T> conditionSetter,
            Function<ProcessVariable, List<de.viadee.bpm.vPAV.processing.model.data.ProcessVariable>> operationProvider,
            String operationDescription) {
        this.conditionSetter = conditionSetter;
        this.operationProvider = operationProvider;
        this.operationDescription = operationDescription;
    }

    @Override
    public T exactly(int n) {
        final Predicate<ProcessVariable> predicate = p -> operationProvider.apply(p).size() == n;
        final String times = n == 1 ? "time" : "times";
        final String description = String.format("%s exactly %s %s", operationDescription, n, times);
        return conditionSetter.apply(new DescribedPredicate<>(predicate, description));
    }

    @Override
    public T atLeast(int n) {
        final Predicate<ProcessVariable> predicate = p -> operationProvider.apply(p).size() >= n;
        final String times = n == 1 ? "time" : "times";
        final String description = String.format("%s at least %s %s", operationDescription, n, times);
        return conditionSetter.apply(new DescribedPredicate<>(predicate, description));
    }

    @Override
    public T atMost(int n) {
        final Predicate<ProcessVariable> predicate = p -> operationProvider.apply(p).size() <= n;
        final String times = n == 1 ? "time" : "times";
        final String description = String.format("%s at most %s %s", operationDescription, n, times);
        return conditionSetter.apply(new DescribedPredicate<>(predicate, description));
    }
}
