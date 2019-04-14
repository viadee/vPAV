package de.viadee.bpm.vPAV.delegates;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;

public class MethodInvocationDelegate implements JavaDelegate {

    @Override
    public void execute(final DelegateExecution execution) throws Exception {

        MethodInvocation ac = new MethodInvocation();
        String name = ac.manipulateVariables(execution, "var");

        String var = (String) execution.getVariable(name);

    }

}