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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import static de.viadee.bpm.vPAV.processing.dataflow.RuleBuilder.defineRule;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

public class RuleBuilderTest {

    private static Condition conditionFrom(Predicate<ProcessVariable> predicate) {
        return new Condition(predicate, "");
    }

    private static <T> Constraint<T> constraintFrom(Predicate<T> predicate) {
        return new Constraint<>(predicate, "");
    }

    @Test()
    public void testRuleWithoutVariablesSucceeds() {
        DataFlowRule rule = defineRule()
                .withVariables(new ArrayList<>())
                .that(constraintFrom(v -> false))
                .should(conditionFrom(v -> false));

        assertThat(rule.check(), is(true));
    }

    @Test()
    public void testRulesWithBrokenConditionFails() {
        List<ProcessVariable> processVariables = Collections.singletonList(new ProcessVariable("variable1"));

        DataFlowRule rule = defineRule()
                .withVariables(processVariables)
                .that(constraintFrom(v -> true))
                .should(conditionFrom(v -> false));

        assertThat(rule.check(), is(false));
    }

    @Test()
    public void testRulesWithNonFulfillableConstraintSucceeds() {
        List<ProcessVariable> processVariables = Collections.singletonList(new ProcessVariable("variable1"));

        DataFlowRule rule = defineRule()
                .withVariables(processVariables)
                .that(constraintFrom(v -> false))
                .should(conditionFrom(v -> false));

        assertThat(rule.check(), is(true));
    }

    @Test()
    public void testRulesWithFullfilledConstrainedAndConditionSucceeds() {
        List<ProcessVariable> processVariables = Collections.singletonList(new ProcessVariable("variable1"));

        DataFlowRule rule = defineRule()
                .withVariables(processVariables)
                .that(constraintFrom(v -> true))
                .should(conditionFrom(v -> true));

        assertThat(rule.check(), is(true));
    }

    @Test()
    public void testCanBuildRuleWithPredefinedConstraints() {

        List<ProcessVariable> processVariables = Collections.singletonList(new ProcessVariable("variable1"));

        DataFlowRule rule = defineRule()
                .withVariables(processVariables)
                .that().areDefinedByServiceTasks()
                .should(conditionFrom(v -> true));

        assertThat(rule.check(), is(true));
    }
}
