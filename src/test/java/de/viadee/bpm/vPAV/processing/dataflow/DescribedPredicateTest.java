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

import org.junit.Test;

import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.*;

public class DescribedPredicateTest {

    @Test
    public void testApplyAppliesPredicate() {
        DescribedPredicate<String> constraint = new DescribedPredicate<>(String::isEmpty, "");

        assertThat(constraint.apply(""), is(true));
        assertThat(constraint.apply("notEmpty"), is(false));
    }

    @Test
    public void testOrCombinesConstraintsCorrectly() {
        DescribedPredicate<String> constraint = new DescribedPredicate<>(String::isEmpty, "constraint1");
        DescribedPredicate<String> constraint2 = new DescribedPredicate<>(s -> s.startsWith("ext_"), "constraint2");

        DescribedPredicate<String> testConstraint = constraint.or(constraint2);

        assertThat(testConstraint.getDescription(), is("constraint1 or constraint2"));
        assertThat(testConstraint.apply(""), is(true));
        assertThat(testConstraint.apply("ext_"), is(true));
        assertThat(testConstraint.apply("notEmpty"), is(false));
    }

    @Test
    public void testAndCombinesConstraintsCorrectly() {
        DescribedPredicate<String> constraint = new DescribedPredicate<>(s -> s.length() == 5, "constraint1");
        DescribedPredicate<String> constraint2 = new DescribedPredicate<>(s -> s.startsWith("ext_"), "constraint2");

        DescribedPredicate<String> testConstraint = constraint.and(constraint2);

        assertThat(testConstraint.getDescription(), is("constraint1 and constraint2"));
        assertThat(testConstraint.apply("five5"), is(false));
        assertThat(testConstraint.apply("ext_"), is(false));
        assertThat(testConstraint.apply("ext_5"), is(true));
    }
}
