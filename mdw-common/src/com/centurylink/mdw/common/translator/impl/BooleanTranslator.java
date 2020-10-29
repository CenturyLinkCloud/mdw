package com.centurylink.mdw.common.translator.impl;

import com.centurylink.mdw.annotations.Variable;

public class BooleanTranslator extends BaseTranslator {

    public String toString(Object obj){
       return obj.toString();
    }

    public Object toObject(String str){
        return Boolean.valueOf(str);
    }
}