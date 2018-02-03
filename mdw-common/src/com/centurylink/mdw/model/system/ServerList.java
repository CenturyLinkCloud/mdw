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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.centurylink.mdw.config.YamlBuilder;
import com.centurylink.mdw.config.YamlPropertyTranslator;

public class ServerList implements Iterable<Server>, YamlPropertyTranslator {

    private List<Server> servers = new ArrayList<>();
    public List<Server> getServers() {
        return servers;
    }

    public ServerList() {
    }

    /**
     * Structured server config.
     */
    public ServerList(Map<?,?> serverMap) {
        for (Object name : serverMap.keySet()) {
            String host = name.toString();
            Map<?,?> config = (Map<?,?>)serverMap.get(host);
            List<?> ports = (List<?>)config.get("ports");
            for (Object port : ports) {
                servers.add(new Server(host, (Integer)port, config));
            }
        }
    }

    public ServerList(List<String> hostPorts) {
        for (String hostPort : hostPorts)
            servers.add(new Server(hostPort));
    }

    public ServerList(ServerList...lists) {
        if (lists != null) {
            for (ServerList list : lists) {
                for (Server server : list.servers) {
                    if (!servers.contains(server))
                        servers.add(server);
                }
            }
        }
    }

    public Server get(int index) {
        return servers.get(index);
    }

    @Override
    public Iterator<Server> iterator() {
        return servers.iterator();
    }

    public boolean isEmpty() {
        return servers.isEmpty();
    }

    public boolean contains(Server server) {
        return servers.contains(server);
    }

    public List<String> getHostPortList() {
        List<String> hostPorts = new ArrayList<>();
        for (Server server : this) {
            hostPorts.add(server.toString());
        }
        return hostPorts;
    }

    @Override
    @SuppressWarnings("unchecked")
    public YamlBuilder translate(Map<String,String> ruleProps) {
        String hostPortsProp = ruleProps.get("[" + getClass().getName() + "]");
        for (String hostPort : hostPortsProp.split("\\s*,\\s*")) {
            servers.add(new Server(hostPort));
        }

        Map<String,Object> topMap = new LinkedHashMap<>();
        Map<String,Object> serverMap = new LinkedHashMap<>();
        topMap.put("servers", serverMap);
        for (Server server : servers) {
            Map<String,Object> hostMap = (Map<String,Object>)serverMap.get(server.getHost());
            if (hostMap == null) {
                hostMap = new LinkedHashMap<>();
                hostMap.put("ports", new ArrayList<String>());
                serverMap.put(server.getHost(), hostMap);
            }
            ((List<String>)hostMap.get("ports")).add(String.valueOf(server.getPort()));
        }

        return new YamlBuilder(topMap);
    }

}
