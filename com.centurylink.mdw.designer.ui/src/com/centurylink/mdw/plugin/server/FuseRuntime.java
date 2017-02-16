/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.server;

import java.io.File;

import org.eclipse.core.runtime.IPath;

public class FuseRuntime extends ServiceMixRuntime {
    @Override
    protected String validateRuntimeLoc() {
        IPath location = getRuntimeWorkingCopy().getLocation();
        File locFile = location.toFile();
        if (!locFile.exists() || !locFile.isDirectory())
            return getType() + " Home must be an existing directory";
        else if (!new File(locFile + "/bin/fuse.bat").exists()
                && !new File(locFile + "/bin/fuse").exists())
            return getType() + " Home must contain bin/fuse.bat or bin/fuse";
        else
            return null;
    }
}
