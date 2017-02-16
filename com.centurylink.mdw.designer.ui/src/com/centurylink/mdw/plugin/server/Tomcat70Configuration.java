/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
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
