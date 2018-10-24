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
package com.centurylink.mdw.services.asset;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.stream.Collectors;

import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.html.FlexmarkInstances;
import com.centurylink.mdw.model.asset.AssetInfo;
import com.centurylink.mdw.util.file.FileHelper;
import com.vladsch.flexmark.ast.Node;
import com.vladsch.flexmark.parser.Parser;

/**
 * TODO: cached output based on file timestamp
 */
public class HtmlRenderer implements Renderer {

    public static final String STYLES = "styles";
    public static final String HTML_BODY = "html-body";

    private static String styles = null;
    static String getStyles() throws IOException {
        if (styles == null) {
            InputStream is = FileHelper.readFile("css/styles.css", HtmlRenderer.class.getClassLoader());
            if (is != null) {
                try (BufferedReader buffer = new BufferedReader(new InputStreamReader(is))) {
                    styles = buffer.lines().collect(Collectors.joining("\n"));
                }
            }
        }
        return styles;
    }

    private AssetInfo asset;

    public HtmlRenderer(AssetInfo asset) {
        this.asset = asset;
    }

    public byte[] render(Map<String,String> options) throws RenderingException {
        Path filePath = Paths.get(asset.getFile().getPath());
        try {
            if (asset.getExtension().equals("html")) {
                return Files.readAllBytes(filePath);
            }
            else if (asset.getExtension().equals("md")) {

                Parser parser = FlexmarkInstances.getParser(null);
                com.vladsch.flexmark.html.HtmlRenderer renderer = FlexmarkInstances.getRenderer(null);

                StringBuilder html = new StringBuilder();
                boolean withStyles = "true".equalsIgnoreCase(options.get(STYLES));
                if (withStyles) {
                    html.append("<html>\n<head>\n<style>\n");
                    html.append(getStyles());
                    html.append("\n</style>\n</head>\n<body>");
                }
                Node document = parser.parse(new String(Files.readAllBytes(filePath)));
                html.append(renderer.render(document));
                if (withStyles) {
                    html.append("\n<body>\n</html>");
                }
                return html.toString().getBytes();
            }
            else {
                throw new RenderingException(ServiceException.NOT_IMPLEMENTED, "Cannot convert " + asset.getExtension() + " to HTML");
            }
        }
        catch (IOException ex) {
            throw new RenderingException(ServiceException.INTERNAL_ERROR, "Error reading: " + filePath, ex);
        }
    }
}
