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
package de.viadee.bpm.vPAV.output;

import de.viadee.bpm.vPAV.config.model.*;
import de.viadee.bpm.vPAV.config.reader.*;
import de.viadee.bpm.vPAV.constants.ConfigConstants;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 * Ergebnisse aus dem Checker in ein definiertes XML-Format schreiben
 *
 */
public class RuleSetOutputWriter {

    /**
     * Writes the effective ruleSet to the vPAV output folder to provide traceability
     *
     * @param rules
     *            Contains the actual configuration of rules
     * @throws OutputWriterException
     *             Occurs if output can not be written
     */
    public void write(RuleSet rules) throws OutputWriterException {
        Writer writer = null;

        Path path = Paths.get(ConfigConstants.EFFECTIVE_RULESET);
        if (path.toFile().exists())
            path.toFile().delete();

        try {
            writer = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(ConfigConstants.EFFECTIVE_RULESET), "utf-8"));
            final JAXBContext context = JAXBContext.newInstance(XmlRuleSet.class);
            final Marshaller m = context.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            m.marshal(transformToXmlDatastructure(rules), writer);
        } catch (final UnsupportedEncodingException e) {
            throw new OutputWriterException("unsupported encoding");
        } catch (final FileNotFoundException e) {
            throw new OutputWriterException("Effective config file couldn't be generated");
        } catch (final JAXBException e) {
            throw new OutputWriterException("xml output (effective config file) couldn't be generated (jaxb-error)");
        } finally {
            try {
                writer.close();
            } catch (Exception ex) {
                /* ignore */
            }
        }
    }

    /**
     * Reads the final ruleSet and recreates a XML file
     *
     * @param rules
     * @return xmlRuleSet
     */
    private static XmlRuleSet transformToXmlDatastructure(final RuleSet rules) {
        XmlRuleSet xmlRuleSet = new XmlRuleSet();
        Collection<XmlRule> allRules = transformRuleMapToXml(rules.getAllRules());
        xmlRuleSet.setRules(allRules);
        return xmlRuleSet;
    }

    private static Collection<XmlRule> transformRuleMapToXml(Map<String, Map<String, Rule>> ruleMap) {
        Collection<XmlRule> xmlRuleCollection = new ArrayList<>();
        for (Map.Entry<String, Map<String, Rule>> entry : ruleMap.entrySet()) {
            for (Map.Entry<String, Rule> ruleEntry : entry.getValue().entrySet()) {
                Rule rule = ruleEntry.getValue();

                // Get XmlModelConventions
                Collection<XmlModelConvention> xModelConventions = new ArrayList<XmlModelConvention>();
                for (ModelConvention modCon : rule.getModelConventions()) {
                    XmlModelConvention xmlMoCon = new XmlModelConvention(modCon.getType());
                    xModelConventions.add(xmlMoCon);
                }

                // Get XmlElementConvention
                Collection<XmlElementConvention> xElementConventions = new ArrayList<XmlElementConvention>();
                for (ElementConvention elCon : rule.getElementConventions()) {
                    ElementFieldTypes eFT = elCon.getElementFieldTypes();
                    if (eFT != null) {
                        Collection<String> cElFieTy = eFT.getElementFieldTypes();
                        XmlElementFieldTypes xmlElFieTy = new XmlElementFieldTypes(cElFieTy,
                                elCon.getElementFieldTypes().isExcluded());
                        XmlElementConvention xmlElCon = new XmlElementConvention(elCon.getName(), xmlElFieTy,
                                elCon.getDescription(), elCon.getPattern());
                        xElementConventions.add(xmlElCon);
                    } else {
                        XmlElementConvention xmlElCon = new XmlElementConvention(elCon.getName(), null,
                                elCon.getDescription(), elCon.getPattern());
                        xElementConventions.add(xmlElCon);
                    }
                }

                // Get XmlSettings
                Collection<XmlSetting> xSettings = new ArrayList<>();
                for (Map.Entry<String, Setting> sEntry : rule.getSettings().entrySet()) {
                    Setting s = sEntry.getValue();

                    if (!sEntry.getValue().getScriptPlaces().isEmpty()) {
                        for (String place : sEntry.getValue().getScriptPlaces()) {
                            XmlSetting xmlSetting = new XmlSetting(s.getName(), place, s.getType(), s.getId(),
                                    s.getRequired(),
                                    s.getValue());
                            xSettings.add(xmlSetting);
                        }
                    } else {
                        XmlSetting xmlSetting = new XmlSetting(s.getName(), null, s.getType(), s.getId(), s.getRequired(),
                                s.getValue());
                        xSettings.add(xmlSetting);
                    }
                }

                // create xmlRule
                XmlRule xRule = new XmlRule(rule.getId(), rule.getName(), rule.isActive(), rule.getRuleDescription(),
                        xSettings.isEmpty() ? null : xSettings,
                        xElementConventions.isEmpty() ? null : xElementConventions,
                        xModelConventions.isEmpty() ? null : xModelConventions);

                // add xmlRule to Collection
                xmlRuleCollection.add(xRule);
            }
        }
        return xmlRuleCollection;
    }
}
