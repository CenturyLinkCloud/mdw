/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.translator;

import com.centurylink.mdw.common.Compatibility;
import com.centurylink.mdw.common.cache.impl.VariableTypeCache;
import com.centurylink.mdw.common.exception.TranslationException;
import com.centurylink.mdw.common.provider.ProviderRegistry;
import com.centurylink.mdw.common.spring.SpringAppContext;
import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.model.value.process.PackageVO;
import com.centurylink.mdw.model.value.variable.VariableTypeVO;

public abstract class VariableTranslator implements com.centurylink.mdw.variable.VariableTranslator {

    protected static String EMPTY_STRING = "<EMPTY>";
    protected static String  ARRAY_DELIMETER = "~";


    private PackageVO pkg;
    public PackageVO getPackage() { return pkg; }
    public void setPackage(PackageVO pkg) { this.pkg = pkg; }

    /**
     * Converts the passed in object to a string
     * @param pObject
     * @return String
     */
    public abstract String toString(Object pObject)
    throws TranslationException;

    /**
     * converts the passed in string to an equivalent object
     * @param pStr
     * @return Object
     */
    public abstract Object toObject(String pStr)
    throws TranslationException;



    /**
    * Returns the translator for the passed in pType
    * @param type
    * @return Translator
    */
    public static final com.centurylink.mdw.variable.VariableTranslator getTranslator(PackageVO packageVO, String type) {
        com.centurylink.mdw.variable.VariableTranslator trans = null;
        try {
            VariableTypeVO vo = VariableTypeCache.getVariableTypeVO(Compatibility.getVariableType(type));
            if (vo == null)
                return null;

            // try dynamic java first (preferred in case patch override is needed)
            ClassLoader parentLoader = packageVO == null ? VariableTranslator.class.getClassLoader() : packageVO.getClassLoader();
            trans = ProviderRegistry.getInstance().getDynamicVariableTranslator(vo.getTranslatorClass(), parentLoader);
            if (trans == null) {
                com.centurylink.mdw.variable.VariableTranslator injected
                    = SpringAppContext.getInstance().getVariableTranslator(vo.getTranslatorClass(), packageVO);
                if (injected != null)
                    trans = injected;
                if (trans == null) {
                    Class<?> cl = Class.forName(vo.getTranslatorClass());
                    trans = (VariableTranslator)cl.newInstance();
                }
            }
        } catch (Throwable th) {
            th.printStackTrace();
            trans = null;
        }
        if (trans != null)
            trans.setPackage(packageVO);
        return trans;
    }

    public static final com.centurylink.mdw.variable.VariableTranslator getTranslator(String type) {
        return getTranslator(null, type);
    }

    /**
    * Translates the Passed in String value based on the
    * Passed in type
    * @param type
    * @param value
    * @return Translated Object
    */
    public static Object toObject(String type, String value){
        if(StringHelper.isEmpty(value) || EMPTY_STRING.equals(value)){
            return null;
        }
        com.centurylink.mdw.variable.VariableTranslator trans = getTranslator(type);
        return trans.toObject(value);
    }

    @Deprecated
    public static String toString(String type, Object value) {
        return toString(null, type, value);
    }

    /**
    * Returns String value for the passed in Type and Value
    * @param pkg workflow package
    * @param type variable type
    * @param value object value
    */
    public static String toString(PackageVO pkg, String type, Object value) {
        if (value == null) {
            return EMPTY_STRING;
        }
        com.centurylink.mdw.variable.VariableTranslator trans = getTranslator(pkg, type);
        return trans.toString(value);
    }

    /**
     * @deprecated use realToString(PackageVO, String, Object)
     */
     @Deprecated
     public static String realToString(String type, Object value) {
         com.centurylink.mdw.variable.VariableTranslator trans = getTranslator(type);
         if (trans instanceof DocumentReferenceTranslator)
             return ((DocumentReferenceTranslator)trans).realToString(value);
         else return trans.toString(value);
     }

    /**
     * Serializes a runtime variable object to its string value.  Documents are expanded.
     * @param pkg workflow package
     * @param type variable type
     * @param value object value
     * @return serialized string value
     */
    public static String realToString(PackageVO pkg, String type, Object value) {
        com.centurylink.mdw.variable.VariableTranslator trans = getTranslator(pkg, type);
        if (trans instanceof DocumentReferenceTranslator)
            return ((DocumentReferenceTranslator)trans).realToString(value);
        else
            return trans.toString(value);
    }

    /**
     * Deserializes variable string values to runtime objects.
     *
     * @param pkg workflow package
     * @param type variable type
     * @param value string value
     * @return deserialized object
     */
    public static Object realToObject(PackageVO pkg, String type, String value) {
        com.centurylink.mdw.variable.VariableTranslator trans = getTranslator(pkg, type);
        if (trans instanceof DocumentReferenceTranslator)
            return ((DocumentReferenceTranslator)trans).realToObject(value);
        else
            return trans.toObject(value);
    }

    @Deprecated
    public static boolean isDocumentReferenceVariable(String type) {
        return isDocumentReferenceVariable(null, type);
    }

    /**
     * If pkg is null then will use any available bundle to provide the translator.
     */
    public static boolean isDocumentReferenceVariable(PackageVO pkg, String type) {
        com.centurylink.mdw.variable.VariableTranslator trans = getTranslator(pkg, type);
        return (trans instanceof DocumentReferenceTranslator);
    }

    @Deprecated
    public static boolean isXmlDocumentTranslator(String type) {
        return isXmlDocumentTranslator(null, type);
    }

    /**
     * If pkg is null then will use any available bundle to provide the translator.
     */
    public static boolean isXmlDocumentTranslator(PackageVO pkg, String type) {
        com.centurylink.mdw.variable.VariableTranslator trans = getTranslator(pkg, type);
        return (trans instanceof XmlDocumentTranslator);
    }
}