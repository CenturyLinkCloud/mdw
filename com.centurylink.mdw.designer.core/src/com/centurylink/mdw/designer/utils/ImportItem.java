/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.designer.utils;

public class ImportItem {
    
    public static final int TYPE_PACKAGE = 1;
    public static final int TYPE_IMPLEMENTOR = 2;
    public static final int TYPE_HANDLER = 3;
    public static final int TYPE_PROCESS = 4;
    public static final int TYPE_RULESET = 5;
    
    public static final int STATUS_NEW_VERSION = 1;
    public static final int STATUS_SAME_VERSION = 2;
    public static final int STATUS_OLD_VERSION = 3;
    public static final int STATUS_NEW = 4;
    public static final int STATUS_SAME = 5;
    public static final int STATUS_DIFFERENT = 6;
    public static final int STATUS_NOT_PACKAGE = 7;
    
    private String name;
    private int type;
    private int status;
    private boolean selected;
    
    public ImportItem(String name, int type, int status) {
        this.name = name;
        this.type = type;
        this.status = status;
        selected = status==STATUS_NEW_VERSION||status==STATUS_NEW||status==STATUS_DIFFERENT;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public String getName() {
        return name;
    }

    public int getType() {
        return type;
    }

    public int getStatus() {
        return status;
    }
    
    public String getTypeAsString() {
        switch (type) {
        case TYPE_PACKAGE: return "package";
        case TYPE_IMPLEMENTOR: return "implementor";
        case TYPE_HANDLER: return "event handler";
        case TYPE_PROCESS: return "process";
        case TYPE_RULESET: return "rule set";
        default: return "";
        }
    }

    public String getStatusAsString() {
        switch (status) {
        case STATUS_NEW_VERSION: return "new/new version";
        case STATUS_SAME_VERSION: return "version exist";
        case STATUS_OLD_VERSION: return "old version";
        case STATUS_NEW: return "new";
        case STATUS_SAME: return "exist and same";
        case STATUS_DIFFERENT: return "exist and different";
        case STATUS_NOT_PACKAGE: return "not a package";
        default: return "";
        }
    }
    
    public boolean canImport() {
        return status==STATUS_NEW_VERSION || status==STATUS_NEW || status==STATUS_DIFFERENT
        	|| status==STATUS_SAME_VERSION&&type==TYPE_RULESET;
    }

}
