/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.model.value.process;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.centurylink.mdw.model.value.attribute.AttributeVO;

public class LaneVO implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String PARTICIPANT_TYPE = "PARTICIPANT_TYPE";
    public static final String PARTICIPANT_PEOPLE = "People";
    public static final String PARTICIPANT_SYSTEM = "System";

    private Long    laneId;
    private String  laneName;
    private PoolVO  poolVO;
    private List<AttributeVO> attributes;

    /**
     * Default Constructor
     */
    public LaneVO() { }

    /**
     * Returns ID of Lane
     * @param
     * @return java.lang.Long LaneID
     */
    public Long getLaneId() {
        return laneId;
    }

    /**
     * Set ID of Lane
     * @param java.lang.Long laneID
     * @return
     */
    public void setLaneId(Long laneId) {
        this.laneId = laneId;
    }

    /**
     * Get PoolVO Object that this lane belongs to
     * @param
     * @return com.centurylink.mdw.model.value.process.PoolVO
     */
    public PoolVO getPoolVO() {
        return poolVO;
    }

    /**
     * Set PoolVO Object that this lane belongs to
     * @param com.centurylink.mdw.model.value.process.PoolVO
     * @return
     */
    public void setPool(PoolVO poolVO) {
        this.poolVO = poolVO;
    }

    /**
     * Set Lane name
     * @param
     * @return java.lang.String
     */
    public String getLaneName() {
        return laneName;
    }

    /**
     * Get Lane name
     * @param
     * @return
     */
    public void setLaneName(String laneName) {
        this.laneName = laneName;
    }

    /**
     * Returns the list of AttributeVOs for Lane
     * @param
     * @return java.util.List<com.centurylink.mdw.model.value.process.ArributeVO>
     */
    public List<AttributeVO> getAttributes() {
        return attributes;
    }

    /**
     * Sets the List of AttributeVOs for Lane
     * @param
     * @return java.util.List<com.centurylink.mdw.model.value.process.ArributeVO>
     */
    public void setAttributes(List<AttributeVO> attributes) {
        this.attributes = attributes;
    }

    public boolean isSystem() {
        String av = AttributeVO.findAttribute(attributes, PARTICIPANT_TYPE);
        if (av==null || av.equals(PARTICIPANT_SYSTEM)) return true;
        else return false;
    }

    public void setSystem(boolean isSystem) {
        if (attributes==null) attributes = new ArrayList<AttributeVO>();
        AttributeVO.setAttribute(attributes, PARTICIPANT_TYPE,
                isSystem?PARTICIPANT_SYSTEM:PARTICIPANT_PEOPLE);
    }

    public String getAttribute(String attrname) {
        return AttributeVO.findAttribute(attributes, attrname);
    }

    public void setAttribute(String attrname, String value) {
        if (attributes==null) attributes = new ArrayList<AttributeVO>();
        AttributeVO.setAttribute(attributes, attrname, value);
    }
}
