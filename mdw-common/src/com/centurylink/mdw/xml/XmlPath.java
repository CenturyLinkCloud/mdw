/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.xml;

//import java.util.HashMap;
//import java.util.Map;

import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlCursor.XmlBookmark;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * 
 * XmlPath
 * 
 * This class wraps around name-space agnostic XPath
 * 
 * @version 1.0
 */
public class XmlPath {
    
    private String path_string; // for parsing
    private int n;      // for parsing - string length of path_string
    private int k;      // for parsing - next char position;
    private PathSegment path_seg;   // result of parsing
    
    public XmlPath(String path_string) throws XmlException {
        this.path_string = path_string;
        n = path_string.length();
        k = 0;
        Token token = getNextToken();
        if (token!=null && token.kind==TokenKind.SLASH) token = getNextToken(); // ignore initial slash
        path_seg = parse_path_segment(token);  
    }

    /**
     * Using XPath or XQuery.
     * NOTE!!!! To use this code, need to include
     * xbean_xpath.jar, saxon9.jar, saxon9-dom.jar in CLASSPATH
     * in startWebLogic.cmd
     * @param xmlbean
     * @param path
     * @return
     */

    public static String evaluate(XmlObject xmlbean, String path) {
        XmlCursor cursor = xmlbean.newCursor();
        String value;
        
// 1.2.3. use XQuery or selectPath
//
//        cursor.toFirstChild();
//        String defaultNamespace = cursor.namespaceForPrefix("");
//        String namespaceDecl = "declare default element namespace '" + defaultNamespace + "';";
//        Map<String,String> namespaces = new HashMap<String,String>();
//        cursor.getAllNamespaces(namespaces);
//        for (String prefix : namespaces.keySet())
//        {
//            namespaceDecl += "declare namespace " + prefix + "='" + namespaces.get(prefix) + "';";
//        }
//  1. use XQuery
//        XmlCursor results = cursor.execQuery(namespaceDecl + path);
//        value = (results==null)?null:results.getTextValue();
        
//  2. use selectPath on XmlObject
//        XmlObject[] result = xmlbean.selectPath(namespaceDecl + path);
        
//  3. use selectPath on XmlCursor   
//        cursor.toParent();
//        XmlOptions options = new XmlOptions();
//        options.put(Path.PATH_DELEGATE_INTERFACE,"org.apache.xmlbeans.impl.xpath.saxon.XBeansXPath");
//        cursor.selectPath(namespaceDecl + path, options);
//        if (cursor.getSelectionCount()>0) {
//            cursor.toNextSelection();
//            value = cursor.getTextValue();
//        } else value = null;

// 4. use our own implementation
        try {
            XmlPath matcher = new XmlPath(path);
            value = matcher.evaluate_segment(cursor, matcher.path_seg);
        } catch (XmlException e) {
            value = null; // xpath syntax error - treated as no match
        }
        
        cursor.dispose();
        return value;
    }
    
    public String getHashBucket() {
    	return path_seg.name==null?"*":path_seg.name;	// either a root element name or *
    }
    
    public static String getRootNodeName(Document reqdoc) {
        Node node = reqdoc.getFirstChild();
        while (node!=null && node.getNodeType()!=Node.ELEMENT_NODE) {
            node = node.getNextSibling();
        }
        return node==null?null:node.getNodeName();
    }

    public static String getRootNodeName(XmlObject xmlbean) {
        XmlCursor cursor = xmlbean.newCursor();
        cursor.toFirstChild();
        return cursor.getName().getLocalPart();
    }
    
    public static String getRootNodeValue(XmlObject xmlbean) {
        XmlCursor cursor = xmlbean.newCursor();
        cursor.toFirstChild();
        return cursor.getTextValue();
    }
    
    ////// our own implementation of namespace-agnostic XPath matcher 
    
    private static class Condition {
        String name;
        boolean isAttribute;
        String value;
    }
    
    private static class PathSegment {
        String name;            // special case: "*" - wild card; null - recursive descent
        Condition condition;
        PathSegment rest;
        boolean isAttribute;
    }
    
    enum TokenKind { NAME, AT, VALUE, EQ, LBRACKET, RBRACKET, SLASH, STAR };
    
    private static class Token {
        TokenKind kind;
        int start, end;
    }
    
    public String evaluate(XmlObject xmlbean) {
        XmlCursor cursor = xmlbean.newCursor();
        String value = evaluate_segment(cursor, path_seg);
        cursor.dispose();
        return value;
    }
    
    private static boolean isNameChar(char ch) {
        return ch==':' || ch=='-' || ch=='_' || Character.isLetterOrDigit(ch);
    }
    
    private Token getNextToken() throws XmlException {
        while (k<n && Character.isSpaceChar(path_string.charAt(k))) k++;
        if (k>=n) return null;
        char ch = path_string.charAt(k);
        Token token = new Token();
        token.start = k;
        token.end = k+1;
        switch (ch) {
        case '[': token.kind = TokenKind.LBRACKET; break;
        case ']': token.kind = TokenKind.RBRACKET; break;
        case '@': token.kind = TokenKind.AT; break;
        case '=': token.kind = TokenKind.EQ; break;
        case '*': token.kind = TokenKind.STAR; break;
        case '/': token.kind = TokenKind.SLASH; break;
        case '\'': token.kind = TokenKind.VALUE; getValueToken(token,'\''); break;
        case '"': token.kind = TokenKind.VALUE; getValueToken(token,'"'); break;
        default:
            if (isNameChar(ch)) {
                token.kind = TokenKind.NAME;
                getNameToken(token);
            } else {
                throw new XmlException("Invalid XPath Pattern: " + path_string);
            }
            break;
        }
        k = token.end;
        return token;
    }
    
    private void getNameToken(Token token) {
        int l = k+1;
        while (l<n) {
            char ch = path_string.charAt(l);
            if (!isNameChar(ch)) break;
            l++;
        }
        token.end = l;
    }
    
    private void getValueToken(Token token, char delimiter) {
        int l = k+1;
        while (l<n) {
            char ch = path_string.charAt(l);
            if (ch==delimiter) break;
            l++;
        }
        token.start = k;
        token.end = l+1;
    }
    
    private void parseException(Token token) throws XmlException {
        throw new XmlException("Invalid XPath from character " + (token==null?k:token.start) + ": " + path_string);
    }
    
    private PathSegment parse_path_segment(Token token) throws XmlException {
        PathSegment path = new PathSegment();
        if (token==null) {
            parseException(token);
        } else if (token.kind==TokenKind.NAME) {
            path.isAttribute = false;
            path.name = path_string.substring(token.start, token.end);
            token = getNextToken();
            if (token!=null && token.kind==TokenKind.LBRACKET) {
                token = parse_condition(path_string, n, token, path);
            } else path.condition = null;
            if (token!=null && token.kind==TokenKind.SLASH) {
                path.rest = parse_path_segment(getNextToken());
            } else if (token!=null) {
                parseException(token);
            } else path.rest = null;
        } else if (token.kind==TokenKind.STAR) {
            path.isAttribute = false;
            path.name = "*";
            token = getNextToken();
            if (token!=null && token.kind==TokenKind.LBRACKET) {
                token = parse_condition(path_string, n, token, path);
            } else path.condition = null;
            if (token!=null && token.kind==TokenKind.SLASH) {
                path.rest = parse_path_segment(getNextToken());
            } else if (token!=null) {
                parseException(token);
            } else path.rest = null;
        } else if (token.kind==TokenKind.SLASH) {   // recursive descent
            path.name = null;
            path.isAttribute = false;
            path.rest = parse_path_segment(getNextToken());
        } else if (token.kind==TokenKind.AT) {      // select attribute
            path.isAttribute = true;
            token = getNextToken();
            if (token==null || token.kind!=TokenKind.NAME) parseException(token);
            path.name = path_string.substring(token.start, token.end);
            token = getNextToken();
            if (token!=null) parseException(token);
            path.condition = null;
            path.rest = null;
        } else {
            parseException(token);
        }
        return path;
    }
    
    private Token parse_condition(String path_string, int n, Token token, PathSegment path) 
            throws XmlException {
        token = getNextToken();
        if (token==null)
            throw new XmlException("Invalid XPath Pattern: " + path_string);
        path.condition = new Condition();
        if (token.kind == TokenKind.AT) {
            path.condition.isAttribute = true;
            token = getNextToken();
        } else path.condition.isAttribute = false;
        if (token==null || token.kind!=TokenKind.NAME)
            throw new XmlException("Invalid XPath Pattern: " + path_string);
        path.condition.name = path_string.substring(token.start, token.end);
        token = getNextToken();
        if (token==null || token.kind!=TokenKind.EQ)
            throw new XmlException("Invalid XPath Pattern: " + path_string);
        token = getNextToken();
        if (token==null || token.kind!=TokenKind.VALUE && token.kind!=TokenKind.NAME)
            throw new XmlException("Invalid XPath Pattern: " + path_string);
        if (token.kind==TokenKind.NAME) path.condition.value = path_string.substring(token.start, token.end);
        else path.condition.value = path_string.substring(token.start+1, token.end-1);
        token = getNextToken();
        if (token==null || token.kind!=TokenKind.RBRACKET)
            throw new XmlException("Invalid XPath Pattern: " + path_string);
        token = getNextToken();
        return token;
    }
    
    private String evaluate_segment(XmlCursor cursor, PathSegment path) {
        String value = null;
        if (path.name==null) {
            value = evaluate_recursive_descent(cursor, path.rest);
        } else if (path.isAttribute) {
            XmlBookmark bookmark = new XmlBookmark(){};
            cursor.setBookmark(bookmark);
            boolean more = cursor.toFirstAttribute();
            while (value==null && more) {
                if (cursor.getName().getLocalPart().equals(path.name) || path.name.equals("*")) {
                    value = cursor.getTextValue();
                }
                more = cursor.toNextAttribute();
            }
            cursor.toBookmark(bookmark);
        } else {
            XmlBookmark bookmark = new XmlBookmark(){};
            cursor.setBookmark(bookmark);
            boolean more = cursor.toFirstChild();
            while (value==null && more) {
                if (cursor.getName().getLocalPart().equals(path.name) || path.name.equals("*")) {
                    if (path.condition==null || verify_condition(cursor, path.condition)) {
                        if (path.rest==null) value = cursor.getTextValue();
                        else value = evaluate_segment(cursor, path.rest);
                    }
                }
                more = cursor.toNextSibling();
            }
            cursor.toBookmark(bookmark);
        }
        return value;
    }
    
    private String evaluate_recursive_descent(XmlCursor cursor, PathSegment path) {
        String value = null;
        XmlBookmark bookmark = new XmlBookmark(){};
        cursor.setBookmark(bookmark);
        value = evaluate_segment(cursor, path);
        if (value!=null) return value;
        boolean more = cursor.toFirstChild();
        while (value==null && more) {
            value = evaluate_recursive_descent(cursor, path);
            more = cursor.toNextSibling();
        }
        cursor.toBookmark(bookmark);
        return value;
    }
    
    private boolean verify_condition(XmlCursor cursor, Condition condition) {
        boolean more, found=false;
        XmlBookmark bookmark = new XmlBookmark(){};
        cursor.setBookmark(bookmark);
        if (condition.isAttribute) {
            for (more=cursor.toFirstAttribute(); more&&!found; more=cursor.toNextAttribute()) {
                if (cursor.getName().getLocalPart().equals(condition.name)) {
                    found = cursor.getTextValue().trim().equals(condition.value);
                }
            }
        } else {
            for (more=cursor.toFirstChild(); more&&!found; more=cursor.toNextSibling()) {
                if (cursor.getName().getLocalPart().equals(condition.name)) {
                    found = cursor.getTextValue().trim().equals(condition.value);
                }
            }
        }
        cursor.toBookmark(bookmark);
        return found;
    }
    
    /**
     * Test code
     * @param args
     */
    public static void main(String[] args) {
        String xml = "<Ping xmlns:ps='http:'><G a='8'><ps:H> asf\n</ps:H></G><ps:EventName>START</ps:EventName></Ping>";
        String path_string = "//G[H=asf]";
        try {
            XmlObject xmlBean = XmlObject.Factory.parse(xml);
            XmlPath path = new XmlPath(path_string);
            String value = path.evaluate(xmlBean);
            System.out.println("Value=" + value);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
}
