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
package com.centurylink.mdw.camel;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.camel.CamelContext;
import org.apache.camel.Message;
import org.apache.camel.builder.xml.XPathBuilder;

public class Expression {

    private static Pattern tokenPattern = Pattern.compile("([\\$#]\\{.*?\\})");

    private CamelContext camelContext;
    private String input;
    private Map<String,String> namespaces;

    public Expression(CamelContext camelContext, String input, Map<String,String> namespaces) {
        this.camelContext = camelContext;
        this.input = input;
        this.namespaces = namespaces;
    }

    public String substitute(Message message) {
        StringBuffer substituted = new StringBuffer(input.length());
        Matcher matcher = tokenPattern.matcher(input);
        int index = 0;
        while (matcher.find()) {
            String match = matcher.group();
            substituted.append(input.substring(index, matcher.start()));
            Object value = evaluateXPath(match.substring(2, match.length() - 1), message);
            if (value != null)
                substituted.append(value);
            index = matcher.end();
        }
        substituted.append(input.substring(index));
        return substituted.toString();
    }

    private String evaluateXPath(String expression, Message message) {
        XPathBuilder xpath = XPathBuilder.xpath(expression);

        if (namespaces != null)
            xpath.setNamespaces(namespaces);
        return xpath.evaluate(camelContext, message.getBody(), String.class);
    }

}
