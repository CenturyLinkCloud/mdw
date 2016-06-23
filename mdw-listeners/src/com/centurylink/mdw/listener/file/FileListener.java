/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.listener.file;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

import javax.naming.NamingException;
import javax.sql.DataSource;

import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.common.utilities.file.WildcardFilenameFilter;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.model.listener.ListenerException;

public abstract class FileListener {

    private static StandardLogger _logger = LoggerUtil.getStandardLogger();

    private String _name;
    public String getName() { return _name; }
    public void setName(String name) { _name = name; }

    private FileListenerTimerTask _timerTask;
    private File _directory;
    private String _filenamePattern;

    public abstract void reactToFile(File file);

    public void listen(Properties props) throws ListenerException {
        _directory = new File(props.getProperty("Directory"));
        if (!_directory.exists() || !_directory.isDirectory())
            throw new ListenerException("Directory does not exist: " + _directory);

        long period = Integer.parseInt(props.getProperty("IntervalMinutes")) * 60 * 1000;

        String delayMinutes = props.getProperty("DelayMinutes");
        if (delayMinutes == null)
            delayMinutes = "0";
        long delay = Integer.parseInt(delayMinutes) * 60 * 1000;

        _filenamePattern = props.getProperty("FilenamePattern");
        if (_filenamePattern == null)
            _filenamePattern = "*";

        _timerTask = new FileListenerTimerTask();

        Timer timer = new Timer();
        timer.scheduleAtFixedRate(_timerTask, delay, period);
    }

    public void stopListening() {
        _timerTask.cancel();
    }

    class FileListenerTimerTask extends TimerTask {
        /**
         * Reads the specified directory, calling processFile() for
         * each file matching the filename pattern.
         */
        public void run() {
            File[] files = _directory.listFiles(new WildcardFilenameFilter(_filenamePattern));
            for (File file : files) {
                try {
                    processFile(file);
                }
                catch (Exception ex) {
                    _logger.severeException(ex.getMessage(), ex);
                }
            }

        }

        /**
         * Locks the event_log records and checks whether an entry exists, indicating
         * that the file has already been processed.  If no entry exists, insert an entry
         * and release the lock.  Call the reactToFile() method and update the event_log
         * record to status_cd 4 for completed.
         *
         * @param file
         * @return
         */
        synchronized void processFile(File file) throws SQLException, NamingException {

            final int IN_PROGRESS = 2;
            final int FINISHED = 4;

            DataSource ds = ApplicationContext.getMdwDataSource();
            Connection conn = ds.getConnection();

            try {
                conn.setAutoCommit(false);
                Statement stmt = conn.createStatement();
                stmt.executeQuery(getSelectSql(file, true));
                ResultSet rs = stmt.executeQuery(getSelectSql(file, false));
                if (!rs.next()) {
                  stmt.executeUpdate(getInsertSql(file, IN_PROGRESS));
                  conn.commit();
                  reactToFile(file);
                  stmt.executeUpdate(getUpdateSql(file, FINISHED));
                  conn.commit();
                }
                else {
                  _logger.info("File listener " + _name + " ignoring file: " + file + " (already processed)");
                  conn.rollback();
                }
            }
            finally {
                conn.close();
            }
        }

        private String getSelectSql(File file, boolean lock) {
          String sql = "select * from EVENT_LOG \n"
            + "where EVENT_NAME = 'File Processed' \n"
            + "and EVENT_LOG_OWNER = '" + _name + "' \n";
          if (lock)
            sql += "for update \n";
          else
            sql += "and EVENT_SOURCE = '" + file + "' \n";
          return sql;
        }

        private String getUpdateSql(File file, int statusCode) {
            String sql = "update EVENT_LOG \n"
              + "set STATUS_CD = " + statusCode + " \n"
              + "where EVENT_NAME = 'File Processed' \n"
              + "and EVENT_LOG_OWNER = '" + _name + "' \n"
              + "and EVENT_SOURCE = '" + file + " '\n";
            return sql;
        }

        private String getInsertSql(File file, int statusCode) {
            String insertSql = "insert into EVENT_LOG \n"
              + "(EVENT_LOG_ID, EVENT_NAME, EVENT_LOG_OWNER, EVENT_LOG_OWNER_ID, EVENT_SOURCE, EVENT_CATEGORY, STATUS_CD) \n"
              + "values (EVENT_LOG_ID_SEQ.NEXTVAL, 'File Processed', '" + _name + "', " + System.currentTimeMillis() + ", '" + file + "', 'File', " + statusCode + ") \n";
            return insertSql;
        }

    }
}
