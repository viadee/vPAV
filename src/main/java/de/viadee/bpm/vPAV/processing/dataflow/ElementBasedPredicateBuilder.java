package de.viadee.bpm.vPAV.processing.dataflow;

import de.viadee.bpm.vPAV.processing.model.data.BpmnElement;

public interface ElementBasedPredicateBuilder<T> {
    T ofType(Class clazz);
    T withPrefix(String prefix);
    T withPostfix(String postfix);
    T withNameMatching(String regex);
    T thatFulfill(DescribedPredicateEvaluator<BpmnElement> predicate);
}
