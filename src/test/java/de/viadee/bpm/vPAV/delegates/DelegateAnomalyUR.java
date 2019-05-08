package de.viadee.bpm.vPAV.delegates;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;

import java.util.Random;

public class DelegateAnomalyUR implements JavaDelegate {

    private final static String first = "1";

    @Override
    public void execute(DelegateExecution execution) {
        dd(execution);
    }

    private void dd(DelegateExecution execution) {
        Random r = new Random();
        execution.removeVariable(first);
        if (r.nextInt(5) > 2) {
            int i = 3;
        }

        execution.getVariable(first);
    }
}
