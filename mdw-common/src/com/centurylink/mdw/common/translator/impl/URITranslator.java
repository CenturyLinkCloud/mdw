package com.centurylink.mdw.common.translator.impl;

import java.net.URI;

public class URITranslator extends BaseTranslator {

    public String toString(Object obj){
       return obj.toString();
    }

    public Object toObject(String str){
      return URI.create(str);
    }

}