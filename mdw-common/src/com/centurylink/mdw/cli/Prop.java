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
package com.centurylink.mdw.cli;

public class Prop {

    /**
     * Name in templates and --param.
     */
    private String name;
    public String getName() { return name; }

    /**
     * File path relative to project dir.
     */
    private String file;
    public String getFile() { return file; }

    private String property;
    public String getProperty() { return property; }

    public Prop(String name, String file, String property) {
        this(name, file, property, false);
    }

    public Prop(String name, String file, String property, boolean inProjectDir) {
        this.name = name;
        this.file = file;
        this.property = property;
        this.inProjectDir = inProjectDir;
    }

    /**
     * Specified via command-line.
     */
    boolean specified;

    public String toString() {
        String s = name + " --> (" + file + ") " + property;
        if (specified)
            s += "*";
        return s;
    }

    /**
     * Whether the prop file exists in projectDir vs configRoot
     */
    boolean inProjectDir;


}
