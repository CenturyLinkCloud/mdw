package com.centurylink.mdw.tests.code;

import com.centurylink.mdw.annotations.Variable;
import com.centurylink.mdw.common.translator.impl.BaseTranslator;
import com.centurylink.mdw.translator.TranslationException;

@Variable(type="java.lang.Float")
public class FloatTranslator extends BaseTranslator {
    public String toString(Object obj) throws TranslationException {
        return obj.toString();
    }

    @Override
    public Object toObject(String str) throws TranslationException {
        return new Float(str);
    }
}
