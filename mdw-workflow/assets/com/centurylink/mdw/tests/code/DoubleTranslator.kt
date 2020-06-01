package com.centurylink.mdw.tests.code

import com.centurylink.mdw.annotations.Variable
import com.centurylink.mdw.common.translator.impl.BaseTranslator

@Variable(type="java.lang.Double")
class DoubleTranslator : BaseTranslator() {

    override fun toString(obj: Any): String {
        return obj.toString()
    }

    override fun toObject(str: String): Any {
        return java.lang.Double(str)
    }
}