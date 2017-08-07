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
package de.viadee.bpm.vPAV.processing.model.data;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import de.viadee.bpm.vPAV.processing.model.graph.Path;

/**
 * Class for holding issues (errors, warnings, infos) from the checkers
 * 
 */
public class CheckerIssue {

  private String ruleName;

  private CriticalityEnum classification;

  private String bpmnFile;

  private String resourceFile;

  private String elementId;

  private String elementName;

  private String variable;

  private Anomaly anomaly;

  private List<Path> invalidPaths;

  private String message;

  public CheckerIssue() {
  }

  public CheckerIssue(final String ruleName, final CriticalityEnum classification,
      final String bpmnFile, final String resourceFile, final String elementId,
      final String elementName, final String variable, final Anomaly anomaly,
      final List<Path> invalidPaths, final String message) {
    super();
    this.ruleName = ruleName;
    this.variable = variable;
    this.anomaly = anomaly;
    this.invalidPaths = invalidPaths;
    this.classification = classification;
    this.bpmnFile = bpmnFile;
    this.resourceFile = resourceFile;
    this.elementId = elementId;
    this.elementName = elementName;
    this.message = message;
  }

  public String getId() {
    return getMD5(
        ruleName + "_" + bpmnFile + "_" + resourceFile + "_" + elementId + "_" + variable);
  }

  public String getRuleName() {
    return ruleName;
  }

  public String getVariable() {
    return variable;
  }

  public Anomaly getAnomaly() {
    return anomaly;
  }

  public List<Path> getInvalidPaths() {
    return invalidPaths;
  }

  public CriticalityEnum getClassification() {
    return classification;
  }

  public String getBpmnFile() {
    return bpmnFile;
  }

  public String getResourceFile() {
    return resourceFile;
  }

  public String getElementId() {
    return elementId;
  }

  public String getElementName() {
    return elementName;
  }

  public String getMessage() {
    return message;
  }

  public void setClassification(final CriticalityEnum classification) {
    this.classification = classification;
  }

  public void setBpmnFile(final String bpmnFile) {
    this.bpmnFile = bpmnFile;
  }

  public void setElementId(final String elementId) {
    this.elementId = elementId;
  }

  public void setElementName(final String elementName) {
    this.elementName = elementName;
  }

  public void setMessage(final String message) {
    this.message = message;
  }

  public static String getMD5(String input) {
    try {
      MessageDigest messageDigest = MessageDigest.getInstance("MD5");
      byte[] messageDigestByteArray = messageDigest.digest(input.getBytes());
      BigInteger number = new BigInteger(1, messageDigestByteArray);
      String hashtext = number.toString(16);
      // Now we need to zero pad it if you actually want the full 32 chars.
      while (hashtext.length() < 32) {
        hashtext = "0" + hashtext;
      }
      return hashtext;
    } catch (final NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }
}
