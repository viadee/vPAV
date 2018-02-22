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

public class Setting {

    private String name;

    private String value;

    private ArrayList<String> scriptPlaces = new ArrayList<String>();

    private String type;

    private String id;

    private boolean required;

    /**
     *
     * @param name
     *            Name of the setting
     * @param scriptPlace
     *            Allowed places for scripts
     * @param type
     *            Type of task
     * @param value
     *            Value of setting
     * @param id
     *            Id of certain task
     * @param required
     *            Boolean to specify the explicit need for a certain extension key
     */
    public Setting(final String name, final String scriptPlace, final String type, final String id,
            final boolean required, final String value) {
        super();
        this.name = name;
        this.value = value;
        this.type = type;
        this.id = id;
        this.required = required;
        if (scriptPlace != null)
            scriptPlaces.add(scriptPlace);
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public String getType() {
        return type;
    }

    public String getId() {
        return id;
    }

    public boolean getRequired() {
        return required;
    }

    public ArrayList<String> getScriptPlaces() {
        return scriptPlaces;
    }

    public void addScriptPlace(String place) {
        scriptPlaces.add(place);
    }

    public void setScriptPlace(ArrayList<String> scriptPlaces) {
        this.scriptPlaces = scriptPlaces;
    }
}
