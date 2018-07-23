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

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.xmlbeans.SchemaType;
import org.apache.xmlbeans.XmlError;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.omg.spec.bpmn.x20100524.di.BPMNEdge;
import org.omg.spec.bpmn.x20100524.di.BPMNEdgeDocument;
import org.omg.spec.bpmn.x20100524.di.BPMNShape;
import org.omg.spec.bpmn.x20100524.di.BPMNShapeDocument;
import org.omg.spec.bpmn.x20100524.model.BusinessRuleTaskDocument;
import org.omg.spec.bpmn.x20100524.model.DefinitionsDocument;
import org.omg.spec.bpmn.x20100524.model.EndEventDocument;
import org.omg.spec.bpmn.x20100524.model.ExclusiveGatewayDocument;
import org.omg.spec.bpmn.x20100524.model.IntermediateCatchEventDocument;
import org.omg.spec.bpmn.x20100524.model.IntermediateThrowEventDocument;
import org.omg.spec.bpmn.x20100524.model.ParallelGatewayDocument;
import org.omg.spec.bpmn.x20100524.model.ProcessDocument;
import org.omg.spec.bpmn.x20100524.model.ScriptTaskDocument;
import org.omg.spec.bpmn.x20100524.model.SequenceFlowDocument;
import org.omg.spec.bpmn.x20100524.model.ServiceTaskDocument;
import org.omg.spec.bpmn.x20100524.model.StartEventDocument;
import org.omg.spec.bpmn.x20100524.model.SubProcessDocument;
import org.omg.spec.bpmn.x20100524.model.TBaseElement;
import org.omg.spec.bpmn.x20100524.model.TBusinessRuleTask;
import org.omg.spec.bpmn.x20100524.model.TDefinitions;
import org.omg.spec.bpmn.x20100524.model.TEndEvent;
import org.omg.spec.bpmn.x20100524.model.TExclusiveGateway;
import org.omg.spec.bpmn.x20100524.model.TIntermediateCatchEvent;
import org.omg.spec.bpmn.x20100524.model.TIntermediateThrowEvent;
import org.omg.spec.bpmn.x20100524.model.TParallelGateway;
import org.omg.spec.bpmn.x20100524.model.TProcess;
import org.omg.spec.bpmn.x20100524.model.TScriptTask;
import org.omg.spec.bpmn.x20100524.model.TSequenceFlow;
import org.omg.spec.bpmn.x20100524.model.TServiceTask;
import org.omg.spec.bpmn.x20100524.model.TStartEvent;
import org.omg.spec.bpmn.x20100524.model.TSubProcess;
import org.omg.spec.bpmn.x20100524.model.TUserTask;
import org.omg.spec.bpmn.x20100524.model.UserTaskDocument;
import org.omg.spec.dd.x20100524.dc.Bounds;
import org.omg.spec.dd.x20100524.dc.Point;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.centurylink.mdw.constant.WorkAttributeConstant;
import com.centurylink.mdw.constant.WorkTransitionAttributeConstant;
import com.centurylink.mdw.model.attribute.Attribute;
import com.centurylink.mdw.model.variable.Variable;
import com.centurylink.mdw.model.workflow.Activity;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.model.workflow.TextNote;
import com.centurylink.mdw.model.workflow.Transition;

/**
 * <p>
 * Converts an MDW process definition into a BPMN standards xml.
 *
 * Also caters for drawing the activities in the correct place on screen
 *
 * </p>
 *
 * @author aa70413
 *
 */
public class BpmnExportHelper {

    // Root element
    private TDefinitions defs;

    private static final String MDW_ATTRIBUTE = "mdw:Attribute";
    private static final String MDW_NAMESPACE = "http://mdw.centurylink.com/bpm";

    public String exportProcess(Process processVO) throws IOException {
        // Create BPMN root element
        DefinitionsDocument defdoc = DefinitionsDocument.Factory.newInstance();
        // Create definitions element
        defs = createDefinitions(defdoc);
        // Add a root element and convert to Process
        // this is needed since the xsd uses substitutes
        XmlObject root = defs.addNewRootElement();
        root = substitute(root, ProcessDocument.type.getDocumentElementName(), TProcess.type);
        // Create the process element
        TProcess process = createProcess(root, processVO);
        defs.addNewBPMNDiagram().addNewBPMNPlane().setBpmnElement(new QName(process.getId()));

        addProcessElements(process, processVO);

        if (validate(defdoc)) {
            return defdoc.toString();
        }
        else {
            throw new IOException("Invalid definition");
        }
    }

    /**
     * @param process
     * @param processVO
     * @throws XmlException
     */
    private void addProcessElements(TBaseElement process, Process processVO) {
        // Add activities
        addActivities(process, processVO);
        // Add transitions
        addTransitions(process, processVO);
        // Add subprocesses
        addSubprocesses(process, processVO);
    }

    /**
     * Validates the xml after creation
     *
     * @param defdoc
     * @return boolean valid?
     */
    private boolean validate(DefinitionsDocument defdoc) {
        List<XmlError> errorList = new ArrayList<>();
        XmlOptions options = new XmlOptions().setSavePrettyPrint().setSavePrettyPrintIndent(2)
                .setSaveAggressiveNamespaces();
        options.setErrorListener(errorList);

        System.out.println("!--toString---");
        System.out.println(defdoc.toString());
        boolean valid = defdoc.validate(options);
        System.out.println("Document is " + (valid ? "valid" : "invalid"));
        if (!valid) {
            for (int i = 0; i < errorList.size(); i++) {
                XmlError error = errorList.get(i);

                System.out.println("\n");
                System.out.println("Message: " + error.getMessage() + "\n");
                System.out.println(
                        "Location of invalid XML: " + error.getCursorLocation().xmlText() + "\n");
            }
        }
        return valid;
    }

    /**
     * <p>
     * This deals with creating and drawing subprocess elements like exception
     * handler
     * </p>
     *
     * @param process
     * @param processVO
     * @throws XmlException
     */
    private void addSubprocesses(TBaseElement process, Process processVO) {
        List<Process> subprocs = processVO.getSubprocesses();
        if (subprocs != null) {
            for (Process subproc : subprocs) {
                // Add for subprocesses
                XmlObject flow = getProcessFlowElement(process);
                if (flow != null) {
                    // Convert for substitutes
                    flow = substitute(flow, SubProcessDocument.type.getDocumentElementName(),
                            TSubProcess.type);
                    TSubProcess subProcess = (TSubProcess) flow.changeType(TSubProcess.type);
                    subProcess.setId("P" + subproc.getId());
                    subProcess.setName(subproc.getName());
                    subProcess.addNewExtensionElements().set(getExtentionElements(subproc));
                    addProcessElements(subProcess, subproc);
                }
            }
        }
    }

    /**
     * @param process
     * @param processVO
     */
    private void addTransitions(TBaseElement process, Process processVO) {
        List<com.centurylink.mdw.model.workflow.Transition> connectors = processVO.getTransitions();
        for (Transition conn : connectors) {
            XmlObject flow = getProcessFlowElement(process);
            if (flow != null) {
                flow = substitute(flow, SequenceFlowDocument.type.getDocumentElementName(),
                        TSequenceFlow.type);
                TSequenceFlow seqflow = (TSequenceFlow) flow.changeType(TSequenceFlow.type);
                String referrId = conn.getLogicalId();
                seqflow.setId(referrId);
                seqflow.setName(conn.getCompletionCode());
                seqflow.setSourceRef("A" + conn.getFromId());
                seqflow.setTargetRef("A" + conn.getToId());
                seqflow.addNewExtensionElements().set(getExtentionElements(conn));
                // Get coordinates
                TransitionPoints coords = parseTransitionCoordinates(
                        conn.getAttribute(WorkTransitionAttributeConstant.TRANSITION_DISPLAY_INFO));
                // Add an activity shape
                XmlObject diagramElement = defs.getBPMNDiagramArray(0).getBPMNPlane()
                        .addNewDiagramElement();
                diagramElement = substitute(diagramElement,
                        BPMNEdgeDocument.type.getDocumentElementName(), BPMNEdge.type);
                BPMNEdge shape = (BPMNEdge) diagramElement.changeType(BPMNEdge.type);
                shape.setBpmnElement(new QName(referrId));

                // Set the cooordinates
                for (int i = 0; i < coords.getXs().length; i++) {
                    Point startPoint = shape.addNewWaypoint();
                    startPoint.setX(coords.getXs()[i]);
                    startPoint.setY(coords.getYs()[i]);
                }
            }
        }
    }

    /**
     * @param process
     * @param processVO
     * @throws XmlException
     */
    private void addActivities(TBaseElement process, Process processVO) {
        List<Activity> acts = processVO.getActivities();
        for (Activity act : acts) {
            XmlObject flow = getProcessFlowElement(process);
            String referrId = "";
            if (flow != null)
                referrId = substituteActivityType(flow, act);
            // Get coordinates
            Coordinates coords = parseCoordinates(
                    act.getAttribute(WorkAttributeConstant.WORK_DISPLAY_INFO));
            // Add an activity shape
            XmlObject diagramElement = defs.getBPMNDiagramArray(0).getBPMNPlane()
                    .addNewDiagramElement();
            diagramElement = substitute(diagramElement,
                    BPMNShapeDocument.type.getDocumentElementName(), BPMNShape.type);
            BPMNShape shape = (BPMNShape) diagramElement.changeType(BPMNShape.type);
            shape.setBpmnElement(new QName(referrId));
            // Set the cooordinates
            Bounds bounds = shape.addNewBounds();
            bounds.setX(coords.getX());
            bounds.setY(coords.getY());
            bounds.setWidth(coords.getWidth());
            bounds.setHeight(coords.getHeight());
        }
    }

    /**
     * @param process
     * @return XmlObject Process Flow Element
     */
    private XmlObject getProcessFlowElement(TBaseElement process) {
        XmlObject flow = null;
        if (process instanceof TProcess) {
            flow = ((TProcess) process).addNewFlowElement();

        }
        else if (process instanceof TSubProcess) {
            flow = ((TSubProcess) process).addNewFlowElement();

        }
        return flow;
    }

    /**
     * @param defdoc
     * @return a TDefinitions object
     */
    private TDefinitions createDefinitions(DefinitionsDocument defdoc) {
        TDefinitions definitions = defdoc.addNewDefinitions();
        // Set definitions basic attributes
        definitions.setTargetNamespace("http://www.omg.org/spec/BPMN/20100524/MODEL");
        definitions.setTypeLanguage("http://www.java.com/javaTypes");
        return definitions;
    }

    /**
     * @param root
     * @param processVO
     * @return TProcess process
     */
    private TProcess createProcess(XmlObject root, Process processVO) {
        TProcess process = (TProcess) root.changeType(TProcess.type);
        process.setId("MainProcess");
        process.setName(processVO.getName());
        process.setIsExecutable(true);
        process.addNewExtensionElements().set(getExtentionElements(processVO));
        return process;
    }

    /**
     * @param root
     * @param documentElementName
     * @param type
     * @return XmlObject root.substitute
     */
    private XmlObject substitute(XmlObject root, QName documentElementName, SchemaType type) {
        return root.substitute(documentElementName, type);
    }

    private String substituteActivityType(XmlObject flow, Activity act) {
        String referrId;
        switch (act.getImplementor()) {
        case "com.centurylink.mdw.workflow.activity.process.ProcessStartActivity":
            flow = substitute(flow, StartEventDocument.type.getDocumentElementName(),
                    TStartEvent.type);
            TStartEvent startEvent = (TStartEvent) flow.changeType(TStartEvent.type);
            startEvent.setId(act.getLogicalId());
            startEvent.setName(act.getName());
            startEvent.addNewExtensionElements().set(getExtentionElements(act));
            referrId = startEvent.getId();
            break;
        case "com.centurylink.mdw.workflow.activity.process.ProcessFinishActivity":
            flow = substitute(flow, EndEventDocument.type.getDocumentElementName(), TEndEvent.type);
            TEndEvent endEvent = (TEndEvent) flow.changeType(TEndEvent.type);
            endEvent.setId(act.getLogicalId());
            endEvent.setName(act.getName());
            endEvent.addNewExtensionElements().set(getExtentionElements(act));
            referrId = endEvent.getId();
            break;
        case "com.centurylink.mdw.workflow.activity.script.ScriptEvaluator":
            flow = substitute(flow, ExclusiveGatewayDocument.type.getDocumentElementName(),
                    TExclusiveGateway.type);
            TExclusiveGateway xOREvent = (TExclusiveGateway) flow
                    .changeType(TExclusiveGateway.type);
            xOREvent.setId(act.getLogicalId());
            xOREvent.setName(act.getName());
            xOREvent.addNewExtensionElements().set(getExtentionElements(act));
            referrId = xOREvent.getId();
            break;
        case "com.centurylink.mdw.workflow.activity.script.ScriptExecutorActivity":
            flow = substitute(flow, ScriptTaskDocument.type.getDocumentElementName(),
                    TScriptTask.type);
            TScriptTask scriptTask = (TScriptTask) flow.changeType(TScriptTask.type);
            scriptTask.setId(act.getLogicalId());
            scriptTask.setName(act.getName());
            scriptTask.addNewExtensionElements().set(getExtentionElements(act));
            referrId = scriptTask.getId();
            break;
        case "com.centurylink.mdw.workflow.activity.process.InvokeSubProcessActivity":
            flow = substitute(flow, SubProcessDocument.type.getDocumentElementName(),
                    TSubProcess.type);
            TSubProcess subProcTask = (TSubProcess) flow.changeType(TSubProcess.type);
            subProcTask.setId(act.getLogicalId());
            subProcTask.setName(act.getName());
            subProcTask.addNewExtensionElements().set(getExtentionElements(act));
            referrId = subProcTask.getId();
            break;
        case "com.centurylink.mdw.workflow.activity.process.InvokeHeterogeneousProcessActivity":
            flow = substitute(flow, SubProcessDocument.type.getDocumentElementName(),
                    TSubProcess.type);
            TSubProcess hetProcTask = (TSubProcess) flow.changeType(TSubProcess.type);
            hetProcTask.setId(act.getLogicalId());
            hetProcTask.setName(act.getName());
            hetProcTask.addNewExtensionElements().set(getExtentionElements(act));
            referrId = hetProcTask.getId();
            break;
        case "com.centurylink.mdw.workflow.activity.task.AutoFormManualTaskActivity":
            flow = substitute(flow, UserTaskDocument.type.getDocumentElementName(), TUserTask.type);
            TUserTask autoformTask = (TUserTask) flow.changeType(TUserTask.type);
            autoformTask.setId(act.getLogicalId());
            autoformTask.setName(act.getName());
            autoformTask.addNewExtensionElements().set(getExtentionElements(act));
            referrId = autoformTask.getId();
            break;
        case "com.centurylink.mdw.workflow.activity.task.CustomManualTaskActivity":
            flow = substitute(flow, UserTaskDocument.type.getDocumentElementName(), TUserTask.type);
            TUserTask customTask = (TUserTask) flow.changeType(TUserTask.type);
            customTask.setId(act.getLogicalId());
            customTask.setName(act.getName());
            customTask.addNewExtensionElements().set(getExtentionElements(act));
            referrId = customTask.getId();
            break;
        case "com.centurylink.mdw.workflow.activity.timer.TimerWaitActivity":
            flow = substitute(flow, IntermediateCatchEventDocument.type.getDocumentElementName(),
                    TIntermediateCatchEvent.type);
            TIntermediateCatchEvent timerEvent = (TIntermediateCatchEvent) flow
                    .changeType(TIntermediateCatchEvent.type);
            timerEvent.setId(act.getLogicalId());
            timerEvent.setName(act.getName());
            timerEvent.addNewExtensionElements().set(getExtentionElements(act));
            referrId = timerEvent.getId();
            break;
        case "com.centurylink.mdw.drools.DroolsActivity":
            flow = substitute(flow, BusinessRuleTaskDocument.type.getDocumentElementName(),
                    TBusinessRuleTask.type);
            TBusinessRuleTask brulesTask = (TBusinessRuleTask) flow
                    .changeType(TBusinessRuleTask.type);
            brulesTask.setId(act.getLogicalId());
            brulesTask.setName(act.getName());
            brulesTask.addNewExtensionElements().set(getExtentionElements(act));
            referrId = brulesTask.getId();
            break;
        case "com.centurylink.mdw.drools.DroolsDecisionTableActivity":
            flow = substitute(flow, BusinessRuleTaskDocument.type.getDocumentElementName(),
                    TBusinessRuleTask.type);
            TBusinessRuleTask brulesTableTask = (TBusinessRuleTask) flow
                    .changeType(TBusinessRuleTask.type);
            brulesTableTask.setId(act.getLogicalId());
            brulesTableTask.setName(act.getName());
            brulesTableTask.addNewExtensionElements().set(getExtentionElements(act));
            referrId = brulesTableTask.getId();
            break;
        case "com.centurylink.mdw.workflow.activity.sync.SynchronizationActivity":
            flow = substitute(flow, ParallelGatewayDocument.type.getDocumentElementName(),
                    TParallelGateway.type);
            TParallelGateway parallelGateway = (TParallelGateway) flow
                    .changeType(TParallelGateway.type);
            parallelGateway.setId(act.getLogicalId());
            parallelGateway.setName(act.getName());
            parallelGateway.addNewExtensionElements().set(getExtentionElements(act));
            referrId = parallelGateway.getId();
            break;
        case "com.centurylink.mdw.workflow.activity.event.EventWaitActivity":
            flow = substitute(flow, IntermediateCatchEventDocument.type.getDocumentElementName(),
                    TIntermediateCatchEvent.type);
            TIntermediateCatchEvent waitEvent = (TIntermediateCatchEvent) flow
                    .changeType(TIntermediateCatchEvent.type);
            waitEvent.setId(act.getLogicalId());
            waitEvent.setName(act.getName());
            waitEvent.addNewExtensionElements().set(getExtentionElements(act));
            referrId = waitEvent.getId();
            break;
        case "com.centurylink.mdw.microservice.DependenciesWaitActivity":
            flow = substitute(flow, IntermediateCatchEventDocument.type.getDocumentElementName(),
                    TIntermediateCatchEvent.type);
            TIntermediateCatchEvent dependenciesEvent = (TIntermediateCatchEvent) flow
                    .changeType(TIntermediateCatchEvent.type);
            dependenciesEvent.setId(act.getLogicalId());
            dependenciesEvent.setName(act.getName());
            dependenciesEvent.addNewExtensionElements().set(getExtentionElements(act));
            referrId = dependenciesEvent.getId();
            break;
        case "com.centurylink.mdw.workflow.activity.event.EventCheckActivity":
            flow = substitute(flow, IntermediateCatchEventDocument.type.getDocumentElementName(),
                    TIntermediateCatchEvent.type);
            TIntermediateCatchEvent checkEevent = (TIntermediateCatchEvent) flow
                    .changeType(TIntermediateCatchEvent.type);
            checkEevent.setId(act.getLogicalId());
            checkEevent.setName(act.getName());
            checkEevent.addNewExtensionElements().set(getExtentionElements(act));
            referrId = checkEevent.getId();
            break;
        case "com.centurylink.mdw.workflow.activity.event.PublishEventMessage":
            flow = substitute(flow, IntermediateThrowEventDocument.type.getDocumentElementName(),
                    TIntermediateThrowEvent.type);
            TIntermediateThrowEvent signalThrowEvent = (TIntermediateThrowEvent) flow
                    .changeType(TIntermediateThrowEvent.type);
            signalThrowEvent.setId(act.getLogicalId());
            signalThrowEvent.setName(act.getName());
            signalThrowEvent.addNewExtensionElements().set(getExtentionElements(act));
            referrId = signalThrowEvent.getId();
            break;
        case "com.centurylink.mdw.microservice.ServiceEventPublish":
            flow = substitute(flow, IntermediateThrowEventDocument.type.getDocumentElementName(),
                    TIntermediateThrowEvent.type);
            TIntermediateThrowEvent serviceThrowEvent = (TIntermediateThrowEvent) flow
                    .changeType(TIntermediateThrowEvent.type);
            serviceThrowEvent.setId(act.getLogicalId());
            serviceThrowEvent.setName(act.getName());
            serviceThrowEvent.addNewExtensionElements().set(getExtentionElements(act));
            referrId = serviceThrowEvent.getId();
            break;
        case "com.centurylink.mdw.workflow.activity.event.PublishEventMessageRest":
            flow = substitute(flow, IntermediateThrowEventDocument.type.getDocumentElementName(),
                    TIntermediateThrowEvent.type);
            TIntermediateThrowEvent publishEvent = (TIntermediateThrowEvent) flow
                    .changeType(TIntermediateThrowEvent.type);
            publishEvent.setId(act.getLogicalId());
            publishEvent.setName(act.getName());
            publishEvent.addNewExtensionElements().set(getExtentionElements(act));
            referrId = publishEvent.getId();
            break;
        default:
            flow = substitute(flow, ServiceTaskDocument.type.getDocumentElementName(),
                    TServiceTask.type);
            TServiceTask bpmnact = (TServiceTask) flow.changeType(TServiceTask.type);
            bpmnact.setId(act.getLogicalId());
            bpmnact.setName(act.getName());
            bpmnact.addNewExtensionElements().set(getExtentionElements(act));
            referrId = bpmnact.getId();
        }
        return referrId;
    }

    private Coordinates parseCoordinates(String attrvalue) {
        Coordinates rect = new Coordinates();
        if (attrvalue == null || attrvalue.length() == 0)
            return rect;
        String[] tmps = attrvalue.split(",");
        rect.setX(Double.parseDouble(tmps[0].substring(2)));
        rect.setY(Double.parseDouble(tmps[1].substring(2)));
        rect.setWidth(Double.parseDouble(tmps[2].substring(2)));
        rect.setHeight(Double.parseDouble(tmps[3].substring(2)));
        return rect;
    }

    private TransitionPoints parseTransitionCoordinates(String dispinfo) {
        TransitionPoints trans = new TransitionPoints();
        if (dispinfo != null && dispinfo.length() > 0) {
            String[] attrs = dispinfo.split(",");
            String[] sts = attrs[3].substring(3).split("&");
            int[] xs = new int[sts.length];
            for (int j = 0; j < xs.length; j++) {
                xs[j] = Integer.parseInt(sts[j]);
            }
            trans.setXs(xs);
            sts = attrs[4].substring(3).split("&");
            int[] ys = new int[sts.length];
            for (int j = 0; j < ys.length; j++) {
                ys[j] = Integer.parseInt(sts[j]);
            }
            trans.setYs(ys);
        }
        return trans;
    }

    private class TransitionPoints {
        public int[] getXs() {
            return xs;
        }

        public void setXs(int[] xs) {
            this.xs = xs;
        }

        public int[] getYs() {
            return ys;
        }

        public void setYs(int[] ys) {
            this.ys = ys;
        }

        private int[] xs;
        private int[] ys;

    }

    private class Coordinates {
        /**
         * @return the x
         */
        public double getX() {
            return x;
        }

        /**
         * @param x
         *            the x to set
         */
        public void setX(double x) {
            this.x = x;
        }

        /**
         * @return the y
         */
        public double getY() {
            return y;
        }

        /**
         * @param y
         *            the y to set
         */
        public void setY(double y) {
            this.y = y;
        }

        /**
         * @return the width
         */
        public double getWidth() {
            return width;
        }

        /**
         * @param width
         *            the width to set
         */
        public void setWidth(double width) {
            this.width = width;
        }

        /**
         * @return the height
         */
        public double getHeight() {
            return height;
        }

        /**
         * @param height
         *            the height to set
         */
        public void setHeight(double height) {
            this.height = height;
        }

        private double x;
        private double y;
        private double width;
        private double height;
    }

    private void addElements(Document doc, Element elements, List<?> attrs) {
        for (Object attr : attrs) {
            if (attr instanceof Attribute) {
                Attribute attribute = (Attribute) attr;
                if (elements.getLocalName() == null
                        || "mdw:ProcessExtensions".equals(elements.getNodeName())
                        || (!"WORK_DISPLAY_INFO".equals(attribute.getAttributeName())
                                && !"LOGICAL_ID".equals(attribute.getAttributeName())))
                    elements.appendChild(getNode(doc, MDW_ATTRIBUTE, attribute.getAttributeName(),
                            attribute.getAttributeValue()));
            }
            else if (attr instanceof Variable) {
                Variable var = (Variable) attr;
                elements.appendChild(getVariableNode(doc, "mdw:Variable", var));
            }
            else if (attr instanceof TextNote) {
                TextNote textNote = (TextNote) attr;
                elements.appendChild(getTextNode(doc, "mdw:TextNote", textNote));
            }
        }
    }

    private Node getNode(Document doc, String element, String name, String value) {
        Element attribute = doc.createElement(element);
        attribute.setAttribute("name", name);
        if (!value.isEmpty())
            attribute.appendChild(doc.createTextNode(value));
        return attribute;
    }

    private Node getVariableNode(Document doc, String element, Variable var) {
        Element attribute = doc.createElement(element);
        attribute.setAttribute("name", var.getName());
        attribute.setAttribute("category", var.getCategory());
        if (var.getDisplaySequence() != null)
            attribute.setAttribute("dispaySequence", String.valueOf(var.getDisplaySequence()));
        else
            attribute.setAttribute("dispaySequence", "0");
        attribute.appendChild(getNode(doc, "mdw:type", var.getType(), ""));
        return attribute;
    }

    private Node getTextNode(Document doc, String element, TextNote note) {
        Element attribute = doc.createElement(element);
        attribute.setAttribute("content", note.getContent());
        attribute.setAttribute("Reference", note.getReference());
        addElements(doc, attribute, note.getAttributes());
        return attribute;
    }

    private XmlObject getExtentionElements(Object obj) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;
        try {
            builder = factory.newDocumentBuilder();
            Document doc = builder.newDocument();
            if (obj instanceof Activity) {
                Activity act = (Activity) obj;
                Element elements = doc.createElementNS(MDW_NAMESPACE, "mdw:Attributes");
                doc.appendChild(elements);
                elements.appendChild(
                        getNode(doc, MDW_ATTRIBUTE, "Implementor", act.getImplementor()));
                addElements(doc, elements, act.getAttributes());
            }
            else if (obj instanceof Transition) {
                Transition trans = (Transition) obj;
                Element elements = doc.createElementNS(MDW_NAMESPACE, "mdw:Attributes");
                doc.appendChild(elements);
                String[] displayInfo = trans
                        .getAttribute(WorkTransitionAttributeConstant.TRANSITION_DISPLAY_INFO)
                        .split(",");
                elements.appendChild(
                        getNode(doc, MDW_ATTRIBUTE, "Event", String.valueOf(trans.getEventType())));
                elements.appendChild(getNode(doc, MDW_ATTRIBUTE,
                        WorkTransitionAttributeConstant.TRANSITION_DISPLAY_INFO,
                        displayInfo[0] + "," + displayInfo[1]));
                if (!trans.getTransitionDelayUnit().isEmpty())
                    elements.appendChild(getNode(doc, MDW_ATTRIBUTE, "TransitionDelayUnit",
                            trans.getTransitionDelayUnit()));
                if (trans.getTransitionDelay() != 0)
                    elements.appendChild(getNode(doc, MDW_ATTRIBUTE, "TransitionDelay",
                            String.valueOf(trans.getTransitionDelay())));

            }
            else if (obj instanceof Process) {
                Process proc = (Process) obj;
                Element elements = doc.createElementNS(MDW_NAMESPACE, "mdw:ProcessExtensions");
                doc.appendChild(elements);
                addElements(doc, elements, proc.getAttributes());
                addElements(doc, elements, proc.getVariables());
                if (!proc.getTextNotes().isEmpty())
                    addElements(doc, elements, proc.getTextNotes());
            }
            return XmlObject.Factory.parse(getExtensionElemetnsXml(doc));
        }
        catch (XmlException | ParserConfigurationException e) {
            System.err.println("Unable to add extension elements");
        }
        return null;
    }

    private String getExtensionElemetnsXml(Document doc) {
        StringWriter writer = new StringWriter();
        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();

            DOMSource domSource = new DOMSource(doc);
            StreamResult streamResult = new StreamResult(writer);
            transformer.transform(domSource, streamResult);
        }
        catch (TransformerException e) {
            System.err.println("Unable to get extension elements");
        }
        return writer.toString();
    }
}
