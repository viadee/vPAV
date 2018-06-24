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

import java.util.Collection;

class ConditionedSetImpl implements ConditionedSet {

    private final RuleBuilder ruleBuilder;

    public ConditionedSetImpl(RuleBuilder ruleBuilder) {
        this.ruleBuilder = ruleBuilder;
    }

    @Override
    public ProcessVariablePredicateBuilder<ConditionedSet> andShouldBe() {
        return new ProcessVariablePredicateBuilderImpl<>(ruleBuilder::andShouldBe);
    }

    @Override
    public ProcessVariablePredicateBuilder<ConditionedSet> orShouldBe() {
        return new ProcessVariablePredicateBuilderImpl<>(ruleBuilder::orShouldBe);
    }

    @Override
    public ConditionedSet andShouldBe(DescribedPredicateEvaluator<ProcessVariable> condition) {
        return ruleBuilder.andShouldBe(condition);
    }

    @Override
    public ConditionedSet orShouldBe(DescribedPredicateEvaluator<ProcessVariable> condition) {
        return ruleBuilder.orShouldBe(condition);
    }

    @Override
    public void check(Collection<ProcessVariable> variables) {
        ruleBuilder.check(variables);
    }

    @Override
    public Collection<EvaluationResult<ProcessVariable>> evaluate(Collection<ProcessVariable> variables) {
        return ruleBuilder.evaluate(variables);
    }

    @Override
    public String getRuleDescription() {
        return ruleBuilder.getRuleDescription();
    }
}
