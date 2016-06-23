/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.model.value.task;

// JAVA IMPORTS -----------------------------------------------------

// CUSTOM IMPORTS -----------------------------------------------------

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 */


public class TaskInstanceReportVO implements Serializable{

    // CONSTANTS ------------------------------------------------------
    public static final String REPORT_TYPE_TASK_NAME = "TASK_NAME";
    public static final String REPORT_TYPE_USER_ID = "USER_ID";
    public static final String REPORT_TYPE_TASK_CATEGORY = "TASK_CATEGORY";
	// CLASS VARIABLES ------------------------------------------------
    public static final long serialVersionUID = 1L;
	// INSTANCE VARIABLES ---------------------------------------------
    private String entityName;
    private List<TaskInstanceReportItemVO> items;
	// CONSTRUCTORS ---------------------------------------------------
    public TaskInstanceReportVO(){
        items = new ArrayList<TaskInstanceReportItemVO>();
    }

    public TaskInstanceReportVO(String pName){
        this.entityName = pName;
        items = new ArrayList<TaskInstanceReportItemVO>();
    }
	// PUBLIC AND PROTECTED METHODS -----------------------------------

     /**
      * Returns the entity Name
      */
     public String getEntityName(){
        return this.entityName;
     }

    /**
     * Adds the passed in data as report Item
     * @param pState
     * @param pCount
     */
    public void addTaskInstanceReportItem(Integer pState, Integer pCount){
        items.add(new TaskInstanceReportItemVO(pState, pCount));
    }

    /**
     * Returns the array of report items
     * @return Array of TaskInstanceReportItemVO[]
     */
    public TaskInstanceReportItemVO[] getTaskInstanceReportItems(){
      if(this.items.isEmpty()){
          return new TaskInstanceReportItemVO[0];
      }
      return (TaskInstanceReportItemVO[])this.items.toArray(new TaskInstanceReportItemVO[]{});

    }
    // PRIVATE METHODS ------------------------------------------------

	// ACCESSOR METHODS -----------------------------------------------

	// INNER CLASSES --------------------------------------------------
    public class TaskInstanceReportItemVO implements Serializable{

    	private static final long serialVersionUID = 1L;

        private Integer itemState;
        private Integer itemCount;

        public TaskInstanceReportItemVO(Integer pState, Integer pCount){
           this.itemState = pState;
           this.itemCount = pCount;
        }

        public Integer getItemState(){
            return this.itemState;
        }
        public Integer getItemCount(){
            return this.itemCount;
        }


    }
}
