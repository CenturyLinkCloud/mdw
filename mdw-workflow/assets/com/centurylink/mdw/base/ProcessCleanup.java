/*
 * Copyright (C) 2017 CenturyLink, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.centurylink.mdw.base;

import com.centurylink.mdw.annotations.ScheduledJob;
import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.config.PropertyException;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.model.workflow.WorkStatus;
import com.centurylink.mdw.services.workflow.RoundRobinScheduledJob;
import com.centurylink.mdw.timer.cleanup.ScriptRunner;
import com.centurylink.mdw.util.CallURL;
import com.centurylink.mdw.util.file.FileHelper;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

import java.io.*;
import java.sql.*;
import java.util.Properties;

/**
 * This script cleans up old database entries from tables that are older than a specified time
 * Make sure appropriate db package is imported and Cleanup-Runtime.sql is there.
 * Add following to mdw.yaml
 timer.task:
 ProcessCleanup: # run every 15 min
 TimerClass: com.centurylink.mdw.timer.cleanup.ProcessCleanup
 Schedule: 0,15,30,45 * * * *    # to run daily at 2:30 am use : Schedule: 30 2 * * ? *
 RuntimeCleanupScript: Cleanup-Runtime.sql
 ProcessExpirationAgeInDays: 225 #How old process instance should be to be a candidate for deleting
 MaximumProcessExpiration: 1  #How many process instances to be deleted in each run
 ExternalEventExpirationAgeInDays: 225
 CommitInterval: 1000
 * if you need to make change in above properties then first delete the db entry by identifying the row using
 * this sql:  select * from event_instance where event_name like '%ScheduledJob%'
 * Then re-start the server/instance for new clean-up properties to be effective.
 */
//Moved this class to base package to enable the process clean up to delete the data from DB
//added the @scheduledjob to enable it by default
@ScheduledJob(value="ProcessCleanup", schedule="${props['mdw.cleanup.job.cleanupscheduler']}", enabled="${props['mdw.cleanup.job.enabled']}", defaultEnabled= false, isExclusive=true)
public class ProcessCleanup extends RoundRobinScheduledJob implements com.centurylink.mdw.model.monitor.ScheduledJob  {

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
        logger.info("running process cleanup now "+System.currentTimeMillis());
        logger.info("methodEntry-->ProcessCleanup.run()");
        int processExpirationDays = 0;
        int maxProcesses = 0;
        int eventExpirationDays = 0;
        int commitInterval = 10000;
        String cleanupScript = null;

        if (PropertyManager.isYaml()) {
            try {
                Properties cleanupTaskProperties = PropertyManager.getInstance().getProperties(PropertyNames.MDW_TIMER_TASK);
                if (cleanupTaskProperties !=null && cleanupTaskProperties.size() != 0) {
                    processExpirationDays = Integer.parseInt(cleanupTaskProperties.getProperty(PropertyNames.MDW_TIMER_TASK + ".ProcessCleanup.ProcessExpirationAgeInDays", "180"));
                    maxProcesses = Integer.parseInt(cleanupTaskProperties.getProperty(PropertyNames.MDW_TIMER_TASK + ".ProcessCleanup.MaximumProcessExpiration", "0"));
                    eventExpirationDays = Integer.parseInt(cleanupTaskProperties.getProperty(PropertyNames.MDW_TIMER_TASK + ".ProcessCleanup.ExternalEventExpirationAgeInDays", "180"));
                    commitInterval = Integer.parseInt(cleanupTaskProperties.getProperty(PropertyNames.MDW_TIMER_TASK + ".ProcessCleanup.CommitInterval", "10000"));
                    cleanupScript = cleanupTaskProperties.getProperty(PropertyNames.MDW_TIMER_TASK + ".ProcessCleanup.RuntimeCleanupScript", "Cleanup-Runtime.sql");
                } else {
                    processExpirationDays = PropertyManager.getIntegerProperty("mdw.cleanup.job.ProcessExpirationAgeInDays", 180);
                    maxProcesses = PropertyManager.getIntegerProperty("mdw.cleanup.job.maxProcesses", 0);
                    eventExpirationDays = PropertyManager.getIntegerProperty("mdw.cleanup.job.eventExpirationDays", 180);
                    commitInterval = PropertyManager.getIntegerProperty("mdw.cleanup.job.commitInterval", 10000);
                    cleanupScript = PropertyManager.getProperty("mdw.cleanup.job.RuntimeCleanupScript","Cleanup-Runtime.sql" );
                }
            } catch (PropertyException e) {
                logger.info("ProcessCleanup.run() : Properties not found" + e.getMessage());
            }
        } else {
            processExpirationDays = PropertyManager.getIntegerProperty(
                    PropertyNames.PROCESS_CLEANUP + "/ProcessExpirationAgeInDays", 180);
            maxProcesses = PropertyManager.getIntegerProperty(
                    PropertyNames.PROCESS_CLEANUP + "/MaximumProcessExpiration", 0);
            eventExpirationDays = PropertyManager.getIntegerProperty(
                    PropertyNames.PROCESS_CLEANUP + "/ExternalEventExpirationAgeInDays", 180);
            commitInterval = PropertyManager
                    .getIntegerProperty(PropertyNames.PROCESS_CLEANUP + "/CommitInterval", 10000);
            cleanupScript = PropertyManager
                    .getProperty(PropertyNames.PROCESS_CLEANUP + "/RuntimeCleanupScript");
        }
        if (cleanupScript == null) {
            cleanupScript = "Cleanup-Runtime.sql";
        }

        DatabaseAccess db = new DatabaseAccess(null);
        cleanup(db, cleanupScript, maxProcesses, processExpirationDays, eventExpirationDays,
                commitInterval, null);

        logger.info("methodExit-->ProcessCleanup.run()");
    }

    private void enable_output(DatabaseAccess db, int bufsize) throws SQLException {
        String query = "begin dbms_output.enable(" + bufsize + "); end;";
        try (CallableStatement callStmt = db.getConnection().prepareCall(query)) {
            callStmt.executeUpdate();
        }catch (SQLException e){}
    }

    @SuppressWarnings("unused")
    private void disable_output(DatabaseAccess db) throws SQLException {
        String query = "begin dbms_output.disable; end;";
        try (CallableStatement callStmt = db.getConnection().prepareCall(query)) {
            callStmt.executeUpdate();
        }catch (SQLException e){}
    }

    private void show_output(DatabaseAccess db) throws SQLException {
        try (
                CallableStatement show_stmt = db.getConnection()
                        .prepareCall("declare " + "    l_line varchar2(255); " + "    l_done number; "
                                + "    l_buffer long; " + "begin " + "  loop "
                                + "    exit when length(l_buffer)+255 > :maxbytes OR l_done = 1; "
                                + "    DBMS_OUTPUT.get_line( l_line, l_done ); "
                                + "    l_buffer := l_buffer || l_line || chr(10); " + "  end loop; "
                                + " :done := l_done; " + " :buffer := l_buffer; " + "end;")){
            int done = 0;
            int maxbytes = 4096; // retrieve up to 4096 bytes at a time
            show_stmt.registerOutParameter(2, Types.INTEGER);
            show_stmt.registerOutParameter(3, Types.VARCHAR);
            for (;;) {
                show_stmt.setInt(1, maxbytes);
                show_stmt.executeUpdate();
                if (null == logger) {
                    System.out.println(show_stmt.getString(3));
                }
                else {
                    logger.info(show_stmt.getString(3));
                }
                done = show_stmt.getInt(2);
                if (done == 1)
                    break;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void cleanup(DatabaseAccess db, String filename, int maxProcInst,
            int processExpirationDays, int eventExpirationDays, int commitInterval, String jdbcUrl) {
        Connection conn = null;
        InputStream is = null;
        String printMsg = "";
        CallableStatement callStmt = null;
        ResultSet rs = null;
        try {
            File cleanupScript = null;
            File assetRoot = ApplicationContext.getAssetRoot();

            if (jdbcUrl != null || db.isMySQL()) {
                if (assetRoot != null)
                    cleanupScript = new File(assetRoot + "/com/centurylink/mdw/mysql/" + filename);
                if (cleanupScript.exists())
                    printMsg = "Located MySQL Cleanup Script file: " + cleanupScript.getAbsolutePath();
                else
                    printMsg = "Unable to locate MySQL cleanup Script file: Make sure com.centurylink.mdw.mysql package has Cleanup-Runtime.sql";
            }
            else {  //Assume Oracle DB by default
                if (assetRoot != null)
                    cleanupScript = new File(assetRoot + "/com/centurylink/mdw/oracle/" + filename);
                if (cleanupScript.exists())
                    printMsg = "Located Oracle cleanup Script file: " + cleanupScript.getAbsolutePath();
                else
                    printMsg = "Unable to locate Oracle Cleanup Script file: Make sure com.centurylink.mdw.oracle package has Cleanup-Runtime.sql";
            }
            if (null == logger)
                System.out.println(printMsg);
            else
                logger.info(printMsg);

            is = new FileInputStream(cleanupScript);
            if (jdbcUrl != null) {  //added this logic to do unit testing db connection is not longer possible with DatabaseAccess using jdbcUrl
                try {
                    Class.forName("com.mysql.jdbc.Driver");
                    conn = DriverManager.getConnection(jdbcUrl);
                } catch (ClassNotFoundException e) {
                    printMsg = "Unable to get mysql driver: " + e;
                    if (null == logger)
                        System.out.println(printMsg);
                    else
                        logger.info(printMsg);
                    if (is != null)
                        is.close();
                    return;
                }
            }
            else {
                conn = db.openConnection();
                conn.setAutoCommit(false);
            }

            byte[] bytes = FileHelper.readFromResourceStream(is);
            String query = new String(bytes);
            query = query.replaceAll("\\r", "").trim();
            if (query.endsWith("/"))
                query = query.substring(0, query.length() - 1);


            if (jdbcUrl != null || db.isMySQL()) {
                ScriptRunner runner = new ScriptRunner(conn, false, false);
                String filePath = cleanupScript.getAbsolutePath().replace("\\","\\\\");
                runner.runScript(new BufferedReader(new FileReader(filePath)));
                callStmt = conn.prepareCall("{call mysql_cleanup(?,?,?,?,?,?)}");
            }
            else if (db.isOracle()) {
                callStmt = db.getConnection().prepareCall(query);
                enable_output(db, 16384);
            }
            callStmt.setInt(1, maxProcInst); // max proc instances to delete
            callStmt.setInt(2, processExpirationDays); // number of days for expiration for process and related
            callStmt.setInt(3, eventExpirationDays); // number of days for expiration for events
            callStmt.setInt(4, 0); // process ID; 0 indicates any process
            callStmt.setString(5,
                    WorkStatus.STATUS_FAILED.toString() + ","
                            + WorkStatus.STATUS_COMPLETED.toString() + ","
                            + WorkStatus.STATUS_CANCELLED.toString()); // process instance statuses
            callStmt.setInt(6, commitInterval);
            boolean isResultSet = callStmt.execute();
            if (db != null && !db.isMySQL())
                show_output(db);
            else if (db != null && db.isMySQL()) {

                int updateCount = -1;
                printMsg = "";
                if (isResultSet) {
                    rs = callStmt.getResultSet();
                    printMsg = "DB Cleanup Script Output:";
                }
                else {
                    updateCount = callStmt.getUpdateCount();
                    printMsg = "DB Cleanup Script Output is missing";
                }
                if (null == logger)
                    System.out.println(printMsg);
                else
                    logger.info(printMsg);
                while (isResultSet || updateCount >= 0) {
                    if (rs != null) {
                        int i = 1;  // Column index
                        while (rs.next()) {
                            try {
                                while (true) {
                                    printMsg = rs.getObject(i).toString();
                                    if (i != 1)
                                        printMsg = " | " + printMsg;
                                    if (null == logger)
                                        System.out.println(printMsg);
                                    else
                                        logger.info(printMsg);
                                    i++;
                                    if(rs.isAfterLast())
                                        break;
                                }
                            } catch (SQLException e) {/* Not that many columns in row */ }
                        }
                        rs.close();
                    }
                    else if (updateCount >=0) {
                        if (null == logger)
                            System.out.println("Update count: " + updateCount);
                        else
                            logger.info("Update count: " + updateCount);
                    }
                    isResultSet = callStmt.getMoreResults();
                    rs = isResultSet ? callStmt.getResultSet() : null;
                    updateCount = isResultSet ? -1 : callStmt.getUpdateCount();

                }
            }
        }
        catch (Exception e) {
            printMsg = e.getMessage();
            if (null == logger)
                System.out.println(printMsg);
            else
                logger.severeException(printMsg, e);
            e.printStackTrace();
        }
        finally {
            if (callStmt != null)
                try{
                    callStmt.close();
                }catch (SQLException sqle){
                }
            if (rs != null)
                try{
                    rs.close();
                }catch (SQLException sqle){
                }
            if (db != null)
                db.closeConnection();
            if (is != null)
                try {
                    is.close();
                }

                catch (IOException e) {
                    System.out.println(e.getMessage());
                }
        }
    }

    // TODO misc things for runtime data clean ups
    // - clean-up PL/SQL script does not work for non-Oracle DBMS
    // - processes that are not completed but parent processes are completed -
    // what to do?
    // - attachment table is not cleaned - is it used???
    // - how to clean up data for process instances that are really old but not
    // completed?
    // - clean TASK_INST_GRP_MAPP and TASK_INST_INDEX need to be excluded for
    // MDW 5.0
    // - clean task instances that are not associated with process instances
    // - document table is difficult due to its mixed usage. The following is
    // current implementation
    // based on owner type:
    // VARIABLE_INSTANCE: process instance ID is always populated. Delete along
    // with process instance
    // Note: previously there are cases when process instance ID is 0 (and
    // variable instance id is 0 as well)
    // one place in RegressionTestEventHandler, when data is to be passed to a
    // document variable
    // this is fixed on 3/26/2011 by changing to DOCUMENT as owner. Keep an eye
    // to see if there are other cases.
    // ADAPTOR_REQUEST: process instance ID is always populated. Delete along
    // with process instance
    // ADAPTOR_RESPONSE: process instance ID is always populated. Delete along
    // with process instance
    // TASK_INSTANCE:
    // a. when task manager notifies engine for classic tasks (ActionRequest) -
    // process
    // instance ID is always populated.
    // b. (local) general tasks send message to engine. process instance id
    // never populated
    // INTERNAL_EVENT:
    // a. when launching process directly and some argument is to be bound to
    // document variable
    // (same usage as DOCUMENT owner, so changed to use DOCUMENT on 3/26/2011)
    // b. signal (PublishEventMessage) - process instance ID is populated
    // LISTENER_REQUEST: process instance ID is *NOT* always populated. Need to
    // investigate
    // seems can delete based on age (mostly for requests to event handler not
    // related to processes)
    // can be sooner than event log
    // LISTENER_RESPONSE: owner ID is corresponding LISTENER_REQUEST is, so ...
    // (process instance id is never populated)
    // PROCESS_INSTANCE: process instance ID is always populated. Delete along
    // with process instance
    // DOCUMENT: used when an argument to a LISTENER_REQUEST document is itself
    // a document when starting a processs
    // USER: pre-flow tasks. Process instance ID is never populated
    // Process Launch/Designer/etc: when launching processes directly from
    // designer/task manager
    // so the current strategy:
    // * delete all when process instance ID is populated.
    // * for LISTNER_REQUEST with no process instance ID, delete based on aging
    // parameter
    // * delete all LISTENER_RESPONSE for which the corresponding
    // LISTENER_REQUEST is already deleted
    // * delete all DOCUMENT when parent document is already deleted
    // * for TASK_INSTANCE with no process instance ID, delete them in 7 days
    // * for USER, delete them based on aging parameters
    // * to be done: for misc owner types, delete based on aging parameters
}
