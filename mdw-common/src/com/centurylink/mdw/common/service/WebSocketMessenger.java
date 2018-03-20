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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.websocket.CloseReason;
import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

@ServerEndpoint("/websocket")
public class WebSocketMessenger {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    private Map<String,List<Session>> topicSubscribers = new HashMap<>();

    private static WebSocketMessenger instance;
    /**
     * @return null until first subscriber
     */
    public static WebSocketMessenger getInstance() {
        return instance;
    }

    public WebSocketMessenger() {
        instance = this;
    }

    @OnOpen
    public void onOpen(Session session, EndpointConfig config) {
    }

    @OnClose
    public void unsubscribe(Session session, CloseReason reason) {
        synchronized(topicSubscribers) {
            // remove session
            for (List<Session> sessions : topicSubscribers.values()) {
                if (sessions.contains(session))
                    sessions.remove(session);
            }
            // remove topics without any sessions
            for (String topic : topicSubscribers.keySet()) {
                if (topicSubscribers.get(topic).isEmpty())
                    topicSubscribers.remove(topic);
            }
        }
    }

    @OnError
    public void onError(Session session, Throwable t) {
        if (t instanceof IOException && t.getMessage() != null && t.getMessage().startsWith(
                "An established connection was aborted")) {
            // avoid nuisance logging when browser closes connection
            if (logger.isMdwDebugEnabled())
                logger.severeException(t.getMessage(), t);
        }
        else {
            logger.severeException(t.getMessage(), t);
        }
    }

    @OnMessage
    public void subscribe(Session session, String topic) {
       unsubscribe(session, null);
       synchronized(topicSubscribers) {
           List<Session> sessions = topicSubscribers.get(topic);
           if (sessions == null) {
               sessions = new ArrayList<>();
               topicSubscribers.put(topic, sessions);
           }
           if (!sessions.contains(session))
               sessions.add(session);
       }
    }

    /**
     * Returns true if any subscribers.
     */
    public boolean send(String topic, String message) throws IOException {
        List<Session> sessions = topicSubscribers.get(topic);
        if (sessions != null) {
            for (Session session : sessions) {
                session.getBasicRemote().sendText(message);
            }
        }
        return sessions != null;
    }
}
