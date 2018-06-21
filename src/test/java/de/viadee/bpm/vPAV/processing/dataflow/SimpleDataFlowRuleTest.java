package de.viadee.bpm.vPAV.processing.dataflow;

import org.junit.Test;

import java.util.Collections;

public class SimpleDataFlowRuleTest {

    @Test
    public void testCheckWorksWithoutConstraint() {
        SimpleDataFlowRule rule = new SimpleDataFlowRule(null, new DescribedPredicateEvaluator<>(v -> true, ""));

        rule.check(Collections.emptyList());
    }
}
