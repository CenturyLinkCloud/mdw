/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.hub.context;

import java.io.File;

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
