package com.centurylink.mdw.variable;

import com.centurylink.mdw.model.workflow.Package;
import com.centurylink.mdw.translator.DocumentReferenceTranslator;
import com.centurylink.mdw.translator.TranslationException;

public interface VariableTranslator {

    String EMPTY_STRING = "<EMPTY>";

    /**
     * Serialize the given object to a string value
     */
    String toString(Object obj) throws TranslationException;

    /**
     * Deserialize an object from the given string
     */
    Object toObject(String str) throws TranslationException;

    Package getPackage();

    default boolean isDocumentReferenceVariable() {
        return this instanceof DocumentReferenceTranslator;
    }
}
