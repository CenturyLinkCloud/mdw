/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.model.variable;

import java.io.Serializable;

public class DocumentReference implements Serializable {

    private Long documentId;

    public DocumentReference(Long documentId) {
        this.documentId = documentId;
    }

    public DocumentReference(String stringRep) {
        documentId = new Long(stringRep.substring(9));
    }

    public String toString() {
        return "DOCUMENT:" + documentId;
    }

    public Long getDocumentId() {
        return documentId;
    }
}
