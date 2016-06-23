/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.translator.impl;

// CUSTOM IMPORTS -----------------------------------------------------

// JAVA IMPORTS -------------------------------------------------------

import com.centurylink.mdw.common.translator.VariableTranslator;
import com.centurylink.mdw.common.utilities.StringHelper;

import java.util.StringTokenizer;

/**
 * 
 */
public class StringArrayTranslator extends VariableTranslator {

	// CONSTANTS ------------------------------------------------------

	// CLASS VARIABLES ------------------------------------------------

	// INSTANCE VARIABLES ---------------------------------------------

	// CONSTRUCTORS ---------------------------------------------------

	// PUBLIC AND PROTECTED METHODS -----------------------------------
	/**
	 * converts the passed in String to an equivalent object
	 * 
	 * @param pStr
	 * @return Object
	 */
	public Object toObject(String pStr) {
		String[] retArr = null;
		if (StringHelper.isEmpty(pStr) || EMPTY_STRING.equals(pStr)) {
			return new String[0];
		}
		StringTokenizer tokenizer = new StringTokenizer(pStr, ARRAY_DELIMETER);
		retArr = new String[tokenizer.countTokens()];
		int index = 0;
		while (tokenizer.hasMoreTokens()) {
			retArr[index] = tokenizer.nextToken();
			index++;
		}
		return retArr;
	}

	/**
	 * Converts the passed in object to a string
	 * 
	 * @param pObject
	 * @return String
	 */
	public String toString(Object pObject) {
		if (pObject instanceof String) {
			return (String) pObject;
		}
		String[] strArr = (String[]) pObject;
		if (strArr == null || strArr.length == 0) {
			return EMPTY_STRING;
		}
		StringBuffer buff = new StringBuffer();
		for (int i = 0; i < strArr.length; i++) {
			buff.append(strArr[i]).append(ARRAY_DELIMETER);
		}
		String st = buff.toString();
		return st.substring(0, st.length() - 1);
	}

	// PRIVATE METHODS ------------------------------------------------

	// ACCESSOR METHODS -----------------------------------------------

	// INNER CLASSES --------------------------------------------------

}