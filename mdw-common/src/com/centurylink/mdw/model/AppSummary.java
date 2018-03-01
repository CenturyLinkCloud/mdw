/**
 * Copyright (c) 2018 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.model;

public class AppSummary implements Jsonable {

    private String appId;
    public String getAppId() { return appId; }
    public void setAppId(String appId) { this.appId = appId; }

    private String appVersion;
    public String getAppVersion() { return appVersion; }
    public void setAppVersion(String appVersion) { this.appVersion = appVersion; }

    private String mdwVersion;
    public String getMdwVersion() { return mdwVersion; }
    public void setMdwVersion(String mdwVersion) { this.mdwVersion = mdwVersion; }

    private String mdwBuild;
    public String getMdwBuild() { return mdwBuild; }
    public void setMdwBuild(String mdwBuild) { this.mdwBuild = mdwBuild; }

    private String mdwHubUrl;
    public String getMdwHubUrl() { return mdwHubUrl; }
    public void setMdwHubUrl(String mdwHubUrl) { this.mdwHubUrl = mdwHubUrl; }

    private String authMethod;
    public String getAuthMethod() { return authMethod; }
    public void setAuthMethod(String authMethod) { this.authMethod = authMethod; }

    private String servicesUrl;
    public String getServicesUrl() { return servicesUrl; }
    public void setServicesUrl(String servicesUrl) { this.servicesUrl = servicesUrl; }

    private Repository repository;
    public Repository getRepository() { return repository; }
    public void setRepository(Repository repository) { this.repository = repository; }

}
