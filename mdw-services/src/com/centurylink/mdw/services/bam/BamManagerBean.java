/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services.bam;

import java.sql.ResultSet;
import java.util.Map;

import org.apache.xmlbeans.XmlObject;

import com.centurylink.mdw.services.BamInterface;
import com.centurylink.mdw.services.BamManager;
import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.common.utilities.property.PropertyManager;
import com.centurylink.mdw.dataaccess.DatabaseAccess;


public class BamManagerBean implements BamManager
{
    protected DatabaseAccess db;

    public BamManagerBean(DatabaseAccess db) {
        this.db = db;
    }

    public BamManagerBean() {
    }

    public String handleEventMessage(String request, XmlObject xmlbean, Map<String,String> metainfo) throws Exception {
        DatabaseAccess bamDB = null;
        try {
            String dburl = PropertyManager.getProperty("bam.database.url");
            // when it is not defined, use native database
            //db = new DatabaseAccess(dburl, context);
            bamDB = new DatabaseAccess(dburl);
            bamDB.openConnection();
            BamInterface processor = new BamInterface(bamDB);
            String response = processor.handleEventMessage(request, xmlbean, metainfo);
            bamDB.commit();
            return response;
        } catch (Exception e) {
            StandardLogger logger = LoggerUtil.getStandardLogger();
            logger.severeException(e.getMessage(), e);  // should never come here
            bamDB.rollback();
            return null;
        } finally {
            if (bamDB!=null) bamDB.closeConnection();
        }
    }

    public String getMainProcessName(String masterRequestId) throws DataAccessException {
        try {
            db.openConnection();
            String query = "SELECT RULE_SET_NAME FROM PROCESS_INSTANCE, RULE_SET WHERE MASTER_REQUEST_ID = ?"
                    + " AND owner NOT IN ('PROCESS_INSTANCE', 'MAIN_PROCESS_INSTANCE')"
                    + " AND RULE_SET_ID = PROCESS_ID";
            Object[] args = new Object[1];
            args[0] = masterRequestId;
            ResultSet rs = db.runSelect(query, args);
            if (!rs.next())
                throw new DataAccessException("failed to load process info");
            return rs.getString(1);
        }
        catch (Exception e) {
            throw new DataAccessException(0, "failed to load process info", e);
        }
        finally {
            db.closeConnection();
        }
    }

    public Long getMainProcessInstanceId(String masterRequestId) throws DataAccessException {
        try {
            db.openConnection();
            String query = "SELECT PROCESS_INSTANCE_ID"
                    + " FROM PROCESS_INSTANCE WHERE MASTER_REQUEST_ID = ?"
                    + " AND owner NOT IN ('PROCESS_INSTANCE', 'MAIN_PROCESS_INSTANCE')";
            Object[] args = new Object[1];
            args[0] = masterRequestId;
            ResultSet rs = db.runSelect(query, args);
            if (!rs.next())
                throw new DataAccessException("failed to load process instance");
            return rs.getLong(1);
        }
        catch (Exception e) {
            throw new DataAccessException(0, "failed to load process instance", e);
        }
        finally {
            db.closeConnection();
        }
    }

    public Long getMainProcessId(String masterRequestId) throws DataAccessException {
        try {
            db.openConnection();
            String query = "SELECT PROCESS_ID FROM PROCESS_INSTANCE WHERE MASTER_REQUEST_ID = ?"
                    + " AND owner NOT IN ('PROCESS_INSTANCE', 'MAIN_PROCESS_INSTANCE')";
            Object[] args = new Object[1];
            args[0] = masterRequestId;
            ResultSet rs = db.runSelect(query, args);
            if (!rs.next())
                throw new DataAccessException("failed to load process info for masterRequestId = " + masterRequestId);
            return rs.getLong(1);
        }
        catch (Exception e) {
            throw new DataAccessException(0, "failed to load process masterRequestId =" + masterRequestId, e);
        }
        finally {
            db.closeConnection();
        }
    }

}
