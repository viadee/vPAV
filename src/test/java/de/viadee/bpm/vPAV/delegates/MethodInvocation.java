package de.viadee.bpm.vPAV.delegates;

import org.camunda.bpm.engine.delegate.DelegateExecution;

class MethodInvocation {

    String manipulateVariables(DelegateExecution execution, String name) {
        execution.removeVariable(name);
        return name;
    }
}