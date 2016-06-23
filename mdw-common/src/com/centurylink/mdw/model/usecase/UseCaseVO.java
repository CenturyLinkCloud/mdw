/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.model.usecase;

import java.util.ArrayList;
import java.util.List;

import com.centurylink.mdw.model.value.attribute.RuleSetVO;
import com.qwest.mbeng.DomDocument;
import com.qwest.mbeng.FormatDom;
import com.qwest.mbeng.MbengException;
import com.qwest.mbeng.MbengNode;

public class UseCaseVO {
	
    public static String[] referenceTypes = {
    	"Request XSD", "Response XSD", "Sample Request", "Sample Response",
    	"Requirement Document", "Architecture Document", "Test Cases",
    	"Other"};
	
	private static final String TAG_ROOT = "UseCase";
	private static final String TAG_TITLE = "Title";
	private static final String TAG_INTRO = "Introduction";
	private static final String TAG_STEP = "Step";
	private static final String TAG_PROCESS_NAME = "ProcessName";
	private static final String TAG_PROCESS_VERSION = "ProcessVersion";
	private static final String TAG_ACTIVITY = "Activity";
	private static final String TAG_CONTENT = "Content";
	private static final String TAG_NAME = "Name";
	private static final String TAG_TYPE = "Type";
	private static final String TAG_URL = "URL";
	private static final String TAG_EXCEPTION_HANDLING = "ExceptionHandling";
	private static final String TAG_REFERENCES = "References";
	private static final String TAG_REFERENCE = "Reference";

	private List<UseCaseStepVO> steps;
	private boolean dirty;
	private RuleSetVO resource;
	private String title;
	private String introduction;
	private List<Reference> references;
	
	public UseCaseVO(RuleSetVO resource) {
		dirty = false;
		this.resource = resource;
		steps = new ArrayList<UseCaseStepVO>();
		references = null;
		if (resource.getRuleSet().length()==0) {
			addStep(0, "Pre-condition", null, 0, "", "(include pre-condition here)");
			addStep(1, "Post-condition", null, 0, "", "(include post-condition here)");
			title = resource.getName();
		} else {
			load(resource.getRuleSet());
		}
	}
	
	public List<UseCaseStepVO> getSteps() {
		return steps;
	}

	public boolean isDirty() {
		return dirty;
	}

	public void setDirty(boolean dirty) {
		this.dirty = dirty;
	}
	
	public void setName(String name) {
		resource.setName(name);
	}
	
	public String getName() {
		return resource.getName();
	}
	
	public void removeStep(int seqno) {
		steps.remove(seqno);
	}
	
	public void addStep(int seqno, String title, String procname, int version, String actname, String content) {
		UseCaseStepVO step = new UseCaseStepVO();
		step.setTitle(title);
		step.setProcessName(procname);
		step.setActivityId(actname);
		step.setRequirement(content);
		step.setProcessVersion(version);
		steps.add(seqno, step);
	}
	
	public RuleSetVO getResource() {
		resource.setRuleSet(getXml());
		return resource;
	}
	
	public Long getId() {
		return resource.getId();
	}
	
	public void setId(Long id) {
		resource.setId(id);
	}
	
	private void load(String reportXml) {
		FormatDom fmter = new FormatDom();
		DomDocument doc = new DomDocument();
		try {
			fmter.load(doc, reportXml);
			MbengNode node;
			for (node=doc.getRootNode().getFirstChild();node!=null; node=node.getNextSibling()) {
				if (node.getName().equals(TAG_TITLE))
					title = node.getValue();
				else if (node.getName().equals(TAG_INTRO))
					introduction = node.getValue();
				else if (node.getName().equals(TAG_REFERENCES)) {
					MbengNode d;
					for (d=node.getFirstChild(); d!=null; d=d.getNextSibling()) {
						MbengNode name = d.findChild(TAG_NAME);
						MbengNode type = d.findChild(TAG_TYPE);
						MbengNode url = d.findChild(TAG_URL);
						addReference(name.getValue(), type.getValue(), url.getValue());
					}
				} else if (node.getName().equals(TAG_STEP)) {
					UseCaseStepVO m = new UseCaseStepVO();
					MbengNode n, d;
					for (n=node.getFirstChild();n!=null; n=n.getNextSibling()) {
						if (n.getName().equals(TAG_PROCESS_NAME)) m.setProcessName(n.getValue());
						else if (n.getName().equals(TAG_PROCESS_VERSION)) 
							m.setProcessVersion(Integer.parseInt(n.getValue()));
						else if (n.getName().equals(TAG_ACTIVITY)) m.setActivityId(n.getValue());
						else if (n.getName().equals(TAG_CONTENT)) m.setRequirement(n.getValue());
						else if (n.getName().equals(TAG_TITLE)) m.setTitle(n.getValue());
						else if (n.getName().equals(TAG_EXCEPTION_HANDLING)) 
							m.setExceptionHandling(n.getValue());
						else if (n.getName().equals(TAG_REFERENCES)) {
							for (d=n.getFirstChild(); d!=null; d=d.getNextSibling()) {
								MbengNode name = d.findChild(TAG_NAME);
								MbengNode type = d.findChild(TAG_TYPE);
								MbengNode url = d.findChild(TAG_URL);
								m.addReference(name.getValue(), type.getValue(), url.getValue());
							}
						}
						
					}
					steps.add(m);
				}
			}
		} catch (MbengException e) {
			// just do not load contents
		}
	}

	 private String getXml() {
	        FormatDom fmter = new FormatDom();
	        DomDocument doc = new DomDocument();
	        MbengNode root = doc.getRootNode();
	        root.setName(TAG_ROOT);
            MbengNode node = doc.newNode(TAG_TITLE,title, "X", ' ');
	        root.appendChild(node);
	        node = doc.newNode(TAG_INTRO,introduction, "X", ' ');
	        root.appendChild(node);
	        MbengNode reqresp, data;
	        if (getReferences()!=null) {
            	node = doc.newNode(TAG_REFERENCES, "", "X", ' ');
            	root.appendChild(node);
            	for (Reference attr : getReferences()) {
            		MbengNode attrnode = doc.newNode(TAG_REFERENCE, "", "A", ' ');
            		node.appendChild(attrnode);
            		data = doc.newNode(TAG_NAME, attr.getName(), "A", ' ');
            		attrnode.appendChild(data);
            		data = doc.newNode(TAG_TYPE, attr.getType(), "A", ' ');
            		attrnode.appendChild(data);
            		data = doc.newNode(TAG_URL, attr.getUrl(), "A", ' ');
            		attrnode.appendChild(data);
            	}
            }
	        for (UseCaseStepVO m : steps) {
	            node = doc.newNode(TAG_STEP, "", "X", ' ');
	            root.appendChild(node);
	            node.appendChild(doc.newNode(TAG_PROCESS_NAME, m.getProcessName(), "X", ' '));
	            node.appendChild(doc.newNode(TAG_PROCESS_VERSION, Integer.toString(m.getProcessVersion()), "X", ' '));
	            node.appendChild(doc.newNode(TAG_ACTIVITY, m.getActivityId(), "X", ' '));
	            node.appendChild(doc.newNode(TAG_CONTENT, m.getRequirement(), "X", ' '));
	            node.appendChild(doc.newNode(TAG_TITLE, m.getTitle(), "X", ' '));
	            node.appendChild(doc.newNode(TAG_CONTENT, m.getRequirement(), "X", ' '));
	            node.appendChild(doc.newNode(TAG_EXCEPTION_HANDLING, m.getExceptionHandling(), "X", ' '));
	            if (m.getReferences()!=null) {
	            	reqresp = doc.newNode(TAG_REFERENCES, "", "X", ' ');
	            	node.appendChild(reqresp);
	            	for (Reference attr : m.getReferences()) {
	            		MbengNode attrnode = doc.newNode(TAG_REFERENCE, "", "A", ' ');
	            		reqresp.appendChild(attrnode);
	            		data = doc.newNode(TAG_NAME, attr.getName(), "A", ' ');
	            		attrnode.appendChild(data);
	            		data = doc.newNode(TAG_TYPE, attr.getType(), "A", ' ');
	            		attrnode.appendChild(data);
	            		data = doc.newNode(TAG_URL, attr.getUrl(), "A", ' ');
	            		attrnode.appendChild(data);
	            	}
	            }
	        }
	        return fmter.format(doc);
	    }

	public String getIntroduction() {
		return introduction;
	}

	public void setIntroduction(String introduction) {
		this.introduction = introduction;
	}
	
	public List<Reference> getReferences() {
		return references;
	}
	public void setReferences(List<Reference> references) {
		this.references = references;
	}
	
	public void addReference(String name, String type, String url) {
		Reference ref = new Reference();
		ref.name = name;
		ref.type = type;
		ref.url = url;
		if (references==null) references = new ArrayList<Reference>();
		references.add(ref);
	}
	
	public void removeReference(int i) {
		references.remove(i);
	}
	
	public void updateReference(int i, String name, String type, String url) {
		Reference ref = references.get(i);
		ref.name = name;
		ref.type = type;
		ref.url = url;
	}
	
	public static class Reference {
		private String name;
		private String type;
		private String url;
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public String getType() {
			return type;
		}
		public void setType(String type) {
			this.type = type;
		}
		public String getUrl() {
			return url;
		}
		public void setUrl(String url) {
			this.url = url;
		}
	}

}
