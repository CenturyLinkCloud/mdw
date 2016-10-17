/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.service.resource;

import java.sql.SQLException;
import java.util.Map;

import javax.xml.transform.TransformerException;

import org.json.JSONException;

import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.service.JsonService;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.common.service.TextService;
import com.centurylink.mdw.common.service.XmlService;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.common.utilities.property.PropertyManager;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.model.data.bam.MasterRequest;
import com.centurylink.mdw.services.dao.bam.BamDataAccess;
import com.centurylink.mdw.services.dao.bam.BamDataAccessDao;

public class BamEventSummary implements TextService, XmlService, JsonService {

    public static final String PARAM_MASTER_REQUEST_ID = "MasterRequestId";
    public static final String PARAM_REALM = "Realm";
    StandardLogger logger = LoggerUtil.getStandardLogger();

    public String getText(Map<String,Object> parameters, Map<String,String> metaInfo) throws ServiceException {
        String masterRequestId = (String) parameters.get(PARAM_MASTER_REQUEST_ID);
        String realm = (String) parameters.get(PARAM_REALM);
        if (masterRequestId != null && realm != null) {
            try {
                return getBamEvents(masterRequestId, realm);
            }
            catch (DataAccessException ex) {
                throw new ServiceException(ex.getMessage(), ex);
            }
        }
        else {
          throw new ServiceException("Missing parameter: both 'MasterRequestId' and 'Realm' are required. masterRequestId = " + masterRequestId + " Realm = " + realm);
        }
    }

    /**
     * Bypasses cache.
     * @param masterRequestId
     * @param realm
     */
    private String getBamEvents(String masterRequestId, String realm) throws DataAccessException {
        DatabaseAccess db = null;
        String dburl = PropertyManager.getProperty("bam.database.url");
        MasterRequest masterRequest = null;
        db = new DatabaseAccess(dburl);
        try {
            db.openConnection();
            BamDataAccess dbhelper = new BamDataAccessDao();
            masterRequest = dbhelper.loadMasterRequest(db, masterRequestId, realm, 3);
            db.commit();
            if (masterRequest != null)
                return masterRequest.toXml();
            else return null;
        }
        catch (SQLException e) {
            logger.severeException("Unable to get BAM data for masterRequestId =" + masterRequestId + " and realm =" + realm + e.getMessage(), e);  // should never come here
            db.rollback();
            return null;
        }
        catch (TransformerException e) {
            logger.severeException("Unable to Transform BAM data for masterRequestId =" + masterRequestId + " and realm =" + realm + e.getMessage(), e);  // should never come here
            db.rollback();
            return null;
        }
        finally{
            if (db!=null) db.closeConnection();
        }
    }

    @Override
    public String getJson(Map<String, Object> parameters, Map<String, String> metaInfo) throws ServiceException {
         try {
            return org.json.XML.toJSONObject(getText(parameters, metaInfo)).toString();
        }
        catch (JSONException e) {
            throw new ServiceException("Unable to parse XML to Json "+e.getMessage(), e);
        }
    }

    @Override
    public String getXml(Map<String, Object> parameters, Map<String, String> metaInfo) throws ServiceException {
        return getText(parameters, metaInfo);
    }

}
