/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.translator.impl;


// CUSTOM IMPORTS -----------------------------------------------------

// JAVA IMPORTS -------------------------------------------------------

import com.centurylink.mdw.common.exception.TranslationException;
import com.centurylink.mdw.common.translator.VariableTranslator;

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