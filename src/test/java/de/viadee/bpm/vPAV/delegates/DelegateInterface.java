package de.viadee.bpm.vPAV.delegates;

import org.camunda.bpm.engine.delegate.DelegateExecution;

public interface DelegateInterface extends DelegateInterface2 {

    @Override
    void execute(DelegateExecution execution) throws Exception;
}