/**
 * BSD 3-Clause License
 *
 * Copyright © 2018, viadee Unternehmensberatung GmbH
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * * Neither the name of the copyright holder nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package de.viadee.bpm.vPAV.processing.dataflow;

import de.viadee.bpm.vPAV.processing.model.data.CriticalityEnum;
import de.viadee.bpm.vPAV.processing.model.data.ProcessVariable;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

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
            assertThat(e.getMessage(), containsString("Rule 'Process variables should be not wrong!' was violated 1 times"));
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
            assertThat(e.getMessage(), containsString("Process variables that are easily fulfilling something should be"));
        }
    }

    @Test
    public void testErrorMessageContainsSeverityDescription() {
        SimpleDataFlowRule rule = new SimpleDataFlowRule(
                new DescribedPredicateEvaluator<>(EvaluationResult::forSuccess, "easily fulfilling something"),
                new DescribedPredicateEvaluator<>(v -> EvaluationResult.forViolation("also holding this condition", v), ""));

        try {
            rule.withCriticality(CriticalityEnum.WARNING).check(Collections.singletonList(new ProcessVariable("var1")));
        } catch (AssertionError e) {
            assertThat(e.getMessage(), containsString("times [Criticality: WARNING]"));
        }
    }

    @Test
    public void testErrorMessageLeavesOutViolationIfNotProvided() {
        SimpleDataFlowRule rule = new SimpleDataFlowRule(
                new DescribedPredicateEvaluator<>(EvaluationResult::forSuccess, "easily fulfilling something"),
                new DescribedPredicateEvaluator<>(EvaluationResult::forViolation, ""));

        try {
            rule.check(Collections.singletonList(new ProcessVariable("var1")));
        } catch (AssertionError e) {
            assertThat(e.getMessage(), not(containsString("but was")));
        }
    }

    @Test
    public void testErrorMessageContainsCorrectViolationText() {
        SimpleDataFlowRule rule = new SimpleDataFlowRule(null,
                new DescribedPredicateEvaluator<>(v -> EvaluationResult.forViolation("not right", v), ""));

        try {
            rule.check(Collections.singletonList(new ProcessVariable("var1")));
        } catch (AssertionError e) {
            assertThat(e.getMessage(), containsString("'var1' needed to be"));
            assertThat(e.getMessage(), containsString("but was not right"));
        }
    }

    @Test
    public void testErrorMessageContainsAllViolations() {
        SimpleDataFlowRule rule = new SimpleDataFlowRule(null,
                new DescribedPredicateEvaluator<>(v -> v.getName().equals("correct name") ?
                        EvaluationResult.forSuccess(v) :
                        EvaluationResult.forViolation("not right", v), ""));

        try {
            rule.check(Arrays.asList(
                    new ProcessVariable("var1"),
                    new ProcessVariable("correct name"),
                    new ProcessVariable("var3")));
        } catch (AssertionError e) {
            assertThat(e.getMessage(), containsString("'var1' needed to be  but was not right"));
            assertThat(e.getMessage(), containsString("'var3' needed to be  but was not right"));
            assertThat(e.getMessage(), containsString("was violated 2 times"));
            assertThat(e.getMessage(), not(containsString("correct name is not right")));
        }
    }

    @Test
    public void testBecauseAttachesReasonToDescription() {
        SimpleDataFlowRule rule = new SimpleDataFlowRule(
                new DescribedPredicateEvaluator<>(EvaluationResult::forSuccess, "easily fulfilling something"),
                new DescribedPredicateEvaluator<>(EvaluationResult::forViolation, "also holding tough condition"));

        try {
            rule.because("of reasons").check(Collections.singletonList(new ProcessVariable("var1")));
        } catch (AssertionError e) {
            assertThat(e.getMessage(), containsString("easily fulfilling something should be also holding tough condition because of reasons"));
        }
    }
}
