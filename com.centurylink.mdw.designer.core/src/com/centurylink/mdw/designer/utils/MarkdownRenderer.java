/**
 * Copyright (c) 2017 CenturyLink, Inc. All Rights Reserved.
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
            return "";
        else {
            Parser parser = Parser.builder().build();
            Node document = parser.parse(markdown);
            HtmlRenderer renderer = HtmlRenderer.builder().build();
            return "<div style=\"height:100%;overflow:auto;font-family:'Helvetica Neue', Helvetica, Arial, sans-serif\">" + renderer.render(document) + "</div>";
        }
    }
}
