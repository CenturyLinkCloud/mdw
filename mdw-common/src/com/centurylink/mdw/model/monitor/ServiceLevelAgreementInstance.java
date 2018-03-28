/*
 * Copyright (C) 2017 CenturyLink, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.centurylink.mdw.model.monitor;

import java.io.Serializable;
import java.util.Date;

import com.centurylink.mdw.constant.OwnerType;

public class ServiceLevelAgreementInstance implements Serializable {

    public static final Integer SLA_BREAK_REPORTED_YES = new Integer(1);
    public static final Integer SLA_BREAK_REPORTED_NO = new Integer(2);

    public static final String OWNER_PROCESS_INSTANCE = OwnerType.PROCESS_INSTANCE;
    public static final String OWNER_ACTIVITY_INSTANCE = OwnerType.ACTIVITY_INSTANCE;
    public static final String OWNER_TASK_INSTANCE = OwnerType.TASK_INSTANCE;

    private Integer breakReportedIndicator;
    private Date estimatedCompletionDate;
    private Long id;
    private String ownerType;
    private Long ownerId;

    /**
     * Indicator that tells if the SLA has been broken
     * @return Integer
     */
    public Integer getSLABreakReportedInd() {
        return this.breakReportedIndicator;
    }

    /**
     * Sets the SLA Break reported Ind
     * @param pInd
     */
    public void setSLABreakReportedInd(Integer pInd) {
        this.breakReportedIndicator = pInd;
    }

    /**
     * returns the estimatedCompletionDate
     */
    public Date getEstimatedCompletionDate() {
        return this.estimatedCompletionDate;
    }

    /**
     * Sets the estimatedCompletionDate
     * @param estimatedCompletionDate
     */
    public void setEstimatedCompletionDate(Date estimatedCompletionDate) {
        this.estimatedCompletionDate = estimatedCompletionDate;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOwnerType() {
        return ownerType;
    }

    public void setOwnerType(String ownerType) {
        this.ownerType = ownerType;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }


}
