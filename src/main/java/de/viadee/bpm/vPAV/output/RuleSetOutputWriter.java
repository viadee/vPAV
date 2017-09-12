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
 * THIS SOFTWARE IS PROVIDED BY <viadee Unternehmensberatung GmbH> ''AS IS'' AND ANY
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
package de.viadee.bpm.vPAV.output;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import de.viadee.bpm.vPAV.AbstractRunner;
import de.viadee.bpm.vPAV.ConstantsConfig;
import de.viadee.bpm.vPAV.config.model.ElementConvention;
import de.viadee.bpm.vPAV.config.model.ElementFieldTypes;
import de.viadee.bpm.vPAV.config.model.ModelConvention;
import de.viadee.bpm.vPAV.config.model.Rule;
import de.viadee.bpm.vPAV.config.model.Setting;
import de.viadee.bpm.vPAV.config.reader.XmlElementConvention;
import de.viadee.bpm.vPAV.config.reader.XmlElementFieldTypes;
import de.viadee.bpm.vPAV.config.reader.XmlModelConvention;
import de.viadee.bpm.vPAV.config.reader.XmlRule;
import de.viadee.bpm.vPAV.config.reader.XmlRuleSet;
import de.viadee.bpm.vPAV.config.reader.XmlSetting;

/**
 * Ergebnisse aus dem Checker in ein definiertes XML-Format schreiben
 * 
 */
public class RuleSetOutputWriter {
	
	private static Logger logger = Logger.getLogger(RuleSetOutputWriter.class.getName());

    public void write(Map<String, Rule> rules) throws OutputWriterException {
        Writer writer = null;
        
        Path path = Paths.get(ConstantsConfig.EFFECTIVE_RULESET);        
        if (path.toFile().exists())
        		path.toFile().delete();        

        try {
            writer = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(ConstantsConfig.EFFECTIVE_RULESET), "utf-8"));
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
                /* ignore */}
        }
    }

    private static XmlRuleSet transformToXmlDatastructure(final Map<String, Rule> rules) {
        XmlRuleSet xmlRuleSet = new XmlRuleSet();
        Collection<XmlRule> xmlRules = new ArrayList<XmlRule>();

        for (Map.Entry<String, Rule> entry : rules.entrySet()) {
            Rule rule = entry.getValue();

            // Get XmlModelConventions
            Collection<XmlModelConvention> xModelConventions = new ArrayList<XmlModelConvention>();
            for (ModelConvention ModCon : rule.getModelConventions()) {
                XmlModelConvention xmlMoCon = new XmlModelConvention(ModCon.getName(), ModCon.getPattern());
                xModelConventions.add(xmlMoCon);
            }

            // Get XmlElementConvention
            Collection<XmlElementConvention> xElementConventions = new ArrayList<XmlElementConvention>();
            for (ElementConvention ElCon : rule.getElementConventions()) {
                ElementFieldTypes eFT = ElCon.getElementFieldTypes();
                if (eFT != null) {
                    Collection<String> cElFieTy = eFT.getElementFieldTypes();
                    XmlElementFieldTypes xmlElFieTy = new XmlElementFieldTypes(cElFieTy,
                            ElCon.getElementFieldTypes().isExcluded());
                    XmlElementConvention xmlElCon = new XmlElementConvention(ElCon.getName(), xmlElFieTy,
                            ElCon.getPattern());
                    xElementConventions.add(xmlElCon);
                } else {
                    XmlElementConvention xmlElCon = new XmlElementConvention(ElCon.getName(), null,
                            ElCon.getPattern());
                    xElementConventions.add(xmlElCon);
                }
            }

            // Get XmlSettings
            Collection<XmlSetting> xSettings = new ArrayList<XmlSetting>();
            for (Map.Entry<String, Setting> sEntry : rule.getSettings().entrySet()) {
                Setting s = sEntry.getValue();
                if (!sEntry.getValue().getScriptPlaces().isEmpty()) {
                    for (String place : sEntry.getValue().getScriptPlaces()) {
                        XmlSetting xmlSetting = new XmlSetting(s.getName(), place, s.getValue());
                        xSettings.add(xmlSetting);
                    }
                } else {
                    XmlSetting xmlSetting = new XmlSetting(s.getName(), null, s.getValue());
                    xSettings.add(xmlSetting);
                }
            }

            // create xmlRule
            XmlRule xRule = new XmlRule(rule.getName(), rule.isActive(), xSettings.isEmpty() ? null : xSettings,
                    xElementConventions.isEmpty() ? null : xElementConventions,
                    xModelConventions.isEmpty() ? null : xModelConventions);

            // add xmlRule to Collection
            xmlRules.add(xRule);
        }
        xmlRuleSet.setRules(xmlRules);
        return xmlRuleSet;
    }
}
