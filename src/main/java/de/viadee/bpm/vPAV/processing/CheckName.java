package de.viadee.bpm.vPAV.processing;

import org.camunda.bpm.model.bpmn.instance.BaseElement;

public class CheckName {       

    public static String checkName(final BaseElement baseElement){
        
        String identifier = baseElement.getAttributeValue("name");
        
        if(identifier == "" || identifier == null){
            identifier = baseElement.getAttributeValue("id");
        }
        
        return identifier;        
    }
    
    
}
