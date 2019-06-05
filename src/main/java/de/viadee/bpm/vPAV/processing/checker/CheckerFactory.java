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
package de.viadee.bpm.vPAV.processing.checker;

import de.viadee.bpm.vPAV.BpmnScanner;
import de.viadee.bpm.vPAV.Messages;
import de.viadee.bpm.vPAV.RuntimeConfig;
import de.viadee.bpm.vPAV.config.model.Rule;
import de.viadee.bpm.vPAV.config.model.RuleSet;
import de.viadee.bpm.vPAV.constants.BpmnConstants;
import de.viadee.bpm.vPAV.constants.ConfigConstants;
import de.viadee.bpm.vPAV.processing.ProcessVariablesScanner;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.logging.Logger;

/**
 * Factory decides which Checkers will be used in defined situations
 */
public class CheckerFactory {

    private static final Logger LOGGER = Logger.getLogger(CheckerFactory.class.getName());

    private Map<String, String> incorrectCheckers = new HashMap<>();

    /**
     * create checkers
     *
     * @param ruleConf                rules for checker
     * @param resourcesNewestVersions resourcesNewestVersions in context
     * @param bpmnScanner             bpmnScanner for model
     * @param scanner                 ProcessVariablesScanner for process variables, if active
     * @return checkers returns checkers
     */
    public Collection<ElementChecker> createElementCheckerInstances(final Map<String, Map<String, Rule>> ruleConf,
                                                             final Collection<String> resourcesNewestVersions, final BpmnScanner bpmnScanner,
                                                             final ProcessVariablesScanner scanner) {

        final HashSet<String> instantiatedCheckerClasses = new HashSet<>();
        final Collection<ElementChecker> checkers = new ArrayList<>();
        AbstractElementChecker newChecker;

        for (Map<String, Rule> rules : ruleConf.values()) {
            for (Rule rule : rules.values()) {
                String fullyQualifiedName = getFullyQualifiedName(rule);

                if (rule.getName().equals(ConfigConstants.CREATE_OUTPUT_RULE)) {
                    continue;
                }

                if (!fullyQualifiedName.isEmpty() && !rule.getName().equals("ProcessVariablesModelChecker")
                        && !rule.getName().equals("DataFlowChecker")) { //$NON-NLS-1$
                    try {
                        if (!rule.getName().equals("VersioningChecker") && !rule.getName()
                                .equals("MessageCorrelationChecker")) { //$NON-NLS-1$
                            Class<?> clazz = Class.forName(fullyQualifiedName);
                            Constructor<?> c = clazz.getConstructor(Rule.class, BpmnScanner.class);
                            newChecker = (AbstractElementChecker) c.newInstance(rule, bpmnScanner);
                        } else if (scanner != null && rule.getName().equals("MessageCorrelationChecker")) {
                            Class<?> clazz = Class.forName(fullyQualifiedName);
                            Constructor<?> c = clazz.getConstructor(Rule.class, BpmnScanner.class,
                                    ProcessVariablesScanner.class);
                            newChecker = (AbstractElementChecker) c.newInstance(rule, bpmnScanner, scanner);
                        } else {
                            Class<?> clazz = Class.forName(fullyQualifiedName);
                            Constructor<?> c = clazz.getConstructor(Rule.class, BpmnScanner.class, Collection.class);
                            newChecker = (AbstractElementChecker) c.newInstance(rule, bpmnScanner,
                                    resourcesNewestVersions);
                        }

                        // Check if checker is singleton and if an instance already exists
                        if (instantiatedCheckerClasses.contains(fullyQualifiedName)) {
                            if (newChecker.isSingletonChecker()) {
                                // Multiple instances of a singleton checker are considered incorrect
                                this.setIncorrectCheckers(rule, String.format(Messages.getString("CheckerFactory.9"), //$NON-NLS-1$
                                        rule.getName()));
                                LOGGER.warning("Multiple rule definitions of checker '" + rule.getName() + "' found. Only the first rule will be applied.");
                            } else {
                                checkers.add(newChecker);
                            }
                        } else {
                            instantiatedCheckerClasses.add(fullyQualifiedName);
                            checkers.add(newChecker);
                        }
                    } catch (NoSuchMethodException | SecurityException | ClassNotFoundException | IllegalAccessException
                            | IllegalArgumentException | InvocationTargetException | InstantiationException e) {
                        LOGGER.warning("Class " + fullyQualifiedName
                                + " not found or couldn't be instantiated"); //$NON-NLS-1$ //$NON-NLS-2$
                        rule.deactivate();
                    }
                }
            }
        }

        return checkers;
    }

    public Collection<ModelChecker> createModelCheckerInstances(final RuleSet ruleConf,
                                                                final Collection<String> resourcesNewestVersions, final BpmnScanner bpmnScanner,
                                                                final ProcessVariablesScanner scanner) {
        final Collection<ModelChecker> checkers = new ArrayList<>();
        return checkers;
    }

    /**
     * get the fullyQualifiedName of the rule
     *
     * @param rule Rule in Map
     * @return fullyQualifiedName
     */
    private String getFullyQualifiedName(Rule rule) {
        String fullyQualifiedName = ""; //$NON-NLS-1$

        if (rule.isInternalRule()) {
            fullyQualifiedName = BpmnConstants.INTERN_LOCATION + rule.getName().trim();
        } else if (rule.getSettings() != null
                && rule.getSettings().containsKey(BpmnConstants.EXTERN_LOCATION)) {
            fullyQualifiedName =
                    rule.getSettings().get(BpmnConstants.EXTERN_LOCATION).getValue() + "." //$NON-NLS-1$
                            + rule.getName().trim();
        }

        if (fullyQualifiedName.isEmpty()) {
            LOGGER.warning("Checker '" + rule.getName() //$NON-NLS-1$
                    + "' not found. Please add setting for external_location in ruleSet.xml."); //$NON-NLS-1$
            rule.deactivate();

            setIncorrectCheckers(rule, String.format(Messages.getString("CheckerFactory.8"), //$NON-NLS-1$
                    rule.getName()));
        }
        return fullyQualifiedName;
    }

    public void setIncorrectCheckers(final Rule rule, final String message) {
        if (!getIncorrectCheckers().containsKey(rule.getName())) {
            this.incorrectCheckers.put(rule.getName(), message);
        }
    }

    public Map<String, String> getIncorrectCheckers() {
        return this.incorrectCheckers;
    }
}
