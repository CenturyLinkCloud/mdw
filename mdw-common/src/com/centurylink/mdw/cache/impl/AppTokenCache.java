/**
 * Copyright (c) 2018 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.cache.impl;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.centurylink.mdw.annotations.RegisteredService;
import com.centurylink.mdw.cache.CacheService;
import com.centurylink.mdw.dataaccess.db.CommonDataAccess;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

@RegisteredService(CacheService.class)
public class AppTokenCache implements CacheService {

	private static StandardLogger logger = LoggerUtil.getStandardLogger();
	private static volatile Map<String,String> mdwAppTokenMap = new ConcurrentHashMap<String,String>();

	public static String getAppToken(String appId) {
		if (mdwAppTokenMap.isEmpty())
			load();

		return mdwAppTokenMap.get(appId);
	}

	public static void addToken(String appId, String value) {
		if (value != null && value.length() > 0)
			mdwAppTokenMap.put(appId, value);
	}

	private static synchronized void load() {
		Map<String,String> tempMdwAppTokenMap = mdwAppTokenMap;
        if (tempMdwAppTokenMap.isEmpty()) {
        	try {
        		CommonDataAccess dao = new CommonDataAccess();
             	List<String> appIds = dao.getValueOwnerIds("APP", "MDW_APP_TOKEN", null);
             	for (String appId : appIds) {
             		tempMdwAppTokenMap.put(appId, dao.getValue("APP", appId, "MDW_APP_TOKEN"));
             	}
        	}
            catch (SQLException e) {
            	logger.severeException("Could not load the Application Tokens", e);
            }
        }
        mdwAppTokenMap = tempMdwAppTokenMap;
	}

	@Override
	public void refreshCache() throws Exception {
		mdwAppTokenMap.clear();
	}

	@Override
	public void clearCache() {
	}

}
