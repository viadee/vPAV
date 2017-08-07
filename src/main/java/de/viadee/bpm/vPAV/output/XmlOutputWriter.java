/**
 * Copyright � 2017, viadee Unternehmensberatung GmbH All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met: 1. Redistributions of source code must retain the above copyright notice, this list of
 * conditions and the following disclaimer. 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation and/or other materials provided with the
 * distribution. 3. All advertising materials mentioning features or use of this software must display the following
 * acknowledgement: This product includes software developed by the viadee Unternehmensberatung GmbH. 4. Neither the
 * name of the viadee Unternehmensberatung GmbH nor the names of its contributors may be used to endorse or promote
 * products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY <COPYRIGHT HOLDER> ''AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO
 * EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package de.viadee.bpm.vPAV.output;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import de.viadee.bpm.vPAV.ConstantsConfig;
import de.viadee.bpm.vPAV.processing.model.data.BpmnElement;
import de.viadee.bpm.vPAV.processing.model.data.CheckerIssue;
import de.viadee.bpm.vPAV.processing.model.graph.Path;

/**
 * Ergebnisse aus dem Checker in ein definiertes XML-Format schreiben
 * 
 */
public class XmlOutputWriter implements IssueOutputWriter {

    public void write(final Collection<CheckerIssue> issues) throws OutputWriterException {

        Writer writer = null;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(ConstantsConfig.VALIDATION_XML_OUTPUT), "utf-8"));
            final JAXBContext context = JAXBContext.newInstance(XmlCheckerIssues.class);
            final Marshaller m = context.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            m.marshal(transformToXmlDatastructure(issues), writer);
        } catch (final UnsupportedEncodingException e) {
            throw new OutputWriterException("unsupported encoding");
        } catch (final FileNotFoundException e) {
            throw new OutputWriterException("output file couldn't be generated");
        } catch (final JAXBException e) {
            throw new OutputWriterException("xml output couldn't be generated (jaxb-error)");
        } finally {
            try {
                writer.close();
            } catch (Exception ex) {
                /* ignore */}
        }
    }

    private static XmlCheckerIssues transformToXmlDatastructure(
            final Collection<CheckerIssue> issues) {
        XmlCheckerIssues xmlIssues = new XmlCheckerIssues();
        for (final CheckerIssue issue : issues) {
            final List<XmlPath> xmlPaths = new ArrayList<XmlPath>();
            final List<Path> invalidPaths = issue.getInvalidPaths();
            if (invalidPaths != null) {
                for (final Path path : invalidPaths) {
                    List<BpmnElement> elements = path.getElements();
                    List<XmlPathElement> pathElements = new ArrayList<XmlPathElement>();
                    for (final BpmnElement element : elements) {
                        String elementName = element.getBaseElement().getAttributeValue("name");
                        if (elementName != null) {
                            // filter newlines
                            elementName = elementName.replace("\n", "");
                        }
                        pathElements.add(new XmlPathElement(element.getBaseElement().getId(), elementName));
                    }
                    xmlPaths.add(new XmlPath(pathElements));
                }
            }
            final String elementName = issue.getElementName();
            xmlIssues.addIssue(new XmlCheckerIssue(issue.getId(), issue.getRuleName(),
                    issue.getClassification().name(), issue.getBpmnFile(), issue.getResourceFile(),
                    issue.getElementId(), elementName == null ? null : elementName.replace("\n", ""),
                    issue.getMessage(), issue.getVariable(),
                    issue.getAnomaly() == null ? null : issue.getAnomaly().getDescription(),
                    xmlPaths.isEmpty() ? null : xmlPaths));
        }
        return xmlIssues;
    }
}
