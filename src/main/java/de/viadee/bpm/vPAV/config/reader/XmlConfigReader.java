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
package de.viadee.bpm.vPAV.config.reader;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import de.viadee.bpm.vPAV.RuntimeConfig;
import de.viadee.bpm.vPAV.config.model.ElementConvention;
import de.viadee.bpm.vPAV.config.model.ElementFieldTypes;
import de.viadee.bpm.vPAV.config.model.ModelConvention;
import de.viadee.bpm.vPAV.config.model.Rule;
import de.viadee.bpm.vPAV.config.model.Setting;

/**
 * Used to read the config file (ruleSet.xml) and extract the configured rules
 * Requirements: Exisiting ruleSet.xml in src/test/resources
 */
public final class XmlConfigReader implements ConfigReader {

	/**
	 * @throws ConfigReaderException
	 *             If file can not be found in classpath
	 */
	@Override
	public Map<String, Rule> read(final String file) throws ConfigReaderException {

		try {
			final JAXBContext jaxbContext = JAXBContext.newInstance(XmlRuleSet.class);
			final Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();

			InputStream fRuleSet = RuntimeConfig.getInstance().getClassLoader().getResourceAsStream(file);

			if (fRuleSet != null) {
				final XmlRuleSet ruleSet = (XmlRuleSet) jaxbUnmarshaller.unmarshal(fRuleSet);
				return transformFromXmlDatastructues(ruleSet);
			} else {
				throw new ConfigReaderException("ConfigFile could not be found");
			}
		} catch (JAXBException e) {
			throw new ConfigReaderException(e);
		}
	}

	/**
	 * Retrieves all rules, by default deactivated
	 *
	 * @return rules
	 */
	public Map<String, Rule> getDeactivatedRuleSet() {
		final Map<String, Rule> rules = new HashMap<String, Rule>();

		for (String name : RuntimeConfig.getInstance().getViadeeRules()) {
			if (name.equals("CreateOutputHTML")) {
				rules.put(name, new Rule(name, true, null, new HashMap<String, Setting>(),
						new ArrayList<ElementConvention>(), new ArrayList<ModelConvention>()));
			} else {
				rules.put(name, new Rule(name, false, null, new HashMap<String, Setting>(),
						new ArrayList<ElementConvention>(), new ArrayList<ModelConvention>()));
			}
		}

		return rules;
	}

	/**
	 * Transforms XmlRuleSet to rules
	 *
	 * @param ruleSet
	 * @return rules
	 * @throws ConfigReaderException
	 *             If file could not be read properly
	 */
	private static Map<String, Rule> transformFromXmlDatastructues(final XmlRuleSet ruleSet)
			throws ConfigReaderException {
		final Map<String, Rule> rules = new HashMap<String, Rule>();

		final Collection<XmlRule> xmlRules = ruleSet.getRules();
		for (final XmlRule rule : xmlRules) {
			final String name = rule.getName();
			if (name == null)
				throw new ConfigReaderException("rule name is not set");
			final boolean state = rule.isState();
			final String ruleDescription = rule.getDescription();
			final Collection<XmlElementConvention> xmlElementConventions = rule.getElementConventions();
			final ArrayList<ElementConvention> elementConventions = new ArrayList<ElementConvention>();
			if (xmlElementConventions != null) {
				for (final XmlElementConvention xmlElementConvention : xmlElementConventions) {
					final XmlElementFieldTypes xmlElementFieldTypes = xmlElementConvention.getElementFieldTypes();
					ElementFieldTypes elementFieldTypes = null;
					if (xmlElementFieldTypes != null) {
						elementFieldTypes = new ElementFieldTypes(xmlElementFieldTypes.getElementFieldTypes(),
								xmlElementFieldTypes.isExcluded());
					}
					if (!checkRegEx(xmlElementConvention.getPattern()))
						throw new ConfigReaderException("RegEx (" + xmlElementConvention.getPattern() + ") of " + name
								+ " (" + xmlElementConvention.getName() + ") is incorrect");
					elementConventions.add(new ElementConvention(xmlElementConvention.getName(), elementFieldTypes,
							xmlElementConvention.getDescription(), xmlElementConvention.getPattern()));
				}
			}
			final Collection<XmlModelConvention> xmlModelConventions = rule.getModelConventions();
			final ArrayList<ModelConvention> modelConventions = new ArrayList<ModelConvention>();
			if (xmlModelConventions != null) {
				for (final XmlModelConvention xmlModelConvention : xmlModelConventions) {
					modelConventions.add(new ModelConvention(xmlModelConvention.getType()));
				}
			}
			final Collection<XmlSetting> xmlSettings = rule.getSettings();
			final Map<String, Setting> settings = new HashMap<String, Setting>();
			if (xmlSettings != null) {
				for (final XmlSetting xmlSetting : xmlSettings) {
					if (!settings.containsKey(xmlSetting.getName())) {
						settings.put(xmlSetting.getName(),
								new Setting(xmlSetting.getName(), xmlSetting.getScript(), xmlSetting.getType(),
										xmlSetting.getId(), xmlSetting.getRequired(), xmlSetting.getValue()));
					} else {
						settings.get(xmlSetting.getName()).addScriptPlace(xmlSetting.getScript());
					}
				}
			}
			rules.put(name, new Rule(name, state, ruleDescription, settings, elementConventions, modelConventions));
		}

		return rules;
	}

	private static boolean checkRegEx(String regEx) {
		boolean correct = false;

		if (regEx.isEmpty())
			return correct;

		try {
			Pattern.compile(regEx);
			correct = true;
		} catch (PatternSyntaxException e) {
		}
		return correct;
	}
}
