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

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.swt.widgets.Shell;
import org.xml.sax.SAXException;

import com.centurylink.mdw.plugin.PluginUtil;
import com.centurylink.mdw.plugin.project.model.ServerSettings;

/**
 * Currently we don't use ServerRunner for Tomcat (see TomcatServerBehavior), so
 * this class is unused.
 */
public class TomcatConfigurator extends ServerConfigurator {
    public TomcatConfigurator(ServerSettings serverSettings) {
        super(serverSettings);
    }

    public void doConfigure(Shell shell) {
        // presently configuration is external/manual
    }

    public String getDeployText() {
        return "TODO";
    }

    public void doDeploy(Shell shell) {
        // TODO
    }

    public String launchNewServerCreation(Shell shell) {
        // should never happen
        throw new UnsupportedOperationException("Not supported for Tomcat");
    }

    public void parseServerAdditionalInfo()
            throws IOException, SAXException, ParserConfigurationException {
        // nothing to show
    }

    @Override
    public String[] getEnvironment(boolean debug) {
        String[] env = new String[3];
        env[0] = "JAVA_HOME=" + getServerSettings().getJdkHome();
        env[1] = "CATALINA_HOME=" + getServerSettings().getHomeWithFwdSlashes();
        String runtimeEnv = getServerSettings().getProject().checkRequiredVersion(6, 0)
                ? "-Dmdw.runtime.env=dev" : "-DruntimeEnv=dev";
        env[2] = "JAVA_OPTS=" + runtimeEnv + " -Dmdw.config.location="
                + getServerSettings().getHomeWithFwdSlashes() + "/mdw/config"
                + (getServerSettings().getJavaOptions() == null ? ""
                        : getServerSettings().getJavaOptions());
        if (debug)
            env[2] += " -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address="
                    + getServerSettings().getDebugPort();
        return (String[]) PluginUtil.appendArrays(getCurrentEnv(), env);
    }

    @Override
    public String getCommandDir() {
        return getServerSettings().getHome() + FILE_SEP + "bin";
    }

    @Override
    public String getStartCommand() {
        String cmd = (FILE_SEP.equals("/") ? "catalina.sh run" : "catalina.bat run");
        return getCommandDir() + FILE_SEP + cmd;
    }

    @Override
    public String getStopCommand() {
        String cmd = (FILE_SEP.equals("/") ? "catalina.sh stop" : "catalina.bat stop");
        return getCommandDir() + FILE_SEP + cmd;
    }

}
