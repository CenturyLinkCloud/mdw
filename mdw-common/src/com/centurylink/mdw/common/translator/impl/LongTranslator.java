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


import com.centurylink.mdw.translator.VariableTranslator;


/**
 *
 */
public class LongTranslator extends VariableTranslator {

    // CONSTANTS ------------------------------------------------------

    // CLASS VARIABLES ------------------------------------------------

    // INSTANCE VARIABLES ---------------------------------------------

    // CONSTRUCTORS ---------------------------------------------------
    public LongTranslator(){
    }
    // PUBLIC AND PROTECTED METHODS -----------------------------------
    /**
     * Converts the passed in object to a string
     * @param pObject
     * @return String
     */
    public String toString(Object pObject){
       return pObject.toString();
    }

    /**
     * converts the passed in String to an equivalent object
     * @param pStr
     * @return Object
     */
    public Object toObject(String pStr){
     return new Long(pStr);
    }

       // PRIVATE METHODS ------------------------------------------------

    // ACCESSOR METHODS -----------------------------------------------

    // INNER CLASSES --------------------------------------------------
}