package de.viadee.bpm.vPAV.processing.dataflow;

import de.viadee.bpm.vPAV.processing.model.data.BpmnElement;
import de.viadee.bpm.vPAV.processing.model.data.ProcessVariable;
import org.camunda.bpm.model.bpmn.instance.ServiceTask;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ElementBasedPredicateBuilderImpl<T> implements ElementBasedPredicateBuilder<T> {

    private final Function<DescribedPredicateEvaluator<ProcessVariable>, T> conditionSetter;
    private final Function<ProcessVariable, List<BpmnElement>> elementProvider;
    private final String elementDescription;

    public ElementBasedPredicateBuilderImpl(
            Function<DescribedPredicateEvaluator<ProcessVariable>, T> conditionSetter,
            Function<ProcessVariable,List<BpmnElement>> elementProvider,
            String elementDescription) {
        this.conditionSetter = conditionSetter;
        this.elementProvider = elementProvider;
        this.elementDescription = elementDescription;
    }

    @Override
    public T ofType(Class clazz) {
        final Function<BpmnElement, EvaluationResult<BpmnElement>> evaluator = element -> {
            return element.getBaseElement() instanceof ServiceTask ?
                    EvaluationResult.forSuccess(element) :
                    EvaluationResult.forViolation(String.format("needed to be of type %s but was %s",
                            clazz, element.getBaseElement().getClass()), element);
        };
        final String description = String.format("of type %s", clazz);
        return thatFulfill(new DescribedPredicateEvaluator<>(evaluator, description));
    }

    @Override
    public T withPrefix(String prefix) {
        final Function<BpmnElement, EvaluationResult<BpmnElement>> evaluator = element -> {
            return element.getBaseElement().getId().startsWith(prefix) ?
                    EvaluationResult.forSuccess(element) :
                    EvaluationResult.forViolation(String.format("needed to be prefixed with %s but was %s",
                            prefix, element.getBaseElement().getId()), element);
        };
        final String description = String.format("with prefix %s", prefix);
        return thatFulfill(new DescribedPredicateEvaluator<>(evaluator, description));
    }

    @Override
    public T withPostfix(String postfix) {
        return null;
    }

    @Override
    public T withNameMatching(String regex) {
        return null;
    }

    @Override
    public T thatFulfill(DescribedPredicateEvaluator<BpmnElement> predicate) {
        final Function<ProcessVariable, EvaluationResult<ProcessVariable>> evaluator = p -> {
            List<BpmnElement> elements = elementProvider.apply(p);
            return elements.stream().anyMatch(e -> predicate.evaluate(e).isFulfilled()) ?
                    EvaluationResult.forSuccess(p) :
                    EvaluationResult.forViolation(String.format("needed to be %s %s but was %s by %s",
                            elementDescription, predicate.getDescription(), elementDescription,
                            elements.stream()
                                    .map(e -> e.getBaseElement().getClass().toString())
                                    .collect(Collectors.joining(", "))
                    ), p);
        };
        final String description = String.format("%s %s", elementDescription, predicate.getDescription());
        return conditionSetter.apply(new DescribedPredicateEvaluator<>(evaluator, description));
    }
}
