package de.viadee.bpm.vPAV.delegates;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.ExecutionListener;

public class BlockSplitDelegate implements ExecutionListener {

    @Override
    public void notify(DelegateExecution execution) throws Exception {
        method1(execution);
        execution.getVariable("var1");
    }

    private void method1(DelegateExecution execution) {
        execution.setVariable("var1", true);
    }
}

