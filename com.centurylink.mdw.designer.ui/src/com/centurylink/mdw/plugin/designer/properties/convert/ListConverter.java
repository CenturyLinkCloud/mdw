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

import java.util.List;

import com.centurylink.mdw.common.utilities.StringHelper;

public class ListConverter implements ValueConverter {
    public Object toModelValue(String propertyValue) throws ConversionException {
        if (propertyValue == null)
            return null;
        return StringHelper.parseList(propertyValue);
    }

    public String toPropertyValue(Object modelValue) throws ConversionException {
        if (modelValue == null)
            return null;

        if (!(modelValue instanceof List<?>))
            throw new ConversionException("Model value must be an instance of List rather than "
                    + modelValue.getClass().getName());

        String value = "";
        List<?> strings = (List<?>) modelValue;
        for (int i = 0; i < strings.size(); i++) {
            value += strings.get(i);
            if (i < strings.size() - 1)
                value += "#";
        }
        return value;
    }

}
