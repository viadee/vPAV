package de.viadee.bpm.vPAV.processing.dataflow;

import java.util.Optional;

class EvaluationResult {
    private String message;
    private boolean result;

    public static EvaluationResult forViolation(String violationMessage) {
        return new EvaluationResult(true, violationMessage);
    }

    public static EvaluationResult forSuccess() {
        return new EvaluationResult(true, null);
    }

    private EvaluationResult(boolean result, String message) {
        this.message = message;
        this.result = result;
    }

    public boolean isFulfilled() {
        return !result;
    }

    public Optional<String> getViolationMessage() {
        return Optional.ofNullable(message);
    }
}
