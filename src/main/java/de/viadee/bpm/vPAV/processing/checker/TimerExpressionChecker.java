/*
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
package de.viadee.bpm.vPAV.processing.checker;

import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.parser.CronParser;
import de.viadee.bpm.vPAV.BpmnScanner;
import de.viadee.bpm.vPAV.Messages;
import de.viadee.bpm.vPAV.config.model.Rule;
import de.viadee.bpm.vPAV.output.IssueWriter;
import de.viadee.bpm.vPAV.processing.code.flow.BpmnElement;
import de.viadee.bpm.vPAV.processing.model.data.CheckerIssue;
import de.viadee.bpm.vPAV.processing.model.data.CriticalityEnum;
import net.time4j.range.IsoRecurrence;
import net.time4j.range.MomentInterval;
import org.camunda.bpm.engine.impl.calendar.DurationHelper;
import org.camunda.bpm.model.bpmn.instance.*;

import javax.xml.bind.DatatypeConverter;
import javax.xml.datatype.DatatypeFactory;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;

public class TimerExpressionChecker extends AbstractElementChecker {

    public TimerExpressionChecker(final Rule rule) {
        super(rule);
    }

    /**
     * Check TimerEvents for correct usage of ISO 8601 and CRON definitions
     *
     * @return issues
     */
    @Override
    public Collection<CheckerIssue> check(final BpmnElement element) {

        final BaseElement baseElement = element.getBaseElement();
        final Collection<CheckerIssue> issues = new ArrayList<>();

        // Map with string (contains the timer definiton) and the element itself
        // (contains name and id)

        // check if the element is an event and retrieve id
        if (baseElement.getId() != null && (baseElement instanceof IntermediateCatchEvent
                || baseElement instanceof StartEvent || baseElement instanceof BoundaryEvent)) {

            ArrayList<TimerEventDefinition> timers = BpmnScanner.getTimerImplementation(baseElement);

            for (TimerEventDefinition timer : timers) {
                if (timer.getTimeDate() != null) {
                    try {
                        DatatypeConverter.parseDateTime(timer.getTimeDate().getTextContent());
                    } catch (Exception e) {
                        issues.addAll(IssueWriter.createIssue(rule, CriticalityEnum.ERROR, element, timer.getId(),
                                Messages.getString("TimerExpressionChecker.1")));
                    }
                }
                if (timer.getTimeDuration() != null) {
                    try {
                        DatatypeFactory.newInstance().newDuration(timer.getTimeDuration().getTextContent());
                    } catch (Exception e) {
                        issues.addAll(IssueWriter.createIssue(rule, CriticalityEnum.ERROR, element, timer.getId(),
                                Messages.getString("TimerExpressionChecker.2")));
                    }
                }
                if (timer.getTimeCycle() != null) {
                    boolean isCron = false;
                    boolean isDur = false;
                    boolean hasRepeatingIntervals = false;
                    boolean isRepeatingDur = false;
                    String timerCycleDefinition = timer.getTimeCycle().getTextContent();

                    if (!timerCycleDefinition.contains("P") && !timerCycleDefinition.contains("Z")
                            //$NON-NLS-1$ //$NON-NLS-2$
                            && timerCycleDefinition.contains(" ")) { //$NON-NLS-1$
                        isCron = true;
                    }

                    if (timerCycleDefinition.startsWith("R")) { //$NON-NLS-1$
                        hasRepeatingIntervals = true;
                    }

                    if (timerCycleDefinition.startsWith("P") //$NON-NLS-1$
                            && !(timerCycleDefinition.contains("/") || timerCycleDefinition
                            .contains("--"))) { //$NON-NLS-1$ //$NON-NLS-2$
                        isDur = true;
                    }

                    if (timerCycleDefinition.contains("/P")) {
                        isRepeatingDur = true;
                    }

                    if (isCron) {
                        try {
                            CronDefinition cronDef = CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ);
                            CronParser cronParser = new CronParser(cronDef);
                            Cron cronJob = cronParser.parse(timerCycleDefinition);
                            cronJob.validate();
                        } catch (IllegalArgumentException e) {
                            issues.addAll(IssueWriter.createIssue(rule, CriticalityEnum.ERROR, element, timer.getId(),
                                    Messages.getString("TimerExpressionChecker.10")));
                        }
                    }

                    if (!isCron && !hasRepeatingIntervals && !isDur) {
                        try {
                            MomentInterval.parseISO(timerCycleDefinition);
                        } catch (ParseException e) {
                            issues.addAll(IssueWriter.createIssue(rule, CriticalityEnum.ERROR, element, timer.getId(),
                                    Messages.getString("TimerExpressionChecker.11")));
                        }
                    }

                    if (!isCron && hasRepeatingIntervals && !isDur && !isRepeatingDur) {
                        try {
                            IsoRecurrence.parseMomentIntervals(timerCycleDefinition);
                        } catch (ParseException ex) {
                            issues.addAll(IssueWriter.createIssue(rule, CriticalityEnum.ERROR, element, timer.getId(),
                                    Messages.getString("TimerExpressionChecker.12")));
                        }
                    }

                    if (isRepeatingDur) {
                        try {
                            new DurationHelper(timerCycleDefinition, null).getDateAfter(null);
                        } catch (Exception ex) {
                            issues.addAll(IssueWriter.createIssue(rule, CriticalityEnum.ERROR, element, timer.getId(),
                                    Messages.getString("TimerExpressionChecker.12")));
                        }
                    }

                    if (isDur && !isCron && !hasRepeatingIntervals) {
                        try {
                            DatatypeFactory.newInstance().newDuration(timerCycleDefinition);
                        } catch (Exception ex) {
                            issues.addAll(IssueWriter.createIssue(rule, CriticalityEnum.ERROR, element, timer.getId(),
                                    Messages.getString("TimerExpressionChecker.13")));
                        }
                    }
                }
                if (timer.getTimeCycle() == null && timer.getTimeDuration() == null && timer.getTimeDate() == null) {
                    issues.addAll(IssueWriter.createIssue(rule, CriticalityEnum.ERROR, element, timer.getId(),
                            Messages.getString("TimerExpressionChecker.15")));
                }
            }
        }

        return issues;
    }
}
