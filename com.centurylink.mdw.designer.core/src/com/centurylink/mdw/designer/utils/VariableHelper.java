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
package com.centurylink.mdw.designer.utils;

import com.centurylink.mdw.common.cache.impl.VariableTypeCache;
import com.centurylink.mdw.common.translator.DocumentReferenceTranslator;
import com.centurylink.mdw.common.translator.VariableTranslator;
import com.centurylink.mdw.designer.DesignerDataAccess;
import com.centurylink.mdw.model.value.variable.VariableTypeVO;

public class VariableHelper {

	/**
	 * Check if the variable type is of a document.
	 * Custom variable translators must be loaded from server to make
	 * the decision precise.
	 *
	 * @param vartype
	 * @param dao
	 * @return
	 */
	public static boolean isDocumentVariable(String vartype, DesignerDataAccess dao) {
		VariableTranslator translator;
		VariableTypeVO vo = VariableTypeCache.getVariableTypeVO(vartype);
		if (vo==null) return false;
		try {
			Class<?> cl = Class.forName(vo.getTranslatorClass());
			translator = (VariableTranslator)cl.newInstance();
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return (translator instanceof DocumentReferenceTranslator);
	}

	/**
	 * This is a cheaper way for guessing whether the variable
	 * is a document variable, without a need to load custom variable
	 * translator from server to the client.
	 * @param vartype
	 * @return
	 */
	public static boolean isDocumentVariable(String vartype) {
		VariableTranslator translator;
		VariableTypeVO vo = VariableTypeCache.getVariableTypeVO(vartype);
		if (vo==null) return false;
		try {
			Class<?> cl = Class.forName(vo.getTranslatorClass());
			translator = (VariableTranslator)cl.newInstance();
			return (translator instanceof DocumentReferenceTranslator);
		} catch (Exception e) {
			// assuming all custom translators are for document variable types
			return true;
		}
	}

	/**
	 * This is another cheaper way for better guessing whether the variable
	 * is a document variable, when variable instance value is available
	 * @param vartype
	 * @param value variable instance value
	 * @return
	 */
	public static boolean isDocumentVariable(String vartype, String value) {
		VariableTranslator translator;
		VariableTypeVO vo = VariableTypeCache.getVariableTypeVO(vartype);
		if (vo==null) return false;
		try {
			Class<?> cl = Class.forName(vo.getTranslatorClass());
			translator = (VariableTranslator)cl.newInstance();
			return (translator instanceof DocumentReferenceTranslator);
		} catch (Exception e) {
			if (value==null) return false;
			return value.startsWith("DOCUMENT:");
		}
	}

}
