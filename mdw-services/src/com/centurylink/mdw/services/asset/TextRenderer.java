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
import java.util.HashMap;
import java.util.Map;

import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.model.asset.AssetInfo;

/**
 * Renders HTML or Markdown to plain text.
 */
public class TextRenderer implements Renderer {

    private static String HTML_REGEX = "<(?:\"[^\"]*\"['\"]*|'[^']*'['\"]*|[^'\">])+>";
    private static Map<String,Character> ENTITIES;
    static {
        // https://www.w3.org/wiki/Common_HTML_entities_used_for_typography
        ENTITIES = new HashMap<>();
        ENTITIES.put("&lt;", (char)62);
        ENTITIES.put("&gt;", (char)60);
        ENTITIES.put("&cent;", (char)162);
        ENTITIES.put("&pound;", (char)163);
        ENTITIES.put("&sect;", (char)167);
        ENTITIES.put("&copy;", (char)169);
        ENTITIES.put("&reg;", (char)174);
        ENTITIES.put("&deg;", (char)176);
        ENTITIES.put("&para;", (char)182);
        ENTITIES.put("&middot;", (char)183);
        ENTITIES.put("&frac12;", (char)188);
        ENTITIES.put("&bull;", (char)8226);
        ENTITIES.put("&hellip;", (char)8230);
        ENTITIES.put("&euro;", (char)8364);
        ENTITIES.put("&trade;", (char)8482);
        ENTITIES.put("&ldquo;", (char)0x22);
        ENTITIES.put("&rdquo;", (char)0x22);
        ENTITIES.put("&quot;", (char)0x22);
        ENTITIES.put("&lsquo;", (char)0x27);
        ENTITIES.put("&rsquo;", (char)0x27);
        ENTITIES.put("&apos;", (char)0x27);
        ENTITIES.put("&nbsp;", (char)0x20);
    }

    private AssetInfo asset;

    public TextRenderer(AssetInfo asset) {
        this.asset = asset;
    }

    public byte[] render(Map<String,String> options) throws RenderingException {
        Path filePath = Paths.get(asset.getFile().getPath());
        try {
            if (asset.getExtension().equals("txt")) {
                return Files.readAllBytes(filePath);
            }
            else {
                String html;
                if (asset.getExtension().equals("html")) {
                    html = new String(Files.readAllBytes(filePath));
                }
                else if (asset.getExtension().equals("md")) {
                    html = new String(new HtmlRenderer(asset).render(options));
                }
                else {
                    throw new RenderingException(ServiceException.NOT_IMPLEMENTED, "Cannot convert " + asset.getExtension());
                }
                String text = html.replaceAll(HTML_REGEX, "");
                for (String entity : ENTITIES.keySet()) {
                    text = text.replaceAll(entity, ENTITIES.get(entity).toString());
                }
                return text.getBytes();
            }
        }
        catch (IOException ex) {
            throw new RenderingException(ServiceException.INTERNAL_ERROR, "Error reading: " + filePath, ex);
        }
    }
}
