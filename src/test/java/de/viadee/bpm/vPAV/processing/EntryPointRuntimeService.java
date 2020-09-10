package de.viadee.bpm.vPAV.processing;

import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.spring.boot.starter.event.PostDeployEvent;

import java.util.HashMap;

public class EntryPointRuntimeService {

    private RuntimeService runtimeService;

    public void startProcessWithVariables(PostDeployEvent event) {
        HashMap<String, Object> variables = new HashMap<>();
        variables.put("variable", "firstValue");
        variables.put("anotherVariable", "anotherValue");
        variables.remove("variable");
        runtimeService.startProcessInstanceByKey("Process_1", variables);
    }

    public void startProcess(PostDeployEvent event) {
        runtimeService.startProcessInstanceById("myId");
        runtimeService.startProcessInstanceByKey("myKey");
        runtimeService.startProcessInstanceByMessage("myMessage");
        runtimeService.startProcessInstanceByMessageAndProcessDefinitionId("myMessage2", "myId2");
    }
}
