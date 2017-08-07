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
package de.viadee.bpm.vPAV.beans;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * parses bean mapping file for recognition in the analysis
 *
 */
public class BeanMappingXmlParser {

    public static Logger logger = Logger.getLogger(BeanMappingXmlParser.class.getName());

    public static Map<String, String> parse(final File beanMappingFile) {

        final Map<String, String> beanNamesCorrespondingClasses = new HashMap<String, String>();

        try {
            final Document xmlDoc = readXmlDocumentFile(beanMappingFile);
            beanNamesCorrespondingClasses.putAll(readBeanNamesAndCorrespondingClasses(xmlDoc));
        } catch (final ParserConfigurationException | SAXException | IOException ex) {
            logger.warning("bean mapping couldn't be loaded from beanMapping.xml");
        }

        return beanNamesCorrespondingClasses;
    }

    /**
     * Read Xml document
     * 
     * @param beanMappingFile
     * @return
     * @throws ParserConfigurationException
     * @throws IOException
     * @throws SAXException
     */
    private static Document readXmlDocumentFile(final File beanMappingFile)
            throws ParserConfigurationException, IOException, SAXException {
        final DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        final DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        final Document xmlDoc = dBuilder.parse(beanMappingFile);
        return xmlDoc;
    }

    /**
     * get bean names with corresponding classes from xml document
     * 
     * @param xmlDoc
     * @return
     */
    private static Map<String, String> readBeanNamesAndCorrespondingClasses(final Document xmlDoc) {
        final Map<String, String> beanNamesCorrespondingClasses = new HashMap<String, String>();

        final NodeList nodeList = xmlDoc.getElementsByTagName("bean");
        for (int i = 0; i < nodeList.getLength(); i++) {
            final Node xmlNode = nodeList.item(i);
            if (xmlNode.getNodeType() == Node.ELEMENT_NODE) {
                final Element xmlElement = (Element) xmlNode;
                beanNamesCorrespondingClasses.put(xmlElement.getAttribute("name"),
                        xmlElement.getAttribute("value"));
            }
        }

        return beanNamesCorrespondingClasses;
    }
}
