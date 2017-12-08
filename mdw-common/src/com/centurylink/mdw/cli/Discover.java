/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
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

    private File destDir;

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

    private String packaging;

    public String getPackaging() {
        return artifactId;
    }

    public Discover(File destDir, String groupId) {
        this.destDir = destDir;
        this.groupId = groupId;
    }

    public Discover(String groupId, boolean latest) {
        this.groupId = groupId;
        this.latest = latest;
    }

    public Discover(String groupId, String artifactId, String version, boolean latest,
            String packaging) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.latest = latest;
        this.packaging = packaging;
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
                    jsonObj.put("version", artifact.getString("latestVersion"));
                    array.put(jsonObj);
                }
            }
        }
        return this;
    }

    public void importPackages(ProgressMonitor... progressMonitors) throws IOException {
        String url = "http://search.maven.org/remotecontent?filepath=";
        for (String pkg : getBaseAssetPackages()) {
            List<String> pkgs = new ArrayList<>();
            int index = pkg.indexOf('-');
            String pakage = pkg.substring(0, index);
            String ver = pkg.substring(index + 1);
            pkgs.add(pakage);
            String qulifier = groupId.replace("assets", "");
            String artifact = pakage.replace(qulifier, "").replace('.', '-');
            File tempZip = Files.createTempFile("central-discovery", ".zip").toFile();
            new Download(new URL(url + groupId.replace('.', '/') + "/" + artifact + "/" + ver + "/"
                    + artifact + "-" + ver + ".zip"), tempZip).run(progressMonitors);

            Archive archive = new Archive(destDir, pkgs);
            archive.backup();
            System.out.println("Unzipping into: " + destDir);
            new Unzip(tempZip, destDir, true).run();
            archive.archive(true);
            if (!tempZip.delete())
                throw new IOException("Failed to delete: " + tempZip.getAbsolutePath());
        }
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
        if (packaging != null && !packaging.isEmpty())
            query.append("p:").append(packaging);
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
