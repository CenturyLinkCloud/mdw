/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.model.monitor;

import com.centurylink.mdw.constant.OwnerType;
import com.centurylink.mdw.util.StringHelper;

import java.io.Serializable;
import java.util.Date;

public class ServiceLevelAgreement implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String OWNER_PROCESS =  OwnerType.PROCESS;
    public static final String OWNER_ACTIVITY =  OwnerType.ACTIVITY;
    public static final String OWNER_ACTIVITY_IMPLEMENTOR =  OwnerType.ACTIVITY_IMPLEMENTOR;
    public static final String OWNER_TASK = OwnerType.TASK;

    public static final String INTERVAL_SECONDS = "Seconds";
    public static final String INTERVAL_MINUTES = "Minutes";
    public static final String INTERVAL_HOURS = "Hours";
    public static final String INTERVAL_DAYS = "Days";
    
    private float hours;
    private Date startDate, endDate;
    private Long id;

    /**
    * Returns the SLA in hrs
    * @return Integer
    */
    public Float getSLAInHours() { return hours; }


    /**
    * Sets the SLA in hrs
    * @param pHrs
    */
   public void setSLAInHours(Float pHrs) { hours = pHrs; }
   
   public Date getSlaStartDate() { return startDate; }
   
   public Date getSlaEndDate() { return endDate; }
   
   public void setSlaStartDate(Date startDate) { this.startDate = startDate; }
   
   public void setSlaEndDate(Date endDate) { this.endDate = endDate; }

   public Long getId() { return id; }

   public void setId(Long id) { this.id = id; }
   
   /**
    * Convert interval of specified unit to seconds
    * @param interval
    * @param unit
    * @return
    */
   public static int unitsToSeconds(String interval, String unit) {
       if (StringHelper.isEmpty(interval)) return 0;
       else if (unit == null) return (int)(Double.parseDouble(interval));
       else if (unit.equals(INTERVAL_DAYS)) return (int)(Double.parseDouble(interval)*86400);
       else if (unit.equals(INTERVAL_HOURS)) return (int)(Double.parseDouble(interval)*3600);
       else if (unit.equals(INTERVAL_MINUTES)) return (int)(Double.parseDouble(interval)*60);
       else return (int)(Double.parseDouble(interval));
   }
   
   /**
    * Convert seconds to specified units
    * @param seconds
    * @param unit
    * @return
    */
   public static String secondsToUnits(int seconds, String unit) {
       if (unit == null) return String.valueOf(seconds);
       else if (unit.equals(INTERVAL_DAYS)) return String.valueOf(Math.round(seconds/86400));
       else if (unit.equals(INTERVAL_HOURS)) return String.valueOf(Math.round(seconds/3600));
       else if (unit.equals(INTERVAL_MINUTES)) return String.valueOf(Math.round(seconds/60));
       else  return String.valueOf(seconds);
   }

}
