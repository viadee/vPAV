package de.viadee.bpm.vPAV.processing.dataflow;

import org.camunda.bpm.model.bpmn.instance.ServiceTask;

import java.util.function.Function;
import java.util.stream.Collectors;

class ProcessVariablePredicateBuilderImpl<T> implements ProcessVariablePredicateBuilder<T> {

    private final Function<DescribedPredicateEvaluator<ProcessVariable>, T> constraintSetter;

    ProcessVariablePredicateBuilderImpl(Function<DescribedPredicateEvaluator<ProcessVariable>, T> constraintSetter) {
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
        final Function<ProcessVariable, EvaluationResult<ProcessVariable>> evaluator = p -> {
            return p.getDefinitions().stream().anyMatch(o -> o.getElement().getBaseElement() instanceof ServiceTask) ?
                    EvaluationResult.forSuccess(p) :
                    EvaluationResult.forViolation("needed to be defined by ServiceTask but was defined by" +
                            p.getDefinitions().stream()
                                    .map(o -> o.getElement().getBaseElement().getClass().toString())
                                    .collect(Collectors.joining(", ")), p);
        };
        final String description = "defined by service tasks";
        return constraintSetter.apply(new DescribedPredicateEvaluator<>(evaluator, description));
    }

    @Override
    public T prefixed(String prefix) {
        final Function<ProcessVariable, EvaluationResult<ProcessVariable>> evaluator = p -> {
            return p.getName().startsWith(prefix) ?
                    EvaluationResult.forSuccess(p) :
                    EvaluationResult.forViolation("needed to be prefixed by " + prefix, p);
        };
        final String description = String.format("prefixed with '%s'", prefix);
        return constraintSetter.apply(new DescribedPredicateEvaluator<>(evaluator, description));
    }
}
