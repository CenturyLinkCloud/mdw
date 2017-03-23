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

import com.centurylink.mdw.plugin.project.model.ServerSettings;
import com.centurylink.mdw.plugin.project.model.ServerSettings.ContainerType;

public class FuseServerBehavior extends ServiceMixServerBehavior {
    public static final String DEFAULT_JAVA_OPTS = "-server -Xms512m -Xmx1024m -XX:MaxPermSize=256m -Dderby.system.home=\"%KARAF_DATA%\\derby\" -Dderby.storage.fileSyncTransactionLog=true -Dcom.sun.management.jmxremote -Dkaraf.delay.console=false -XX:+UnlockDiagnosticVMOptions -XX:+UnsyncloadClass";

    @Override
    ServerSettings getServerSettings() {
        ServerSettings serverSettings = super.getServerSettings();
        serverSettings.setContainerType(ContainerType.Fuse);
        serverSettings.setJavaOptions(getServer().getAttribute(JAVA_OPTIONS, DEFAULT_JAVA_OPTS));
        return serverSettings;
    }
}
