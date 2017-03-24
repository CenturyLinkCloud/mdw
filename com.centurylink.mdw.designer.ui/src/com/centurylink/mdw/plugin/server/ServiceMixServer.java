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

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jst.server.core.IWebModule;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IModuleType;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.model.ServerDelegate;

import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;

public class ServiceMixServer extends ServerDelegate {
    public static final String ID_PREFIX = "com.centurylink.server.servicemix";

    public static final String LOCATION = "location";
    public static final String SERVER_PORT = "serverPort";
    public static final String SSH_PORT = "sshPort";
    public static final String USER = "user";
    public static final String PASSWORD = "password";

    private List<BuildFileChangeListener> buildFileChangeListeners = new ArrayList<BuildFileChangeListener>();

    @Override
    protected void initialize() {
        super.initialize();
        for (IModule module : getServer().getModules()) {
            IFile buildFile = getBuildFile(module);
            if (buildFile.exists()) {
                BuildFileChangeListener listener = new BuildFileChangeListener(buildFile);
                ResourcesPlugin.getWorkspace().addResourceChangeListener(listener,
                        IResourceChangeEvent.POST_CHANGE);
                buildFileChangeListeners.add(listener);
            }
        }
    }

    @Override
    public IStatus canModifyModules(IModule[] add, IModule[] remove) {
        if (add != null) {
            int size = add.length;
            for (int i = 0; i < size; i++) {
                IModule module = add[i];
                if (!"jst.web".equals(module.getModuleType().getId())
                        && !"jst.utility".equals(module.getModuleType().getId()))
                    return new Status(IStatus.ERROR, MdwPlugin.PLUGIN_ID, 0,
                            "Web and Utility modules only", null);
            }
        }

        return Status.OK_STATUS;
    }

    @Override
    public IModule[] getChildModules(IModule[] module) {
        if (module == null)
            return null;

        IModuleType moduleType = module[0].getModuleType();

        if (module.length == 1 && moduleType != null && "jst.web".equals(moduleType.getId())) {
            IWebModule webModule = (IWebModule) module[0].loadAdapter(IWebModule.class, null);
            if (webModule != null)
                return webModule.getModules();
        }
        return new IModule[0];
    }

    @SuppressWarnings("restriction")
    @Override
    public IModule[] getRootModules(IModule module) throws CoreException {
        if ("jst.web".equals(module.getModuleType().getId())
                || "jst.utility".equals(module.getModuleType().getId())) {
            IStatus status = canModifyModules(new IModule[] { module }, null);
            if (status == null || !status.isOK())
                throw new CoreException(status);
            return new IModule[] { module };
        }

        return org.eclipse.jst.server.core.internal.J2EEUtil.getWebModules(module, null);
    }

    @Override
    public void modifyModules(IModule[] add, IModule[] remove, IProgressMonitor monitor)
            throws CoreException {
        for (IModule module : add) {
            IFile buildFile = getBuildFile(module);
            if (buildFile.exists()) {
                BuildFileChangeListener listener = new BuildFileChangeListener(buildFile);
                ResourcesPlugin.getWorkspace().addResourceChangeListener(listener,
                        IResourceChangeEvent.POST_CHANGE);
                buildFileChangeListeners.add(listener);
            }
        }
        for (IModule module : remove) {
            IFile buildFile = getBuildFile(module);
            if (buildFile.exists()) {
                for (BuildFileChangeListener listener : buildFileChangeListeners) {
                    if (listener.buildFile.equals(buildFile))
                        ResourcesPlugin.getWorkspace().removeResourceChangeListener(listener);
                }
            }
        }
    }

    private IFile getBuildFile(IModule module) {
        IFile buildFile = module.getProject().getFile("build.gradle");
        if (!buildFile.exists())
            buildFile = module.getProject().getFile("pom.xml");
        return buildFile;
    }

    public String getName() {
        return getServer().getName();
    }

    public void setName(String name) {
        getServerWorkingCopy().setName(name);
    }

    public String getLocation() {
        return getAttribute(LOCATION, "");
    }

    public void setLocation(String location) {
        setAttribute(LOCATION, location);
    }

    public String getHost() {
        return getServer().getHost();
    }

    public void setHost(String host) {
        getServerWorkingCopy().setHost(host);
    }

    public int getPort() {
        return getAttribute(SERVER_PORT, 8181);
    }

    public void setPort(int port) {
        setAttribute(SERVER_PORT, port);
    }

    public int getSshPort() {
        return getAttribute(SSH_PORT, 0);
    }

    public void setSshPort(int port) {
        setAttribute(SSH_PORT, port);
    }

    public String getUser() {
        return getAttribute(USER, "");
    }

    public void setUser(String user) {
        setAttribute(USER, user);
    }

    public String getPassword() {
        return getAttribute(PASSWORD, "");
    }

    public void setPassword(String password) {
        setAttribute(PASSWORD, password);
    }

    public String validateServerLoc() {
        String msg = null;
        String location = getLocation();
        if (location == null || location.isEmpty())
            msg = "";
        else {
            File locationFile = new File(location);
            if (!locationFile.exists() || !locationFile.isDirectory())
                msg = "Location must be an existing directory";
            else if (!new File(locationFile + "/bin/karaf.bat").exists()
                    && !new File(locationFile + "/bin/karaf.sh").exists())
                msg = "Location must contain bin/karaf.bat or bin/karaf.sh";
        }
        return msg;
    }

    public IStatus validate() {
        String msg = null;

        // check server loc
        msg = validateServerLoc();

        if (msg == null) {
            // check port
            if (getPort() == 0)
                msg = "";
        }

        if (msg == null) {
            // check ssh port
            if (getSshPort() == 0)
                msg = "";
            else {
                String sshPort = readSshPortProp();
                if (sshPort != null && !sshPort.equals(String.valueOf(getSshPort()))) {
                    String warn = "SSH Port (" + getSshPort()
                            + ") does not match value specified in the sshPort property in\n"
                            + new File(getLocation() + "/etc/org.apache.karaf.shell.cfg") + " ("
                            + sshPort + ").";
                    return new Status(IStatus.WARNING, MdwPlugin.PLUGIN_ID, 0, warn, null);
                }
            }
        }

        if (msg == null) {
            // check user
            if (getUser() == null || getUser().isEmpty())
                msg = "";
        }

        if (msg == null) {
            if (getPassword() == null || getPassword().isEmpty())
                msg = "";
        }

        if (msg == null)
            return Status.OK_STATUS;
        else
            return new Status(IStatus.ERROR, MdwPlugin.PLUGIN_ID, 0, msg, null);
    }

    public void save() throws CoreException {
        getServerWorkingCopy().save(true, null);
    }

    IRuntime getRuntime() {
        return getServer().getRuntime();
    }

    @Override
    public void dispose() {
        super.dispose();
        for (BuildFileChangeListener listener : buildFileChangeListeners)
            ResourcesPlugin.getWorkspace().removeResourceChangeListener(listener);
        buildFileChangeListeners.clear();
    }

    String readSshPortProp() {
        String sshPort = null;
        File karafShellProps = new File(getLocation() + "/etc/org.apache.karaf.shell.cfg");
        if (karafShellProps.exists()) {
            Properties shellProps = new Properties();
            try {
                shellProps.load(new FileInputStream(karafShellProps));
                sshPort = shellProps.getProperty("sshPort");
            }
            catch (Exception ex) {
                PluginMessages.log(ex);
            }
        }
        return sshPort;
    }

    class BuildFileChangeListener implements IResourceChangeListener {
        private IFile buildFile;

        BuildFileChangeListener(IFile buildFile) {
            this.buildFile = buildFile;
        }

        @SuppressWarnings("restriction")
        public void resourceChanged(IResourceChangeEvent event) {
            if (event.getType() == IResourceChangeEvent.POST_CHANGE) {
                IResourceDelta rootDelta = event.getDelta();
                IResourceDelta artifactDelta = rootDelta.findMember(buildFile.getFullPath());
                if (artifactDelta != null && artifactDelta.getKind() == IResourceDelta.CHANGED
                        && (artifactDelta.getFlags() & IResourceDelta.CONTENT) != 0) {
                    // the file has been changed
                    for (IModule module : getServer().getModules()) {
                        if (module.getProject().equals(buildFile.getProject())) {
                            org.eclipse.wst.server.core.internal.Server server = (org.eclipse.wst.server.core.internal.Server) getServer();
                            int newState = server.getModulePublishState(
                                    new IModule[] { module }) == IServer.PUBLISH_STATE_FULL
                                            ? IServer.PUBLISH_STATE_FULL
                                            : IServer.PUBLISH_STATE_INCREMENTAL;
                            server.setModulePublishState(new IModule[] { module }, newState);
                        }
                    }
                }
            }
        }
    }
}
