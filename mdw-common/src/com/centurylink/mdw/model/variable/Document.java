/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.model.variable;

import java.io.Serializable;
import java.util.Date;

import com.centurylink.mdw.model.workflow.Package;
import com.centurylink.mdw.translator.VariableTranslator;

public class Document implements Serializable {

    private Long documentId;
    private Date createDate;
    private Date modifyDate;
    private String documentType;
    private String ownerType;
    private Long ownerId;
    private String content;        // content in string format
    private Object object;        // content in object format

    /**
     * This is deprecated; use getObject(String type, PackageVO pkg)
     * @return
     */
    @Deprecated
    public Object getObject(String type) {
        return getObject(type, null);
    }

    public Object getObject(String type, Package pkg) {
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

    @Deprecated
    public String getContent() {
        return getContent(null);
    }
    public String getContent(Package pkg) {
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
