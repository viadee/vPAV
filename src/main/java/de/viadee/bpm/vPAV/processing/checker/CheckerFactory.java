/*
 * BSD 3-Clause License
 *
 * Copyright © 2020, viadee Unternehmensberatung AG
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

import de.viadee.bpm.vPAV.Messages;
import de.viadee.bpm.vPAV.RuntimeConfig;
import de.viadee.bpm.vPAV.config.model.Rule;
import de.viadee.bpm.vPAV.config.model.RuleSet;
import de.viadee.bpm.vPAV.constants.BpmnConstants;
import de.viadee.bpm.vPAV.processing.EntryPointScanner;
import de.viadee.bpm.vPAV.processing.code.flow.FlowAnalysis;
import de.viadee.bpm.vPAV.processing.dataflow.DataFlowRule;
import de.viadee.bpm.vPAV.processing.model.data.AnomalyContainer;
import de.viadee.bpm.vPAV.processing.model.data.ProcessVariable;
import de.viadee.bpm.vPAV.processing.model.graph.Path;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.logging.Logger;

/**
 * Factory decides which Checkers will be used in defined situations
 */
public class CheckerFactory {

    private static final Logger LOGGER = Logger.getLogger(CheckerFactory.class.getName());

    private final Map<String, String> incorrectCheckers = new HashMap<>();

    /**
     * create checkers
     *
     * @param ruleConf                rules for checker
     * @param resourcesNewestVersions resourcesNewestVersions in context
     * @param scanner                 ProcessVariablesScanner for process variables, if active
     * @param dataFlowRules           dataFlowRules
     * @param processVariables        Process Variables
     * @param invalidPathMap          invalidPathMap
     * @return checkers returns checkers
     */
    public Collection<Checker>[] createCheckerInstances(final RuleSet ruleConf,
            final Collection<String> resourcesNewestVersions,
            final EntryPointScanner scanner,
            final Collection<DataFlowRule> dataFlowRules, final Collection<ProcessVariable> processVariables,
            final Map<AnomalyContainer, List<Path>> invalidPathMap, final FlowAnalysis flowAnalysis) {

        final HashSet<String> instantiatedCheckerClasses = new HashSet<>();
        final Collection<Checker>[] checkers = new Collection[2];
        checkers[0] = createElementCheckers(instantiatedCheckerClasses, ruleConf, resourcesNewestVersions,
                scanner);
        checkers[1] = createModelCheckers(instantiatedCheckerClasses, ruleConf, dataFlowRules,
                processVariables, invalidPathMap, flowAnalysis);

        return checkers;
    }

    private Collection<Checker> createElementCheckers(final HashSet<String> instantiatedCheckerClasses,
            final RuleSet ruleConf, final Collection<String> resourcesNewestVersions,
            final EntryPointScanner scanner) {
        final Collection<Checker> elementCheckers = new ArrayList<>();
        AbstractElementChecker newChecker;
        // Create element checkers.
        for (Map<String, Rule> rules : ruleConf.getElementRules().values()) {
            for (Rule rule : rules.values()) {
                String fullyQualifiedName = getFullyQualifiedName(rule);

                if (rule.isActive() && !fullyQualifiedName.isEmpty()) { // $NON-NLS-1$
                    try {
                        if (!rule.getName().equals("VersioningChecker")
                                && !rule.getName().equals("MessageCorrelationChecker")) { //$NON-NLS-1$
                            Class<?> clazz = Class.forName(fullyQualifiedName);
                            Constructor<?> c = clazz.getConstructor(Rule.class);
                            newChecker = (AbstractElementChecker) c.newInstance(rule);
                        } else if (scanner != null && rule.getName().equals("MessageCorrelationChecker")) {
                            Class<?> clazz = Class.forName(fullyQualifiedName);
                            Constructor<?> c = clazz.getConstructor(Rule.class,
                                    EntryPointScanner.class);
                            newChecker = (AbstractElementChecker) c.newInstance(rule, scanner);
                        } else {
                            Class<?> clazz = Class.forName(fullyQualifiedName);
                            Constructor<?> c = clazz.getConstructor(Rule.class, Collection.class);
                            newChecker = (AbstractElementChecker) c.newInstance(rule, resourcesNewestVersions);
                        }

                        // Check if checker is singleton and if an instance already exists
                        if (instantiatedCheckerClasses.contains(fullyQualifiedName)) {
                            if (newChecker.isSingletonChecker()) {
                                // Multiple instances of a singleton checker are considered incorrect
                                this.setIncorrectCheckers(rule,
                                        String.format(Messages.getString("CheckerFactory.9"), //$NON-NLS-1$
                                                rule.getName()));
                                LOGGER.warning("Multiple rule definitions of checker '" + rule.getName()
                                        + "' found. Only the first rule will be applied.");
                            } else {
                                elementCheckers.add(newChecker);
                            }
                        } else {
                            instantiatedCheckerClasses.add(fullyQualifiedName);
                            elementCheckers.add(newChecker);
                        }
                    } catch (NoSuchMethodException | SecurityException | ClassNotFoundException | IllegalAccessException
                            | IllegalArgumentException | InvocationTargetException | InstantiationException e) {
                        LOGGER.warning("Class " + fullyQualifiedName
                                + " not found or couldn't be instantiated"); //$NON-NLS-2$ //$NON-NLS-2$
                        rule.deactivate();
                    }
                }
            }
        }
        return elementCheckers;
    }

    private Collection<Checker> createModelCheckers(final HashSet<String> instantiatedCheckerClasses,
            final RuleSet ruleConf, final Collection<DataFlowRule> dataFlowRules,
            final Collection<ProcessVariable> processVariables,
            final Map<AnomalyContainer, List<Path>> invalidPathMap, final FlowAnalysis flowAnalysis) {
        final Collection<Checker> modelCheckers = new ArrayList<>();
        ModelChecker newModelChecker;
        // Create model checkers.
        for (Map<String, Rule> rules : ruleConf.getModelRules().values()) {
            for (Rule rule : rules.values()) {
                String fullyQualifiedName = getFullyQualifiedName(rule);
                if (rule.isActive() && !fullyQualifiedName.isEmpty()) {
                    try {
                        Class<?> clazz = Class.forName(fullyQualifiedName);
                        if (rule.getName().equals("DataFlowChecker")) {
                            Constructor<?> c = clazz
                                    .getConstructor(Rule.class, Map.class, Collection.class, Collection.class,
                                            FlowAnalysis.class);
                            newModelChecker = (ModelChecker) c
                                    .newInstance(rule, invalidPathMap, processVariables, dataFlowRules, flowAnalysis);
                        } else {
                            Constructor<?> c = clazz
                                    .getConstructor(Rule.class, Map.class, Collection.class, FlowAnalysis.class);
                            newModelChecker = (ModelChecker) c
                                    .newInstance(rule, invalidPathMap, processVariables, flowAnalysis);
                        }

                        // Check if checker is singleton and if an instance already exists
                        if (instantiatedCheckerClasses.contains(fullyQualifiedName)) {
                            if (newModelChecker.isSingletonChecker()) {
                                // Multiple instances of a singleton checker are considered incorrect
                                this.setIncorrectCheckers(rule,
                                        String.format(Messages.getString("CheckerFactory.9"), //$NON-NLS-1$
                                                rule.getName()));
                                LOGGER.warning("Multiple rule definitions of checker '" + rule.getName()
                                        + "' found. Only the first rule will be applied.");
                            } else {
                                modelCheckers.add(newModelChecker);
                            }
                        } else {
                            instantiatedCheckerClasses.add(fullyQualifiedName);
                            modelCheckers.add(newModelChecker);
                        }

                    } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException
                            | IllegalAccessException | InvocationTargetException e) {
                        LOGGER.warning("Class " + fullyQualifiedName
                                + " not found or couldn't be instantiated"); //$NON-NLS-2$ //$NON-NLS-2$
                        rule.deactivate();
                    }
                }
            }
        }
        return modelCheckers;
    }

    /**
     * get the fullyQualifiedName of the rule
     *
     * @param rule Rule in Map
     * @return fullyQualifiedName
     */
    private String getFullyQualifiedName(Rule rule) {
        String fullyQualifiedName = ""; //$NON-NLS-1$
        if (Arrays.asList(RuntimeConfig.getInstance().getViadeeRules()).contains(rule.getName())) {
            fullyQualifiedName = BpmnConstants.INTERN_LOCATION + rule.getName().trim();
        } else if (rule.getSettings() != null && rule.getSettings().containsKey(BpmnConstants.EXTERN_LOCATION)) {
            fullyQualifiedName = rule.getSettings().get(BpmnConstants.EXTERN_LOCATION).getValue() + "." //$NON-NLS-1$
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
