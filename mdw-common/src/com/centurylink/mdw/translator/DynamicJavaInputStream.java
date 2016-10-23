/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.translator;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;

import com.centurylink.mdw.java.CompiledJavaCache;
import com.centurylink.mdw.model.workflow.Package;

/**
 * Uses the CompiledJavaCache for loading classes to be deserialized.
 */
public class DynamicJavaInputStream extends ObjectInputStream {

    private Package packageVO;

    public DynamicJavaInputStream(InputStream in, Package packageVO) throws IOException {
        super(in);
        this.packageVO = packageVO;
    }

    @Override
    protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
        try {
            try {
                return super.resolveClass(desc);
            }
            catch (ClassNotFoundException ex) {
                // try the compiled java cache
                return CompiledJavaCache.getResourceClass(desc.getName(), getClass().getClassLoader(), packageVO);
            }
        }
        catch (Exception ex) {
            throw new ClassNotFoundException(desc.getName(), ex);
        }
    }
}
