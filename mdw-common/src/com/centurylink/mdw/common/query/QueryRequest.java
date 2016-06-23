/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.query;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QueryRequest implements Serializable {

    public static final long serialVersionUID = 1L;
    public static final int ALL_ROWS = -1;
    public static final int FIRST_PAGE_INDEX = 0;

    private int pageSize;
    private int showAllDisplayRows;

    private int pageIndex;

    private Map<String,String> restrictions;
    private List<QueryRequest.Restriction> restrictionList;

    private String orderBy;

    private boolean isAscendingOrder;

    public QueryRequest() {
        super();
        restrictions = new HashMap<String,String>();

    }

    /**
     * Method that returns the restriction list
     *
     * @return List of {@link QueryRequest.Restriction}
     */
    public List<QueryRequest.Restriction> getRestrictionList() {
        return restrictionList;
    }

    /**
     *
     * @param pList
     *            of {@link QueryRequest.Restriction}
     */
    public void setRestrictionList(List<QueryRequest.Restriction> pList) {
        this.restrictionList = pList;
    }

    /**
     * Method that adds to the restriction list
     *
     * @param pItem
     *            {@link QueryRequest.Restriction}
     */
    public void addToRestrictionList(QueryRequest.Restriction pItem) {
        if (this.restrictionList == null) {
            this.restrictionList = new ArrayList<QueryRequest.Restriction>();
        }
        this.restrictionList.add(pItem);
    }

    /**
     * Returns the pageSize
     *
     * @return pageSize
     */
    public int getPageSize() {
        return this.pageSize;
    }

    /**
     * set the pageSize
     *
     * @param pageSize
     */
    public void setPageSize(int pSize) {
        this.pageSize = pSize;
    }

   /**
    * Returns the showAllDisplayRows
    *
    * @return showAllDisplayRows
    */
   public int getShowAllDisplayRows() {
       return this.showAllDisplayRows;
   }

   /**
    * set the showAllDisplayRows
    *
    * @param showAllDisplayRows
    */
   public void setShowAllDisplayRows(int showAllDisplayRows) {
       this.showAllDisplayRows = showAllDisplayRows;
   }

    /**
     * Ruturns the pageIndex
     *
     * @return pageIndex
     */
    public int getPageIndex() {
        return this.pageIndex;
    }

    /**
     * set the page index
     *
     * @param pIndex
     */
    public void setPageIndex(int pIndex) {
        this.pageIndex = pIndex;
    }

    /**
     * Returns the data
     */
    public Map<String,String> getRestrictions() {
        return this.restrictions;
    }

    /**
     * sets the data collection
     *
     * @param pData
     */
    public void setRestrictions(Map<String,String> pData) {
        if (pData != null) {
            this.restrictions.clear();
            this.restrictions.putAll(pData);
        }

    }

    /**
     * Adds the restrictions
     *
     * @param pName
     * @param pValue
     */
    public void addRestriction(String pName, Object pValue) {
        this.addRestriction(pName, pValue);
    }

    /**
     * returns the orderBY column name array
     */
    public String getOrderBy() {
        return this.orderBy;
    }

    /**
     * Sets the order by pOrderBy
     *
     * @param pOrderBy
     */
    public void setOrderBy(String pOrderBy) {
        this.orderBy = pOrderBy;
    }

    /**
     * returns the boolean flag isAscendingOrder
     */
    public boolean isAscendingOrder() {
        return this.isAscendingOrder;
    }

    /**
     * Sets the isAscendingOrder
     *
     * @param isAscendingOrder
     */
    public void setIsAscendingOrder(boolean pFlag) {
        this.isAscendingOrder = pFlag;
    }

    /**
     *
     * @param pName
     * @param pValue
     * @return
     */
    public static EqualRestriction equalRestriction(String pName, Object pValue) {
        return new EqualRestriction(pName, pValue);
    }

    /**
     *
     * @param pName
     * @param pValue
     * @return
     */
    public static LikeRestriction likeRestriction(String pName, Object pValue) {
        return new LikeRestriction(pName, pValue);
    }

    /**
     *
     * @param pName
     * @param pValue1
     * @param pValue2
     * @return
     */
    public static BetweenRestriction betweenRestriction(String pName, Object pValue1, Object pValue2) {
        return new BetweenRestriction(pName, pValue1, pValue2);
    }

    // INNER CLASSES -----------------------------------------------------------

    public static abstract class Restriction {
        private String fieldName;

        private Object fieldValue;

        protected Restriction(String pFieldName, Object pFieldValue) {
            this.fieldName = pFieldName;
            this.fieldValue = pFieldValue;
        }

        public String getFieldName() {
            return fieldName;
        }

        public void setFieldName(String fieldName) {
            this.fieldName = fieldName;
        }

        public Object getFieldValue() {
            return fieldValue;
        }

        public void setFieldValue(Object fieldValue) {
            this.fieldValue = fieldValue;
        }

    }

    public static class EqualRestriction extends Restriction {
        public EqualRestriction(String pFieldName, Object pFieldValue) {
            super(pFieldName, pFieldValue);
        }

    }

    public static class LikeRestriction extends Restriction {
        private boolean isWildCardPrefix;

        public LikeRestriction(String pFieldName, Object pFieldValue) {
            super(pFieldName, pFieldValue);
        }

        public boolean isWildCardPrefix() {
            return isWildCardPrefix;
        }

        public void setWildCardPrefix(boolean isWildCardPrefix) {
            this.isWildCardPrefix = isWildCardPrefix;
        }

    }

    public static class BetweenRestriction extends Restriction {

        private Object fieldValue2;

        public BetweenRestriction(String pFieldName, Object pValue1, Object pValue2) {
            super(pFieldName, pValue1);
            this.fieldValue2 = pValue2;
        }

        public Object getFieldValue2() {
            return fieldValue2;
        }

        public void setFieldValue2(Object fieldValue2) {
            this.fieldValue2 = fieldValue2;
        }

    }

}
