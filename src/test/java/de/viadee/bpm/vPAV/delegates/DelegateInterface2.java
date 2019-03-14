package de.viadee.bpm.vPAV.delegates;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;

public interface DelegateInterface2 extends JavaDelegate {

    @Override
    void execute(DelegateExecution execution) throws Exception;
}