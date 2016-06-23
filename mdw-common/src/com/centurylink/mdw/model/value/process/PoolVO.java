/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.model.value.process;

import java.io.Serializable;
import java.util.List;

import com.centurylink.mdw.model.value.attribute.AttributeVO;

public class PoolVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long         poolId;
    private PackageVO    packageVO;
    private Long         processId;
    private String       poolName;
    private List<LaneVO> lanes;
    private List<AttributeVO> attributes;

    /**
     * Default Constructor
     */
    public PoolVO() {}

    /**
     * Returns ID of pool
     * @param
     * @return java.lang.Long poolId
     */
    public Long getPoolId() {
        return poolId;
    }

    /**
     * Set ID of pool
     * @param java.lang.Long pooId
     * @return
     */
    public void setPoolId(Long poolId) {
        this.poolId = poolId;
    }

    /**
     * Get PackageVO that this pool is associated with
     * @param
     * @return com.centurylink.mdw.model.value.process.PackageVO
     */
    public PackageVO getPackageVO() {
        return packageVO;
    }

    /**
     * Set PackageVO that this pool is associated with
     * @param com.centurylink.mdw.model.value.process.PackageVO
     * @return
     */
    public void setPackageVO(PackageVO packageVO) {
        this.packageVO = packageVO;
    }

    /**
     * Get ID of process that uses this pool
     * @param
     * @return java.lang.Long processId
     */
    public Long getProcessId() {
        return this.processId;
    }

    /**
     * Set ID of process that uses this pool
     * @param  java.lang.Long processId
     * @return
     */
    public void setProcessId(Long processId) {
        this.processId = processId;
    }

    /**
     * Gets the name of this pool
     * @param
     * @return java.lang.String
     */
    public String getPoolName() {
        return poolName;
    }

    /**
     * Sets the name of this pool
     * @param  java.lang.String
     * @return
     */
    public void setPoolName(String poolName) {
        this.poolName = poolName;
    }

    /**
     * Set LaneVO for this Pool
     * @param  java.util.List<com.centurylink.mdw.model.value.process.LaneVO>
     * @return
     */
    public List<LaneVO> getLanes() {
        return lanes;
    }

    /**
     * Set LaneVO for this Pool
     * @param  java.util.List<com.centurylink.mdw.model.value.process.LaneVO>
     * @return
     */
    public void setLanes(List<LaneVO> lanes) {
        this.lanes = lanes;
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
}
