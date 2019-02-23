package com.centurylink.mdw.discovery;

import com.centurylink.mdw.model.PackageMeta;
import com.centurylink.mdw.model.ProjectMeta;
import com.centurylink.mdw.util.HttpHelper;
import com.centurylink.mdw.util.file.Packages;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.*;

public abstract class GitDiscoverer implements Discoverer {

    private static boolean FRUGAL_MDW_REQUESTS = true;
    private static final String MDW_GIT = "https://github.com/CenturyLinkCloud/mdw.git";

    private URL repoUrl;
    public URL getRepoUrl() { return repoUrl; }

    private String token;
    protected String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    private String ref;
    public String getRef() { return ref; }
    public void setRef(String ref) {
        if (ref == null || !ref.equals(this.ref)) {
            assetPath = null;
            packages = null;
            packageInfo = null;
        }
        this.ref = ref;
    }

    public abstract URL getApiBase();
    public abstract String getRepoPath();
    public abstract String getRepoName();
    public abstract List<String> getBranches(int max) throws IOException;
    public abstract List<String> getTags(int max) throws IOException;

    abstract JSONObject getFileInfo(String path, String ref) throws IOException;
    abstract JSONArray getTreeInfo(String path, String ref, boolean recursive) throws IOException;
    abstract HttpHelper getHttpHelper(String url) throws IOException;
    abstract String getPackageName(String path) throws IOException;

    private String assetPath;
    private List<String> packages;
    private Map<String, PackageMeta> packageInfo;

    private String label;

    public GitDiscoverer(URL repoUrl) throws MalformedURLException {
        this.repoUrl = repoUrl;
        label = new URL(repoUrl.getProtocol(), repoUrl.getHost(), repoUrl.getPort(), repoUrl.getPath()).toString();
    }

    @Override
    public String getAssetPath() throws IOException {
        if (assetPath == null) {
            if (getRef() == null)
                throw new IOException("Missing ref");
            Path projectPath = null;
            JSONObject projectYaml = getFileInfo("project.yaml", getRef());
            if (projectYaml == null) {
                // try one-level deep only
                JSONArray topTree = getTreeInfo("", getRef(), false);
                for (int i = 0; i < topTree.length(); i++) {
                    JSONObject item = topTree.getJSONObject(i);
                    String type = item.optString("type");
                    if (type.equals("tree") || type.equals("dir")) {
                        String itemPath = item.getString("path");
                        projectYaml = getFileInfo(itemPath + "/project.yaml", getRef());
                        if (projectYaml != null) {
                            projectPath = new File(itemPath).toPath();
                            break;
                        }
                    }
                }
            }
            if (projectYaml == null)
                throw new IOException("project.yaml not found");

            String base64 = projectYaml.getString("content");
            ProjectMeta projectMeta = new ProjectMeta(Base64.getMimeDecoder().decode(base64));
            assetPath = projectMeta.getAssetLocation();
            if (projectPath != null) {
                assetPath = new File(projectPath + "/" + assetPath).toPath().normalize().toString();
            }
        }
        return assetPath;
    }

    @Override
    public List<String> getPackages() throws IOException {
        if (packages == null) {
            JSONArray treeInfo = getTreeInfo(getAssetPath(), getRef(), true);
            packages = new ArrayList<>();
            for (int i = 0; i < treeInfo.length(); i++) {
                JSONObject item = treeInfo.getJSONObject(i);
                String path = item.getString("path");
                if (path.endsWith("/.mdw")) {
                    String pkg = getPackageName(path);
                    if (MDW_GIT.equals(repoUrl.toString()) || !Packages.isMdwPackage(pkg))
                        packages.add(pkg);
                }
            }
        }

        Collections.sort(packages);
        return packages;
    }

    @Override
    public Map<String, PackageMeta> getPackageInfo() throws IOException {
        if (packageInfo == null) {
            packageInfo = new HashMap<>();
            String mdwVersion = null;
            for (String pkg : getPackages()) {
                PackageMeta pkgInfo;
                if (mdwVersion == null) {
                    String yamlPath = getAssetPath() + "/" + pkg.replace('.', '/') + "/.mdw/package.yaml";
                    JSONObject packageYaml = getFileInfo(yamlPath, getRef());
                    if (packageYaml == null)
                        throw new IOException("not found: " + yamlPath);
                    String base64 = packageYaml.getString("content");
                    pkgInfo = new PackageMeta(Base64.getMimeDecoder().decode(base64));
                    if (MDW_GIT.equals(repoUrl.toString()) && FRUGAL_MDW_REQUESTS)
                        mdwVersion = pkgInfo.getVersion();
                }
                else {
                    // avoid so many requests (TODO: icons)
                    pkgInfo = new PackageMeta(pkg);
                    pkgInfo.setVersion(mdwVersion);
                }
                packageInfo.put(pkg, pkgInfo);
            }
        }
        return packageInfo;
    }

    protected List<String> getArrayValues(String url, String property) throws IOException {
        HttpHelper http = getHttpHelper(url);
        String responseContent = http.get();
        if (http.getResponseCode() != 200)
            throw new IOException(url + " -> " + http.getResponseCode() + ": " + http.getResponseMessage());
        JSONArray jsonArray = new JSONArray(responseContent);
        List<String> branches = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonObj = jsonArray.getJSONObject(i);
            branches.add(jsonObj.getString(property));
        }
        return branches;
    }

    @Override
    public String toString() {
        return label;
    }
}
