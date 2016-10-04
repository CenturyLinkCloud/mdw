/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.translator;

import org.json.JSONObject;

import com.centurylink.mdw.common.exception.TranslationException;

public interface JsonTranslator {

    public JSONObject toJson(Object obj) throws TranslationException;

    public Object fromJson(JSONObject json) throws TranslationException;
}
