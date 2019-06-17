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
package de.viadee.bpm.vPAV;

import de.viadee.bpm.vPAV.constants.BpmnConstants;
import de.viadee.bpm.vPAV.processing.ProcessingException;
import org.apache.commons.collections4.map.LinkedMap;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BpmnScanner {

    private DocumentBuilder builder;

    private Document doc;

    private ModelVersionEnum modelVersion;

    private static final Logger LOGGER = Logger.getLogger(BpmnScanner.class.getName());

    private enum ModelVersionEnum {
        V1, V2, V3
    }

    /**
     * The Camunda API's method "getImplementation" doesn't return the correct
     * Implementation, so the we have to scan the xml of the model for the
     * implementation
     * <p>
     * @param path
     * path to model
     */
    public BpmnScanner(String path) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, Boolean.TRUE);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setNamespaceAware(true);
            builder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            LOGGER.log(Level.SEVERE, "Could not instantiate BpmnScanner because of an configuration error.", e);
        }
        try {
            setModelVersion(path);
        } catch (SAXException | ParserConfigurationException | IOException e) {
            throw new ProcessingException("Could not instantiate BpmnScanner. Run aborted");
        }
    }

    /**
     * Checks which camunda namespace is used in a given model and sets the version
     * correspondingly
     *
     * @param path Path to model
     * @throws SAXException
     * @throws IOException
     * @throws ParserConfigurationException
     */
    private void setModelVersion(String path) throws SAXException, IOException, ParserConfigurationException {
        // parse the given bpmn model
        doc = builder.parse(path);

        if (doc.getElementsByTagName(BpmnConstants.DEFINITIONS).getLength() > 0)
            modelVersion = ModelVersionEnum.V1;
        else if (doc.getElementsByTagName(BpmnConstants.BPMN_DEFINITIONS).getLength() > 0)
            modelVersion = ModelVersionEnum.V2;
        else if (doc.getElementsByTagName(BpmnConstants.BPMN2_DEFINITIONS).getLength() > 0)
            modelVersion = ModelVersionEnum.V3;
        else
            throw new ParserConfigurationException("Can't get the version of the BPMN Model");
    }

    /**
     * Return the Implementation of an specific element (sendTask, ServiceTask or
     * BusinessRuleTask)
     *
     * @param id id of bpmn element
     * @return return_implementation contains implementation
     */
    public String getImplementation(String id) {
        // List to hold return values
        String returnImplementation = null;

        String nodeName;

        // List for all Task elements
        ArrayList<NodeList> listNodeList = new ArrayList<NodeList>();

        switch (modelVersion) {
            case V1:
                listNodeList.add(doc.getElementsByTagName(BpmnConstants.BUSINESS_RULE_TASK));
                listNodeList.add(doc.getElementsByTagName(BpmnConstants.SERVICE_TASK));
                listNodeList.add(doc.getElementsByTagName(BpmnConstants.SEND_TASK));
                listNodeList.add(doc.getElementsByTagName(BpmnConstants.END_EVENT));
                listNodeList.add(doc.getElementsByTagName(BpmnConstants.INTERMEDIATE_THROW_EVENT));
                break;
            case V2:
                listNodeList.add(doc.getElementsByTagName(BpmnConstants.BPMN_BUSINESS_RULE_TASK));
                listNodeList.add(doc.getElementsByTagName(BpmnConstants.BPMN_SERVICE_TASK));
                listNodeList.add(doc.getElementsByTagName(BpmnConstants.BPMN_SEND_TASK));
                listNodeList.add(doc.getElementsByTagName(BpmnConstants.BPMN_END_EVENT));
                listNodeList.add(doc.getElementsByTagName(BpmnConstants.BPMN_INTERMEDIATE_THROW_EVENT));
                break;
            case V3:
                listNodeList.add(doc.getElementsByTagName(BpmnConstants.BPMN2_BUSINESS_RULE_TASK));
                listNodeList.add(doc.getElementsByTagName(BpmnConstants.BPMN2_SERVICE_TASK));
                listNodeList.add(doc.getElementsByTagName(BpmnConstants.BPMN2_SEND_TASK));
                listNodeList.add(doc.getElementsByTagName(BpmnConstants.BPMN2_END_EVENT));
                listNodeList.add(doc.getElementsByTagName(BpmnConstants.BPMN2_INTERMEDIATE_THROW_EVENT));
                break;
            default:
                listNodeList = null;
        }

        // iterate over list<NodeList> and check each NodeList (BRTask,
        // ServiceTask and SendTask)
        for (final NodeList list : listNodeList) {
            // iterate over list and check child of each node
            for (int i = 0; i < list.getLength(); i++) {
                Element taskElement = (Element) list.item(i);
                // check if the ids are corresponding
                if (id.equals(taskElement.getAttribute(BpmnConstants.ATTR_ID))) {
                    NamedNodeMap taskElementAttr = taskElement.getAttributes();
                    // check if more than 1 inner attribute exists
                    if (taskElementAttr.getLength() > 1) {
                        // check all attributes, whether they fit an
                        // implementation
                        for (int x = 0; x < taskElementAttr.getLength(); x++) {
                            Node attr = taskElementAttr.item(x);
                            // node_name equals an implementation
                            nodeName = attr.getNodeName();
                            if (nodeName.equals(BpmnConstants.CAMUNDA_CLASS)
                                    || nodeName.equals(BpmnConstants.CAMUNDA_EXPRESSION)
                                    || nodeName.equals(BpmnConstants.CAMUNDA_DEXPRESSION)
                                    || nodeName.equals(BpmnConstants.CAMUNDA_DMN)
                                    || nodeName.equals(BpmnConstants.CAMUNDA_EXT)) {
                                returnImplementation = nodeName;
                                break;
                            }
                        }
                        // if inner attributes dont consist of implementations
                    }
                    if (taskElementAttr.getNamedItem(BpmnConstants.CAMUNDA_CLASS) == null
                            && taskElementAttr.getNamedItem(BpmnConstants.CAMUNDA_EXPRESSION) == null
                            && taskElementAttr.getNamedItem(BpmnConstants.CAMUNDA_DEXPRESSION) == null
                            && taskElementAttr.getNamedItem(BpmnConstants.CAMUNDA_DMN) == null
                            && taskElementAttr.getNamedItem(BpmnConstants.CAMUNDA_EXT) == null) {
                        returnImplementation = BpmnConstants.IMPLEMENTATION;
                        break;
                    }
                }
            }
        }
        return returnImplementation;
    }

    /**
     * @param id             id of bpmnElement
     * @param implementation DelegateExpression/Java Class
     * @return implementationReference
     */
    public String getImplementationReference(String id, String implementation) {
        String implementationReference = "";

        // List for all Task elements
        ArrayList<NodeList> listNodeList = new ArrayList<NodeList>();

        switch (modelVersion) {
            case V1:
                listNodeList.add(doc.getElementsByTagName(BpmnConstants.BUSINESS_RULE_TASK));
                listNodeList.add(doc.getElementsByTagName(BpmnConstants.SERVICE_TASK));
                listNodeList.add(doc.getElementsByTagName(BpmnConstants.SEND_TASK));
                break;
            case V2:
                listNodeList.add(doc.getElementsByTagName(BpmnConstants.BPMN_BUSINESS_RULE_TASK));
                listNodeList.add(doc.getElementsByTagName(BpmnConstants.BPMN_SERVICE_TASK));
                listNodeList.add(doc.getElementsByTagName(BpmnConstants.BPMN_SEND_TASK));
                break;
            case V3:
                listNodeList.add(doc.getElementsByTagName(BpmnConstants.BPMN2_BUSINESS_RULE_TASK));
                listNodeList.add(doc.getElementsByTagName(BpmnConstants.BPMN2_SERVICE_TASK));
                listNodeList.add(doc.getElementsByTagName(BpmnConstants.BPMN2_SEND_TASK));
                break;
            default:
                listNodeList = null;
        }

        // iterate over list<NodeList> and check each NodeList (BRTask,
        // ServiceTask and SendTask)
        for (final NodeList list : listNodeList) {
            // iterate over list and check child of each node
            for (int i = 0; i < list.getLength(); i++) {
                Element taskElement = (Element) list.item(i);

                // check if the ids are corresponding
                if (id.equals(taskElement.getAttribute(BpmnConstants.ATTR_ID))) {
                    // check for implementation reference
                    if (implementation.equals(BpmnConstants.CAMUNDA_CLASS)) {
                        implementationReference = taskElement.getAttribute(BpmnConstants.CAMUNDA_CLASS);
                    } else if (implementation.equals(BpmnConstants.CAMUNDA_DEXPRESSION)) {
                        implementationReference = taskElement.getAttribute(BpmnConstants.CAMUNDA_DEXPRESSION);
                    }
                }
            }
        }

        return implementationReference;
    }

    /**
     * Return the Implementation of an specific element (endEvent and/or
     * intermediateThrowEvent)
     *
     * @param id id of bpmn element
     * @return return_implementation contains implementation
     */
    public String getEventImplementation(String id) {
        // List to hold return values
        String returnImplementation = null;

        // List for all Task elements
        ArrayList<NodeList> listNodeList = new ArrayList<NodeList>();

        switch (modelVersion) {
            case V1:
                listNodeList.add(doc.getElementsByTagName(BpmnConstants.END_EVENT));
                listNodeList.add(doc.getElementsByTagName(BpmnConstants.INTERMEDIATE_THROW_EVENT));
                break;
            case V2:
                listNodeList.add(doc.getElementsByTagName(BpmnConstants.BPMN_END_EVENT));
                listNodeList.add(doc.getElementsByTagName(BpmnConstants.BPMN_INTERMEDIATE_THROW_EVENT));
                break;
            case V3:
                listNodeList.add(doc.getElementsByTagName(BpmnConstants.BPMN2_END_EVENT));
                listNodeList.add(doc.getElementsByTagName(BpmnConstants.BPMN2_INTERMEDIATE_THROW_EVENT));
                break;
            default:
                listNodeList = null;
        }

        // iterate over list<NodeList> and check each NodeList (endEvent,
        // intermediateThrowEvent)
        for (final NodeList list : listNodeList) {
            // iterate over list and check child of each node
            for (int i = 0; i < list.getLength(); i++) {
                final Element taskElement = (Element) list.item(i);

                // check if the ids are corresponding
                if (id.equals(taskElement.getAttribute(BpmnConstants.ATTR_ID))) {

                    final NodeList childNodes = taskElement.getChildNodes();

                    // check all attributes, whether they equal a messageEventDefinition
                    for (int x = 0; x < childNodes.getLength(); x++) {
                        if (childNodes.item(x).getLocalName() != null
                                && childNodes.item(x).getLocalName().equals(BpmnConstants.MESSAGE_EVENT_DEFINITION)) {
                            final Element event = (Element) childNodes.item(x);

                            // if the node messageEventDefinition contains the camunda expression -> return
                            if (event.getAttributeNode(BpmnConstants.CAMUNDA_EXPRESSION) != null) {
                                returnImplementation = event.getAttributeNode(BpmnConstants.CAMUNDA_EXPRESSION)
                                        .toString();
                            } else if (event.getAttributeNode(BpmnConstants.CAMUNDA_DEXPRESSION) != null) {
                                returnImplementation = event.getAttributeNode(BpmnConstants.CAMUNDA_DEXPRESSION)
                                        .toString();
                            } else if (event.getAttributeNode(BpmnConstants.CAMUNDA_CLASS) != null) {
                                returnImplementation = event.getAttributeNode(BpmnConstants.CAMUNDA_CLASS).toString();
                            } else if (event.getAttributeNode(BpmnConstants.CAMUNDA_EXT) != null) {
                                returnImplementation = event.getAttributeNode(BpmnConstants.CAMUNDA_EXT).toString();
                            }

                            if (event.getAttributeNode(BpmnConstants.CAMUNDA_DEXPRESSION) == null
                                    && event.getAttributeNode(BpmnConstants.CAMUNDA_EXPRESSION) == null
                                    && event.getAttributeNode(BpmnConstants.CAMUNDA_CLASS) == null
                                    && event.getAttributeNode(BpmnConstants.CAMUNDA_EXT) == null) {
                                returnImplementation = BpmnConstants.IMPLEMENTATION;
                            }
                        }
                    }
                }
            }
        }
        return returnImplementation;
    }

    /**
     * @param id       id of bpmn element
     * @param listType Type of Attribute
     * @param extType  Type of Listener
     * @return value of Listener
     */
    public ArrayList<String> getListener(String id, String listType, String extType) {

        // list to hold return values
        ArrayList<String> returnAttrList = new ArrayList<>();

        // List for all Task elements
        NodeList nodeListExtensionElements;

        // search for script tag

        switch (modelVersion) {
            case V1:
                nodeListExtensionElements = doc.getElementsByTagName(BpmnConstants.EXTENSION_ELEMENTS);
                break;
            case V2:
                nodeListExtensionElements = doc.getElementsByTagName(BpmnConstants.BPMN_EXTENSION_ELEMENTS);
                break;
            case V3:
                nodeListExtensionElements = doc.getElementsByTagName(BpmnConstants.BPMN2_EXTENSION_ELEMENTS);
                break;
            default:
                nodeListExtensionElements = null;
        }

        // search for parent with id
        for (int i = 0; i < nodeListExtensionElements.getLength(); i++) {
            if (((Element) nodeListExtensionElements.item(i).getParentNode()).getAttribute(BpmnConstants.ATTR_ID)
                    .equals(id)) {
                NodeList childNodes = nodeListExtensionElements.item(i).getChildNodes();
                for (int x = 0; x < childNodes.getLength(); x++) {
                    if (childNodes.item(x).getNodeName().equals(extType)) {
                        String attName = checkAttributesOfNode(childNodes.item(x), listType);
                        if (attName != null)
                            returnAttrList.add(attName);
                    }
                }
            }
        }
        return returnAttrList;
    }

    /**
     * @param node     node to check
     * @param listType Type of ExecutionListener
     * @return textContent of ListenerType
     */
    private String checkAttributesOfNode(Node node, String listType) {
        NamedNodeMap attributes = node.getAttributes();
        for (int x = 0; x < attributes.getLength(); x++) {
            if (attributes.item(x).getNodeName().equals(listType)) {
                return attributes.item(x).getTextContent();
            }
        }
        return null;
    }

    /**
     * Check if model has an scriptTag
     *
     * @param id id of bpmn element
     * @return scriptPlaces contains script type
     */
    public ArrayList<String> getScriptTypes(String id) {
        // bool to hold return values
        ArrayList<String> returnScriptType = new ArrayList<>();

        // List for all Task elements
        NodeList nodeList;

        // search for script tag
        nodeList = doc.getElementsByTagName(BpmnConstants.SCRIPT_TAG);

        // search for parent with id
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node n = nodeList.item(i).getParentNode();
            if (idMatch(nodeList.item(i), id)) {
                returnScriptType.add(n.getLocalName());
            }
        }

        return returnScriptType;
    }

    /**
     * Retrieve startevents to check whether any parent node is a subprocess
     *
     * @param id id of element to check
     * @return true if id was found
     */
    public boolean checkStartEvent(String id) {

        // List for startevents elements
        NodeList nodeList;
        boolean isSubprocess = false;

        switch (modelVersion) {
            case V1:
                nodeList = doc.getElementsByTagName(BpmnConstants.START_EVENT);
                break;
            case V2:
                nodeList = doc.getElementsByTagName(BpmnConstants.BPMN_START_EVENT);
                break;
            case V3:
                nodeList = doc.getElementsByTagName(BpmnConstants.BPMN2_START_EVENT);
                break;
            default:
                nodeList = null;
        }

        if (nodeList != null) {
            for (int i = 0; i < nodeList.getLength(); i++) {
                Element startEvent = (Element) nodeList.item(i);
                if (id.equals(startEvent.getAttribute(BpmnConstants.ATTR_ID))) {
                    return isSubprocess(startEvent);
                }
            }
        }

        return isSubprocess;
    }

    /**
     * Check whether any parent node is a subprocess
     *
     * @param e Element
     * @return True if any parent node is a subprocess and false if any parent node
     * is a process
     */
    private boolean isSubprocess(Element e) {

        if (e.getParentNode() != null) {
            if (e.getParentNode().getLocalName() != null
                    && e.getParentNode().getLocalName().equals(BpmnConstants.SUBPROCESS)) {
                return true;
            } else if (e.getParentNode().getLocalName() != null
                    && e.getParentNode().getLocalName().equals(BpmnConstants.PROCESS)) {
                return false;
            }
            isSubprocess((Element) e.getParentNode());
        }

        return false;
    }

    /**
     * Check if any parentnode has the specific id
     *
     * @param n  Node to check their parents
     * @param id id to check
     * @return true if id was found
     */
    private boolean idMatch(Node n, String id) {
        Element e = (Element) n;

        if (e.getAttribute(BpmnConstants.ATTR_ID).equals(id))
            return true;

        while (e.getParentNode() != null && !e.getParentNode().getLocalName().equals(BpmnConstants.PROCESS)) {
            Element check = (Element) e.getParentNode();
            if (check.getAttribute(BpmnConstants.ATTR_ID).equals(id)) {
                return true;
            } else {
                e = (Element) e.getParentNode();
            }
        }
        return false;
    }

    /**
     * Checks for scripts in conditional expressions
     *
     * @param id id of the element
     * @return boolean has condition Expression
     */
    public boolean hasScriptInCondExp(String id) {
        // List for all Task elements
        NodeList nodeList = null;

        switch (modelVersion) {
            case V1:
                // create nodelist that contains all Tasks with the namespace
                nodeList = doc.getElementsByTagName(BpmnConstants.SEQUENCE);
                break;
            case V2:
                nodeList = doc.getElementsByTagName(BpmnConstants.BPMN_SEQUENCE);
                break;
            case V3:
                nodeList = doc.getElementsByTagName(BpmnConstants.BPMN2_SEQUENCE);
                break;
        }

        if (nodeList != null) {
            for (int i = 0; i < nodeList.getLength(); i++) {
                Element sequenceElement = (Element) nodeList.item(i);
                if (sequenceElement.getAttribute(BpmnConstants.ATTR_ID).equals(id)) {
                    return hasCondExp(sequenceElement);
                }
            }
        }

        return false;
    }

    /**
     * check if sequenceFlow has an Script (value in language attribute) in
     * conditionalExpression
     *
     * @param sq sequenceFlowNode
     * @return true or false
     */
    private boolean hasCondExp(Element sq) {
        NodeList childNodes = null;
        if (sq.hasChildNodes()) {
            childNodes = sq.getChildNodes();
            for (int i = 0; i < childNodes.getLength(); i++) {
                Node childNode = childNodes.item(i);
                if (childNode.getLocalName() != null && childNode.getLocalName().equals(BpmnConstants.CONDITION_EXPRESSION)) {
                    Element childElement = (Element) childNode;
                    if (childElement.getAttribute(BpmnConstants.LANG).trim().length() > 0)
                        return true;
                }
            }
        }
        return false;
    }

    /**
     * Return a list of used gateways for a given bpmn model
     *
     * @param id id of bpmn element
     * @return gateway contains script type
     */
    public String getXorGateWays(String id) {
        final NodeList nodeList;

        String gateway = "";

        switch (modelVersion) {
            case V1:
                nodeList = doc.getElementsByTagName(BpmnConstants.GATEWAY);
                break;
            case V2:
                nodeList = doc.getElementsByTagName(BpmnConstants.BPMN_GATEWAY);
                break;
            case V3:
                nodeList = doc.getElementsByTagName(BpmnConstants.BPMN2_GATEWAY);
                break;
            default:
                return "";
        }

        // iterate over list and check each item
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element taskElement = (Element) nodeList.item(i);

            // check if the ids are corresponding
            if (id.equals(taskElement.getAttribute(BpmnConstants.ATTR_ID))) {
                gateway = taskElement.getAttribute(BpmnConstants.ATTR_ID);
            }
        }
        return gateway;
    }

    /**
     * Return number of outgoing
     *
     * @param id id of bpmn element
     * @return outgoing number of outgoing
     */
    public int getOutgoing(String id) {
        final NodeList nodeList;
        String out = "";
        int outgoing = 0;

        switch (modelVersion) {
            case V1:
                // create nodelist that contains all Tasks with the namespace
                nodeList = doc.getElementsByTagName(BpmnConstants.GATEWAY);
                out = BpmnConstants.OUT;
                break;
            case V2:
                nodeList = doc.getElementsByTagName(BpmnConstants.BPMN_GATEWAY);
                out = BpmnConstants.BPMN_OUT;
                break;
            case V3:
                nodeList = doc.getElementsByTagName(BpmnConstants.BPMN2_GATEWAY);
                out = BpmnConstants.BPMN2_OUT;
                break;
            default:
                return -1;
        }

        // iterate over list and check each item
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element taskElement = (Element) nodeList.item(i);

            // check if the ids are corresponding
            if (id.equals(taskElement.getAttribute(BpmnConstants.ATTR_ID))) {
                NodeList childNodeGateway = taskElement.getChildNodes();
                for (int x = 0; x < childNodeGateway.getLength(); x++) {
                    if (childNodeGateway.item(x).getNodeName().equals(out)) {
                        outgoing++;
                    }
                }
            }
        }
        return outgoing;
    }

    /**
     * get sequenceFlow attributes such as sourceRef and targetRef
     *
     * @param id id of bpmn element
     * @return ArrayList of outgoing Nodes
     */
    public ArrayList<String> getSequenceFlowDef(String id) {

        final ArrayList<String> references = new ArrayList<>();

        NodeList nodeList = getNodeListByVersion();

        if (nodeList != null) {
            for (int i = 0; i < nodeList.getLength(); i++) {
                Element taskElement = (Element) nodeList.item(i);

                // check if the ids are corresponding and retrieve the attributes for target and
                // source reference
                if (id.equals(taskElement.getAttribute(BpmnConstants.ATTR_ID))) {
                    references.add(taskElement.getAttribute(BpmnConstants.SOURCE_REF));
                    references.add(taskElement.getAttribute(BpmnConstants.TARGET_REF));
                }
            }
        }

        return references;
    }

    /**
     * check xor gateways for outgoing edges
     *
     * @param id id of bpmn element
     * @return ArrayList of outgoing Nodes
     */
    public ArrayList<Node> getOutgoingEdges(String id) {

        ArrayList<Node> outgoingEdges = new ArrayList<Node>();
        NodeList nodeList = null;
        String out = "";

        switch (modelVersion) {
            case V1:
                // create nodelist that contains all Tasks with the namespace
                nodeList = doc.getElementsByTagName(BpmnConstants.GATEWAY);
                out = BpmnConstants.OUT;
                break;
            case V2:
                nodeList = doc.getElementsByTagName(BpmnConstants.BPMN_GATEWAY);
                out = BpmnConstants.BPMN_OUT;
                break;
            case V3:
                nodeList = doc.getElementsByTagName(BpmnConstants.BPMN2_GATEWAY);
                out = BpmnConstants.BPMN2_OUT;
                break;
        }

        if (nodeList != null) {
            for (int i = 0; i < nodeList.getLength(); i++) {
                Element taskElement = (Element) nodeList.item(i);

                // check if the ids are corresponding and retrieve the outgoing edges of the xor
                // gateway
                if (id.equals(taskElement.getAttribute(BpmnConstants.ATTR_ID))) {
                    NodeList children = taskElement.getChildNodes();
                    for (int j = 0; j < children.getLength(); j++) {
                        if (children.item(j).getNodeName().equals(out)) {
                            outgoingEdges.add(checkNamingOfEdges(children.item(j).getTextContent()));
                        }
                    }
                }
            }
        }

        return outgoingEdges;
    }

    /**
     * check xor gateways for outgoing edges
     *
     * @param id id of edge
     * @return edge
     */
    public Node checkNamingOfEdges(String id) {

        Node edge = null;
        NodeList nodeList = getNodeListByVersion();

        if (nodeList != null) {
            for (int i = 0; i < nodeList.getLength(); i++) {
                Element taskElement = (Element) nodeList.item(i);
                if (taskElement.getAttribute(BpmnConstants.ATTR_ID).equals(id)) {
                    edge = taskElement;
                }
            }
        }

        return edge;
    }

    private NodeList getNodeListByVersion() {
        switch (modelVersion) {
            case V1:
                // create nodelist that contains all Tasks with the namespace
                return doc.getElementsByTagName(BpmnConstants.SEQUENCE);
            case V2:
                return doc.getElementsByTagName(BpmnConstants.BPMN_SEQUENCE);
            case V3:
                return doc.getElementsByTagName(BpmnConstants.BPMN2_SEQUENCE);
            default:
                return null;
        }
    }

    /**
     * get ids and timer definition for all timer event types
     *
     * @param id id of bpmn element
     * @return Map with timerEventDefinition-Node and his child
     */
    public Map<Element, Element> getTimerImplementation(final String id) {

        // List for all Task elements
        ArrayList<NodeList> listNodeList = new ArrayList<NodeList>();

        switch (modelVersion) {
            case V1:
                listNodeList.add(doc.getElementsByTagName(BpmnConstants.START_EVENT));
                listNodeList.add(doc.getElementsByTagName(BpmnConstants.INTERMEDIATE_CATCH_EVENT));
                listNodeList.add(doc.getElementsByTagName(BpmnConstants.BOUNDARY_EVENT));
                break;
            case V2:
                listNodeList.add(doc.getElementsByTagName(BpmnConstants.BPMN_START_EVENT));
                listNodeList.add(doc.getElementsByTagName(BpmnConstants.BPMN_INTERMEDIATE_CATCH_EVENT));
                listNodeList.add(doc.getElementsByTagName(BpmnConstants.BPMN_BOUNDARY_EVENT));
                break;
            case V3:
                listNodeList.add(doc.getElementsByTagName(BpmnConstants.BPMN2_START_EVENT));
                listNodeList.add(doc.getElementsByTagName(BpmnConstants.BPMN2_INTERMEDIATE_CATCH_EVENT));
                listNodeList.add(doc.getElementsByTagName(BpmnConstants.BPMN2_BOUNDARY_EVENT));
                break;
            default:
                listNodeList = null;
        }

        final Map<Element, Element> timerList = new HashMap<>();

        // iterate over list<NodeList>
        for (final NodeList list : listNodeList) {
            for (int i = 0; i < list.getLength(); i++) {
                final Element taskElement = (Element) list.item(i);

                // check whether a node matches with the provided id
                if (taskElement.getAttribute(BpmnConstants.ATTR_ID).equals(id)) {

                    final NodeList childNodes = taskElement.getChildNodes();
                    for (int x = 0; x < childNodes.getLength(); x++) {

                        // check if an event consists of a timereventdefinition tag
                        if (childNodes.item(x).getLocalName() != null
                                && childNodes.item(x).getLocalName().equals(BpmnConstants.TIMER_EVENT_DEFINITION)) {

                            timerList.put(taskElement, null);

                            // retrieve values of children
                            final Element taskElement2 = (Element) childNodes.item(x);
                            final NodeList childChildNodes = taskElement2.getChildNodes();
                            for (int y = 0; y < childChildNodes.getLength(); y++) {
                                // localname must be either timeDate, timeCycle or timeDuration
                                // add nodes/elements to map
                                if (childChildNodes.item(y).getLocalName() != null) {
                                    timerList.put(taskElement, (Element) childChildNodes.item(y));
                                }
                            }
                        }
                    }
                }
            }
        }
        return timerList;
    }

    /**
     * get mapping type for input/output parameters for given element by id
     *
     * @param id      id of current element
     * @param mapping input or output mapping
     * @return mapping type
     */
    public LinkedMap<String, String> getMappingType(final String id, final String mapping) {
        final LinkedMap<String, String> mappingTypes = new LinkedMap<>();
        final NodeList nodeList = doc.getElementsByTagName(mapping);
        for (int i = 0; i < nodeList.getLength(); i++) {
            final Node node = nodeList.item(i);
            if (idMatch(node, id)) {
                // if more than one child, check on list and value
                if (node.hasChildNodes() && node.getChildNodes().getLength() > 1) {
                    final NodeList nodeChilds = node.getChildNodes();
                    for (int y = 0; y < nodeChilds.getLength(); y++) {
                        switch (nodeChilds.item(y).getNodeName()) {
                            case BpmnConstants.CAMUNDA_LIST: {
                                mappingTypes.put(nodeChilds.item(y).getNodeName(), null);
                                break;
                            }
                            case BpmnConstants.CAMUNDA_MAP: {
                                mappingTypes.put(nodeChilds.item(y).getNodeName(), null);
                                break;
                            }
                            case BpmnConstants.CAMUNDA_SCRIPT: {
                                mappingTypes.put(nodeChilds.item(y).getNodeName(), ((Element) nodeChilds.item(y)).getAttribute(BpmnConstants.SCRIPT_FORMAT));
                                break;
                            }
                        }
                    }
                } else {
                    mappingTypes.put(mapping, null);
                }
            }
        }
        return mappingTypes;
    }


    /**
     * get mapping of inputParameters
     *
     * @param id element id
     * @return HashMap with mapped variables and values
     */
    public HashMap<String, Map<String, String>> getInputMapping(final String id) {
        return getInputMappingValues(id);
    }

    /**
     * get input mapping of given element
     *
     * @param id id of the element
     * @return HashMap with mapped variables and values
     */
    private HashMap<String, Map<String, String>> getInputMappingValues(final String id) {
        // Map for all Task elements
        final HashMap<String, Map<String, String>> variables = new HashMap<>();

        final NodeList nodeList = doc.getElementsByTagName(BpmnConstants.CAMUNDA_INPUT_PARAMETER);

        retrieveMapping(id, variables, nodeList);
        return variables;
    }

    /**
     * get mapping of outputParameters
     *
     * @param id element id
     * @return HashMap with mapped variables and values
     */
    public HashMap<String, Map<String, String>> getOutputMapping(final String id) {
        return getOutputMappingValues(id);
    }

    /**
     * get output mapping of given element
     *
     * @param id id of the element
     * @return HashMap with mapped variables and values
     */
    private HashMap<String, Map<String, String>> getOutputMappingValues(final String id) {
        // Map for all Task elements
        final HashMap<String, Map<String, String>> variables = new HashMap<>();

        final NodeList nodeList = doc.getElementsByTagName(BpmnConstants.CAMUNDA_OUTPUT_PARAMETER);

        retrieveMapping(id, variables, nodeList);
        return variables;
    }

    /**
     * Retrieves the input/output mapping for a given Element by Id
     *
     * @param id        Id of current element
     * @param variables Map holding variable mapping
     * @param nodeList  list of nodes for current parent BPMN element
     */
    private void retrieveMapping(final String id, final HashMap<String, Map<String, String>> variables,
                                 final NodeList nodeList) {
        for (int i = 0; i < nodeList.getLength(); i++) {
            final Node node = nodeList.item(i);
            if (idMatch(node, id)) {
                // if more than one child, check on list and value
                if (node.hasChildNodes() && node.getChildNodes().getLength() > 1) {
                    final NodeList nodeChilds = node.getChildNodes();
                    for (int y = 0; y < nodeChilds.getLength(); y++) {
                        switch (nodeChilds.item(y).getNodeName()) {
                            case BpmnConstants.CAMUNDA_LIST: {
                                final NodeList listChilds = nodeChilds.item(y).getChildNodes();
                                final HashMap<String, String> innerVariables = new HashMap<>();
                                for (int z = 0; z < listChilds.getLength(); z++) {
                                    if (listChilds.item(z).getNodeName().equals(BpmnConstants.CAMUNDA_VALUE)) {
                                        innerVariables.put(Integer.toString(z), listChilds.item(z).getTextContent());
                                        variables.put(((Element) node).getAttribute(BpmnConstants.ATTR_NAME),
                                                innerVariables);
                                    }
                                }
                                break;
                            }
                            case BpmnConstants.CAMUNDA_MAP: {
                                final NodeList mapChilds = nodeChilds.item(y).getChildNodes();
                                final HashMap<String, String> innerVariables = new HashMap<>();
                                for (int x = 0; x < mapChilds.getLength(); x++) {
                                    if (mapChilds.item(x).getNodeName().equals(BpmnConstants.CAMUNDA_ENTRY)) {
                                        innerVariables.put(
                                                ((Element) mapChilds.item(x)).getAttribute(BpmnConstants.ATTR_KEY),
                                                mapChilds.item(x).getTextContent());
                                        variables.put(((Element) node).getAttribute(BpmnConstants.ATTR_NAME),
                                                innerVariables);
                                    }
                                }
                                break;
                            }
                            case BpmnConstants.CAMUNDA_SCRIPT: {
                                final NodeList listChilds = nodeChilds.item(y).getChildNodes();
                                final HashMap<String, String> innerVariables = new HashMap<>();
                                for (int z = 0; z < listChilds.getLength(); z++) {
                                    innerVariables.put(Integer.toString(z), listChilds.item(z).getTextContent());
                                    variables.put(((Element) node).getAttribute(BpmnConstants.ATTR_NAME), innerVariables);
                                }
                                break;
                            }
                        }
                    }
                } else {
                    final HashMap<String, String> innerVariables = new HashMap<>();
                    innerVariables.put(((Element) node).getAttribute(BpmnConstants.ATTR_NAME), node.getTextContent());
                    variables.put(((Element) node).getAttribute(BpmnConstants.ATTR_NAME), innerVariables);
                }
            }
        }
    }

    /**
     * get value of expression
     *
     * @param id id from element
     * @return value of expression
     */
    public ArrayList<String> getFieldInjectionExpression(String id) {
        ArrayList<String> varNames = new ArrayList<String>();
        NodeList nodeList = doc.getElementsByTagName(BpmnConstants.CAMUNDA_FIELD);
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (idMatch(node, id)) {
                for (int y = 0; y < node.getChildNodes().getLength(); y++) {
                    if (node.getChildNodes().item(y).getNodeName().equals(BpmnConstants.CAMUNDA_EXPRESSION)) {
                        varNames.add(node.getChildNodes().item(y).getTextContent());
                    }
                }
            }
        }
        return varNames;
    }

    /**
     * get names of variable in fieldInjection
     *
     * @param id id from element
     * @return names of variable
     */
    public ArrayList<String> getFieldInjectionVarName(String id) {
        ArrayList<String> varNames = new ArrayList<String>();
        NodeList nodeList = doc.getElementsByTagName(BpmnConstants.CAMUNDA_FIELD);
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (idMatch(node, id))
                for (int y = 0; y < node.getAttributes().getLength(); y++) {
                    if (node.getAttributes().item(y).getNodeName().equals(BpmnConstants.ATTR_NAME))
                        varNames.add(node.getAttributes().item(y).getNodeValue());
                }
        }
        return varNames;
    }

    /**
     * get errorEventDefinition
     *
     * @param id id from element
     * @return errorEvent
     */
    public Map<String, String> getErrorEvent(String id) {

        // List for all Task elements
        ArrayList<NodeList> listNodeList = new ArrayList<NodeList>();

        switch (modelVersion) {
            case V1:
                listNodeList.add(doc.getElementsByTagName(BpmnConstants.BOUNDARY_EVENT));
                break;
            case V2:
                listNodeList.add(doc.getElementsByTagName(BpmnConstants.BPMN_BOUNDARY_EVENT));
                break;
            case V3:
                listNodeList.add(doc.getElementsByTagName(BpmnConstants.BPMN2_BOUNDARY_EVENT));
                break;
            default:
                listNodeList = null;

        }

        final Map<String, String> boundaryEventList = new HashMap<>();

        // iterate over list<NodeList>
        for (final NodeList list : listNodeList) {
            for (int i = 0; i < list.getLength(); i++) {
                final Element taskElement = (Element) list.item(i);

                // check whether a node matches with the provided id
                if (taskElement.getAttribute(BpmnConstants.ATTR_ID).equals(id)) {

                    final NodeList childNodes = taskElement.getChildNodes();
                    for (int x = 0; x < childNodes.getLength(); x++) {

                        // check if an event consists of a errorEventDefinition tag
                        if (childNodes.item(x).getLocalName() != null
                                && childNodes.item(x).getLocalName().equals(BpmnConstants.ERROR_EVENT_DEFINITION)) {
                            final Element taskElement2 = (Element) childNodes.item(x);
                            boundaryEventList.put(taskElement2.getAttribute(BpmnConstants.ATTR_ERROR_REF),
                                    taskElement2.getAttribute(BpmnConstants.CAMUNDA_ERROR_MESSAGE_VARIABLE));

                        }
                    }
                }
            }
        }
        return boundaryEventList;
    }

    /**
     * get errorDefinition
     *
     * @param id id from element
     * @return Map with errorName and errorCode
     */
    public Map<String, String> getErrorDef(String id) {

        NodeList nodeList = null;

        switch (modelVersion) {
            case V1:
                nodeList = doc.getElementsByTagName(BpmnConstants.ERROR);
                break;
            case V2:
                nodeList = doc.getElementsByTagName(BpmnConstants.BPMN_ERROR);
                break;
            case V3:
                nodeList = doc.getElementsByTagName(BpmnConstants.BPMN2_ERROR);
                break;
            default:
                break;
        }

        final Map<String, String> errorDef = new HashMap<>();

        if (nodeList != null) {
            // iterate over list and check each item
            for (int i = 0; i < nodeList.getLength(); i++) {
                Element taskElement = (Element) nodeList.item(i);

                // check if the ids are corresponding
                if (id.equals(taskElement.getAttribute(BpmnConstants.ATTR_ID))) {
                    errorDef.put(taskElement.getAttribute(BpmnConstants.ATTR_NAME),
                            taskElement.getAttribute(BpmnConstants.ATTR_ERROR_CODE));
                }
            }
        }

        return errorDef;
    }

    /**
     * get errorCodeVariable
     *
     * @param id id from element
     * @return String with errorCodeVariable
     */
    public String getErrorCodeVar(String id) {
        // List for all Task elements
        ArrayList<NodeList> listNodeList = new ArrayList<NodeList>();

        switch (modelVersion) {
            case V1:
                listNodeList.add(doc.getElementsByTagName(BpmnConstants.BOUNDARY_EVENT));
                break;
            case V2:
                listNodeList.add(doc.getElementsByTagName(BpmnConstants.BPMN_BOUNDARY_EVENT));
                break;
            case V3:
                listNodeList.add(doc.getElementsByTagName(BpmnConstants.BPMN2_BOUNDARY_EVENT));
                break;
            default:
                listNodeList = null;

        }

        String getErrorCodeVar = "";

        // iterate over list<NodeList>
        for (final NodeList list : listNodeList) {
            for (int i = 0; i < list.getLength(); i++) {
                final Element taskElement = (Element) list.item(i);

                // check whether a node matches with the provided id
                if (taskElement.getAttribute(BpmnConstants.ATTR_ID).equals(id)) {

                    final NodeList childNodes = taskElement.getChildNodes();
                    for (int x = 0; x < childNodes.getLength(); x++) {

                        // check if an event consists of a errorEventDefinition tag
                        if (childNodes.item(x).getLocalName() != null
                                && childNodes.item(x).getLocalName().equals(BpmnConstants.ERROR_EVENT_DEFINITION)) {
                            final Element taskElement2 = (Element) childNodes.item(x);
                            getErrorCodeVar = taskElement2.getAttribute(BpmnConstants.CAMUNDA_ERROR_CODE_VARIABLE);
                        }
                    }
                }
            }
        }
        return getErrorCodeVar;
    }

    /**
     * @param id id of boundaryErrorEvent
     * @return attachedToTask
     */
    public String getErrorEventMapping(String id) {

        String attachedToTask = "";
        NodeList nodeList = null;

        switch (modelVersion) {
            case V1:
                nodeList = doc.getElementsByTagName(BpmnConstants.BOUNDARY_EVENT);
                break;
            case V2:
                nodeList = doc.getElementsByTagName(BpmnConstants.BPMN_BOUNDARY_EVENT);
                break;
            case V3:
                nodeList = doc.getElementsByTagName(BpmnConstants.BPMN2_BOUNDARY_EVENT);
                break;
            default:
                break;
        }

        if (nodeList != null) {
            // iterate over list and check each item
            for (int i = 0; i < nodeList.getLength(); i++) {
                Element taskElement = (Element) nodeList.item(i);

                // check if the ids are corresponding
                if (id.equals(taskElement.getAttribute(BpmnConstants.ATTR_ID))) {
                    attachedToTask = taskElement.getAttribute(BpmnConstants.ATTACHED_TO_REF);
                }
            }
        }

        return attachedToTask;
    }

    /**
     * @param id id of bpmn element
     * @return map with key value pair of given element
     */
    public Map<String, String> getKeyPairs(final String id) {

        final Map<String, String> keyPairs = new HashMap<String, String>();

        final NodeList nodeList = doc.getElementsByTagName(BpmnConstants.CAMUNDA_PROPERTY);

        for (int i = 0; i < nodeList.getLength(); i++) {

            // Due to the static nesting of nodes, we can check the third parent node
            // whether the id are corresponding
            Element parent_element = (Element) nodeList.item(i).getParentNode().getParentNode().getParentNode();
            if (parent_element.getAttribute(BpmnConstants.ATTR_ID).equals(id)) {
                Element extension_node = (Element) nodeList.item(i);
                keyPairs.put(extension_node.getAttribute(BpmnConstants.ATTR_NAME),
                        extension_node.getAttribute(BpmnConstants.ATTR_VALUE));
            }
        }

        return keyPairs;
    }

    public ArrayList<String> getLinkRefs(String id) {
        return getModelRefs(id, BpmnConstants.LINK_EVENT_DEFINITION, BpmnConstants.ATTR_NAME);
    }

    public ArrayList<String> getSignalRefs(String id) {
        return getModelRefs(id, BpmnConstants.SIGNAL_EVENT_DEFINITION, BpmnConstants.ATTR_SIGNAL_REF);
    }

    public ArrayList<String> getMessageRefs(String id) {
        ArrayList<String> messageRefs = getModelRefs(id, BpmnConstants.MESSAGE_EVENT_DEFINITION,
                BpmnConstants.ATTR_MESSAGE_REF);
        messageRefs.addAll(getMessageRefFromReceiveTask(id));
        return messageRefs;
    }

    private ArrayList<String> getMessageRefFromReceiveTask(String id) {
        ArrayList<String> messageRefs = new ArrayList<>();
        NodeList nodeList = null;

        switch (modelVersion) {
            case V1:
                nodeList = doc.getElementsByTagName(BpmnConstants.RECEIVE_TASK);
                break;
            case V2:
                nodeList = doc.getElementsByTagName(BpmnConstants.BPMN_RECEIVE_TASK);
                break;
            case V3:
                nodeList = doc.getElementsByTagName(BpmnConstants.BPMN2_RECEIVE_TASK);
                break;
            default:
                break;
        }

        if (nodeList != null) {
            // iterate over list and check each item
            for (int i = 0; i < nodeList.getLength(); i++) {
                Element taskElement = (Element) nodeList.item(i);
                if (id.equals(taskElement.getAttribute(BpmnConstants.ATTR_ID)))
                    messageRefs.add(taskElement.getAttribute(BpmnConstants.ATTR_MESSAGE_REF));
            }
        }

        return messageRefs;
    }

    private ArrayList<String> getModelRefs(String id, String eventDefinition, String attrRef) {
        ArrayList<String> refs = new ArrayList<String>();

        switch (modelVersion) {
            case V1:
                refs.addAll(getRefs(id, BpmnConstants.BOUNDARY_EVENT, eventDefinition, attrRef));
                refs.addAll(getRefs(id, BpmnConstants.INTERMEDIATE_CATCH_EVENT, eventDefinition, attrRef));
                refs.addAll(getRefs(id, BpmnConstants.INTERMEDIATE_THROW_EVENT, eventDefinition, attrRef));
                refs.addAll(getRefs(id, BpmnConstants.START_EVENT, eventDefinition, attrRef));
                refs.addAll(getRefs(id, BpmnConstants.END_EVENT, eventDefinition, attrRef));
                break;
            case V2:
                refs.addAll(getRefs(id, BpmnConstants.BPMN_BOUNDARY_EVENT, eventDefinition, attrRef));
                refs.addAll(getRefs(id, BpmnConstants.BPMN_INTERMEDIATE_CATCH_EVENT, eventDefinition, attrRef));
                refs.addAll(getRefs(id, BpmnConstants.BPMN_INTERMEDIATE_THROW_EVENT, eventDefinition, attrRef));
                refs.addAll(getRefs(id, BpmnConstants.BPMN_START_EVENT, eventDefinition, attrRef));
                refs.addAll(getRefs(id, BpmnConstants.BPMN_END_EVENT, eventDefinition, attrRef));
                break;
            case V3:
                refs.addAll(getRefs(id, BpmnConstants.BPMN2_BOUNDARY_EVENT, eventDefinition, attrRef));
                refs.addAll(getRefs(id, BpmnConstants.BPMN_INTERMEDIATE_CATCH_EVENT, eventDefinition, attrRef));
                refs.addAll(getRefs(id, BpmnConstants.BPMN_INTERMEDIATE_THROW_EVENT, eventDefinition, attrRef));
                refs.addAll(getRefs(id, BpmnConstants.BPMN_START_EVENT, eventDefinition, attrRef));
                refs.addAll(getRefs(id, BpmnConstants.BPMN_END_EVENT, eventDefinition, attrRef));
                break;
            default:
                break;
        }

        return refs;
    }

    private ArrayList<String> getRefs(String id, String tag, String eventDefinition, String attrRef) {
        ArrayList<String> signalRefs = new ArrayList<>();
        NodeList nodeList;

        nodeList = doc.getElementsByTagName(tag);

        // iterate over list and check each item
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element taskElement = (Element) nodeList.item(i);

            if (id.equals(taskElement.getAttribute(BpmnConstants.ATTR_ID)) && nodeList.item(i).hasChildNodes()) {
                NodeList boundaryChilds = nodeList.item(i).getChildNodes();
                for (int x = 0; x < boundaryChilds.getLength(); x++) {
                    if (boundaryChilds.item(x).getLocalName() != null
                            && boundaryChilds.item(x).getLocalName().equals(eventDefinition)) {
                        Element boundaryChild = (Element) boundaryChilds.item(x);
                        signalRefs.add(boundaryChild.getAttribute(attrRef));
                    }
                }
            }
        }
        return signalRefs;
    }

    /**
     * Retrieve the message name
     *
     * @param messageRef id of message
     * @return messageName
     */
    public String getMessageName(String messageRef) {
        NodeList nodeList = null;
        switch (modelVersion) {
            case V1:
                nodeList = doc.getElementsByTagName(BpmnConstants.MESSAGE);
                break;
            case V2:
                nodeList = doc.getElementsByTagName(BpmnConstants.BPMN_MESSAGE);
                break;
            case V3:
                nodeList = doc.getElementsByTagName(BpmnConstants.BPMN2_MESSAGE);
                break;
            default:
                break;
        }
        return getName(nodeList, messageRef);
    }

    /**
     * Retrieve the signal name
     *
     * @param signalRef id of signal
     * @return signalName
     */
    public String getSignalName(String signalRef) {
        NodeList nodeList = null;
        switch (modelVersion) {
            case V1:
                nodeList = doc.getElementsByTagName(BpmnConstants.SIGNAL);
                break;
            case V2:
                nodeList = doc.getElementsByTagName(BpmnConstants.BPMN_SIGNAL);
                break;
            case V3:
                nodeList = doc.getElementsByTagName(BpmnConstants.BPMN2_SIGNAL);
                break;
            default:
                break;
        }
        return getName(nodeList, signalRef);
    }

    /**
     * return attribute name of element
     *
     * @param nodeList list of nodes to check
     * @param id       element id
     * @return name
     */
    private String getName(NodeList nodeList, String id) {
        String name = "";

        if (nodeList != null) {
            // iterate over list and check each item
            for (int i = 0; i < nodeList.getLength(); i++) {
                Element taskElement = (Element) nodeList.item(i);

                // check if the ids are corresponding
                if (id.equals(taskElement.getAttribute(BpmnConstants.ATTR_ID))) {
                    name = taskElement.getAttribute(BpmnConstants.ATTR_NAME);
                }
            }
        }

        return name;
    }
}
