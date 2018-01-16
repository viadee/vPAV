/**
 * Copyright © 2017, viadee Unternehmensberatung GmbH
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class BPMNScanner {

    private DocumentBuilderFactory factory;

    private DocumentBuilder builder;

    private Document doc;

    private ModelVersionEnum modelVersion;

    private enum ModelVersionEnum {
        V1, V2, V3
    }

    /**
     * The Camunda API's method "getimplementation" doesn't return the correct Implementation, so the we have to scan
     * the xml of the model for the implementation
     *
     ** @param path
     *            path to model
     * @throws ParserConfigurationException
     *             exception if document cant be parsed
     * @throws IOException
     *             Signals that an I/O exception of some sort has occurred
     * @throws SAXException
     *             Encapsulate a general SAX error or warning.
     */
    public BPMNScanner(String path) throws ParserConfigurationException, SAXException, IOException {
        factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        builder = factory.newDocumentBuilder();
        setModelVersion(path);
    }

    /**
     * Checks which camunda namespace is used in a given model and sets the version correspondingly
     *
     * @param path
     * @throws SAXException
     * @throws IOException
     * @throws ParserConfigurationException
     */
    private void setModelVersion(String path) throws SAXException, IOException, ParserConfigurationException {
        // parse the given bpmn model
        doc = builder.parse(path);

        if (doc.getElementsByTagName(BPMNConstants.DEFINITIONS).getLength() > 0)
            modelVersion = ModelVersionEnum.V1;
        else if (doc.getElementsByTagName(BPMNConstants.BPMN_DEFINITIONS).getLength() > 0)
            modelVersion = ModelVersionEnum.V2;
        else if (doc.getElementsByTagName(BPMNConstants.BPMN2_DEFINITIONS).getLength() > 0)
            modelVersion = ModelVersionEnum.V3;
        else
            throw new ParserConfigurationException("Can't get the version of the BPMN Model");
    }

    /**
     * Return the Implementation of an specific element (sendTask, ServiceTask or BusinessRuleTask)
     *
     * @param id
     *            id of bpmn element
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
                listNodeList.add(doc.getElementsByTagName(BPMNConstants.BUSINESSRULETASK));
                listNodeList.add(doc.getElementsByTagName(BPMNConstants.SERVICETASK));
                listNodeList.add(doc.getElementsByTagName(BPMNConstants.SENDTASK));
                listNodeList.add(doc.getElementsByTagName(BPMNConstants.ENDEVENT));
                listNodeList.add(doc.getElementsByTagName(BPMNConstants.INTERMEDIATETHROWEVENT));
                break;
            case V2:
                listNodeList.add(doc.getElementsByTagName(BPMNConstants.BPMN_BUSINESSRULETASK));
                listNodeList.add(doc.getElementsByTagName(BPMNConstants.BPMN_SERVICETASK));
                listNodeList.add(doc.getElementsByTagName(BPMNConstants.BPMN_SENDTASK));
                listNodeList.add(doc.getElementsByTagName(BPMNConstants.BPMN_ENDEVENT));
                listNodeList.add(doc.getElementsByTagName(BPMNConstants.BPMN_INTERMEDIATETHROWEVENT));
                break;
            case V3:
                listNodeList.add(doc.getElementsByTagName(BPMNConstants.BPMN2_BUSINESSRULETASK));
                listNodeList.add(doc.getElementsByTagName(BPMNConstants.BPMN2_SERVICETASK));
                listNodeList.add(doc.getElementsByTagName(BPMNConstants.BPMN2_SENDTASK));
                listNodeList.add(doc.getElementsByTagName(BPMNConstants.BPMN2_ENDEVENT));
                listNodeList.add(doc.getElementsByTagName(BPMNConstants.BPMN2_INTERMEDIATETHROWEVENT));
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
                NamedNodeMap taskElementAttr = taskElement.getAttributes();

                // check if the ids are corresponding
                if (id.equals(taskElement.getAttribute(BPMNConstants.ATTR_ID))) {
                    // check if more than 1 inner attribute exists
                    if (taskElementAttr.getLength() > 1) {
                        // check all attributes, whether they fit an
                        // implementation
                        for (int x = 0; x < taskElementAttr.getLength(); x++) {
                            Node attr = taskElementAttr.item(x);
                            // node_name equals an implementation
                            nodeName = attr.getNodeName();
                            if (nodeName.equals(BPMNConstants.CAMUNDA_CLASS)
                                    || nodeName.equals(BPMNConstants.CAMUNDA_EXPRESSION)
                                    || nodeName.equals(BPMNConstants.CAMUNDA_DEXPRESSION)
                                    || nodeName.equals(BPMNConstants.CAMUNDA_DMN)
                                    || nodeName.equals(BPMNConstants.CAMUNDA_EXT)) {
                                returnImplementation = nodeName;
                            }
                        }
                        // if inner attributes dont consist of implementations
                    }
                    if (taskElementAttr.getNamedItem(BPMNConstants.CAMUNDA_CLASS) == null
                            && taskElementAttr.getNamedItem(BPMNConstants.CAMUNDA_EXPRESSION) == null
                            && taskElementAttr.getNamedItem(BPMNConstants.CAMUNDA_DEXPRESSION) == null
                            && taskElementAttr.getNamedItem(BPMNConstants.CAMUNDA_DMN) == null
                            && taskElementAttr.getNamedItem(BPMNConstants.CAMUNDA_EXT) == null) {
                        returnImplementation = BPMNConstants.IMPLEMENTATION;
                    }
                }
            }
        }
        return returnImplementation;
    }

    /**
     *
     * @param id
     *            id of bpmnElement
     * @param implementation
     *            DelegateExpression/Java Class
     * @return implementationReference
     */
    public String getImplementationReference(String id, String implementation) {
        String implementationReference = "";

        // List for all Task elements
        ArrayList<NodeList> listNodeList = new ArrayList<NodeList>();

        switch (modelVersion) {
            case V1:
                listNodeList.add(doc.getElementsByTagName(BPMNConstants.BUSINESSRULETASK));
                listNodeList.add(doc.getElementsByTagName(BPMNConstants.SERVICETASK));
                listNodeList.add(doc.getElementsByTagName(BPMNConstants.SENDTASK));
                break;
            case V2:
                listNodeList.add(doc.getElementsByTagName(BPMNConstants.BPMN_BUSINESSRULETASK));
                listNodeList.add(doc.getElementsByTagName(BPMNConstants.BPMN_SERVICETASK));
                listNodeList.add(doc.getElementsByTagName(BPMNConstants.BPMN_SENDTASK));
                break;
            case V3:
                listNodeList.add(doc.getElementsByTagName(BPMNConstants.BPMN2_BUSINESSRULETASK));
                listNodeList.add(doc.getElementsByTagName(BPMNConstants.BPMN2_SERVICETASK));
                listNodeList.add(doc.getElementsByTagName(BPMNConstants.BPMN2_SENDTASK));
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
                if (id.equals(taskElement.getAttribute(BPMNConstants.ATTR_ID))) {
                    // check for implementation reference
                    if (implementation.equals(BPMNConstants.CAMUNDA_CLASS)) {
                        implementationReference = taskElement.getAttribute(BPMNConstants.CAMUNDA_CLASS);
                    } else if (implementation.equals(BPMNConstants.CAMUNDA_DEXPRESSION)) {
                        implementationReference = taskElement.getAttribute(BPMNConstants.CAMUNDA_DEXPRESSION);
                    }
                }
            }
        }

        return implementationReference;
    }

    /**
     * Return the Implementation of an specific element (endEvent and/or intermediateThrowEvent)
     *
     * @param id
     *            id of bpmn element
     * @return return_implementation contains implementation
     */
    public String getEventImplementation(String id) {
        // List to hold return values
        String returnImplementation = null;

        // List for all Task elements
        ArrayList<NodeList> listNodeList = new ArrayList<NodeList>();

        switch (modelVersion) {
            case V1:
                listNodeList.add(doc.getElementsByTagName(BPMNConstants.ENDEVENT));
                listNodeList.add(doc.getElementsByTagName(BPMNConstants.INTERMEDIATETHROWEVENT));
                break;
            case V2:
                listNodeList.add(doc.getElementsByTagName(BPMNConstants.BPMN_ENDEVENT));
                listNodeList.add(doc.getElementsByTagName(BPMNConstants.BPMN_INTERMEDIATETHROWEVENT));
                break;
            case V3:
                listNodeList.add(doc.getElementsByTagName(BPMNConstants.BPMN2_ENDEVENT));
                listNodeList.add(doc.getElementsByTagName(BPMNConstants.BPMN2_INTERMEDIATETHROWEVENT));
                break;
            default:
                listNodeList = null;
        }

        // iterate over list<NodeList> and check each NodeList (endEvent, intermediateThrowEvent)
        for (final NodeList list : listNodeList) {
            // iterate over list and check child of each node
            for (int i = 0; i < list.getLength(); i++) {
                final Element taskElement = (Element) list.item(i);

                // check if the ids are corresponding
                if (id.equals(taskElement.getAttribute(BPMNConstants.ATTR_ID))) {

                    final NodeList childNodes = taskElement.getChildNodes();

                    // check all attributes, whether they equal a messageEventDefinition
                    for (int x = 0; x < childNodes.getLength(); x++) {
                        if (childNodes.item(x).getLocalName() != null
                                && childNodes.item(x).getLocalName().equals(BPMNConstants.MESSAGEEVENTDEFINITION)) {
                            final Element event = (Element) childNodes.item(x);

                            // if the node messageEventDefinition contains the camunda expression -> return
                            if (event.getAttributeNode(BPMNConstants.CAMUNDA_EXPRESSION) != null) {
                                returnImplementation = event.getAttributeNode(BPMNConstants.CAMUNDA_EXPRESSION)
                                        .toString();
                            } else if (event.getAttributeNode(BPMNConstants.CAMUNDA_DEXPRESSION) != null) {
                                returnImplementation = event.getAttributeNode(BPMNConstants.CAMUNDA_DEXPRESSION)
                                        .toString();
                            } else if (event.getAttributeNode(BPMNConstants.CAMUNDA_CLASS) != null) {
                                returnImplementation = event.getAttributeNode(BPMNConstants.CAMUNDA_CLASS).toString();
                            } else if (event.getAttributeNode(BPMNConstants.CAMUNDA_EXT) != null) {
                                returnImplementation = event.getAttributeNode(BPMNConstants.CAMUNDA_EXT).toString();
                            }

                            if (event.getAttributeNode(BPMNConstants.CAMUNDA_DEXPRESSION) == null
                                    && event.getAttributeNode(BPMNConstants.CAMUNDA_EXPRESSION) == null
                                    && event.getAttributeNode(BPMNConstants.CAMUNDA_CLASS) == null
                                    && event.getAttributeNode(BPMNConstants.CAMUNDA_EXT) == null) {
                                returnImplementation = BPMNConstants.IMPLEMENTATION;
                            }
                        }
                    }
                }
            }
        }
        return returnImplementation;
    }

    /**
     *
     * @param id
     *            id of bpmn element
     * @param listType
     *            Type of Attribute
     * @param extType
     *            Type of Listener
     * @return value of Listener
     */
    public ArrayList<String> getListener(String id, String listType, String extType) {

        // list to hold return values
        ArrayList<String> returnAttrList = new ArrayList<String>();

        // List for all Task elements
        NodeList nodeListExtensionElements;

        // search for script tag

        switch (modelVersion) {
            case V1:
                nodeListExtensionElements = doc.getElementsByTagName(BPMNConstants.EXTELEMENTS);
                break;
            case V2:
                nodeListExtensionElements = doc.getElementsByTagName(BPMNConstants.BPMN_EXTELEMENTS);
                break;
            case V3:
                nodeListExtensionElements = doc.getElementsByTagName(BPMNConstants.BPMN2_EXTELEMENTS);
                break;
            default:
                nodeListExtensionElements = null;
        }

        // search for parent with id
        for (int i = 0; i < nodeListExtensionElements.getLength(); i++) {
            if (((Element) nodeListExtensionElements.item(i).getParentNode()).getAttribute(BPMNConstants.ATTR_ID)
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
     *
     * @param node
     *            node to check
     * @param listType
     *            Type of ExecutionListener
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
     * @param id
     *            id of bpmn element
     * @return scriptPlaces contains script type
     */
    public ArrayList<String> getScriptTypes(String id) {
        // bool to hold return values
        ArrayList<String> returnScriptType = new ArrayList<String>();

        // List for all Task elements
        NodeList nodeList;

        // search for script tag
        nodeList = doc.getElementsByTagName(BPMNConstants.SCRIPT_TAG);

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
     * Check if any parentnode has the specific id
     *
     * @param n
     *            Node to check their parents
     * @param id
     *            id to check
     * @return true if id was found
     */
    private boolean idMatch(Node n, String id) {
        Element e = (Element) n;

        if (e.getAttribute(BPMNConstants.ATTR_ID).equals(id))
            return true;

        while (e.getParentNode() != null && !e.getParentNode().getLocalName().equals(BPMNConstants.PROCESS)) {
            Element check = (Element) e.getParentNode();
            if (check.getAttribute(BPMNConstants.ATTR_ID).equals(id)) {
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
     * @param id
     *            id of the element
     * @return boolean has condition Expression
     */
    public boolean hasScriptInCondExp(String id) {
        // List for all Task elements
        NodeList nodeList = null;

        switch (modelVersion) {
            case V1:
                // create nodelist that contains all Tasks with the namespace
                nodeList = doc.getElementsByTagName(BPMNConstants.SEQUENCE);
                break;
            case V2:
                nodeList = doc.getElementsByTagName(BPMNConstants.BPMN_SEQUENCE);
                break;
            case V3:
                nodeList = doc.getElementsByTagName(BPMNConstants.BPMN2_SEQUENCE);
                break;
        }

        for (int i = 0; i < nodeList.getLength(); i++) {
            Element sequenceElement = (Element) nodeList.item(i);
            if (sequenceElement.getAttribute(BPMNConstants.ATTR_ID).equals(id)) {
                return hasCondExp(sequenceElement);
            }
        }

        return false;
    }

    /**
     * check if sequenceFlow has an Script (value in language attribute) in conditionalExpression
     *
     * @param sq
     *            sequenceFlowNode
     * @return true or false
     */
    private boolean hasCondExp(Element sq) {
        NodeList childNodes = null;
        if (sq.hasChildNodes()) {
            childNodes = sq.getChildNodes();
            for (int i = 0; i < childNodes.getLength(); i++) {
                Node childNode = childNodes.item(i);
                if (childNode.getLocalName() != null && childNode.getLocalName().equals(BPMNConstants.CONDEXP)) {
                    Element childElement = (Element) childNode;
                    if (childElement.getAttribute(BPMNConstants.LANG).trim().length() > 0)
                        return true;
                }
            }
        }
        return false;
    }

    /**
     * Return a list of used gateways for a given bpmn model
     *
     * @param id
     *            id of bpmn element
     * @return gateway contains script type
     *
     */
    public String getXorGateWays(String id) {
        final NodeList nodeList;

        String gateway = "";

        switch (modelVersion) {
            case V1:
                nodeList = doc.getElementsByTagName(BPMNConstants.GATEWAY);
                break;
            case V2:
                nodeList = doc.getElementsByTagName(BPMNConstants.BPMN_GATEWAY);
                break;
            case V3:
                nodeList = doc.getElementsByTagName(BPMNConstants.BPMN2_GATEWAY);
                break;
            default:
                return "";
        }

        // iterate over list and check each item
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element taskElement = (Element) nodeList.item(i);

            // check if the ids are corresponding
            if (id.equals(taskElement.getAttribute(BPMNConstants.ATTR_ID))) {
                gateway = taskElement.getAttribute(BPMNConstants.ATTR_ID);
            }
        }
        return gateway;
    }

    /**
     * Return number of outgoing
     *
     * @param id
     *            id of bpmn element
     * @return outgoing number of outgoing
     */
    public int getOutgoing(String id) {
        final NodeList nodeList;
        String out = "";
        int outgoing = 0;

        switch (modelVersion) {
            case V1:
                // create nodelist that contains all Tasks with the namespace
                nodeList = doc.getElementsByTagName(BPMNConstants.GATEWAY);
                out = BPMNConstants.OUT;
                break;
            case V2:
                nodeList = doc.getElementsByTagName(BPMNConstants.BPMN_GATEWAY);
                out = BPMNConstants.BPMN_OUT;
                break;
            case V3:
                nodeList = doc.getElementsByTagName(BPMNConstants.BPMN2_GATEWAY);
                out = BPMNConstants.BPMN2_OUT;
                break;
            default:
                return -1;
        }

        // iterate over list and check each item
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element taskElement = (Element) nodeList.item(i);

            // check if the ids are corresponding
            if (id.equals(taskElement.getAttribute(BPMNConstants.ATTR_ID))) {
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
     * @param id
     *            id of bpmn element
     * @return ArrayList of outgoing Nodes
     */
    public ArrayList<String> getSequenceFlowDef(String id) {

        final ArrayList<String> references = new ArrayList<>();

        NodeList nodeList = null;

        switch (modelVersion) {
            case V1:
                nodeList = doc.getElementsByTagName(BPMNConstants.SEQUENCE);
                break;
            case V2:
                nodeList = doc.getElementsByTagName(BPMNConstants.BPMN_SEQUENCE);
                break;
            case V3:
                nodeList = doc.getElementsByTagName(BPMNConstants.BPMN2_SEQUENCE);
                break;
        }

        for (int i = 0; i < nodeList.getLength(); i++) {
            Element taskElement = (Element) nodeList.item(i);

            // check if the ids are corresponding and retrieve the attributes for target and source reference
            if (id.equals(taskElement.getAttribute(BPMNConstants.ATTR_ID))) {
                references.add(taskElement.getAttribute(BPMNConstants.SOURCEREF));
                references.add(taskElement.getAttribute(BPMNConstants.TARGETREF));
            }
        }
        return references;
    }

    /**
     * check xor gateways for outgoing edges
     *
     * @param id
     *            id of bpmn element
     * @return ArrayList of outgoing Nodes
     */
    public ArrayList<Node> getOutgoingEdges(String id) {

        ArrayList<Node> outgoingEdges = new ArrayList<Node>();
        NodeList nodeList = null;
        String out = "";

        switch (modelVersion) {
            case V1:
                // create nodelist that contains all Tasks with the namespace
                nodeList = doc.getElementsByTagName(BPMNConstants.GATEWAY);
                out = BPMNConstants.OUT;
                break;
            case V2:
                nodeList = doc.getElementsByTagName(BPMNConstants.BPMN_GATEWAY);
                out = BPMNConstants.BPMN_OUT;
                break;
            case V3:
                nodeList = doc.getElementsByTagName(BPMNConstants.BPMN2_GATEWAY);
                out = BPMNConstants.BPMN2_OUT;
                break;
        }

        for (int i = 0; i < nodeList.getLength(); i++) {
            Element taskElement = (Element) nodeList.item(i);

            // check if the ids are corresponding and retrieve the outgoing edges of the xor gateway
            if (id.equals(taskElement.getAttribute(BPMNConstants.ATTR_ID))) {
                NodeList children = taskElement.getChildNodes();
                for (int j = 0; j < children.getLength(); j++) {
                    if (children.item(j).getNodeName().equals(out)) {
                        outgoingEdges.add(checkNamingOfEdges(children.item(j).getTextContent()));
                    }
                }
            }
        }
        return outgoingEdges;
    }

    /**
     * check xor gateways for outgoing edges
     *
     * @param id
     *            id of edge
     * @return edge
     */
    public Node checkNamingOfEdges(String id) {

        Node edge = null;
        NodeList nodeList = null;

        switch (modelVersion) {
            case V1:
                // create nodelist that contains all Tasks with the namespace
                nodeList = doc.getElementsByTagName(BPMNConstants.SEQUENCE);
                break;
            case V2:
                nodeList = doc.getElementsByTagName(BPMNConstants.BPMN_SEQUENCE);
                break;
            case V3:
                nodeList = doc.getElementsByTagName(BPMNConstants.BPMN2_SEQUENCE);
                break;
        }

        for (int i = 0; i < nodeList.getLength(); i++) {
            Element taskElement = (Element) nodeList.item(i);
            if (taskElement.getAttribute(BPMNConstants.ATTR_ID).equals(id)) {
                edge = taskElement;
            }
        }
        return edge;
    }

    /**
     * get ids and timer definition for all timer event types
     *
     * @param id
     *            id of bpmn element
     * @return Map with timerEventDefinition-Node and his child
     */
    public Map<Element, Element> getTimerImplementation(final String id) {

        // List for all Task elements
        ArrayList<NodeList> listNodeList = new ArrayList<NodeList>();

        switch (modelVersion) {
            case V1:
                listNodeList.add(doc.getElementsByTagName(BPMNConstants.STARTEVENT));
                listNodeList.add(doc.getElementsByTagName(BPMNConstants.INTERMEDIATECATCHEVENT));
                listNodeList.add(doc.getElementsByTagName(BPMNConstants.BOUNDARYEVENT));
                break;
            case V2:
                listNodeList.add(doc.getElementsByTagName(BPMNConstants.BPMN_STARTEVENT));
                listNodeList.add(doc.getElementsByTagName(BPMNConstants.BPMN_INTERMEDIATECATCHEVENT));
                listNodeList.add(doc.getElementsByTagName(BPMNConstants.BPMN_BOUNDARYEVENT));
                break;
            case V3:
                listNodeList.add(doc.getElementsByTagName(BPMNConstants.BPMN2_STARTEVENT));
                listNodeList.add(doc.getElementsByTagName(BPMNConstants.BPMN2_INTERMEDIATECATCHEVENT));
                listNodeList.add(doc.getElementsByTagName(BPMNConstants.BPMN2_BOUNDARYEVENT));
                break;
            default:
                listNodeList = null;
        }

        // final ArrayList<Element> timerList = new ArrayList<>();
        final Map<Element, Element> timerList = new HashMap<>();

        // iterate over list<NodeList>
        for (final NodeList list : listNodeList) {
            for (int i = 0; i < list.getLength(); i++) {
                final Element taskElement = (Element) list.item(i);

                // check whether a node matches with the provided id
                if (taskElement.getAttribute(BPMNConstants.ATTR_ID).equals(id)) {

                    final NodeList childNodes = taskElement.getChildNodes();
                    for (int x = 0; x < childNodes.getLength(); x++) {

                        // check if an event consists of a timereventdefinition tag
                        if (childNodes.item(x).getLocalName() != null
                                && childNodes.item(x).getLocalName().equals(BPMNConstants.TIMEREVENTDEFINTION)) {

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
     * Retrieve the message name of a given receiveTask
     *
     * @param messageRef
     *            id of message
     * @return messageName
     */
    public String getMessageName(String messageRef) {
        // List for all messages
        ArrayList<NodeList> listNodeList = new ArrayList<NodeList>();
        String messageName = "";

        switch (modelVersion) {
            case V1:
                listNodeList.add(doc.getElementsByTagName(BPMNConstants.MESSAGE));
                break;
            case V2:
                listNodeList.add(doc.getElementsByTagName(BPMNConstants.BPMN_MESSAGE));
                break;
            case V3:
                listNodeList.add(doc.getElementsByTagName(BPMNConstants.BPMN2_MESSAGE));
                break;
            default:
                listNodeList = null;
        }

        for (NodeList list : listNodeList) {
            for (int i = 0; i < list.getLength(); i++) {

                final Element taskElement = (Element) list.item(i);

                // check whether a node matches with the provided id
                if (taskElement.getAttribute(BPMNConstants.ATTR_ID).equals(messageRef)) {
                    messageName = taskElement.getAttribute(BPMNConstants.ATTR_NAME);
                }
            }
        }
        return messageName;

    }

    /**
     * get List of output variables
     *
     * @param id
     *            id of the element
     * @return outputVariables
     */
    private ArrayList<String> getInOutVariables(String id, String inOut) {
        // List for all Task elements
        ArrayList<String> listVariables = new ArrayList<String>();

        NodeList nodeList = doc.getElementsByTagName(inOut);

        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (idMatch(node, id)) {
                listVariables.add(((Element) node).getAttribute(BPMNConstants.ATTR_NAME));
            }
        }
        return listVariables;
    }

    public ArrayList<String> getOutputVariables(String id) {
        return getInOutVariables(id, BPMNConstants.CAMUNDA_OUTPAR);
    }

    /**
     * get Values of outputParameters
     *
     * @param id
     *            element id
     * @return ArrayList of String
     */
    public ArrayList<String> getInOutputVariablesValue(String id) {
        ArrayList<String> variableValues = getInOutputVariablesValue(id, BPMNConstants.CAMUNDA_INPAR);
        variableValues.addAll(getInOutputVariablesValue(id, BPMNConstants.CAMUNDA_OUTPAR));
        return variableValues;
    }

    /**
     * get List of in or Out variables value
     *
     * @param id
     *            id of the element
     * @param inOut
     *            tagname to search value
     * @return
     */
    private ArrayList<String> getInOutputVariablesValue(String id, String inOut) {
        // List for all Task elements
        ArrayList<String> listVariablesValue = new ArrayList<String>();

        NodeList nodeList = doc.getElementsByTagName(inOut);

        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (idMatch(node, id)) {
                // if more than one child, check on list and value
                if (node.hasChildNodes() && node.getChildNodes().getLength() > 1) {
                    NodeList nodeChilds = node.getChildNodes();
                    for (int y = 0; y < nodeChilds.getLength(); y++) {
                        if (nodeChilds.item(y).getNodeName().equals(BPMNConstants.CAMUNDA_LIST)) {
                            NodeList listChilds = nodeChilds.item(y).getChildNodes();
                            for (int z = 0; z < listChilds.getLength(); z++) {
                                if (listChilds.item(z).getNodeName().equals(BPMNConstants.CAMUNDA_VALUE)) {
                                    listVariablesValue.add(listChilds.item(z).getTextContent());
                                }
                            }
                        } else if (nodeChilds.item(y).getNodeName().equals(BPMNConstants.CAMUNDA_MAP)) {
                            NodeList mapChilds = nodeChilds.item(y).getChildNodes();
                            for (int x = 0; x < mapChilds.getLength(); x++) {
                                if (mapChilds.item(x).getNodeName().equals(BPMNConstants.CAMUNDA_ENTRY)) {
                                    listVariablesValue.add(mapChilds.item(x).getTextContent());
                                }
                            }
                        }
                    }
                } else {
                    listVariablesValue.add(node.getTextContent());
                }
            }
        }
        return listVariablesValue;
    }

    /**
     * get List of input variables
     *
     * @param id
     *            id of the element
     * @return inputVariables
     */
    public ArrayList<String> getInputVariables(String id) {
        return getInOutVariables(id, BPMNConstants.CAMUNDA_INPAR);
    }

    /**
     * get value of expression
     *
     * @param id
     *            id from element
     * @return value of expression
     */
    public ArrayList<String> getFieldInjectionExpression(String id) {
        ArrayList<String> varNames = new ArrayList<String>();
        NodeList nodeList = doc.getElementsByTagName(BPMNConstants.CAMUNDA_FIELD);
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (idMatch(node, id)) {
                for (int y = 0; y < node.getChildNodes().getLength(); y++) {
                    if (node.getChildNodes().item(y).getNodeName().equals(BPMNConstants.CAMUNDA_EXPRESSION)) {
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
     * @param id
     *            id from element
     * @return names of variable
     */
    public ArrayList<String> getFieldInjectionVarName(String id) {
        ArrayList<String> varNames = new ArrayList<String>();
        NodeList nodeList = doc.getElementsByTagName(BPMNConstants.CAMUNDA_FIELD);
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (idMatch(node, id))
                for (int y = 0; y < node.getAttributes().getLength(); y++) {
                    if (node.getAttributes().item(y).getNodeName().equals(BPMNConstants.ATTR_NAME))
                        varNames.add(node.getAttributes().item(y).getNodeValue());
                }
        }
        return varNames;
    }

    /**
     * get errorEventDefinition
     *
     * @param id
     *            id from element
     * @return errorEvent
     */
    public Map<String, String> getErrorEvent(String id) {

        // List for all Task elements
        ArrayList<NodeList> listNodeList = new ArrayList<NodeList>();

        switch (modelVersion) {
            case V1:
                listNodeList.add(doc.getElementsByTagName(BPMNConstants.BOUNDARYEVENT));
                break;
            case V2:
                listNodeList.add(doc.getElementsByTagName(BPMNConstants.BPMN_BOUNDARYEVENT));
                break;
            case V3:
                listNodeList.add(doc.getElementsByTagName(BPMNConstants.BPMN2_BOUNDARYEVENT));
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
                if (taskElement.getAttribute(BPMNConstants.ATTR_ID).equals(id)) {

                    final NodeList childNodes = taskElement.getChildNodes();
                    for (int x = 0; x < childNodes.getLength(); x++) {

                        // check if an event consists of a errorEventDefinition tag
                        if (childNodes.item(x).getLocalName() != null
                                && childNodes.item(x).getLocalName().equals(BPMNConstants.ERROREVENTDEFINITION)) {
                            final Element taskElement2 = (Element) childNodes.item(x);
                            boundaryEventList.put(taskElement2.getAttribute(BPMNConstants.ATTR_ERRORREF),
                                    taskElement2.getAttribute(BPMNConstants.CAMUNDA_ERRORCODEMESSVAR));

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
     * @param id
     *            id from element
     * @return Map with errorName and errorCode
     */
    public Map<String, String> getErrorDef(String id) {

        NodeList nodeList = null;

        switch (modelVersion) {
            case V1:
                nodeList = doc.getElementsByTagName(BPMNConstants.ERROR);
                break;
            case V2:
                nodeList = doc.getElementsByTagName(BPMNConstants.BPMN_ERROR);
                break;
            case V3:
                nodeList = doc.getElementsByTagName(BPMNConstants.BPMN2_ERROR);
                break;
            default:
                break;
        }

        final Map<String, String> errorDef = new HashMap<String, String>();

        // iterate over list and check each item
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element taskElement = (Element) nodeList.item(i);

            // check if the ids are corresponding
            if (id.equals(taskElement.getAttribute(BPMNConstants.ATTR_ID))) {
                errorDef.put(taskElement.getAttribute(BPMNConstants.ATTR_NAME),
                        taskElement.getAttribute(BPMNConstants.ATTR_ERRORCODE));
            }
        }
        return errorDef;
    }

    /**
     * get errorCodeVariable
     *
     * @param id
     *            id from element
     * @return String with errorCodeVariable
     */
    public String getErrorCodeVar(String id) {
        // List for all Task elements
        ArrayList<NodeList> listNodeList = new ArrayList<NodeList>();

        switch (modelVersion) {
            case V1:
                listNodeList.add(doc.getElementsByTagName(BPMNConstants.BOUNDARYEVENT));
                break;
            case V2:
                listNodeList.add(doc.getElementsByTagName(BPMNConstants.BPMN_BOUNDARYEVENT));
                break;
            case V3:
                listNodeList.add(doc.getElementsByTagName(BPMNConstants.BPMN2_BOUNDARYEVENT));
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
                if (taskElement.getAttribute(BPMNConstants.ATTR_ID).equals(id)) {

                    final NodeList childNodes = taskElement.getChildNodes();
                    for (int x = 0; x < childNodes.getLength(); x++) {

                        // check if an event consists of a errorEventDefinition tag
                        if (childNodes.item(x).getLocalName() != null
                                && childNodes.item(x).getLocalName().equals(BPMNConstants.ERROREVENTDEFINITION)) {
                            final Element taskElement2 = (Element) childNodes.item(x);
                            getErrorCodeVar = taskElement2.getAttribute(BPMNConstants.CAMUNDA_ERRORCODEVAR);
                        }
                    }
                }
            }
        }
        return getErrorCodeVar;
    }

    /**
     *
     * @param id
     *            id of boundaryErrorEvent
     * @return attachedToTask
     */
    public String getErrorEventMapping(String id) {

        String attachedToTask = "";
        NodeList nodeList = null;

        switch (modelVersion) {
            case V1:
                nodeList = doc.getElementsByTagName(BPMNConstants.BOUNDARYEVENT);
                break;
            case V2:
                nodeList = doc.getElementsByTagName(BPMNConstants.BPMN_BOUNDARYEVENT);
                break;
            case V3:
                nodeList = doc.getElementsByTagName(BPMNConstants.BPMN2_BOUNDARYEVENT);
                break;
            default:
                break;
        }

        // iterate over list and check each item
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element taskElement = (Element) nodeList.item(i);

            // check if the ids are corresponding
            if (id.equals(taskElement.getAttribute(BPMNConstants.ATTR_ID))) {
                attachedToTask = taskElement.getAttribute(BPMNConstants.ATTACHED_TO_REF);
            }
        }

        return attachedToTask;
    }

    /**
     *
     * @param id
     *            id of bpmn element
     * @return map with key value pair of given element
     */
    public Map<String, String> getKeyPairs(final String id) {

        final Map<String, String> keyPairs = new HashMap<String, String>();

        final NodeList nodeList = doc.getElementsByTagName(BPMNConstants.CAMUNDA_PROPERTY);

        for (int i = 0; i < nodeList.getLength(); i++) {

            // Due to the static nesting of nodes, we can check the third parent node whether the id are corresponding
            Element parent_element = (Element) nodeList.item(i).getParentNode().getParentNode().getParentNode();
            if (parent_element.getAttribute(BPMNConstants.ATTR_ID).equals(id)) {
                Element extension_node = (Element) nodeList.item(i);
                keyPairs.put(extension_node.getAttribute(BPMNConstants.ATTR_NAME),
                        extension_node.getAttribute(BPMNConstants.ATTR_VALUE));
            }
        }

        return keyPairs;
    }
}
