/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.service.resource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.common.service.TextService;

public class SourceCode implements TextService {

    public static final String PARAM_CLASS_NAME = "className";
    
    public String getText(Map<String,Object> parameters, Map<String,String> metaInfo) throws ServiceException {
        String className = parameters.get(PARAM_CLASS_NAME) == null ? null : parameters.get(PARAM_CLASS_NAME).toString();
        if (className == null) {
            throw new ServiceException("Missing parameter: 'className' is required.");
        }
        
        if (className.endsWith(".class"))
            className = className.substring(0, className.length() - 6);
        if (className.endsWith(".java"))
            className = className.substring(0, className.length() - 5);
        
        String sourceFile = className.replace('.', '/');
        sourceFile = sourceFile + ".java";
        if (!sourceFile.startsWith("/"))
            sourceFile = "/" + sourceFile;
        
        InputStream is = null;
        try {
            is = this.getClass().getResourceAsStream(sourceFile);
            if (is == null)
              throw new ServiceException("Cannot find source code file on classpath: " + sourceFile);
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String source = "", line = "";
            while ((line = br.readLine()) != null)
                source += line + "\n";
            return source;
        }
        catch (IOException ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
        finally {
            if (is != null) {
                try {
                  is.close();
                }
                catch (IOException ex) {
                }
            }
        }
    }

}
