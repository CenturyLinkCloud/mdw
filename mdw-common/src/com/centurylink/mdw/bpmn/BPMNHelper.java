package com.centurylink.mdw.bpmn;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.xmlbeans.SchemaType;
import org.apache.xmlbeans.XmlError;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.omg.spec.bpmn.x20100524.di.BPMNEdge;
import org.omg.spec.bpmn.x20100524.di.BPMNEdgeDocument;
import org.omg.spec.bpmn.x20100524.di.BPMNShape;
import org.omg.spec.bpmn.x20100524.di.BPMNShapeDocument;
import org.omg.spec.bpmn.x20100524.model.CallActivityDocument;
import org.omg.spec.bpmn.x20100524.model.DefinitionsDocument;
import org.omg.spec.bpmn.x20100524.model.EndEventDocument;
import org.omg.spec.bpmn.x20100524.model.ProcessDocument;
import org.omg.spec.bpmn.x20100524.model.SequenceFlowDocument;
import org.omg.spec.bpmn.x20100524.model.StartEventDocument;
import org.omg.spec.bpmn.x20100524.model.SubProcessDocument;
import org.omg.spec.bpmn.x20100524.model.TBaseElement;
import org.omg.spec.bpmn.x20100524.model.TCallActivity;
import org.omg.spec.bpmn.x20100524.model.TDefinitions;
import org.omg.spec.bpmn.x20100524.model.TDocumentation;
import org.omg.spec.bpmn.x20100524.model.TEndEvent;
import org.omg.spec.bpmn.x20100524.model.TProcess;
import org.omg.spec.bpmn.x20100524.model.TProcessType;
import org.omg.spec.bpmn.x20100524.model.TSequenceFlow;
import org.omg.spec.bpmn.x20100524.model.TStartEvent;
import org.omg.spec.bpmn.x20100524.model.TSubProcess;
import org.omg.spec.dd.x20100524.dc.Bounds;
import org.omg.spec.dd.x20100524.dc.Point;

import com.centurylink.mdw.constant.WorkAttributeConstant;
import com.centurylink.mdw.constant.WorkTransitionAttributeConstant;
import com.centurylink.mdw.model.note.TextNote;
import com.centurylink.mdw.model.workflow.Activity;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.util.file.FileHelper;

/**
 * <p>
 * Converts an MDW process definition into a BPMN standards xml.
 *
 * Also caters for drawing the activities in the correct place on screen
 *
 * </p>
 * @author aa70413
 *
 */
public class BPMNHelper {

    private boolean drawDiagram;

    //Root element
    private TDefinitions defs;

    public BPMNHelper() {
        drawDiagram = true;
    }

    public void exportProcess(Process processVO, String filename) {

        // Create BPMN root element
        DefinitionsDocument defdoc = DefinitionsDocument.Factory.newInstance();

        // Create definitions element
        defs = createDefinitions(defdoc);
        // Add a root element and convert to Process
        // this is needed since the xsd uses substitutes
        XmlObject root = defs.addNewRootElement();
        root = substitute(root, ProcessDocument.type.getDocumentElementName(), TProcess.type);
        // Create the process element
        TProcess myprocess = createProcess(root, processVO);

        if (drawDiagram) {
            defs.addNewBPMNDiagram().addNewBPMNPlane().setBpmnElement(new QName(myprocess.getId()));;
        }
        addProcessElements(myprocess, processVO);

        if (validate(defdoc)) {
            // Save to file
            save(filename, defdoc);
        }

    }


    /**
     * @param myprocess
     * @param processVO
     */
    private void addProcessElements(TBaseElement myprocess, Process processVO) {
        // Add activities
        addActivities(myprocess, processVO);

        // Add transitions
        addTransitions(myprocess, processVO);

        // Add subprocesses
        addSubprocesses(myprocess, processVO);

        // Add Text Notes
        addTextNotes(myprocess, processVO);

    }

    /**
     * @param filename
     * @param defdoc
     */
    private void save(String filename, DefinitionsDocument defdoc) {
        FileOutputStream os = null;

        try {
            os = new FileOutputStream(filename);
            os.write(defdoc.toString().getBytes());
            os.close();
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        finally {
            if (os != null) {
                try {
                    os.close();
                }
                catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

    }

    /**
     * Validates the xml after creation
     *
     * @param defdoc
     * @return
     */
    private boolean validate(DefinitionsDocument defdoc) {
        List<XmlError> errorList = new ArrayList<XmlError>();
        XmlOptions options = new XmlOptions().setSavePrettyPrint().setSavePrettyPrintIndent(2)
                .setSaveAggressiveNamespaces();
        options.setErrorListener(errorList);

        System.out.println("!--toString---");
        System.out.println(defdoc.toString());
        boolean valid = defdoc.validate(options);
        System.out.println("Document is " + (valid ? "valid" : "invalid"));
        if (!valid) {

            for (int i = 0; i < errorList.size(); i++) {
                XmlError error = (XmlError) errorList.get(i);

                System.out.println("\n");
                System.out.println("Message: " + error.getMessage() + "\n");
                System.out.println(
                        "Location of invalid XML: " + error.getCursorLocation().xmlText() + "\n");
            }
        }
        return valid;
    }

    /**
     * @param myprocess
     * @param processVO
     */
    private void addTextNotes(TBaseElement myprocess, Process processVO) {
        if (processVO.getTextNotes() != null) {
            for (TextNote note : processVO.getTextNotes()) {
                TDocumentation doc = myprocess.addNewDocumentation();
                doc.setId(getModifiedId(processVO.getName() + "-Note-" + note.getLogicalId()));
                doc.setTextFormat(note.getContent());
            }
        }

    }

    /**
     * <p>
     * This deals with creating and drawing subprocess elements like
     * exception handler
     * </p>
     * @param myprocess
     * @param processVO
     */
    private void addSubprocesses(TBaseElement myprocess, Process processVO) {
        List<Process> subprocs = processVO.getSubProcesses();
        if (subprocs != null) {
            for (Process subproc : subprocs) {
                // Add for subprocesses
                XmlObject flow = getProcessFlowElement(myprocess);
                // Convert for substitutes
                flow = substitute(flow, SubProcessDocument.type.getDocumentElementName(),
                        TSubProcess.type);
                TSubProcess subProcess = (TSubProcess) flow.changeType(TSubProcess.type);
                subProcess.setId(getModifiedId(subproc.getName() + subproc.getId()));
                subProcess.setName(subproc.getName());
                addProcessElements(subProcess,subproc );

            }
        }

    }
    /**
     * Added the '_' to ensure that it conforms to xsd type convention
     * @param str
     * @return
     */
    private String getModifiedId(String str) {
        return "_" + FileHelper.stripDisallowedFilenameChars(str).replaceAll(" ", "");
    }

    /**
     * @param myprocess
     * @param processVO
     */
    private void addTransitions(TBaseElement myprocess, Process processVO) {
        List<com.centurylink.mdw.model.workflow.Transition> connectors = processVO.getTransitions();
        for (com.centurylink.mdw.model.workflow.Transition conn : connectors) {
            XmlObject flow = getProcessFlowElement(myprocess);
            flow = substitute(flow, SequenceFlowDocument.type.getDocumentElementName(),
                    TSequenceFlow.type);
            TSequenceFlow seqflow = (TSequenceFlow) flow.changeType(TSequenceFlow.type);
            String referrId = getModifiedId(processVO.getName() + "-" + conn.getLogicalId() + "-"
                    + conn.getWorkTransitionId());
            seqflow.setId(referrId);
            seqflow.setName(conn.getLogicalId());
            seqflow.setSourceRef(getModifiedId(processVO.getName() + "-" + conn.getFromWorkId()));
            seqflow.setTargetRef(getModifiedId(processVO.getName() + "-" + conn.getToWorkId()));
            // Draw diagram if necessary
            if (drawDiagram) {
                // Get coordinates
                Transition coords = parseTransitionCoordinates(conn.getAttribute(WorkTransitionAttributeConstant.TRANSITION_DISPLAY_INFO));
                // Add an activity shape
                XmlObject diagramElement = defs.getBPMNDiagramArray(0).getBPMNPlane().addNewDiagramElement();
                diagramElement = substitute(diagramElement, BPMNEdgeDocument.type.getDocumentElementName(),
                        BPMNEdge.type);
                BPMNEdge shape = (BPMNEdge) diagramElement.changeType(BPMNEdge.type);
                shape.setBpmnElement(new QName(referrId));
                // Set the cooordinates

                // Start point
                Point startPoint = shape.addNewWaypoint();
                startPoint.setX(coords.getLx());
                startPoint.setY(coords.getLy());
                //End point
                Point endPoint = shape.addNewWaypoint();
                endPoint.setX(coords.getXs()[coords.getXs().length-1]);
                endPoint.setY(coords.getYs()[coords.getYs().length-1]);

            }

        }

    }

    /**
     * @param myprocess
     * @param processVO
     */
    private void addActivities(TBaseElement myprocess, Process processVO) {
        /**
         * Iterate through activities and add
         * TODO add BPMNtoMDWmappings.json
         */
        List<Activity> acts = processVO.getActivities();
        for (Activity act : acts) {
            XmlObject flow = getProcessFlowElement(myprocess);
            String referrId = "";
            if ("com.qwest.mdw.workflow.activity.impl.process.ProcessStartControlledActivity"
                    .equals(act.getImplementorClassName())
                    || "com.centurylink.mdw.workflow.activity.process.ProcessStartActivity"
                            .equals(act.getImplementorClassName())) {
                // Start Event
                flow = substitute(flow, StartEventDocument.type.getDocumentElementName(),
                        TStartEvent.type);
                TStartEvent startEvent = (TStartEvent) flow.changeType(TStartEvent.type);
                startEvent.setId(getModifiedId(processVO.getName() + "-" + act.getActivityId()));
                startEvent.setName(act.getActivityName());
                referrId = startEvent.getId();
            }
            else if ("com.qwest.mdw.workflow.activity.impl.process.ProcessFinishControlledActivity"
                    .equals(act.getImplementorClassName())
                    || "com.centurylink.mdw.workflow.activity.process.ProcessFinishActivity"
                            .equals(act.getImplementorClassName())) {
                flow = substitute(flow, EndEventDocument.type.getDocumentElementName(),
                        TEndEvent.type);
                // End Event
                TEndEvent endEvent = (TEndEvent) flow.changeType(TEndEvent.type);
                endEvent.setId(getModifiedId(processVO.getName() + "-" + act.getActivityId()));
                endEvent.setName(act.getActivityName());
                referrId = endEvent.getId();

            }
            else {
                flow = substitute(flow, CallActivityDocument.type.getDocumentElementName(),
                        TCallActivity.type);

                TCallActivity bpmnact = (TCallActivity) flow.changeType(TCallActivity.type);
                bpmnact.setId(getModifiedId(processVO.getName() + "-" + act.getActivityId()));
                bpmnact.setName(act.getActivityName());
                referrId = bpmnact.getId();
            }

            // Draw diagram if necessary
            if (drawDiagram) {
                // Get coordinates
                Coordinates coords = parseCoordinates(act.getAttribute(WorkAttributeConstant.WORK_DISPLAY_INFO));
                // Add an activity shape
                XmlObject diagramElement = defs.getBPMNDiagramArray(0).getBPMNPlane().addNewDiagramElement();
                diagramElement = substitute(diagramElement, BPMNShapeDocument.type.getDocumentElementName(),
                        BPMNShape.type);
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

    }

    /**
     * @param myprocess
     * @return
     */
    private XmlObject getProcessFlowElement(TBaseElement myprocess) {
        XmlObject flow = null;
        if (myprocess instanceof TProcess) {
            flow = ((TProcess)myprocess).addNewFlowElement();

        } else if (myprocess instanceof TSubProcess) {
            flow = ((TSubProcess)myprocess).addNewFlowElement();

        }
        return flow;
    }

    /**
     * @param defdoc
     * @return a TDefinitions object
     */
    private TDefinitions createDefinitions(DefinitionsDocument defdoc) {
        TDefinitions defs = defdoc.addNewDefinitions();
        // Set definitions basic attributes
        defs.setTargetNamespace("http://www.omg.org/spec/BPMN/20100524/MODEL");
        defs.setTypeLanguage("http://www.java.com/javaTypes");
        return defs;
    }

    /**
     * @param root
     * @param processVO
     * @return
     */
    private TProcess createProcess(XmlObject root, Process processVO) {
        TProcess myprocess = (TProcess) root.changeType(TProcess.type);
        myprocess.setId(getModifiedId(processVO.getName() + processVO.getId()));
        myprocess.setName(processVO.getName());
        myprocess.setProcessType(TProcessType.PUBLIC);
        myprocess.setIsExecutable(true);
        return myprocess;
    }

    /**
     * @param root
     * @param documentElementName
     * @param type
     * @return
     */
    private XmlObject substitute(XmlObject root, QName documentElementName,
            SchemaType type) {
         return root.substitute(documentElementName, type);
    }

    private Coordinates parseCoordinates(String attrvalue) {
        Coordinates rect = new Coordinates();
       if (attrvalue==null || attrvalue.length()==0) return rect;
        String [] tmps = attrvalue.split(",");
        int k;
        String an, av;
        for (int i=0; i<tmps.length; i++) {
            k = tmps[i].indexOf('=');
            if (k<=0) continue;
            an = tmps[i].substring(0,k);
            av = tmps[i].substring(k+1);
            if (an.equals("x")) rect.setX(Double.parseDouble(av));
            else if (an.equals("y")) rect.setY(Double.parseDouble(av));
            else if (an.equals("w")) rect.setWidth(Double.parseDouble(av));
            else if (an.equals("h")) rect.setHeight(Double.parseDouble(av));
        }
        return rect;
    }
    private Transition parseTransitionCoordinates(String dispinfo) {
        Transition trans = new Transition();
        if (dispinfo!=null && dispinfo.length()>0) {
            String attrs[] = dispinfo.split(",");
            int k=0, lx = 0, ly=0;
            int[] xs = null, ys = null;

            String an, av;
            for (int i=0; i<attrs.length; i++) {
                k = attrs[i].indexOf('=');
                if (k<=0) continue;
                an = attrs[i].substring(0,k);
                av = attrs[i].substring(k+1);
                if (an.equals("lx")) {
                    lx = Integer.parseInt(av);
                } else if (an.equals("ly")) {
                    ly = Integer.parseInt(av);
                } else if (an.equals("xs")) {
                    if (av!=null && av.length()>0) {
                        String sts[] = av.split("&");
                        xs = new int[sts.length];
                        for (int j=0; j<xs.length; j++) {
                            xs[j] = Integer.parseInt(sts[j]);
                        }
                    }
                } else if (an.equals("ys")) {
                    if (av!=null && av.length()>0) {
                        String sts[] = av.split("&");
                        ys = new int[sts.length];
                        for (int j=0; j<ys.length; j++) {
                            ys[j] = Integer.parseInt(sts[j]);
                        }
                    }
                }
            }
            trans.setLx(lx);
            trans.setLy(ly);
            trans.setXs(xs);
            trans.setYs(ys);

         }
        return trans;
    }

    private class Transition {
        public int getLx() {
            return lx;
        }
        public void setLx(int lx) {
            this.lx = lx;
        }
        public int getLy() {
            return ly;
        }
        public void setLy(int ly) {
            this.ly = ly;
        }
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
        private int lx;
        private int ly;
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
         * @param x the x to set
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
         * @param y the y to set
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
         * @param width the width to set
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
         * @param height the height to set
         */
        public void setHeight(double height) {
            this.height = height;
        }

        private double x, y, width, height;
    }

    public static void main(String[] args) {

    }

}
