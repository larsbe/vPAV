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

import org.springframework.util.Assert;

import java.util.Collection;

public class RuleBuilder implements DataFlowRule {

    private Condition condition;
    private Constraint<ProcessVariable> constraint;

    public static RuleBuilder processVariables() {
        return new RuleBuilder();
    }

    public RuleBuilder should(Condition condition) {
        this.condition = condition;
        return this;
    }

    public ConstrainedProcessVariableSet that(Constraint<ProcessVariable> constraint) {
        this.constraint = constraint;
        return new ConstrainedProcessVariableSetImpl(this);
    }

    public ConstrainedProcessVariableSet orThat(Constraint<ProcessVariable> constraint) {
        Assert.notNull(this.constraint, "Constraint conjunction is not allowed without defining initial constraint");
        this.constraint = this.constraint.or(constraint);
        return new ConstrainedProcessVariableSetImpl(this);
    }

    public ConstrainedProcessVariableSet andThat(Constraint<ProcessVariable> constraint) {
        Assert.notNull(this.constraint, "Constraint conjunction is not allowed without defining initial constraint");
        this.constraint = this.constraint.and(constraint);
        return new ConstrainedProcessVariableSetImpl(this);
    }

    public ProcessVariableConstraintBuilder that() {
        return new ProcessVariableConstraintBuilderImpl(this::that);
    }

    public DataFlowRule build() {
        return new SimpleDataFlowRule(constraint, condition);
    }

    @Override
    public boolean check(Collection<ProcessVariable> variables) {
        return new SimpleDataFlowRule(constraint, condition).check(variables);
    }
}