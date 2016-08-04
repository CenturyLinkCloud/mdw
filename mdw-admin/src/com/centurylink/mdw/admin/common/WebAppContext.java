/**
 * Copyright (c) 2015 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.admin.common;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.centurylink.mdw.common.constant.AuthConstants;
import com.centurylink.mdw.common.constant.PropertyNames;
import com.centurylink.mdw.common.utilities.property.PropertyManager;

public class WebAppContext {
    
    private static Mdw mdw;
    public static Mdw getMdw() throws IOException {
        if (mdw == null) {
            
            String hubRoot = PropertyManager.getProperty("mdw.admin.hub.url");
            if (hubRoot == null) {
                // user relative url root
                String container = PropertyManager.getProperty(PropertyNames.MDW_CONTAINER_DATASOURCE_PROVIDER);
                if ("OSGi".equals(container))
                    hubRoot = "/MDWHub";
                else
                    hubRoot = "/mdw";
            }
            
            String servicesRoot = PropertyManager.getProperty("mdw.admin.services.url");
            if (servicesRoot == null)
                servicesRoot = hubRoot;
            
            File assetRoot = null;
            String assetLoc = PropertyManager.getProperty(PropertyNames.MDW_ASSET_LOCATION);
            if (assetLoc != null)
                assetRoot = new File(assetLoc);

            String overridePackage = PropertyManager.getProperty(PropertyNames.MDW_ADMIN_OVERRIDE_PACKAGE);
            if (overridePackage == null)
                overridePackage = "mdw-admin";

            mdw = new Mdw(getMdwVersion(), hubRoot, servicesRoot, assetRoot, overridePackage);
            
            boolean isDev = "dev".equals(System.getProperty("runtimeEnv"));
            if (isDev) {
                String hubUser = PropertyManager.getProperty(PropertyNames.MDW_DEV_USER);
                if (hubUser == null)
                    hubUser = PropertyManager.getProperty("mdw.hub.user"); // compatibility
                mdw.setHubUser(hubUser);
            }
            
            if (isDev || AuthConstants.getOAuthTokenLocation() != null || AuthConstants.isMdwLdapAuth())
                mdw.setLoginPage("/login");
            else
                mdw.setLoginPage("/authentication/login.jsf");
            
            String tasksUi = PropertyManager.getProperty(PropertyNames.MDW_TASKS_UI);
            if (tasksUi != null)
                mdw.setTasksUi(tasksUi);
        }
        return mdw;
    }
    
    /**
     * Recursively list override files.  Assumes dir path == ext == type.
     */
    public static List<File> listOverrideFiles(String type) throws IOException {
        List<File> files = new ArrayList<File>();
        if (getMdw().getOverrideRoot().isDirectory()) {
            File dir = new File(getMdw().getOverrideRoot() + "/" + type);
            if (dir.isDirectory())
                addFiles(files, dir, type);
        }
        return files;
    }
    
    private static void addFiles(List<File> list, File dir, String ext) {
        for (File file : dir.listFiles()) {
            if (file.isDirectory())
                addFiles(list, file, ext);
            else if (file.getName().endsWith("." + ext))
                list.add(file);
        }
    }
    
    public static final String VERSION_PATH = "META-INF/mdw/version.properties";
    public static final String VERSION_PROP = "mdw.version";
    
    private static String mdwVersion;
    private static String getMdwVersion() throws IOException {
        // TODO: test in linux OSGi
        if (mdwVersion == null) {
            InputStream is = WebAppContext.class.getClassLoader().getResourceAsStream(VERSION_PATH);
            if (is == null)
                is = WebAppContext.class.getClassLoader().getResourceAsStream("../../" + VERSION_PATH);
            Properties versionProps = new Properties();
            versionProps.load(is);
            return versionProps.getProperty(VERSION_PROP);
        }
        return mdwVersion;
    }
}
