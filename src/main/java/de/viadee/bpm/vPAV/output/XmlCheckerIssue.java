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
package de.viadee.bpm.vPAV.output;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "issue")
@XmlType(propOrder = { "id", "ruleName", "bpmnFile", "resourceFile", "classification", "elementId",
    "elementName", "variable", "anomaly", "paths", "message" })
public class XmlCheckerIssue {

  private String id;

  private String ruleName;

  private String bpmnFile;

  private String resourceFile;

  private String variable;

  private String anomaly;

  private List<XmlPath> paths;

  private String classification;

  private String elementId;

  private String elementName;

  private String message;

  public XmlCheckerIssue() {
  }

  public XmlCheckerIssue(final String id, final String ruleName, final String classification,
      final String bpmnFile, final String resourceFile, final String elementId,
      final String elementName, final String message, final String variable, final String anomaly,
      final List<XmlPath> invalidPaths) {
    super();
    this.id = id;
    this.ruleName = ruleName;
    this.classification = classification;
    this.bpmnFile = bpmnFile;
    this.resourceFile = resourceFile;
    this.elementId = elementId;
    this.elementName = elementName;
    this.message = message;
    this.variable = variable;
    this.anomaly = anomaly;
    this.paths = invalidPaths;
  }

  @XmlElement(name = "id", required = true)
  public String getId() {
    return id;
  }

  @XmlElement(name = "ruleName", required = true)
  public String getRuleName() {
    return ruleName;
  }

  @XmlElement(name = "resourceFile", required = false)
  public String getResourceFile() {
    return resourceFile;
  }

  @XmlElement(name = "variable", required = false)
  public String getVariable() {
    return variable;
  }

  @XmlElement(name = "anomaly", required = false)
  public String getAnomaly() {
    return anomaly;
  }

  @XmlElementWrapper(name = "paths")
  @XmlElement(name = "path", required = false)
  public List<XmlPath> getPaths() {
    return paths;
  }

  @XmlElement(name = "classification", required = true)
  public String getClassification() {
    return classification;
  }

  @XmlElement(name = "bpmnFile", required = true)
  public String getBpmnFile() {
    return bpmnFile;
  }

  @XmlElement(name = "elementId", required = true)
  public String getElementId() {
    return elementId;
  }

  @XmlElement(name = "elementName", required = false)
  public String getElementName() {
    return elementName;
  }

  @XmlElement(name = "message", required = true)
  public String getMessage() {
    return message;
  }

  public void setClassification(String classification) {
    this.classification = classification;
  }

  public void setBpmnFile(String bpmnFile) {
    this.bpmnFile = bpmnFile;
  }

  public void setElementId(String elementId) {
    this.elementId = elementId;
  }

  public void setElementName(String elementName) {
    this.elementName = elementName;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public void setId(String id) {
    this.id = id;
  }

  public void setRuleName(String ruleName) {
    this.ruleName = ruleName;
  }

  public void setResourceFile(String resourceFile) {
    this.resourceFile = resourceFile;
  }

  public void setVariable(String variable) {
    this.variable = variable;
  }

  public void setAnomaly(String anomaly) {
    this.anomaly = anomaly;
  }

  public void setPaths(List<XmlPath> paths) {
    this.paths = paths;
  }
}
