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

import org.eclipse.wst.server.core.IRuntime;

public class JBossSettingsPage extends ServerSettingsPage {
    public static final String PAGE_TITLE = "JBoss Settings";
    public static final int DEFAULT_PORT = 8080;
    public static final String DEFAULT_USER = "admin";

    public JBossSettingsPage() {
        setTitle(PAGE_TITLE);
        setDescription("Enter your JBoss Server information.\n"
                + "This will be written to your environment properties file.");
    }

    public String getServerName() {
        return "JBoss";
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
        return (vendor.equals("JBoss Community") || vendor.equals("JBoss")
                || vendor.equals("JBoss Enterprise Middleware")) && version.startsWith("5");
    }

    protected String serverHomeSpecializedCheck(String serverHome) {
        if (serverHome != null && serverHome.length() != 0
                && !checkFile(serverHome + "/lib/jboss-j2se.jar"))
            return "JBoss Home must contain lib/jboss-j2se.jar.jar";
        else
            return null;
    }

    protected String serverLocSpecializedCheck(String serverLoc) {
        if (serverLoc != null && serverLoc.length() != 0 && !checkDir(serverLoc + "/deploy"))
            return "JBoss Server Location must contain a 'deploy' directory";
        else
            return null;
    }
}
