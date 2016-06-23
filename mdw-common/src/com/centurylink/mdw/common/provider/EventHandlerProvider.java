/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.provider;

import com.centurylink.mdw.event.ExternalEventHandler;

/**
 * Provides instances of ExtenalEventHandlers to the MDW runtime engine.
 * In an OSGi environment workflow client bundles register as EventHandlerProviders so that
 * the MDW engine can request an event handler instance that is loaded within the context of
 * the client bundle.  This allows client-provided implementors full access through their
 * ClassLoaders to that bundle's Java classes and resources.
 */
public interface EventHandlerProvider extends Provider<ExternalEventHandler> {

}
