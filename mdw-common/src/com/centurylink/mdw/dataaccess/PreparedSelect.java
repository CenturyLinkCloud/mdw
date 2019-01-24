package com.centurylink.mdw.dataaccess;

public class PreparedSelect {

    private String sql;
    public String getSql() { return sql; }

    private Object[] params;
    public Object[] getParams() { return params; }

    private String message;
    public String getMessage() { return message; }

    public PreparedSelect(String sql, Object[] params) {
        this(sql, params, null);
    }

    public PreparedSelect(String sql, Object[] params, String message) {
        this.sql = sql;
        this.params = params;
        this.message = message;
    }
}
