/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.dataaccess;

public class TableSequenceName {
	
	private String tableName;
	
	public TableSequenceName(String tableName) {
		this.tableName = tableName;
	}
	
	public String getSequenceName() {
		if (tableName.equalsIgnoreCase("EVENT_WAIT_INSTANCE"))
			return "EVENT_WAIT_INSTANCE_ID_SEQ";
		return "UKNOWN_SEQUENCE";
	}

}
