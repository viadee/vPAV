/**
 * BSD 3-Clause License
 *
 * Copyright © 2019, viadee Unternehmensberatung AG
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

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class DescribedPredicateEvaluatorTest {

    @Test
    public void testEvaluateAppliesPredicateCorrectly() {
        DescribedPredicateEvaluator<String> constraint = new DescribedPredicateEvaluator<>(s -> s.isEmpty() ?
                EvaluationResult.forSuccess(s) : EvaluationResult.forViolation("error", s), "");

        assertThat(constraint.evaluate("").isFulfilled(), is(true));
        assertThat(constraint.evaluate("notEmpty").isFulfilled(), is(false));
    }

    @Test
    public void testEvaluateSetsMessagesCorrectly() {
        DescribedPredicateEvaluator<String> constraint = new DescribedPredicateEvaluator<>(s -> s.isEmpty() ?
                EvaluationResult.forSuccess(s) : EvaluationResult.forViolation("error", s), "");
        DescribedPredicateEvaluator<String> constraint2 = new DescribedPredicateEvaluator<>(s -> s.isEmpty() ?
                EvaluationResult.forSuccess("success", s) : EvaluationResult.forViolation(s), "");

        assertFalse(constraint.evaluate("").getMessage().isPresent());
        assertThat(constraint.evaluate("notEmpty").getMessage().orElse(null), is("error"));
        assertThat(constraint2.evaluate("").getMessage().orElse(null), is("success"));
        assertFalse(constraint2.evaluate("notEmpty").getMessage().isPresent());
    }

    @Test
    public void testOrCombinesConstraintsCorrectly() {
        DescribedPredicateEvaluator<String> constraint = new DescribedPredicateEvaluator<>(s -> s.isEmpty() ?
                EvaluationResult.forSuccess(s) : EvaluationResult.forViolation("", s), "constraint1");
        DescribedPredicateEvaluator<String> constraint2 = new DescribedPredicateEvaluator<>(s -> s.startsWith("ext_") ?
                EvaluationResult.forSuccess(s) : EvaluationResult.forViolation("", s), "constraint2");

        DescribedPredicateEvaluator<String> testConstraint = constraint.or(constraint2);

        assertThat(testConstraint.getDescription(), is("constraint1 or constraint2"));
        assertTrue(testConstraint.evaluate("").isFulfilled());
        assertTrue(testConstraint.evaluate("ext_").isFulfilled());
        assertFalse(testConstraint.evaluate("notEmpty").isFulfilled());
    }

    @Test
    public void testOrCombinesMessagesCorrectly() {
        DescribedPredicateEvaluator<String> constraint = new DescribedPredicateEvaluator<>(s -> s.length() == 5 ?
                EvaluationResult.forSuccess("success1", s) : EvaluationResult.forViolation("violation1", s), "constraint1");
        DescribedPredicateEvaluator<String> constraint2 = new DescribedPredicateEvaluator<>(s -> s.startsWith("ext_") ?
                EvaluationResult.forSuccess("success2", s) : EvaluationResult.forViolation("violation2", s), "constraint2");

        DescribedPredicateEvaluator<String> testConstraint = constraint.or(constraint2);

        assertThat(testConstraint.evaluate("wrongg").getMessage().orElse(null), is("violation1 and violation2"));
        assertThat(testConstraint.evaluate("ext_").getMessage().orElse(null), is("success2"));
        assertThat(testConstraint.evaluate("ext_5").getMessage().orElse(null), is("success1 and success2"));
        assertThat(testConstraint.evaluate("five5").getMessage().orElse(null), is("success1"));
    }

    @Test
    public void testAndCombinesConstraintsCorrectly() {
        DescribedPredicateEvaluator<String> constraint = new DescribedPredicateEvaluator<>(s -> s.length() == 5 ?
                EvaluationResult.forSuccess(s) : EvaluationResult.forViolation("", s), "constraint1");
        DescribedPredicateEvaluator<String> constraint2 = new DescribedPredicateEvaluator<>(s -> s.startsWith("ext_") ?
                EvaluationResult.forSuccess(s) : EvaluationResult.forViolation("", s), "constraint2");

        DescribedPredicateEvaluator<String> testConstraint = constraint.and(constraint2);

        assertThat(testConstraint.getDescription(), is("constraint1 and constraint2"));
        assertFalse(testConstraint.evaluate("five5").isFulfilled());
        assertFalse(testConstraint.evaluate("ext_").isFulfilled());
        assertTrue(testConstraint.evaluate("ext_5").isFulfilled());
    }

    @Test
    public void testAndCombinesMessagesCorrectly() {
        DescribedPredicateEvaluator<String> constraint = new DescribedPredicateEvaluator<>(s -> s.length() == 5 ?
                EvaluationResult.forSuccess("success1", s) : EvaluationResult.forViolation("violation1", s), "constraint1");
        DescribedPredicateEvaluator<String> constraint2 = new DescribedPredicateEvaluator<>(s -> s.startsWith("ext_") ?
                EvaluationResult.forSuccess("success2", s) : EvaluationResult.forViolation("violation2", s), "constraint2");

        DescribedPredicateEvaluator<String> testConstraint = constraint.and(constraint2);

        assertThat(testConstraint.evaluate("ext_5").getMessage().orElse(null), is("success1 and success2"));
        assertThat(testConstraint.evaluate("five5").getMessage().orElse(null), is("violation2"));
        assertThat(testConstraint.evaluate("ext_").getMessage().orElse(null), is("violation1"));
        assertThat(testConstraint.evaluate("notEmpty").getMessage().orElse(null), is("violation1 and violation2"));
    }

    @Test
    public void testAndCombinesMessagesInCaseOfAbsence() {
        DescribedPredicateEvaluator<String> constraint = new DescribedPredicateEvaluator<>(s -> s.length() == 5 ?
                EvaluationResult.forSuccess(s) : EvaluationResult.forViolation("violation1", s), "constraint1");
        DescribedPredicateEvaluator<String> constraint2 = new DescribedPredicateEvaluator<>(s -> s.startsWith("ext_") ?
                EvaluationResult.forSuccess("success2", s) : EvaluationResult.forViolation(s), "constraint2");

        DescribedPredicateEvaluator<String> testConstraint = constraint.and(constraint2);

        assertFalse(testConstraint.evaluate("five5").getMessage().isPresent());
        assertThat(testConstraint.evaluate("ext_").getMessage().orElse(null), is("violation1"));
        assertThat(testConstraint.evaluate("notEmpty").getMessage().orElse(null), is("violation1"));
    }

    @Test
    public void testInverseInversesTheConstraintCorrectly() {
        DescribedPredicateEvaluator<String> constraint = new DescribedPredicateEvaluator<>(s -> s.startsWith("ext_") ?
                EvaluationResult.forSuccess(s) : EvaluationResult.forViolation(s), "constraint");

        DescribedPredicateEvaluator<String> testConstraint = constraint.inverse();

        assertThat(testConstraint.getDescription(), is("not constraint"));
        assertTrue(testConstraint.evaluate("").isFulfilled());
        assertFalse(testConstraint.evaluate("ext_").isFulfilled());
        assertTrue(testConstraint.evaluate("notEmpty").isFulfilled());
    }
}
