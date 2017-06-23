/**
 * Copyright (c) 2017 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.tom.model;

import java.util.Date;
import java.util.Map;

import javax.validation.constraints.Size;

import org.json.JSONObject;

import com.centurylink.mdw.model.Jsonable;

public class WorkItem implements Jsonable {
    
    public WorkItem(JSONObject json) {
        bind(json);    
    }
    
    private Map<String,String> attributes;
    public Map<String,String> getAttributes() { return attributes; }
    public void setAttributes(Map<String,String> attributes) { this.attributes = attributes; }
    
    private Work work;
    public Work getWork() { return work; }
    public void setWork(Work work) { this.work = work; }
   
    private Duration plannedDuration;
    public Duration getPlannedDuration() { return plannedDuration; }
    public void setPlannedDuration(Duration plannedDuration) { this.plannedDuration = plannedDuration; }
    
    public class Work implements Jsonable {
        
        public Work(JSONObject json) {
            bind(json);    
        }
        
        private String action;
        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
        
        @Size(max=5)
        private int quantity;
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
    }
    
    public class Duration implements Jsonable {
        
        public Duration(JSONObject json) {
            bind(json);    
        }
        
        private Date start;
        public Date getStart() { return start; }
        public void setStart(Date start) { this.start = start; }
        
        private Date end;
        public Date getEnd() { return end; }
        public void setEnd(Date end) { this.end = end; }
        
        private String timeZone;
        public String getTimeZone() { return timeZone; }
        public void setTimeZone(String timeZone) { this.timeZone = timeZone; }
        
    }        
}
