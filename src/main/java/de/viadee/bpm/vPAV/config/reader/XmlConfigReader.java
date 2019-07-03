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
import de.viadee.bpm.vPAV.constants.BpmnConstants;
import de.viadee.bpm.vPAV.constants.ConfigConstants;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.InputStream;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Used to read the config file (ruleSet.xml) and extract the configured rules
 * Requirements: Existing ruleSet.xml in src/test/resources
 */
public final class XmlConfigReader implements ConfigReader {
    private static final Logger LOGGER = Logger.getLogger(XmlConfigReader.class.getName());

    /**
     * @param file Location of file relative to project
     * @throws ConfigReaderException If file can not be found in classpath
     */
    @Override
    public RuleSet read(final String file) throws ConfigReaderException {

        try {
            final JAXBContext jaxbContext = JAXBContext.newInstance(XmlRuleSet.class);
            final Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();

            InputStream fRuleSet = RuntimeConfig.getInstance().getClassLoader().getResourceAsStream(file);

            if (fRuleSet != null) {
                final XmlRuleSet ruleSet = (XmlRuleSet) jaxbUnmarshaller.unmarshal(fRuleSet);
                return transformFromXmlDatastructures(ruleSet);
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
    public RuleSet getDeactivatedRuleSet() {
        final Map<String, Map<String, Rule>> rules = new HashMap<>();
        Map<String, Rule> newrule;

        for (String name : RuntimeConfig.getInstance().getViadeeRules()) {
            newrule = new HashMap<>();

            if (name.equals("CreateOutputHTML")) {
                newrule.put(name, new Rule(name, true, null, new HashMap<>(),
                        new ArrayList<>(), new ArrayList<>()));
            } else {
                newrule.put(name, new Rule(name, false, null, new HashMap<>(),
                        new ArrayList<>(), new ArrayList<>()));
            }
            rules.put(name, newrule);
        }

        return new RuleSet(rules, new HashMap<>(), false);
    }

    /**
     * Transforms XmlRuleSet to rules
     *
     * @param ruleSet RuleSet as XmlRuleSet
     * @return rules Transformed map of rules
     * @throws ConfigReaderException If file could not be read properly
     */
    private static RuleSet transformFromXmlDatastructures(final XmlRuleSet ruleSet)
            throws ConfigReaderException {
        final Map<String, Map<String, Rule>> rules = new HashMap<>();

        final Collection<XmlRule> xmlRules = ruleSet.getRules();
        for (final XmlRule rule : xmlRules) {
            final String id = (rule.getId() == null) ? rule.getName() : rule.getId();
            final String name = rule.getName();
            if (name == null)
                throw new ConfigReaderException("rule name is not set");
            final boolean state = rule.isState();
            final String ruleDescription = rule.getDescription();
            final Collection<XmlElementConvention> xmlElementConventions = rule.getElementConventions();
            final ArrayList<ElementConvention> elementConventions = new ArrayList<>();
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
            final ArrayList<ModelConvention> modelConventions = new ArrayList<>();
            if (xmlModelConventions != null) {
                for (final XmlModelConvention xmlModelConvention : xmlModelConventions) {
                    modelConventions.add(new ModelConvention(xmlModelConvention.getType()));
                }
            }
            final Collection<XmlSetting> xmlSettings = rule.getSettings();
            final Map<String, Setting> settings = new HashMap<>();
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

            if (!rules.containsKey(name)) {
                rules.put(name, new HashMap<>());
            }
            rules.get(name).put(id,
                    new Rule(id, name, state, ruleDescription, settings, elementConventions, modelConventions));
        }

        // TODO as soon as we finally move the properties to an external file, we don't
        // need this checks anymore
        // Some rules are only allowed once. Check this.
        checkSingletonRule(rules, ConfigConstants.HASPARENTRULESET);
        checkSingletonRule(rules, ConfigConstants.CREATE_OUTPUT_RULE);
        checkSingletonRule(rules, "language");

        return splitRules(rules);
    }

    private static RuleSet splitRules(Map<String, Map<String, Rule>> rules) {
        boolean hasParentRuleSet = false;
        HashMap<String, Map<String, Rule>> elementRules = new HashMap<>();
        HashMap<String, Map<String, Rule>> modelRules = new HashMap<>();

        // Check config rules
        // TODO can be removed in future if it is in properties
        if (rules.containsKey(ConfigConstants.HASPARENTRULESET)) {
            hasParentRuleSet = rules.get(ConfigConstants.HASPARENTRULESET).get(ConfigConstants.HASPARENTRULESET).isActive();
            rules.remove(ConfigConstants.HASPARENTRULESET);
        }
        if (rules.containsKey(ConfigConstants.CREATE_OUTPUT_RULE)) {
            LOGGER.warning("Usage of 'CreateOutputHtml' rule is deprecated. Please use vpav.properties instead.");
            ConfigConstants.getInstance().setHtmlOutputEnabled(
                    rules
                            .get(ConfigConstants.CREATE_OUTPUT_RULE)
                            .get(ConfigConstants.CREATE_OUTPUT_RULE).isActive());
            rules.remove(ConfigConstants.CREATE_OUTPUT_RULE);
        }
        if (rules.containsKey("language")) {
            LOGGER.warning("Usage of 'language' rule is deprecated. Please use vpav.properties instead.");
            final Map<String, Setting> settings = rules.get("language").get("language").getSettings();
            if (settings.get("locale").getValue().equals("de")) {
                ConfigConstants.getInstance().setLanguage("de_DE");
            } else {
                ConfigConstants.getInstance().setLanguage("en_US");
            }
            rules.remove(ConfigConstants.HASPARENTRULESET);
        }

        List<String> viadeeRules = Arrays.asList(RuntimeConfig.getInstance().getViadeeRules());
        List<String> viadeeElementRules = Arrays.asList(RuntimeConfig.getInstance().getViadeeElementRules());

        // Determine for each rule if it is a model or element checker
        for (Map.Entry<String, Map<String, Rule>> checkerRules : rules.entrySet()) {
            if (viadeeRules.contains(checkerRules.getKey())) {
                if (viadeeElementRules.contains(checkerRules.getKey())) {
                    // Element checker
                    elementRules.put(checkerRules.getKey(), checkerRules.getValue());

                } else {
                    // Model checker
                    modelRules.put(checkerRules.getKey(), checkerRules.getValue());
                }

            } else {
                String className = "";
                Rule firstRule = (Rule) checkerRules.getValue().values().toArray()[0];
                if (firstRule.getSettings() != null
                        && firstRule.getSettings().containsKey(BpmnConstants.EXTERN_LOCATION)) {
                    className =
                            firstRule.getSettings().get(BpmnConstants.EXTERN_LOCATION).getValue() + "." //$NON-NLS-1$
                                    + firstRule.getName().trim();

                    // Load class and check implemented interface
                    try {
                        Class<?> clazz = Class.forName(className);
                        Class<?> superClazz = clazz.getSuperclass();
                        switch (superClazz.getName()) {
                            case "de.viadee.bpm.vPAV.processing.checker.AbstractElementChecker":
                                // Element checker
                                elementRules.put(checkerRules.getKey(), checkerRules.getValue());
                                break;
                            case "de.viadee.bpm.vPAV.processing.checker.AbstractModelChecker":
                                // Model checker
                                modelRules.put(checkerRules.getKey(), checkerRules.getValue());
                                break;
                            default:
                                // Class does not extend a know abstract checker
                                for (Rule rule : checkerRules.getValue().values()) {
                                    rule.deactivate();
                                }
                                LOGGER.warning("Class " + className
                                        + " does not extend a valid checker class. All rules of this type were deactivated.");
                                break;
                        }
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        return new RuleSet(elementRules, modelRules, hasParentRuleSet);
    }

    private static void checkSingletonRule(Map<String, Map<String, Rule>> rules, String rulename) {
        Map<String, Rule> rulesSubset = rules.get(rulename);
        if (rulesSubset != null && (rulesSubset.size() > 1 || rulesSubset.get(rulename) == null)) {
            LOGGER.severe("Rule '" + rulename + "' is only allowed once and without defining an ID.");
        }
    }

    private static boolean checkRegEx(String regEx) {
        boolean correct = false;

        if (regEx.isEmpty())
            return false;

        try {
            Pattern.compile(regEx);
            correct = true;
        } catch (PatternSyntaxException e) {
        }
        return correct;
    }
}
