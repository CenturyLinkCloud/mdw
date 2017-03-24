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
package com.centurylink.mdw.plugin.server;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

@SuppressWarnings("restriction")
public class Tomcat70Configuration
        extends org.eclipse.jst.server.tomcat.core.internal.Tomcat70Configuration {
    public Tomcat70Configuration(IFolder path) {
        super(path);
    }

    @Override
    public void load(IFolder folder, IProgressMonitor monitor) throws CoreException {
        super.load(folder, monitor);
    }

    @Override
    public IStatus cleanupServer(IPath baseDir, IPath installDir, boolean removeKeptContextFiles,
            IProgressMonitor monitor) {
        return super.cleanupServer(baseDir, installDir, removeKeptContextFiles, monitor);
    }

    @Override
    public IStatus backupAndPublish(IPath tomcatDir, boolean doBackup, IProgressMonitor monitor) {
        return super.backupAndPublish(tomcatDir, doBackup, monitor);
    }
}
