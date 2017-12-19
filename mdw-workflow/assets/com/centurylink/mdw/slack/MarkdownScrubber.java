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

    private String toMarkdown() {
        return input; // TODO
    }
}
