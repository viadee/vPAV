package de.viadee.bpm.vPAV;
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

import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class BPMNScanner {

    private final String businessRuleTask_one = "bpmn:businessRuleTask";

    private final String serviceTask_one = "bpmn:serviceTask";

    private final String sendTask_one = "bpmn:sendTask";

    private final String gateway_one = "bpmn:exclusiveGateway";

    private final String out_one = "bpmn:outgoing";

    private final String businessRuleTask_two = "bpmn2:businessRuleTask";

    private final String serviceTask_two = "bpmn2:serviceTask";

    private final String sendTask_two = "bpmn2:sendTask";

    private final String gateway_two = "bpmn2:exclusiveGateway";

    private final String out_two = "bpmn2:outgoing";

    private final String businessRuleTask_three = "businessRuleTask";

    private final String serviceTask_three = "serviceTask";

    private final String sendTask_three = "sendTask";

    private final String gateway_three = "exclusiveGateway";

    private final String out_three = "outgoing";

    private final String scriptTag = "camunda:script";

    private final String c_class = "camunda:class";

    private final String c_exp = "camunda:expression";

    private final String c_dexp = "camunda:delegateExpression";

    private final String c_dmn = "camunda:decisionRef";

    private final String c_ext = "camunda:type";

    private final String imp = "implementation";

    private String node_name;

    private DocumentBuilderFactory factory;

    private DocumentBuilder builder;

    private Document doc;

    // private boolean new_model = true;

    private String model_Version;

    /**
     * The Camunda API's method "getimplementation" doesn't return the correct Implementation, so the we have to scan
     * the xml of the model for the implementation
     */
    public BPMNScanner() throws ParserConfigurationException {
        factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        builder = factory.newDocumentBuilder();
    }

    private void setModelVersion(String path) throws SAXException, IOException, ParserConfigurationException {
        // parse the given bpmn model
        doc = builder.parse(path);

        if (doc.getElementsByTagName("bpmn:definitions").getLength() > 0)
            model_Version = "one";
        else if (doc.getElementsByTagName("bpmn2:definitions").getLength() > 0)
            model_Version = "two";
        else if (doc.getElementsByTagName("definitions").getLength() > 0)
            model_Version = "three";
        else
            throw new ParserConfigurationException("Can't get the version of the BPMN Model");
    }

    /**
     * Return the Implementation of an specific element (sendTask, ServiceTask or BusinessRuleTask)
     *
     * @param path,
     *            id
     * @throws ParserConfigurationException,
     *             XPathExpressionException, SAXException, IOException
     * @return
     */
    public String getImplementation(String path, String id)
            throws SAXException, IOException, XPathExpressionException, ParserConfigurationException {
        // List to hold return values
        String return_implementation = null;

        // List for all Task elements
        ArrayList<NodeList> listNodeList = new ArrayList<NodeList>();

        // parse the given bpmn model
        doc = builder.parse(path);

        // set Model Version
        setModelVersion(path);

        if (model_Version == "one") {
            // create nodelist that contains all Tasks with the namespace
            listNodeList.add(doc.getElementsByTagName(businessRuleTask_one));
            listNodeList.add(doc.getElementsByTagName(serviceTask_one));
            listNodeList.add(doc.getElementsByTagName(sendTask_one));
        } else if (model_Version == "two") {
            listNodeList.add(doc.getElementsByTagName(businessRuleTask_two));
            listNodeList.add(doc.getElementsByTagName(serviceTask_two));
            listNodeList.add(doc.getElementsByTagName(sendTask_two));
        } else if (model_Version == "three") {
            listNodeList.add(doc.getElementsByTagName(businessRuleTask_three));
            listNodeList.add(doc.getElementsByTagName(serviceTask_three));
            listNodeList.add(doc.getElementsByTagName(sendTask_three));
        }

        // iterate over list<NodeList> and check each NodeList (BRTask, ServiceTask and SendTask)
        for (final NodeList list : listNodeList) {
            // iterate over list and check child of each node
            for (int i = 0; i < list.getLength(); i++) {
                Element Task_Element = (Element) list.item(i);
                NamedNodeMap Task_Element_Attr = Task_Element.getAttributes();

                // check if the ids are corresponding
                if (id.equals(Task_Element.getAttribute("id"))) {
                    // check if more than 1 inner attribute exists
                    if (Task_Element_Attr.getLength() > 1) {
                        // check all attributes, whether they fit an implementation
                        for (int x = 0; x < Task_Element_Attr.getLength(); x++) {
                            Node attr = Task_Element_Attr.item(x);
                            // node_name equals an implementation
                            node_name = attr.getNodeName();
                            if (node_name.equals(c_class) || node_name.equals(c_exp)
                                    || node_name.equals(c_dexp) || node_name.equals(c_dmn) || node_name.equals(c_ext)) {
                                return_implementation = node_name;
                            }
                        }
                        // if inner attributes dont consist of implementations
                    }
                    if (Task_Element_Attr.getNamedItem(c_class) == null
                            && Task_Element_Attr.getNamedItem(c_exp) == null
                            && Task_Element_Attr.getNamedItem(c_dexp) == null
                            && Task_Element_Attr.getNamedItem(c_dmn) == null
                            && Task_Element_Attr.getNamedItem(c_ext) == null) {
                        return_implementation = imp;
                    }
                }
            }
        }
        return return_implementation;
    }

    /**
     * Check if model has an scriptTag
     *
     * @param path
     * @return boolean
     */
    public String getScriptType(String path, String id) throws SAXException, IOException {
        // bool to hold return values
        String return_scriptType = null;

        // List for all Task elements
        NodeList nodeList;

        // parse the given bpmn model
        doc = builder.parse(path);

        // search for script tag
        nodeList = doc.getElementsByTagName(scriptTag);

        // search for parent with id
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node n = nodeList.item(i).getParentNode();
            return_scriptType = n.getNodeName();
            while (n.getParentNode() != null) {
                if (((Element) n).getAttribute("id").equals(id)) {
                    return return_scriptType;
                } else {
                    n = n.getParentNode();
                }
            }
        }

        return null;
    }

    /**
     * Return a list of used gateways for a given bpmn model
     *
     * @param path
     *            from model
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     *
     */
    public String getXorGateWays(String path, String id)
            throws SAXException, IOException, ParserConfigurationException {
        final NodeList nodeList;

        String gateway = "";

        doc = builder.parse(path);

        // set Model Version
        setModelVersion(path);

        if (model_Version == "one") {
            // create nodelist that contains all Tasks with the namespace
            nodeList = doc.getElementsByTagName(gateway_one);
        } else if (model_Version == "two") {
            nodeList = doc.getElementsByTagName(gateway_two);
        } else if (model_Version == "three") {
            nodeList = doc.getElementsByTagName(gateway_three);
        } else {
            return "";
        }

        // iterate over list and check each item
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element Task_Element = (Element) nodeList.item(i);

            // check if the ids are corresponding
            if (id.equals(Task_Element.getAttribute("id"))) {
                gateway = Task_Element.getAttribute("id");
            }
        }
        return gateway;
    }

    /*
     * Return number of outgoing
     *
     * @param path, id
     *
     * @return number of outgoing
     */
    public int getOutgoing(String path, String id) throws SAXException, IOException, ParserConfigurationException {
        final NodeList nodeList;
        String out = "";
        int outgoing = 0;

        doc = builder.parse(path);

        // set Model Version
        setModelVersion(path);

        if (model_Version == "one") {
            // create nodelist that contains all Tasks with the namespace
            nodeList = doc.getElementsByTagName(gateway_one);
            out = out_one;
        } else if (model_Version == "two") {
            nodeList = doc.getElementsByTagName(gateway_two);
            out = out_two;
        } else if (model_Version == "three") {
            nodeList = doc.getElementsByTagName(gateway_three);
            out = out_three;
        } else {
            return -1;
        }

        // iterate over list and check each item
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element Task_Element = (Element) nodeList.item(i);

            // check if the ids are corresponding
            if (id.equals(Task_Element.getAttribute("id"))) {
                NodeList childNodeGateway = Task_Element.getChildNodes();
                for (int x = 0; x < childNodeGateway.getLength(); x++) {
                    if (childNodeGateway.item(x).getNodeName().equals(out)) {
                        outgoing++;
                    }
                }
            }
        }
        return outgoing;
    }
}