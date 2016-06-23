/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services.mbeng;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.apache.xmlbeans.XmlObject;
import org.w3c.dom.Document;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.model.FormDataDocument;
import com.centurylink.mdw.model.value.variable.DocumentReference;
import com.centurylink.mdw.model.value.variable.DocumentVO;
import com.centurylink.mdw.model.value.variable.VariableVO;
import com.centurylink.mdw.services.process.ProcessExecuter;
import com.qwest.mbeng.DomDocument;
import com.qwest.mbeng.Logger;
import com.qwest.mbeng.MbengDocument;
import com.qwest.mbeng.MbengException;
import com.qwest.mbeng.MbengNode;
import com.qwest.mbeng.MbengRuleSet;
import com.qwest.mbeng.MbengRuntime;
import com.qwest.mbeng.MbengTableArray;
import com.qwest.mbeng.MbengVariable;

public abstract class MbengMDWRuntime extends MbengRuntime {

	private ProcessExecuter engine;
	private HashMap<String,Object> documentMap;
	private HashMap<String,String> vartypeMap;

    public MbengMDWRuntime(MbengRuleSet ruleset, ProcessExecuter engine, Logger logger)
        throws MbengException
    {
        super(ruleset, logger);
        this.engine = engine;
        documentMap = null;
    }

    public MbengMDWRuntime(String ruleName, String ruleText, char ruleType,
            List<VariableVO> vars, ProcessExecuter engine, StandardLogger logger) throws MbengException {
        super(RuleCache.getRuleSet(ruleName, ruleText, ruleType, vars), new MbengMDWLogger(logger));
        this.engine = engine;
        documentMap = null;
    }

    private Object getDocumentValue(DocumentReference docref, String type) throws MbengException {
        try {
            DocumentVO docvo = engine.getDocument(docref, false);
            return docvo==null?null:docvo.getObject(type);
        } catch (Exception e) {
            throw new MbengException("Cannot load document", e);
        }
    }

    private String getDocumentContent(DocumentReference docref) throws MbengException {
        try {
            DocumentVO docvo = engine.getDocument(docref, false);
            return docvo==null?null:docvo.getContent();
        } catch (Exception e) {
            throw new MbengException("Cannot load document", e);
        }
    }

    private void recordMappedObject(String varname, String vartype, Object object) {
    	if (documentMap==null) {
    		documentMap = new HashMap<String,Object>();
    		vartypeMap = new HashMap<String,String>();
    	}
        documentMap.put(varname, object);
        vartypeMap.put(varname, vartype);
    }

    abstract protected Object getParameterValue(MbengVariable var);
    abstract protected String getParameterString(MbengVariable var);
    abstract protected void setParameterString(String varname, String v) throws ActivityException;
    abstract protected void setParameterDocument(String varname, String vartype, Object v) throws ActivityException;
    abstract protected String getPseudoVariableValue(String varname);

    /**
     * This method is called by Magic engine to obtain binding for a variable.
     */
	@Override
	protected Object getBinding(MbengVariable var) {
		Object object;
		if (var instanceof VariableVO) {
			if (var.getKind()==MbengVariable.KIND_DOCUMENT || var.getKind()==MbengVariable.KIND_TABLE) {
				object = documentMap==null?null:documentMap.get(var.getName());
				if (object==null) {
					DocumentReference docref = (DocumentReference)getParameterValue(var);
					String vartype = ((VariableVO)var).getVariableType();
					if (docref!=null) {
						try {
							if (vartype.equals(MbengDocument.class.getName()))
								object = getDocumentValue(docref, vartype);
							else if (vartype.equals(MbengTableArray.class.getName()))
								object = getDocumentValue(docref, vartype);
							else if (vartype.equals(FormDataDocument.class.getName()))
								object = getDocumentValue(docref, vartype);
							else if (vartype.equals(XmlObject.class.getName())) {
								DomDocument domdoc = new DomDocument();
							    domdoc.parse(getDocumentContent(docref));
							    object = domdoc;
							} else if (vartype.equals(Document.class.getName())) {
								DomDocument domdoc = new DomDocument();
							    domdoc.parse(getDocumentContent(docref));
							    object = domdoc;
							} // else cannot handle, including JSON doc
						} catch (MbengException e) {
							super.logline("Get exception when trying to get binding for " + var.getName());
						}
					}
					if (object!=null) recordMappedObject(var.getName(), vartype, object);
				}
			} else {
				object = getParameterString(var);
			}
		} else if (var instanceof PseudoVariableVO) {
			object = this.getPseudoVariableValue(var.getName());
		} else object = super.getBinding(var.getName());
		return object;
	}

	@Override
	protected void setBinding(MbengVariable var, Object object) {
		if (var instanceof VariableVO) {
			if (var.getKind()==MbengVariable.KIND_DOCUMENT || var.getKind()==MbengVariable.KIND_TABLE) {
				String vartype = ((VariableVO)var).getVariableType();
				recordMappedObject(var.getName(), vartype, object);
				// create/update at the end
			} else {
				try {
					if (object instanceof MbengNode) object = ((MbengNode)object).getValue();
					setParameterString(var.getName(), object.toString());
				} catch (ActivityException e) {
					super.logline("Failed to set binding for " + var.getName() +
							" - exception " + e.getMessage());
				}
			}
		} else super.setBinding(var, object);
	}

	public void saveDocuments() throws MbengException {
		if (documentMap==null) return;
		Set<String> varnames = documentMap.keySet();
		// need to sort vars to avoid deadlock! // TODO
		for (String varname : varnames) {
			Object binding = documentMap.get(varname);
			Object object;
			boolean isDirty;
			String vartype = vartypeMap.get(varname);
			if (vartype.equals(MbengDocument.class.getName())) {
				isDirty = ((MbengDocument)binding).isDirty();
				object = binding;
			} else if (vartype.equals(MbengTableArray.class.getName())) {
				isDirty = true;
				object = binding;
			} else if (vartype.equals(FormDataDocument.class.getName())) {
				isDirty = ((FormDataDocument)binding).isDirty();
				object = binding;
			} else if (vartype.equals(XmlObject.class.getName())) {
				DomDocument domdoc = (DomDocument)binding;
				isDirty = domdoc.isDirty();
				object = domdoc.xmlText();
			} else if (vartype.equals(Document.class.getName())) {
				DomDocument domdoc = (DomDocument)binding;
				isDirty = domdoc.isDirty();
				object = domdoc.getXmlDocument();
			} else {
				// cannot handle. TODO add handling for JSON
				isDirty = false;
				object = null;
			}
			if (isDirty) {
				try {
					this.setParameterDocument(varname, vartype, object);
				} catch (ActivityException e) {
					super.logline("Failed to set binding for " + varname +
							" - exception " + e.getMessage());
					throw new MbengException("SaveDocuments", e);
				}
			}
	    }
	}

    public static class PseudoVariableVO implements MbengVariable {
    	String name;

    	public PseudoVariableVO(String name) {
    		this.name = name;
    	}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public int getKind() {
			return MbengVariable.KIND_STRING;
		}

		@Override
		public Object newInstance() throws InstantiationException {
			return null;
		}

    }

}
