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
package com.centurylink.mdw.cli;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

public class Discover extends Setup {

    private String groupId;

    private JSONObject packages;

    public JSONObject getPackages() {
        return packages;
    }

    public String getGroupId() {
        return groupId;
    }

    private String artifactId;

    public String getArtifactId() {
        return artifactId;
    }

    private String version;

    public String getVersion() {
        return version;
    }

    private boolean latest;

    public boolean isLatest() {
        return latest;
    }

    public Discover(String groupId, boolean latest) {
        this.groupId = groupId;
        this.latest = latest;
    }

    public Discover(String groupId, String artifactId, String version, boolean latest) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.latest = latest;
    }

    public Discover run(ProgressMonitor... progressMonitors) throws IOException {
        JSONObject json = new JSONObject(searchArtifacts());
        packages = new JSONObject();
        packages.put("assetRoot", "assets");
        packages.put("packages", new JSONArray());
        if (json.has("response")) {
            JSONObject response = json.getJSONObject("response");
            if (response.has("docs")) {
                JSONArray artifactsArr = response.getJSONArray("docs");
                for (int i = 0; i < artifactsArr.length(); i++) {
                    JSONObject artifact = artifactsArr.getJSONObject(i);
                    JSONArray array = packages.getJSONArray("packages");
                    JSONObject jsonObj = new JSONObject();
                    jsonObj.put("name", artifact.getString("g").replace("assets", "")
                            + artifact.getString("a").replace("-", "."));
                    jsonObj.put("artifact", artifact.getString("a"));
                    if (!latest)
                        jsonObj.put("version", artifact.getString("v"));
                    else
                        jsonObj.put("version", artifact.getString("latestVersion"));
                    array.put(jsonObj);
                }
            }
        }
        return this;
    }

    public String searchArtifacts() throws IOException {
        StringBuilder query = new StringBuilder(
                "http://search.maven.org/solrsearch/select?wt=json&rows=50");
        if (!latest)
            query.append("&core=gav");
        query.append("&q=");
        if (groupId != null && !groupId.isEmpty())
            query.append("g:").append(groupId).append("AND");
        if (artifactId != null && !artifactId.isEmpty())
            query.append("a:").append(artifactId).append("AND");
        if (version != null && !version.isEmpty())
            query.append("v:").append(version).append("AND");
        String url = query.toString();
        if (url.endsWith("AND"))
            url = url.substring(0, url.length() - 3);

        URLConnection connection = new URL(url).openConnection();
        BufferedReader bufferedReader = new BufferedReader(
                new InputStreamReader(connection.getInputStream()));
        String line;
        StringBuilder content = new StringBuilder();
        while ((line = bufferedReader.readLine()) != null) {
            content.append(line + "\n");
        }
        bufferedReader.close();
        return content.toString();
    }
}
