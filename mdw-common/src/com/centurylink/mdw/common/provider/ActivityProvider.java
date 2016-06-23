/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.provider;

import com.centurylink.mdw.activity.types.GeneralActivity;

/**
 * Provides instances of GeneralActivity implementor to the MDW runtime engine.
 * In an OSGi environment workflow client bundles register as ActivityProviders so that
 * the MDW engine can request an activity instance that is loaded within the context of
 * the client bundle.  This allows client-provided implementors full access through their
 * ClassLoaders to that bundle's Java classes and resources.
 */
public interface ActivityProvider extends Provider<GeneralActivity> {

    public GeneralActivity getInstance(String className)
    throws ClassNotFoundException, IllegalAccessException, InstantiationException;
}
