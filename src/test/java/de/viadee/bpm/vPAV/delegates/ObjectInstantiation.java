package de.viadee.bpm.vPAV.delegates;

import org.camunda.bpm.engine.delegate.DelegateExecution;

class ObjectInstantiation {

    private DelegateExecution execution;

    private String name;

    ObjectInstantiation(DelegateExecution execution, String name) {
        this.execution = execution;
        this.name = name;
    }

    String manipulateVariables() {
        this.execution.removeVariable(this.name);
        return this.name;
    }
}
