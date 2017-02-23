/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.dataaccess;

import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.naming.NamingException;
import javax.sql.DataSource;

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.ApplicationConstants;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.model.task.TaskInstance;
import com.centurylink.mdw.util.StringHelper;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoDatabase;

public class DatabaseAccess {

    private String database_name; // JDBC url or a connection pool name
    private static String INTERNAL_DATA_SOURCE = null;
    private static Long db_time_diff = null;

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

    private boolean isEmbedded;

    private static boolean isMongoDB = false;
    private static MongoClient mongoClient;
    public static MongoDatabase getMongoDb() {
        if (mongoClient == null)
            return null;
        else
            return mongoClient.getDatabase("mdw");
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
        return (jdbcUrl.contains("localhost") || (dbprop != null && jdbcUrl.contains(ApplicationContext.getServerHost()) && ApplicationContext.getServerHostPort().equalsIgnoreCase(dbprop)));
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

        if (database_name.startsWith("jdbc:oracle:thin:")) {
            int slash_loc = database_name.indexOf('/');
            int at_loc = database_name.indexOf('@');
            if (slash_loc<0||at_loc<0||at_loc<slash_loc) {
                throw new SQLException("Incorrect Oracle connection spec: " + database_name);
            }
            String dbuser = database_name.substring(17,slash_loc);
            String dbpass = database_name.substring(slash_loc+1,at_loc);
            String dburl = "jdbc:oracle:thin:" + database_name.substring(at_loc);
            try {
                try {
                    DriverManager.getDriver(dburl);
                } catch (SQLException e) {
                    Class.forName("oracle.jdbc.driver.OracleDriver");
                }
                // driver needs to be loaded when running this stand-alone, but not within WLS
                java.util.Properties dbprops = new java.util.Properties();
                dbprops.put("user", dbuser);
                dbprops.put("password", dbpass);
                if (connectParams != null) {
                    for (String paramName : connectParams.keySet())
                        dbprops.put(paramName, connectParams.get(paramName));
                }
                connection = DriverManager.getConnection(dburl, dbprops);

            } catch (ClassNotFoundException e) {
                throw new SQLException("Cannot find Oracle Driver class");
            }
        } else if (database_name.startsWith("jdbc:mysql:") || database_name.startsWith("jdbc:mariadb:")) {
            int qmark_loc = database_name.indexOf('?');
            if (qmark_loc<0) {
                throw new SQLException("Incorrect MySQL connection spec: " + database_name);
            }
            String dburl = database_name.substring(0,qmark_loc);
            String rest = database_name.substring(qmark_loc);
            int amp_loc = rest.indexOf('&');
            if (amp_loc<0)
                throw new SQLException("Incorrect MySQL connection spec: " + database_name);
            if (!rest.startsWith("?user="))
                throw new SQLException("Incorrect MySQL connection spec: " + database_name);
            String dbuser = rest.substring(6,amp_loc);
            rest = rest.substring(amp_loc);
            if (!rest.startsWith("&password="))
                throw new SQLException("Incorrect MySQL connection spec: " + database_name);
            String dbpass = rest.substring(10);
            try {
                try {
                    DriverManager.getDriver(dburl);
                } catch (SQLException e) {
                    Class.forName("com.mysql.jdbc.Driver");
                }
               // driver needs to be loaded when running this stand-alone, but not within WLS
                connection = DriverManager.getConnection(dburl, dbuser, dbpass);
            } catch (ClassNotFoundException e) {
                throw new SQLException("Cannot find MySQL Driver class");
            }
        } else {
            try {
                DataSource dataSource = ApplicationContext.getDataSourceProvider().getDataSource(database_name);
                connection = dataSource.getConnection();
            } catch (NamingException e) {
                throw new SQLException("Failed to find data source " + database_name, e);
            }
        }
        connection.setAutoCommit(false);

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
            LoggerUtil.getStandardLogger().info(mongoClient.getMongoClientOptions().toString());
        }
    }

    public void commit() throws SQLException {
        connection.commit();
    }

    public void rollback() {
        try {
            if (connectionIsOpen()) {
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
        if (queryTimeout > 0 && ps != null)
            ps.setQueryTimeout(queryTimeout);
        if (connection instanceof QueryLogger)
            return ((QueryLogger)connection).executeUpdate(ps, query);
        else return ps.executeUpdate();
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
             // ignore reminder if the difference millisec is less than a minute and return nearest half-hour
            db_time_diff = new Long(Math.abs(r) < 60000 ? q*1800000 : raw_diff);

            System.out.println("Database time difference: " + db_time_diff/1000.0 + " seconds (raw diff=" + raw_diff + ")");
            TaskInstance.setDbTimeDiff(db_time_diff);
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
}
