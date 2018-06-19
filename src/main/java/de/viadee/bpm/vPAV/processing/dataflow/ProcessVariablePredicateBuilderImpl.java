package de.viadee.bpm.vPAV.processing.dataflow;

import org.camunda.bpm.model.bpmn.instance.ServiceTask;

import java.util.function.Function;
import java.util.function.Predicate;

class ProcessVariablePredicateBuilderImpl<T> implements ProcessVariablePredicateBuilder<T> {

    private final Function<DescribedPredicate<ProcessVariable>, T> constraintSetter;

    ProcessVariablePredicateBuilderImpl(Function<DescribedPredicate<ProcessVariable>, T> constraintSetter) {
        this.constraintSetter = constraintSetter;
    }

    @Override
    public OperationBasedPredicateBuilder<T> defined() {
        return new OperationBasedPredicateBuilderImpl<>(constraintSetter, ProcessVariable::getDefinitions, "defined");
    }

    @Override
    public OperationBasedPredicateBuilder<T> read() {
        return new OperationBasedPredicateBuilderImpl<>(constraintSetter, ProcessVariable::getReads, "read");
    }

    @Override
    public OperationBasedPredicateBuilder<T> written() {
        return new OperationBasedPredicateBuilderImpl<>(constraintSetter, ProcessVariable::getWrites, "written");
    }

    @Override
    public T definedByServiceTasks() {
        final Predicate<ProcessVariable> predicate = p -> p.getDefinitions().stream().anyMatch(o -> o.getElement().getBaseElement() instanceof ServiceTask);
        final String description = "defined by service tasks";
        return constraintSetter.apply(new DescribedPredicate<>(predicate, description));
    }

    @Override
    public T prefixed(String prefix) {
        final Predicate<ProcessVariable> predicate = p -> p.getName().startsWith(prefix);
        final String description = String.format("prefixed with '%s'", prefix);
        return constraintSetter.apply(new DescribedPredicate<>(predicate, description));
    }
}
