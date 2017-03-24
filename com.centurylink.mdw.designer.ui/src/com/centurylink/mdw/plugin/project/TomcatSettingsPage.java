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
package com.centurylink.mdw.plugin.project;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.wst.server.core.IRuntime;

import com.centurylink.mdw.plugin.project.model.ServerSettings;

public class TomcatSettingsPage extends ServerSettingsPage {
    public static final String PAGE_TITLE = "Tomcat Settings";
    public static final int DEFAULT_PORT = 8080;
    public static final String DEFAULT_USER = "tomcat";

    public TomcatSettingsPage() {
        setTitle(PAGE_TITLE);
        setDescription("Enter your Tomcat server information.\n"
                + "This will be written to your environment properties file.");
    }

    public String getServerName() {
        return "Tomcat";
    }

    public int getDefaultServerPort() {
        return DEFAULT_PORT;
    }

    public String getDefaultServerUser() {
        return DEFAULT_USER;
    }

    public boolean checkForAppropriateRuntime(IRuntime runtime) {
        String vendor = runtime.getRuntimeType().getVendor();
        String version = runtime.getRuntimeType().getVersion();
        return (vendor.equals("Apache") || vendor.equals("Apache")) && version.startsWith("6");
    }

    @Override
    protected void createServerLocControls(Composite parent, int ncol) {
        // same as server home
    }

    protected String serverHomeSpecializedCheck(String serverHome) {
        if (serverHome != null && serverHome.length() != 0
                && !checkFile(serverHome + "/lib/catalina.jar"))
            return "Apache Tomcat Home must contain lib/catalina.jar";
        else
            return null;
    }

    protected String serverLocSpecializedCheck(String serverLoc) {
        ServerSettings serverSettings = getProject().getServerSettings();
        serverSettings.setServerLoc(serverSettings.getHome());
        return null;
    }

}
