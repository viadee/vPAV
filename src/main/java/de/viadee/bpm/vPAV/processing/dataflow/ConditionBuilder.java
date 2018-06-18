package de.viadee.bpm.vPAV.processing.dataflow;

public interface ConditionBuilder extends DataFlowRule {
    ConditionBuilder should();
    ConditionBuilder should(Condition condition);
}
