package de.viadee.bpm.vPAV.delegates;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;

public class TestDelegateFlowGraph implements JavaDelegate {

    @Override
    public void execute(final DelegateExecution execution) throws Exception {

        int max = 3;
        int count = 0;
        boolean flag = false;


        // turns out to be:
        // execution.setVariable("0", 0);
        // execution.setVariable("1", 1);
        // execution.setVariable("2", 2);
        while (count < max) {
            execution.setVariable(Integer.toString(count), count);
            count++;
            flag = true;
        }

        // execution.getVariable("1");
        final int count_var = (Integer) execution.getVariable("test");

        if (flag) {
            // execution.removeVariable("1");
            doThis(execution, count_var);
            System.out.println("Here");
        }

        execution.getVariable("3");
        doThat(execution);

    }

    private void doThis(final DelegateExecution execution, final int count) {
        execution.removeVariable(Integer.toString(count));
    }

    private int doThat(final DelegateExecution execution) {
        return (Integer) execution.getVariable("3");
    }
}

