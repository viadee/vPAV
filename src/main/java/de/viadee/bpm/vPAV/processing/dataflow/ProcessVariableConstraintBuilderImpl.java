package de.viadee.bpm.vPAV.processing.dataflow;

import org.camunda.bpm.model.bpmn.instance.ServiceTask;

import java.util.function.Function;
import java.util.function.Predicate;

public class ProcessVariableConstraintBuilderImpl implements ProcessVariableConstraintBuilder {

    private final Function<Constraint<ProcessVariable>, ConstrainedProcessVariableSet> constraintSetter;

    ProcessVariableConstraintBuilderImpl(Function<Constraint<ProcessVariable>, ConstrainedProcessVariableSet> constraintSetter) {
        this.constraintSetter = constraintSetter;
    }

    @Override
    public ConstrainedProcessVariableSet areDefinedByServiceTasks() {
        final Predicate<ProcessVariable> predicate = p -> p.getDefinitions().stream().anyMatch(o -> o.getElement().getBaseElement() instanceof ServiceTask);
        final String description = "are defined by service tasks";
        return constraintSetter.apply(new Constraint<>(predicate, description));
    }

    @Override
    public ConstrainedProcessVariableSet havePrefix(String prefix) {
        final Predicate<ProcessVariable> predicate = p -> p.getName().startsWith(prefix);
        final String description = String.format("have prefix '%s'", prefix);
        return constraintSetter.apply(new Constraint<>(predicate, description));
    }
}
