/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.constant;

/**
 * Constants relating to process variables.
 */
public class VariableConstants {

	/**
	 * Variable mode constants
	 */
    public static final Integer VARIABLE_CATEGORY_LOCAL = new Integer(0);
    public static final Integer VARIABLE_CATEGORY_INPUT = new Integer(1);
    public static final Integer VARIABLE_CATEGORY_OUTPUT = new Integer(2);
    public static final Integer VARIABLE_CATEGORY_IN_OUT = new Integer(3);

    /**
     * variable name constants
     */
    public static final String MASTER_DOCUMENT = "MasterDocument";
    public static final String REQUEST = "request";
    public static final String REQUEST_HEADERS = "requestHeaders";
    public static final String RESPONSE = "response";
    public static final String RESPONSE_HEADERS = "responseHeaders";
    public static final String MDW_UTIL_MAP = "MDWUtilMap";
    public static final String PACKAGE_ID = "PackageId";

}
