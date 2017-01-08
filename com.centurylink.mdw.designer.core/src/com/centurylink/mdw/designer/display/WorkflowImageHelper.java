/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.designer.display;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.dataaccess.DataAccess;
import com.centurylink.mdw.dataaccess.VersionControl;
import com.centurylink.mdw.designer.DesignerDataAccess;
import com.centurylink.mdw.designer.icons.IconFactory;
import com.centurylink.mdw.designer.utils.NodeMetaInfo;
import com.centurylink.mdw.designer.utils.ProcessWorker;
import com.centurylink.mdw.designer.utils.RestfulServer;
import com.centurylink.mdw.model.data.work.WorkStatuses;
import com.centurylink.mdw.model.value.process.ProcessInstanceVO;
import com.centurylink.mdw.model.value.process.ProcessVO;
import com.centurylink.mdw.model.value.work.ActivityInstanceVO;

/**
 * Replaces ImageServletHelper.  This is only to be accessed from within the server, and only for VCS Assets.
 */
public class WorkflowImageHelper {

    private ProcessVO process;
    public ProcessVO getProcess() { return process; }

    private ProcessInstanceVO processInstance;
    public ProcessInstanceVO getProcessInstance() { return processInstance; }
    public void setProcessInstance(ProcessInstanceVO processInstance) { this.processInstance = processInstance; }

    private List<ProcessInstanceVO> embeddedInstances;
    public List<ProcessInstanceVO> getEmbeddedInstances() { return embeddedInstances; }
    public void setEmbeddedInstances(List<ProcessInstanceVO> embedded) { this.embeddedInstances = embedded; }

    private Long selectedActivityInstanceId;
    public Long getSelectedActivityInstanceId() { return selectedActivityInstanceId; }
    public void setSelectedActivityInstanceId(Long instanceId) { this.selectedActivityInstanceId = instanceId; }

    private String selectedActivity; // logicalId
    public String getSelectedActivity() { return selectedActivity; }
    public void setSelectedActivity(String logicalId) { this.selectedActivity = logicalId; }

    public WorkflowImageHelper(ProcessVO process) {
        this.process = process;
    }

    /**
     * Generate a process instance image.
     */
    public BufferedImage getProcessImage() throws Exception {

        VersionControl vc = DataAccess.getAssetVersionControl(ApplicationContext.getAssetRoot());
        String serviceUrl = ApplicationContext.getServicesUrl();
        RestfulServer restfulServer = new RestfulServer("jdbc://dummy", "mdwapp", serviceUrl);
        restfulServer.setVersionControl(vc);
        restfulServer.setRootDirectory(ApplicationContext.getAssetRoot());
        DesignerDataAccess dao = new DesignerDataAccess(restfulServer, null, "mdwapp");
        // clone the process since it'll be converted to Designer
        Long processId = process.getId();
        process = new ProcessVO(process);
        process.setId(processId);

        return generateImage(dao);
    }

    /**
     * Generate a process instance image.
     */
    private BufferedImage generateImage(DesignerDataAccess dao) throws Exception {
        NodeMetaInfo metainfo = new NodeMetaInfo();
        metainfo.init(dao.getActivityImplementors(), DataAccess.currentSchemaVersion);
        new ProcessWorker().convert_to_designer(process);

        IconFactory iconFactory = new IconFactory();
        iconFactory.setDesignerDataAccess(dao);

        Graph graph = null;
        Node selectedNode = null;

        if (processInstance != null) {
            processInstance.setProcessName(process.getProcessName());
            graph = new Graph(process, processInstance, metainfo, iconFactory);
            graph.setStatus(new StringBuffer());
            if (embeddedInstances != null) {
                for (ProcessInstanceVO embeddedInstance : embeddedInstances) {
                    if (embeddedInstance.getStatusCode() == null && embeddedInstance.getStatus() != null)
                        embeddedInstance.setStatusCode(WorkStatuses.getCode(embeddedInstance.getStatus()));
                    Long embeddedProcId = new Long(embeddedInstance.getComment());
                    SubGraph subgraph = graph.getSubGraph(embeddedProcId);
                    if (subgraph != null) {
                        List<ProcessInstanceVO> insts = subgraph.getInstances();
                        if (insts == null) {
                            insts = new ArrayList<ProcessInstanceVO>();
                            subgraph.setInstances(insts);
                        }
                        insts.add(embeddedInstance);
                    }
                }
                for (SubGraph subgraph : graph.subgraphs)
                    subgraph.setStatus(new StringBuffer());
            }

            if (selectedActivityInstanceId != null) {
                ActivityInstanceVO selectedInstance = processInstance.getActivity(selectedActivityInstanceId);
                if (selectedInstance == null) {
                    for (SubGraph subgraph : graph.subgraphs) {
                        if (subgraph.getInstances() != null) {
                            for (ProcessInstanceVO subinst : subgraph.getInstances()) {
                                selectedInstance = subinst.getActivity(selectedActivityInstanceId);
                                if (selectedInstance != null)
                                    break;
                            }
                        }
                        if (selectedInstance != null)
                            break;
                    }
                }
                if (selectedInstance != null)
                    selectedNode = graph.getNode("A" + selectedInstance.getDefinitionId());
            }
        }
        else {
            graph = new Graph(process, metainfo, iconFactory);
            if (selectedActivity != null)
                selectedNode = graph.getNode(selectedActivity);
        }

        Dimension graphsize = graph.getGraphSize();
        WorkflowImage canvas = new WorkflowImage(graph, dao);
        if (selectedNode != null)
            canvas.setSelectedObject(selectedNode);

        int h_margin = 72, v_margin = 72;
        BufferedImage image = new BufferedImage(graphsize.width + h_margin,
                graphsize.height + v_margin, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        canvas.paintComponent(g2);
        g2.dispose();
        return image;
    }
}