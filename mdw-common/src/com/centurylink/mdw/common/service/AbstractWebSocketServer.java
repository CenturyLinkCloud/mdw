/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.service;

import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.util.Collection;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;

import com.centurylink.mdw.provider.StartupException;
import com.centurylink.mdw.provider.StartupService;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

/**
 * WebSocket server
 */
public abstract class AbstractWebSocketServer extends org.java_websocket.server.WebSocketServer implements StartupService {

    protected static StandardLogger logger = LoggerUtil.getStandardLogger();

    public AbstractWebSocketServer(InetSocketAddress address) {
        super(address);
    }

    @Override
    public void onOpen(WebSocket websocket, ClientHandshake handshake) {
    }

    @Override
    public void onClose(WebSocket websocket, int code, String reason, boolean remote) {
    }

    @Override
    public void onError(WebSocket websocket, Exception ex) {
        ex.printStackTrace(); // TODO
    }

    @Override
    public void onMessage(WebSocket websocket, String message) {
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

    @Override
    public abstract void onShutdown();

    @Override
    public abstract void onStartup() throws StartupException;

    @Override
    public abstract boolean isEnabled();

}
