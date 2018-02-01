/*
 * Copyright (C) 2018 CenturyLink, Inc.
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
package com.centurylink.mdw.model.system;

import java.net.URL;
import java.util.Map;

public class Server {

    private String host;
    public String getHost() {
        return host;
    }

    private int port;
    public int getPort() {
        return port;
    }

    private Map<?,?> config;
    public Map<?,?> getConfig() { return config; }

    public Server(String host, int port) {
        this(host, port, null);
    }

    public Server(String host, int port, Map<?,?> config) {
        this.host = host;
        this.port = port;
        this.config = config;
    }

    public Server(String hostPort) {
        int colon = hostPort.indexOf(':');
        if (colon == -1) {
            this.host = hostPort;
        }
        else {
            this.host = hostPort.substring(0, colon);
            this.port = Integer.parseInt(hostPort.substring(colon + 1));
        }
    }

    public Server(URL url) {
        this.host = url.getHost();
        this.port = url.getPort();
        if (this.port == -1) {
            this.port = url.getProtocol().equalsIgnoreCase("https") ? 443 : 80;
        }
    }

    @Override
    public String toString() {
        return host + ":" + port;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof Server) {
            Server otherServer = (Server)other;
            return otherServer.host.equals(host) && otherServer.port == port;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }
}
