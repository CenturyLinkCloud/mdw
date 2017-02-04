/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.translator.impl;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.centurylink.mdw.translator.TranslationException;
import com.centurylink.mdw.translator.VariableTranslator;

public class DateTranslator extends VariableTranslator {
    private static DateFormat dateFormat;
    static {
        dateFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy");
    }

    public String toString(Object obj){
        return dateFormat.format((Date)obj);
    }

    public Object toObject(String str) throws TranslationException {
        try {
            return dateFormat.parse(str);
        }
        catch (ParseException ex) {
            throw new TranslationException(ex.getMessage(), ex);
        }
    }
}
