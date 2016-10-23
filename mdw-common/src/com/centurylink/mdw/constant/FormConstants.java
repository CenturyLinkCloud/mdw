/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.constant;

public class FormConstants {

    public static final String FORMATTR_VX = "VX";
    public static final String FORMATTR_VY = "VY";
    public static final String FORMATTR_VW = "VW";
    public static final String FORMATTR_VH = "VH";
    public static final String FORMATTR_LX = "LX";
    public static final String FORMATTR_LY = "LY";
    public static final String FORMATTR_LW = "LW";
    public static final String FORMATTR_LH = "LH";
    public static final String FORMATTR_ID = "ID";
    public static final String FORMATTR_DATA = "DATA";
    public static final String FORMATTR_LABEL = "LABEL";
    public static final String FORMATTR_AUTOVALUE = "AUTOVALUE";
    public static final String FORMATTR_VALIDATORS = "VALIDATORS";
    public static final String FORMATTR_VALIDATE = "VALIDATE";
    public static final String FORMATTR_CHOICES = "CHOICES";
    public static final String FORMATTR_EDITABLE = "EDITABLE";
    public static final String FORMATTR_CAN_TYPE_IN = "CAN_TYPE_IN";
    public static final String FORMATTR_IS_STATIC = "IS_STATIC";
    public static final String FORMATTR_VISIBLE = "VISIBLE";
    public static final String FORMATTR_REQUIRED = "REQUIRED";
    public static final String FORMATTR_ACTION = "ACTION";
    public static final String FORMATTR_DIRECTION = "DIRECTION";
    public static final String FORMATTR_TEMPLATE = "TEMPLATE";
    public static final String FORMATTR_INVALID_MSG = "INVALID_MSG";
    public static final String FORMATTR_TIP = "TIP";
    public static final String FORMATTR_IMAGE = "IMAGE";
    public static final String FORMATTR_SHOW_BUSY = "SHOW_BUSY";
    public static final String FORMATTR_PROMPT = "PROMPT";
    public static final String FORMATTR_DATE_PATTERN = "DATE_PATTERN";
    public static final String FORMATTR_TABLE_STYLE = "TABLE_STYLE";
    public static final String FORMATTR_COLUMN_STYLE = "COLUMN_STYLE";
    public static final String FORMATTR_TABBING_STYLE = "TABBING_STYLE";
    public static final String FORMATTR_SORTABLE = "SORTABLE";

    // the following attributes are for rootnote only
    public static final String FORMATTR_NO_MESSAGE_LOGGING = "NO_MESSAGE_LOGGING";

    public static final String FORMATTRVALUE_TABLESTYLE_SCROLLED = "scrolled";
    public static final String FORMATTRVALUE_TABLESTYLE_PAGINATED = "paginated";
    public static final String FORMATTRVALUE_TABLESTYLE_SIMPLE = "simple";

    public static final String FORMATTRVALUE_TABBINGSTYLE_CLIENT = "client";
    public static final String FORMATTRVALUE_TABBINGSTYLE_SERVER = "server";
    public static final String FORMATTRVALUE_TABBINGSTYLE_AJAX = "ajax";
    public static final String FORMATTRVALUE_TABBINGSTYLE_JQUERY = "jquery";

    public static final String FORMATTRVALUE_PROMPT_NONE = "none";
    public static final String FORMATTRVALUE_PROMPT_CONFIRM = "confirm";
    public static final String FORMATTRVALUE_PROMPT_INPUT = "input";
    public static final String FORMATTRVALUE_PROMPT_SELECT = "select";

    public static final String WIDGET_PAGELET = "PAGELET";
    public static final String WIDGET_TEXT = "TEXT";
    public static final String WIDGET_TEXTAREA = "TEXTAREA";
    public static final String WIDGET_DROPDOWN = "DROPDOWN";
    public static final String WIDGET_LIST = "LIST";
    public static final String WIDGET_HYPERLINK = "HYPERLINK";
    public static final String WIDGET_BUTTON = "BUTTON";
    public static final String WIDGET_PANEL = "PANEL";
    public static final String WIDGET_MENUBAR = "MENUBAR";
    public static final String WIDGET_MENU = "MENU";
    public static final String WIDGET_MENUITEM = "MENUITEM";
    public static final String WIDGET_TABBEDPANE = "TABBEDPANE";
    public static final String WIDGET_TAB = "TAB";
    public static final String WIDGET_TABLE = "TABLE";
    public static final String WIDGET_COLUMN = "COLUMN";
    public static final String WIDGET_RADIOBUTTONS = "RADIOBUTTONS";
    public static final String WIDGET_INCLUDE = "INCLUDE";
    public static final String WIDGET_LISTPICKER = "LISTPICKER";
    public static final String WIDGET_DATE = "DATE";
    public static final String WIDGET_CHECKBOX = "CHECKBOX";


    public static String[] all_widget_names = {
        FormConstants.WIDGET_TEXT,
        FormConstants.WIDGET_TEXTAREA,
        FormConstants.WIDGET_DROPDOWN,
        FormConstants.WIDGET_LIST,
        FormConstants.WIDGET_HYPERLINK,
        FormConstants.WIDGET_BUTTON,
        FormConstants.WIDGET_PANEL,
        FormConstants.WIDGET_MENUBAR,
        FormConstants.WIDGET_MENU,
        FormConstants.WIDGET_MENUITEM,
        FormConstants.WIDGET_TABBEDPANE,
        FormConstants.WIDGET_TAB,
        FormConstants.WIDGET_TABLE,
        FormConstants.WIDGET_COLUMN,
        FormConstants.WIDGET_RADIOBUTTONS,
        FormConstants.WIDGET_DATE,
        FormConstants.WIDGET_CHECKBOX,
        FormConstants.WIDGET_LISTPICKER,
        FormConstants.WIDGET_INCLUDE
    };

    // various URL arguments
    // html hidden fields (never really URL arguments)
    public static final String URLARG_HIDDEN_WINID = "hiddenWinid";
    public static final String URLARG_HIDDEN_ACTION = "hiddenAction";
    public static final String URLARG_HIDDEN_ISDIALOG = "hiddenIsDialog";
    public static final String URLARG_WINID = "winid";        // used by JSF version in template
    // explicit URL parameters
    public static final String URLARG_INPUTXML = "inputxml";
    public static final String URLARG_INPUTDOC = "inputdoc";
    public static final String URLARG_INPUTREF = "inputref";
    public static final String URLARG_TASK_INSTANCE_ID = "taskInstanceId";
    public static final String URLARG_UNIQUE_ID = "uniqueid";
    public static final String URLARG_PARENT = "parent";
    public static final String URLARG_REFRESH = "refresh";
    public static final String URLARG_PERSIST = "persist";
    public static final String URLARG_PROMPT = "prompt";
    // action parameters (there are more - should not hard code them)
    public static final String URLARG_CUID = "cuid";
    // both for explicit URL and action parameters
    public static final String URLARG_FORMNAME = "formName";
    public static final String URLARG_NAME = "name";
    public static final String URLARG_ACTION = "action";
    public static final String URLARG_DIR = "dir";            // list picker action direction
    public static final String URLARG_LIST = "list";        // list picker action list
    public static final String URLARG_TABLE = "table";        // for table paging function
    public static final String URLARG_TOPAGE = "topage";    // for table paging function
    public static final String URLARG_META = "meta";        // for table paging function
    public static final String URLARG_PAGESIZE = "pagesize";    // for table paging function
    public static final String URLARG_ROW = "row";            // for table add/delete/update row
    public static final String URLARG_SORTON = "sorton";    // for table paging function
    public static final String URLARG_TABS = "tabs";        // for tabbing of tabbed pane
    public static final String URLARG_TAB = "tab";            // for tabbing of tabbed pane
    public static final String URLARG_DATA = "data";        // for tabbing of tabbed pane
    public static final String URLARG_TIMEOUT = "timeout";    // timeout for calling engine
    public static final String URLARG_COMPLETION_CODE = "completionCode";    // completion code for calling engine
    public static final String URLARG_COMMENT = "comment";
    public static final String URLARG_PROMPT_INPUT = "prompt_input";

    public static final String TASK_CORRELATION_ID_PREFIX = "TaskInstance:";
    public static final String TABLE_ROW_DIALOG_PREFIX = "TableRowDialog";

}
