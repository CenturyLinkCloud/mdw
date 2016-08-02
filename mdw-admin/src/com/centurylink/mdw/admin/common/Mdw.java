/**
 * Copyright (c) 2015 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.admin.common;

import java.io.File;

/**
 * Lightweight server-side model object for dynamically populating index.html.
 */
public class Mdw {
    
    private String version;
    public String getVersion() { return version; }
    
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
    
    private String tasksUi = "mdw-hub"; // TODO: will be mdw-admin
    public String getTasksUi() { return tasksUi; }
    public void setTasksUi(String tasksUi) { this.tasksUi = tasksUi; }
    
    private String hubUser = "";
    public String getHubUser() { return hubUser; }
    public void setHubUser(String user) { this.hubUser = user; }
    
    private String loginPage;
    public String getLoginPage() { return loginPage; }
    public void setLoginPage(String page) { this.loginPage = page; }
        
    Mdw(String version, String hubRoot, String servicesRoot, File assetRoot, String overridePackage) {
        this.version = version;
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
