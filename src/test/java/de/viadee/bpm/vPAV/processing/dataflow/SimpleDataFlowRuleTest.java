package de.viadee.bpm.vPAV.processing.dataflow;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static de.viadee.bpm.vPAV.processing.dataflow.RuleBuilder.processVariables;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThat;

public class SimpleDataFlowRuleTest {

    @Test
    public void testCheckWorksWithoutConstraint() {
        SimpleDataFlowRule rule = new SimpleDataFlowRule(null, new DescribedPredicateEvaluator<>(EvaluationResult::forSuccess, ""));

        rule.check(Collections.emptyList());
    }

    @Test
    public void testAppliesConstraintCorrectly() {
        SimpleDataFlowRule rule = new SimpleDataFlowRule(
                new DescribedPredicateEvaluator<>(v -> !v.getName().equals("var1") ?
                        EvaluationResult.forViolation("error", v) :
                        EvaluationResult.forSuccess(v), ""),
                new DescribedPredicateEvaluator<>(EvaluationResult::forSuccess, ""));

        rule.check(Collections.singletonList(new ProcessVariable("var1")));
    }

    @Test
    public void testErrorMessageContainsConditionDescription() {
        SimpleDataFlowRule rule = new SimpleDataFlowRule(null,
                new DescribedPredicateEvaluator<>(v -> EvaluationResult.forViolation("", v), "not wrong!"));

        try {
            rule.check(Collections.singletonList(new ProcessVariable("var1")));
        } catch (AssertionError e) {
            assertThat(e.getMessage(), containsString("Rule 'process variables should be not wrong!' was violated 1 times"));
        }
    }

    @Test
    public void testErrorMessageContainsConstraintDescription() {
        SimpleDataFlowRule rule = new SimpleDataFlowRule(
                new DescribedPredicateEvaluator<>(EvaluationResult::forSuccess, "easily fulfilling something"),
                new DescribedPredicateEvaluator<>(v -> EvaluationResult.forViolation("", v), ""));

        try {
            rule.check(Collections.singletonList(new ProcessVariable("var1")));
        } catch (AssertionError e) {
            assertThat(e.getMessage(), containsString("process variables that are easily fulfilling something should be"));
        }
    }

    @Test
    public void testErrorMessageContainsCorrectViolationText() {
        SimpleDataFlowRule rule = new SimpleDataFlowRule(null,
                new DescribedPredicateEvaluator<>(v -> EvaluationResult.forViolation("is not right", v), ""));

        try {
            rule.check(Collections.singletonList(new ProcessVariable("var1")));
        } catch (AssertionError e) {
            assertThat(e.getMessage(), containsString("var1 is not right"));
        }
    }

    @Test
    public void testErrorMessageContainsAllViolations() {
        SimpleDataFlowRule rule = new SimpleDataFlowRule(null,
                new DescribedPredicateEvaluator<>(v -> v.getName().equals("correct name") ?
                        EvaluationResult.forSuccess(v) :
                        EvaluationResult.forViolation("is not right", v), ""));

        try {
            rule.check(Arrays.asList(
                    new ProcessVariable("var1"),
                    new ProcessVariable("correct name"),
                    new ProcessVariable("var3")));
        } catch (AssertionError e) {
            assertThat(e.getMessage(), containsString("var1 is not right"));
            assertThat(e.getMessage(), containsString("var3 is not right"));
            assertThat(e.getMessage(), containsString("was violated 2 times"));
            assertThat(e.getMessage(), not(containsString("correct name is not right")));
        }
    }
}
