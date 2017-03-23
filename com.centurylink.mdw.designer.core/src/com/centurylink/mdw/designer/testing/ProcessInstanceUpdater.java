/*
 * Copyright (C) 2017 CenturyLink, Inc.
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
package com.centurylink.mdw.designer.testing;

import java.awt.Rectangle;
import java.util.ArrayList;

import com.centurylink.mdw.designer.MainFrame;
import com.centurylink.mdw.designer.display.Graph;
import com.centurylink.mdw.designer.display.Link;
import com.centurylink.mdw.designer.display.Node;
import com.centurylink.mdw.designer.display.SubGraph;
import com.centurylink.mdw.designer.pages.DesignerPage;
import com.centurylink.mdw.designer.runtime.ProcessInstancePage;
import com.centurylink.mdw.model.data.work.WorkStatus;
import com.centurylink.mdw.model.data.work.WorkTransitionStatus;
import com.centurylink.mdw.model.value.process.ProcessInstanceVO;
import com.centurylink.mdw.model.value.work.ActivityInstanceVO;
import com.centurylink.mdw.model.value.work.WorkTransitionInstanceVO;

public class ProcessInstanceUpdater {
    
    private MainFrame frame;
    private ProcessInstancePage processInstancePage;
    public ProcessInstancePage getProcessInstancePage() { return processInstancePage; }
    public void setProcessInstancePage(ProcessInstancePage page) { this.processInstancePage = page; }
    
    public ProcessInstanceUpdater(ProcessInstancePage processInstancePage) {
        this.processInstancePage = processInstancePage;
        if (processInstancePage != null)
          this.frame = processInstancePage.frame;
    }
    
    public void handleMessage(Long procId, Long procInstId, String subtype, String time, String id, String msg) {
        if (subtype.equals("m")) {
            updateProcessInstance(processInstancePage, procInstId, new Long(procId), msg);
        } else if (subtype.equals("a") && processInstancePage!=null) {
            updateActivityInstance(processInstancePage, new Long(procId), new Long(procInstId), time, id, msg);
        } else if (subtype.equals("t") && processInstancePage!=null) {
            updateTransitionInstance(processInstancePage, new Long(procId), new Long(procInstId), time, id, msg);
        }
    }
    
    private void updateProcessInstance(ProcessInstancePage procInstPage, Long procInstId,
            Long procId, String msg) {
        ProcessInstanceVO processInstance = procInstPage.getProcessInstance();
        if (!isShowingThisInstance(processInstance.getId())) showInstance(procInstPage);
        if (!processInstance.getId().equals(procInstId)) {
            if (msg.startsWith(WorkStatus.LOGMSG_PROC_COMPLETE)) {
                Graph graph = procInstPage.getProcess();
                if (graph.subgraphs!=null) {
                    for (SubGraph s : graph.subgraphs) {
                    	// try every subgraph
                    	s.setInstanceStatus(procInstId, WorkStatus.STATUS_COMPLETED);
                    }
                }
            } else if (msg.startsWith(WorkStatus.LOGMSG_PROC_START)) {
            	int k = msg.indexOf("(embedded process ");
            	if (k>0) {
            		int k1 = k + "(embedded process ".length();
            		int k2 = msg.indexOf(')', k1);
            		Long subprocId = new Long(msg.substring(k1,k2));
            		for (SubGraph subgraph : procInstPage.getProcess().subgraphs) {
            			if (subgraph.getProcessVO().getProcessId().equals(subprocId)) {
            				ProcessInstanceVO pi = null;
            				if (subgraph.getInstances()!=null) {
            					for (ProcessInstanceVO pi0 : subgraph.getInstances()) {
            						if (pi0.getId().equals(procInstId)) {
            							pi = pi0;
            							break;
            						}
            					}
            				} else subgraph.setInstances(new ArrayList<ProcessInstanceVO>());
            				if (pi==null) {
            					pi = new ProcessInstanceVO(procId, subgraph.getName());
            					pi.setId(procInstId);
            					pi.setStatusCode(WorkStatus.STATUS_IN_PROGRESS);
            					pi.setActivities(new ArrayList<ActivityInstanceVO>());
            					pi.setTransitions(new ArrayList<WorkTransitionInstanceVO>());
            					subgraph.getInstances().add(pi);
            				}
                        	subgraph.setInstanceStatus(procInstId, WorkStatus.STATUS_IN_PROGRESS);
            				break;
            			}
            		}
            	}
            }
        }
    }
    
    private void updateActivityInstance(ProcessInstancePage procInstPage, Long procId, Long procInstId,
            String timestr, String id, String msg) {
        ProcessInstanceVO procInst = procInstPage.getProcessInstance();
        if (!isShowingThisInstance(procInst.getId())) showInstance(procInstPage);
        int k = id.indexOf('.');
        String actId = id.substring(0,k);
        String actInstId = id.substring(k+1);
        Node node;
        if (!procInst.getId().equals(procInstId)) {		// embedded process
        	node = this.findNodeInSubgraph(procInstPage.getProcess(), new Long(actId));
            procInst = this.findEmbeddedProcessInstance(procInstPage.getProcess(), procInstId);
        } else {
        	node = procInstPage.getProcess().findNode(new Long(actId));
        }
        if (node!=null) {
            ActivityInstanceVO actInst = null;
            for (ActivityInstanceVO ai : procInst.getActivities()) {
                if (ai.getId().toString().equals(actInstId)) {
                    actInst = ai;
                    break;
                }
            }
            if (actInst==null) {
                actInst = new ActivityInstanceVO();
                procInst.getActivities().add(actInst);
                actInst.setId(new Long(actInstId));
                actInst.setDefinitionId(new Long(actId));
                actInst.setOwnerId(procInst.getId());
                actInst.setStartDate(timestr);
                actInst.setStatusCode(WorkStatus.STATUS_IN_PROGRESS);
                node.addInstance(actInst, false);
            }
            if (msg.startsWith(WorkStatus.LOGMSG_COMPLETE)) {
                actInst.setEndDate(timestr);
                actInst.setStatusCode(WorkStatus.STATUS_COMPLETED);
                node.setInstanceStatus(actInst);
            } else if (msg.startsWith(WorkStatus.LOGMSG_START)) {
                // do nothing - already loaded by code above or by full load
            } else if (msg.startsWith(WorkStatus.LOGMSG_FAILED)) {
                actInst.setEndDate(timestr);
                actInst.setStatusCode(WorkStatus.STATUS_FAILED);
                node.setInstanceStatus(actInst);
            } else if (msg.startsWith(WorkStatus.LOGMSG_SUSPEND)) {
                actInst.setStatusCode(WorkStatus.STATUS_WAITING);
                node.setInstanceStatus(actInst);
            } else if (msg.startsWith(WorkStatus.LOGMSG_HOLD)) {
                actInst.setStatusCode(WorkStatus.STATUS_HOLD);
                node.setInstanceStatus(actInst);
            } else {
                // System.out.println("How to handle activity message '" + msg +"'?");
            }
            Rectangle aRect = new java.awt.Rectangle(node.x-10,node.y-10,node.w+20,node.h+30);
            procInstPage.canvas.scrollRectToVisible(aRect);
        } else {
            System.out.println("Cannot find node with ID " + actId);
        }
        procInstPage.repaint();
    }

    private void updateTransitionInstance(ProcessInstancePage procInstPage, Long procId, Long procInstId,
            String timestr, String id, String msg) {
    	ProcessInstanceVO procInst = procInstPage.getProcessInstance();
        if (!isShowingThisInstance(procInst.getId())) this.showInstance(procInstPage);
        int k = id.indexOf('.');
        String transId = id.substring(0,k);
        String transInstId = id.substring(k+1);
        Link link;
        if (!procInst.getId().equals(procInstId)) {		// embedded process
            link = findLinkInSubgraph(procInstPage.getProcess(), new Long(transId));
            procInst = this.findEmbeddedProcessInstance(procInstPage.getProcess(), procInstId);
        } else {
            link = procInstPage.getProcess().findLink(new Long(transId));
        }
        if (link!=null) {
            WorkTransitionInstanceVO transInst = null;
            for (WorkTransitionInstanceVO ti : procInst.getTransitions()) {
                if (ti.getTransitionInstanceID().toString().equals(transInstId)) {
                    transInst = ti;
                    break;
                }
            }
            //          SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd.HH:mm:ss.SSS");
            //          Date time = df.parse(timestr);
            if (transInst==null) {
                transInst = new WorkTransitionInstanceVO();
                procInst.getTransitions().add(transInst);
                transInst.setTransitionID(new Long(transId));
                transInst.setTransitionInstanceID(new Long(transInstId));
                transInst.setProcessInstanceID(procInst.getId());
                transInst.setStartDate(timestr);
                transInst.setEndDate(timestr);
                transInst.setStatusCode(WorkTransitionStatus.STATUS_COMPLETED);
                link.addInstance(transInst);
            }
        } else {
            System.out.println("Cannot find link with ID " + transId);
        }
        procInstPage.repaint();
    }

    protected boolean isShowingThisInstance(Long mainProcInstId) {
        DesignerPage current_page = frame.getCurrentPage();
        if (current_page instanceof ProcessInstancePage) {
        	ProcessInstanceVO procInst = ((ProcessInstancePage)current_page).getProcessInstance();
            if (procInst==null) return false;
            return mainProcInstId.equals(procInst.getId());
        } else return false;
    }
    
    protected Node findNodeInSubgraph(Graph graph, Long actId) {
        for (SubGraph subgraph : graph.subgraphs) {
        	Node node = subgraph.findNode(actId);
        	if (node!=null) return node;
        }
        return null;
    }
    
    protected Link findLinkInSubgraph(Graph graph, Long linkId) {
        for (SubGraph subgraph : graph.subgraphs) {
        	Link link = subgraph.findLink(linkId);
        	if (link!=null) return link;
        }
        return null;
    }
    
    protected ProcessInstanceVO findEmbeddedProcessInstance(Graph graph, Long procInstId) {
    	for (SubGraph subgraph : graph.subgraphs) {
    		if (subgraph.getInstances()==null) continue;
    		for (ProcessInstanceVO pi : subgraph.getInstances()) {
    			if (pi.getId().equals(procInstId)) return pi;
    		}
    	}
        return null;
    }   
    
    protected void showInstance(ProcessInstancePage procInstPage) {
        frame.setPage(procInstPage);
    }

}
