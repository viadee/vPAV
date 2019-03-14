package de.viadee.bpm.vPAV.delegates;

import org.camunda.bpm.engine.delegate.DelegateExecution;

public class TransitiveDelegate implements DelegateInterface {

    @Override
    public void execute(DelegateExecution execution) {
        execution.getVariable("test");
    }
}
