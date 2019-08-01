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
package de.viadee.bpm.vPAV.processing.model.data;

import de.viadee.bpm.vPAV.processing.code.flow.BpmnElement;

/**
 * Represents a process variable operation with some meaningful information.
 *
 */
public class ProcessVariableOperation {

	private String id;

	private String name;

	private VariableOperation operation;

	private String scopeId;

	/** Detailed Information about the location of the match **/
	private BpmnElement element;

	private String resourceFilePath;

	private ElementChapter chapter;

	private KnownElementFieldType fieldType;

	private int index;
	// Guaranteed that the operation takes place or not
	private boolean operationType;

	public ProcessVariableOperation(final String name, final BpmnElement element, final ElementChapter chapter,
			final KnownElementFieldType fieldType, final String resourceFilePath, final VariableOperation operation,
			final String scopeId, final int index) {
		super();
		this.name = name;
		this.element = element;
		this.resourceFilePath = resourceFilePath;
		this.chapter = chapter;
		this.fieldType = fieldType;
		this.operation = operation;
		this.scopeId = scopeId;
		this.index = index;
		this.id = createId();
		element.getFlowAnalysis().incrementOperationCounter();
	}

	private String createId() {
		return CheckerIssue.getMD5(name + "_" + chapter + "_" + fieldType + "_" + resourceFilePath + "_" + operation
				+ "_" + scopeId + "_" + System.nanoTime());
	}

	public String getName() {
		return name;
	}

	public String getId() {
		return id;
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

	public void setScopeId(String scopeId) {
		this.scopeId = scopeId;
	}

	public void setOperationType(boolean type) {
		this.operationType = type;
	}

	public boolean getOperationType() {
		return operationType;
	}

	public int getIndex() {
		return index;
	}

	public String toString() {
		return name + " [" + element.getProcessDefinition() + ", " + element.getBaseElement().getId() + ", Scope: "
				+ scopeId + ", " + chapter.name() + ", " + fieldType.getDescription() + ", " + resourceFilePath + "]";
	}

	@Override
	public boolean equals(final Object o) {
		if (o instanceof ProcessVariableOperation) {
			final ProcessVariableOperation p = (ProcessVariableOperation) o;
			return name.equals(p.getName());
		}
		return false;
	}
}
