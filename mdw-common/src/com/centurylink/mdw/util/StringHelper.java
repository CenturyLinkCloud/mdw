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
package com.centurylink.mdw.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.Vector;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.model.JsonObject;

/**
 * Helper classes for String utilities.
 */
public class StringHelper {

    private static NumberFormat _f;
    private static SimpleDateFormat _df;
    private static SimpleDateFormat _serviceDateFormat;
    private static SimpleDateFormat _filenameDateFormat;

    static {
        _f = NumberFormat.getInstance();
        _f.setMaximumFractionDigits(2);
        _f.setMinimumFractionDigits(2);
        _df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        _serviceDateFormat = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss");
        _filenameDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss");
    }

    /**
     * Checks if a string is empty - null strings, "", and "  " are considered empty.
     *
     * @param aStr A String value.
     *
     * @return A boolean <code>true</code> if the String is empty,
     * otherwise <code>false</code>.
     */
    public static boolean isEmpty(String aStr) {
        return (aStr == null || aStr.trim().length() == 0);
    }

    /**
     * Appends a String to the StringBuffer if the String is not null.
     *
     * @param aBuff A StringBuffer value.
     * @param aStr A String value.
     */
    public static void appendNotNullString(StringBuffer aBuff, String aStr) {
        if (aBuff != null && aStr != null && aStr.trim().length() > 0) {
            aBuff.append(" ");
            aBuff.append(aStr);
        }
    }

    /**
     * Utility method that gets formatted string representing the <code>Object</code>.
     * <p>
     * For String object it returns <code>"string_value"</code>.
     * For other objects it returns <code>[obj.toString()]</code>.
     * If the object is null then it returns <code>null</code>.
     * <p>
     * Example: aAttrName="aObj",aAttrName="aObj", etc, where the "," is controlled
     * using the aPrependSeparator set to true.
     *
     * @param aAttrName A String attribute name
     * @param aObj An Object value.
     * @param aPrependSeparator A boolean value used in
     * PreFormatString(String, boolean) to append a ","
     * before the aAttrName value.
     *
     * @return A String - formatted string
     *
     * @see #PreFormatString(String, boolean)
     */
    public static String makeFormattedString(
        String aAttrName,
        Object aObj,
        boolean aPrependSeparator) {
        StringBuffer mStrBuff = new StringBuffer(PreFormatString(aAttrName, aPrependSeparator));
        if (aObj == null)
            mStrBuff.append("null");
        else if (aObj instanceof String) {
            mStrBuff.append("\"");
            mStrBuff.append(aObj.toString());
            mStrBuff.append("\"");
        } else {
            mStrBuff.append("[");
            mStrBuff.append(aObj.toString());
            mStrBuff.append("]");
        }
        return mStrBuff.toString();
    }

    /**
     * Utility method that gets formatted string representing a <code>int</code>.
     * <p>
     * Example: aAttrName="String.valueOf(aInt)",aAttrName="String.valueOf(aInt)",
     * etc, where the "," is controlled using the aPrependSeparator set to true.
     *
     * @param aAttrName A String attribute name value.
     * @param aInt An integer value.
     * @param aPrependSeparator A boolean value used in
     * PreFormatString(String, boolean) to append a ","
     * before the aAttrName value.
     *
     * @return A formatted String.
     *
     * @see #PreFormatString(String, boolean)
     */
    public static String makeFormattedString(
        String aAttrName,
        int aInt,
        boolean aPrependSeparator) {
        StringBuffer mStrBuff = new StringBuffer(PreFormatString(aAttrName, aPrependSeparator));
        mStrBuff.append(String.valueOf(aInt));
        return mStrBuff.toString();
    }

    /**
     * Utility method that gets formatted string representing a <code>char</code>.
     * The returned string has sinqle quotes(') arround the char.
     * <p>
     * Example: aAttrName="'String.valueOf((char)aChar)'",aAttrName="'String.valueOf((char)aChar)'",
     * etc, where the "," is controlled using the aPrependSeparator set to true.
     *
     * @param aAttrName String attribute name
     * @param aChar A char value
     * @param aPrependSeparator A boolean value used in
     * PreFormatString(String, boolean) to append a ","
     * before the aAttrName value.
     *
     * @return String - formatted string
     *
     * @see #PreFormatString(String, boolean)
     */
    public static String makeFormattedString(
        String aAttrName,
        char aChar,
        boolean aPrependSeparator) {
        StringBuffer mStrBuff = new StringBuffer(PreFormatString(aAttrName, aPrependSeparator));
        mStrBuff.append("'" + String.valueOf((char)aChar) + "'");
        return mStrBuff.toString();
    }

    /**
     * Utility method that gets formatted string representing a <code>long</code>.
     * <p>
     * Example: aAttrName="String.valueOf(aLong)",aAttrName="String.valueOf(aLong)",
     * etc, where the "," is controlled using the aPrependSeparator set to true.
     *
     * @param aAttrName A String attribute name value.
     * @param aLong A long value.
     * @param aPrependSeparator A boolean value used in
     * PreFormatString(String, boolean) to append a ","
     * before the aAttrName value.
     *
     * @return String - formatted string
     *
     * @see #PreFormatString(String, boolean)
     */
    public static String makeFormattedString(
        String aAttrName,
        long aLong,
        boolean aPrependSeparator) {
            StringBuffer mStrBuff = new StringBuffer(PreFormatString(aAttrName, aPrependSeparator));
            mStrBuff.append(String.valueOf(aLong));
            return mStrBuff.toString();
        }
    /**Utility method that gets formatted string representing a <code>float</code>.
     * <p>
     * Example: aAttrName="String.valueOf(aFloat)",aAttrName="String.valueOf(aFloat)",
     * etc, where the "," is controlled using the aPrependSeparator set to true.
     *
     * @param aAttrName A String attribute name value.
     * @param aFloat A float value.
     * @param aPrependSeparator A boolean value used in
     * PreFormatString(String, boolean) to append a ","
     * before the aAttrName value.
     *
     * @return A formatted String.
     *
     * @see #PreFormatString(String, boolean)
     */
    public static String makeFormattedString(
        String aAttrName,
        float aFloat,
        boolean aPrependSeparator) {
            StringBuffer mStrBuff = new StringBuffer(PreFormatString(aAttrName, aPrependSeparator));
            mStrBuff.append(String.valueOf(aFloat));
        return mStrBuff.toString();
    }

    /**
     * Utility method that gets formatted string representing a <code>double</code>.
     * <p>
     * Example: aAttrName="String.valueOf(aDouble)",aAttrName="String.valueOf(aDouble)",
     * etc, where the "," is controlled using the aPrependSeparator set to true.
     *
     * @param aAttrName String attribute name value.
     * @param aDouble A double value.
     * @param aPrependSeparator A boolean value used in
     * PreFormatString(String, boolean) to append a ","
     * before the aAttrName value.
     *
     * @return A formatted String.
     *
     * @see #PreFormatString(String, boolean)
     */
    public static String makeFormattedString(
        String aAttrName,
        double aDouble,
        boolean aPrependSeparator) {
        StringBuffer mStrBuff = new StringBuffer(PreFormatString(aAttrName, aPrependSeparator));
        mStrBuff.append(String.valueOf(aDouble));
        return mStrBuff.toString();
    }

    /**
     * Utility method that gets formatted string representing a <code>boolean</code>.
     * <p>
     * Example: aAttrName="String.valueOf(aBool)",aAttrName="String.valueOf(aBool)",
     * etc, where the "," is controlled using the aPrependSeparator set to true.
     *
     * @param aAttrName A String attribute name value.
     * @param aBool A boolean value.
     * @param aPrependSeparator A boolean value used in
     * PreFormatString(String, boolean) to append a ","
     * before the aAttrName value.
     *
     * @return A formatted String.
     *
     * @see #PreFormatString(String, boolean)
     */
    public static String makeFormattedString(
        String aAttrName,
        boolean aBool,
        boolean aPrependSeparator) {
        StringBuffer mStrBuff = new StringBuffer(PreFormatString(aAttrName, aPrependSeparator));
        mStrBuff.append(String.valueOf(aBool));
        return mStrBuff.toString();
    }

    /**
     * Utility method that formats array of <code>Object</code>.
     * <p>
     * Example: aAttrName[]="[LENGTH=anObjArray.length(anObjArray{i}, ... ]",
     * where the "," is controlled using the aPrependSeparator set to true.
     *
     * @param aAttrName A String attribute name value.
     * @param anObjArray An Object array of values
     * @param aPrependSeparator A boolean value used in
     * PreFormatString(String, boolean) to append a ","
     * before the aAttrName value.
     *
     * @return A formatted String.
     *
     * @see #PreFormatString(String, boolean)
     * @see #makeFormattedString(String, boolean)
     */
    public static String makeFormattedString(
        String aAttrName,
        Object[] anObjArray,
        boolean aPrependSeparator) {
        StringBuffer mStrBuff =
            new StringBuffer(PreFormatString(aAttrName + "[]", aPrependSeparator));
        if (anObjArray == null)
            mStrBuff.append("null");
        else {
            mStrBuff.append("[");
            mStrBuff.append("LENGTH=" + anObjArray.length);
            if (anObjArray.length > 0) {
                for (int i = 0; i < anObjArray.length; i++) {
                    mStrBuff.append(makeFormattedString("(" + i + ")", anObjArray[i]));
                }
            }
            mStrBuff.append("]");
        }
        return mStrBuff.toString();
    }

    /**
     * Utility method that formats array of <code>boolean</code>.
     * <p>
     * Example: aAttrName[]="[LENGTH=anArray.length(anArray{i}, ... ]",
     * where the "," is controlled using the aPrependSeparator set to true.
     *
     * @param aAttrName String attribute name
     * @param anArray A boolean array value.
     * @param aPrependSeparator A boolean value used in
     * PreFormatString(String, boolean) to append a ","
     * before the aAttrName value.
     *
     * @return A formatted String.
     *
     * @see #PreFormatString(String, boolean)
     * @see #makeFormattedString(String, boolean[])
     */
    public static String makeFormattedString(
        String aAttrName,
        boolean[] anArray,
        boolean aPrependSeparator) {
        StringBuffer mStrBuff =
            new StringBuffer(PreFormatString(aAttrName + "[]", aPrependSeparator));
    if (anArray == null)
            mStrBuff.append("null");
        else {
            mStrBuff.append("[");
            mStrBuff.append("LENGTH=" + anArray.length);
            if (anArray.length > 0) {
                for (int i = 0; i < anArray.length; i++) {
                    mStrBuff.append(makeFormattedString("(" + i + ")", anArray[i]));
                }
            }
            mStrBuff.append("]");
        }
        return mStrBuff.toString();
    }

    /**
     * Utility method that formats array of <code>int</code>.
     * <p>
     * Example: aAttrName[]="[LENGTH=anArray.length(anArray{i}, ... ]",
     * where the "," is controlled using the aPrependSeparator set to true.
     *
     * @param aAttrName A String attribute name value.
     * @param anArray An integer array value.
     * @param aPrependSeparator A boolean value used in
     * PreFormatString(String, boolean) to append a ","
     * before the aAttrName value.
     *
     * @return A formatted String.
     *
     * @see #PreFormatString(String, boolean)
     * @see #makeFormattedString(String, int[])
     */
    public static String makeFormattedString(
        String aAttrName,
        int[] anArray,
        boolean aPrependSeparator) {
        StringBuffer mStrBuff =
            new StringBuffer(PreFormatString(aAttrName + "[]", aPrependSeparator));
        if (anArray == null)
            mStrBuff.append("null");
        else {
            mStrBuff.append("[");
            mStrBuff.append("LENGTH=" + anArray.length);
            if (anArray.length > 0) {
                for (int i = 0; i < anArray.length; i++) {
                    mStrBuff.append(makeFormattedString("(" + i + ")", anArray[i]));
                }
            }
            mStrBuff.append("]");
        }
        return mStrBuff.toString();
    }

    /**
     * Utility method that formats array of <code>char</code>.
     * <p>
     * Example: aAttrName[]="[LENGTH=anArray.length(anArray{i}, ... ]",
     * where the "," is controlled using the aPrependSeparator set to true.
     *
     * @param aAttrName A String attribute name value.
     * @param anArray A char array value.
     * @param aPrependSeparator A boolean value used in
     * PreFormatString(String, boolean) to append a ","
     * before the aAttrName value.
     *
     * @return A formatted String.
     *
     * @see #PreFormatString(String, boolean)
     * @see #makeFormattedString(String, char[])
     */
    public static String makeFormattedString(
        String aAttrName,
        char[] anArray,
        boolean aPrependSeparator) {
        StringBuffer mStrBuff =
            new StringBuffer(PreFormatString(aAttrName + "[]", aPrependSeparator));
        if (anArray == null)
            mStrBuff.append("null");
        else {
            mStrBuff.append("[");
            mStrBuff.append("LENGTH=" + anArray.length);
            if (anArray.length > 0) {
                for (int i = 0; i < anArray.length; i++) {
                    mStrBuff.append(makeFormattedString("(" + i + ")", anArray[i]));
                }
            }
            mStrBuff.append("]");
        }
        return mStrBuff.toString();
    }

    /**
     * Utility method that formats array of <code>long</code>.
     * <p>
     * Example: aAttrName[]="[LENGTH=anArray.length(anArray{i}, ... ]",
     * where the "," is controlled using the aPrependSeparator set to true.
     *
     * @param aAttrName A String attribute name value.
     * @param anArray A long array value.
     * @param aPrependSeparator A boolean value used in
     * PreFormatString(String, boolean) to append a ","
     * before the aAttrName value.
     *
     * @return A formatted String.
     *
     * @see #PreFormatString(String, boolean)
     * @see #makeFormattedString(String, long[])
     */
    public static String makeFormattedString(
        String aAttrName,
        long[] anArray,
        boolean aPrependSeparator) {
        StringBuffer mStrBuff =
            new StringBuffer(PreFormatString(aAttrName + "[]", aPrependSeparator));
        if (anArray == null)
            mStrBuff.append("null");
        else {
            mStrBuff.append("[");
            mStrBuff.append("LENGTH=" + anArray.length);
            if (anArray.length > 0) {
                for (int i = 0; i < anArray.length; i++) {
                    mStrBuff.append(makeFormattedString("(" + i + ")", anArray[i]));
                }
            }
            mStrBuff.append("]");
        }
        return mStrBuff.toString();
    }

    /**
     * Utility method that formats array of <code>float</code>.
     * <p>
     * Example: aAttrName[]="[LENGTH=anArray.length(anArray{i}, ... ]",
     * where the "," is controlled using the aPrependSeparator set to true.
     *
     * @param aAttrName A String attribute name value.
     * @param anArray A float array value.
     * @param aPrependSeparator A boolean value used in
     * PreFormatString(String, boolean) to append a ","
     * before the aAttrName value.
     *
     * @return A formatted String.
     *
     * @see #PreFormatString(String, boolean)
     * @see #makeFormattedString(String, float[])
     */
    public static String makeFormattedString(
        String aAttrName,
        float[] anArray,
        boolean aPrependSeparator) {
        StringBuffer mStrBuff =
            new StringBuffer(PreFormatString(aAttrName + "[]", aPrependSeparator));
        if (anArray == null)
            mStrBuff.append("null");
        else {
            mStrBuff.append("[");
            mStrBuff.append("LENGTH=" + anArray.length);
            if (anArray.length > 0) {
                for (int i = 0; i < anArray.length; i++) {
                    mStrBuff.append(makeFormattedString("(" + i + ")", anArray[i]));
                }
            }
            mStrBuff.append("]");
        }
        return mStrBuff.toString();
    }

    /**
     * Utility method that formats array of <code>byte</code>.
     * <p>
     * Example: aAttrName[]="[LENGTH=anArray.length(anArray{i}, ... ]",
     * where the "," is controlled using the aPrependSeparator set to true.
     *
     * @param aAttrName A String attribute name value.
     * @param anArray A byte array value.
     * @param aPrependSeparator A boolean value used in
     * PreFormatString(String, boolean) to append a ","
     * before the aAttrName value.
     *
     * @return A formatted String.
     *
     * @see #PreFormatString(String, boolean)
     * @see #makeFormattedString(String, byte[])
     */
    public static String makeFormattedString(
        String aAttrName,
        byte[] anArray,
        boolean aPrependSeparator) {
        StringBuffer mStrBuff =
            new StringBuffer(PreFormatString(aAttrName + "[]", aPrependSeparator));
        if (anArray == null)
            mStrBuff.append("null");
        else {
            mStrBuff.append("[");
    mStrBuff.append("LENGTH=" + anArray.length);
            if (anArray.length > 0) {
                for (int i = 0; i < anArray.length; i++) {
                    mStrBuff.append(makeFormattedString("(" + i + ")", anArray[i]));
                }
            }
            mStrBuff.append("]");
        }
        return mStrBuff.toString();
    }

    /**
     * Utility method that formats array of <code>short</code>.
     * <p>
     * Example: aAttrName[]="[LENGTH=anArray.length(anArray{i}, ... ]",
     * where the "," is controlled using the aPrependSeparator set to true.
     *
     * @param aAttrName A String attribute name value.
     * @param anArray A short array value.
     * @param aPrependSeparator A boolean value used in
     * PreFormatString(String, boolean) to append a ","
     * before the aAttrName value.
     *
     * @return A formatted String.
     *
     * @see #PreFormatString(String, boolean)
     * @see #makeFormattedString(String, short[])
     */
    public static String makeFormattedString(
        String aAttrName,
        short[] anArray,
        boolean aPrependSeparator) {
        StringBuffer mStrBuff =
            new StringBuffer(PreFormatString(aAttrName + "[]", aPrependSeparator));
        if (anArray == null)
            mStrBuff.append("null");
        else {
            mStrBuff.append("[");
            mStrBuff.append("LENGTH=" + anArray.length);
            if (anArray.length > 0) {
                for (int i = 0; i < anArray.length; i++) {
                    mStrBuff.append(makeFormattedString("(" + i + ")", anArray[i]));
                }
            }
            mStrBuff.append("]");
        }
        return mStrBuff.toString();
    }

    /**
     * Utility method that formats array of <code>double</code>.
     * <p>
     * Example: aAttrName[]="[LENGTH=anArray.length(anArray{i}, ... ]",
     * where the "," is controlled using the aPrependSeparator set to true.
     *
     * @param aAttrName A String attribute name value.
     * @param anArray A double array value.
     * @param aPrependSeparator A boolean value used in
     * PreFormatString(String, boolean) to append a ","
     * before the aAttrName value.
     *
     * @return A formatted String.
     *
     * @see #PreFormatString(String, boolean)
     * @see #makeFormattedString(String, double[])
     */
    public static String makeFormattedString(
        String aAttrName,
        double[] anArray,
        boolean aPrependSeparator) {
        StringBuffer mStrBuff =
            new StringBuffer(PreFormatString(aAttrName + "[]", aPrependSeparator));
        if (anArray == null)
            mStrBuff.append("null");
        else {
            mStrBuff.append("[");
            mStrBuff.append("LENGTH=" + anArray.length);
            if (anArray.length > 0) {
                for (int i = 0; i < anArray.length; i++) {
                    mStrBuff.append(makeFormattedString("(" + i + ")", anArray[i]));
                }
            }
            mStrBuff.append("]");
        }
        return mStrBuff.toString();
    }

    /**
     * Overloaded method with <code>aPrependSeparator = true</code>.
     *
     * @see #makeFormattedString(String, Object[], boolean)
     *
     * @param aAttrName A String attribute name value.
     * @param anObjArray An Object array value.
     *
     * @return A formatted String.
     */
    public static String makeFormattedString(String aAttrName, Object[] anObjArray) {
        return makeFormattedString(aAttrName, anObjArray, true);
    }

    /**
     * Overloaded method with <code>aPrependSeparator = true</code>.
     *
     * @see #makeFormattedString(String, boolean[], boolean)
     *
     * @param aAttrName A String attribute name value.
     * @param anArray A boolean array value.
     *
     * @return A formatted String.
     */
    public static String makeFormattedString(String aAttrName, boolean[] anArray) {
        return makeFormattedString(aAttrName, anArray, true);
    }

    /**
     * Overloaded method with <code>aPrependSeparator = true</code>.
     *
     * @see #makeFormattedString(String, char[], boolean)
     *
     * @param aAttrName A String attribute name value.
     * @param anArray A char array value.
     *
     * @return A formatted String.
     */
    public static String makeFormattedString(String aAttrName, char[] anArray) {
        return makeFormattedString(aAttrName, anArray, true);
    }

    /**
     * Overloaded method with <code>aPrependSeparator = true</code>.
     *
     * @see #makeFormattedString(String, int[], boolean)
     *
     * @param aAttrName A String attribute name value.
     * @param anArray An integer array value.
     *
     * @return A formatted String.
     */
    public static String makeFormattedString(String aAttrName, int[] anArray) {
        return makeFormattedString(aAttrName, anArray, true);
    }

    /**
     * Overloaded method with <code>aPrependSeparator = true</code>.
     *
     * @see #makeFormattedString(String, long[], boolean)
     *
     * @param aAttrName A String attribute name value.
     * @param anArray A long array value.
     *
     * @return A formatted String.
     */
    public static String makeFormattedString(String aAttrName, long[] anArray) {
        return makeFormattedString(aAttrName, anArray, true);
    }

    /**
     * Overloaded method with <code>aPrependSeparator = true</code>.
     *
     * @see #makeFormattedString(String, float[], boolean)
     *
     * @param aAttrName A String attribute name value.
     * @param anArray A float array value.
     *
     * @return A formatted String.
     */
    public static String makeFormattedString(String aAttrName, float[] anArray) {
        return makeFormattedString(aAttrName, anArray, true);
    }

    /**
     * Overloaded method with <code>aPrependSeparator = true</code>.
     *
     * @see #makeFormattedString(String, double[], boolean)
     *
     * @param aAttrName A String attribute name value.
     * @param anArray A double array value.
     *
     * @return A formatted String.
     */
    public static String makeFormattedString(String aAttrName, double[] anArray) {
        return makeFormattedString(aAttrName, anArray, true);
    }

    /**
     * Overloaded method with <code>aPrependSeparator = true</code>.
     *
     * @see #makeFormattedString(String, byte[], boolean)
     *
     * @param aAttrName A String attribute name value.
     * @param anArray A byte array value.
     *
     * @return A formatted String.
     */
    public static String makeFormattedString(String aAttrName, byte[] anArray) {
        return makeFormattedString(aAttrName, anArray, true);
    }

    /**
     * Overloaded method with <code>aPrependSeparator = true</code>.
     *
     * @see #makeFormattedString(String, short[], boolean)
     *
     * @param aAttrName A String attribute name value.
     * @param anArray A short array value.
     *
     * @return A formatted String.
     */
    public static String makeFormattedString(String aAttrName, short[] anArray) {
        return makeFormattedString(aAttrName, anArray, true);
    }

    /**
     * Overloaded method with <code>aPrependSeparator = true</code>.
     *
     * @see #makeFormattedString(String, Object, boolean)
     *
     * @param aAttrName A String attribute name value.
     * @param aObj An Object value.
     *
     * @return A formatted String.
     */
    public static String makeFormattedString(String aAttrName, Object aObj) {
        return makeFormattedString(aAttrName, aObj, true);
    }

    /**
     * Overloaded method with <code>aPrependSeparator = true</code>.
     *
     * @see #makeFormattedString(String, int, boolean)
     *
     * @param aAttrName A String attribute name value.
     * @param aInt An integer value.
     *
     * @return A formatted String.
     */
    public static String makeFormattedString(String aAttrName, int aInt) {
        return makeFormattedString(aAttrName, aInt, true);
    }

    /**
     * Overloaded method with <code>aPrependSeparator = true</code>.
     *
     * @see #makeFormattedString(String, long, boolean)
     *
     * @param aAttrName A String attribute name value.
     * @param aLong An long value.
     *
     * @return A formatted String.
     */
    public static String makeFormattedString(String aAttrName, long aLong) {
        return makeFormattedString(aAttrName, aLong, true);
    }

    /**
     * Overloaded method with <code>aPrependSeparator = true</code>.
     *
     * @see #makeFormattedString(String, float, boolean)
     *
     * @param aAttrName A String attribute name value.
     * @param aFloat An float value.
     *
     * @return A formatted String.
     */
    public static String makeFormattedString(String aAttrName, float aFloat) {
        return makeFormattedString(aAttrName, aFloat, true);
    }

    /**
     * Overloaded method with <code>aPrependSeparator = true</code>.
     *
     * @see #makeFormattedString(String, double, boolean)
     *
     * @param aAttrName A String attribute name value.
     * @param aDouble An double value.
     *
     * @return A formatted String.
     */
    public static String makeFormattedString(String aAttrName, double aDouble) {
        return makeFormattedString(aAttrName, aDouble, true);
    }

    /**
     * Overloaded method with <code>aPrependSeparator = true</code>.
     *
     * @see #makeFormattedString(String, boolean, boolean)
     *
     * @param aAttrName A String attribute name value.
     * @param aBool An boolean value.
     *
     * @return A formatted String.
     */
    public static String makeFormattedString(String aAttrName, boolean aBool) {
        return makeFormattedString(aAttrName, aBool, true);
    }

    /**
     * Overloaded method with <code>aPrependSeparator = true</code>.
     *
     * @see #makeFormattedString(String, char, boolean)
     *
     * @param aAttrName A String attribute name value.
     * @param aChar An char value.
     *
     * @return A formatted String.
     */
    public static String makeFormattedString(String aAttrName, char aChar) {
        return makeFormattedString(aAttrName, (char)aChar, true);
    }

    /**
     * Replaces the 'search string' with 'replace string' in a given 'source string'
     *
     * @param aSrcStr A source String value.
     * @param aSearchStr A String value to be searched for in the source String.
     * @param aReplaceStr A String value that replaces the search String.
     *
     * @return A String with the 'search string' replaced by 'replace string'.
     */
    public static String Replace(String aSrcStr, String aSearchStr, String aReplaceStr) {
        if (aSrcStr == null || aSearchStr == null || aReplaceStr == null)
            return aSrcStr;

        if (aSearchStr.length() == 0
            || aSrcStr.length() == 0
            || aSrcStr.indexOf(aSearchStr) == -1) {
            return aSrcStr;
        }

        StringBuffer mBuff = new StringBuffer();
        int mSrcStrLen, mSearchStrLen;

        mSrcStrLen = aSrcStr.length();
        mSearchStrLen = aSearchStr.length();
        for (int i = 0, j = 0; i < mSrcStrLen; i = j + mSearchStrLen) {
            j = aSrcStr.indexOf(aSearchStr, i);
            if (j == -1) {
                mBuff.append(aSrcStr.substring(i));
                break;
            }
            mBuff.append(aSrcStr.substring(i, j));
            mBuff.append(aReplaceStr);
        }
        return mBuff.toString();
    }

    /**
     * Creates the pre-format string for <code>toString()</code>.
     * Returns a string <code>aAttrName=</code>. If aPrependSeparator is true
     * then prepends a comma(,) separator before the formatted string.
     * <p>
     * Example: aAttrName= or ,aAttrName= where the "," is controlled
     * using the aPrependSeparator set to true.
     *
     * @param aAttrName A String attribute name value.
     * @param aPrependSeparator A boolean value where <code>true</code> will append
     * a "," to the String.
     *
     * @return A pre-format String.
     */
    private static String PreFormatString(String aAttrName, boolean aPrependSeparator) {
        StringBuffer mStrBuff = new StringBuffer();
        if (aPrependSeparator)
            mStrBuff.append(",");
        mStrBuff.append(aAttrName);
        mStrBuff.append("=");
        return mStrBuff.toString();
    }

    /**
     * Checks if both the strings are equal for value - Not Case sensitive.
     *
     * @param pStr1 A String value.
     * @param pStr2 A String value.
     *
     * @return boolean A boolean <code>true</code> if the Strings are equal,
     * otherwise <code>false</code>.
     *
     * @see String#equalsIgnoreCase(java.lang.String)
     */
    public static boolean isEqualIgnoreCase(String pStr1, String pStr2) {
        if (pStr1 == null && pStr2 == null) {
            return true;
        } else if (pStr1 == null || pStr2 == null) {
            return false;
        } else if (pStr1.equalsIgnoreCase(pStr2)) {
            return true;
        }
        return false;
    }
    /**
     * Checks if both the strings are equal for value - Case Sensitive.
     *
     * @param pStr1 A String value.
     * @param pStr2 A String value.
     *
     * @return boolean A boolean <code>true</code> if the Strings are equal,
     * otherwise <code>false</code>.
     *
     * @see String#equals(java.lang.Object)
     */
    public static boolean isEqual(String pStr1, String pStr2) {
        if (pStr1 == null && pStr2 == null) {
            return true;
        } else if (pStr1 == null || pStr2 == null) {
            return false;
        } else if (pStr1.equals(pStr2)) {
            return true;
        }
        return false;
    }

    /**
     * Removes all the spcial charactes and spaces from the passed in string - none
     * letters or digits.
     *
     * @param pStr A String value.
     *
     * @return A cleaned String.
     *
     * @see Character#isLetterOrDigit(char)
     */
    public static String cleanString(String pStr) {
        if (pStr == null || pStr.equals("")) {
            return pStr;
        }
        StringBuffer buff = new StringBuffer();
        for (int i = 0; i < pStr.length(); i++) {
            char aChar = pStr.charAt(i);
            if (Character.isLetterOrDigit(aChar)) {
                buff.append(aChar);
            }
        }
        return buff.toString();
    }

    /**
     * This method strips out all new line characters from the passed String.
     *
     * @param pString A String value.
     *
     * @return A clean String.
     *
     * @see StringTokenizer
     */
    public static String stripNewLineChar(String pString) {
        String tmpFidValue = pString;
        StringTokenizer aTokenizer = new StringTokenizer(pString, "\n");
        if (aTokenizer.countTokens() > 1) {
            StringBuffer nameBuffer = new StringBuffer();
            while (aTokenizer.hasMoreTokens()) {
                nameBuffer.append(aTokenizer.nextToken());
            }
            tmpFidValue = nameBuffer.toString();
        }
        return tmpFidValue;
    }

    /**
     * Returns the parsed double value out of the passed in String
     *
     * @param pStr A String value.
     *
     * @return A double value.
     *
     * @see Double#parseDouble(java.lang.String)
    */
    public static double getDouble(String pStr) {
        if (isEmpty(pStr)) {
            return 0.0;
        }
        double value = 0.0;
        pStr = pStr.substring(0, pStr.length() - 2) + "." + pStr.substring(pStr.length() - 2);
        try {
            value = Double.parseDouble(pStr);
        } catch (NumberFormatException ex) {
        }
        return value;
    }

    /**
     * Returns the parsed long value for the passed in String.
     *
     * @param pStr A String value.
     *
     * @return A long value.
     *
     * @see Long#parseLong(java.lang.String)
     */
    public static long getLong(String pStr) {
        if (isEmpty(pStr)) {
            return 0;
        }
        long value = 0;
        try {
            value = Long.parseLong(pStr);
        } catch (NumberFormatException nm) {
        }
        return value;
    }

    /**
     * Returns the parsed integer value for the passed in String.
     *
     * @param pStr A String value.
     *
     * @return An integer value.
     *
     * @see Integer#parseInt(java.lang.String)
     */
    public static int getInteger(String pStr, int defval) {
        if (isEmpty(pStr)) return defval;
        try {
            return Integer.parseInt(pStr);
        } catch (NumberFormatException nm) {
            return defval;
        }
    }

    /**
     * Returns the boolean value the passed in String.
     *
     * @param pStr A String value.
     *
     * @return A boolean value.
     *
     * @see Boolean#getBoolean(java.lang.String)
     */
    public static boolean getBoolean(String pStr) {
        if (isEmpty(pStr)) {
            return false;
        }
        return Boolean.getBoolean(pStr);
    }

    /**
    * Method which parses the input String to send back the value corresponding to the
    * input key. For eg:
    * If the inputStr is "TradingPartnerKey=5; OWNER=L3; CBTN=3036240004; RTI=105"
    * startingChar is '=', endingChar = ';' & key = "TradingPartnerKey"
    * Then the response String will be "5"
    * @param inputStr The input String to be parsed
    * @param startingChar The first character to be found after the input key
    * @param endingChar The first character to be found after the input key-value pair
    * @param key For which you need to extract the value
    * @return
    */
    public static String parseString(String inputStr, char startingChar, char endingChar, String key) {
        if (null == inputStr || null == key) {
            return null;
        }
        int matchedStartingIndex = inputStr.indexOf(key + startingChar);
        if (-1 == matchedStartingIndex) {
            return null;
        }
        int matchedEndingIndex = inputStr.indexOf(endingChar, matchedStartingIndex);
        if (-1 == matchedEndingIndex) {
                return inputStr.substring(matchedStartingIndex + key.length() + 1);
            } else {
                return inputStr.substring(matchedStartingIndex + key.length() + 1, matchedEndingIndex);
            }
    }


    /**
     * Method which checks whether a string which is having many values
     * separated by a delimiter contains a particular value or not
     * If the compositeStr is "QC, ATT, L3" & strToBeSearched is "QC"
     * & regex is "," then the method will return true.
     * @param strToBeSearched the value which needs to be searched for in the
     * composite String
     * @param compositeStr the String which contains all the values
     * separated by a delimiter
     * @param regex the delimiting regular expression
     * @return
     */
    public static boolean isContainedIn(String strToBeSearched, String compositeStr,
         String regex) {

        if ((null == strToBeSearched) || (null == compositeStr) || (null == regex))
            return false;
        String[] splitValues = compositeStr.split(regex);
        boolean isFound = false;
        for(int i=0; i<splitValues.length; i++) {
            if(strToBeSearched.equals(splitValues[i].trim())) {
                isFound = true;
                break;
            }
        }
        return isFound;
    }

    public static String reverseString(String pStr){
          if(pStr == null || pStr.length() < 1){
            return pStr;
          }
          char[] chars = pStr.toCharArray();
          StringBuffer buff = new StringBuffer();
          for(int i= (chars.length -1); i >= 0; i--){
             buff.append(chars[i]);
          }
          return buff.toString();
    }

    /**
     * Escape meta characters with backslash.
     *
     * @param value original string
     * @param metachars characters that need to be escaped
     * @return modified string with meta characters escaped with backslashes
     */
    public static String escapeWithBackslash(String value, String metachars) {
        StringBuffer sb = new StringBuffer();
        int i, n = value.length();
        char ch;
        for (i=0; i<n; i++) {
            ch = value.charAt(i);
            if (ch=='\\' || metachars.indexOf(ch)>=0) {
                sb.append('\\');
            }
            sb.append(ch);
        }
        return sb.toString();
    }

    /**
     * Remove backslashes that are used as escape characters
     * @param value
     * @return
     */
    public static String removeBackslashEscape(String value) {
        StringBuffer sb = new StringBuffer();
        int i, n = value.length();
        char ch;
        boolean lastIsEscape = false;
        for (i=0; i<n; i++) {
            ch = value.charAt(i);
            if (lastIsEscape) {
                sb.append(ch);
                lastIsEscape = false;
            } else {
                if (ch=='\\') lastIsEscape = true;
                else sb.append(ch);
            }
        }
        return sb.toString();
    }

    public static String[] splitWithoutEscaped(String value, char delimiter) {
        Vector<String> res = new Vector<String>();
        if (value==null) return new String[0];
        int i, n=value.length(), start=0;
        boolean escaped = false;
        char ch;
        for (i=0; i<n; i++) {
            if (escaped) {
                escaped = false;
            } else {
                ch = value.charAt(i);
                if (ch=='\\') escaped = true;
                else if (ch==delimiter) {
                    res.add(value.substring(start, i));
                    start = i+1;
                }
            }
        }
        if (start<n) res.add(value.substring(start));
        String[] ret = new String[res.size()];
        for (i=0; i<ret.length; i++) ret[i] = res.elementAt(i);
        return ret;
    }

    public static String getMapValue(String map, String name, char delimiter) {
        if (map.startsWith("{")) {
            try {
                JSONObject json = new JsonObject(map);
                if (json.has(name))
                    return json.getString(name);
                else
                    return null;
            }
            catch (JSONException ex) {
                throw new StringParseException(ex);
            }
        }
        else {
            int name_start=0;
            int n = map.length();
            int m = name.length();
            while (name_start>=0) {
                name_start = map.indexOf(name, name_start);
                if (name_start>=0) {
                    if ((name_start==0||map.charAt(name_start-1)==delimiter)
                        && (name_start+m==n || map.charAt(name_start+m)=='=')) {
                        int value_start = name_start+m+1;
                        int k;
                        char ch;
                        boolean escaped = false;
                        for (k=value_start; k<n; k++) {
                            if (escaped) escaped = false;
                            else {
                                ch = map.charAt(k);
                                if (ch=='\\') escaped = true;
                                else if (ch==delimiter) break;
                            }
                        }
                        return map.substring(value_start, k);
                    } else name_start += m;
                }
            }
            return null;
        }
    }

    public static String formatMap(Map<String,String> map) {
        StringBuffer sb = new StringBuffer();
        boolean first = true;
        for (String key : map.keySet()) {
            if (first) first = false;
            else sb.append(';');
            sb.append(key);
            sb.append('=');
            String value = map.get(key);
            if (value==null) value = "";
            sb.append(escapeWithBackslash(value, ";"));
        }
        return sb.toString();
    }

    public static List<String> parseList(String value) {
        List<String> list = new ArrayList<String>();
        if (value.startsWith("[")) {
            try {
                JSONArray jsonArr = new JSONArray(value);
                for (int i = 0; i < jsonArr.length(); i++)
                    list.add(jsonArr.getString(i));
            }
            catch (JSONException ex) {
                throw new StringParseException(ex);
            }
        }
        else {
            StringTokenizer st = new StringTokenizer(value, "#");
            while (st.hasMoreTokens())
                list.add(st.nextToken());
        }
        return list;
    }

    public static String serialize(List<String> list, boolean json) {
        String value = "";
        if (json) {
            JSONArray jsonArr = new JSONArray();
            for (String str : list)
                jsonArr.put(str);
            return jsonArr.toString(); // no pretty
        }
        else {
            for (int i = 0; i < list.size(); i++) {
                if (i > 0)
                    value += "#";
                value += list.get(i);
            }
        }
        return value;
    }

    public static Map<String,String> parseMap(String map) {
        HashMap<String,String> hash = new LinkedHashMap<String,String>();
        if (map != null) {
            if (map.startsWith("{")) {
                try {
                    return JsonUtil.getMap(new JsonObject(map));
                }
                catch (JSONException ex) {
                    throw new StringParseException(ex);
                }
            }
            else {
                int name_start = 0;
                int n = map.length();
                int m;
                while (name_start<n) {
                    m = name_start;
                    char ch = map.charAt(m);
                    while (ch!='=' && ch!=';' && m<n-1) {
                        m++;
                        ch = map.charAt(m);
                    }
                    if (ch=='=') {
                        int value_start = m+1;
                        boolean escaped = false;
                        for (m=value_start; m<n; m++) {
                            if (escaped) escaped = false;
                            else {
                                ch = map.charAt(m);
                                if (ch=='\\') escaped = true;
                                else if (ch==';') break;
                            }
                        }
                        hash.put(map.substring(name_start,value_start-1).trim(),
                                map.substring(value_start, m).trim());
                        name_start = m+1;
                    } else if (ch==';') {
                        if (m>name_start) {
                            hash.put(map.substring(name_start, m).trim(), null);
                        }
                        name_start = m+1;
                    } else {    // m == n-1
                        if (m>name_start) {
                            hash.put(map.substring(name_start, m).trim(), null);
                        }
                        name_start = m+1;
                    }
                }
            }
        }
        return hash;
    }

    public static List<String[]> parseTable(String string,
            char field_delimiter, char row_delimiter, int columnCount) {
        List<String[]> table = new ArrayList<String[]>();
        if (string != null) {
            if (string.startsWith("[")) {
                List<String[]> rows = new ArrayList<String[]>();
                try {
                    JSONArray outer = new JSONArray(string);
                    for (int i = 0; i < outer.length(); i++) {
                        String[] row = new String[columnCount];
                        JSONArray inner = outer.getJSONArray(i);
                        for (int j = 0; j < row.length; j++) {
                            if (inner.length() > j)
                                row[j] = inner.getString(j);
                            else
                                row[j] = "";
                        }
                        rows.add(row);
                    }
                    return rows;
                }
                catch (JSONException ex) {
                    throw new StringParseException(ex.getMessage(), ex);
                }
            }
            else {
                int row_start = 0;
                int field_start;
                int n = string.length();
                String[] row;
                int m, j;
                StringBuffer sb;
                while (row_start<n) {
                    row = new String[columnCount];
                    table.add(row);
                    j = 0;
                    field_start = row_start;
                    char ch=field_delimiter;
                    while (ch==field_delimiter) {
                        sb = new StringBuffer();
                        boolean escaped = false;
                        for (m=field_start; m<n; m++) {
                            ch = string.charAt(m);
                            if (ch=='\\' && !escaped) {
                                escaped = true;
                            }
                            else {
                                if (!escaped && (ch==field_delimiter || ch==row_delimiter)) {
                                    break;
                                }
                                else {
                                    sb.append(ch);
                                    escaped = false;
                                }
                            }
                        }
                        if (j<columnCount) row[j] = sb.toString();
                        if (m>=n || ch==row_delimiter) {
                            row_start = m+1;
                            break;
                        } else {  // ch==field_delimiter
                            field_start = m+1;
                            j++;
                        }
                    }
                }
            }
        }
        return table;
    }

    public static String serializeTable(List<String[]> rows) {
        StringBuffer serialized = new StringBuffer();
        for (int i = 0; i < rows.size(); i++) {
            if (i > 0)
                serialized.append(';');
            String[] row = rows.get(i);
            for (int j = 0; j < row.length; j++) {
                if (j > 0)
                    serialized.append(',');
                if (row[j] != null)
                    serialized.append(StringHelper.escapeWithBackslash(row[j], ",;"));
            }
        }
        return serialized.toString();
    }

    public static String dateToString(Date d) {
        return d==null?null:_df.format(d);
    }

    public static Date stringToDate(String s) {
        if (s==null) return null;
        try {
            return _df.parse(s);
        } catch (Exception e) {
            return null;
        }
    }

    public static String serviceDateToString(Date d) {
        return d == null ? null : _serviceDateFormat.format(d);
    }

    public static Date serviceStringToDate(String s) {
        if (s == null)
            return null;

        try {
            return _serviceDateFormat.parse(s);
        }
        catch (Exception e) {
            return null;
        }
    }

    public static String filenameDateToString(Date d) {
        return d == null ? null : _filenameDateFormat.format(d);
    }

    public static Date filenameStringToDate(String s) {
        if (s == null)
            return null;

        try {
            return _filenameDateFormat.parse(s);
        }
        catch (Exception e) {
            return null;
        }
    }
    public static String escapeXml(String str) {
        return str.replaceAll("&", "&amp;").replaceAll("<", "&lt;")
            .replaceAll("'", "&#39;").replaceAll("\"", "&quot;");
        // note: &apos; works for XML only, not html
    }

    public static String[] toStringArray(List<String> list) {
        String[] sarray = new String[list.size()];
        int i = 0;
        for (String s : list) {
            sarray[i++] = s;
        }
        return sarray;
    }

    private static final String HEX = "0123456789ABCDEF";

    public static String byteArrayToHexString(byte[] byteArray) {
        int len = byteArray.length;
        StringBuffer hexStr = new StringBuffer();
        for (int j=0; j < len; j++)
            hexStr.append(byteToHex((char)byteArray[j]));
        return hexStr.toString();
    }

    private static String byteToHex(char val) {
        int hi = (val & 0xF0) >> 4;
        int lo = (val & 0x0F);
        return "" + HEX.charAt(hi) + HEX.charAt(lo);
    }

    /**
     * Compress a string value using GZIP.
     */
    public static String compress(String uncompressedValue) throws IOException {
        if (uncompressedValue == null)
            return null;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        GZIPOutputStream gzip = null;
        try {
            gzip = new GZIPOutputStream(out);
            gzip.write(uncompressedValue.getBytes());
            gzip.close();
            return out.toString("ISO-8859-1");
        }
        finally {
            if (gzip != null)
                gzip.close();
        }
    }

    /**
     * Uncompress a string value that was compressed using GZIP.
     */
    public static String uncompress(String compressedValue) throws IOException {
        if (compressedValue == null)
            return null;

        ByteArrayInputStream in = new ByteArrayInputStream(compressedValue.getBytes("ISO-8859-1"));
        GZIPInputStream gzip = null;
        Reader reader = null;
        try {
            String value = "";
            gzip = new GZIPInputStream(in);
            reader = new InputStreamReader(gzip, "ISO-8859-1");
            int i;
            while ((i = reader.read()) != -1)
                value += (char) i;
            return value;
        }
        finally {
            if (gzip != null)
                gzip.close();
            if (reader != null)
                reader.close();
        }
    }

    public static String formatDate(Date d) {
        return serviceDateToString(d);
    }

    // To count comma separated columns in a row to maintain compatibility
    public static int delimiterColumnCount(String row, String delimeterChar, String escapeChar) {
        if (row.indexOf(escapeChar) > 0)
            return row.replace(escapeChar, " ").length() - row.replace(",", "").length();
        else
            return row.length() - row.replace(",", "").length();
    }

    private static DateFormat isoDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss:SSS'Z'");
    static {
        isoDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public static String formatIsoDate(Date date) {
        return isoDateFormat.format(date);
    }
    public static Date parseIsoDate(String iso) throws java.text.ParseException {
        return isoDateFormat.parse(iso);
    }

    public static class StringParseException extends RuntimeException {
        public StringParseException(String message) {
            super(message);
        }
        public StringParseException(String message, Throwable cause) {
            super(message, cause);
        }
        public StringParseException(Throwable cause) {
            super(cause);
        }
    }

}
