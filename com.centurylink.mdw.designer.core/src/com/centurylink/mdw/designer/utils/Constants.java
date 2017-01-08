/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.designer.utils;

/**
 *
 */
public class Constants {

    public static final String ACTION_PACKAGE = "PACKAGE_PROCESS";
	public static final String ACTION_EXIT = "EXIT";
    public static final String ACTION_EDIT = "EDIT";
    public static final String ACTION_ADD_VAR = "ADD_VARIABLE";
    public static final String ACTION_CREATE_VAR = "CREATE_VARIABLE";
    public static final String ACTION_COMBO = "COMBO";
    public static final String ACTION_CLEAR_START_FROM = "ClearStartFrom";
    public static final String ACTION_CLEAR_START_TO = "ClearStartTo";
    public static final String ACTION_CLEAR_END_FROM = "ClearEndFrom";
    public static final String ACTION_CLEAR_END_TO = "ClearEndTo";
    public static final String ACTION_CALENDAR_START_FROM ="CalendarStartFrom";
    public static final String ACTION_CALENDAR_START_TO ="CalendarStartTo";
    public static final String ACTION_CALENDAR_END_FROM ="CalendarEndFrom";
    public static final String ACTION_CALENDAR_END_TO ="CalendarEndTo";
    public static final String ACTION_IMPORT ="Import";
    public static final String ACTION_EXPORT ="Export";
    public static final String ACTION_EXPORT_XPDL = "ExportXPDL";
    public static final String ACTION_FILTER ="Filter";
    public static final String ACTION_LOGIN = "LOGIN";
	public static final String ACTION_LOGOUT = "LOGOUT";
	public static final String ACTION_FLOWVIEW = "FLOWVIEW";
	public static final String ACTION_DOCVIEW = "DOCVIEW";
	public static final String ACTION_SVRVIEW = "SVRVIEW";
    public static final String ACTION_PREVIOUS = "Previous";
	public static final String ACTION_PROCLIST = "PROCLIST";
	public static final String ACTION_NEW = "NEW";
    public static final String ACTION_NEW_ACTIVITY_IMPL = "NEW_ACTIVITY_IMPL";
    public static final String ACTION_NEXT= "Next";
	public static final String ACTION_DELETE = "DELETE";
    public static final String ACTION_RESET = "Reset";
	public static final String ACTION_SAVE = "SAVE";
    public static final String ACTION_SAVE_IMG = "SAVE_IMG";
    public static final String ACTION_SAVE_XY = "SAVE_XY";
	public static final String ACTION_PRINT = "PRINT";
	public static final String ACTION_TIPMODE = "TIPMODE";
	public static final String ACTION_LINKTYPE = "LINKTYPE";
	public static final String ACTION_CALCSTEP = "CALCSTEP";
    public static final String ACTION_DECALSTEP= "DECALSTEP";
	public static final String ACTION_SUBVIEW = "SUBVIEW";
	public static final String ACTION_MORE = "MORE";
    public static final String ACTION_START = "START";
    public static final String ACTION_REFRESH_CACHE = "ACTION_REFRESH_CACHE";
    public static final String BACK = "BACK";
	public static final String DATABASE_MDW = "MDW";
    public static final String VIEW_ALL_PROCESS_INSTANCE="ALLPROCESSINSTANCE";
    public static final String VIEW_ALL_TASK_INSTANCE="ALLTASKINSTANCE";
    public static final String CUSTOM_SETUP_BUTTON = "CUSTOM_SETUP";
    public static final String EXTERNAL_EVENT_BUTTON = "EXTERNAL_EVENT";
    public static final String ACTION_INCLUDE ="INCLUDE";
    public static final String ACTION_EXCLUDE ="EXCLUDE";
    public static final String ACTION_MOVEUP ="MOVEUP";
    public static final String ACTION_MOVEDOWN ="MOVEDOWN";
    public static final String LOGOUT= "LOGOUT";

    public static boolean isMacOsX() {
        return "Mac OS X".equals(System.getProperty("os.name"));
    }

}
