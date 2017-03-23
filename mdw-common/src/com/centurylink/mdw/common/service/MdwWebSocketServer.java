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
package com.centurylink.mdw.common.service;

import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.java_websocket.WebSocket;
import org.java_websocket.WebSocketImpl;
import org.java_websocket.handshake.ClientHandshake;

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.provider.StartupException;
import com.centurylink.mdw.provider.StartupService;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

/**
 * MDW WebSocket server
 */
public class MdwWebSocketServer extends org.java_websocket.server.WebSocketServer implements StartupService {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();
    private static MdwWebSocketServer instance = null;
    private static final Object instanceLock = new Object();
    private final Map<String,List<WebSocket>> jsonNameToConnections = new HashMap<String,List<WebSocket>>();

    public MdwWebSocketServer() {
        super(new InetSocketAddress(ApplicationContext.getWebSocketPort()));

        if (instance == null)
            instance = this;
    }

    public MdwWebSocketServer(InetSocketAddress address) {
        super(address);

        if (instance == null)
            instance = this;
    }

    /**
     * Use this method to access the singleton of WebSocketServer
     */
    public static MdwWebSocketServer getInstance() {
        MdwWebSocketServer tempInstance = instance;
        if (tempInstance == null) { // This should never happen since it's instantiated by StartupListener
            synchronized(instanceLock) {
                tempInstance = instance;
                if (tempInstance == null)
                    instance = new MdwWebSocketServer();
            }
        }
        return instance;
    }

    @Override
    public void onOpen(WebSocket websocket, ClientHandshake handshake) {
    }

    @Override
    public void onClose(WebSocket websocket, int code, String reason, boolean remote) {
        unsubscribeConnection(websocket);
    }

    @Override
    public void onError(WebSocket websocket, Exception ex) {
        ex.printStackTrace(); // TODO
    }

    @Override
    public void onMessage(WebSocket websocket, String message) {
        // First, un-subscribe for any previously subscribed to message
        unsubscribeConnection(websocket);

        List<WebSocket> myList;
        if (jsonNameToConnections.get(message) == null) {
            myList = new ArrayList<WebSocket>();
            myList.add(websocket);
            synchronized(jsonNameToConnections) {
                jsonNameToConnections.put(message, myList);
            }
        }
        else {
            myList = jsonNameToConnections.get(message);
            if (!myList.contains(websocket)) {
                synchronized(jsonNameToConnections) {
                    myList.add(websocket);
                }
            }
        }
    }


    @Override
    protected boolean onConnect(SelectionKey key) {
        // TODO Auto-generated method stub
        return super.onConnect(key);
    }

    public void send(String text) {
        Collection<WebSocket> conns = connections();
        synchronized (conns) {
            for (WebSocket conn : conns) {
                conn.send(text);
            }
        }
    }

    public void send(String text, String key) {
        List<WebSocket> interestedConns = jsonNameToConnections.get(key);
        if (interestedConns == null || interestedConns.isEmpty())
            send(text);
        else {
            Collection<WebSocket> conns = connections();
            synchronized (conns) {
                for (WebSocket conn : conns) {
                    if (interestedConns.contains(conn)) {
                        conn.send(text);
                    }
                }
            }
        }
    }

    @Override
    public void onShutdown() {
        try {
            getInstance().stop();
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
        }
    }

    @Override
    public void onStartup() throws StartupException {
        WebSocketImpl.DEBUG = logger.isMdwDebugEnabled();
        try {
            getInstance().start();
            logger.info("MdwWebSocketServer started on port: " + getPort());
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
            throw new StartupException(ex.getMessage(), ex);
        }
    }

    @Override
    public boolean isEnabled() {
        return PropertyManager.getProperty(PropertyNames.MDW_WEBSOCKET_URL) != null;
    }

    public boolean hasInterestedConnections(String key) {
        if (jsonNameToConnections.get(key) != null && !jsonNameToConnections.get(key).isEmpty()) {
            Collection<WebSocket> conns = connections();
            List<WebSocket> interestedConns = jsonNameToConnections.get(key);
            synchronized (conns) {
                for (WebSocket conn : conns) {
                    if (interestedConns.contains(conn))
                        return true;
                }
            }
        }
        return false;
    }

    private void unsubscribeConnection(WebSocket websocket) {
        synchronized(jsonNameToConnections) {
            // Remove connection from map
            for (List<WebSocket> conns : jsonNameToConnections.values()) {
                if (conns.contains(websocket))
                    conns.remove(websocket);
            }
            // Remove keys from map without connections left
            for (String key : jsonNameToConnections.keySet()) {
                if (jsonNameToConnections.get(key).isEmpty())
                    jsonNameToConnections.remove(key);
            }
        }
    }
}
