package com.centurylink.mdw.slack;

/**
 * Slack doesn't support real markdown, so we need to translate
 * to/from the MDW Discussion tab.  TODO: very crude at the moment
 */
public class MarkdownScrubber {

    private static final String BULLET = String.valueOf((char)8226);

    private String input;
    public MarkdownScrubber(String input) {
        this.input = input;
    }

    public String toSlack() {
        return input
            // italics
            .replaceAll("\\*([^\\s]+?)\\*", "_$1_")
            // bold
            .replaceAll("_\\*([^\\s]+?)_\\*", "*$1*")
            .replaceAll("__([^\\s]+?)__", "*$1*")
            // strikethrough
            .replaceAll("~~([^\\s]+?)~~", "~$1~")
            // unordered lists
            .replaceAll("(\n\\s*)-(.*)", "$1" + BULLET + "$2")
            .replaceAll("(\n\\s*)\\*(.*)", "$1" + BULLET + "$2")
            // links: <http://www.foo.com|www.foo.com>
            .replaceAll("\\[(.+)]\\((.+)\\)", "<$2|$1>");
    }

    public String toMarkdown() {
        return input
            // bold
            .replaceAll("\\*([^\\s]+?)\\*", "**$1**")
            // italics
            .replaceAll("_([^\\s]+?)_", "*$1*")
            // strikethrough
            .replaceAll("~([^\\s]+?)~", "~~$1~~")
            // unordered lists
            .replaceAll("(\n\\s*)" + BULLET + "(.*)", "$1-$2");
    }
}
