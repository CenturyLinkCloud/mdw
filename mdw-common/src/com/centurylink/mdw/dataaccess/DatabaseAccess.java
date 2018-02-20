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

import java.io.Closeable;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.naming.NamingException;
import javax.sql.DataSource;

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.cache.impl.PackageCache;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.ApplicationConstants;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.util.StringHelper;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;

public class DatabaseAccess implements Closeable {

    private String database_name; // JDBC url or a connection pool name
    private static String INTERNAL_DATA_SOURCE = null;
    private static Long db_time_diff = null;

    private static int retryMax = PropertyManager.getIntegerProperty(PropertyNames.MDW_TRANSACTION_RETRY_MAX, 3);
    private static int retryInterval = PropertyManager.getIntegerProperty(PropertyNames.MDW_TRANSACTION_RETRY_INTERVAL, 500);

    private static Map<String, DataSource> loadedDataSources = new ConcurrentHashMap<String, DataSource>();
    private static Map<String, Boolean> collectionDocIdIndexed = new ConcurrentHashMap<String, Boolean>();

    private static EmbeddedDataAccess embedded;
    public static EmbeddedDataAccess getEmbedded() { return embedded; }

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

    private static boolean isMongoDB = false;
    private static MongoClient mongoClient;
    public static MongoDatabase getMongoDb() {
        if (mongoClient == null)
            return null;
        else
            return mongoClient.getDatabase("mdw");
    }

    public static boolean checkForDocIdIndex(String collectionName) {
        if (collectionDocIdIndexed.get(collectionName) != null)
            return ((Boolean)collectionDocIdIndexed.get(collectionName)).booleanValue();
        return false;
    }

    /**
     *
     * @param database_name either a data source name
     *         or JDBC URL. It can also be null, in which
     *         case the default MDW data source is used
     * @param context EJB context. When it is not null,
     *         transaction is handled by the EJB, which must
     *         be configured with container managed transactions
     */
    public DatabaseAccess(String database_name) {
        if (database_name == null) {
            if (INTERNAL_DATA_SOURCE == null) {
                INTERNAL_DATA_SOURCE = ApplicationConstants.MDW_FRAMEWORK_DATA_SOURCE_NAME;
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

        if ("true".equalsIgnoreCase(PropertyManager.getProperty(PropertyNames.MDW_DB_MICROSECOND_PRECISION)))
            precisionSupport = true;
        else
            precisionSupport = false;

        if (isEmbedded) {
            try {
                checkAndStartEmbeddedDb();
            }
            catch (SQLException ex) {
                LoggerUtil.getStandardLogger().severeException("Failed to start embedded DB: " + INTERNAL_DATA_SOURCE, ex);
            }
        }

        if (!StringHelper.isEmpty(PropertyManager.getProperty(PropertyNames.MDW_MONGODB_HOST)))
            isMongoDB = true;

        if (isMongoDB && mongoClient == null)  // Check and open client for MongoDB
            openMongoDbClient();
    }

    public DatabaseAccess(String dbName, Map<String,String> connectParams) {
        this(dbName);
        this.connectParams = connectParams;
    }

    /**
     * Whether this VM instance should start up an embedded database.
     */
    private static boolean isEmbeddedDb(String jdbcUrl) {
        String dbprop = PropertyManager.getProperty(PropertyNames.MDW_DB_EMBEDDED_HOST_PORT);
        return (jdbcUrl.contains("localhost") || (dbprop != null && jdbcUrl.contains(ApplicationContext.getServerHost()) && ApplicationContext.getServer().toString().equalsIgnoreCase(dbprop)));
    }

    private static synchronized void checkAndStartEmbeddedDb() throws SQLException {
        if (embedded == null) {
            String url = PropertyManager.getProperty(PropertyNames.MDW_DB_URL);
            String user = PropertyManager.getProperty(PropertyNames.MDW_DB_USERNAME);
            String password = PropertyManager.getProperty(PropertyNames.MDW_DB_PASSWORD);
            String assetLoc = PropertyManager.getProperty(PropertyNames.MDW_ASSET_LOCATION);
            if (assetLoc == null)
                throw new SQLException("Missing property required for embedded db: " + PropertyNames.MDW_ASSET_LOCATION);
            String baseLoc = PropertyManager.getProperty(PropertyNames.MDW_DB_BASE_LOC);
            if (baseLoc == null)
                baseLoc = assetLoc + "/../data/db";
            String dataLoc = PropertyManager.getProperty(PropertyNames.MDW_DB_DATA_LOC);
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

    public Connection openConnection() throws SQLException {
        if (connectionIsOpen()) return connection;
        ps = null;
        rs = null;

        try {
            DataSource dataSource = ApplicationContext.getDataSourceProvider().getDataSource(database_name);

            // Only need to load driver the first time, which creates the connection factory.  All JDBC drivers are provided as assets in a package
            if (loadedDataSources.get(database_name) == null || !loadedDataSources.get(database_name).equals(dataSource)) {
                List<com.centurylink.mdw.model.workflow.Package> pkgList = PackageCache.getPackages();
                if (pkgList == null || pkgList.size() <= 0)
                    throw new SQLException("MDW asset package containing JDBC driver is required");

                ClassLoader origCL = ApplicationContext.setContextCloudClassLoader(pkgList.get(0));
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

    private static synchronized void openMongoDbClient() {
        if (mongoClient == null) {
            String mongoHost = PropertyManager.getProperty(PropertyNames.MDW_MONGODB_HOST);
            int mongoPort = PropertyManager.getIntegerProperty(PropertyNames.MDW_MONGODB_PORT, 27017);
            int maxConnections = PropertyManager.getIntegerProperty(PropertyNames.MDW_MONGODB_POOLSIZE, PropertyManager.getIntegerProperty(PropertyNames.MDW_DB_POOLSIZE, 100));

            MongoClientOptions.Builder options = MongoClientOptions.builder();
            options.socketKeepAlive(true);
            if (maxConnections > 100)  // MongoClient default is 100 max connections per host
                options.connectionsPerHost(maxConnections);

            mongoClient = new MongoClient(new ServerAddress(mongoHost, mongoPort), options.build());

            for (String name : mongoClient.getDatabase("mdw").listCollectionNames()) {
                createMongoDocIdIndex(name);
            }
            LoggerUtil.getStandardLogger().info(mongoClient.getMongoClientOptions().toString());
        }
    }

    public static void createMongoDocIdIndex(String collectionName) {
        IndexOptions indexOptions = new IndexOptions().unique(true).background(true);
        MongoCollection<org.bson.Document> collection = mongoClient.getDatabase("mdw").getCollection(collectionName);
        String indexName = collection.createIndex(Indexes.ascending("document_id"), indexOptions);
        LoggerUtil.getStandardLogger().mdwDebug("Created Index : " + indexName + " on collection : " + collectionName);
        collectionDocIdIndexed.putIfAbsent(collectionName, Boolean.valueOf(true));
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
        } catch (SQLException e) {
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
        } catch (Throwable e) {
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
        } catch (Exception e) {
        }
        ps = null;
    }

    public void closeResultSet() {
        try {
            if (rs != null)
                rs.close();
        } catch (Exception e) {
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

    private ResultSet logExecuteQuery(String query) throws SQLException {
        if (queryTimeout > 0 && ps != null)
            ps.setQueryTimeout(queryTimeout);
        if (connection instanceof QueryLogger)
            return ((QueryLogger)connection).executeQuery(ps, query);
        else return ps.executeQuery();
    }

    private int logExecuteUpdate(String query) throws SQLException {
        // Only retry if autoCommit is true
        int retriesRemaining = connection.getAutoCommit() ? retryMax : 0;

        if (queryTimeout > 0 && ps != null)
            ps.setQueryTimeout(queryTimeout);

        while (retriesRemaining >= 0) {
            try {
                if (connection instanceof QueryLogger)
                    return ((QueryLogger)connection).executeUpdate(ps, query);
                else
                    return ps.executeUpdate();
            } catch (Exception e) {
                if (retriesRemaining-- > 0) {
                    LoggerUtil.getStandardLogger().infoException("SQL Exception occured on query: " + query + "\nRetrying...\n", e);
                    try {
                        Thread.sleep(retryInterval); // short delay before retry
                    } catch (InterruptedException e1) {
                        LoggerUtil.getStandardLogger().info("Sleep was interrupted.");
                    }
                }
                else
                    throw e;  // Can't retry anymore, throw the exception
            }
        }
        return -1;  // Not reachable code
    }

    private int [] logExecuteBatch(String query) throws SQLException {
        if (queryTimeout > 0 && ps != null)
            ps.setQueryTimeout(queryTimeout);
        if (connection instanceof QueryLogger)
            return ((QueryLogger)connection).executeBatch(ps, query);
        else return ps.executeBatch();
    }

    public ResultSet runSelect(String query, Object arg)
       throws SQLException
    {
        closeStatement();
        closeResultSet();
        ps = connection.prepareStatement(query);
        if (arg!=null) setStatementArgument(1, arg);
        rs = logExecuteQuery(query);
        return rs;
    }

    public ResultSet runSelect(String query, Object[] arguments)
        throws SQLException
    {
        closeStatement();
        closeResultSet();
        ps = connection.prepareStatement(query);
        if (arguments!=null) {
            for (int i=0; i<arguments.length; i++) {
                setStatementArgument(i+1, arguments[i]);
            }
        }
        rs = logExecuteQuery(query);
        return rs;
    }

    public int runUpdate(String query, Object arg)
            throws SQLException {
        this.closeStatement();
        ps = connection.prepareStatement(query);
        if (arg!=null) setStatementArgument(1, arg);
        return logExecuteUpdate(query);
    }

    public int runUpdate(String query, Object[] arguments)
            throws SQLException {
        this.closeStatement();
        ps = connection.prepareStatement(query);
        if (arguments!=null) {
            for (int i=0; i<arguments.length; i++) {
                setStatementArgument(i+1, arguments[i]);
            }
        }
        return logExecuteUpdate(query);
    }

    public Long runInsertReturnId(String query, Object[] arguments)
        throws SQLException {
        this.closeStatement();
        ps = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
        if (arguments!=null) {
            for (int i=0; i<arguments.length; i++) {
                setStatementArgument(i+1, arguments[i]);
            }
        }
        logExecuteUpdate(query);
        rs = ps.getGeneratedKeys();
        if (rs.next()) return rs.getLong(1);
//        else throw new SQLException("Failed to obtain generated key");
        else return null;
    }

    public void prepareStatement(String query) throws SQLException {
        this.closeStatement();
        ps = connection.prepareStatement(query);
    }

    public void addToBatch(Object [] arguments) throws SQLException {
        if (arguments!=null) {
            for (int i=0; i<arguments.length; i++) {
                setStatementArgument(i+1, arguments[i]);
            }
        }
        ps.addBatch();
    }

    public int[] runBatchUpdate() throws SQLException {
        return logExecuteBatch("(batch)");
    }

    public ResultSet runSelectWithPreparedStatement(Object arg) throws SQLException {
        closeResultSet();
        if (arg!=null) setStatementArgument(1, arg);
        return logExecuteQuery("(prepared query)");
    }

    public int runUpdateWithPreparedStatement(Object arg) throws SQLException {
        setStatementArgument(1, arg);
        return logExecuteUpdate("(prepared update)");
    }

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
     * @param conn
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
                ResultSet rs = runSelect(query, null);
                rs.next();
                ts = rs.getTimestamp(1);
                closeConnection();
            } else {
                ResultSet rs = runSelect(query, null);
                rs.next();
                ts = rs.getTimestamp(1);
            }
            long raw_diff = ts.getTime()-System.currentTimeMillis();
            long r = (raw_diff + 30000) % 1800000; // Remaining millisecs after dividing by 1/2 hour
            long q = (raw_diff + 30000) / 1800000; // quotient after dividing by 1/2 hour
             // ignore remainder if the difference millisec is less than a minute and return nearest half-hour
            db_time_diff = new Long(Math.abs(r) < 60000 ? q*1800000 : raw_diff);
            System.out.println("Database time difference: " + db_time_diff/1000.0 + " seconds (raw diff=" + raw_diff + ")");
        }
        return System.currentTimeMillis()+db_time_diff.longValue();
    }

    public static long getCurrentTime() {
        return System.currentTimeMillis()+db_time_diff.longValue();
    }
    /**
     * The current database Date/Time.  If db_time_diff is not known (eg Designer), server time is returned.
     */
    public static Date getDbDate() {
        return db_time_diff == null ? new Date() : new Date(getCurrentTime());
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
     * @param rowCount
     * @return
     */
    public String pagingQuerySuffix(int startRow, int rowCount) {
        if (isMySQL)
            return " limit " + startRow + ", " + rowCount;
        else
            return "\n) allrows where rownum <= " + (startRow+rowCount) + ") where rnum  > " + startRow;
    }

    public void setQueryTimeout(int seconds) throws SQLException {
        queryTimeout = seconds;
    }

    public int getQueryTimeout() throws SQLException {
        return queryTimeout;
    }

    public String toString() {
        return database_name;
    }

    /**
     * MongoDB doesn't allow keys to have dots (.) or to start with $.  This method encodes such keys if found
     * and returns a new BSON document
     */
    public static org.bson.Document encodeMongoDoc(org.bson.Document doc) {
        org.bson.Document newDoc = new org.bson.Document();
        for (String key : doc.keySet()) {
            Object value = doc.get(key);
            if (value instanceof org.bson.Document)
                value = encodeMongoDoc(doc.get(key, org.bson.Document.class));
            else if (value instanceof List<?>) {
                for (int i=0; i < ((List<?>)value).size(); i++) {
                    Object obj = ((List<?>)value).get(i);
                    if (obj instanceof org.bson.Document)
                        ((List<org.bson.Document>)value).set(i, encodeMongoDoc((org.bson.Document)obj));
                }
            }

            String newKey = key;
            if (key.startsWith("$"))
                newKey = "\\uff04" + key.substring(1);
            if (key.contains(".")) {
                newKey = newKey.replace(".", "\\uff0e");
            }
            newDoc.put(newKey, value);
        }
        return newDoc;
    }

    /**
     * MongoDB doesn't allow keys to have dots (.) or to start with $.  This method decodes such keys back to dots and $ if found
     * and returns a new BSON document
     */
    public static org.bson.Document decodeMongoDoc(org.bson.Document doc) {
        org.bson.Document newDoc = new org.bson.Document();
        for (String key : doc.keySet()) {
            Object value = doc.get(key);
            if (value instanceof org.bson.Document)
                value = decodeMongoDoc(doc.get(key, org.bson.Document.class));
            else if (value instanceof List<?>) {
                for (int i=0; i < ((List<?>)value).size(); i++) {
                    Object obj = ((List<?>)value).get(i);
                    if (obj instanceof org.bson.Document)
                        ((List<org.bson.Document>)value).set(i, decodeMongoDoc((org.bson.Document)obj));
                }
            }

            String newKey = key;
            if (key.startsWith("\\uff04"))
                newKey = "$" + key.substring(6);
            if (key.contains("\\uff0e")) {
                newKey = newKey.replace("\\uff0e", ".");
            }
            newDoc.put(newKey, value);
        }
        return newDoc;
    }

    @Override
    public void close() {
        closeConnection();
    }
}
