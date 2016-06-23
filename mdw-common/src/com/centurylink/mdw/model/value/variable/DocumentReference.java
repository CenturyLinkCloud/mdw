/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.model.value.variable;

import java.io.Serializable;

public class DocumentReference implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private Long documentId;
    private String server;		// for remote document reference
    
    public DocumentReference(Long documentId, String server) {
        this.documentId = documentId;
        this.server = server;
    }
    
    public DocumentReference(String stringRep) {
        int at = stringRep.indexOf('@');
        if (at > 0) {
            documentId = new Long(stringRep.substring(9, at));
            server = stringRep.substring(at + 1);
        }
        else {
            documentId = new Long(stringRep.substring(9));
        }
    }
    
//    public DocumentReference(DocumentVO doc) {
//        documentId = doc.getDocumentId();
//        server = null;
//    }
    
    @Override
    public String toString() {
        if (server==null) return "DOCUMENT:" + documentId.toString();
        else return "DOCUMENT:" + documentId.toString() + "@" + server;
    }
    
    public Long getDocumentId() {
        return documentId;
    }
    
    public String getServer() {
        return server;
    }

}
