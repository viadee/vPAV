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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.camunda.bpm.model.bpmn.instance.BaseElement;

import de.viadee.bpm.vPAV.BPMNScanner;
import de.viadee.bpm.vPAV.config.model.Rule;
import de.viadee.bpm.vPAV.config.model.Setting;
import de.viadee.bpm.vPAV.processing.CheckName;
import de.viadee.bpm.vPAV.processing.model.data.BpmnElement;
import de.viadee.bpm.vPAV.processing.model.data.CheckerIssue;
import de.viadee.bpm.vPAV.processing.model.data.CriticalityEnum;

public class ExtensionChecker extends AbstractElementChecker {

    public ExtensionChecker(Rule rule, BPMNScanner bpmnScanner) {
        super(rule, bpmnScanner);
    }

    @Override
    public Collection<CheckerIssue> check(BpmnElement element) {
        final Collection<CheckerIssue> issues = new ArrayList<CheckerIssue>();
        final ArrayList<Setting> set = new ArrayList<Setting>();
        final BaseElement bpmnElement = element.getBaseElement();
        final Map<String, String> keyPairs = new HashMap<String, String>();

        final Map<String, Setting> settings = rule.getSettings();

        // Retrieve extension key pair from bpmn model
        keyPairs.putAll(bpmnScanner.getKeyPairs(bpmnElement.getId()));

        for (Map.Entry<String, Setting> settingsEntry : settings.entrySet()) {
            set.add(settingsEntry.getValue());
        }

        // Iterate over each setting specified in the ruleSet
        for (Setting setting : set) {

            // If specified task type and element type match -> proceed
            if (setting.getType() != null
                    && setting.getType().equals(bpmnElement.getElementType().getInstanceType().getSimpleName())) {

                if (!keyPairs.isEmpty()) {

                    // Iterate over key pairs
                    for (Map.Entry<String, String> entry : keyPairs.entrySet()) {

                        final String key = entry.getKey();
                        final String value = entry.getValue();

                        if (key != null && !key.isEmpty()) {

                            // If name specified in ruleSet does not match with name specified in model
                            if (!containsKey(set, key)) {
                                issues.add(new CheckerIssue(rule.getName(), CriticalityEnum.ERROR,
                                        element.getProcessdefinition(), null,
                                        bpmnElement.getAttributeValue("id"),
                                        bpmnElement.getAttributeValue("name"), null, null, null,
                                        "Key of '" + CheckName.checkName(bpmnElement)
                                                + "' could not be resolved. Check whether ruleset and model are congruent."));
                                continue;
                            } else {

                                if (value != null && !value.isEmpty()) {
                                    final String patternString = settings.get(key).getValue();
                                    final Pattern pattern = Pattern.compile(patternString);
                                    Matcher matcher = pattern.matcher(value);

                                    // if predefined value of a key-value pair does not fit a given regex
                                    // (e.g. digits for timeout)
                                    if (!matcher.matches()) {
                                        issues.add(new CheckerIssue(rule.getName(), CriticalityEnum.ERROR,
                                                element.getProcessdefinition(), null,
                                                bpmnElement.getAttributeValue("id"),
                                                bpmnElement.getAttributeValue("name"), null, null, null,
                                                "Key-Value pair of '" + CheckName.checkName(bpmnElement)
                                                        + "' does not fit the configured setting of the rule set. Check the extension with key '"
                                                        + setting.getName() + "'."));
                                        keyPairs.remove(key);
                                        break;
                                    }
                                } else {
                                    // If value is empty
                                    issues.add(new CheckerIssue(rule.getName(), CriticalityEnum.ERROR,
                                            element.getProcessdefinition(), null,
                                            bpmnElement.getAttributeValue("id"),
                                            bpmnElement.getAttributeValue("name"), null, null, null,
                                            "Value of '" + CheckName.checkName(bpmnElement)
                                                    + "' is empty. Check whether ruleset and model are congruent."));
                                    keyPairs.remove(key);
                                    break;
                                }

                            }
                            // No key specified in the model
                        } else {
                            issues.add(new CheckerIssue(rule.getName(), CriticalityEnum.ERROR,
                                    element.getProcessdefinition(), null, bpmnElement.getAttributeValue("id"),
                                    bpmnElement.getAttributeValue("name"), null, null, null,
                                    "The model does not consist of a key for '"
                                            + CheckName.checkName(bpmnElement)
                                            + "'. Please specify a key matching the ruleset."));
                            keyPairs.remove(key);
                            break;
                        }
                    }
                    // No key and no value specified in the model
                } else {
                    issues.add(new CheckerIssue(rule.getName(), CriticalityEnum.ERROR,
                            element.getProcessdefinition(), null, bpmnElement.getAttributeValue("id"),
                            bpmnElement.getAttributeValue("name"), null, null, null,
                            "The ruleset specifies the use of an extension for '"
                                    + CheckName.checkName(bpmnElement)
                                    + "'."));
                }
            }
            continue;
        }
        return issues;
    }

    /**
     * Checks if the list of settings contains a key and returns boolean value
     *
     * @param settings
     *            List of all settings
     * @param key
     *            Key extracted from bpmn model
     * @return True/false dependend on whether element is contained in list
     */
    public static boolean containsKey(ArrayList<Setting> settings, String key) {
        for (Setting setting : settings) {
            if (setting.getName().equals(key)) {
                return true;
            }
        }
        return false;
    }

}
