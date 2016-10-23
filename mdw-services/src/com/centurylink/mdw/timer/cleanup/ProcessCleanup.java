/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.timer.cleanup;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;

import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.PropertyGroups;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.model.workflow.WorkStatus;
import com.centurylink.mdw.services.workflow.RoundRobinScheduledJob;
import com.centurylink.mdw.util.CallURL;
import com.centurylink.mdw.util.file.FileHelper;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

/**
 * Clean up old database entries from processes that are older than a specified amount
 */
public class ProcessCleanup extends RoundRobinScheduledJob {

    private static final String PROCESS_SQL_FILE_NAME = "ProcessSql.txt";
    private static final String EXTERNAL_SQL_FILE_NAME = "EventSql.txt";
    private static final String EVENT_LOG_SQL_FILE_NAME = "EventLogSql.txt";
    private static final String SQL_DELIMITER = "@@@@@";

    private StandardLogger logger;

    /**
     * Default Constructor
     */
    public ProcessCleanup() {
    }

    /**
     * Method that gets invoked periodically by the container
     *
     */
    public void run(CallURL args) {

        logger = LoggerUtil.getStandardLogger();

        logger.info("methodEntry-->ProcessCleanup.run()");

        int processExpirationDays = PropertyManager.getIntegerProperty(PropertyGroups.PROCESS_CLEANUP+"/ProcessExpirationAgeInDays",0);
        int maxProcesses = PropertyManager.getIntegerProperty(PropertyGroups.PROCESS_CLEANUP+"/MaximumProcessExpiration",0);
        int eventExpirationDays = PropertyManager.getIntegerProperty(PropertyGroups.PROCESS_CLEANUP+"/ExternalEventExpirationAgeInDays",0);
        int commitInterval = PropertyManager.getIntegerProperty(PropertyGroups.PROCESS_CLEANUP+"/CommitInterval",10000);
        String cleanupScript = PropertyManager.getProperty(PropertyGroups.PROCESS_CLEANUP+"/RuntimeCleanupScript");

        if (cleanupScript!=null) {
        	DatabaseAccess db = new DatabaseAccess(null);
        	cleanup(db, cleanupScript, maxProcesses, processExpirationDays, eventExpirationDays, commitInterval);
        } else {
        	try {
	            ArrayList<String> eventLogSql = readSql(EVENT_LOG_SQL_FILE_NAME);
	            executeEventLogDelete(eventLogSql, maxProcesses, eventExpirationDays);

	            // find the minimum PID to start looking at
	            Long minPID = getMinPid();

	            ArrayList<Long> expiredProcesses = getProcessIds(processExpirationDays, maxProcesses, minPID);
	            ArrayList<String> processSql = readSql(PROCESS_SQL_FILE_NAME);
	            executeDelete(processSql, expiredProcesses);

	            ArrayList<Long> expiredEvents = getEventIds(eventExpirationDays, maxProcesses, minPID);
	            ArrayList<String> eventSql = readSql(EXTERNAL_SQL_FILE_NAME);
	            executeDelete(eventSql, expiredEvents);
        	} catch (Exception ex) {
        		logger.severeException(ex.getMessage(), ex);
        		throw new RuntimeException(ex);
        	}
        }

        logger.info("methodExit-->ProcessCleanup.run()");
    }

    // PRIVATE METHODS ------------------------------------------------
    /**
     * Takes a filename on the classpath as a param, and reads SQL statements delimited by SQL_DELIMITER on
     * it's own line
     * @param filename
     * @return ArrayList<String> with each element containing a SQL statement
     */
    private ArrayList<String> readSql(String filename) throws IOException {
        ArrayList<String> sqlStatements = new ArrayList<String>();
        InputStream stream = null;
        stream = this.getClass().getClassLoader().getResourceAsStream(filename);
        BufferedReader bsr = new BufferedReader(new InputStreamReader(stream));
        String line = "";
        StringBuffer sb = new StringBuffer();
        String statement = null;
        while (line != null) {
            line = bsr.readLine();
            if (!SQL_DELIMITER.equals(line)) {
                sb.append(line);
            } else {
                statement = sb.toString();
                sqlStatements.add(statement);
                sb = new StringBuffer();
            }
        }

        bsr.close();
        stream.close();

        return sqlStatements;
    }

    /**
     * Get the list of process ID's that fit the criteria of being in the correct state and old enough
     * to warrant deletion.
     * @param days
     * @param maxIds
     * @return ArrayList<Long> of ID's
     */
    private ArrayList<Long> getProcessIds(int pDays, int pMaxIds, Long pMinPID) {
        final ArrayList<Long> pids = new ArrayList<Long>();
        String getPidSql = "select process_instance_id from ( "+
        " select process_instance_id from process_instance where status_cd = 4 "+
        " and process_instance_id >= ? and round(sysdate - create_dt) > ? order by process_instance_id) "+
        " where rownum < ? ";

        DatabaseAccess db = new DatabaseAccess(null);
        try {
        	db.openConnection();
        	Object[] args = new Object[3];
        	args[0] = pMinPID;
        	args[1] = pDays;
        	args[2] = pMaxIds;
        	ResultSet rs = db.runSelect(getPidSql, args);
        	while (rs.next()) {
        		pids.add(rs.getLong(1));
        	}
        } catch (SQLException ex) {
            logger.severeException(ex.getMessage(), ex);
        } finally {
        	db.closeConnection();
        }

        return pids;
    }

    /**
     * Get the list of event ID's that fit the criteria of being in the correct state and old enough
     * to warrant deletion.
     * @param days
     * @param maxIds
     * @return ArrayList<Long> of ID's
     */
    private ArrayList<Long> getEventIds(int pDays, int pMaxIds, Long pMinPID) {
        final ArrayList<Long> eids = new ArrayList<Long>();
        String getEidSql = "  select owner_id from ( select owner_id, owner from ( " +
        " select owner_id, owner from process_instance where status_cd = 4 " +
        " and process_instance_id > ? and round(sysdate - create_dt) > ? order by process_instance_id) "+
        " where rownum < ? ) where owner = 'EXTERNAL_EVENT_INSTANCE'";

        DatabaseAccess db = new DatabaseAccess(null);
        try {
        	db.openConnection();
        	Object[] args = new Object[3];
        	args[0] = pMinPID;
        	args[1] = pDays;
        	args[2] = pMaxIds;
        	ResultSet rs = db.runSelect(getEidSql, args);
        	while (rs.next()) {
        		eids.add(rs.getLong(1));
        	}
        } catch (SQLException ex) {
            logger.severeException(ex.getMessage(), ex);
        } finally {
        	db.closeConnection();
        }

        return eids;
    }

    /**
     * Returns the minimum process_instance ID that has a status code of 4
     * @return Long - minPid where the status_cd = 4
     */
    private Long getMinPid() {
        String minPidSql = "select min(PROCESS_INSTANCE_ID) FROM PROCESS_INSTANCE WHERE status_cd = 4";
        DatabaseAccess db = new DatabaseAccess(null);
        Long MinPid;
        try {
        	db.openConnection();
        	ResultSet rs = db.runSelect(minPidSql, null);
        	if (rs.next()) MinPid = rs.getLong(1);
        	else MinPid = null;	// not possible
        } catch (SQLException ex) {
            logger.severeException(ex.getMessage(), ex);
            MinPid = null;
        } finally {
        	db.closeConnection();
        }
        return MinPid;
    }

    /**
     * Take a list of delete statements, and for each ID, iterate over each statement to perform the deletes
     * @param pStatementList
     * @param pIds
     *
     */
    private void executeDelete(ArrayList<String> pStatementList, ArrayList<Long> pIds) {
        int[] countByQuery = new int[pStatementList.size()];

        DatabaseAccess db = new DatabaseAccess(null);
        int deletedRows = 0;
        try {
        	db.openConnection();
        	//iterate over the queries and then pIds
            for (int j=0; j<pStatementList.size(); j++) {
                logger.debug("Executing " + pStatementList.get(j));
                db.prepareStatement(pStatementList.get(j));
                for (int i=0; i<pIds.size(); i++) {
                	db.addToBatch(new Object[]{pIds.get(i)});
	            }
                int[] counts = db.runBatchUpdate();
                for (int i=0; i<counts.length; i++) {
                	deletedRows += counts[i];
                	countByQuery[j] += counts[j];
                }
            }
        } catch (SQLException ex) {
            logger.severeException(ex.getMessage(), ex);
            ex.printStackTrace();
        } finally {
        	db.closeConnection();
        }

        logger.info("ProcessCleanup.executeDelete() -> Total rows deleted from multiple tables: " + deletedRows);

        for (int j=0; j<pStatementList.size(); j++) {
        	logger.info("ProcessCleanup.executeDelete() " + countByQuery[j]
                             + " deletes for SQL: " + pStatementList.get(j));
        }

    }

    /**
     * Take a list of Event Delete statements, execute the delete for everything earlier than
     * pRetainDays
     * @param pStatementList
     * @param pMaxRows
     * @param pRetainDays
     */
    private void executeEventLogDelete(ArrayList<String> pStatementList, int pMaxRows, int pRetainDays) {
        //This first part determines the min EventLog ID
        String minELidSql = "select min(EVENT_LOG_ID) FROM EVENT_LOG";
        int[] countByQuery = new int[pStatementList.size()];

        DatabaseAccess db = new DatabaseAccess(null);
        Long ELid;

        try {
        	db.openConnection();
        	ResultSet rs = db.runSelect(minELidSql, null);
        	if (rs.next()) ELid = rs.getLong(1);
        	else ELid = null;	// not possible
        } catch (SQLException ex) {
            logger.severeException(ex.getMessage(), ex);
            ex.printStackTrace();
            return;
        } finally {
        	db.closeConnection();
        }

        //create a query object for each statement, creating an setId method to set the long value
        //before each execution

        int deletedRows = 0;
        try {
        	db.openConnection();
        	Object[] args = new Object[4];
            args[0] = ELid;
            args[1] = ELid;
            args[2] = pMaxRows;
            args[3] = pRetainDays;
            for (int i=0; i<pStatementList.size(); i++) {
                logger.debug("Executing " + pStatementList.get(i));
            	int count = db.runUpdate(pStatementList.get(i), args);
            	deletedRows += count;
            	countByQuery[i] += count;
            }
        } catch (SQLException ex) {
            logger.severeException(ex.getMessage(), ex);
            ex.printStackTrace();
        } finally {
        	db.closeConnection();
        }

        logger.info("ProcessCleanup.executeEventLogDelete() -> Total rows deleted from multiple tables: " + deletedRows);
        for (int j=0; j<pStatementList.size(); j++) {
        	logger.info("ProcessCleanup.executeEventLogDelete() " + countByQuery[j]
                               + " deletes for SQL: " + pStatementList.get(j));
        }

    }

    private void enable_output(DatabaseAccess db, int bufsize) throws SQLException {
    	String query = "begin dbms_output.enable(" + bufsize + "); end;";
    	CallableStatement callStmt = db.getConnection().prepareCall(query);
    	callStmt.executeUpdate();
    }

    @SuppressWarnings("unused")
	private void disable_output(DatabaseAccess db) throws SQLException {
    	String query = "begin dbms_output.disable; end;";
    	CallableStatement callStmt = db.getConnection().prepareCall(query);
    	callStmt.executeUpdate();
    }

    private void show_output(DatabaseAccess db) throws SQLException {
    	CallableStatement show_stmt = db.getConnection().prepareCall(
    	          "declare " +
    	          "    l_line varchar2(255); " +
    	          "    l_done number; " +
    	          "    l_buffer long; " +
    	          "begin " +
    	          "  loop " +
    	          "    exit when length(l_buffer)+255 > :maxbytes OR l_done = 1; " +
    	          "    DBMS_OUTPUT.get_line( l_line, l_done ); " +
    	          "    l_buffer := l_buffer || l_line || chr(10); " +
    	          "  end loop; " +
    	          " :done := l_done; " +
    	          " :buffer := l_buffer; " +
    	          "end;" );
    	int done = 0;
    	int maxbytes = 4096;	// retrieve up to 4096 bytes at a time
        show_stmt.registerOutParameter( 2, Types.INTEGER );
        show_stmt.registerOutParameter( 3, Types.VARCHAR );
        for(;;)
        {
            show_stmt.setInt( 1, maxbytes);
            show_stmt.executeUpdate();
			if (null == logger) {
				System.out.println(show_stmt.getString(3));
			} else {
				logger.info(show_stmt.getString(3));
			}
            done = show_stmt.getInt(2);
            if (done== 1) break;
        }
    }

    private void cleanup(DatabaseAccess db, String filename,
            int maxProcInst, int processExpirationDays, int eventExpirationDays, int commitInterval) {
        try {
            InputStream is = FileHelper.openConfigurationFile(filename, getClass().getClassLoader());
            byte[] bytes = FileHelper.readFromResourceStream(is);
            String query = new String(bytes);
            query = query.replaceAll("\\r", "").trim();
            if (query.endsWith("/")) query = query.substring(0,query.length()-1);
            db.openConnection();
            if (!db.isMySQL()) enable_output(db, 16384);
            CallableStatement callStmt = db.getConnection().prepareCall(query);
            callStmt.setInt(1, maxProcInst);            // max proc instances to delete
            callStmt.setInt(2, processExpirationDays);  // number of days for expiration for process and related
            callStmt.setInt(3, eventExpirationDays);    // number of days for expiration for events
            callStmt.setInt(4, 0);                      // process ID; 0 indicates any process
            callStmt.setString(5, WorkStatus.STATUS_FAILED.toString()
                    + "," + WorkStatus.STATUS_COMPLETED.toString()
                    + "," + WorkStatus.STATUS_CANCELLED.toString());        // process instance statuses
            callStmt.setInt(6, commitInterval);
            callStmt.execute();
            if (!db.isMySQL()) show_output(db);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            db.closeConnection();
        }
    }

    public static void main (String args[]) throws Exception
    {
    	String jdbcUrl = "jdbc:oracle:thin:mdwdev/mdwdev@mdwdevdb.dev.qintra.com:1594:mdwdev";
//    	jdbcUrl = "jdbc:oracle:thin:bpms/bpms@esowf_d.dev.qintra.com:1541:esowf01d";
    	DatabaseAccess db = new DatabaseAccess(jdbcUrl);
    	ProcessCleanup me = new ProcessCleanup();
    	me.cleanup(db, "Cleanup-Runtime.sql", 5, 180, 175, 2);
    }

	// TODO misc things for runtime data clean ups
    // - clean-up PL/SQL script does not work for non-Oracle DBMS
    // - processes that are not completed but parent processes are completed - what to do?
    // - attachment table is not cleaned - is it used???
    // - how to clean up data for process instances that are really old but not completed?
    // - clean TASK_INST_GRP_MAPP and TASK_INST_INDEX need to be excluded for MDW 5.0
    // - clean task instances that are not associated with process instances
    // - document table is difficult due to its mixed usage. The following is current implementation
    //   based on owner type:
    //		VARIABLE_INSTANCE: process instance ID is always populated. Delete along with process instance
    //			Note: previously there are cases when process instance ID is 0 (and variable instance id is 0 as well)
    //			one place in RegressionTestEventHandler, when data is to be passed to a document variable
    //			this is fixed on 3/26/2011 by changing to DOCUMENT as owner. Keep an eye to see if there are other cases.
    //		ADAPTOR_REQUEST: process instance ID is always populated. Delete along with process instance
    //		ADAPTOR_RESPONSE: process instance ID is always populated. Delete along with process instance
    //		TASK_INSTANCE:
    //			a. when task manager notifies engine for classic tasks (ActionRequest) - process
    //				instance ID is always populated.
    //			b. (local) general tasks send message to engine. process instance id never populated
    //		INTERNAL_EVENT:
    //			a. when launching process directly and some argument is to be bound to document variable
    //					(same usage as DOCUMENT owner, so changed to use DOCUMENT on 3/26/2011)
    //			b. signal (PublishEventMessage) - process instance ID is populated
    //		LISTENER_REQUEST: process instance ID is *NOT* always populated. Need to investigate
    //				seems can delete based on age (mostly for requests to event handler not related to processes)
    //				can be sooner than event log
    //		LISTENER_RESPONSE: owner ID is corresponding LISTENER_REQUEST is, so ... (process instance id is never populated)
    //		PROCESS_INSTANCE: process instance ID is always populated. Delete along with process instance
    //		DOCUMENT: used when an argument to a LISTENER_REQUEST document is itself a document when starting a processs
    //		USER: pre-flow tasks. Process instance ID is never populated
    //		Process Launch/Designer/etc: when launching processes directly from designer/task manager
    //   so the current strategy:
    //		* delete all when process instance ID is populated.
    //		* for LISTNER_REQUEST with no process instance ID, delete based on aging parameter
    //		* delete all LISTENER_RESPONSE for which the corresponding LISTENER_REQUEST is already deleted
    //		* delete all DOCUMENT when parent document is already deleted
    //		* for TASK_INSTANCE with no process instance ID, delete them in 7 days
    //		* for USER, delete them based on aging parameters
    //		* to be done: for misc owner types, delete based on aging parameters
}
