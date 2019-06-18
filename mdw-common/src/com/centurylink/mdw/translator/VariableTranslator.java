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
package com.centurylink.mdw.translator;

import com.centurylink.mdw.app.Compatibility;
import com.centurylink.mdw.cache.impl.VariableTypeCache;
import com.centurylink.mdw.dataaccess.file.MdwBaselineData;
import com.centurylink.mdw.model.variable.VariableType;
import com.centurylink.mdw.model.workflow.Package;
import com.centurylink.mdw.provider.ProviderRegistry;
import com.centurylink.mdw.spring.SpringAppContext;
import org.apache.commons.lang.StringUtils;

import java.util.List;

public abstract class VariableTranslator implements com.centurylink.mdw.variable.VariableTranslator {

    protected static String EMPTY_STRING = "<EMPTY>";
    protected static String  ARRAY_DELIMETER = "~";

    private static final List<VariableType> mdwVariableTypes = new MdwBaselineData().getVariableTypes();

    private Package pkg;
    public Package getPackage() { return pkg; }
    public void setPackage(Package pkg) { this.pkg = pkg; }

    /**
     * Converts the passed in object to a string
     * @param pObject object
     * @return String
     */
    public abstract String toString(Object pObject)
    throws TranslationException;

    /**
     * converts the passed in string to an equivalent object
     * @param pStr string
     * @return Object
     */
    public abstract Object toObject(String pStr)
    throws TranslationException;



    /**
    * Returns the translator for the passed in pType
    * @param type type
    * @return Translator
    */
    public static final com.centurylink.mdw.variable.VariableTranslator getTranslator(Package packageVO, String type) {
        com.centurylink.mdw.variable.VariableTranslator trans = null;
        try {
            VariableType vo = VariableTypeCache.getVariableTypeVO(Compatibility.getVariableType(type));
            if (vo == null)
                return null;

            // try dynamic java first (preferred in case patch override is needed / Exclude MDW built-in translators)
            if (!mdwVariableTypes.contains(vo)) {
                ClassLoader parentLoader = packageVO == null ? VariableTranslator.class.getClassLoader() : packageVO.getClassLoader();
                trans = ProviderRegistry.getInstance().getDynamicVariableTranslator(vo.getTranslatorClass(), parentLoader);
            }
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
    * @param type type
    * @param value value
    * @return Translated Object
    */
    public static Object toObject(String type, String value){
        if (StringUtils.isBlank(value) || EMPTY_STRING.equals(value)){
            return null;
        }
        com.centurylink.mdw.variable.VariableTranslator trans = getTranslator(type);
        if (trans != null)
            return trans.toObject(value);
        else
            return null;
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
    public static String toString(Package pkg, String type, Object value) {
        if (value == null) {
            return EMPTY_STRING;
        }
        com.centurylink.mdw.variable.VariableTranslator trans = getTranslator(pkg, type);
        if (trans != null)
            return trans.toString(value);
        else
            return "";
    }

    /**
     * Serializes a runtime variable object to its string value.  Documents are expanded.
     * @param pkg workflow package
     * @param type variable type
     * @param value object value
     * @return serialized string value
     */
    public static String realToString(Package pkg, String type, Object value) {
        if (value == null)
            return "";
        com.centurylink.mdw.variable.VariableTranslator trans = getTranslator(pkg, type);
        if (trans == null)
            return "";
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
    public static Object realToObject(Package pkg, String type, String value) {
        if (StringUtils.isBlank(value))
            return null;
        com.centurylink.mdw.variable.VariableTranslator trans = getTranslator(pkg, type);
        if (trans == null)
            return null;
        if (trans instanceof DocumentReferenceTranslator)
            return ((DocumentReferenceTranslator)trans).realToObject(value);
        else
            return trans.toObject(value);
    }

    /**
     * If pkg is null then will use any available bundle to provide the translator.
     */
    public static boolean isDocumentReferenceVariable(Package pkg, String type) {
        com.centurylink.mdw.variable.VariableTranslator trans = getTranslator(pkg, type);
        return (trans instanceof DocumentReferenceTranslator);
    }

    /**
     * If pkg is null then will use any available bundle to provide the translator.
     */
    public static boolean isXmlDocumentTranslator(Package pkg, String type) {
        com.centurylink.mdw.variable.VariableTranslator trans = getTranslator(pkg, type);
        return (trans instanceof XmlDocumentTranslator);
    }
}