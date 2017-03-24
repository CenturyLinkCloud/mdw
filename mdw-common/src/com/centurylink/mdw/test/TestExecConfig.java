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
package com.centurylink.mdw.test;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.Jsonable;

public class TestExecConfig implements Jsonable {

    /**
     * System properties.  TODO: thread props for Gradle
     */
    public static final String MDW_TEST_STUB_PORT = "mdw.test.stub.port";
    public static final String MDW_TEST_VERBOSE = "mdw.test.verbose";
    public static final String MDW_TEST_CREATE_REPLACE = "mdw.test.create.replace";
    public static final String MDW_TEST_PIN_TO_SERVER = "mdw.test.pin.to.server";
    public static final String MDW_TEST_SERVER_URL = "mdw.test.server.url";

    private int threads = 5; // thread pool size
    public int getThreads() { return threads; }
    public void setThreads(int threads) { this.threads = threads; }

    private int interval = 2; // seconds
    public int getInterval() { return interval; }
    public void setInterval(int interval) { this.interval = interval; }

    private boolean stubbing;
    public boolean isStubbing() { return stubbing; }
    public void setStubbing(boolean stubbing) { this.stubbing = stubbing; }

    private int stubPort;
    public int getStubPort() { return stubPort; }
    public void setStubPort(int port) { this.stubPort = port; }

    private boolean loadTest; // ignored presently
    public boolean isLoadTest() { return loadTest; }
    public void setLoadTest(boolean loadTest) { this.loadTest = loadTest; }

    private boolean verbose = true;
    public boolean isVerbose() { return verbose; }
    public void setVerbose(boolean verbose) { this.verbose = verbose; }

    private boolean createReplace;
    public boolean isCreateReplace() { return createReplace; }
    public void setCreateReplace(boolean createReplace) { this.createReplace = createReplace; }

    private boolean pinToServer = true;
    public boolean isPinToServer() { return pinToServer; }
    public void setPinToServer(boolean pinToServer) { this.pinToServer = pinToServer; }

    /**
     * Implies running workflow through REST.
     */
    private String serverUrl;
    public String getServerUrl() { return serverUrl; }
    public void setServerUrl(String url) { this.serverUrl = url; }

    public TestExecConfig() {
        // default options
    }

    public TestExecConfig(JSONObject json) throws JSONException {
        if (json.has("threads"))
            this.threads = json.getInt("threads");
        if (json.has("interval"))
            this.interval = json.getInt("interval");
        if (json.has("stubbing")) {
            this.stubbing = json.getBoolean("stubbing");
            if (stubbing && json.has("stubPort"))
                this.stubPort = json.getInt("stubPort");
        }
        if (json.has("loadTest"))
            this.loadTest = json.getBoolean("loadTest");
        if (json.has("verbose"))
            this.verbose = json.getBoolean("verbose");
        if (json.has("createReplace"))
            this.createReplace = json.getBoolean("createReplace");
        if (json.has("pinToServer"))
            this.pinToServer = json.getBoolean("pinToServer");
        if (json.has("serverUrl"))
            this.serverUrl = json.getString("serverUrl");
    }

    public TestExecConfig(Properties properties) {
        String stubPort = properties.getProperty(MDW_TEST_STUB_PORT);
        if (stubPort != null)
            this.stubPort = Integer.parseInt(stubPort);
        String verbose = properties.getProperty(MDW_TEST_VERBOSE);
        if (verbose != null)
            this.verbose = Boolean.parseBoolean(verbose);
        String createReplace = properties.getProperty(MDW_TEST_CREATE_REPLACE);
        if (createReplace != null)
            this.createReplace = Boolean.parseBoolean(createReplace);
        String pinToServer = properties.getProperty(MDW_TEST_PIN_TO_SERVER);
        if (pinToServer != null)
            this.pinToServer = Boolean.parseBoolean(pinToServer);
        this.serverUrl = properties.getProperty(MDW_TEST_SERVER_URL);
    }

    public JSONObject getJson() throws JSONException {
        JSONObject json = new JSONObject();
        if (threads > 0)
            json.put("threads", threads);
        if (interval > 0)
            json.put("interval", interval);
        if (stubbing)
            json.put("stubbing", stubbing);
        if (stubbing && stubPort > 0)
            json.put("stubPort", stubPort);
        if (loadTest)
            json.put("loadTest", loadTest);
        if (verbose)
            json.put("verbose", verbose);
        if (createReplace)
            json.put("createReplace", createReplace);
        if (pinToServer)
            json.put("pinToServer", pinToServer);
        if (serverUrl != null)
            json.put("serverUrl", serverUrl);
        return json;
    }

    public String getJsonName() {
        return "testExecConfig";
    }
}
