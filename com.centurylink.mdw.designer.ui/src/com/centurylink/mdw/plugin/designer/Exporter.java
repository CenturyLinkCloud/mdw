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
package com.centurylink.mdw.plugin.designer;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import org.apache.xmlbeans.XmlException;
import org.json.JSONException;

import com.centurylink.mdw.common.constant.OwnerType;
import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.utilities.timer.ActionCancelledException;
import com.centurylink.mdw.common.utilities.timer.ProgressMonitor;
import com.centurylink.mdw.dataaccess.ProcessPersister;
import com.centurylink.mdw.designer.DesignerDataAccess;
import com.centurylink.mdw.model.value.activity.ActivityImplementorVO;
import com.centurylink.mdw.model.value.process.PackageVO;
import com.centurylink.mdw.model.value.process.ProcessVO;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.plugin.designer.model.WorkflowPackage;

public class Exporter {
    private DesignerDataAccess designerDataAccess;

    public Exporter(DesignerDataAccess designerDataAccess) {
        this.designerDataAccess = designerDataAccess;
    }

    public String exportPackages(List<WorkflowPackage> packages, boolean exportJson,
            boolean includeTaskTemplates, ProgressMonitor progressMonitor)
            throws DataAccessException, RemoteException, ActionCancelledException, JSONException,
            XmlException {
        String export = null;

        progressMonitor.progress(10);

        int schemaVersion = designerDataAccess.getDatabaseSchemaVersion();
        List<PackageVO> pkgVos = new ArrayList<PackageVO>();
        for (WorkflowPackage pv : packages)
            pkgVos.add(pv.getPackageVO());
        export = designerDataAccess.exportPackages(pkgVos, schemaVersion, exportJson,
                includeTaskTemplates, progressMonitor);
        for (WorkflowPackage pv : packages)
            pv.setExported(true);

        return export;
    }

    public String exportPackage(WorkflowPackage packageVersion, boolean includeTaskTemplates,
            boolean inferReferencedImplementors, ProgressMonitor progressMonitor)
            throws DataAccessException, RemoteException, ActionCancelledException, XmlException {
        String xml = null;

        // auto-infer the implementors and save the package before exporting
        List<ActivityImplementorVO> implementors = packageVersion.getPackageVO().getImplementors();
        if (implementors == null)
            implementors = new ArrayList<ActivityImplementorVO>();
        if (inferReferencedImplementors) {
            progressMonitor.subTask("Finding referenced activity implementors");
            List<ActivityImplementorVO> inferredImplementors = designerDataAccess
                    .getReferencedImplementors(packageVersion.getPackageVO());
            for (ActivityImplementorVO inferred : inferredImplementors) {
                if (!implementors.contains(inferred))
                    implementors.add(inferred);
            }
        }

        if (progressMonitor.isCanceled())
            throw new ActionCancelledException();

        packageVersion.getPackageVO().setImplementors(implementors);

        progressMonitor.progress(10);

        progressMonitor.subTask("Saving package");
        designerDataAccess.savePackage(packageVersion.getPackageVO(),
                ProcessPersister.PersistType.UPDATE);
        if (progressMonitor.isCanceled())
            throw new ActionCancelledException();
        progressMonitor.progress(10);

        int schemaVersion = designerDataAccess.getDatabaseSchemaVersion();
        xml = designerDataAccess.exportPackage(packageVersion.getId(), schemaVersion,
                includeTaskTemplates, progressMonitor);
        packageVersion.setExported(true);

        return xml;
    }

    public String exportProcess(String name, String version, boolean oldNamespaces)
            throws DataAccessException, RemoteException, XmlException {
        int dotIdx = version.indexOf('.');
        int major = Integer.parseInt(version.substring(0, dotIdx));
        int minor = Integer.parseInt(version.substring(dotIdx + 1));
        // load the process for export
        ProcessVO procVO = designerDataAccess.getProcessDefinition(name, major * 1000 + minor);
        return designerDataAccess.exportProcess(procVO.getProcessId(), oldNamespaces);
    }

    public String exportAttributes(String prefix, WorkflowElement workflowElement,
            ProgressMonitor progressMonitor)
            throws DataAccessException, RemoteException, ActionCancelledException, XmlException {
        int schemaVersion = designerDataAccess.getDatabaseSchemaVersion();
        if (workflowElement instanceof WorkflowPackage)
            return designerDataAccess.exportAttributes(prefix, workflowElement.getId(),
                    schemaVersion, progressMonitor, OwnerType.PACKAGE);
        else
            return designerDataAccess.exportAttributes(prefix, workflowElement.getId(),
                    schemaVersion, progressMonitor, OwnerType.PROCESS);
    }

    public String exportTaskTemplates(WorkflowPackage packageVersion,
            ProgressMonitor progressMonitor)
            throws DataAccessException, RemoteException, ActionCancelledException {
        return designerDataAccess.exportTaskTemplates(packageVersion.getId(), progressMonitor);
    }
}
