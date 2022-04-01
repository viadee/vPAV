/*
 * BSD 3-Clause License
 *
 * Copyright © 2022, viadee Unternehmensberatung AG
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
package de.viadee.bpm.vpav.processing;

import java.util.HashSet;
import java.util.Set;

public class EntryPoint {

	private String className;

	private String methodName;

	private String messageName;

	private String entryPointName;

	private String processDefinitionKey;

	private Set<String> processVariables;

	public EntryPoint(final String className, final String methodName, final String messageName,
			final String entryPointName, final String processDefinitionKey) {
		this.className = className;
		this.methodName = methodName;
		this.messageName = messageName;
		this.entryPointName = entryPointName;
		this.processDefinitionKey = processDefinitionKey;
		this.processVariables = new HashSet<>();
	}

	public EntryPoint(final String className, final String methodName, final String messageName,
			final String entryPointName, final String processDefinitionKey, final Set<String> processVariables) {
		this.className = className;
		this.methodName = methodName;
		this.messageName = messageName;
		this.entryPointName = entryPointName;
		this.processDefinitionKey = processDefinitionKey;
		this.processVariables = processVariables;
	}

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public String getMethodName() {
		return methodName;
	}

	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}

	public String getMessageName() {
		return messageName;
	}

	public void setMessageName(String messageName) {
		this.messageName = messageName;
	}

	public String getEntryPointName() {
		return entryPointName;
	}

	public void setEntryPointName(String entryPointName) {
		this.entryPointName = entryPointName;
	}

	public Set<String> getProcessVariables() {
		return processVariables;
	}

	public void setProcessVariables(Set<String> processVariables) {
		this.processVariables = processVariables;
	}

	public String getProcessDefinitionKey() {
		return processDefinitionKey;
	}

	public void setProcessDefinitionKey(String processDefinitionKey) {
		this.processDefinitionKey = processDefinitionKey;
	}
}
