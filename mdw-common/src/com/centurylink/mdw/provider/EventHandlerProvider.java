/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.provider;

import com.centurylink.mdw.event.ExternalEventHandler;

/**
 * Provides instances of ExtenalEventHandlers to the MDW runtime engine.
 */
public interface EventHandlerProvider extends Provider<ExternalEventHandler> {

}
