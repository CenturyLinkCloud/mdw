/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.model.value.variable;

import java.io.Serializable;
import java.util.Date;

import com.centurylink.mdw.common.translator.VariableTranslator;
import com.centurylink.mdw.model.value.process.PackageVO;

public class DocumentVO implements Serializable {

    public static final long serialVersionUID = 1L;

    private Long documentId;
    private Long processInstanceId;
    private Date createDate;
    private Date modifyDate;
    private String documentType;
    private String searchKey1, searchKey2;
    private String ownerType;
    private Long ownerId;
    private String content;		// content in string format
    private Object object;		// content in object format

    /**
     * This is deprecated; use getObject(String type, PackageVO pkg)
     * @return
     */
    @Deprecated
    public Object getObject(String type) {
        return getObject(type, null);
	}

    public Object getObject(String type, PackageVO pkg) {
    	if (type==null || type.equals(documentType)) {
    		if (object==null && content!=null) {
    			object = VariableTranslator.realToObject(pkg, documentType, content);
    		}
    	} else {
    		if (content!=null) {
    			documentType = type;
    			object = VariableTranslator.realToObject(pkg, documentType, content);
    		} else if (object!=null) {
    			content = VariableTranslator.realToString(pkg, documentType, object);
    			documentType = type;
    			object = VariableTranslator.realToObject(pkg, documentType, content);
    		}
    	}
		return object;
	}

    /**
     * This is deprecated, need to use setObject(Object object, String type)
     */
    public void setObject(Object object) {
		setObject(object, null);
	}
	public void setObject(Object object, String type) {
		this.object = object;
		if (type!=null) documentType = type;
		content = null;
	}
	public Long getDocumentId() {
        return documentId;
    }
    public void setDocumentId(Long documentId) {
        this.documentId = documentId;
    }
    public Long getProcessInstanceId() {
        return processInstanceId;
    }
    public void setProcessInstanceId(Long processInstanceId) {
        this.processInstanceId = processInstanceId;
    }
    public Date getCreateDate() {
        return createDate;
    }
    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }
    public Date getModifyDate() {
        return modifyDate;
    }
    public void setModifyDate(Date modifyDate) {
        this.modifyDate = modifyDate;
    }
    public String getDocumentType() {
        return documentType;
    }
    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }
    public String getSearchKey1() {
        return searchKey1;
    }
    public void setSearchKey1(String searchKey1) {
        this.searchKey1 = searchKey1;
    }
    public String getSearchKey2() {
        return searchKey2;
    }
    public void setSearchKey2(String searchKey2) {
        this.searchKey2 = searchKey2;
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
    public String getContent() {
        if (content==null && object!=null) {
            content = VariableTranslator.realToString(documentType, object);
        }
        return content;
    }
    public String getContent(PackageVO pkg) {
        if (content==null && object!=null) {
            content = VariableTranslator.realToString(pkg, documentType, object);
        }
        return content;
    }
    public void setContent(String content) {
        this.content = content;
        object = null;
    }

    /**
     * Forces reserialization from object.
     */
    public void clearStringContent() {
        this.content = null;
    }

}
