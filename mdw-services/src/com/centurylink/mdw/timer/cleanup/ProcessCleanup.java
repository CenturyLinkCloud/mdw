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
package com.centurylink.mdw.timer.cleanup;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Types;

import com.centurylink.mdw.app.ApplicationContext;
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
 * Clean up old database entries from tables that are older than a specified amount
 * Add following to mdw.properties
 * # Scheduled job - process clean up.
 * Make sure appropriate db package is imported and Cleanup-Runtime.sql is there.
 * mdw.timer.task.ProcessCleanup.TimerClass=com.centurylink.mdw.timer.cleanup.ProcessCleanup
 * # run every 15 min
 * mdw.timer.task.ProcessCleanup.Schedule=0,15,30,45 * * * *
 * # How old process instance should be to be a candidate for deleting
 * MDWFramework.ProcessCleanup/ProcessExpirationAgeInDays=90
 * # How many process instances to be deleted in each run
 * MDWFramework.ProcessCleanup/MaximumProcessExpiration=1000
 * MDWFramework.ProcessCleanup/ExternalEventExpirationAgeInDays=90
 * MDWFramework.ProcessCleanup/CommitInterval=10000
 */

public class ProcessCleanup extends RoundRobinScheduledJob {

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

        int processExpirationDays = PropertyManager.getIntegerProperty(
                PropertyGroups.PROCESS_CLEANUP + "/ProcessExpirationAgeInDays", 0);
        int maxProcesses = PropertyManager.getIntegerProperty(
                PropertyGroups.PROCESS_CLEANUP + "/MaximumProcessExpiration", 0);
        int eventExpirationDays = PropertyManager.getIntegerProperty(
                PropertyGroups.PROCESS_CLEANUP + "/ExternalEventExpirationAgeInDays", 0);
        int commitInterval = PropertyManager
                .getIntegerProperty(PropertyGroups.PROCESS_CLEANUP + "/CommitInterval", 10000);
        String cleanupScript = PropertyManager
                .getProperty(PropertyGroups.PROCESS_CLEANUP + "/RuntimeCleanupScript");

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
        CallableStatement show_stmt = db.getConnection()
                .prepareCall("declare " + "    l_line varchar2(255); " + "    l_done number; "
                        + "    l_buffer long; " + "begin " + "  loop "
                        + "    exit when length(l_buffer)+255 > :maxbytes OR l_done = 1; "
                        + "    DBMS_OUTPUT.get_line( l_line, l_done ); "
                        + "    l_buffer := l_buffer || l_line || chr(10); " + "  end loop; "
                        + " :done := l_done; " + " :buffer := l_buffer; " + "end;");
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
    }

    private void cleanup(DatabaseAccess db, String filename, int maxProcInst,
            int processExpirationDays, int eventExpirationDays, int commitInterval, String jdbcUrl) {
        Connection conn = null;
        InputStream is = null;
        String printMsg = "";
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
                db.openConnection();
                conn = db.getConnection();
            }

            byte[] bytes = FileHelper.readFromResourceStream(is);
            String query = new String(bytes);
            query = query.replaceAll("\\r", "").trim();
            if (query.endsWith("/"))
                query = query.substring(0, query.length() - 1);

            CallableStatement callStmt = null;
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
            callStmt.execute();
            if (db != null && !db.isMySQL())
                show_output(db);

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

    public static void main(String args[]) throws Exception {
        // "jdbc:oracle:thin:mdwdev/mdwdev@mdwdevdb.dev.qintra.com:1594:mdwdev";
        String jdbcUrl = "jdbc:mariadb://localhost:3308/mdw?user=mdw&password=mdw";
        //String jdbcUrl = "jdbc:mysql://127.0.0.1:3306/mdw?user=root&password=mdw";
        ProcessCleanup me = new ProcessCleanup();
        // db , cleanupfile , maxProcInst, processExpirationDays , eventExpirationDays , commitInterval, status IN (3,4,5)
        me.cleanup(null, "Cleanup-Runtime.sql", 1, 360, 360, 2, jdbcUrl);
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
