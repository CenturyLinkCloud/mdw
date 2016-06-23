/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.utilities.property;

/**
 */

public class PropertyUtil extends Object {

    private static PropertyUtil instance = null;

    /**
     * Returns the handle to the property Manager
     * @return PropertyManager
     */
    public static PropertyUtil getInstance() {
    	if (instance==null) {
    		getInstance_synchronized();
    	}
    	return instance;
    }

	private static synchronized void getInstance_synchronized() {
		if(instance==null) instance = new PropertyUtil();
	}

    /**
     * returns the handle to the property manager
     * @return PropertyManager
     */
 	public PropertyManager getPropertyManager() {
 		return PropertyManager.getInstance();
 	}
    
}