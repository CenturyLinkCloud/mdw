/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.service.action;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.bpm.MDWStatusMessageDocument;
import com.centurylink.mdw.bpm.MDWStatusMessageDocument.MDWStatusMessage;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.common.service.XmlService;
import com.centurylink.mdw.config.PropertyException;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.event.EventHandlerException;
import com.centurylink.mdw.service.Action;
import com.centurylink.mdw.service.ActionRequestDocument;
import com.centurylink.mdw.service.ActionRequestDocument.ActionRequest;
import com.centurylink.mdw.service.Parameter;
import com.centurylink.mdw.services.cache.CacheRegistration;
import com.centurylink.mdw.util.HttpHelper;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

public class RefreshCache implements XmlService {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    public String getXml(XmlObject request, Map<String,String> metaInfo)
    throws ServiceException {
        try {
            boolean refreshProps = true;
            boolean refreshAllCaches = true;
            boolean refreshSingleCache = false;
            String singleCacheName = null;

            Object refreshType = metaInfo.get("RefreshType");
            if ("Properties".equals(refreshType)) {
                refreshAllCaches = false;
            }
            else if ("Caches".equals(refreshType)) {
                // props included with cache refresh
                // refreshProps = false;
            }
            else if ("SingleCache".equals(refreshType)) {
                refreshSingleCache = true;
                refreshAllCaches = false;
                refreshProps = false;
            }

            if (metaInfo.get("CacheName") != null) {
                singleCacheName = metaInfo.get("CacheName").toString();
            }

            if (refreshProps) {
                // local refresh
                PropertyManager.getInstance().refreshCache();
                LoggerUtil.getStandardLogger().refreshCache();  // in case log props have changed
            }

            String requestUrl = metaInfo.get("RequestURL");

            if (refreshAllCaches) {
                List<String> excludedFormats = null;
                if (metaInfo.get("ExcludedFormats") != null)
                    excludedFormats = Arrays.asList(metaInfo.get("ExcludedFormats").toString().split(","));
                CacheRegistration.getInstance().refreshCaches(excludedFormats);
                logger.info("Cache refresh complete.");
            }

            if (refreshSingleCache) {
                if (singleCacheName == null)
                    throw new EventHandlerException("Missing request parameter: 'CacheName'");
                new CacheRegistration().refreshCache(singleCacheName);
            }

            if (metaInfo.get("GlobalRefresh") != null && Boolean.parseBoolean(metaInfo.get("GlobalRefresh").toString())) {
                // pass the message on to other servers (minus the global flag)
                ActionRequestDocument actionRequestDoc = ActionRequestDocument.Factory.newInstance();
                ActionRequest actionRequest = actionRequestDoc.addNewActionRequest();
                Action action = actionRequest.addNewAction();
                action.setName("RefreshProcessCache");
                for (String paramName : metaInfo.keySet()) {
                    if (!paramName.equals("GlobalRefresh")) {
                        Parameter parameter = action.addNewParameter();
                        parameter.setName(paramName);
                        parameter.setStringValue(metaInfo.get(paramName).toString());
                    }
                }
                for (URL serviceUrl : getOtherServerUrls(requestUrl)) {
                    HttpHelper httpHelper = new HttpHelper(serviceUrl);
                    String response = httpHelper.post(actionRequestDoc.xmlText());
                    MDWStatusMessageDocument statusMessageDoc = MDWStatusMessageDocument.Factory.parse(response);
                    MDWStatusMessage statusMessage = statusMessageDoc.getMDWStatusMessage();
                    if (statusMessage.getStatusCode() != 0)
                        throw new EventHandlerException(statusMessage.getStatusCode(), "Remote refresh failed: " + statusMessage.getStatusMessage());
                }
            }

            return createSuccessResponse("Refresh successful.");
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
            return createErrorResponse(ex.getMessage());
        }
    }

    private List<URL> getOtherServerUrls(String requestUrl) throws PropertyException {

        List<URL> serverUrls = new ArrayList<URL>();
        List<String> serverList = ApplicationContext.getCompleteServerList();
        for (String server : serverList) {
            String serviceUrl = "http://" + server + "/" + ApplicationContext.getMdwHubContextRoot() + "/services";
            try {
                URL thisUrl = new URL(requestUrl);
                URL otherUrl = new URL(serviceUrl);
                if (!(thisUrl.getHost().equals(otherUrl.getHost())) || thisUrl.getPort() != otherUrl.getPort()) {
                    serverUrls.add(new URL(serviceUrl));
                    // mdwweb may have separate caches
                    String serviceUrl2 = "http://" + server + "/" + ApplicationContext.getServicesContextRoot() + "/services";
                    if (!serviceUrl.equals(serviceUrl2))
                      serverUrls.add(new URL(serviceUrl2));
                }
            } catch (MalformedURLException ex) {
                logger.severeException(ex.getMessage(), ex);
            }
        }
        return serverUrls;
    }

    private String createSuccessResponse(String message) {
        MDWStatusMessageDocument successResponseDoc = MDWStatusMessageDocument.Factory.newInstance();
        MDWStatusMessage statusMessage = successResponseDoc.addNewMDWStatusMessage();
        statusMessage.setStatusCode(0);
        statusMessage.setStatusMessage(message);
        return successResponseDoc.xmlText(getXmlOptions());
    }

    private String createErrorResponse(String message) {
        MDWStatusMessageDocument errorResponseDoc = MDWStatusMessageDocument.Factory.newInstance();
        MDWStatusMessage statusMessage = errorResponseDoc.addNewMDWStatusMessage();
        statusMessage.setStatusCode(-1);
        statusMessage.setStatusMessage(message);
        return errorResponseDoc.xmlText(getXmlOptions());
    }

    private XmlOptions getXmlOptions() {
        return new XmlOptions().setSavePrettyPrint().setSavePrettyPrintIndent(2);
    }

    public String getText(Object requestObj, Map<String,String> metaInfo) throws ServiceException {
        return getXml((XmlObject)requestObj, metaInfo);
    }
}
