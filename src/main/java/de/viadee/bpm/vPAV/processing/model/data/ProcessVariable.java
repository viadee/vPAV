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

/**
 * Represents a process variable with some meaningful information.
 *
 */
public class ProcessVariable {

  private String name;

  private VariableOperation operation;

  private String scopeId;

  /** Detailed Information about the location of the match **/
  private BpmnElement element;

  private String resourceFilePath;

  private ElementChapter chapter;

  private KnownElementFieldType fieldType;

  public ProcessVariable(final String name, final BpmnElement element, final ElementChapter chapter,
      final KnownElementFieldType fieldType, final String resourceFilePath,
      final VariableOperation operation, final String scopeId) {
    super();
    this.name = name;
    this.element = element;
    this.resourceFilePath = resourceFilePath;
    this.chapter = chapter;
    this.fieldType = fieldType;
    this.operation = operation;
    this.scopeId = scopeId;
  }

  public String getName() {
    return name;
  }

  public String getResourceFilePath() {
    return resourceFilePath;
  }

  public BpmnElement getElement() {
    return element;
  }

  public ElementChapter getChapter() {
    return chapter;
  }

  public KnownElementFieldType getFieldType() {
    return fieldType;
  }

  public VariableOperation getOperation() {
    return operation;
  }

  public String getScopeId() {
    return scopeId;
  }

  public String toString() {
    return name + " [" + element.getProcessdefinition() + ", " + element.getBaseElement().getId()
        + ", Scope: " + scopeId + ", " + chapter.name() + ", " + fieldType.getDescription() + ", "
        + resourceFilePath + "]";
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }

  @Override
  public boolean equals(final Object o) {
    if (o instanceof ProcessVariable) {
      final ProcessVariable p = (ProcessVariable) o;
      if (name.equals(p.getName())) {
        return true;
      }
    }
    return false;
  }
}
