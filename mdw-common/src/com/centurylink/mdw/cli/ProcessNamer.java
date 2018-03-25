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
package com.centurylink.mdw.cli;

public class ProcessNamer {

    protected String basePackage;
    protected String pkgPath;
    protected String baseName;

    public ProcessNamer(String basePackage, String path) {
        this.basePackage = basePackage;
        pkgPath = baseName = path;
        int slashCurly = pkgPath.lastIndexOf("/{");
        if (slashCurly > 0) {
            pkgPath = pkgPath.substring(0, slashCurly);
            String firstSeg = path.substring(0, slashCurly);
            String lastSeg = path.substring(slashCurly + 2, path.lastIndexOf("}"));
            if (firstSeg.endsWith("s")) // TODO check if unique
                baseName = firstSeg.substring(0, firstSeg.length() - 1);
            else
                baseName = firstSeg + "By" + Character.toUpperCase(lastSeg.charAt(0)) + lastSeg.substring(1);
        }
        int pkgSlash = pkgPath.lastIndexOf("/");
        if (pkgSlash > 0) {
            pkgPath = pkgPath.substring(0, pkgSlash);
            baseName = path.substring(pkgPath.length());
        }
        if (baseName.startsWith("/"))
            baseName = baseName.substring(1);
        baseName = Character.toUpperCase(baseName.charAt(0)) + baseName.substring(1);
        baseName.replace('/', '_');
    }

    public String getPackage() {
        return basePackage + pkgPath.replace('/', '.');
    }

    public String getName(String httpMethod) {
        if (httpMethod.equalsIgnoreCase("get")) {
            return "Retrieve" + baseName;
        }
        if (httpMethod.equalsIgnoreCase("post")) {
            return "Create" + baseName;
        }
        if (httpMethod.equalsIgnoreCase("put") || httpMethod.equalsIgnoreCase("patch")) {
            return "Update" + baseName;
        }
        if (httpMethod.equalsIgnoreCase("delete")) {
            return "Delete" + baseName;
        }
        else {
            String method = httpMethod.toLowerCase();
            return Character.toUpperCase(method.charAt(0)) + method.substring(1) + baseName;
        }
    }

}
