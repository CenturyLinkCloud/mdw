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

import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.ServerCore;
import org.eclipse.wst.server.core.internal.Server;
import org.eclipse.wst.server.core.model.ServerDelegate;

import com.centurylink.mdw.plugin.project.model.ServerSettings;

@SuppressWarnings("restriction")
public class ServerRunnerMonitor implements ServerStatusListener {
    private ServerSettings serverSettings;
    private ServerStatusChecker statusCheck;

    private Server server;

    public ServerRunnerMonitor(ServerSettings serverSettings) {
        this.serverSettings = serverSettings;
        statusCheck = new ServerStatusChecker(serverSettings);
    }

    public void startup() {
        for (IServer server : ServerCore.getServers()) {
            if (server.getRuntime() != null && server.getRuntime().getRuntimeType() != null
                    && server.getRuntime().getRuntimeType().getId()
                            .startsWith("com.centurylink.server.runtime")) {
                ServiceMixServer smxServer = (ServiceMixServer) server
                        .loadAdapter(ServerDelegate.class, null);
                if (smxServer.getName().equals(serverSettings.getServerName())) {
                    this.server = (Server) server;
                    statusCheck.addStatusListener(this);
                    statusCheck.start();
                }
            }
        }
    }

    public void statusChanged(String status) {
        if (status.equals(ServerStatusListener.SERVER_STATUS_RUNNING))
            server.setServerState(IServer.STATE_STARTED);
        else if (server.getServerState() != IServer.STATE_STARTING
                && (status.equals(ServerStatusListener.SERVER_STATUS_STOPPED)
                        || status.equals(ServerStatusListener.SERVER_STATUS_ERRORED)))
            server.setServerState(IServer.STATE_STOPPED);
        else if (status.equals(ServerStatusListener.SERVER_STATUS_WAIT))
            server.setServerState(IServer.STATE_STARTING);

        if (server.getServerState() == IServer.STATE_STOPPED) {
            if (statusCheck != null)
                statusCheck.stop();
        }
    }
}
