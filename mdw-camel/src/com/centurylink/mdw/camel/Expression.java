/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
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
