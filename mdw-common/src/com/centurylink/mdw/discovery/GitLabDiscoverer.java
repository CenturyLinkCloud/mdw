package com.centurylink.mdw.discovery;

import com.centurylink.mdw.util.DesignatedHostSslVerifier;
import com.centurylink.mdw.util.HttpHelper;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GitLabDiscoverer extends GitDiscoverer {

    private URL apiBase;
    private String repoPath;
    private String repoName;

    public GitLabDiscoverer(URL repoUrl) throws IOException {
        this(repoUrl, false);
    }

    /**
     * Must be a public repository, or setToken() must be called.
     */
    public GitLabDiscoverer(URL repoUrl, boolean isTrusted) throws IOException {
        super(repoUrl);
        String apiBaseUrl = "https://";
        if (repoUrl.getUserInfo() != null) {
            apiBaseUrl += repoUrl.getUserInfo() + "@";
        }
        apiBaseUrl += repoUrl.getHost();
        apiBase = new URL(apiBaseUrl + "/api/v4");
        String path = repoUrl.getPath();
        repoPath = path.substring(1, path.lastIndexOf('.'));
        repoName = repoPath.substring(repoPath.lastIndexOf('/') + 1);
        String query = repoUrl.getQuery();
        if (query != null) {
            for (String pair : query.split("&")) {
                int eq = pair.indexOf('=');
                if (eq > 0 && eq < pair.length() - 1 &&
                        (pair.substring(0, eq).equals("Private-Token") || pair.substring(0, eq).equals("private_token"))) {
                    setToken(pair.substring(eq + 1));
                }
            }
        }
        if (isTrusted) {
            try {
                DesignatedHostSslVerifier.setupSslVerification(repoUrl.getHost());
            }
            catch (Exception ex) {
                throw new IOException(ex);
            }
        }
    }

    @Override
    public URL getApiBase() { return apiBase; }

    @Override
    public String getRepoPath() {  return repoPath; }

    @Override
    public String getRepoName() { return repoName; }

    @Override
    public List<String> getBranches(int max) throws IOException {
        if (max <= 0 || max > 100)
            max = 100;
        // projects/MDW_DEV%2Fmdw-demo-ctl/repository/branches
        String url = apiBase + "/projects/" + URLEncoder.encode(repoPath, "utf-8") + "/repository/branches?per_page=" + max;
        return getArrayValues(url,"name");
    }

    @Override
    public List<String> getTags(int max) throws IOException {
        if (max <= 0 || max > 100)
            max = 100;
        // projects/MDW_DEV%2Fmdw-demo-ctl/repository/tags
        String url = apiBase + "/projects/" + URLEncoder.encode(repoPath, "utf-8") + "/repository/tags?per_page=" + max;
        return getArrayValues(url,"name");
    }

    public JSONObject getFileInfo(String path, String ref) throws IOException {
        // projects/MDW_DEV%2Fmdw-demo-ctl/repository/files/project.yaml?ref=master
        String url = apiBase + "/projects/" + URLEncoder.encode(repoPath, "utf-8") + "/repository/files/" +
                URLEncoder.encode(path, "utf-8") + "?ref=" + ref;
        HttpHelper http = getHttpHelper(url);
        String info = http.get();
        if (http.getResponseCode() == 404) {
            return null;
        }
        else if (http.getResponseCode() != 200) {
            throw new IOException(url + " -> " + http.getResponseCode() + ": " + http.getResponseMessage());
        }
        return new JSONObject(info);
    }

    public JSONArray getTreeInfo(String path, String ref, boolean recursive) throws IOException {
        // projects/MDW_DEV%2Fmdw-demo-ctl/repository/tree?path=/&ref=master&page_size=100
        String url = apiBase + "/projects/" + URLEncoder.encode(repoPath, "utf-8") + "/repository/tree?path=" +
                URLEncoder.encode(path, "utf-8") + "&ref=" + ref + "&per_page=100";
        if (recursive)
            url += "&recursive=true";
        HttpHelper http = getHttpHelper(url);
        String treeInfo = http.get();
        if (http.getResponseCode() != 200)
            throw new IOException(url + " -> " + http.getResponseCode() + ": " + http.getResponseMessage());
        return new JSONArray(treeInfo);
    }

    /**
     * Paths are relative to the repository base.
     */
    protected String getPackageName(String path) throws IOException {
        return path.substring(getAssetPath().length() + 1, path.length() - 5).replace('/', '.');
    }

    protected HttpHelper getHttpHelper(String url) throws IOException {
        HttpHelper http = new HttpHelper(new URL(url));
        if (getToken() != null) {
            Map<String,String> headers = new HashMap<>();
            headers.put("Private-Token", getToken());
            http.setHeaders(headers);
        }
        return http;
    }

}
