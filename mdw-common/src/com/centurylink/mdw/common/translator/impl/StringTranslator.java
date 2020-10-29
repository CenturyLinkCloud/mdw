package com.centurylink.mdw.common.translator.impl;

public class StringTranslator extends BaseTranslator {

    public Object toObject(String str){
        return str;
    }

    public String toString(Object obj) {
        return (String)obj;
    }
}