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


import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONObject;

import com.centurylink.mdw.annotations.RegisteredService;
import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.provider.StartupException;
import com.centurylink.mdw.provider.StartupService;
import com.centurylink.mdw.test.TestCase;
import com.centurylink.mdw.test.TestCaseList;
import com.centurylink.mdw.util.HttpHelper;

/**
 * Startup service for sending slack notification on test build.
 */
@RegisteredService(StartupService.class)
public class AutoTestWebSocketClient extends WebSocketClient  implements StartupService  {

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

    public AutoTestWebSocketClient( URI serverURI ) {
        super( serverURI );
    }

    @Override
    public void onOpen( ServerHandshake handshakedata ) {
        send("AutomatedTests");
        System.out.println( "opened connection" );
        // if you plan to refuse connection based on ip or httpfields overload: onWebsocketHandshakeReceivedAsClient
    }

    @Override
    public void onMessage( String message ) {
        TestCaseList testList = new TestCaseList(ApplicationContext.getAssetRoot(), new JSONObject(message));
        List<TestCase> testCaseList = testList.getTestCases();
        for (TestCase testCase : testCaseList) {
            if (testCase.getStatus() == TestCase.Status.Failed || testCase.getStatus() == TestCase.Status.Errored) {
                //Send a message on Slack
                HttpHelper helper;
                try {
                    String testCaseUrl = ApplicationContext.getMdwHubUrl() + "/#/tests/" + testCase.getPackage() + "/" + testCase.getName();
                    String slackWebhook = System.getenv(PropertyNames.MDW_TEAM_SLACK_CHANNEL);
                    if (slackWebhook != null) {
                        helper = HttpHelper.getHttpHelper("POST", new URL(slackWebhook));
                        helper.getConnection().setHeader("Content-Type", "application/json");
                        String payload = "\"test case failed: <" +testCaseUrl + "|" + testCase.getName() + ">\"}";
                        helper.post("{\"text\": "+ payload);
                    }
                    else {
                        System.out.println("testCase failed: " + testCaseUrl);
                    }
                }
                catch (MalformedURLException e) {
                    e.printStackTrace();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onClose( int code, String reason, boolean remote ) {
        // The codecodes are documented in class org.java_websocket.framing.CloseFrame
        System.out.println( "Connection closed by " + ( remote ? "remote peer" : "us" ) );
    }

    @Override
    public void onError( Exception ex ) {
        ex.printStackTrace();
        // if the error is fatal then onClose will be called additionally
    }

    public static void main( String[] args ) throws URISyntaxException {
        AutoTestWebSocketClient c = new AutoTestWebSocketClient( new URI( "ws://localhost:8282" )); // more about drafts here: http://github.com/TooTallNate/Java-WebSocket/wiki/Drafts
        c.connect();
    }

}
