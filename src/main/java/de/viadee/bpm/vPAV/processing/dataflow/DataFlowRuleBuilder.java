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
import org.springframework.util.Assert;

import java.util.Collection;

public class DataFlowRuleBuilder implements ProcessVariableSet, ConditionedProcessVariableSet, ConstrainedProcessVariableSet, DataFlowRule {

    private DescribedPredicateEvaluator<ProcessVariable> condition;
    private DescribedPredicateEvaluator<ProcessVariable> constraint;

    public static ProcessVariableSet processVariables() {
        return new DataFlowRuleBuilder();
    }

    @Override
    public ConditionedProcessVariableSet shouldBe(DescribedPredicateEvaluator<ProcessVariable> condition) {
        this.condition = condition;
        return this;
    }

    @Override
    public ProcessVariablePredicateBuilder<ConditionedProcessVariableSet> shouldBe() {
        return new ProcessVariablePredicateBuilderImpl<>(this::shouldBe);
    }

    @Override
    public ConditionedProcessVariableSet orShouldBe(DescribedPredicateEvaluator<ProcessVariable> condition) {
        Assert.notNull(this.condition, "Condition conjunction is not allowed without defining initial condition");
        this.condition = this.condition.or(condition);
        return this;
    }

    @Override
    public ProcessVariablePredicateBuilder<ConditionedProcessVariableSet> andShouldBe() {
        return new ProcessVariablePredicateBuilderImpl<>(this::andShouldBe);
    }

    @Override
    public ProcessVariablePredicateBuilder<ConditionedProcessVariableSet> orShouldBe() {
        return new ProcessVariablePredicateBuilderImpl<>(this::orShouldBe);
    }

    @Override
    public ConditionedProcessVariableSet andShouldBe(DescribedPredicateEvaluator<ProcessVariable> condition) {
        Assert.notNull(this.condition, "Condition conjunction is not allowed without defining initial condition");
        this.condition = this.condition.and(condition);
        return this;
    }

    @Override
    public ConstrainedProcessVariableSet thatAre(DescribedPredicateEvaluator<ProcessVariable> constraint) {
        this.constraint = constraint;
        return this;
    }

    @Override
    public ProcessVariablePredicateBuilder<ConstrainedProcessVariableSet> orThatAre() {
        return new ProcessVariablePredicateBuilderImpl<>(this::orThatAre);
    }

    @Override
    public ProcessVariablePredicateBuilder<ConstrainedProcessVariableSet> andThatAre() {
        return new ProcessVariablePredicateBuilderImpl<>(this::andThatAre);
    }

    @Override
    public ConstrainedProcessVariableSet orThatAre(DescribedPredicateEvaluator<ProcessVariable> constraint) {
        Assert.notNull(this.constraint, "Constraint conjunction is not allowed without defining initial constraint");
        this.constraint = this.constraint.or(constraint);
        return this;
    }

    @Override
    public ConstrainedProcessVariableSet andThatAre(DescribedPredicateEvaluator<ProcessVariable> constraint) {
        Assert.notNull(this.constraint, "Constraint conjunction is not allowed without defining initial constraint");
        this.constraint = this.constraint.and(constraint);
        return this;
    }

    @Override
    public ProcessVariablePredicateBuilder<ConstrainedProcessVariableSet> thatAre() {
        return new ProcessVariablePredicateBuilderImpl<>(this::thatAre);
    }

    @Override
    public void check(Collection<ProcessVariable> variables) {
        new SimpleDataFlowRule(constraint, condition).check(variables);
    }

    @Override
    public Collection<EvaluationResult<ProcessVariable>> evaluate(Collection<ProcessVariable> variables) {
        return new SimpleDataFlowRule(constraint, condition).evaluate(variables);
    }

    @Override
    public String getRuleDescription() {
        return new SimpleDataFlowRule(constraint, condition).getRuleDescription();
    }

    @Override
    public CriticalityEnum getCriticality() {
        return new SimpleDataFlowRule(constraint, condition).getCriticality();
    }

    @Override
    public DataFlowRule because(String reason) {
        return new SimpleDataFlowRule(constraint, condition).because(reason);
    }

    @Override
    public DataFlowRule withCriticality(CriticalityEnum criticality) {
        return new SimpleDataFlowRule(constraint, condition).withCriticality(criticality);
    }
}
