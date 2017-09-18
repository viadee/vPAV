/**
 * Copyright � 2017, viadee Unternehmensberatung GmbH
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

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.DatatypeConverter;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.camunda.bpm.model.bpmn.instance.BaseElement;
import org.camunda.bpm.model.bpmn.instance.BoundaryEvent;
import org.camunda.bpm.model.bpmn.instance.IntermediateCatchEvent;
import org.camunda.bpm.model.bpmn.instance.StartEvent;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.parser.CronParser;

import de.viadee.bpm.vPAV.BPMNScanner;
import de.viadee.bpm.vPAV.config.model.Rule;
import de.viadee.bpm.vPAV.processing.CheckName;
import de.viadee.bpm.vPAV.processing.model.data.BpmnElement;
import de.viadee.bpm.vPAV.processing.model.data.CheckerIssue;
import de.viadee.bpm.vPAV.processing.model.data.CriticalityEnum;
import net.time4j.Duration;
import net.time4j.range.IsoRecurrence;
import net.time4j.range.MomentInterval;

public class TimerExpressionChecker extends AbstractElementChecker {

    final String path;

    final String timeDate = "timeDate";

    final String timeDuration = "timeDuration";

    final String timeCycle = "timeCycle";

    public TimerExpressionChecker(final Rule rule, final String path) {
        super(rule);
        this.path = path;
    }

    @Override
    public Collection<CheckerIssue> check(final BpmnElement element) {

        final BaseElement baseElement = element.getBaseElement();
        final Collection<CheckerIssue> issues = new ArrayList<CheckerIssue>();

        // Map with string (contains the timer definiton) and the element itself (contains name and id)
        Map<Element, Element> list = new HashMap<>();
        final BPMNScanner scan;

        // check if the element is an event and retrieve id
        if (baseElement.getId() != null && (baseElement instanceof IntermediateCatchEvent
                || baseElement instanceof StartEvent || baseElement instanceof BoundaryEvent)) {

            try {

                scan = new BPMNScanner();
                list = scan.getTimerImplementation(path, baseElement.getId());
                String timerDefinition;

                for (Map.Entry<Element, Element> entry : list.entrySet()) {

                    if (entry.getValue() != null) {
                        timerDefinition = entry.getValue().getParentNode().getTextContent().trim();
                    } else {
                        timerDefinition = "";
                    }

                    if (timerDefinition != null && !timerDefinition.trim().isEmpty()) {

                        // timeDate
                        if (entry.getValue() != null && (entry.getValue().getNodeName() != null
                                && entry.getValue().getNodeName().contains(timeDate))) {
                            try {
                                DatatypeConverter.parseDateTime(timerDefinition);
                            } catch (Exception e) {
                                issues.add(new CheckerIssue(rule.getName(), CriticalityEnum.WARNING,
                                        element.getProcessdefinition(), null, entry.getKey().getAttribute("id"),
                                        baseElement.getAttributeValue("name"), null, null, null,
                                        "time event '" + CheckName.checkTimer(entry.getKey())
                                                + "' does not follow the ISO 8601 scheme for timeDates."));
                            }
                        }
                        // timeDuration
                        if (entry.getValue() != null && (entry.getValue().getNodeName() != null
                                && entry.getValue().getNodeName().contains(timeDuration))) {
                            try {
                                DatatypeFactory.newInstance().newDuration(timerDefinition);
                            } catch (Exception e) {
                                issues.add(new CheckerIssue(rule.getName(), CriticalityEnum.WARNING,
                                        element.getProcessdefinition(), null, entry.getKey().getAttribute("id"),
                                        baseElement.getAttributeValue("name"), null, null, null,
                                        "time event '" + CheckName.checkTimer(entry.getKey())
                                                + "' does not follow the ISO 8601 scheme for timeDuration."));
                            }
                        }
                        // timeCycle
                        if (entry.getValue() != null && (entry.getValue().getNodeName() != null
                                && entry.getValue().getNodeName().contains(timeCycle))) {

                            boolean isParsed = false;
                            boolean isCron = false;
                            boolean hasRepeatingIntervals = false;

                            if (!timerDefinition.contains("P") && !timerDefinition.contains("Z")) {
                                isCron = true;
                            }

                            if (timerDefinition.startsWith("R")) {
                                hasRepeatingIntervals = true;
                            }

                            if (!isParsed && isCron) {
                                try {
                                    CronDefinition cronDef = CronDefinitionBuilder
                                            .instanceDefinitionFor(CronType.QUARTZ);
                                    CronParser cronParser = new CronParser(cronDef);
                                    Cron cronJob = cronParser.parse(timerDefinition);
                                    cronJob.validate();
                                    isParsed = true;
                                } catch (IllegalArgumentException e) {
                                    issues.add(new CheckerIssue(rule.getName(), CriticalityEnum.WARNING,
                                            element.getProcessdefinition(), null, entry.getKey().getAttribute("id"),
                                            baseElement.getAttributeValue("name"), null, null, null,
                                            "time event '" + CheckName.checkTimer(entry.getKey())
                                                    + "' does not follow the scheme for CRON jobs."));
                                }
                            }

                            if (!isParsed && !isCron && !hasRepeatingIntervals) {
                                try {
                                    MomentInterval.parseISO(timerDefinition);
                                    isParsed = true;
                                } catch (ParseException e) {
                                    issues.add(new CheckerIssue(rule.getName(), CriticalityEnum.WARNING,
                                            element.getProcessdefinition(), null, entry.getKey().getAttribute("id"),
                                            baseElement.getAttributeValue("name"), null, null, null,
                                            "time event '" + CheckName.checkTimer(entry.getKey())
                                                    + "' does not follow the ISO 8601 scheme for intervals."));
                                }
                            }

                            if (!isParsed && !isCron && !hasRepeatingIntervals) {
                                try {
                                    Duration.parsePeriod(timerDefinition);
                                } catch (ParseException ex) {
                                    issues.add(new CheckerIssue(rule.getName(), CriticalityEnum.WARNING,
                                            element.getProcessdefinition(), null, entry.getKey().getAttribute("id"),
                                            baseElement.getAttributeValue("name"), null, null, null,
                                            "time event '" + CheckName.checkTimer(entry.getKey())
                                                    + "' does not follow the ISO 8601 scheme for periods."));
                                    isParsed = true;
                                }
                            }

                            if (!isParsed && !isCron && hasRepeatingIntervals) {
                                try {
                                    IsoRecurrence<MomentInterval> ir = IsoRecurrence
                                            .parseMomentIntervals(timerDefinition);
                                } catch (ParseException ex) {
                                    issues.add(new CheckerIssue(rule.getName(), CriticalityEnum.WARNING,
                                            element.getProcessdefinition(), null, entry.getKey().getAttribute("id"),
                                            baseElement.getAttributeValue("name"), null, null, null,
                                            "time event '" + CheckName.checkTimer(entry.getKey())
                                                    + "' does not follow the ISO 8601 scheme for repeating intervals."));
                                }
                            }

                        }

                    } else if (entry.getValue() == null || entry.getValue().getLocalName() == null
                            || entry.getValue().getLocalName().isEmpty()) {
                        issues.add(new CheckerIssue(rule.getName(), CriticalityEnum.WARNING,
                                element.getProcessdefinition(), null, entry.getKey().getAttribute("id"),
                                baseElement.getAttributeValue("name"), null, null, null,
                                "time event '" + CheckName.checkTimer(entry.getKey())
                                        + "' has no timer definition type specified "));

                    } else if (timerDefinition == null || timerDefinition.trim().isEmpty()) {
                        issues.add(new CheckerIssue(rule.getName(), CriticalityEnum.WARNING,
                                element.getProcessdefinition(), null, entry.getKey().getAttribute("id"),
                                baseElement.getAttributeValue("name"), null, null, null,
                                "time event '" + CheckName.checkTimer(entry.getKey())
                                        + "' has no timer definition specified "));
                    }
                }

            } catch (SAXException | IOException | ParserConfigurationException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }

        return issues;
    }
}
