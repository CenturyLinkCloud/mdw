/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.designer.display;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.dataaccess.DataAccess;
import com.centurylink.mdw.dataaccess.VersionControl;
import com.centurylink.mdw.designer.DesignerDataAccess;
import com.centurylink.mdw.designer.icons.IconFactory;
import com.centurylink.mdw.designer.utils.NodeMetaInfo;
import com.centurylink.mdw.designer.utils.ProcessWorker;
import com.centurylink.mdw.designer.utils.RestfulServer;
import com.centurylink.mdw.model.value.process.ProcessInstanceVO;
import com.centurylink.mdw.model.value.process.ProcessVO;

/**
 * Replaces ImageServletHelper.  This is only to be accessed from within the server, and only for VCS Assets.
 */
public class WorkflowImageHelper {

    public BufferedImage getProcessImage(ProcessVO process) throws Exception {
        return getProcessImage(process, null);
    }

    /**
     * Generate a process instance image.
     */
    public BufferedImage getProcessImage(ProcessVO process, ProcessInstanceVO processInstance) throws Exception {

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

        return generateImage(process, processInstance, dao);
    }

    /**
     * Generate a process instance image.
     */
    private BufferedImage generateImage(ProcessVO process, ProcessInstanceVO processInstance, DesignerDataAccess dao)
    throws Exception {
        NodeMetaInfo metainfo = new NodeMetaInfo();
        metainfo.init(dao.getActivityImplementors(), DataAccess.currentSchemaVersion);
        new ProcessWorker().convert_to_designer(process);

        IconFactory iconFactory = new IconFactory();
        iconFactory.setDesignerDataAccess(dao);

        Graph graph = null;
        if (processInstance != null) {
            processInstance.setProcessName(process.getProcessName());
            graph = new Graph(process, processInstance, metainfo, iconFactory);
            graph.setStatus(new StringBuffer());
        }
        else {
            graph = new Graph(process, metainfo, iconFactory);
        }

        Dimension graphsize = graph.getGraphSize();
        WorkflowImage canvas = new WorkflowImage(graph, dao);
        int h_margin = 72, v_margin = 72;
        BufferedImage image = new BufferedImage(graphsize.width + h_margin,
                graphsize.height + v_margin, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        canvas.paintComponent(g2);
        g2.dispose();
        return image;
    }
}