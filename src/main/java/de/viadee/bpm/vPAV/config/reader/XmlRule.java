/**
 * Copyright � 2017, viadee Unternehmensberatung GmbH
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
 * THIS SOFTWARE IS PROVIDED BY <COPYRIGHT HOLDER> ''AS IS'' AND ANY
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
package de.viadee.bpm.vPAV.config.reader;

import java.util.Collection;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * 
 * dient dazu...
 * 
 * Annahmen und Voraussetzungen sind...
 *
 */
@XmlRootElement(name = "rule")
@XmlType(propOrder = { "name", "state", "settings", "elementConventions", "modelConventions" })
public class XmlRule {

  private String name;

  private boolean state;

  private Collection<XmlSetting> settings;

  private Collection<XmlElementConvention> elementConventions;

  private Collection<XmlModelConvention> modelConventions;

  public XmlRule() {
  }

  public XmlRule(String name, boolean state, final Collection<XmlSetting> settings,
      final Collection<XmlElementConvention> elementConventions,
      final Collection<XmlModelConvention> modelConventions) {
    super();
    this.name = name;
    this.state = state;
    this.settings = settings;
    this.elementConventions = elementConventions;
    this.modelConventions = modelConventions;
  }

  @XmlElement(name = "name", required = true)
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @XmlElement(name = "state", required = true)
  public boolean isState() {
    return state;
  }

  public void setState(boolean state) {
    this.state = state;
  }

  @XmlElementWrapper(name = "settings")
  @XmlElement(name = "setting", required = false)
  public Collection<XmlSetting> getSettings() {
    return settings;
  }

  public void setSettings(Collection<XmlSetting> settings) {
    this.settings = settings;
  }

  @XmlElementWrapper(name = "elementConventions")
  @XmlElement(name = "elementConvention", required = false)
  public Collection<XmlElementConvention> getElementConventions() {
    return elementConventions;
  }

  public void setElementConventions(Collection<XmlElementConvention> elementConventions) {
    this.elementConventions = elementConventions;
  }

  @XmlElementWrapper(name = "modelConventions")
  @XmlElement(name = "modelConvention", required = false)
  public Collection<XmlModelConvention> getModelConventions() {
    return modelConventions;
  }

  public void setModelConventions(Collection<XmlModelConvention> modelConventions) {
    this.modelConventions = modelConventions;
  }
}
