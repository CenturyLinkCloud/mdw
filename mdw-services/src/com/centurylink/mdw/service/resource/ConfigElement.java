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
import com.centurylink.mdw.common.service.XmlService;
import com.centurylink.mdw.common.utilities.FileHelper;

public class ConfigElement implements XmlService {

    public static final String PARAM_PATH = "path";
    public static final String PARAM_NAME = "name";
    
    public String getXml(Map<String,Object> parameters, Map<String,String> metaInfo) throws ServiceException {
        Object name = parameters.get(PARAM_PATH);
        if (name == null) {
            name = parameters.get(PARAM_NAME);
            if (name == null)
              throw new ServiceException("Missing parameter: one of 'path' or 'name' is required.");
        }
        // TODO: non-filesystem requests
        StringBuffer contents = new StringBuffer();
        InputStream stream = null;
        try {
            stream = FileHelper.openConfigurationFile(name.toString());
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            String line;
            while ((line=reader.readLine()) != null)
              contents.append(line).append("\n");
        }
        catch (IOException ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
        finally {
            try {
                if (stream != null)
                  stream.close();
            }
            catch (IOException ex) {
            }
        }

        return contents.toString();
    }

    public String getText(Map<String,Object> parameters, Map<String,String> metaInfo) throws ServiceException {
        return getXml(parameters, metaInfo);
    }

}
