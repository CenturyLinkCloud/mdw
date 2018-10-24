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
package com.centurylink.mdw.html;

import java.util.Arrays;

import com.vladsch.flexmark.ext.anchorlink.AnchorLinkExtension;
import com.vladsch.flexmark.ext.autolink.AutolinkExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.ext.typographic.TypographicExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.superscript.SuperscriptExtension;
import com.vladsch.flexmark.util.options.MutableDataSet;

public class FlexmarkInstances {
    private static MutableDataSet renderOptions;

    private static Parser parser;

    private static HtmlRenderer renderer;
    
    static {
        renderOptions = new MutableDataSet();
        renderOptions.set(Parser.EXTENSIONS,
                Arrays.asList(AnchorLinkExtension.create(),
                        AutolinkExtension.create(),
                        SuperscriptExtension.create(),
                        TablesExtension.create(),
                        TypographicExtension.create()));
        parser = Parser.builder(renderOptions).build();
        renderer = HtmlRenderer.builder(renderOptions).build();
    }
    
    private FlexmarkInstances() {
        
    }

    public static Parser getParser(MutableDataSet renderOptions) {
        if (renderOptions == null)
            return parser;
        return Parser.builder(renderOptions).build();
    }

    public static HtmlRenderer getRenderer(MutableDataSet renderOptions) {
        if (renderOptions == null)
            return renderer;
        return HtmlRenderer.builder(renderOptions).build();
    }
}
