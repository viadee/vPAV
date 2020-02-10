package de.viadee.bpm.vPAV.delegates;

import org.camunda.bpm.engine.impl.bpmn.behavior.AbstractBpmnActivityBehavior;
import org.camunda.bpm.engine.impl.pvm.delegate.ActivityExecution;

public class OverloadedExecuteDelegate extends AbstractBpmnActivityBehavior {

    @Override
    public void execute(final ActivityExecution execution) throws Exception {
        execution.getVariable("myVariable");
    }

    public void signal(ActivityExecution execution, String signalName, Object signalData) throws Exception {
        // leave the service task activity:
        leave(execution);
    }
}
