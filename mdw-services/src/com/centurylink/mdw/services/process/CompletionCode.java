/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services.process;

import com.centurylink.mdw.bpm.EventTypeDocument;
import com.centurylink.mdw.model.data.event.EventType;
import com.centurylink.mdw.model.data.work.WorkStatus;

public class CompletionCode {

	private Integer activityInstanceStatus;
	private EventTypeDocument.EventType.Enum eventType;
	private String completionCode;

	public CompletionCode() {
		this.activityInstanceStatus = null;
	}

    public void parse(String compCode) {
    	if (compCode!=null) {
        	int k2 = compCode.indexOf("::");
        	if (k2>0) {
        		String v = compCode.substring(0,k2);
        		compCode = compCode.substring(k2+2);
        		for (int j=0; j<WorkStatus.allStatusNames.length; j++) {
        			if (v.equals(WorkStatus.allStatusNames[j])) {
        				activityInstanceStatus = WorkStatus.allStatusCodes[j];
        				break;
        			}
        		}
        	}
        	int k = compCode.indexOf(':');
    		if (k<0) {
    			eventType = EventTypeDocument.EventType.Enum.forString(compCode);
    			if (eventType!=null) compCode = null;
    			else {
    				if (compCode.length()==0) compCode = null;
    				eventType = EventTypeDocument.EventType.FINISH;
    			}
    		} else {
    			String eventName = compCode.substring(0,k);
    			eventType = EventTypeDocument.EventType.Enum.forString(eventName);
    			if (eventType!=null) {
    				compCode = compCode.substring(k+1);
    				if (compCode.length()==0) compCode = null;
    			} else eventType = EventTypeDocument.EventType.FINISH;
    		}
        } else {
        	eventType = EventTypeDocument.EventType.FINISH;
        }
    	if (activityInstanceStatus==null && eventType.equals(EventTypeDocument.EventType.ERROR))
    		activityInstanceStatus = WorkStatus.STATUS_FAILED;
    	completionCode = compCode;
    }

    public String getCompletionCode() {
    	return this.completionCode;
    }

    public void setCompletionCode(String code) {
    	if (code!=null && code.length()>0) this.completionCode = code;
    	else this.completionCode = null;
    }

    public Integer getActivityInstanceStatus() {
    	return this.activityInstanceStatus;
    }

    public EventTypeDocument.EventType.Enum getXmlEventType() {
    	return eventType;
    }

    public Integer getEventType() {
    	String en = eventType.toString();
    	for (int i=0; i<EventType.allEventTypeNames.length; i++) {
    		if (en.equals(EventType.allEventTypeNames[i])) return EventType.allEventTypes[i];
    	}
    	return null;
    }

    public String getEventTypeName() {
    	return eventType.toString();
    }

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		if (activityInstanceStatus!=null && !activityInstanceStatus.equals(WorkStatus.STATUS_COMPLETED)) {
			for (int j=0; j<WorkStatus.allStatusCodes.length; j++) {
    			if (activityInstanceStatus.equals(WorkStatus.allStatusCodes[j])) {
    				sb.append(WorkStatus.allStatusNames[j]).append("::");
    				break;
    			}
    		}
		}
		if (eventType!=null && !eventType.equals(EventTypeDocument.EventType.FINISH)) {
			sb.append(eventType.toString()).append(":");
		}
		if (completionCode!=null) sb.append(completionCode);
		return sb.length()>0?sb.toString():null;
	}



}
