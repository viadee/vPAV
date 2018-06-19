package de.viadee.bpm.vPAV.processing.dataflow;

class EvaluationResult {
    private String ruleDescription;
    private boolean result;
    private ProcessVariable correspondingVariable;

    public EvaluationResult(String ruleDescription, boolean result, ProcessVariable correspondingVariable) {
        this.ruleDescription = ruleDescription;
        this.result = result;
        this.correspondingVariable = correspondingVariable;
    }

    public boolean isRuleViolated() {
        return !result;
    }

    public String getDescription() {
        return correspondingVariable.getName() + ruleDescription;
    }
}
