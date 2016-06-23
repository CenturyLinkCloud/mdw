/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.osgi;

import java.util.ArrayList;
import java.util.List;

import com.centurylink.mdw.common.provider.PackageAwareProvider;
import com.centurylink.mdw.common.provider.Provider;

public class PackageAwareProviderLocator<T> extends ProviderLocator<T> {

    public PackageAwareProviderLocator(List<PackageAwareProvider<T>> pkgAwareProviders) {
        List<Provider<T>> providers = new ArrayList<Provider<T>>();
        providers.addAll(pkgAwareProviders);
        setProviders(providers);
    }

    public PackageAwareProvider<T> getLatestMatch(BundleSpec bundleSpec) {
        return (PackageAwareProvider<T>) super.getLatestMatch(bundleSpec);
    }
}
