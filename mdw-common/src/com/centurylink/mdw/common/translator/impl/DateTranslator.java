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
