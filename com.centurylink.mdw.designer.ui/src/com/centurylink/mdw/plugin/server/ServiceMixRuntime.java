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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.wst.server.core.model.RuntimeDelegate;

import com.centurylink.mdw.plugin.MdwPlugin;

public class ServiceMixRuntime extends RuntimeDelegate {

    public static final String JAVA_HOME = "javaHome";

    @Override
    public void setDefaults(IProgressMonitor monitor) {
        super.setDefaults(monitor);
    }

    public String getName() {
        return getRuntime().getName();
    }

    public void setName(String name) {
        getRuntimeWorkingCopy().setName(name);
    }

    public String getType() {
        return getRuntime().getRuntimeType().getName();
    }

    public String getVersion() {
        return getRuntime().getRuntimeType().getVersion();
    }

    public IPath getLocation() {
        return getRuntime().getLocation();
    }

    public void setLocation(IPath location) {
        getRuntimeWorkingCopy().setLocation(location);
    }

    public String getJavaHome() {
        return getAttribute(JAVA_HOME, "");
    }

    public void setJavaHome(String javaHome) {
        setAttribute(JAVA_HOME, javaHome);
    }

    public IStatus validate() {
        IStatus result = super.validate(); // check name null or exists, and
                                           // location null
        if (result != null && !result.isOK())
            return result;

        // check server home
        String msg = validateRuntimeLoc();

        if (msg == null) {
            // check java home
            String javaHome = getJavaHome();
            if (javaHome == null || javaHome.isEmpty())
                msg = "";
            else {
                File javaHomeFile = new File(javaHome);
                if (!javaHomeFile.exists() || !javaHomeFile.isDirectory())
                    msg = "Java Home must be an existing directory";
                else if (!new File(javaHomeFile + "/jre/lib/rt.jar").exists()
                        && !new File(javaHomeFile + "/lib/rt.jar").exists())
                    msg = "Java Home must contain jre/lib/rt.jar or lib/rt.jar";
            }
        }

        if (msg == null)
            return Status.OK_STATUS;
        else
            return new Status(IStatus.ERROR, MdwPlugin.PLUGIN_ID, 0, msg, null);
    }

    protected String validateRuntimeLoc() {
        IPath location = getRuntimeWorkingCopy().getLocation();
        File locFile = location.toFile();
        if (!locFile.exists() || !locFile.isDirectory())
            return getType() + " Home must be an existing directory";
        else if (!new File(locFile + "/bin/servicemix.bat").exists()
                && !new File(locFile + "/bin/servicemix.sh").exists())
            return getType() + " Home must contain bin/servicemix.bat or bin/servicemix.sh";
        else
            return null;
    }

    public void save() throws CoreException {
        getRuntimeWorkingCopy().save(true, null);
    }

}
