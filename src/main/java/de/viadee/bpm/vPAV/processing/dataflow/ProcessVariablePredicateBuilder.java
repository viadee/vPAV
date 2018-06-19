package de.viadee.bpm.vPAV.processing.dataflow;

public interface ProcessVariablePredicateBuilder<T> {
    OperationBasedPredicateBuilder<T> defined();
    OperationBasedPredicateBuilder<T> read();
    OperationBasedPredicateBuilder<T> written();
    T definedByServiceTasks();
    T prefixed(String prefix);
}
