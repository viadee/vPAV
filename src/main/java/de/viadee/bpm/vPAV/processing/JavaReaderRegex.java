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
package de.viadee.bpm.vPAV.processing;

import com.google.common.collect.ListMultimap;
import de.viadee.bpm.vPAV.FileScanner;
import de.viadee.bpm.vPAV.processing.code.flow.FlowGraph;
import de.viadee.bpm.vPAV.processing.model.data.BpmnElement;
import de.viadee.bpm.vPAV.processing.model.data.ElementChapter;
import de.viadee.bpm.vPAV.processing.model.data.KnownElementFieldType;
import de.viadee.bpm.vPAV.processing.model.data.ProcessVariableOperation;

import java.util.logging.Logger;

public class JavaReaderRegex implements JavaReader {

	public static final Logger LOGGER = Logger.getLogger(JavaReaderRegex.class.getName());

	/**
	 * Checks a java delegate for process variable references (read/write/delete).
	 * Contraints: This method only examines variables in a java delegate that can
	 * be resolved prior runtime (static code analysis).
	 * 
	 *
	 * @param fileScanner
	 *            FileScanner
	 * @param classFile
	 *            JavaDelegate class name
	 * @param element
	 *            BpmnElement
	 * @param chapter
	 *            ElementChapter
	 * @param fieldType
	 *            KnownElementFieldType
	 * @param scopeId
	 *            ScopeId
	 * 
	 * @return Process Variables
	 */
	@Override
	public ListMultimap<String, ProcessVariableOperation> getVariablesFromJavaDelegate(final FileScanner fileScanner,
			final String classFile, final BpmnElement element, final ElementChapter chapter,
			final KnownElementFieldType fieldType, final String scopeId, final FlowGraph flowGraph) {
		// convert package format in a concrete path to the java class (.java)
		String filePath = "";
		if (classFile != null && classFile.trim().length() > 0) {
			filePath = classFile.replaceAll("\\.", "/") + ".java";
		}
		final ListMultimap<String, ProcessVariableOperation> variables = ResourceFileReader.readResourceFile(filePath,
				element, chapter, fieldType, scopeId);
		return variables;
	}

	/**
	 * Unused implementation
	 */
	@Override
	public ListMultimap<String, ProcessVariableOperation> getVariablesFromClass(final String className,
			final ProcessVariablesScanner scanner, final BpmnElement element, final String resourceFilePath,
			final EntryPoint entry) {
		// TODO Auto-generated method stub
		return null;
	}
}
