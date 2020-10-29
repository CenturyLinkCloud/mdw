package com.centurylink.mdw.workflow.adapter.db;

import java.sql.SQLException;
import java.util.List;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.activity.types.AdapterActivity;
import com.centurylink.mdw.annotations.Activity;
import com.centurylink.mdw.config.PropertyException;
import com.centurylink.mdw.adapter.AdapterException;
import com.centurylink.mdw.adapter.ConnectionException;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.util.log.StandardLogger.LogLevel;
import com.centurylink.mdw.util.timer.Tracked;
import com.centurylink.mdw.workflow.adapter.ObjectAdapterActivity;

@Tracked(LogLevel.TRACE)
@Activity(value="JDBC SQL Adapter", category=AdapterActivity.class, icon="com.centurylink.mdw.base/adapter.png",
        pagelet="com.centurylink.mdw.base/jdbcSql.pagelet")
public class JdbcSqlAdapter extends ObjectAdapterActivity {

    public enum QueryType {
        Select,
        Update
    }

    public static final String JDBC_DATASOURCE = "jdbcDataSource";
    public static final String QUERY_TYPE = "queryType";
    public static final String PARAM_VAR = "parameterVariable";
    public static final String SQL_QUERY = "sqlQuery";

    @Override
    public final boolean isSynchronous() {
        return true;
    }

    /**
     * Returns a db connection based on the configured jdbc url or datasource, which
     * includes the resource path. Override for HTTPS or other connection type.
     */
    @Override
    protected Object openConnection() throws ConnectionException {
        try {
            String dataSource = getDataSource();
            if (dataSource == null)
                throw new ConnectionException("Missing attribute: " + JDBC_DATASOURCE);

            DatabaseAccess dbAccess = new DatabaseAccess(dataSource);
            dbAccess.openConnection();
            return dbAccess;
        }
        catch (Exception ex) {
            throw new ConnectionException(ConnectionException.CONNECTION_DOWN, ex.getMessage(), ex);
        }
    }

    @Override
    protected void closeConnection(Object connection) {
        ((DatabaseAccess)connection).closeConnection();
    }

    /**
     * The JDBC DataSource name.
     */
    protected String getDataSource() throws PropertyException {
        return getAttributeValueSmart(JDBC_DATASOURCE);
    }

    protected QueryType getQueryType() throws PropertyException {
        return QueryType.valueOf(getAttributeValueSmart(QUERY_TYPE));
    }

    protected String getSqlQuery() throws PropertyException {
        return getAttributeValueSmart(SQL_QUERY);
    }

    /**
     * Invokes the JDBC Query.
     * If QueryType is Select, returns a java.sql.ResultSet.
     * If QueryType is Update, returns an Integer with the number of rows updated.
     */
    public Object invoke(Object conn, Object requestData) throws AdapterException {
        try {
            DatabaseAccess dbAccess = (DatabaseAccess)conn;
            if (requestData == null)
                throw new AdapterException("Missing SQL Query");
            String query = (String) requestData;
            QueryType queryType = getQueryType();

            Object queryParams = getQueryParameters();
            if (queryParams instanceof List<?>) {
                if (queryType == QueryType.Select)
                    return dbAccess.runSelect(query, ((List<?>) queryParams).toArray());
                else if (queryType == QueryType.Update) {
                    Integer ret = new Integer(dbAccess.runUpdate(query, ((List<?>)queryParams).toArray()));
                    dbAccess.commit();
                    return ret;
                }
                else
                    throw new AdapterException("Unsupported query type: " + queryType);
            }
            else {
                if (queryType == QueryType.Select)
                    return dbAccess.runSelect(query, queryParams);
                else if (queryType == QueryType.Update) {
                    Integer ret = new Integer(dbAccess.runUpdate(query, queryParams));
                    dbAccess.commit();
                    return ret;
                }
                else
                    throw new AdapterException("Unsupported query type: " + queryType);
            }
        }
        catch (SQLException ex) {
            AdapterException adapEx = new AdapterException(-1, ex.getMessage(), ex);
            if (isRetryable(ex))
              adapEx.setRetryable(true);
            throw adapEx;
        }
        catch (Exception ex) {
            throw new AdapterException(-1, ex.getMessage() , ex);
        }
    }

    /**
     * Override to govern retry behavior.
     */
    protected boolean isRetryable(Exception ex) {
        return false;
    }

    /**
     * The method returns parameter values to use in the query.
     * The default implementation returns the value of the defined parameters variable,
     * which can be one of the following types:
     * <ul>
     *   <li>java.lang.String</li>
     *   <li>java.lang.Integer</li>
     *   <li>java.lang.Long</li>
     *   <li>java.util.Date</li>
     *   <li>java.lang.String[]</li>
     *   <li>java.lang.Integer[]</li>
     *   <li>java.lang.Long[]</li>
     *   <li>java.lang.Object (as long as its value is one of the supported types, or an Array of supported types)</li>
     *   <li>null (no parameters)</li>
     * </ul>
     *
     * If no parameterVariable is set, this behaves the same as if the variable value is null (no parameters).
     * Returns an Object or an array of Objects constructed from the parameters.
     */
    protected Object getQueryParameters() throws ActivityException {
        try {
            String paramVar = getAttributeValueSmart(PARAM_VAR);
            if (paramVar == null)
                return null;
            return this.getVariableValue(paramVar);
        }
        catch (Exception ex) {
            throw new ActivityException(ex.getMessage(), ex);
        }
    }

    /**
     * Returns the parameterized SQL query to execute.
     */
    @Override
    protected Object getRequestData() throws ActivityException {
        try {
            return executePreScript(getSqlQuery());
        }
        catch (Exception ex) {
            throw new ActivityException(ex.getMessage(), ex);
        }
    }

}
