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
package com.centurylink.mdw.translator;

import com.centurylink.mdw.common.translator.impl.BaseTranslator;
import com.centurylink.mdw.model.variable.DocumentReference;

public abstract class DocumentReferenceTranslator extends BaseTranslator {

    public final Object toObject(String str) throws TranslationException {
        return new DocumentReference(new Long(str.substring(9)));
    }

    public final String toString(Object obj) throws TranslationException {
        return obj.toString();
    }

    /**
     * toString converts DocumentReference to string,
     * whereas this method converts the real object to string
     */
    public abstract String realToString(Object obj)
    throws TranslationException;

    /**
     * toObject converts String to DocumentReference
     * whereas this methods converts the string to real object
     */
    public abstract Object realToObject(String str)
    throws TranslationException;
}
