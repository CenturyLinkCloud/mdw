/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.designer.runtime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.centurylink.mdw.common.constant.OwnerType;
import com.centurylink.mdw.common.query.QueryRequest;
import com.centurylink.mdw.designer.DesignerDataAccess;
import com.centurylink.mdw.designer.display.Graph;
import com.centurylink.mdw.designer.display.SubGraph;
import com.centurylink.mdw.designer.pages.DesignerPage;
import com.centurylink.mdw.designer.runtime.ProcessInstanceTreeModel.ProcessInstanceTreeNode;
import com.centurylink.mdw.model.value.process.ProcessInstanceVO;
import com.centurylink.mdw.model.value.process.ProcessVO;

public class ProcessInstanceLoader {

    private ProcessInstanceVO processInstance;
    private DesignerPage page;
    private String errmsg = null;

    public ProcessInstanceLoader(ProcessInstanceVO processInstance, DesignerPage page) {
        this.processInstance = processInstance;
        this.page = page;
    }

    public Graph loadCompletionMap(ProcessVO procdef, ProcessInstanceVO processInstance, DesignerDataAccess dao) throws Exception {
    	ProcessInstanceVO fullInfo = dao.getProcessInstanceAll(processInstance.getId(), procdef);
    	processInstance.copyFrom(fullInfo);
    	Graph graph = new Graph(procdef, processInstance, page.getDataModel().getNodeMetaInfo(), page.frame.getIconFactory());
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
                    	ProcessInstanceVO fullChildInfo  =
                            dao.getProcessInstanceAll(childInst.getId(), procdef);
                        childInst.copyFrom(fullChildInfo);
                    }
                }
        	}
    	}
    	StringBuffer errmsg_buffer = new StringBuffer();
    	graph.setStatus(errmsg_buffer);
    	if (errmsg_buffer.length()>0) {
    		errmsg = "There are runtime information about activities and transitions\n"
    			+ " that are not found in process definition.\n"
    			+ "This may happen when the process definition has been modified.\n"
    			+ errmsg_buffer.toString();
    	}
    	return graph;
    }

    private ProcessInstanceTreeModel createOrUpdateModelRecursive(ProcessInstanceVO procInst)
            throws Exception {
        ProcessInstanceTreeModel model;
        ProcessInstanceTreeNode node;
        if (OwnerType.PROCESS_INSTANCE.equals(procInst.getOwner())) {
            // parent is a process instance
            model = page.getDataModel().findInstanceTreeAndNode(
                    procInst.getOwnerId(),
                    procInst.getRemoteServer(),
                    procInst.getMasterRequestId());
            if (model==null) {
            	ProcessInstanceVO parentInst = page.frame.dao.
                    getProcessInstanceBase(procInst.getOwnerId(), procInst.getRemoteServer());
                model = createOrUpdateModelRecursive(parentInst);
            } else {
            }
            ProcessInstanceTreeNode parentNode = model.getCurrentProcess();
            node = parentNode.addChild(model.new ProcessInstanceTreeNode(procInst));
            model.setCurrentProcess(node);
        } else {        // top level node
            model = new ProcessInstanceTreeModel();
            node = model.getRoot();
            node.setEntry(procInst);
            model.setCurrentProcess(node);
        }
        return model;
    }

    public ProcessInstanceTreeModel createOrUpdateModel(ProcessInstanceTreeModel model) throws Exception {
        ProcessInstanceTreeNode node;
        if (model!=null) {
            node = model.getCurrentProcess();
            if (processInstance!=node.getEntry()) {
                node.getEntry().copyFrom(processInstance);
                processInstance = node.getEntry();
            }
        } else {
            model = createOrUpdateModelRecursive(processInstance);
        }
        return model;
    }

    public String getErrorMessage() {
        return errmsg;
    }
}

