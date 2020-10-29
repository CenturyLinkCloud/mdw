package com.centurylink.mdw.model.workflow;

import com.centurylink.mdw.activity.types.GeneralActivity;
import com.centurylink.mdw.cache.VariableTypeCache;
import com.centurylink.mdw.common.translator.impl.BaseTranslator;
import com.centurylink.mdw.java.CompiledJavaCache;
import com.centurylink.mdw.java.MdwJavaException;
import com.centurylink.mdw.model.system.MdwVersion;
import com.centurylink.mdw.model.variable.VariableType;
import com.centurylink.mdw.pkg.PackageClassLoader;
import com.centurylink.mdw.request.RequestHandler;
import com.centurylink.mdw.spring.SpringAppContext;
import com.centurylink.mdw.translator.DocumentReferenceTranslator;
import com.centurylink.mdw.translator.TranslationException;
import com.centurylink.mdw.variable.VariableTranslator;

import java.io.File;
import java.io.IOException;

public class Package implements Comparable<Package> {

    public static final String MDW = "com.centurylink.mdw";

    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    private MdwVersion version;

    public MdwVersion getVersion() {
        return this.version == null ? new MdwVersion(0) : this.version;
    }

    public void setVersion(MdwVersion version) {
        this.version = version;
    }

    private File directory;

    public File getDirectory() {
        return directory;
    }

    public void setDirectory(File dir) {
        this.directory = dir;
    }

    public String getLabel() {
        return getName() + getVersion().getLabel();
    }

    public Package() {
    }

    public Package(PackageMeta meta, File pkgDir) {
        this.name = meta.getName();
        this.version = meta.getVersion();
        this.directory = pkgDir;
    }

    private PackageClassLoader packageClassLoader = null;

    public PackageClassLoader getClassLoader() {
        if (packageClassLoader == null)
            packageClassLoader = new PackageClassLoader(this);
        return packageClassLoader;
    }

    public GeneralActivity getActivityImplementor(String className)
            throws ReflectiveOperationException, IOException, MdwJavaException {
        // try dynamic java first
        try {
            ClassLoader parentLoader = getClassLoader();
            return (GeneralActivity) CompiledJavaCache.getInstance(className, parentLoader, this);
        } catch (ClassNotFoundException ex) {
            // not located as dynamic java
        }
        GeneralActivity injected = SpringAppContext.getInstance().getActivityImplementor(className, this);
        if (injected != null)
            return injected;
        if (getClassLoader().hasClass(className))
            return getClassLoader().loadClass(className).asSubclass(GeneralActivity.class).newInstance();
        return Package.class.getClassLoader().loadClass(className).asSubclass(GeneralActivity.class).newInstance();
    }

    public RequestHandler getRequestHandler(String className)
            throws ReflectiveOperationException, IOException, MdwJavaException {
        // try dynamic java first (preferred in case patch override is needed)
        try {
            ClassLoader parentLoader = getClassLoader();
            return (RequestHandler) CompiledJavaCache.getInstance(className, parentLoader, this);
        } catch (ClassNotFoundException ex) {
            // not located as dynamic java
        }
        RequestHandler injected = SpringAppContext.getInstance().getRequestHandler(className, this);
        if (injected != null)
            return injected;
        if (getClassLoader().hasClass(className))
            return getClassLoader().loadClass(className).asSubclass(RequestHandler.class).newInstance();
        return Package.class.getClassLoader().loadClass(className).asSubclass(RequestHandler.class).newInstance();
    }

    public VariableTranslator getVariableTranslator(String translatorClass)
            throws ReflectiveOperationException, IOException, MdwJavaException {
        VariableTranslator translator;
        // try dynamic java first (preferred in case patch override is needed)
        try {
            ClassLoader parentLoader = getClassLoader();
            translator = (VariableTranslator) CompiledJavaCache.getInstance(translatorClass, parentLoader, this);
        }
        catch (ClassNotFoundException ex) {
            // not located as dynamic java
            translator = SpringAppContext.getInstance().getVariableTranslator(translatorClass, this);
            if (translator == null) {
                if (getClassLoader().hasClass(translatorClass))
                    translator = getClassLoader().loadClass(translatorClass).asSubclass(VariableTranslator.class).newInstance();
                else
                    translator = Package.class.getClassLoader().loadClass(translatorClass).asSubclass(VariableTranslator.class).newInstance();
            }
        }
        if (translator instanceof BaseTranslator)
            ((BaseTranslator)translator).setPackage(this);
        return translator;
    }

    /**
     * Get a translator instance for the given variable type.
     */
    public VariableTranslator getTranslator(String variableType) throws TranslationException {
        VariableType varType = VariableTypeCache.getVariableType(variableType);
        if (varType == null)
            throw new TranslationException("Variable type not found: " + variableType);
        try {
            return getVariableTranslator(varType.getTranslatorClass());
        } catch (Exception ex) {
            throw new TranslationException("Cannot translate: " + variableType, ex);
        }
    }

    public Object getObjectValue(String variableType, String strValue) throws TranslationException {
        return getObjectValue(variableType, strValue, false, null);
    }

    @Deprecated
    public Object getObjectValue(String variableType, String strValue, boolean deep)
            throws TranslationException {
        return getObjectValue(variableType, strValue, deep, null);
    }

    /**
     * Translates a string to a deserialized object using this package's ClassLoader.
     */
    public Object getObjectValue(String variableType, String strValue, boolean deep, String documentType)
            throws TranslationException {
        if (strValue == null || strValue.trim().isEmpty() || strValue.equals(VariableTranslator.EMPTY_STRING))
            return null;
        VariableTranslator translator = getTranslator(variableType);
        if (translator == null)
            throw new TranslationException("Translator not found: " + variableType);
        if (deep && translator instanceof DocumentReferenceTranslator)
            return ((DocumentReferenceTranslator)translator).toObject(strValue, documentType);
        else
            return translator.toObject(strValue);
    }


    public String getStringValue(String variableType, Object objValue) throws TranslationException {
        return getStringValue(variableType, objValue, false);
    }

    /**
     * Translates an object to its serialized string representation using this package's ClassLoader.
     */
    public String getStringValue(String variableType, Object objValue, boolean deep) throws TranslationException {
        if (objValue == null)
            return deep ? "" : VariableTranslator.EMPTY_STRING;  // TODO: why
        VariableTranslator translator = getTranslator(variableType);
        if (translator == null)
            throw new TranslationException("Translator not found: " + variableType);
        if (deep && translator instanceof DocumentReferenceTranslator)
            return ((DocumentReferenceTranslator)translator).toString(objValue, variableType);
        else
            return translator.toString(objValue);
    }

    @Override
    public String toString() {
        return getLabel();
    }

    @Override
    public int compareTo(Package other) {
        // latest first
        if (this.name.equals(other.name))
            return this.version.compareTo(other.version);
        else
            return this.name.compareToIgnoreCase(other.name);
    }
}
