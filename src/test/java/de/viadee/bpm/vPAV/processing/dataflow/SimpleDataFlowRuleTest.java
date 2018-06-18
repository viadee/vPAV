package de.viadee.bpm.vPAV.processing.dataflow;

import org.junit.Test;

import java.util.Collections;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

public class SimpleDataFlowRuleTest {

    @Test
    public void testCheckWorksWithoutConstraint() {
        SimpleDataFlowRule rule = new SimpleDataFlowRule(null, new Condition(v -> true, ""));

        assertThat(rule.check(Collections.emptyList()), is(true));
    }
}
