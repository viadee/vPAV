package de.viadee.bpm.vPAV.config.model;

import java.util.Map;

public class RuleSet {
    private Map<String, Map<String, Rule>> elementRules;
    private Map<String, Map<String, Rule>> modelRules;

    public RuleSet(Map<String, Map<String, Rule>> elementRules, Map<String, Map<String, Rule>> modelRules) {
        this.elementRules = elementRules;
        this.modelRules = modelRules;
    }

    public Map<String, Map<String, Rule>> getModelRules() {
        return  modelRules;
    }

    public Map<String, Map<String, Rule>> getElementRules() {
        return  elementRules;
    }
}
