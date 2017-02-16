/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
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
