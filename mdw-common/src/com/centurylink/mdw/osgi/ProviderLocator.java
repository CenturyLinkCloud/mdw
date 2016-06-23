/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.osgi;

import java.util.List;

import org.osgi.framework.Bundle;

import com.centurylink.mdw.common.provider.Provider;

public class ProviderLocator<T> {

    private List<Provider<T>> providers;
    protected void setProviders(List<Provider<T>> providers) { this.providers = providers; }

    /**
     * Subclass must set the provider list.
     */
    protected ProviderLocator() {
    }

    public ProviderLocator(List<Provider<T>> providers) {
        this.providers = providers;
    }

    public Provider<T> getLatestMatch(BundleSpec bundleSpec) {
        Provider<T> latestMatch = null;
        for (Provider<T> provider : providers) {
            Bundle providerBundle = provider.getBundleContext().getBundle();
            if (bundleSpec.meetsSpec(providerBundle)
                  && (latestMatch == null || latestMatch.getBundleContext().getBundle().getVersion().compareTo(providerBundle.getBundleContext().getBundle().getVersion()) < 0)) {
                latestMatch = provider;
            }
        }
        return latestMatch;
    }

    public String getLatestMatchingBundleVersion(BundleSpec bundleSpec) {
        Provider<T> latestMatch = getLatestMatch(bundleSpec);
        return latestMatch == null ? null : latestMatch.getBundleContext().getBundle().getVersion().toString();
    }

}
