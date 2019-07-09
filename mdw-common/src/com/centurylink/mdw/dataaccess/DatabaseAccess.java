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
package com.centurylink.mdw.dataaccess;

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.cache.impl.PackageCache;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.dataaccess.db.DocumentDb;
import com.centurylink.mdw.spring.SpringAppContext;
import com.centurylink.mdw.util.file.FileHelper;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.naming.NamingException;
import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.*;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class DatabaseAccess {

    public static final String MDW_DATA_SOURCE = "MDWDataSource";
    private static StandardLogger logger = LoggerUtil.getStandardLogger();
    private static boolean IS_TRACE = logger.isTraceEnabled();

    private String database_name; // JDBC url or a connection pool name
    private static String INTERNAL_DATA_SOURCE = null;
    private static Long db_time_diff = null;

    private static int retryMax = PropertyManager.getIntegerProperty(PropertyNames.MDW_TRANSACTION_RETRY_MAX, 3);
    private static int retryInterval = PropertyManager.getIntegerProperty(PropertyNames.MDW_TRANSACTION_RETRY_INTERVAL, 500);

    private static Map<String,DataSource> loadedDataSources = new ConcurrentHashMap<>();

    private static EmbeddedDataAccess embedded;
    public static EmbeddedDataAccess getEmbedded() { return embedded; }

    private static boolean checkUpgradePerformed;

    protected Map<String,String> connectParams;
    protected Connection connection;
    protected PreparedStatement ps;
    protected ResultSet rs;
    protected int queryTimeout;  // Clients can set the timeout if desired.  Default is no timeout.

    /**
     * Also is true for MariaDB
     */
    private boolean isMySQL;
    public boolean isMySQL() {
        return isMySQL;
    }

    public boolean isOracle() {
        return !isMySQL;
    }

    private boolean isMariaDB;
    public boolean isMariaDB() {
        return isMariaDB;
    }

    private boolean precisionSupport;
    public boolean isPrecisionSupport() {
        return precisionSupport;
    }

    private boolean isEmbedded;

    private static DocumentDb documentDb;
    public static void initDocumentDb() {
            documentDb =  SpringAppContext.getInstance().getBean(DocumentDb.class);
            boolean hasDocDb = documentDb != null && documentDb.isEnabled();
            if (hasDocDb) {
                LoggerUtil.getStandardLogger().info("Using documentDb: " + documentDb.getClass().getName() + " " + documentDb);
                documentDb.initializeDbClient();
            }
            else {
                documentDb = null;
            }
    }

    /**
     * Returns null if not enabled
     */
    public static DocumentDb getDocumentDb() {
        return documentDb;
    }

    /**
     *
     * @param database_name either a data source name
     *         or JDBC URL. It can also be null, in which
     *         case the default MDW data source is used
     */
    public DatabaseAccess(String database_name) {
        if (database_name == null) {
            if (INTERNAL_DATA_SOURCE == null) {
                INTERNAL_DATA_SOURCE = MDW_DATA_SOURCE;
            }
            this.database_name = INTERNAL_DATA_SOURCE;
        }
        else {
            this.database_name = database_name;
        }

        connection = null;
        queryTimeout = 0;  //Default - 0 means no timeout overwrite, so DBCP implementation's default.  Currently (Apache DBCP-1.4.3), no timeout on queries

        if (this.database_name.startsWith("jdbc:oracle"))
            isMySQL = false;
        else if (this.database_name.startsWith("jdbc:mysql") || this.database_name.startsWith("jdbc:mariadb"))
            isMySQL = true;

        if (this.database_name.startsWith("jdbc:mariadb")) {
            isMariaDB = true;
        }

        if (this.database_name.equals(INTERNAL_DATA_SOURCE)) {
            String dbprop = PropertyManager.getProperty(PropertyNames.MDW_DB_URL);
            isMySQL = dbprop != null && (dbprop.startsWith("jdbc:mysql") || dbprop.startsWith("jdbc:mariadb"));
            isMariaDB = dbprop != null && dbprop.startsWith("jdbc:mariadb");
            isEmbedded = dbprop != null && isMariaDB && isEmbeddedDb(dbprop);
        }

        precisionSupport = PropertyManager.getBooleanProperty(PropertyNames.MDW_DB_MICROSECOND_PRECISION, true);

        if (isEmbedded) {
            try {
                checkAndStartEmbeddedDb();
            }
            catch (SQLException ex) {
                LoggerUtil.getStandardLogger().severeException("Failed to start embedded DB: " + INTERNAL_DATA_SOURCE, ex);
            }
        }
    }

    public DatabaseAccess(String dbName, Map<String,String> connectParams) {
        this(dbName);
        this.connectParams = connectParams;
    }

    /**
     * Whether this VM instance should start up an embedded database.
     */
    private static boolean isEmbeddedDb(String jdbcUrl) {
        return (jdbcUrl.startsWith("jdbc:mariadb://localhost:") || jdbcUrl.startsWith("jdbc:mysql://localhost:"));
    }

    private static synchronized void checkAndStartEmbeddedDb() throws SQLException {
        if (embedded == null) {
            String url = PropertyManager.getProperty(PropertyNames.MDW_DB_URL);
            String user = PropertyManager.getProperty(PropertyNames.MDW_DB_USERNAME);
            String password = PropertyManager.getProperty(PropertyNames.MDW_DB_PASSWORD);
            String assetLoc = PropertyManager.getProperty(PropertyNames.MDW_ASSET_LOCATION);
            if (assetLoc == null)
                throw new SQLException("Missing property required for embedded db: " + PropertyNames.MDW_ASSET_LOCATION);
            String baseLoc = PropertyManager.getProperty(PropertyNames.MDW_EMBEDDED_DB_BASE_LOC);
            if (baseLoc == null)
                baseLoc = assetLoc + "/../data/db";
            String dataLoc = PropertyManager.getProperty(PropertyNames.MDW_EMBEDDED_DB_DATA_LOC);
            if (dataLoc == null)
                dataLoc = assetLoc + "/../data/mdw";
            embedded = new EmbeddedDataAccess();
            try {
                embedded.create(url, user, password, assetLoc, baseLoc, dataLoc);
                embedded.run();
            }
            catch (DataAccessException ex) {
                throw new SQLException(ex.getMessage(), ex);
            }
        }
    }

    public void checkAndUpgradeSchema() {
        synchronized(DatabaseAccess.class) {
            if (checkUpgradePerformed)
                return;
            StandardLogger logger = LoggerUtil.getStandardLogger();
            try {
                String upgradeJsonPath = "db/" + (isOracle() ? "oracle" : "mysql") + "/upgrade.json";
                InputStream is = FileHelper.readFile(upgradeJsonPath, DatabaseAccess.class.getClassLoader());
                if (is != null) {
                    logger.info("Check/apply db upgrades: " + upgradeJsonPath);
                    try (BufferedReader buffer = new BufferedReader(new InputStreamReader(is))) {
                        JSONObject upgradeJson = new JSONObject(buffer.lines().collect(Collectors.joining("\n")));
                        JSONArray queriesArr = upgradeJson.getJSONArray("schemaUpgradeQueries");
                        if (queriesArr != null) {
                            try (DbAccess dbAccess = new DbAccess()) {
                                for (int i = 0; i < queriesArr.length(); i++) {
                                    JSONObject queriesObj = queriesArr.getJSONObject(i);
                                    String name = queriesObj.getString("name");
                                    String checkSql = queriesObj.getString("check");
                                    ResultSet rs = dbAccess.runSelect(checkSql);
                                    if (!rs.next()) {
                                        logger.info("  db upgrade: " + name);
                                        String[] upgradeSqls = queriesObj.getString("upgrade").split(";");
                                        for (String upgradeSql : upgradeSqls)
                                            dbAccess.runUpdate(upgradeSql);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            catch (Exception ex) {
                logger.severeException("Failed to check/upgrade db: " + ex.getMessage(), ex);
            }
            finally {
                checkUpgradePerformed = true;
        }
        }
    }

    public Connection openConnection() throws SQLException {
        if (connectionIsOpen()) return connection;
        ps = null;
        rs = null;

        try {
            DataSource dataSource = ApplicationContext.getDataSourceProvider().getDataSource(database_name);

            // Only need to load driver the first time, which creates the connection factory.  All JDBC drivers except for MariaDB are provided as assets in a package
            if (loadedDataSources.get(database_name) == null || !loadedDataSources.get(database_name).equals(dataSource)) {
                List<com.centurylink.mdw.model.workflow.Package> pkgList = PackageCache.getPackages();
                ClassLoader origCL = null;
                if (pkgList != null && pkgList.size() > 0)
                    origCL = ApplicationContext.setContextCloudClassLoader(pkgList.get(0));
                connection = dataSource.getConnection();
                loadedDataSources.put(database_name, dataSource);
                ApplicationContext.resetContextClassLoader(origCL);
            }
            else
                connection = dataSource.getConnection();

        } catch (NamingException e) {
            throw new SQLException("Failed to find data source " + database_name, e);
        }

        connection.setAutoCommit(true);  // New default. Code using startTransaction/stopTransaction methods will set this to false.

        return connection;
    }

    public void commit() throws SQLException {
        if (connectionIsOpen() && !connection.getAutoCommit())
            connection.commit();
    }

    public void rollback() {
        try {
            if (connectionIsOpen() && !connection.getAutoCommit()) {
                connection.rollback();
            }
        } catch (SQLException ignored) {
        }
    }

    public void closeConnection()
    {
        closeStatement();
        closeResultSet();
        try {
            if (connection != null) {
                Connection temp = connection;
                connection = null;
//                temp.commit();    // commit at close connection
                temp.close();
            }
        } catch (Throwable ignored) {
        }
    }

    public boolean isDefaultDatabase() {
        return database_name.equals(INTERNAL_DATA_SOURCE);
    }

    public void closeStatement()
    {
        try {
            if (ps != null)
                ps.close();
        } catch (Exception ignored) {
        }
        ps = null;
    }

    public void closeResultSet() {
        try {
            if (rs != null)
                rs.close();
        } catch (Exception ignored) {
        }
        rs = null;
    }

    public boolean connectionIsOpen() {
        if (connection==null) return false;
        try {
            return !connection.isClosed();
        } catch (SQLException e) {
            connection = null;
            return false;
        }
    }

    private ResultSet logExecuteQuery(String query, Object... arguments) throws SQLException {
        if (queryTimeout > 0 && ps != null)
            ps.setQueryTimeout(queryTimeout);
        long before = System.currentTimeMillis();
        try {
            if (ps != null)
                return ps.executeQuery();
            else
                return null;
        }
        catch (SQLException ex) {
            logger.error("ERRORED SQL: " + DbAccess.substitute(query, arguments));
            throw ex;
        }
        finally {
            if (IS_TRACE) {
                long after = System.currentTimeMillis();
                logger.trace("SQL (" + (after - before) + " ms): " + DbAccess.substitute(query, arguments));
            }
        }
    }

    private int logExecuteUpdate(String query, Object... arguments) throws SQLException {
        // Only retry if autoCommit is true
        int retriesRemaining = connection.getAutoCommit() ? retryMax : 0;

        if (queryTimeout > 0 && ps != null)
            ps.setQueryTimeout(queryTimeout);

        long before = System.currentTimeMillis();

        while (retriesRemaining >= 0) {
            try {
                if (ps != null)
                    return ps.executeUpdate();
                else
                    return -1;
            }
            catch (Exception e) {
                if (retriesRemaining-- > 0) {
                    LoggerUtil.getStandardLogger().infoException("SQL Exception occured on query: " + query + "\nRetrying...\n", e);
                    try {
                        Thread.sleep(retryInterval); // short delay before retry
                    } catch (InterruptedException e1) {
                        LoggerUtil.getStandardLogger().info("Sleep was interrupted.");
                    }
                }
                else {
                    if (e instanceof SQLException) {
                        logger.error("ERRORED SQL: " + DbAccess.substitute(query, arguments));
                    }
                    throw e;  // Can't retry anymore, throw the exception
                }
            }
            finally {
                if (IS_TRACE) {
                    long after = System.currentTimeMillis();
                    logger.trace("SQL (" + (after - before) + " ms): " + DbAccess.substitute(query, arguments));
                }
            }
        }
        return -1;  // Not reachable code
    }

    private int [] logExecuteBatch(String query) throws SQLException {
        if (queryTimeout > 0 && ps != null)
            ps.setQueryTimeout(queryTimeout);
        long before = System.currentTimeMillis();
        try {
            if (ps != null)
                return ps.executeBatch();
            else
                return null;
        }
        catch (SQLException ex) {
            logger.error("ERRORED SQL: " + query);
            throw ex;
        }
        finally {
            if (IS_TRACE) {
                long after = System.currentTimeMillis();
                logger.trace("SQL (" + (after - before) + " ms): " + query);
            }
        }
    }

    public ResultSet runSelect(String query) throws SQLException {
        return runSelect(query, null);
     }

    public ResultSet runSelect(String query, Object arg)
       throws SQLException
    {
        closeStatement();
        closeResultSet();
        ps = connection.prepareStatement(query);
        if (arg!=null) setStatementArgument(1, arg);
        rs = logExecuteQuery(query, arg);
        return rs;
    }

    public ResultSet runSelect(String query, Object[] arguments)
        throws SQLException
    {
        closeStatement();
        closeResultSet();
        ps = connection.prepareStatement(query);
        if (arguments != null) {
            for (int i = 0; i < arguments.length; i++) {
                setStatementArgument(i + 1, arguments[i]);
            }
        }
        rs = logExecuteQuery(query, arguments);
        return rs;
    }

    public ResultSet runSelect(PreparedSelect select) throws SQLException {
        if (select.getMessage() == null)
            return runSelect(select.getSql(), select.getParams());
        else
            return runSelect(select.getMessage(), select.getSql(), select.getParams());
    }

    /**
     * Ordinarily db query logging is at TRACE level.  Use this method
     * to log queries at DEBUG level.
     */
    public ResultSet runSelect(String logMessage, String query, Object[] arguments) throws SQLException {
        long before = System.currentTimeMillis();
        try {
            return runSelect(query, arguments);
        }
        finally {
            if (logger.isDebugEnabled()) {
                long after = System.currentTimeMillis();
                logger.debug(logMessage + " (" + (after - before) + " ms): " + DbAccess.substitute(query, arguments));
            }
        }
    }

    public int runUpdate(String query) throws SQLException {
        return runUpdate(query, null);
    }

    public int runUpdate(String query, Object arg)
            throws SQLException {
        this.closeStatement();
        ps = connection.prepareStatement(query);
        if (arg != null)
            setStatementArgument(1, arg);
        return logExecuteUpdate(query, arg);
    }

    public int runUpdate(String query, Object[] arguments)
            throws SQLException {
        this.closeStatement();
        ps = connection.prepareStatement(query);
        if (arguments != null) {
            for (int i = 0; i < arguments.length; i++) {
                setStatementArgument(i + 1, arguments[i]);
            }
        }
        return logExecuteUpdate(query, arguments);
    }

    /**
     * Ordinarily db query logging is at TRACE level.  Use this method
     * to log queries at DEBUG level.
     */
    public int runUpdate(String logMessage, String query, Object[] arguments) throws SQLException {
        long before = System.currentTimeMillis();
        try {
            return runUpdate(query, arguments);
        }
        finally {
            if (logger.isDebugEnabled()) {
                long after = System.currentTimeMillis();
                logger.debug(logMessage + " (" + (after - before) + " ms): " + DbAccess.substitute(query, arguments));
            }
        }
    }

    public Long runInsertReturnId(String query, Object[] arguments)
        throws SQLException {
        this.closeStatement();
        ps = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
        if (arguments != null) {
            for (int i = 0; i < arguments.length; i++) {
                setStatementArgument(i + 1, arguments[i]);
            }
        }
        logExecuteUpdate(query, arguments);
        rs = ps.getGeneratedKeys();
        if (rs.next())
            return rs.getLong(1);
        else
            return null;
    }

    public void prepareStatement(String query) throws SQLException {
        this.closeStatement();
        ps = connection.prepareStatement(query);
    }

    public void addToBatch(Object [] arguments) throws SQLException {
        if (arguments != null) {
            for (int i = 0; i < arguments.length; i++) {
                setStatementArgument(i + 1, arguments[i]);
            }
        }
        ps.addBatch();
    }

    public int[] runBatchUpdate() throws SQLException {
        return logExecuteBatch("(batch)");
    }

    @Deprecated
    public ResultSet runSelectWithPreparedStatement(Object arg) throws SQLException {
        closeResultSet();
        if (arg!=null) setStatementArgument(1, arg);
        return logExecuteQuery("(prepared query)");
    }

    @Deprecated
    public int runUpdateWithPreparedStatement(Object arg) throws SQLException {
        setStatementArgument(1, arg);
        return logExecuteUpdate("(prepared update)");
    }

    @Deprecated
    public int runUpdateWithPreparedStatement(Object[] arguments) throws SQLException {
        if (arguments!=null) {
            for (int i=0; i<arguments.length; i++) {
                setStatementArgument(i+1, arguments[i]);
            }
        }
        return logExecuteUpdate("(prepared update)");
    }

    private void setStatementArgument(int i, Object value)
            throws SQLException  {
        if (value == null) ps.setString(i, isMySQL?null:"");
        else if (value instanceof String) ps.setString(i, (String)value);
        else if (value instanceof Integer) ps.setInt(i, ((Integer)value).intValue());
        else if (value instanceof Long) ps.setLong(i, ((Long)value).longValue());
        else if (value instanceof java.sql.Date) ps.setDate(i, (java.sql.Date)value);
        else if (value instanceof Date) ps.setTimestamp(i, createDate((Date)value));
        else if (value instanceof Clob) ps.setClob(i, (Clob)value);
        else if (value instanceof Blob) ps.setBlob(i, (Blob)value);
        else ps.setObject(i, value);
    }

    private Timestamp createDate(Date date) {
        return new Timestamp(date.getTime());
    }

    public Connection getConnection() {
        return connection;
    }

    /**
     * This should be only used by CommonDataAccess.startTransaction()
     * @param conn connection
     */
    public void setConnection(Connection conn) {
        connection = conn;
    }

    public long getDatabaseTime() throws SQLException {
        if (db_time_diff==null) {
            String query = isMySQL?"select now()":"select sysdate from dual";
            Timestamp ts;
            if (connection==null) {
                openConnection();
                ResultSet rs = runSelect(query);
                rs.next();
                ts = rs.getTimestamp(1);
                closeConnection();
            } else {
                ResultSet rs = runSelect(query);
                rs.next();
                ts = rs.getTimestamp(1);
            }
            long raw_diff = ts.getTime()-System.currentTimeMillis();
            long r = (raw_diff + 30000) % 1800000; // Remaining millisecs after dividing by 1/2 hour
            long q = (raw_diff + 30000) / 1800000; // quotient after dividing by 1/2 hour
             // ignore remainder if the difference millisec is less than a minute and return nearest half-hour
            db_time_diff = (Math.abs(r) < 60000) ? (q * 1800000) : raw_diff;
            System.out.println("Database time difference: " + db_time_diff/1000.0 + " seconds (raw diff=" + raw_diff + ")");
        }
        return System.currentTimeMillis() + db_time_diff;
    }

    public static long getCurrentTime() {
        return System.currentTimeMillis() + db_time_diff;
    }
    /**
     * The current database Date/Time.  If db_time_diff is not known (eg Designer), server time is returned.
     */
    public static Date getDbDate() {
        return db_time_diff == null ? new Date() : new Date(getCurrentTime());
    }

    public static long getDbTimeDiff() {
        return db_time_diff == null ? 0 : db_time_diff;
    }

    public String pagingQueryPrefix() {
        if (isMySQL)
            return "";
        else
            return "select * from ( select allrows.*, rownum rnum from (\n";
    }

    /**
     *
     * @param startRow the first row to display, index starting from 0
     * @param rowCount number of roes
     * @return String
     */
    public String pagingQuerySuffix(int startRow, int rowCount) {
        if (isMySQL)
            return " limit " + startRow + ", " + rowCount;
        else
            return "\n) allrows where rownum <= " + (startRow+rowCount) + ") where rnum  > " + startRow;
    }

    public String toString() {
        return database_name;
    }
}
