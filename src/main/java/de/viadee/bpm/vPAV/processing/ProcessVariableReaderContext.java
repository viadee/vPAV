package de.viadee.bpm.vPAV.processing;

import java.util.Map;

import de.viadee.bpm.vPAV.processing.model.data.BpmnElement;
import de.viadee.bpm.vPAV.processing.model.data.ProcessVariable;

public class ProcessVariableReaderContext {

private ProcessVariableReaderI readingStrategy;
    
    public void setReadingStrategy(ProcessVariableReaderI readingStrategy) {
        this.readingStrategy = readingStrategy;
    }
    
    public Map<String, ProcessVariable> readingVariables(final BpmnElement element) {
        return readingStrategy.getVariablesFromElement(element);
    }
    
}
