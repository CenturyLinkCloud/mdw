/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.script;

import groovy.lang.Binding;
import groovy.util.GroovyScriptEngine;
import groovy.util.XmlSlurper;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Map;
import java.util.StringTokenizer;

import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.common.Compatibility;
import com.centurylink.mdw.common.Compatibility.SubstitutionResult;
import com.centurylink.mdw.common.cache.impl.PackageVOCache;
import com.centurylink.mdw.common.cache.impl.RuleSetCache;
import com.centurylink.mdw.common.exception.CachingException;
import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.translator.DocumentReferenceTranslator;
import com.centurylink.mdw.common.translator.VariableTranslator;
import com.centurylink.mdw.common.translator.impl.XmlBeanWrapperTranslator;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.common.utilities.timer.CodeTimer;
import com.centurylink.mdw.java.JavaNaming;
import com.centurylink.mdw.model.value.attribute.RuleSetVO;
import com.centurylink.mdw.model.value.process.PackageVO;

public class GroovyExecutor implements ScriptExecutor, ScriptEvaluator {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    private static String getRootDir() {
        String rootDirStr = ApplicationContext.getTempDirectory();
        File rootDir = new File(rootDirStr);
        if (!rootDir.exists())
            rootDir.mkdirs();
        return rootDirStr;
    }

    private static Map<String,String> scriptCache = new Hashtable<String,String>();
    public static void clearCache() {
        scriptCache.clear();
    }

    private String name;
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    private Binding binding;
    protected Binding getBinding() { return binding; }

    public Object execute(String script, Map<String,Object> bindings) throws ExecutionException {
            script += "\nreturn;";  // default to returning null

            binding = new Binding();

            for (String bindName : bindings.keySet()) {
                Object value = bindings.get(bindName);
                binding.setVariable(bindName, value);
            }

            Object retObj = runScript(script);

            for (String bindName : bindings.keySet()) {
                Object value = binding.getVariable(bindName);
                bindings.put(bindName, value);
            }

            return retObj;
    }

    public Object evaluate(String expression, Map<String, Object> bindings)
    throws ExecutionException {
        binding = new Binding();

        for (String bindName : bindings.keySet()) {
            Object value = bindings.get(bindName);
            DocumentReferenceTranslator docRefTranslator = getDocRefTranslator(value);
            if (docRefTranslator != null) {
                try {
                    if (!(docRefTranslator instanceof XmlBeanWrapperTranslator)) {
                        value = new XmlSlurper().parseText(docRefTranslator.realToString(value));
                    }
                }
                catch (Exception ex) {
                    throw new ExecutionException("Cannot parse document content: '" + bindName + "'", ex);
                }
            }
            binding.setVariable(bindName, value);
        }

        return runScript(expression);
    }

    protected Object runScript(String script) throws ExecutionException {
        try {
            CodeTimer timer = new CodeTimer("Create and cache groovy script", true);
            File groovyFile = new File(getRootDir() + "/" + name + ".groovy");
            synchronized(scriptCache) {
                String cached = scriptCache.get(name);
                if (!groovyFile.exists() || !script.equals(cached)) {
                    File rootDir = new File(getRootDir());
                    if (!rootDir.exists()) {
                        if (!rootDir.mkdirs())
                            throw new ExecutionException("Failed to create script root dir: " + rootDir);
                    }
                    FileWriter writer = null;
                    try {
                        writer = new FileWriter(groovyFile);
                        writer.write(script);
                    }
                    finally {
                        if (writer != null)
                            writer.close();
                        timer.stopAndLogTiming("");
                    }
                    scriptCache.put(name, script);
                }
            }
            return getScriptEngine().run(name + ".groovy", binding);
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
            throw new ExecutionException("Error executing Groovy script: '" + name + "'\n" + ex.toString(), ex);
        }
    }

    private DocumentReferenceTranslator getDocRefTranslator(Object value) {

        if (value instanceof org.apache.xmlbeans.XmlObject)
          return (DocumentReferenceTranslator) VariableTranslator.getTranslator(org.apache.xmlbeans.XmlObject.class.getName());
        else if (value instanceof org.w3c.dom.Document)
          return (DocumentReferenceTranslator) VariableTranslator.getTranslator(org.w3c.dom.Document.class.getName());
        else if (value instanceof com.centurylink.mdw.xml.XmlBeanWrapper)
          return (DocumentReferenceTranslator) VariableTranslator.getTranslator(com.centurylink.mdw.xml.XmlBeanWrapper.class.getName());
        else if (value instanceof com.centurylink.mdw.model.FormDataDocument)
            return (DocumentReferenceTranslator) VariableTranslator.getTranslator(com.centurylink.mdw.model.FormDataDocument.class.getName());
        else if (value instanceof groovy.util.Node)
          return (DocumentReferenceTranslator) VariableTranslator.getTranslator(groovy.util.Node.class.getName());
        else if (value instanceof com.qwest.mbeng.MbengDocument)
          return (DocumentReferenceTranslator) VariableTranslator.getTranslator(com.qwest.mbeng.MbengDocument.class.getName());
        else
          return null;
    }

    private static GroovyScriptEngine scriptEngine;
    private static GroovyScriptEngine getScriptEngine() throws DataAccessException, IOException, CachingException {
        synchronized (GroovyScriptEngine.class) {
            if (scriptEngine == null || !RuleSetCache.isLoaded()) {
                CodeTimer timer = new CodeTimer("Initialize script libraries", true);
                initializeScriptLibraries();
                initializeDynamicJavaAssets();
                String[] rootDirs = new String[] { getRootDir() };
                scriptEngine = new GroovyScriptEngine(rootDirs);
                // clear the cached library versions
                scriptEngine.getGroovyClassLoader().clearCache();
                timer.stopAndLogTiming("");
            }
        }

        return scriptEngine;
    }

    public static void initialize() throws DataAccessException, IOException, CachingException {
        clearCache();
        scriptEngine = null;
        getScriptEngine();
    }

    private static void initializeScriptLibraries() throws DataAccessException, IOException, CachingException {
        // write the groovy-language rule_sets into the root directory
        logger.info("Initializing Groovy script assets...");

        for (RuleSetVO groovy : RuleSetCache.getRuleSets(RuleSetVO.GROOVY)) {
            PackageVO pkg = PackageVOCache.getRuleSetPackage(groovy.getId());
            String packageName = pkg == null ? null : JavaNaming.getValidPackageName(pkg.getPackageName());
            File dir = createNeededDirs(packageName);
            String filename = dir + "/" + groovy.getName();
            if (!filename.endsWith(".groovy"))
                filename += ".groovy";
            File file = new File(filename);
            logger.debug("  - writing " + file.getAbsoluteFile());
            if (file.exists())
                file.delete();

            String content = groovy.getRuleSet();
            if (content != null) {
                if (Compatibility.hasCodeSubstitutions())
                    content = doCompatibilityCodeSubstitutions(groovy.getLabel(), content);
                FileWriter writer = new FileWriter(file);
                writer.write(content);
                writer.close();
            }
        }
        logger.info("Groovy script assets initialized.");
    }

    /**
     * so that Groovy scripts can reference these assets during compilation
     */
    private static void initializeDynamicJavaAssets() throws DataAccessException, IOException, CachingException {
        logger.info("Initializing Dynamic Java assets for Groovy...");

        for (RuleSetVO java : RuleSetCache.getRuleSets(RuleSetVO.JAVA)) {
            PackageVO pkg = PackageVOCache.getRuleSetPackage(java.getId());
            String packageName = pkg == null ? null : JavaNaming.getValidPackageName(pkg.getPackageName());
            File dir = createNeededDirs(packageName);
            String filename = dir + "/" + java.getName();
            if (filename.endsWith(".java"))
                filename = filename.substring(0, filename.length() - 5);
            filename += ".groovy";
            File file = new File(filename);
            logger.mdwDebug("  - writing " + file.getAbsoluteFile());
            if (file.exists())
                file.delete();

            String content = java.getRuleSet();
            if (content != null) {
                if (Compatibility.hasCodeSubstitutions())
                    content = doCompatibilityCodeSubstitutions(java.getLabel(), content);
                FileWriter writer = new FileWriter(file);
                writer.write(content);
                writer.close();
            }
        }
        logger.info("Dynamic Java assets initialized for groovy.");
    }

    private static File createNeededDirs(String packageName) {

        String path = getRootDir();
        File dir = new File(path);
        if (packageName != null) {
            StringTokenizer st = new StringTokenizer(packageName, ".");
            while (st.hasMoreTokens()) {
                String token = st.nextToken();
                path += "/" + token;
                dir = new File(path);
                if (!dir.exists())
                    dir.mkdir();
            }
        }

        return dir;
    }

    public static ClassLoader getClassLoader() {
        try {
            return getScriptEngine().getGroovyClassLoader();
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
            return null;
        }
    }

    protected static String doCompatibilityCodeSubstitutions(String label, String in) throws IOException {
        SubstitutionResult substitutionResult = Compatibility.getInstance().performCodeSubstitutions(in);
        if (!substitutionResult.isEmpty()) {
            logger.warn("Compatibility substitutions applied for Groovy asset " + label + " (details logged at debug level).");
            if (logger.isDebugEnabled())
                logger.debug("Compatibility substitutions for " + label + ":\n" + substitutionResult.getDetails());
            if (logger.isMdwDebugEnabled())
                logger.mdwDebug("Substitution output for " + label + ":\n" + substitutionResult.getOutput());
            return substitutionResult.getOutput();
        }
        return in;
    }

}
