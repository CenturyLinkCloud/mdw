/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.dataaccess;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import com.centurylink.mdw.common.exception.PropertyException;
import com.centurylink.mdw.common.utilities.FileHelper;
import com.qwest.mbeng.DomDocument;
import com.qwest.mbeng.FormatDom;
import com.qwest.mbeng.MbengNode;

public class SqlQueries {

    public static final String GET_TASKS_FOR_WORKGROUP_SQL = "Task.GET_TASKS_FOR_WORKGROUP";
    public static final String READ_ALL_TASK_INSTANCE_VO_BY_MASTER_OWNER_ID_SQL =
    	"TaskInstance.READ_ALL_TASK_INSTANCE_VO_BY_MASTER_OWNER_ID_SQL";
    public static final String READ_ALL_BY_USER_ID_SQL = "TaskAction.READ_ALL_BY_MAPPED_USER_ID";
    public static final String READ_ALL_BY_USER_GROUPS_SQL = "TaskAction.READ_ALL_BY_MAPPED_USER_GROUPS";
    public static final String READ_ALL_TASK_STATES_SQL = "TaskState.READ_ALL_TASK_STATES";
    public static final String REPORT_BY_TASK_NAME_SQL = "TaskInstance.REPORT_BY_TASK_NAME";
    public static final String REPORT_BY_USER_ID_SQL = "TaskInstance.REPORT_BY_CUID";
    public static final String REPORT_BY_TASK_CATEGORY_SQL = "TaskInstance.REPORT_BY_TASK_CATEGORY";
    public static final String GET_PROCESS_INSTANCE_VARIABLES = "GET_PROCESS_INSTANCE_VARIABLES";
    public static final String GET_VARIABLE_INSTANCE_OWNER = "GET_VARIABLE_INSTANCE_OWNER";
    public static final String GET_TASK_INSTANCE_VARIABLES = "GET_TASK_INSTANCE_VARIABLES";
    public static final String GET_TEMPLATE_TASKS_FOR_WORKGROUP_SQL = "Task.GET_TEMPLATE_TASKS_FOR_WORKGROUP";

	private static SqlQueries singleton = null;

	private Map<String,String> queries = null;

    private static final String FILE_NAME = "sql-queries.xml";

	public void load() throws PropertyException {
	    InputStream stream = null;
		try {
		    stream = FileHelper.openConfigurationFile(FILE_NAME);
		    DomDocument domdoc = new DomDocument();
		    FormatDom fmter = new FormatDom();
		    fmter.load(domdoc, stream);
	        stream.close();
	        MbengNode node;
	        queries = new HashMap<String,String>();
	        for (node=domdoc.getRootNode().getFirstChild(); node!=null; node=node.getNextSibling()) {
	        	String queryName = node.getAttribute("name");
	        	String query = node.getValue();
	        	queries.put(queryName, query);
	        }
		} catch (Exception ex) {
		    throw new PropertyException(-1, "Cannot load queries", ex);
		} finally {
	    	if (stream!=null) {
				try {stream.close();} catch (IOException e) {}
	    	}
		}
	}


	public void load(String fileName) throws PropertyException {
        InputStream stream = null;
        try {
            stream = FileHelper.openConfigurationFile(fileName);
            DomDocument domdoc = new DomDocument();
            FormatDom fmter = new FormatDom();
            fmter.load(domdoc, stream);
            stream.close();
            MbengNode node;
            queries = new HashMap<String,String>();
            for (node=domdoc.getRootNode().getFirstChild(); node!=null; node=node.getNextSibling()) {
                String queryName = node.getAttribute("name");
                String query = node.getValue();
                queries.put(queryName, query);
            }
            queries.put("FileName", fileName);
        } catch (Exception ex) {
            throw new PropertyException(-1, "Cannot load queries", ex);
        } finally {
            if (stream!=null) {
                try {stream.close();} catch (IOException e) {}
            }
        }
    }


	private static synchronized SqlQueries getSingleton(String fileName) throws PropertyException {
        if (singleton==null) {
            singleton = new SqlQueries();
            if(fileName.equals("")){
              singleton.load();
            }else{
              singleton.load(fileName);
            }
        }else{
            if(!fileName.equals("")){

                if(!(singleton.queries.get("FileName")).equals(fileName)){
                  singleton.load(fileName);
                }
            }
        }
        return singleton;
    }

	public static String getQuery(String queryName) throws PropertyException {
		SqlQueries one = getSingleton("");
		return one.queries.get(queryName);
	}

	public static String getQuery(String queryName,String fileName) throws PropertyException {
	    SqlQueries one = getSingleton(fileName);
        return one.queries.get(queryName);
    }
}
