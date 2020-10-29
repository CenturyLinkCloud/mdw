package com.centurylink.mdw.common.translator.impl;

import com.centurylink.mdw.translator.TranslationException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Date;

public class DateTranslator extends BaseTranslator {

    private static final String dateFormat = "EEE MMM dd HH:mm:ss zzz yyyy";

    public String toString(Object obj){
        if (obj instanceof Instant)
            return obj.toString();
        else
            return new SimpleDateFormat(dateFormat).format((Date) obj);
    }

    public Object toObject(String str) throws TranslationException {
        try {
            return new SimpleDateFormat(dateFormat).parse(str);
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
