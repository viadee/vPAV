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

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

@XmlRootElement(name = "setting")
@XmlType(propOrder = { "name", "script", "type", "id", "required", "value" })
public class XmlSetting {

    private String name;

    private String value;

    private String script;

    private String type;

    private String id;

    private boolean required;

    public XmlSetting() {
    }

    public XmlSetting(final String name, final String script, final String type, final String id,
            final boolean required, final String value) {
        super();
        this.required = required;
        this.name = name;
        this.value = value;
        this.type = type;
        this.script = script;
        this.id = id;
    }

    @XmlAttribute(name = "name", required = true)
    public String getName() {
        return name;
    }

    @XmlAttribute(name = "script", required = false)
    public String getScript() {
        return script;
    }

    @XmlAttribute(name = "type", required = true)
    public String getType() {
        return type;
    }

    @XmlAttribute(name = "id", required = false)
    public String getId() {
        return id;
    }

    @XmlAttribute(name = "required", required = false)
    public boolean getRequired() {
        return required;
    }

    @XmlValue
    public String getValue() {
        return value;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public void setValue(final String value) {
        this.value = value;
    }

    public void setScript(final String script) {
        this.script = script;
    }

    public void setType(final String type) {
        this.type = type;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public void setRequired(final boolean required) {
        this.required = required;
    }
}
