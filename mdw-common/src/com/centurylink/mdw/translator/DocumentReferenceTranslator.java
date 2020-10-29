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
     * whereas this method serializes the actual object to string
     * @param obj document object
     * @param variableType declared variable type
     */
    public abstract String toString(Object obj, String variableType)
            throws TranslationException;

    /**
     * toObject converts String to DocumentReference,
     * whereas this methods deserializes to actual object
     * @param str string representation
     * @param type document runtime type
     */
    public abstract Object toObject(String str, String type)
            throws TranslationException;
}
