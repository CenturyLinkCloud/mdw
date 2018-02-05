/*
 * Copyright (C) 2018 CenturyLink, Inc.
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
package com.centurylink.mdw.config;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;

/**
 * Retains inserted key order.  Not comprehensively tested.
 */
public class OrderedProperties extends Properties {

    private final LinkedHashSet<Object> orderedKeys = new LinkedHashSet<>();

    @Override
    public Set<Object> keySet() {
        return orderedKeys;
    }

    @Override
    public Set<String> stringPropertyNames() {
        Set<String> names = new LinkedHashSet<>();
        for (Object key : orderedKeys)
            names.add(key.toString());
        return names;
    }

    @Override
    public synchronized Enumeration<Object> keys() {
        return Collections.enumeration(orderedKeys);
    }

    @Override
    public synchronized Object put(Object key, Object value) {
        orderedKeys.add(key);
        return super.put(key, value);
    }

    @Override
    public void store(OutputStream out, String comments) throws IOException {
        if (comments != null) {
            super.store(out, comments);
        }
        else {
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(out, "8859_1"));
            synchronized (this) {
                for (Enumeration<?> e = keys(); e.hasMoreElements();) {
                    String key = (String)e.nextElement();
                    String val = (String)get(key);
                    bw.write(key.replaceAll(" ", "\\\\ ").replaceAll("!", "\\\\!") + "=" + val);
                    bw.newLine();
                }
            }
            bw.flush();
        }
    }
}
