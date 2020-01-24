package de.viadee.bpm.vPAV.processing.code.flow.statement;

public enum Statement {

    ASSIGNMENT("assignment"), INVOKE("invoke"), ASSIGNMENT_INVOKE("assignment-invoke");

    private final String description;

    Statement(String value) {
        description = value;
    }

    public String getDescription() {
        return description;
    }
}
