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

package com.centurylink.mdw.testing;


import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import com.centurylink.mdw.annotations.RegisteredService;
import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.provider.StartupException;
import com.centurylink.mdw.provider.StartupService;
import com.centurylink.mdw.util.HttpHelper;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

/**
 * Startup service for sending slack notification on test build.
 */
@RegisteredService(StartupService.class)
public class AutoTestWebSocketClient extends WebSocketClient  implements StartupService  {
    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    public boolean isEnabled() {
        return true;
    }

    public void onStartup() throws StartupException {
        this.connect();
    }

    public void onShutdown() {
    }

    public AutoTestWebSocketClient() throws URISyntaxException {
        super(new URI(PropertyManager.getProperty(PropertyNames.MDW_WEBSOCKET_URL)));
    }

    public AutoTestWebSocketClient(URI serverURI) {
        super(serverURI);
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        send("SlackNotice");
        logger.debug( "opened connection to : " + this.getURI() + " @SlackNotice");
        // if you plan to refuse connection based on ip or httpfields overload: onWebsocketHandshakeReceivedAsClient
    }

    @Override
    public void onMessage(String message) {
        String testCaseUrl = ApplicationContext.getMdwHubUrl() + "/#/tests";
        String payload = "\"" + message + " <"  + testCaseUrl + "|Link >\"}";
        //Send a message on Slack
        HttpHelper helper;
        String slackWebhook = System.getenv("MDW_TESTING_SLACK_CHANNEL");
        try {
            if (slackWebhook != null) {
                helper = HttpHelper.getHttpHelper("POST", new URL(slackWebhook));
                helper.getConnection().setHeader("Content-Type", "application/json");
                helper.post("{\"text\": "+ payload);
            }
            else {
                if (logger.isMdwDebugEnabled())
                    logger.debug("No Slack channel found: " + payload);
            }
        }
        catch (Exception e) {
            logger.mdwDebug("testCase failed: " + payload + ":" + e.getMessage());
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        // The codes are documented in class org.java_websocket.framing.CloseFrame
        logger.debug( "Connection closed by " + ( remote ? "remote peer" : "us" ) );
    }

    @Override
    public void onError(Exception ex) {
        logger.debug( "Web Socket Error: " + ex.getMessage() + this.toString());

    }

    public static void main(String[] args) throws URISyntaxException {
        AutoTestWebSocketClient c = new AutoTestWebSocketClient( new URI( "ws://localhost:8282" )); // more about drafts here: http://github.com/TooTallNate/Java-WebSocket/wiki/Drafts
        c.connect();
    }

}
