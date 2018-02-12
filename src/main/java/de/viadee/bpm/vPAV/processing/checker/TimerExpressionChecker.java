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

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.DatatypeConverter;
import javax.xml.datatype.DatatypeFactory;

import org.camunda.bpm.model.bpmn.impl.BpmnModelConstants;
import org.camunda.bpm.model.bpmn.instance.BaseElement;
import org.camunda.bpm.model.bpmn.instance.BoundaryEvent;
import org.camunda.bpm.model.bpmn.instance.IntermediateCatchEvent;
import org.camunda.bpm.model.bpmn.instance.StartEvent;
import org.w3c.dom.Element;

import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.parser.CronParser;

import de.viadee.bpm.vPAV.BpmnScanner;
import de.viadee.bpm.vPAV.config.model.Rule;
import de.viadee.bpm.vPAV.output.IssueWriter;
import de.viadee.bpm.vPAV.processing.CheckName;
import de.viadee.bpm.vPAV.processing.model.data.BpmnElement;
import de.viadee.bpm.vPAV.processing.model.data.CheckerIssue;
import de.viadee.bpm.vPAV.processing.model.data.CriticalityEnum;
import net.time4j.range.IsoRecurrence;
import net.time4j.range.MomentInterval;

public class TimerExpressionChecker extends AbstractElementChecker {

    public TimerExpressionChecker(final Rule rule, final BpmnScanner bpmnScanner) {
        super(rule, bpmnScanner);
    }

    /**
     * Check TimerEvents for correct usage of ISO 8601 and CRON definitions
     *
     * @return issues
     */
    @Override
    public Collection<CheckerIssue> check(final BpmnElement element) {

        final BaseElement baseElement = element.getBaseElement();
        final Collection<CheckerIssue> issues = new ArrayList<CheckerIssue>();

        // Map with string (contains the timer definiton) and the element itself (contains name and id)
        Map<Element, Element> list = new HashMap<>();

        // check if the element is an event and retrieve id
        if (baseElement.getId() != null && (baseElement instanceof IntermediateCatchEvent
                || baseElement instanceof StartEvent || baseElement instanceof BoundaryEvent)) {

            list = bpmnScanner.getTimerImplementation(baseElement.getId());
            String timerDefinition;

            for (Map.Entry<Element, Element> entry : list.entrySet()) {

                if (entry.getValue() != null) {
                    timerDefinition = entry.getValue().getParentNode().getTextContent().trim();
                } else {
                    timerDefinition = "";
                }

                if (timerDefinition != null && !timerDefinition.trim().isEmpty()) {

                    // BpmnModelConstants.BPMN_ELEMENT_TIME_DATE
                    if (entry.getValue() != null && (entry.getValue().getNodeName() != null
                            && entry.getValue().getNodeName().contains(BpmnModelConstants.BPMN_ELEMENT_TIME_DATE))) {
                        try {
                            DatatypeConverter.parseDateTime(timerDefinition);
                        } catch (Exception e) {
                            issues.add(IssueWriter.createIssue(rule, CriticalityEnum.ERROR, element, entry,
                                    "Time event '" + CheckName.checkTimer(entry.getKey())
                                            + "' does not follow the ISO 8601 scheme for timeDates."));
                        }
                    }
                    // BpmnModelConstants.BPMN_ELEMENT_TIME_DURATION
                    if (entry.getValue() != null && (entry.getValue().getNodeName() != null
                            && entry.getValue().getNodeName()
                                    .contains(BpmnModelConstants.BPMN_ELEMENT_TIME_DURATION))) {
                        try {
                            DatatypeFactory.newInstance().newDuration(timerDefinition);
                        } catch (Exception e) {
                            issues.add(IssueWriter.createIssue(rule, CriticalityEnum.ERROR, element, entry,
                                    "Time event '" + CheckName.checkTimer(entry.getKey())
                                            + "' does not follow the ISO 8601 scheme for timeDuration."));
                        }
                    }
                    // BpmnModelConstants.BPMN_ELEMENT_TIME_CYCLE
                    if (entry.getValue() != null && (entry.getValue().getNodeName() != null
                            && entry.getValue().getNodeName().contains(BpmnModelConstants.BPMN_ELEMENT_TIME_CYCLE))) {

                        boolean isCron = false;
                        boolean isDur = false;
                        boolean hasRepeatingIntervals = false;

                        if (!timerDefinition.contains("P") && !timerDefinition.contains("Z")
                                && timerDefinition.contains(" ")) {
                            isCron = true;
                        }

                        if (timerDefinition.startsWith("R")) {
                            hasRepeatingIntervals = true;
                        }

                        if (timerDefinition.startsWith("P")
                                && !(timerDefinition.contains("/") || timerDefinition.contains("--"))) {
                            isDur = true;
                        }

                        if (isCron) {
                            try {
                                CronDefinition cronDef = CronDefinitionBuilder
                                        .instanceDefinitionFor(CronType.QUARTZ);
                                CronParser cronParser = new CronParser(cronDef);
                                Cron cronJob = cronParser.parse(timerDefinition);
                                cronJob.validate();
                            } catch (IllegalArgumentException e) {
                                issues.add(IssueWriter.createIssue(rule, CriticalityEnum.ERROR, element, entry,
                                        "Time event '" + CheckName.checkTimer(entry.getKey())
                                                + "' does not follow the scheme for CRON jobs."));
                            }
                        }

                        if (!isCron && !hasRepeatingIntervals && !isDur) {
                            try {
                                MomentInterval.parseISO(timerDefinition);
                            } catch (ParseException e) {
                                issues.add(IssueWriter.createIssue(rule, CriticalityEnum.ERROR, element, entry,
                                        "Time event '" + CheckName.checkTimer(entry.getKey())
                                                + "' does not follow the ISO 8601 scheme for intervals."));
                            }
                        }

                        if (!isCron && hasRepeatingIntervals && !isDur) {
                            try {
                                IsoRecurrence.parseMomentIntervals(timerDefinition);
                            } catch (ParseException ex) {
                                issues.add(IssueWriter.createIssue(rule, CriticalityEnum.ERROR, element, entry,
                                        "Time event '" + CheckName.checkTimer(entry.getKey())
                                                + "' does not follow the ISO 8601 scheme for repeating intervals."));
                            }
                        }

                        if (isDur && !isCron && !hasRepeatingIntervals) {
                            try {
                                DatatypeFactory.newInstance().newDuration(timerDefinition);
                            } catch (Exception ex) {
                                issues.add(IssueWriter.createIssue(rule, CriticalityEnum.ERROR, element, entry,
                                        "Time event '" + CheckName.checkTimer(entry.getKey())
                                                + "' does not follow the ISO 8601 scheme for durations as interval."));
                            }
                        }
                    }

                } else if (entry.getValue() == null || entry.getValue().getLocalName() == null
                        || entry.getValue().getLocalName().isEmpty()) {
                    issues.add(IssueWriter.createIssue(rule, CriticalityEnum.ERROR, element, entry,
                            "Time event '" + CheckName.checkTimer(entry.getKey())
                                    + "' has no timer definition type specified"));

                } else if (timerDefinition == null || timerDefinition.trim().isEmpty()) {
                    issues.add(IssueWriter.createIssue(rule, CriticalityEnum.ERROR, element, entry,
                            "Time event '" + CheckName.checkTimer(entry.getKey())
                                    + "' has no timer definition specified"));
                }
            }
        }

        return issues;
    }
}
