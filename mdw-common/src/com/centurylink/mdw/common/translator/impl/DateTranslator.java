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
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Date;

import com.centurylink.mdw.translator.TranslationException;
import com.centurylink.mdw.translator.VariableTranslator;

public class DateTranslator extends VariableTranslator {
    private static DateFormat dateFormat;
    static {
        dateFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy");
    }

    public String toString(Object obj){
        if (obj instanceof Instant)
            return obj.toString();
        else
            return dateFormat.format((Date)obj);
    }

    public Object toObject(String str) throws TranslationException {
        try {
            return dateFormat.parse(str);
        }
        catch (ParseException ex) {
            try {
                // service dates can now be passed in ISO format
                return Date.from(Instant.parse(str));
            }
            catch (DateTimeParseException ex2) {
                throw new TranslationException(ex.getMessage(), ex);
            }
        }
    }
}
