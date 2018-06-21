package de.viadee.bpm.vPAV.processing.dataflow;

import java.util.Optional;

class EvaluationResult<T> {
    private String message;
    private boolean result;

    private T evaluatedVariable;

    public static <T> EvaluationResult<T> forViolation(String violationMessage, T evaluatedVariable) {
        return new EvaluationResult<>(false, violationMessage, evaluatedVariable);
    }

    public static <T> EvaluationResult<T> forSuccess(T evaluatedVariable) {
        return new EvaluationResult<>(true, null, evaluatedVariable);
    }

    private EvaluationResult(boolean result, String message, T evaluatedVariable) {
        this.message = message;
        this.result = result;
        this.evaluatedVariable = evaluatedVariable;
    }

    public boolean isFulfilled() {
        return result;
    }

    public T getEvaluatedVariable() {
        return evaluatedVariable;
    }

    public Optional<String> getViolationMessage() {
        return Optional.ofNullable(message);
    }
}
