package de.viadee.bpm.vPAV.processing;

import java.util.Map;

import de.viadee.bpm.vPAV.processing.model.data.BpmnElement;
import de.viadee.bpm.vPAV.processing.model.data.ProcessVariable;

public interface ProcessVariableReaderI {

    public Map<String, ProcessVariable> getVariablesFromElement(final BpmnElement element);
    
}
