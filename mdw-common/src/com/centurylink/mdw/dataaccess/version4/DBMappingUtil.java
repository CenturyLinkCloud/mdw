/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.dataaccess.version4;

import com.centurylink.mdw.common.exception.PropertyException;
import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.common.utilities.property.PropertyManager;
import com.centurylink.mdw.common.utilities.property.PropertyUtil;

public class DBMappingUtil {
	private static String DB_PROPERTY_GROUP = "MDWFramework.MDWDesigner"; 
	private static String DB_SCHEMA_OWNER_PROPERTY = "mdw.schema_owner";
	
	public static String tagSchemaOwner (String objectName) {
		String retval = objectName;
		try {
			String owner = (schemaOwner == null ? getSchemaOwner() : schemaOwner); 
			retval = (!StringHelper.isEmpty(owner)) ? owner + "." + objectName : objectName; 
		} catch (Exception e) {
			// cannot use logger - it may not be initialized yet
			System.out.println("Exception in getting tagSchemaOwner");
			e.printStackTrace();
		}
		return retval;
	}
	
    private static String schemaOwner;
    
    /**
     * Allows specifying schema owner rather than reading from a property (used
     * by eclipse designer).  An empty string means that no owner should be used.
     */
    public static void setSchemaOwner(String owner) { schemaOwner = owner; }

    public static String getSchemaOwner () throws PropertyException {
    	PropertyManager propMgr = PropertyUtil.getInstance().getPropertyManager();
		return propMgr.getStringProperty(DB_PROPERTY_GROUP, DB_SCHEMA_OWNER_PROPERTY);
	}

}
