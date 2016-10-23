/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.app;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlOptions;

import com.centurylink.mdw.util.file.FileHelper;

public class Compatibility {

    public static final String NAMESPACES_MAP = "namespaces.map";
    public static final String ACTIVITY_IMPLEMENTORS_MAP = "activity-implementors.map";
    public static final String EVENT_HANDLERS_MAP = "event-handlers.map";
    public static final String VARIABLE_TRANSLATORS_MAP = "variable-translators.map";
    public static final String VARIABLE_TYPES_MAP = "variable-types.map";
    public static final String DOCUMENTATION_PATHS_MAP = "documentation-paths.map";

    public static final String CODE_SUBSTITUTIONS_MAP = "code-substitutions.map";
    public static final String PAGE_SUBSTITUTIONS_MAP = "page-substitutions.map";

    private static boolean inEffect = true;
    public static boolean isInEffect() { return inEffect; }

    private static Compatibility instance;
    public static synchronized Compatibility getInstance() throws IOException {
            if (instance == null) {
                instance = new Compatibility();
                instance.load();
            }
        return instance;
    }

    public static Map<String,String> getNamespaces() throws IOException {
        if (!inEffect)
            return null;
        return getInstance().namespaces;
    }

    public static Map<String,String> getReverseNamespaces() throws IOException {
        return getInstance().reverseNamespaces;
    }

    public static XmlOptions namespaceOptions() throws XmlException {
        XmlOptions xmlOptions = new XmlOptions();
        try {
            Map<String,String> map = getNamespaces();
            if (map != null)
                xmlOptions.setLoadSubstituteNamespaces(map);
            return xmlOptions;
        }
        catch (IOException ex) {
            throw new XmlException(ex.getMessage(), ex);
        }
    }

    public static XmlOptions getReverseNamespaceOptions() throws XmlException {
        XmlOptions xmlOptions = new XmlOptions();
        try {
            Map<String,String> reverseMap = getReverseNamespaces();
            if (reverseMap != null)
                xmlOptions.setLoadSubstituteNamespaces(reverseMap);
            return xmlOptions;
        }
        catch (IOException ex) {
            throw new XmlException(ex.getMessage(), ex);
        }
    }

    public static Map<String,String> getActivityImplementors() throws IOException {
        if (!inEffect)
            return null;
        return getInstance().activityImplementors;
    }

    /**
     * If the className argument needs to be mapped to a new implementor class,
     * returns the new class name; otherwise returns the unmodified argument.
     */
    public static String getActivityImplementor(String className) throws IOException {
        if (getActivityImplementors() != null) {
            String newImpl = getActivityImplementors().get(className);
            if (newImpl != null)
                return newImpl;
        }
        return className;
    }

    public static boolean isOldImplementor(String className) throws IOException {
        return getActivityImplementors() != null && getActivityImplementors().containsKey(className);
    }

    public static Map<String,String> getEventHandlers() throws IOException {
        if (!inEffect)
            return null;
        return getInstance().eventHandlers;
    }

    /**
     * If the className argument needs to be mapped to a new event handler class,
     * returns the new class name; otherwise returns the unmodified argument.
     */
    public static String getEventHandler(String className) throws IOException {
        if (getEventHandlers() != null) {
            String newHandler = getEventHandlers().get(className);
            if (newHandler != null)
                return newHandler;
        }
        return className;
    }

    /**
     * If the className argument needs to be mapped to a new variable translator class,
     * returns the new class name; otherwise returns the unmodified argument.
     */
    public static String getVariableTranslator(String className) throws IOException {
        if (getVariableTranslators() != null) {
            String newTranslator = getVariableTranslators().get(className);
            if (newTranslator != null)
                return newTranslator;
        }
        return className;
    }

    public static Map<String,String> getVariableTranslators() throws IOException {
        if (!inEffect)
            return null;
        return getInstance().variableTranslators;
    }

    /**
     * If the type argument needs to be mapped to a new variable type class,
     * returns the new class name; otherwise returns the unmodified argument.
     */
    public static String getVariableType(String type) throws IOException {
        if (getVariableTypes() != null) {
            String newType = getVariableTypes().get(type);
            if (newType != null)
                return newType;
        }
        return type;
    }

    public static Map<String,String> getVariableTypes() throws IOException {
        if (!inEffect)
            return null;
        return getInstance().variableTypes;
    }

    /**
     * If the docPath argument needs to be mapped to a new location,
     * returns the new location path; otherwise returns the unmodified argument.
     */
    public static String getDocumentationPath(String docPath) throws IOException {
        if (getDocumentationPaths() != null) {
            String newPath = getDocumentationPaths().get(docPath);
            if (newPath != null)
                return newPath;
        }
        return docPath;
    }

    public static Map<String,String> getDocumentationPaths() throws IOException {
        if (!inEffect)
            return null;
        return getInstance().documentationPaths;
    }

    public static boolean hasCodeSubstitutions() throws IOException {
        if (!inEffect)
            return false;
        return getInstance().codeSubstitutions != null && getInstance().codeSubstitutions.size() > 0;
    }

    public static boolean hasPageSubstitutions() throws IOException {
        if (!inEffect)
            return false;
        return getInstance().pageSubstitutions != null && getInstance().pageSubstitutions.size() > 0;
    }

    protected Compatibility() {}

    private Map<String,String> namespaces;
    private Map<String,String> reverseNamespaces;

    private Map<String,String> activityImplementors;
    private Map<String,String> eventHandlers;
    private Map<String,String> variableTranslators;
    private Map<String,String> variableTypes;
    private Map<String,String> documentationPaths;
    private Map<String,String> codeSubstitutions;
    private Map<String,String> pageSubstitutions;

    private void load() throws IOException {
        namespaces = loadMap(NAMESPACES_MAP);
        if (namespaces != null) {
            reverseNamespaces = new HashMap<String,String>();
            for (String key : namespaces.keySet())
                reverseNamespaces.put(namespaces.get(key), key);
        }
        activityImplementors = loadMap(ACTIVITY_IMPLEMENTORS_MAP);
        eventHandlers = loadMap(EVENT_HANDLERS_MAP);
        variableTranslators = loadMap(VARIABLE_TRANSLATORS_MAP);
        variableTypes = loadMap(VARIABLE_TYPES_MAP);
        documentationPaths = loadMap(DOCUMENTATION_PATHS_MAP);
        codeSubstitutions = loadMap(CODE_SUBSTITUTIONS_MAP);
        pageSubstitutions = loadMap(PAGE_SUBSTITUTIONS_MAP);
    }

    private Map<String,String> loadMap(String propFile) throws IOException {
        InputStream stream = null;
        try {
            stream = openCompatibilityFile(propFile);
            if (stream == null)
                return null;
            else
                return mapProperties(stream);
        }
        finally {
            if (stream != null)
                stream.close();
        }
    }

    /**
     * Map returns
     */
    private Map<String,String> mapProperties(InputStream mapFileStream) throws IOException {
        OrderedProperties props = new OrderedProperties();
        props.load(mapFileStream);
        Map<String,String> map = new LinkedHashMap<String,String>();
        for (Object key : props.getOrderedMap().keySet())
            map.put(key.toString(), props.getProperty(key.toString()));
        return map;
    }

    private InputStream openCompatibilityFile(String filename) throws IOException {
        return FileHelper.readFile(filename, Compatibility.class.getClassLoader());
    }

    public SubstitutionResult performCodeSubstitutions(String input) throws IOException {
        return performSubstitutions(input, codeSubstitutions);
    }

    public SubstitutionResult performPageSubstitutions(String input) throws IOException {
        return performSubstitutions(input, pageSubstitutions);
    }

    private SubstitutionResult performSubstitutions(String input, Map<String,String> substitutions) throws IOException {
        SubstitutionResult result = new SubstitutionResult();
        result.setOutput(input);
        if (substitutions != null && !substitutions.isEmpty()) {
            for (String search : substitutions.keySet()) {
                String replace = substitutions.get(search);
                StringBuffer substituted = new StringBuffer(result.getOutput().length());
                Matcher matcher = getPattern(search + "|\\n").matcher(result.getOutput());
                int index = 0;
                int line = 1;
                while (matcher.find()) {
                    String found = matcher.group();
                    if (found.equals("\n")) {
                        line++;
                    }
                    else {
                        result.addSubstitution(line, new Substitution(line, found, replace));
                        substituted.append(result.getOutput().substring(index, matcher.start()));
                        substituted.append(replace);
                        index = matcher.end();
                    }
                }
                substituted.append(result.getOutput().substring(index));
                result.setOutput(substituted.toString());
            }
        }
        return result;
    }

    private Map<String, Pattern> compiledPatterns = new HashMap<String, Pattern>();

    private Pattern getPattern(String regex) {
        Pattern pattern = compiledPatterns.get(regex);
        if (pattern == null) {
            pattern = Pattern.compile(regex);
            compiledPatterns.put(regex, pattern);
        }
        return pattern;
    }

    public class SubstitutionResult {
        private String output;
        public String getOutput() { return output; }
        public void setOutput(String output) { this.output = output; }

        private int total;

        private Map<Integer,List<Substitution>> substitutions = new TreeMap<Integer,List<Substitution>>();
        public void addSubstitution(int line, Substitution substitution) {
            List<Substitution> subs = substitutions.get(line);
            if (subs == null) {
                subs = new ArrayList<Substitution>();
                substitutions.put(line, subs);
            }
            subs.add(substitution);
            total++;
        }

        public boolean isEmpty() {
            return total == 0;
        }


        public String getSummary() {
            StringBuffer summary = new StringBuffer();
            summary.append(total).append(" total replacements in ").append(substitutions.size()).append(" lines");
            return summary.toString();
        }

        public String getDetails() {
            StringBuffer details = new StringBuffer(getSummary()).append(":\n");
            for (int line : substitutions.keySet()) {
                details.append("  ").append(line).append(": ");
                List<Substitution> subs = substitutions.get(line);
                for (int i = 0; i < subs.size(); i++) {
                    Substitution sub = subs.get(i);
                    details.append("replaced '").append(sub.found).append("' with '").append(sub.replacedWith).append("'");
                    if (i < subs.size() - 1)
                        details.append(", ");
                }
                details.append("\n");
            }
            return details.toString();
        }
    }

    public class Substitution {
        int line;
        String found;
        String replacedWith;

        public Substitution(int line, String found, String replaced) {
            this.line = line;
            this.found = found;
            this.replacedWith = replaced;
        }
    }

    private class OrderedProperties extends Properties {
        private final Map<Object,Object> orderedMap = new LinkedHashMap<Object,Object>();
        Map<Object,Object> getOrderedMap() { return orderedMap; }

        public Object put(Object key, Object value) {
            orderedMap.put(key, value);
            return super.put(key, value);
        }
    }

}
