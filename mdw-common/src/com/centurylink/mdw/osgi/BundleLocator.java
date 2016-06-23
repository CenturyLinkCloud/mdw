/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.osgi;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.springframework.osgi.util.BundleDelegatingClassLoader;

import com.centurylink.mdw.common.constant.ApplicationConstants;

/**
 * TODO: Refactor and remove dependency on Spring DM when we can move to OSGi 4.3.
 */
public class BundleLocator {

    private BundleContext bundleContext;

    public BundleLocator(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    /**
     * Returns the matching bundle with the latest version.
     */
    public Bundle getBundle(String symbolicName) {
        Bundle latest = null;
        for (Bundle bundle : bundleContext.getBundles()) {
            if (bundle.getSymbolicName() != null && bundle.getSymbolicName().equals(symbolicName) && (latest == null || latest.getVersion().compareTo(bundle.getVersion()) < 0)) {
                latest = bundle;
            }
        }
        return latest;
    }

    /**
     * Null version spec returns latest version.
     */
    public Bundle getBundle(String symbolicName, String versionSpec) {
        return getBundle(new BundleSpec(symbolicName, versionSpec));
    }

    /**
     * Returns the latest version that matches the designated BundleSpec.
     * Null version spec returns latest version.
     */
    public Bundle getBundle(BundleSpec bundleSpec) {
        if (bundleSpec.getVersionSpec() == null)
            return getBundle(bundleSpec.getSymbolicName());

        Bundle latestMatch = null;
        for (Bundle bundle : bundleContext.getBundles()) {
            if ((bundle.getSymbolicName() != null && bundle.getSymbolicName().equals(bundleSpec.getSymbolicName()) && (bundleSpec.meetsVersionSpec(bundle.getVersion())))
                    && (latestMatch == null || latestMatch.getVersion().compareTo(bundle.getVersion()) < 0)) {
                return bundle;
            }
        }
        return null;
    }

    /**
     * Null versionSpec returns ClassLoader of the matching bundle with the latest version.
     */
    public ClassLoader getClassLoader(String symbolicName, String versionSpec) {
        return getClassLoader(new BundleSpec(symbolicName, versionSpec));
    }

    /**
     * Null versionSpec returns ClassLoader of the matching bundle with the latest version.
     */
    public ClassLoader getClassLoader(BundleSpec bundleSpec) {
        Bundle bundle = getBundle(bundleSpec);
        if (bundle != null) {
            return BundleDelegatingClassLoader.createBundleClassLoaderFor(bundle, getClass().getClassLoader());
        }
        return null;
    }

    public ClassLoader getMDWWorkflowClassLoader() {
        Bundle bundle = getBundle(ApplicationConstants.MDW_WORKFLOW_BUNDLE);
        if (bundle != null) {
            return BundleDelegatingClassLoader.createBundleClassLoaderFor(bundle, null);
        }
        return null;
    }
}
