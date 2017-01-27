/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.designer.utils;

import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

public class MarkdownRenderer {

    private String markdown;

    public MarkdownRenderer(String markdown) {
        this.markdown = markdown;
    }

    public String renderHtml() {
        if (markdown == null || markdown.trim().isEmpty())
            return "<html></html>";
        else {
            Parser parser = Parser.builder().build();
            Node document = parser.parse(markdown);
            HtmlRenderer renderer = HtmlRenderer.builder().build();
            return renderer.render(document);
        }
    }
}
