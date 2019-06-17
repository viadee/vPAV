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
package de.viadee.bpm.vPAV.config.reader;

import de.viadee.bpm.vPAV.RuntimeConfig;
import de.viadee.bpm.vPAV.config.model.*;
import de.viadee.bpm.vPAV.constants.ConfigConstants;
import de.viadee.bpm.vPAV.processing.checker.DataFlowChecker;
import de.viadee.bpm.vPAV.processing.checker.ProcessVariablesModelChecker;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Used to read the config file (ruleSet.xml) and extract the configured rules Requirements:
 * Existing ruleSet.xml in src/test/resources
 */
public final class XmlConfigReader implements ConfigReader {
    private static final Logger LOGGER = Logger.getLogger(XmlConfigReader.class.getName());

    /**
     * @param file Location of file relative to project
     * @throws ConfigReaderException If file can not be found in classpath
     */
    @Override
    public Map<String, Map<String, Rule>> read(final String file) throws ConfigReaderException {

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
    public Map<String, Map<String, Rule>> getDeactivatedRuleSet() {
        final Map<String, Map<String, Rule>> rules = new HashMap<>();
        Map<String, Rule> newrule;

        for (String name : RuntimeConfig.getInstance().getViadeeRules()) {
            newrule = new HashMap<>();

            if (name.equals("CreateOutputHTML")) {
                newrule.put(
                        name,
                        new Rule(
                                name,
                                true,
                                null,
                                new HashMap<String, Setting>(),
                                new ArrayList<ElementConvention>(),
                                new ArrayList<ModelConvention>()));
            } else {
                newrule.put(
                        name,
                        new Rule(
                                name,
                                false,
                                null,
                                new HashMap<String, Setting>(),
                                new ArrayList<ElementConvention>(),
                                new ArrayList<ModelConvention>()));
            }
            rules.put(name, newrule);
        }

        return rules;
    }

    /**
     * Transforms XmlRuleSet to rules
     *
     * @param ruleSet
     * @return rules
     * @throws ConfigReaderException If file could not be read properly
     */
    private static Map<String, Map<String, Rule>> transformFromXmlDatastructues(
            final XmlRuleSet ruleSet) throws ConfigReaderException {
        final Map<String, Map<String, Rule>> rules = new HashMap<>();

        final Collection<XmlRule> xmlRules = ruleSet.getRules();
        for (final XmlRule rule : xmlRules) {
            final String id = (rule.getId() == null) ? rule.getName() : rule.getId();
            final String name = rule.getName();
            if (name == null) throw new ConfigReaderException("rule name is not set");
            final boolean state = rule.isState();
            final String ruleDescription = rule.getDescription();
            final Collection<XmlElementConvention> xmlElementConventions = rule.getElementConventions();
            final ArrayList<ElementConvention> elementConventions = new ArrayList<ElementConvention>();
            if (xmlElementConventions != null) {
                for (final XmlElementConvention xmlElementConvention : xmlElementConventions) {
                    final XmlElementFieldTypes xmlElementFieldTypes =
                            xmlElementConvention.getElementFieldTypes();
                    ElementFieldTypes elementFieldTypes = null;
                    if (xmlElementFieldTypes != null) {
                        elementFieldTypes =
                                new ElementFieldTypes(
                                        xmlElementFieldTypes.getElementFieldTypes(), xmlElementFieldTypes.isExcluded());
                    }
                    if (!checkRegEx(xmlElementConvention.getPattern()))
                        throw new ConfigReaderException(
                                "RegEx ("
                                        + xmlElementConvention.getPattern()
                                        + ") of "
                                        + name
                                        + " ("
                                        + xmlElementConvention.getName()
                                        + ") is incorrect");
                    elementConventions.add(
                            new ElementConvention(
                                    xmlElementConvention.getName(),
                                    elementFieldTypes,
                                    xmlElementConvention.getDescription(),
                                    xmlElementConvention.getPattern()));
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
                        settings.put(
                                xmlSetting.getName(),
                                new Setting(
                                        xmlSetting.getName(),
                                        xmlSetting.getScript(),
                                        xmlSetting.getType(),
                                        xmlSetting.getId(),
                                        xmlSetting.getRequired(),
                                        xmlSetting.getValue()));
                    } else {
                        settings.get(xmlSetting.getName()).addScriptPlace(xmlSetting.getScript());
                    }
                }
            }

            if (!rules.containsKey(name)) {
                rules.put(name, new HashMap<String, Rule>());
            }
            rules.get(name).put(id, new Rule(id, name, state, ruleDescription, settings, elementConventions, modelConventions));
        }

        // TODO as soon as we finally move the properties to an external file, we don't need this checks anymore
        // Some rules are only allowed once. Check this.
        checkSingletonRule(rules, ConfigConstants.HASPARENTRULESET);
        checkSingletonRule(rules, ConfigConstants.CREATE_OUTPUT_RULE);
        checkSingletonRule(rules, "language");

        if (ProcessVariablesModelChecker.isSingletonChecker()) {
            checkSingletonRule(rules, ProcessVariablesModelChecker.class.getSimpleName());
        }

        if (DataFlowChecker.isSingletonChecker()) {
            checkSingletonRule(rules, DataFlowChecker.class.getSimpleName());
        }

        return rules;
    }

    private static void checkSingletonRule(Map<String, Map<String, Rule>> rules, String rulename) {
        Map<String, Rule> rulesSubset = rules.get(rulename);
        if (rulesSubset != null && (rulesSubset.size() > 1 || rulesSubset.get(rulename) == null)) {
            LOGGER.severe("Rule '" + rulename + "' is only allowed once and without defining an ID.");
        }
    }

    private static boolean checkRegEx(String regEx) {
        boolean correct = false;

        if (regEx.isEmpty()) return correct;

        try {
            Pattern.compile(regEx);
            correct = true;
        } catch (PatternSyntaxException e) {
        }
        return correct;
    }
}
