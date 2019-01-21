package com.centurylink.mdw.dataaccess;

public class PreparedWhere {

    private String where;
    public String getWhere() { return where; }

    private Object[] params;
    public Object[] getParams() { return params; }

    public PreparedWhere(String where, Object[] params) {
        this.where = where;
        this.params = params;
    }
}
