/*
 * Copyright (C) 2017 CenturyLink, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
