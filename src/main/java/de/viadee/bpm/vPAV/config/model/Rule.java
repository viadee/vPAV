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
package de.viadee.bpm.vPAV.config.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

public class Rule {

    private String name;

    private boolean isActive;

    private String ruleDescription;

    private Map<String, Setting> settings;

    private Collection<ElementConvention> elementConventions;

    private ArrayList<ModelConvention> modelConventions;

    public Rule(final String name, final boolean isActive, final String ruleDescription,
            final Map<String, Setting> settings,
            final Collection<ElementConvention> elementConventions,
            final ArrayList<ModelConvention> modelConventions) {
        super();
        this.name = name;
        this.isActive = isActive;
        this.ruleDescription = ruleDescription;
        this.settings = settings;
        this.elementConventions = elementConventions;
        this.modelConventions = modelConventions;
    }

    public String getName() {
        return name;
    }

    public boolean isActive() {
        return isActive;
    }

    public String getRuleDescription() {
        return ruleDescription;
    }

    public Map<String, Setting> getSettings() {
        return settings;
    }

    public Collection<ElementConvention> getElementConventions() {
        return elementConventions;
    }

    public ArrayList<ModelConvention> getModelConventions() {
        return modelConventions;
    }

    public ArrayList<String> getWhiteList() {
        final ArrayList<String> whiteList = new ArrayList<String>();
        for (ModelConvention modelConvention : modelConventions) {
            if (modelConvention.getType() != null) {
                whiteList.add(modelConvention.getType());
            }
        }
        return whiteList;
    }

    public void deactivate() {
        isActive = false;
    }
}
