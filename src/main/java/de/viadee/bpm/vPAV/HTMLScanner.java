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
package de.viadee.bpm.vPAV;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.xml.sax.SAXException;

public class HTMLScanner {

    private final String camundaVar = "cam-variable-name";

    private final String ngVar = "ng-model";

    private final String input = "input";

    private Document doc;

    public static Logger logger = Logger.getLogger(HTMLScanner.class.getName());

    /**
     * Class to parse an HTML-file
     * 
     * @param path
     *            path of the HTML-file
     * @throws ParserConfigurationException
     *             parser exception
     * @throws SAXException
     *             sax exception
     * @throws IOException
     *             io exception
     */
    public HTMLScanner(String path) throws ParserConfigurationException, SAXException, IOException {
        doc = Jsoup.parse(new File(path), "utf-8");
    }

    /**
     * find all written variables in HTML-file
     * 
     * @return ArrayList of written variablesnames
     */
    public ArrayList<String> getWriteVariables() {
        ArrayList<String> writtenVariables = new ArrayList<String>();

        Elements inputList = doc.select(input + "[" + camundaVar + "]" + "[required=true]");
        inputList.addAll(doc.select(input + "[" + camundaVar + "]" + "[required]"));

        for (Element e : inputList) {
            writtenVariables.add(e.attr(camundaVar));
        }

        return writtenVariables;
    }

    /**
     * find all read variables in HTML-file
     * 
     * @return ArrayList of read variablesnames
     */
    public ArrayList<String> getReadVariables() {
        ArrayList<String> readVariables = new ArrayList<String>();

        Elements inputListC = doc.select(input + "[" + camundaVar + "]" + "[readonly=true]");
        inputListC.addAll(doc.select(input + "[" + camundaVar + "]" + "[readonly]"));

        Elements inputListN = doc.select(input + "[" + ngVar + "]" + "[readonly]");
        inputListN.addAll(doc.select(input + "[" + ngVar + "]" + "[readonly]"));

        for (Element e : inputListC) {
            readVariables.add(e.attr(camundaVar));
        }

        for (Element e : inputListN) {
            if (e.attr(ngVar).contains("."))
                readVariables.add(e.attr(ngVar).substring(0, e.attr(ngVar).indexOf('.')));
            else
                readVariables.add(e.attr(ngVar));
        }

        return readVariables;
    }

}