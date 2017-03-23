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
package com.centurylink.mdw.plugin.designer.model;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import com.centurylink.mdw.plugin.PluginUtil;

public class DocumentTemplate {
    private String name;

    public String getName() {
        return name;
    }

    private String extension;

    public String getExtension() {
        return extension;
    }

    private String location;

    public String getLocation() {
        return location;
    }

    private byte[] content;

    public byte[] getContent() throws IOException {
        if (content == null)
            load();
        return content;
    }

    public DocumentTemplate(String name, String extension, String location) {
        this.name = name;
        this.extension = extension;
        this.location = location;
    }

    private void load() throws IOException {
        try {
            String templateFileName = name.replaceAll(" ", "") + extension;
            URL localUrl = PluginUtil.getLocalResourceUrl(location + "/" + templateFileName);
            File templateFile = new File(new URI(localUrl.toString()));
            content = PluginUtil.readFile(templateFile);
        }
        catch (URISyntaxException ex) {
            throw new IOException(ex.toString());
        }
    }
}
