/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.designer.display;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;

import com.centurylink.mdw.common.constant.OwnerType;
import com.centurylink.mdw.common.query.QueryRequest;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.dataaccess.DataAccess;
import com.centurylink.mdw.designer.DesignerDataAccess;
import com.centurylink.mdw.designer.icons.IconFactory;
import com.centurylink.mdw.designer.runtime.RunTimeDesignerImage;
import com.centurylink.mdw.designer.utils.NodeMetaInfo;
import com.centurylink.mdw.designer.utils.ProcessWorker;
import com.centurylink.mdw.designer.utils.Server;
import com.centurylink.mdw.model.value.process.ProcessInstanceVO;
import com.centurylink.mdw.model.value.process.ProcessVO;

/**
 * @deprecated
 * @see WorkflowImageHelper
 *
 */
@Deprecated
public class ImageServletHelper  {

    /**
     * Generate a process image from the designer server.
     */
    public BufferedImage generateImageForTaskInstance(Long pTaskInstId)
      throws ServletException {

        try{
        	DesignerDataAccess dao = new DesignerDataAccess(new Server(), null, "onServer");
        	ProcessInstanceVO pi = dao.getCauseForTaskInstance(pTaskInstId);
            if(pi == null){
                throw new ServletException("Failed to locate the ProcessInstance for TaskInstanceId:"+pTaskInstId);
            }
            return generateImage(pi, dao);
        }catch(Exception ex){
            log(ex);
	    	throw new ServletException(ex);
        }
    }

     /**
     * Generate a process image from the designer server.
     */
    public BufferedImage generateImageForProcessInstance(Long pProcInstId)
      throws ServletException {

        try{
        	DesignerDataAccess dao = new DesignerDataAccess(new Server(), null, "onServer");
        	ProcessInstanceVO pi = dao.getProcessInstanceBase(pProcInstId,null);
            if(pi == null){
                throw new ServletException("Failed to locate the ProcessInstance for ProcessInstanceId:"+pProcInstId);
            }
            return generateImage(pi, dao);
        }catch(Exception ex){
            log(ex);
	    	throw new ServletException(ex);
        }
    }


    /**
     * Generate a process image from the designer server.
     */
    public BufferedImage generateImageForSecondaryOwner(String pSecOwner, Long pSecOwnerId)
      throws ServletException {

        try{
        	DesignerDataAccess dao = new DesignerDataAccess(new Server(), null, "onServer");
        	ProcessInstanceVO pi = dao.getProcessInstanceForSecondary(pSecOwner, pSecOwnerId);
            if(pi == null){
                throw new ServletException("Failed to locate the ProcessInstance for Secondary Owner and OwnerId");
            }
            return generateImage(pi, dao);
        }catch(Exception ex){
            log(ex);
	    	throw new ServletException(ex);
        }



    }

    /**
     * Generate a process image from the designer server.
     */
    private BufferedImage generateImage(ProcessInstanceVO pProcInst, DesignerDataAccess dao)
          throws ServletException {
        try {
            log("Loading Process " + pProcInst.getProcessId());
        	NodeMetaInfo metainfo = new NodeMetaInfo();
        	metainfo.init(dao.getActivityImplementors(), DataAccess.currentSchemaVersion);
            ProcessVO processVO = dao.getProcess(new Long(pProcInst.getProcessId()),null);
            new ProcessWorker().convert_to_designer(processVO);
            pProcInst.setProcessName(processVO.getProcessName());
            Graph graph = loadCompletionMap(processVO, pProcInst, dao, metainfo);
            Dimension graphsize = graph.getGraphSize();
            RunTimeDesignerImage canvas = new RunTimeDesignerImage(graph, dao);
            int h_margin = 72, v_margin = 72;
            BufferedImage image = new BufferedImage(graphsize.width+h_margin,
                    graphsize.height+v_margin, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2 = image.createGraphics();
            g2.setBackground(Color.WHITE);
            g2.clearRect(0, 0, image.getWidth(), image.getHeight());
            canvas.paintComponent(g2);
            g2.dispose();
            return image;
        }
        catch (Throwable t) {
            log(t);
            if (t.getCause() != null) {
            	log(t.getCause());
            }
            throw new ServletException(t);
        }
    }

    private Graph loadCompletionMap(ProcessVO procdef,
    		ProcessInstanceVO processInstance, DesignerDataAccess dao, NodeMetaInfo metainfo) throws Exception {
    	ProcessInstanceVO fullInfo = dao.getProcessInstanceAll(processInstance.getId(),procdef);
        processInstance.copyFrom(fullInfo);
        IconFactory iconFactory = new IconFactory();
        iconFactory.setDesignerDataAccess(dao);
        Graph graph = new Graph(procdef, processInstance, metainfo, iconFactory);
        if (graph.subgraphs!=null) {
        	if (procdef.isInRuleSet()) {
        		Map<String,String> pMap = new HashMap<String,String>();
            	pMap.put("owner", OwnerType.MAIN_PROCESS_INSTANCE);
            	pMap.put("ownerId", processInstance.getId().toString());
            	pMap.put("processId", procdef.getProcessId().toString());
            	List<ProcessInstanceVO> childProcessInstances =
            		dao.getProcessInstanceList(pMap, 0, QueryRequest.ALL_ROWS, procdef, null).getItems();
            	for (ProcessInstanceVO childInst : childProcessInstances) {
            		ProcessInstanceVO fullChildInfo  =
            			dao.getProcessInstanceAll(childInst.getId(),procdef);
            		childInst.copyFrom(fullChildInfo);
            		Long subprocId = new Long(childInst.getComment());
            		for (SubGraph subgraph : graph.subgraphs) {
            			if (subgraph.getProcessVO().getProcessId().equals(subprocId)) {
            				List<ProcessInstanceVO> insts = subgraph.getInstances();
            				if (insts==null) {
            					insts = new ArrayList<ProcessInstanceVO>();
            					subgraph.setInstances(insts);
            				}
            				insts.add(childInst);
            				break;
            			}
            		}
            	}
        	} else {
        		for (SubGraph subgraph : graph.subgraphs) {
            		Map<String,String> pMap = new HashMap<String,String>();
                	pMap.put("owner", OwnerType.PROCESS_INSTANCE);
                	pMap.put("ownerId", processInstance.getId().toString());
                	pMap.put("processId", subgraph.getProcessVO().getProcessId().toString());
                	List<ProcessInstanceVO> childProcessInstances =
                		dao.getProcessInstanceList(pMap, 0, QueryRequest.ALL_ROWS, procdef, null).getItems();
                	subgraph.setInstances(childProcessInstances);
                	for (ProcessInstanceVO childInst : childProcessInstances) {
                		ProcessInstanceVO fullChildInfo =
                			dao.getProcessInstanceAll(childInst.getId(),procdef);
                		childInst.copyFrom(fullChildInfo);
                	}
        		}
            }
        }
        graph.setStatus(new StringBuffer());
        return graph;
    }

    private void log(String msg) {
    	StandardLogger logger = LoggerUtil.getStandardLogger();
    	if (logger.isMdwDebugEnabled()) logger.mdwDebug(msg);
    }

    private void log(Throwable t) {
        StandardLogger logger = LoggerUtil.getStandardLogger();
    	logger.severeException(t.getMessage(), t);
    }


}