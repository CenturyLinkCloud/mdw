/*
 * Copyright (C) 2018 CenturyLink, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.centurylink.mdw.bpmn;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.centurylink.mdw.constant.WorkAttributeConstant;
import com.centurylink.mdw.constant.WorkTransitionAttributeConstant;
import com.centurylink.mdw.model.variable.Variable;
import com.centurylink.mdw.model.workflow.Activity;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.model.workflow.TextNote;
import com.centurylink.mdw.model.workflow.Transition;

public class BpmnImportHelper {

    private static final Map<String, String> activityMap = new HashMap<>();

    static {
        activityMap.put("startEvent",
                "com.centurylink.mdw.workflow.activity.process.ProcessStartActivity");
        activityMap.put("endEvent",
                "com.centurylink.mdw.workflow.activity.process.ProcessFinishActivity");
        activityMap.put("exclusiveGateway",
                "com.centurylink.mdw.workflow.activity.script.ScriptEvaluator");
        activityMap.put("scriptTask",
                "com.centurylink.mdw.workflow.activity.script.ScriptExecutorActivity");
        activityMap.put("subProcess",
                "com.centurylink.mdw.workflow.activity.process.InvokeSubProcessActivity");
        activityMap.put("userTask",
                "com.centurylink.mdw.workflow.activity.task.AutoFormManualTaskActivity");
        activityMap.put("intermediateCatchEvent",
                "com.centurylink.mdw.workflow.activity.event.EventWaitActivity");
        activityMap.put("businessRuleTask", "com.centurylink.mdw.drools.DroolsActivity");
        activityMap.put("parallelGateway",
                "com.centurylink.mdw.workflow.activity.sync.SynchronizationActivity");
        activityMap.put("intermediateThrowEvent",
                "com.centurylink.mdw.workflow.activity.event.PublishEventMessage");
        activityMap.put("serviceTask",
                "com.centurylink.mdw.workflow.adapter.rest.RestServiceAdapter");
        activityMap.put("callActivity",
                "com.centurylink.mdw.workflow.activity.process.InvokeHeterogeneousProcessActivity");
    }

    public String importProcess(File process) throws IOException {
        Process proc = new Process();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder;
            builder = factory.newDocumentBuilder();
            Document document = builder.parse(
                    new ByteArrayInputStream(Files.readAllBytes(Paths.get(process.getPath()))));
            proc.setSubprocesses(new ArrayList<Process>());
            Node procNode = getNode(document.getDocumentElement(), "process");
            if (procNode != null)
                parseProcessData(document.getDocumentElement(), procNode, proc);
        }
        catch (ParserConfigurationException e) {
            System.err.println("unable to import process ---" + process.getPath());
        }
        catch (SAXException e) {
            System.err.println("unable to import process ---" + process.getPath());
        }
        return proc.getJson().toString(2);
    }

    private void parseProcessData(Element element, Node node, Process proc) {
        parseActivities(element, node, proc);
        Node procExtNode = getNode(node, "mdw:ProcessExtensions");
        if (procExtNode != null)
            parseProcessMetaData(procExtNode, proc);
        Node bpmnPlaneNode = getNode(element, "di:BPMNPlane");
        if (bpmnPlaneNode != null)
            parseDiagramData(bpmnPlaneNode, proc);
    }

    private Node getNode(Node node, String tag) {
        if (Node.ELEMENT_NODE == node.getNodeType() && tag.equals(node.getNodeName()))
            return node;
        NodeList nodeList = node.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node temp = getNode(nodeList.item(i), tag);
            if (temp != null)
                return temp;
        }
        return null;
    }

    private void parseActivities(Element element, Node node, Process proc) {
        NodeList nodeList = node.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node cNode = nodeList.item(i);
            if (Node.ELEMENT_NODE == cNode.getNodeType()) {
                if (activityMap.containsKey(cNode.getNodeName()))
                    if ("subProcess".equals(cNode.getNodeName())
                            && getNode(cNode, "startEvent") != null) {
                        parseSubprocesses(element, cNode, proc);
                    }
                    else
                        parseActivityData(cNode, proc);
                else if ("sequenceFlow".equals(cNode.getNodeName()))
                    parseSequenceFlowData(cNode, proc);
            }
        }
    }

    private void parseSubprocesses(Element element, Node node, Process proc) {
        Process subProc = new Process();
        subProc.setSubprocesses(new ArrayList<Process>());
        subProc.setName(parseAttributeValue(node, "name"));
        parseProcessData(element, node, subProc);
        proc.getSubprocesses().add(subProc);
    }

    private void parseActivityData(Node node, Process proc) {
        if (proc.getActivities() == null)
            proc.setActivities(new ArrayList<Activity>());
        Activity act = new Activity();
        proc.getActivities().add(act);
        act.setId(Long.parseLong(parseAttributeValue(node, "id").substring(1)));
        act.setAttribute(WorkAttributeConstant.LOGICAL_ID, parseAttributeValue(node, "id"));
        act.setName(parseAttributeValue(node, "name"));
        Node attrNode = getNode(node, "mdw:Attributes");
        if (attrNode != null) {
            NodeList nodeactivities = attrNode.getChildNodes();
            for (int i = 0; i < nodeactivities.getLength(); i++) {
                Node actNode = nodeactivities.item(i);
                if (Node.ELEMENT_NODE == actNode.getNodeType()) {
                    String attrName = parseAttributeValue(actNode, "name");
                    if ("Implementor".equals(attrName))
                        act.setImplementor(actNode.getTextContent());
                    else
                        act.setAttribute(parseAttributeValue(actNode, "name"),
                                actNode.getTextContent());
                }
            }
        }
        if (act.getImplementor() == null)
            act.setImplementor(activityMap.get(node.getNodeName()));
    }

    private void parseSequenceFlowData(Node node, Process proc) {
        if (proc.getTransitions() == null)
            proc.setTransitions(new ArrayList<Transition>());
        Transition trans = new Transition();
        String seqId = parseAttributeValue(node, "id");
        trans.setId(Long.parseLong(seqId.substring(1)));
        trans.setAttribute(WorkAttributeConstant.LOGICAL_ID, seqId);
        trans.setFromId(Long.parseLong(parseAttributeValue(node, "sourceRef").substring(1)));
        trans.setToId(Long.parseLong(parseAttributeValue(node, "targetRef").substring(1)));
        String resCode = parseAttributeValue(node, "name");
        if (!resCode.isEmpty())
            trans.setCompletionCode(resCode);
        proc.getTransitions().add(trans);
        Node attrNode = getNode(node, "mdw:Attributes");
        if (attrNode != null) {
            NodeList attrs = attrNode.getChildNodes();
            for (int i = 0; i < attrs.getLength(); i++) {
                Node attr = attrs.item(i);
                if (Node.ELEMENT_NODE == attr.getNodeType()) {
                    String attrName = parseAttributeValue(attr, "name");
                    if ("Event".equals(attrName))
                        trans.setEventType(Integer.parseInt(attr.getTextContent()));
                    else
                        trans.setAttribute(attrName, attr.getTextContent());
                }
            }
        }
    }

    private void parseProcessMetaData(Node node, Process proc) {
        NodeList nodeList = node.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node cNode = nodeList.item(i);
            if (Node.ELEMENT_NODE == cNode.getNodeType()) {
                if ("mdw:Attribute".equals(cNode.getNodeName()))
                    proc.setAttribute(parseAttributeValue(cNode, "name"), cNode.getTextContent());
                else if ("mdw:Variable".equals(cNode.getNodeName()))
                    parseVaraibleData(cNode, proc);
                else if ("mdw:TextNote".equals(cNode.getNodeName()))
                    parseTextNotesData(cNode, proc);
            }
        }
    }

    private void parseVaraibleData(Node node, Process proc) {
        if (proc.getVariables() == null)
            proc.setVariables(new ArrayList<Variable>());
        Variable var = new Variable();
        var.setName(parseAttributeValue(node, "name"));
        var.setDisplaySequence(Integer.parseInt(parseAttributeValue(node, "dispaySequence")));
        var.setVariableCategory(var.getCategoryCode(parseAttributeValue(node, "category")));
        var.setType(parseAttributeValue(getNode(node, "mdw:type"), "name"));
        proc.getVariables().add(var);
    }

    private void parseTextNotesData(Node node, Process proc) {
        if (proc.getTextNotes() == null)
            proc.setTextNotes(new ArrayList<TextNote>());
        TextNote notes = new TextNote();
        notes.setContent(parseAttributeValue(node, "content"));
        NodeList nodeList = node.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node cNode = nodeList.item(i);
            if (Node.ELEMENT_NODE == cNode.getNodeType()) {
                notes.setAttribute(parseAttributeValue(cNode, "name"),
                        cNode.getTextContent().trim());
            }
        }
        proc.getTextNotes().add(notes);
    }

    private String parseAttributeValue(Node node, String attrName) {
        return node.getAttributes().getNamedItem(attrName).getNodeValue();
    }

    private void parseDiagramData(Node node, Process proc) {
        NodeList nodeList = node.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node cNode = nodeList.item(i);
            if (Node.ELEMENT_NODE == cNode.getNodeType()) {
                if ("di:BPMNShape".equals(cNode.getNodeName()))
                    parseShapeData(cNode, proc);
                else if ("di:BPMNEdge".equals(cNode.getNodeName()))
                    parseEdgeData(cNode, proc);
            }
        }
    }

    private void parseShapeData(Node node, Process proc) {
        String actId = parseAttributeValue(node, "bpmnElement");
        Activity act = proc.getActivityById(actId);
        if (act != null) {
            Node bounds = getNode(node, "dc:Bounds");
            String value = "x=" + Double.valueOf(parseAttributeValue(bounds, "x")).intValue()
                    + ",y=" + Double.valueOf(parseAttributeValue(bounds, "y")).intValue() + ",w="
                    + Double.valueOf(parseAttributeValue(bounds, "width")).intValue() + ",h="
                    + Double.valueOf(parseAttributeValue(bounds, "height")).intValue();
            act.setAttribute(WorkAttributeConstant.WORK_DISPLAY_INFO, value);
        }
    }

    private void parseEdgeData(Node node, Process proc) {
        Transition trans = proc.getTransition(
                Long.parseLong(parseAttributeValue(node, "bpmnElement").substring(1)));
        if (trans != null) {
            String transDispay = trans.getAttribute(
                    WorkTransitionAttributeConstant.TRANSITION_DISPLAY_INFO) + "," + "type=Elbow,";
            NodeList nodeList = node.getChildNodes();
            StringBuilder xs = new StringBuilder("xs=");
            StringBuilder ys = new StringBuilder(",ys=");
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node cNode = nodeList.item(i);
                if (Node.ELEMENT_NODE == cNode.getNodeType()) {
                    xs.append(Double.valueOf(parseAttributeValue(cNode, "x")).intValue())
                            .append("&");
                    ys.append(Double.valueOf(parseAttributeValue(cNode, "y")).intValue())
                            .append("&");
                }
            }
            trans.setAttribute(WorkTransitionAttributeConstant.TRANSITION_DISPLAY_INFO, transDispay
                    + xs.substring(0, xs.length() - 1) + ys.substring(0, ys.length() - 1));
        }
    }

}
