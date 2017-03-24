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
package com.centurylink.mdw.plugin.project.extensions;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.project.assembly.ProjectUpdater;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;

public class LibExtension extends ExtensionModule {
    @Override
    public boolean addTo(WorkflowProject project, IProgressMonitor monitor)
            throws ExtensionModuleException, InterruptedException {
        monitor.worked(5);

        try {
            ProjectUpdater updater = new ProjectUpdater(project, MdwPlugin.getSettings());
            updater.addAppLibs(getZipFile(project), new SubProgressMonitor(monitor, 90));
            monitor.worked(5);
        }
        catch (InterruptedException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new ExtensionModuleException(ex.getMessage(), ex);
        }

        return true;
    }

    @Override
    public boolean removeFrom(WorkflowProject project, IProgressMonitor monitor)
            throws ExtensionModuleException, InterruptedException {
        monitor.worked(5);

        try {
            ProjectUpdater updater = new ProjectUpdater(project, MdwPlugin.getSettings());
            updater.removeAppLibs(getZipFile(project), new SubProgressMonitor(monitor, 90));
            monitor.worked(5);
        }
        catch (InterruptedException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new ExtensionModuleException(ex.getMessage(), ex);
        }

        return true;
    }

    @Override
    public boolean update(WorkflowProject project, IProgressMonitor monitor)
            throws ExtensionModuleException, InterruptedException {
        monitor.worked(5);

        try {
            ProjectUpdater updater = new ProjectUpdater(project, MdwPlugin.getSettings());
            updater.addAppLibs(getZipFile(project), new SubProgressMonitor(monitor, 90));
            monitor.worked(5);
        }
        catch (InterruptedException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new ExtensionModuleException(ex.getMessage(), ex);
        }

        return true;
    }

    protected String getZipFile(WorkflowProject project) {
        return getId() + "_" + (getVersion() == null ? project.getMdwVersion() : getVersion())
                + ".zip";
    }

}
