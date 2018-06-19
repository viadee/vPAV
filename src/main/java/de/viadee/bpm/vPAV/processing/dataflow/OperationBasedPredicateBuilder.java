package de.viadee.bpm.vPAV.processing.dataflow;

public interface OperationBasedPredicateBuilder<T> {
    T exactly(int n);
    T atLeast(int n);
    T atMost(int n);
}
