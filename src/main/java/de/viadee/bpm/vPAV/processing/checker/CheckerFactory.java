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
package de.viadee.bpm.vPAV.processing.checker;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import de.viadee.bpm.vPAV.BpmnScanner;
import de.viadee.bpm.vPAV.Messages;
import de.viadee.bpm.vPAV.RuntimeConfig;
import de.viadee.bpm.vPAV.config.model.Rule;
import de.viadee.bpm.vPAV.constants.BpmnConstants;
import de.viadee.bpm.vPAV.constants.ConfigConstants;

/**
 * Factory decides which Checkers will be used in defined situations
 *
 */
public class CheckerFactory {

	private static final Logger LOGGER = Logger.getLogger(CheckerFactory.class.getName());
	
	private Map<String, String> incorrectCheckers = new HashMap<>();

	/**
	 * create checkers
	 *
	 * @param ruleConf
	 *            rules for checker
	 * @param resourcesNewestVersions
	 *            resourcesNewestVersions in context
	 * @param bpmnScanner
	 *            bpmnScanner for model
	 * @return checkers returns checkers
	 */
	public Collection<ElementChecker> createCheckerInstances(final Map<String, Rule> ruleConf,
			final Collection<String> resourcesNewestVersions, final BpmnScanner bpmnScanner) {

		final Collection<ElementChecker> checkers = new ArrayList<ElementChecker>();

		for (Map.Entry<String, Rule> rule : ruleConf.entrySet()) {
			String fullyQualifiedName = getFullyQualifiedName(rule);

			if (rule.getKey().equals(ConfigConstants.CREATE_OUTPUT_RULE)) {
				continue;
			}

			if (!fullyQualifiedName.isEmpty() && !rule.getKey().equals("ProcessVariablesModelChecker")
					&& !rule.getKey().equals("DataFlowChecker")) { //$NON-NLS-1$
				try {					
					if (!rule.getKey().equals("VersioningChecker")) { //$NON-NLS-1$
						Class clazz = Class.forName(fullyQualifiedName);
						Constructor<?> c = clazz.getConstructor(Rule.class, BpmnScanner.class);
						checkers.add((AbstractElementChecker) c.newInstance(rule.getValue(), bpmnScanner));
					} else {
						Class clazz = Class.forName(fullyQualifiedName);
						Constructor<?> c = clazz.getConstructor(Rule.class, BpmnScanner.class, Collection.class);
						checkers.add((AbstractElementChecker) c.newInstance(rule.getValue(), bpmnScanner, resourcesNewestVersions));
					}
				} catch (NoSuchMethodException | SecurityException | ClassNotFoundException | IllegalAccessException
						| IllegalArgumentException | InvocationTargetException | InstantiationException e) {
					LOGGER.warning("Class " + fullyQualifiedName + " not found or couldn't be instantiated"); //$NON-NLS-1$ //$NON-NLS-2$
					rule.getValue().deactivate();
				}
			}
		}
		return checkers;
	}

	/**
	 * get the fullyQualifiedName of the rule
	 *
	 * @param rule
	 *            Rule in Map
	 * @return fullyQualifiedName
	 */
	private String getFullyQualifiedName(Map.Entry<String, Rule> rule) {
		String fullyQualifiedName = ""; //$NON-NLS-1$
		if (Arrays.asList(RuntimeConfig.getInstance().getViadeeRules()).contains(rule.getKey())
				&& rule.getValue().isActive()) {
			fullyQualifiedName = BpmnConstants.INTERN_LOCATION + rule.getValue().getName().trim();
		} else if (rule.getValue().isActive() && rule.getValue().getSettings() != null
				&& rule.getValue().getSettings().containsKey(BpmnConstants.EXTERN_LOCATION)) {
			fullyQualifiedName = rule.getValue().getSettings().get(BpmnConstants.EXTERN_LOCATION).getValue() + "." //$NON-NLS-1$
					+ rule.getValue().getName().trim();
		}
		if (fullyQualifiedName.isEmpty() && rule.getValue().isActive()) {
			LOGGER.warning("Checker '" + rule.getValue().getName() //$NON-NLS-1$
					+ "' not found. Please add setting for external_location in ruleSet.xml."); //$NON-NLS-1$
			rule.getValue().deactivate();
			
			setIncorrectCheckers(rule, String.format(Messages.getString("CheckerFactory.8"), //$NON-NLS-1$
					rule.getValue().getName()));
		}
		return fullyQualifiedName;
	}
	
	public void setIncorrectCheckers(final Map.Entry<String, Rule> rule, final String message) {
		if (!getIncorrectCheckers().containsKey(rule.getValue().getName())) {
			this.incorrectCheckers.put(rule.getValue().getName(), message);
		}
	}
	
	public Map<String, String> getIncorrectCheckers() {
		return this.incorrectCheckers;
	}
}
