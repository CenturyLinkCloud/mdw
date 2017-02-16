/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer;

/**
 * Tracks dirtiness of the designer canvas.
 */
public interface DirtyStateListener {
    public void dirtyStateChanged(boolean dirty);
}