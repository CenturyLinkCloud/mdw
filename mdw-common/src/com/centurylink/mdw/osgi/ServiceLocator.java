/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.osgi;

import java.util.List;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import com.centurylink.mdw.common.service.RegisteredService;

public class ServiceLocator<T> {

    private List<T> services;

    public ServiceLocator(List<T> services) {
        super();
        this.services = services;
    }

    @SuppressWarnings("unchecked")
    public <T extends RegisteredService> T getLatestMatchService(BundleSpec bundleSpec) {
        List<T> matchServices = (List<T>) services;
        Bundle matchBundle = null;
        T latestMatchService = null;
        for (T service : matchServices) {
            Bundle bundle = FrameworkUtil.getBundle(service.getClass()).getBundleContext().getBundle();
            if (bundle != null) {
                if (bundleSpec.meetsSpec(bundle)
                        && (matchBundle == null || matchBundle.getVersion().compareTo(bundle.getVersion()) < 0)) {
                    matchBundle = bundle;
                    latestMatchService = service;
                }
            }
        }
        return latestMatchService;
    }

}
