package com.centurylink.mdw.common.translator.impl;

public class LongTranslator extends BaseTranslator {

    public String toString(Object obj){
       return obj.toString();
    }

    public Object toObject(String str){
     return new Long(str);
    }
}