package com.centurylink.mdw.common.service;

import com.centurylink.mdw.app.ApplicationContext;

import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;
import java.util.ArrayList;
import java.util.List;

public class WebSocketConfig extends ServerEndpointConfig.Configurator {
    @Override
    public void modifyHandshake(ServerEndpointConfig sec, HandshakeRequest request, HandshakeResponse response) {
        List<String> header = new ArrayList<>();
        header.add(ApplicationContext.getHostname());
        response.getHeaders().put("mdw-hostname", header);
    }
}