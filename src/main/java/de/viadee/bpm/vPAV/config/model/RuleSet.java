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
package de.viadee.bpm.vPAV.config.model;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class RuleSet {
    private Map<String, Map<String, Rule>> elementRules;
    private Map<String, Map<String, Rule>> modelRules;
    private boolean hasParentRuleSet = false;
    private String language = null;
    private Rule createOutputHtml;

    public RuleSet() {
        this.elementRules = new HashMap<>();
        this.modelRules = new HashMap<>();
    }

    public RuleSet(Map<String, Map<String, Rule>> elementRules, Map<String, Map<String, Rule>> modelRules) {
        this.elementRules = elementRules;
        this.modelRules = modelRules;
    }

    public Map<String, Map<String, Rule>> getModelRules() {
        return modelRules;
    }

    public Map<String, Map<String, Rule>> getElementRules() {
        return elementRules;
    }

    public Map<String, Map<String, Rule>> getAllActiveRules() {
        Map<String, Map<String, Rule>> allRules = new HashMap<>();
        allRules.putAll(elementRules);
        allRules.putAll(modelRules);

        return allRules.entrySet().stream().filter(
                e -> e.getValue().entrySet().stream().allMatch(
                        r -> r.getValue().isActive()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public boolean hasParentRuleSet() {
        return hasParentRuleSet;
    }

    public String getLanguage() {
        /*
        final Rule rule = rules.get("language").get("language");
            final Map<String, Setting> settings = rule.getSettings();
            if (settings.get("locale").getValue().equals("de")) {
                getResource("de_DE");
            } else if (settings.get("locale").getValue().equals("en")) {
                getResource("en_US");
            }
         */
        return language;
    }

    public Rule getCreateOutputHtml() {
        return createOutputHtml;
    }
}
