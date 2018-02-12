/**
 * Copyright © 2017, viadee Unternehmensberatung GmbH
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. All advertising materials mentioning features or use of this software
 *    must display the following acknowledgement:
 *    This product includes software developed by the viadee Unternehmensberatung GmbH.
 * 4. Neither the name of the viadee Unternehmensberatung GmbH nor the
 *    names of its contributors may be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY <viadee Unternehmensberatung GmbH> ''AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package de.viadee.bpm.vPAV.processing.checker;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;

import org.camunda.bpm.model.bpmn.impl.BpmnModelConstants;
import org.camunda.bpm.model.bpmn.instance.BaseElement;
import org.camunda.bpm.model.bpmn.instance.BusinessRuleTask;

import de.viadee.bpm.vPAV.BpmnScanner;
import de.viadee.bpm.vPAV.RuntimeConfig;
import de.viadee.bpm.vPAV.config.model.Rule;
import de.viadee.bpm.vPAV.constants.BpmnConstants;
import de.viadee.bpm.vPAV.output.IssueWriter;
import de.viadee.bpm.vPAV.processing.CheckName;
import de.viadee.bpm.vPAV.processing.model.data.BpmnElement;
import de.viadee.bpm.vPAV.processing.model.data.CheckerIssue;
import de.viadee.bpm.vPAV.processing.model.data.CriticalityEnum;

/**
 * Checks, whether a business rule task with dmn implementation is valid
 *
 */
public class DmnTaskChecker extends AbstractElementChecker {

    public DmnTaskChecker(final Rule rule, BpmnScanner bpmnScanner) {
        super(rule, bpmnScanner);
    }

    /**
     * Check a BusinessRuleTask for a DMN reference
     *
     * @return issues
     */
    @Override
    public Collection<CheckerIssue> check(final BpmnElement element) {
        final Collection<CheckerIssue> issues = new ArrayList<CheckerIssue>();
        final BaseElement bpmnElement = element.getBaseElement();
        if (bpmnElement instanceof BusinessRuleTask) {
            // read attributes from task
            final String implementationAttr = bpmnScanner.getImplementation(bpmnElement.getId());

            final String dmnAttr = bpmnElement.getAttributeValueNs(BpmnModelConstants.CAMUNDA_NS,
                    BpmnModelConstants.CAMUNDA_ATTRIBUTE_DECISION_REF);
            if (implementationAttr != null) {
                // check if DMN reference is not empty
                if (implementationAttr.equals(BpmnConstants.CAMUNDA_DMN)) {
                    if (dmnAttr == null || dmnAttr.trim().length() == 0) {
                        // Error, because no delegateExpression has been configured
                        issues.addAll(IssueWriter.createIssue(rule, CriticalityEnum.ERROR, element,
                                "Task " + CheckName.checkName(bpmnElement) + " with no dmn reference"));
                    } else {
                        issues.addAll(checkDMNFile(element, dmnAttr));
                    }
                }
            }
        }
        return issues;
    }

    /**
     * Check if the referenced DMN in a BusinessRuleTask exists
     *
     * @param element
     *            BpmnElement
     * @param dmnName
     *            Name of DMN-File
     * @return issues
     */
    private Collection<CheckerIssue> checkDMNFile(final BpmnElement element, final String dmnName) {

        final Collection<CheckerIssue> issues = new ArrayList<CheckerIssue>();
        final BaseElement bpmnElement = element.getBaseElement();
        final String dmnPath = dmnName.replaceAll("\\.", "/") + ".dmn";

        // If a dmn path has been found, check the correctness
        URL urlDMN = RuntimeConfig.getInstance().getClassLoader().getResource(dmnPath);

        if (urlDMN == null) {
            // Throws an error, if the class was not found
            issues.addAll(IssueWriter.createIssue(rule, CriticalityEnum.ERROR, element,
                    "Dmn file for task " + CheckName.checkName(bpmnElement) + " not found"));
        }
        return issues;
    }
}
