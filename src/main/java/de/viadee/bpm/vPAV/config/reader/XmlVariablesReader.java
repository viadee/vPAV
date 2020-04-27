/*
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
package de.viadee.bpm.vPAV.config.reader;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import de.viadee.bpm.vPAV.RuntimeConfig;
import de.viadee.bpm.vPAV.processing.model.data.ProcessVariableOperation;
import de.viadee.bpm.vPAV.processing.model.data.VariableOperation;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Used to read the variables file (variables.xml) and extract the user defined variables
 * Requirements: Existing variables.xml in src/test/resources
 */
public final class XmlVariablesReader {

    private static final Logger LOGGER = Logger.getLogger(XmlVariablesReader.class.getName());

    /**
     * Reads the variable file and transforms variables into another representation
     *
     * @param file           Location of variables.xml file relative to project resources
     * @param defaultProcess process variables are mapped to if no process is defined in file
     * @return Map of manually defined variables
     * @throws JAXBException If file can not be found
     */
    public HashMap<String, ListMultimap<String, ProcessVariableOperation>> read(final String file,
            final String defaultProcess) throws JAXBException {

        final JAXBContext jaxbContext = JAXBContext.newInstance(XmlVariables.class);
        final Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();

        InputStream fVariables = RuntimeConfig.getInstance().getClassLoader().getResourceAsStream(file);

        if (fVariables != null) {
            final XmlVariables xmlVariables = (XmlVariables) jaxbUnmarshaller.unmarshal(fVariables);
            return transformFromXmlDestructure(xmlVariables, defaultProcess);
        } else {
            LOGGER.log(Level.INFO, "No variables.xml file with user defined variables was found.");
            return new HashMap<>();
        }
    }

    /**
     * Transforms XmlVariables to ProcessVariableOperations
     *
     * @param xmlVariables   the Xml Representation of the variables file
     * @param defaultProcess that is assigned to variables without a user defined process
     * @return HashMap of all variables grouped by creation points of variables
     */
    private static HashMap<String, ListMultimap<String, ProcessVariableOperation>> transformFromXmlDestructure(
            final XmlVariables xmlVariables, final String defaultProcess) {
        final Collection<XmlVariable> variableCollection = xmlVariables.getVariables();
        HashMap<String, ListMultimap<String, ProcessVariableOperation>> operations = new HashMap<>();

        for (final XmlVariable variable : variableCollection) {
            ProcessVariableOperation operation;
            try {
                operation = createOperationFromXml(variable, defaultProcess);

                if (variable.getCreationPoint() == null) {
                    variable.setCreationPoint("StartEvent");
                }
                if (!operations.containsKey(variable.getCreationPoint())) {
                    operations.put(variable.getCreationPoint(), ArrayListMultimap.create());
                }
                operations.get(variable.getCreationPoint()).put(operation.getName(), operation);
            } catch (VariablesReaderException e) {
                LOGGER.warning("Variable in variables.xml is missing name. It will be ignored.");
            }
        }

        return operations;
    }

    /**
     * Transforms a single Xml Variable to a ProcessVariableOperation
     *
     * @return ProcessVariableOperation
     * @throws VariablesReaderException if variable's name is not set
     */
    private static ProcessVariableOperation createOperationFromXml(XmlVariable variable, String defaultProcess)
            throws VariablesReaderException {
        final String name = variable.getName();
        String process = variable.getProcess();
        String scope = variable.getScope();
        String operation = variable.getOperation();

        if (name == null) {
            throw new VariablesReaderException("Name is not set.");
        }

        if (process == null) {
            process = defaultProcess;
        }

        if (scope == null) {
            scope = process;
        }

        if (operation == null) {
            operation = "write";
        }

        VariableOperation op;

        switch (operation) {
            case "read":
                op = VariableOperation.READ;
                break;
            case "delete":
                op = VariableOperation.DELETE;
                break;
            default:
                op = VariableOperation.WRITE;
        }

        return new ProcessVariableOperation(name, op, scope);

    }
}
