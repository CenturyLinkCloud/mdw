/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.translator.impl;


import com.centurylink.mdw.common.translator.VariableTranslator;
import com.centurylink.mdw.common.utilities.StringHelper;

import java.util.StringTokenizer;

/**
 * 
 */
public class IntegerArrayTranslator extends VariableTranslator {


	/**
	 * converts the passed in String to an equivalent object
	 * 
	 * @param pStr
	 * @return Object
	 */
	public Object toObject(String pStr) {
		Integer[] retArr = null;
		if (StringHelper.isEmpty(pStr) || EMPTY_STRING.equals(pStr)) {
			return new Integer[0];
		}
		StringTokenizer tokenizer = new StringTokenizer(pStr, ARRAY_DELIMETER);
		retArr = new Integer[tokenizer.countTokens()];
		int index = 0;
		while (tokenizer.hasMoreTokens()) {
			retArr[index] = new Integer(tokenizer.nextToken());
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
		Integer[] objArr = (Integer[]) pObject;
		if (objArr == null || objArr.length == 0) {
			return EMPTY_STRING;
		}
		StringBuffer buff = new StringBuffer();
		for (int i = 0; i < objArr.length; i++) {
			buff.append(objArr[i].toString()).append(ARRAY_DELIMETER);
		}
		String st = buff.toString();
		return st.substring(0, st.length() - 1);
	}



}