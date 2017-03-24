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
package com.centurylink.mdw.plugin.designer.properties.convert;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateConverter implements ValueConverter {
    SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy  HH:mm:ss");

    public Object toModelValue(String propertyValue) throws ConversionException {
        try {
            return dateFormat.parse(propertyValue);
        }
        catch (ParseException ex) {
            throw new ConversionException(ex.getMessage(), ex);
        }
    }

    public String toPropertyValue(Object modelValue) throws ConversionException {
        if (!(modelValue instanceof Date))
            throw new ConversionException("Must be instance of java.util.Date instead of "
                    + modelValue.getClass().getName());
        return dateFormat.format((Date) modelValue);
    }

}
