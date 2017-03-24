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
package com.centurylink.mdw.designer.runtime;

import java.util.ArrayList;
import java.util.List;

import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import com.centurylink.mdw.designer.display.Graph;
import com.centurylink.mdw.model.value.process.ProcessInstanceVO;

public class ProcessInstanceTreeModel implements TreeModel {
    
    ProcessInstanceTreeNode root;
    TreeModelListener listener;
    private ProcessInstanceTreeNode currentProcess;
    
    public class ProcessInstanceTreeNode {
        private ProcessInstanceVO entry;
        private Graph graph;
        private ProcessInstanceTreeNode parent;
        private List<ProcessInstanceTreeNode> children;
        private ProcessInstancePage cachedPage;

        public ProcessInstanceTreeNode(ProcessInstanceVO entry) {
            this.entry = entry;
            graph = null;
            parent = null;
            children = null;
            cachedPage = null;
        }
//        public void clearChildren() { children = null; }
        public void setEntry(ProcessInstanceVO entry) { this.entry=entry; }
        public ProcessInstanceVO getEntry() { return entry; }
        public ProcessInstanceTreeNode addChild(ProcessInstanceTreeNode child) {
            if (children==null) {
                children = new ArrayList<ProcessInstanceTreeNode>();
            }
            int n = children.size();
            children.add(child);
            child.parent = this;
            return children.get(n);
        }
        public String toString(){
            if(entry!=null)
                return entry.getProcessName() + " " + entry.getId();
            else return super.toString();
        }
        public ProcessInstanceTreeNode findChild(ProcessInstanceVO entry) {
            if (children==null) return null;
            for (ProcessInstanceTreeNode child : children) {
                if (child.getEntry().equals(entry)) return child;
            }
            return null;
        }
        public ProcessInstanceTreeNode find(ProcessInstanceVO entry) {
            if (this.entry.equals(entry)) return this;
            if (children==null) return null;
            ProcessInstanceTreeNode found;
            for (ProcessInstanceTreeNode child : children) {
                found = child.find(entry);
                if (found!=null) return found;
            }
            return null;
        }
        public ProcessInstanceTreeNode find(Long procInstId, String server) {
            if (entry.getId().equals(procInstId) &&
                  (server==null&&entry.getRemoteServer()==null
                  ||server!=null&&server.equals(entry.getRemoteServer())))
                return this;
            if (children==null) return null;
            ProcessInstanceTreeNode found;
            for (ProcessInstanceTreeNode child : children) {
                found = child.find(procInstId, server);
                if (found!=null) return found;
            }
            return null;
        }
        public ProcessInstanceTreeNode find(Graph graph) {
            if (graph.equals(this.graph)) return this;
            if (children==null) return null;
            ProcessInstanceTreeNode found;
            for (ProcessInstanceTreeNode child : children) {
                found = child.find(graph);
                if (found!=null) return found;
            }
            return null;
        }
        public Graph getGraph() {
            return graph;
        }
        public void setGraph(Graph graph) {
            this.graph = graph;
        }
        public ProcessInstancePage getPage() {
        	return this.cachedPage;
        }
        public void setPage(ProcessInstancePage page) {
        	this.cachedPage = page;
        }
    }
    
    public ProcessInstanceTreeModel() {
        this.root = new ProcessInstanceTreeNode(null);
        this.listener = null;
        this.currentProcess = null;
    }

    public void addTreeModelListener(TreeModelListener l) {
        listener = l;
    }

    public Object getChild(Object parent, int index) {
        ProcessInstanceTreeNode node = (ProcessInstanceTreeNode)parent;
        if (node.children==null || node.children.size()<=index) return null;
        return node.children.get(index);
    }

    public int getChildCount(Object parent) {
        ProcessInstanceTreeNode node = (ProcessInstanceTreeNode)parent;
        return (node.children==null?0:node.children.size());
    }

    public int getIndexOfChild(Object parent, Object child) {
        ProcessInstanceTreeNode node = (ProcessInstanceTreeNode)parent;
        for (int i=0; i<node.children.size(); i++) {
            ProcessInstanceTreeNode one = node.children.get(i);
            if (node.equals(one)) return i;
        }
        return -1;
    }

    public ProcessInstanceTreeNode getRoot() {
        return root;
    }

    public boolean isLeaf(Object node) {
        ProcessInstanceTreeNode mynode = (ProcessInstanceTreeNode)node;
        return mynode.children==null || mynode.children.size()==0;
    }

    public void removeTreeModelListener(TreeModelListener l) {
        listener = null;    // ???
        
    }

    public void valueForPathChanged(TreePath path, Object newValue) {
    }
    
    public String getLabel() {
        if (root.entry==null) return "???";
        String procname = root.entry.getProcessName();
        String id = root.entry.getId().toString();
        String ownerid = root.entry.getMasterRequestId();
        return procname + " " + id + " [" + ownerid + "]";
    }
    
    public String getId() {
        return root.entry.getId().toString();
    }
    
    public String getMasterRequestId() {
        return root.entry.getMasterRequestId();
    }
    
    public ProcessInstanceTreeNode getCurrentProcess() {
        return currentProcess;
    }

    public void setCurrentProcess(ProcessInstanceTreeNode process) {
        currentProcess = process;
    }
    
    public ProcessInstanceTreeNode find(Long procInstId, String remoteServer) {
        return root.find(procInstId, remoteServer);
    }
    
    public TreePath getCurrentProcessPath() {
        ProcessInstanceTreeNode node = root.find(currentProcess.entry);
        if (node==null) return null;
        int length = 1;
        ProcessInstanceTreeNode parent = node.parent;
        while (parent!=null) {
            length++;
            parent = parent.parent;
        }
        ProcessInstanceTreeNode[] paths = new ProcessInstanceTreeNode[length];
        int i = length-1;
        paths[i--] = node;
        parent = node.parent;
        while (parent!=null) {
            paths[i--] = parent;
            parent = parent.parent;
        }
        return new TreePath(paths);
    }
}