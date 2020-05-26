package com.centurylink.mdw.discovery;

import com.centurylink.mdw.util.HttpHelper;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GitHubDiscoverer extends GitDiscoverer {

    private URL apiBase;
    private String repoPath;
    private String repoName;

    /**
     * Must be a public repository.
     */
    public GitHubDiscoverer(URL repoUrl) throws IOException {
        super(repoUrl);
        String apiBaseUrl = "https://";
        if (repoUrl.getUserInfo() != null) {
            apiBaseUrl += repoUrl.getUserInfo() + "@";
        }
        apiBase = new URL(apiBaseUrl + "api.github.com/repos");
        String path = repoUrl.getPath();
        repoPath = path.substring(1, path.lastIndexOf('.'));
        repoName = repoPath.substring(repoPath.lastIndexOf('/') + 1);
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
        String url = apiBase + "/" + repoPath + "/branches?per_page=" + max;
        return getArrayValues(url,"name");
    }

    @Override
    public List<String> getTags(int max) throws IOException {
        if (max <= 0 || max > 100)
            max = 100;
        String url = apiBase + "/" + repoPath + "/tags?per_page=" + max;
        return getArrayValues(url,"name");
    }

    public JSONObject getFileInfo(String path, String ref) throws IOException {
        String url = apiBase + "/" + repoPath + "/contents/" + URLEncoder.encode(path, "utf-8") + "?ref=" + ref;
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
        if (recursive) {
            if (path.equals("/"))
                throw new UnsupportedOperationException("No recursive from root path");
            String sha = getSha(path, ref);
            String url = apiBase + "/" + repoPath + "/git/trees/" + sha + "?ref=" + ref + "&recursive=true";
            HttpHelper http = getHttpHelper(url);
            String treeInfo = http.get();
            if (http.getResponseCode() != 200)
                throw new IOException(url + " -> " + http.getResponseCode() + ": " + http.getResponseMessage());
            return new JSONObject(treeInfo).getJSONArray("tree");
        }
        else {
            String url = apiBase + "/" + repoPath + "/contents/" + URLEncoder.encode(path, "utf-8") + "?ref=" + ref;
            HttpHelper http = getHttpHelper(url);
            String info = http.get();
            if (http.getResponseCode() != 200)
                throw new IOException(url + " -> " + http.getResponseCode() + ": " + http.getResponseMessage());
            return new JSONArray(info);
        }
    }

    private String getSha(String path, String ref) throws IOException {
        int lastSlash = path.lastIndexOf('/');
        String parentPath;
        if (lastSlash == -1)
            parentPath = "";
        else if (lastSlash < path.length() - 1)
            parentPath = path.substring(0, lastSlash);
        else
            throw new IOException("Invalid path: " + path);
        String itemName = URLEncoder.encode(path.substring(lastSlash + 1), "utf-8");
        String url = apiBase + "/" + repoPath + "/contents/" + parentPath + "?ref=" + ref;
        HttpHelper http = getHttpHelper(url);
        String info = http.get();
        if (http.getResponseCode() != 200)
            throw new IOException(url + " -> " + http.getResponseCode() + ": " + http.getResponseMessage());
        JSONArray items = new JSONArray(info);
        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.getJSONObject(i);
            if (item.getString("name").equals(itemName))
                return item.getString("sha");
        }
        return null;
    }

    /**
     * Paths are relative to assetPath.
     */
    protected String getPackageName(String path) {
        return path.substring(0, path.length() - 5).replace('/', '.');
    }

    protected HttpHelper getHttpHelper(String url) throws IOException {
        HttpHelper http = new HttpHelper(new URL(url));
        Map<String,String> headers = new HashMap<>();
        headers.put("User-Agent", "CenturyLinkCloud/mdw");
        if (getToken() != null)
            headers.put("Authorization", "token " + getToken());
        http.setHeaders(headers);
        return http;
    }
}
