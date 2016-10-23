/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.translator;

import org.json.JSONObject;

public interface JsonTranslator {

    public JSONObject toJson(Object obj) throws TranslationException;

    public Object fromJson(JSONObject json) throws TranslationException;
}
