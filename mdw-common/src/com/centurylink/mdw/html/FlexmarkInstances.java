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
