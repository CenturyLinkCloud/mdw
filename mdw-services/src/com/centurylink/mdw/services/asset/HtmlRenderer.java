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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.model.asset.AssetInfo;
import com.vladsch.flexmark.ast.Node;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.options.MutableDataSet;

/**
 * TODO: cached output based on file timestamp
 */
public class HtmlRenderer implements Renderer {

    private AssetInfo asset;

    public HtmlRenderer(AssetInfo asset) {
        this.asset = asset;
    }

    public byte[] render() throws RenderingException {
        Path filePath = Paths.get(asset.getFile().getPath());
        try {
            if (asset.getExtension().equals("html")) {
                return Files.readAllBytes(filePath);
            }
            else if (asset.getExtension().equals("md")) {
                MutableDataSet options = new MutableDataSet();

                // TODO: other extensions
                options.set(Parser.EXTENSIONS, Arrays.asList(TablesExtension.create()));

                Parser parser = Parser.builder(options).build();
                com.vladsch.flexmark.html.HtmlRenderer renderer = com.vladsch.flexmark.html.HtmlRenderer.builder(options).build();

                // TODO: re-use parser and renderer instances
                Node document = parser.parse(new String(Files.readAllBytes(filePath)));
                String html = renderer.render(document);
                return html.getBytes();
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
