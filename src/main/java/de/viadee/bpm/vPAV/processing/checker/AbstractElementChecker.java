package de.viadee.bpm.vPAV.processing.checker;

import de.viadee.bpm.vPAV.config.model.Rule;

public abstract class AbstractElementChecker implements ElementChecker {

    protected final Rule rule;

    public AbstractElementChecker(final Rule rule) {
        this.rule = rule;
    }

}
