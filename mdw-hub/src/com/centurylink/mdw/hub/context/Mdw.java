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
package com.centurylink.mdw.hub.context;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Lightweight server-side model object for dynamically populating index.html.
 */
public class Mdw {

    private String version;
    public String getVersion() { return version; }

    private String build;
    public String getBuild() { return build; }

    private String hubRoot;
    public String getHubRoot() { return hubRoot; }

    private String servicesRoot;
    public String getServicesRoot() { return servicesRoot; }

    private String centralRoot;
    public String getCentralRoot() { return centralRoot; }
    public void setCentralRoot(String centralRoot) { this.centralRoot = centralRoot; }

    private String appId;
    public String getAppId() { return appId; }
    public void setAppId(String appId) { this.appId = appId; }

    private String docsRoot;
    public String getDocsRoot() { return docsRoot; }
    public void setDocsRoot(String docsRoot) { this.docsRoot = docsRoot; }

    private File assetRoot;
    public File getAssetRoot() { return assetRoot; }

    private String overridePackage;
    public String getOverridePackage() { return overridePackage; }

    private File overrideRoot;
    public File getOverrideRoot() { return overrideRoot; }

    private String hubUser = "";
    public String getHubUser() { return hubUser; }
    public void setHubUser(String user) { this.hubUser = user; }

    private String authMethod;
    public String getAuthMethod() { return authMethod; }
    public void setAuthMethod(String authMethod) { this.authMethod = authMethod; }

    private String authTokenLoc;
    public String getAuthTokenLoc() { return authTokenLoc; }
    public void setAuthTokenLoc(String authTokenLoc) { this.authTokenLoc = authTokenLoc; }

    private String webSocketUrl;
    public String getWebSocketUrl() { return webSocketUrl; }
    public void setWebSocketUrl(String url) { this.webSocketUrl = url; }

    private boolean allowAnyAuthenticatedUser;
    public boolean isAllowAnyAuthenticatedUser() { return allowAnyAuthenticatedUser; }
    public void setAllowAnyAuthenticatedUser(boolean allow) { this.allowAnyAuthenticatedUser = allow; }

    private String discoveryUrl;
    public String getDiscoveryUrl() { return discoveryUrl; }
    public void setDiscoveryUrl(String url) { this.discoveryUrl = url; }

    private String customRoutes;
    public String getCustomRoutes() { return customRoutes; }
    public void setCustomRoutes(String customRoutes) { this.customRoutes = customRoutes; }

    public File getDevOverrideRoot() {
        String devOverrideRoot = System.getProperty("mdw.hub.dev.override.root");
        return devOverrideRoot == null ? null : new File(devOverrideRoot);
    }

    public boolean isDev() {
        return "dev".equalsIgnoreCase(System.getProperty("mdw.runtime.env"));
    }

    /**
     * Finds overridden hub artifacts among assets.
     * Core dev override is also supported for vanilla hub development.
     */
    public File getHubOverride(String path) throws IOException {
        if (getOverrideRoot() != null) {
            File hubOverride = new File(getOverrideRoot() + path);
            if (hubOverride.isFile())
                return hubOverride;
        }
        if (getDevOverrideRoot() != null && isDev()) {
            File devOverride = new File(getDevOverrideRoot() + path);
            if (devOverride.isFile())
                return devOverride;
        }
        return null;
    }

    /**
     * Custom service paths for a spring boot app.
     */
    private List<String> customPaths;
    public List<String> getCustomPaths() { return customPaths; }
    public void setCustomPaths(List<String> paths) { this.customPaths = paths; }

    Mdw(String version, String build, String hubRoot, String servicesRoot, File assetRoot, String overridePackage) {
        this.version = version;
        this.build = build;
        this.hubRoot = hubRoot;
        this.servicesRoot = servicesRoot;
        this.assetRoot = assetRoot;
        this.overridePackage = overridePackage;

        if (assetRoot != null)
            overrideRoot = new File(assetRoot + "/" + getOverridePackage().replace('.', '/'));
    }

    /**
     * For ExpressionUtil model.
     */
    public Mdw getMdw() {
        return this;
    }
}
