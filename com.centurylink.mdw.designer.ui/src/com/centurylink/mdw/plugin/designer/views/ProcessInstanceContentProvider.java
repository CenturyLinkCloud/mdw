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
package com.centurylink.mdw.plugin.designer.views;

import java.util.List;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

import com.centurylink.mdw.model.value.process.ProcessInstanceVO;
import com.centurylink.mdw.model.value.process.ProcessList;
import com.centurylink.mdw.plugin.designer.DesignerProxy;
import com.centurylink.mdw.plugin.designer.model.ProcessInstanceFilter;
import com.centurylink.mdw.plugin.designer.model.ProcessInstanceSort;
import com.centurylink.mdw.plugin.designer.model.WorkflowProcess;

public class ProcessInstanceContentProvider implements IStructuredContentProvider {
    private ProcessInstanceFilter filter = new ProcessInstanceFilter();

    public ProcessInstanceFilter getFilter() {
        return filter;
    }

    public void setFilter(ProcessInstanceFilter pif) {
        this.filter = pif;
    }

    private ProcessInstanceSort sort = new ProcessInstanceSort();

    public ProcessInstanceSort getSort() {
        return sort;
    }

    public void setProcessInstanceSort(ProcessInstanceSort pis) {
        this.sort = pis;
    }

    private List<ProcessInstanceVO> instanceInfo;

    public List<ProcessInstanceVO> getInstanceInfo() {
        return instanceInfo;
    }

    public void setInstanceInfo(List<ProcessInstanceVO> pii) {
        this.instanceInfo = pii;
    }

    private Long instanceCount = new Long(0);

    public Long getInstanceCount() {
        return instanceCount;
    }

    public void setInstanceCount(Long count) {
        this.instanceCount = count;
    }

    private Integer pageIndex = new Integer(1);

    public Integer getPageIndex() {
        return pageIndex;
    }

    public void setPageIndex(Integer index) {
        this.pageIndex = index;
    }

    public Object[] getElements(Object inputElement) {
        WorkflowProcess processVersion = (WorkflowProcess) inputElement;

        if (instanceInfo == null) {
            DesignerProxy designerProxy = processVersion.getProject().getDesignerProxy();
            ProcessList instanceList = designerProxy.getProcessInstanceList(processVersion,
                    filter.getProcessCriteria(), filter.getVariableCriteria(processVersion),
                    pageIndex, filter.getPageSize(), sort.getOrderBy());
            instanceCount = instanceList.getTotal();
            instanceInfo = instanceList.getItems();
        }

        return instanceInfo.toArray(new ProcessInstanceVO[0]);
    }

    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        if (newInput == null)
            instanceCount = new Long(0);
    }

    public void dispose() {
    }
}
