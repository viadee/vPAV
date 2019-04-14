package de.viadee.bpm.vPAV.delegates;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;


public class ObjectInstantiationDelegate implements JavaDelegate {

    @Override
    public void execute(final DelegateExecution execution) throws Exception {

        ObjectInstantiation ac = new ObjectInstantiation(execution, "var");
        String name = ac.manipulateVariables();

        String var = (String) execution.getVariable(name);

    }

}
