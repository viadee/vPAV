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
package de.viadee.bpm.vPAV.processing;

import org.camunda.bpm.model.bpmn.instance.BaseElement;
import org.w3c.dom.Element;

import de.viadee.bpm.vPAV.constants.BpmnConstants;

/**
 *
 * Utility class to check names and return names/ids
 *
 */
public class CheckName {

    /**
     * Checks the name of a BaseElement and returns the identifier if no name is specified
     *
     * @param baseElement
     *            Holds the BaseElement of a given BPMN element
     * @return identifier
     */
    public static String checkName(final BaseElement baseElement) {

        String identifier = baseElement.getAttributeValue(BpmnConstants.ATTR_NAME);

        if (identifier == null || identifier == "") {
            identifier = baseElement.getAttributeValue(BpmnConstants.ATTR_ID);
        }

        return identifier;
    }

    /**
     * Checks the name of a Timer and returns the identifier if no name is specified
     *
     * @param element
     *            Holds the element of a given timerEvent
     * @return identifier
     */
    public static String checkTimer(final Element element) {

        String identifier = element.getAttribute(BpmnConstants.ATTR_NAME);

        if (identifier == null || identifier == "") {
            identifier = element.getAttribute(BpmnConstants.ATTR_ID);
        }

        return identifier;
    }

}
