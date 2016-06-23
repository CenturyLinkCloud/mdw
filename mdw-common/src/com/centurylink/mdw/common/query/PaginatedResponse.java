/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.query;

import java.io.Serializable;


public class PaginatedResponse implements Serializable{

    // CONSTANTS ------------------------------------------------------

	// CLASS VARIABLES ------------------------------------------------
    public static final long serialVersionUID = 1L;

    // INSTANCE VARIABLES ---------------------------------------------------
    private int totalNumberOfRows;
    private int pageSize;
    private int pageIndex;
    private int numberOfRowsReturned;
    private int maxRowsInAllRowsMode;
    private Object[] data;

    // CONSTRUCTORS ---------------------------------------------------
    public PaginatedResponse(){
        super();
    }

    public PaginatedResponse(Object[] pResult, int pTotalRows, int pReturnedRows, int pPageSize, int pPageIndex){
        super();
        this.data = pResult;
        this.numberOfRowsReturned = pReturnedRows;
        this.totalNumberOfRows = pTotalRows;
        this.pageIndex = pPageIndex;
        this.pageSize = pPageSize;
    }

    public PaginatedResponse(Object[] pResult, int pTotalRows, int pReturnedRows, int pPageSize, int pPageIndex, int pShowAllDisplayRows){
        super();
        this.data = pResult;
        this.numberOfRowsReturned = pReturnedRows;
        this.totalNumberOfRows = pTotalRows;
        this.pageIndex = pPageIndex;
        this.pageSize = pPageSize;
        this.maxRowsInAllRowsMode = pShowAllDisplayRows;
    }

    /**
     * Ruturns the total number of rows
     * @return totalNumberOfRows
     */
    public int getTotalNumberOfRows(){
        return this.totalNumberOfRows;
    }

    /**
     * set the total number of rows
     * @param totalNumberOfRows
     */
    public void setTotalNumberOfRows(int pRows){
        this.totalNumberOfRows = pRows;
    }

    /**
     * Ruturns the number of rows returned by the query
     * @return numberOfRowsReturned
     */
    public int getNumberOfRowsReturned(){
        return this.numberOfRowsReturned;
    }

    /**
     * set the number of rows returned by the query
     * @param numberOfRowsReturned
     */
    public void setNumberOfRowsReturned(int pRows){
        this.numberOfRowsReturned = pRows;
    }

    /**
     * Ruturns the pageSize
     * @return pageSize
     */
    public int getPageSize(){
        return this.pageSize;
    }

    /**
     * set the pageSize
     * @param pageSize
     */
    public void setPageSize(int pSize){
        this.pageSize = pSize;
    }

    /**
     * Ruturns the pageIndex
     * @return pageIndex
     */
    public int getPageIndex(){
        return this.pageIndex;
    }

    /**
     * set the page index
     * @param pIndex
     */
    public void setPageIndex(int pIndex){
        this.pageIndex = pIndex;
    }

    /**
     * Returns the data
     */
    public Object[] getData(){
        return this.data;
    }

    /**
     * sets the data collection
     * @param pData
     */
    public void setData(Object[] pData){
        this.data = pData;
    }

    public int getMaxRowsInAllRowsMode() {
        return maxRowsInAllRowsMode;
    }

    /**
     * To set the limit on All rows mode for DataTable
     * @param maxRowsInAllRowsMode
     */
    public void setMaxRowsInAllRowsMode(int maxRowsInAllRowsMode) {
        this.maxRowsInAllRowsMode = maxRowsInAllRowsMode;
    }




}
