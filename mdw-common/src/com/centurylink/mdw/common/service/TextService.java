/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.service;

import java.util.Map;

public interface TextService {
    public String getText(Map<String,Object> parameters, Map<String,String> metaInfo) throws ServiceException;
}
