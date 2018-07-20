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
package com.centurylink.mdw.java;

import com.centurylink.mdw.util.file.FileHelper;

public class JavaNaming {

    public static String getValidPackageName(String raw) {
        String out = FileHelper.stripDisallowedFilenameChars(raw);
        return out.replaceAll(" ", "");
    }

    public static String getValidClassName(String raw) {
        String out = raw.endsWith(".java") ? raw.substring(0, raw.length() - 5) : raw;
        out = FileHelper.stripDisallowedFilenameChars(out);
        for (int i = 0; i < out.length(); i++) {
            if (!Character.isJavaIdentifierPart(out.charAt(i)))
                out = out.replace(out.charAt(i), '_');
        }
        return Character.toUpperCase(out.charAt(0)) + out.substring(1);
    }
}
