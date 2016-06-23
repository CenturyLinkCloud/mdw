/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.model.usecase;

import java.util.ArrayList;
import java.util.List;

import com.centurylink.mdw.model.usecase.UseCaseVO.Reference;

public class UseCaseStepVO {
	
	private String processName;
	private String activityId;
	private int processVersion;
	private String requirement;
	private String exceptionHandling;
	private String title;
	private List<Reference> references;
	
	public String getProcessName() {
		return processName;
	}
	public void setProcessName(String processName) {
		this.processName = processName;
	}
	public String getActivityId() {
		return activityId;
	}
	public void setActivityId(String activityId) {
		this.activityId = activityId;
	}
	public int getProcessVersion() {
		return processVersion;
	}
	public void setProcessVersion(int processVersion) {
		this.processVersion = processVersion;
	}
	public String getRequirement() {
		return requirement;
	}
	public void setRequirement(String content) {
		this.requirement = content;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public List<Reference> getReferences() {
		return references;
	}
	public void setReferences(List<Reference> references) {
		this.references = references;
	}
	
	public String getProcessNameVersion() {
		return processName + " - V" + (processVersion/1000) + "." + (processVersion%1000);
	}
	
	public void setProcessNameVersion(String v) {
		int k = v.indexOf(" - V");
		if (k>0) {
			processName = v.substring(0,k);
			v = v.substring(k+4);
			k = v.indexOf(".");
			processVersion = Integer.parseInt(v.substring(0,k))*1000 +
				Integer.parseInt(v.substring(k+1));
		} else {	// should not be possible
			processName = v;
		}
	}
	
	public void setExceptionHandling(String content) {
		exceptionHandling = content;
	}
	
	public String getExceptionHandling() {
		return exceptionHandling;
	}
	
	public void addReference(String name, String type, String url) {
		Reference ref = new Reference();
		ref.setName(name);
		ref.setType(type);
		ref.setUrl(url);
		if (references==null) references = new ArrayList<Reference>();
		references.add(ref);
	}
	
	public void removeReference(int i) {
		references.remove(i);
	}
	
	public void updateReference(int i, String name, String type, String url) {
		Reference ref = references.get(i);
		ref.setName(name);
		ref.setType(type);
		ref.setUrl(url);
	}
}
