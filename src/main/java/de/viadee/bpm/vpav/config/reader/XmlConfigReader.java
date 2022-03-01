/*
 * BSD 3-Clause License
 *
 * Copyright © 2022, viadee Unternehmensberatung AG
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
package de.viadee.bpm.vpav.config.reader;

import de.viadee.bpm.vpav.RuntimeConfig;
import de.viadee.bpm.vpav.config.model.*;
import de.viadee.bpm.vpav.constants.BpmnConstants;
import de.viadee.bpm.vpav.constants.ConfigConstants;
import de.viadee.bpm.vpav.processing.checker.AbstractElementChecker;
import de.viadee.bpm.vpav.processing.checker.AbstractModelChecker;

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
                return transformFromXmlDestructure(ruleSet);
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
    private static RuleSet transformFromXmlDestructure(final XmlRuleSet ruleSet)
            throws ConfigReaderException {
        final Map<String, Map<String, Rule>> rules = new HashMap<>();

        final Collection<XmlRule> xmlRules = ruleSet.getRules();
        for (final XmlRule rule : xmlRules) {
            Rule ruleObj = transformXmlRule(rule);

            if (!rules.containsKey(ruleObj.getName())) {
                rules.put(ruleObj.getName(), new HashMap<>());
            }
            rules.get(ruleObj.getName()).put(ruleObj.getId(),
                    ruleObj);
        }

        // Has-Parent-RuleSet rule is only allowed once. Check this.
        checkSingletonRule(rules);

        return splitRules(rules);
    }

    /**
     * Transforms a single XmlRule to Rule
     *
     * @return rule
     */
    private static Rule transformXmlRule(XmlRule rule) throws ConfigReaderException {
        final String id = (rule.getId() == null) ? rule.getName() : rule.getId();
        final String name = rule.getName();
        if (name == null)
            throw new ConfigReaderException("rule name is not set");

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
            xmlModelConventions.forEach(xmlModelConvention -> modelConventions.add(new ModelConvention(xmlModelConvention.getType())));
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

        return new Rule(id, name, rule.isState(), rule.getDescription(), settings, elementConventions, modelConventions);
    }

    /**
     * Splits given rules into model and element rules
     *
     * @param rules rules
     * @return RuleSet which includes the rules
     */
    private static RuleSet splitRules(Map<String, Map<String, Rule>> rules) {
        // Check if rule set has parent
        boolean hasParentRuleSet = false;
        if (rules.containsKey(ConfigConstants.HAS_PARENT_RULESET)) {
            hasParentRuleSet = rules.get(ConfigConstants.HAS_PARENT_RULESET).get(ConfigConstants.HAS_PARENT_RULESET)
                    .isActive();
            rules.remove(ConfigConstants.HAS_PARENT_RULESET);
        }

        HashMap<String, Map<String, Rule>> elementRules = new HashMap<>();
        HashMap<String, Map<String, Rule>> modelRules = new HashMap<>();
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
                // Resolve external checker to determine checker type
                resolveExternalChecker(checkerRules, elementRules, modelRules);
            }
        }

        return new RuleSet(elementRules, modelRules, hasParentRuleSet);
    }

    /**
     * Loads the external checker class to determine whether it is a model or element rule
     *
     * @param checkerRules rules of one checker type
     * @param elementRules HashMap where rules are inserted if it is a element checker
     * @param modelRules   HashMap where rules are inserted if it is a model checker
     */
    private static void resolveExternalChecker(Map.Entry<String, Map<String, Rule>> checkerRules,
                                               HashMap<String, Map<String, Rule>> elementRules,
                                               HashMap<String, Map<String, Rule>> modelRules) {
        String className;
        Rule firstRule = (Rule) checkerRules.getValue().values().toArray()[0];
        if (firstRule.getSettings() != null
                && firstRule.getSettings().containsKey(BpmnConstants.EXTERN_LOCATION)) {
            className =
                    firstRule.getSettings().get(BpmnConstants.EXTERN_LOCATION).getValue() + "." //$NON-NLS-1$
                            + firstRule.getName().trim();
            // Load class and check implemented interface
            try {
                Class<?> clazz = Class.forName(className);
                if (AbstractElementChecker.class.isAssignableFrom(clazz)) {
                    // Element checker
                    elementRules.put(checkerRules.getKey(), checkerRules.getValue());
                    return;
                }
                if (AbstractModelChecker.class.isAssignableFrom(clazz)) {
                    // Model checker
                    modelRules.put(checkerRules.getKey(), checkerRules.getValue());
                    return;
                } else {
                    LOGGER.warning("Class " + className
                            + " does not extend a valid checker class. All rules of this type were deactivated.");
                }
            } catch (ClassNotFoundException e) {
                LOGGER.warning("Class " + className + " was not found. All rules of this type were deactivated");
            }
            // Class does not extend a know abstract checker, deactivate rules
            for (Rule rule : checkerRules.getValue().values()) {
                rule.deactivate();
            }
        }
    }

    private static void checkSingletonRule(Map<String, Map<String, Rule>> rules) {
        Map<String, Rule> rulesSubset = rules.get(ConfigConstants.HAS_PARENT_RULESET);
        if (rulesSubset != null && (rulesSubset.size() > 1
                || rulesSubset.get(ConfigConstants.HAS_PARENT_RULESET) == null)) {
            LOGGER.severe("Rule '" + ConfigConstants.HAS_PARENT_RULESET
                    + "' is only allowed once and without defining an ID.");
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
            LOGGER.info("PatternSyntaxException was catched.");
        }
        return correct;
    }
}
