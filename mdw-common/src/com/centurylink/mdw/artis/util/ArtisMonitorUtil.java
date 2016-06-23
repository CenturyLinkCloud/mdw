/**
 * Copyright (c) 2015 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.artis.util;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.centurylink.mdw.artis.ArtisRecordCache;
import com.centurylink.mdw.artis.MDWArtisConstants;

public class ArtisMonitorUtil {
    private static final Log LOG = LogFactory.getLog(ArtisMonitorUtil.class);

    public static ArtisRecordCache getArtisRecordCacheFromVariables(Map<String, Object> variables) {
        ArtisRecordCache response = new ArtisRecordCache();
        if (variables.containsKey(MDWArtisConstants.ARTISCACHE)) {

            Object artisCache = variables.get(MDWArtisConstants.ARTISCACHE);

            if (artisCache != null) {
                if (artisCache instanceof ArtisRecordCache) {
                    response = (ArtisRecordCache) artisCache;
                }
                else {
                    LOG.warn("Have ArtisCache Var but of wrong type: "
                            + artisCache.getClass().getName());
                }
            }
            else {
                LOG.debug("ArtisCache is Null");
            }
        }

        return response;
    }

    public static Map<String, String> getArtisHeadersFromVariables(Map<String, Object> variables) {
        Map<String, String> response = null;
        if (variables.containsKey(MDWArtisConstants.ARTISHEADERS)) {

            Object artisHeaders = variables.get(MDWArtisConstants.ARTISHEADERS);

            if (artisHeaders != null) {
                if (artisHeaders instanceof Map<?, ?>) {
                    try {
                        response = (Map<String, String>) artisHeaders;
                    }
                    catch (ClassCastException classCastException) {
                        LOG.warn("Have ArtisHeaders Var but caught ClassCastException when Type Casting to Map<String,String>: ");
                        LOG.info(
                                "ClassCastException while Type Casting ArtisHeaders to Map<String, String>",
                                classCastException);
                    }
                }
                else {
                    LOG.warn("Have ArtisHeaders Var but of wrong type: "
                            + artisHeaders.getClass().getName());
                }
            }
            else {
                LOG.debug("ArtisHeaders is Null");
            }
        }

        return response;
    }

    public static Map<String, String> getRequestHeadersFromVariables(Map<String, Object> variables) {
        Map<String, String> response = null;
        if (variables.containsKey(MDWArtisConstants.REQUESTHEADERS)) {

            Object requestHeaders = variables.get(MDWArtisConstants.REQUESTHEADERS);

            if (requestHeaders != null) {
                if (requestHeaders instanceof Map<?, ?>) {
                    try {
                        response = (Map<String, String>) requestHeaders;
                    }
                    catch (ClassCastException classCastException) {
                        LOG.warn("Have RequestHeaders Var but caught ClassCastException when Type Casting to Map<String,String>: ");
                        LOG.info(
                                "ClassCastException while Type Casting RequestHeaders to Map<String, String>",
                                classCastException);
                    }
                }
                else {
                    LOG.warn("Have RequestHeaders Var but of wrong type: "
                            + requestHeaders.getClass().getName());
                }
            }
            else {
                LOG.debug("RequestHeaders is Null");
            }
        }

        return response;
    }

}
