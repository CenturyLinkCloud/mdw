/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.osgi;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleReference;

import com.centurylink.mdw.cloud.CloudClassLoader;
import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.common.constant.PropertyNames;
import com.centurylink.mdw.common.utilities.ClasspathUtil;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.common.utilities.property.PropertyManager;
import com.centurylink.mdw.model.value.attribute.RuleSetVO;

/**
 * TODO: consider option to limit bundle jars since this seems to slow down
 * compilation (at least on windows PCs with security junk installed)
 */
public class OsgiClasspath {

    private static final String PATH_SEP = System.getProperty("path.separator");
    private static final String FILE_SEP = System.getProperty("file.separator");

    private static final boolean RELATIVE_PATHS = true;
    private static final String BUNDLE_PATH = "data/cache";
    private static final String BUNDLE_PREFIX = "bundle";
    private static final String DEPLOY_PATH = "deploy";

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    private String systemClasspath;
    private String cloudClasspath;
    private ClassLoader classLoader;
    private BundleSpec bundleSpec;
    private CloudClassLoader cloudClassLoader;

    private List<File> currentBundleJars = new ArrayList<File>();
    private List<File> deployBundleJars = new ArrayList<File>();
    private List<File> latestSpecBundleJars = new ArrayList<File>();
    private List<File> latestOtherBundleJars = new ArrayList<File>();
    private List<File> jarAssetFiles = new ArrayList<File>();

    private String pwd;

    public OsgiClasspath(ClassLoader classLoader, BundleSpec bundleSpec, CloudClassLoader cloudClassLoader) {

        this.classLoader = classLoader;
        this.bundleSpec = bundleSpec;
        this.cloudClassLoader = cloudClassLoader;
    }

    public void read() throws IOException {

        systemClasspath = System.getProperty("java.class.path");
        if (systemClasspath == null)
            systemClasspath = "";

        pwd = System.getProperty("user.dir");  // servicemix instance dir

        String bundlePath = System.getProperty("mdw.osgi.bundle.path");  // allow override just in case
        if (bundlePath == null)
            bundlePath = BUNDLE_PATH;


        // the current bundle's jar files based on classloader
        if (classLoader instanceof BundleReference) {
            long bundleId = ((BundleReference)classLoader).getBundle().getBundleId();
            File bundleDir = new File(pwd + "/" + bundlePath + "/" + BUNDLE_PREFIX + bundleId);
            if (bundleDir.exists() && bundleDir.isDirectory())
                currentBundleJars.addAll(Arrays.asList(ClasspathUtil.listJarFiles(bundleDir, true)));
        }

        // the smx deploy directory's jars
        String deployPath = System.getProperty("mdw.osgi.deploy.path");  // allow override just in case
        if (deployPath == null)
            deployPath = DEPLOY_PATH;
        deployBundleJars.addAll(Arrays.asList(ClasspathUtil.listJarFiles(new File(pwd + "/" + deployPath), true)));

        // add all the cached bundles (if active and taking into account bundleSpec)
        Bundle latestSpecBundle = null;
        Map<String,Bundle> latestOtherBundles = new HashMap<String,Bundle>();
        File bundleDir = new File(pwd + "/" + bundlePath);
        for (File jarFile : ClasspathUtil.listJarFiles(bundleDir, true)) {
            try {
                String bundleIdStr = jarFile.getAbsolutePath().substring((int)(bundleDir.getAbsolutePath().length() + BUNDLE_PREFIX.length()) + 1);
                bundleIdStr = bundleIdStr.substring(0, bundleIdStr.indexOf(FILE_SEP));
                Long bundleId = Long.parseLong(bundleIdStr);
                Bundle bundle = ApplicationContext.getOsgiBundleContext().getBundle(bundleId);
                if (bundle != null) {
                    if (bundleSpec != null && bundle.getSymbolicName().equals(bundleSpec.getSymbolicName())) {
                        if (bundleSpec.meetsVersionSpec(bundle.getVersion())) {
                            if (latestSpecBundle == null || latestSpecBundle.getVersion().compareTo(bundle.getVersion()) < 0)
                                latestSpecBundle = bundle;
                        }
                    }
                    else {
                        Bundle latestOtherBundle = latestOtherBundles.get(bundle.getSymbolicName());
                        if (latestOtherBundle == null || latestOtherBundle.getVersion().compareTo(bundle.getVersion()) < 0)
                            latestOtherBundles.put(bundle.getSymbolicName(), bundle);
                    }
                }
            }
            catch (Exception ex) {
                // don't let an error parsing one bundle prevent building the classpath
                logger.severeException("Error processing bundle classpath jar file: " + jarFile, ex);
            }
        }

        // spec bundle goes first
        if (latestSpecBundle != null) {
            File specBundleDir = new File(bundleDir + "/" + BUNDLE_PREFIX + latestSpecBundle.getBundleId());
            latestSpecBundleJars.addAll(0, Arrays.asList(ClasspathUtil.listJarFiles(specBundleDir, true)));
        }

        // other latest bundles
        for (String bsn : latestOtherBundles.keySet()) {
            Bundle latestOtherBundle = latestOtherBundles.get(bsn);
            File otherBundleDir = new File(bundleDir + "/" + BUNDLE_PREFIX + latestOtherBundle.getBundleId());
            latestOtherBundleJars.addAll(Arrays.asList(ClasspathUtil.listJarFiles(otherBundleDir, true)));
        }

        if (cloudClassLoader != null) {
            if (cloudClassLoader.getClasspath() != null) {
                cloudClasspath = "";
                for (int i = 0; i < cloudClassLoader.getClasspath().size(); i++) {
                    cloudClasspath += cloudClassLoader.getClasspath().get(i);
                    if (i < cloudClassLoader.getClasspath().size() - 1)
                        cloudClasspath += PATH_SEP;
                }
            }
            List<RuleSetVO> jarAssets = cloudClassLoader.getJarAssets();
            if (jarAssets != null) {
                if (ApplicationContext.isFileBasedAssetPersist()) {
                    String assetLoc = PropertyManager.getProperty(PropertyNames.MDW_ASSET_LOCATION);
                    jarAssetFiles.addAll(Arrays.asList(ClasspathUtil.listJarFiles(new File(assetLoc), true)));
                }
                else {
                    // write them to the temp dir (unfortunately needed for Groovy asset dependencies)
                    File tempLibDir = new File(ApplicationContext.getTempDirectory() + "/lib");
                    if (!tempLibDir.exists()) {
                        if (!tempLibDir.mkdirs())
                            throw new IOException("Unable to create directory: " + tempLibDir);
                        for (RuleSetVO jarAsset : jarAssets) {
                            String name = jarAsset.getName().endsWith(".jar") ? jarAsset.getName() : jarAsset.getName() + ".jar";
                            File destFile = new File(tempLibDir + "/" + jarAsset.getPackageName() + "/" + name);
                            jarAssetFiles.add(destFile);
                        }
                    }
                }
            }
        }
    }

    public String toString() {
        if (systemClasspath == null)
            return "null";

        StringBuffer buf = new StringBuffer(systemClasspath);

        // first the specified bundle jars
        for (File jarFile : latestSpecBundleJars)
            buf.append(PATH_SEP).append(getPath(jarFile));
        // then the current bundle jars
        for (File jarFile : currentBundleJars)
            buf.append(PATH_SEP).append(getPath(jarFile));
        // then the deploy directory jars
        for (File jarFile : deployBundleJars)
            buf.append(PATH_SEP).append(getPath(jarFile));
        // finally all other latest bundle version jars
        for (File jarFile : latestOtherBundleJars)
            buf.append(PATH_SEP).append(getPath(jarFile));
        if (cloudClasspath != null)
            buf.append(PATH_SEP).append(cloudClasspath);
        for (File jarAssetFile : jarAssetFiles)
            buf.append(PATH_SEP).append(jarAssetFile.getAbsolutePath());

        return buf.toString();
    }

    private String getPath(File jarFile) {
        if (RELATIVE_PATHS)
            return jarFile.getAbsolutePath().substring(pwd.length() + 1);
        else
            return jarFile.getAbsolutePath();
    }
}
