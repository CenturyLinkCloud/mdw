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

import com.qwest.mbeng.MbengException;
import com.centurylink.mdw.model.value.event.BamMessageDefinition;

public class BamMessageDefValueConverter implements ValueConverter {
    public Object toModelValue(String propertyValue) throws ConversionException {
        if (propertyValue == null || propertyValue.isEmpty())
            return null;

        try {
            return new BamMessageDefinition(propertyValue);
        }
        catch (MbengException ex) {
            throw new ConversionException(ex.getMessage(), ex);
        }
    }

    public String toPropertyValue(Object modelValue) throws ConversionException {
        if (!(modelValue instanceof BamMessageDefinition))
            throw new ConversionException("Must be instance of BamMessageDefinition instead of "
                    + modelValue.getClass().getName());

        BamMessageDefinition bamMsgDef = (BamMessageDefinition) modelValue;

        try {
            return bamMsgDef.format();
        }
        catch (MbengException ex) {
            throw new ConversionException("Conversion error for BAM Message: " + modelValue, ex);
        }
    }
}
