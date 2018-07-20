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
package com.centurylink.mdw.common.translator.impl;


import com.centurylink.mdw.translator.TranslationException;
import com.centurylink.mdw.translator.VariableTranslator;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.StringDecoder;
import org.apache.commons.codec.StringEncoder;
import org.apache.commons.codec.net.BCodec;


/**
 *
 */
public class EncodedStringTranslator extends VariableTranslator {

    // CONSTANTS ------------------------------------------------------

    // CLASS VARIABLES ------------------------------------------------
    private static StringEncoder encoder = new BCodec();
    private static StringDecoder decoder = new BCodec();
    // INSTANCE VARIABLES ---------------------------------------------

    // CONSTRUCTORS ---------------------------------------------------

    // PUBLIC AND PROTECTED METHODS -----------------------------------
    /**
     * converts the passed in String to an equivalent object
     * @param pStr
     * @return Object
     */
    public Object toObject(String pStr){
        String decoded = null;
        try{
            decoded = decoder.decode(pStr);
        }catch(DecoderException ex){
             ex.printStackTrace();
             throw new TranslationException(ex.getMessage());
        }
        return decoded;
    }

    /**
     * Converts the passed in object to a string
     * @param pObject
     * @return String
     */
    public String toString(Object pObject){
        String encoded = null;
        try{
           encoded=  encoder.encode((String)pObject);
        }catch(EncoderException ex){
            ex.printStackTrace();
            throw new TranslationException(ex.getMessage());
        }
        return encoded;
    }



       // PRIVATE METHODS ------------------------------------------------

    // ACCESSOR METHODS -----------------------------------------------

    // INNER CLASSES --------------------------------------------------
}