/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.translator.impl;

import com.centurylink.mdw.common.exception.TranslationException;
import com.centurylink.mdw.common.translator.DocumentReferenceTranslator;
import com.qwest.mbeng.MbengException;
import com.qwest.mbeng.MbengDocumentClass;
import com.qwest.mbeng.MbengDocument;
import com.qwest.mbeng.FormatOmega;

public class ServiceOrderTranslator extends DocumentReferenceTranslator {

    
    public Object realToObject(String str) throws TranslationException {
        try {
			FormatOmega fmter = new FormatOmega(FormatOmega.OMEGA_FTS);
			MbengDocument order = new MbengDocumentClass();
			fmter.load(order, str);
			return order;
		} catch (MbengException e) {
			throw new TranslationException("Failed to parse the order: " + e.getMessage());
		}
    }

    public String realToString(Object object) throws TranslationException {
    	FormatOmega fmter = new FormatOmega(FormatOmega.OMEGA_FTS);
    	return fmter.format((MbengDocument)object);
    }

}
