package com.centurylink.mdw.dataaccess;

import com.centurylink.mdw.dataaccess.db.DocumentDb;
import com.centurylink.mdw.model.variable.Document;
import com.centurylink.mdw.model.workflow.Package;

public class DocumentDbAccess {

    private DocumentDb documentDb;

    public DocumentDbAccess(DocumentDb documentDb) {
        this.documentDb = documentDb;
    }

    public boolean hasDocumentDb() {
        return documentDb != null;
    }

    public void createDocument(Document doc, Package pkg) {
        if (documentDb != null) {
            String content = doc.getContent(pkg);
            if (content != null)
                documentDb.createDocument(doc.getOwnerType(), doc.getId(), content);
        }
    }

    public boolean updateDocumentContent(String ownerType, Long documentId, String content) {
        if (documentDb != null) {
            if (content == null)  // Remove document if new value/content is null
                return documentDb.deleteDocument(ownerType, documentId);
            else
                return documentDb.updateDocument(ownerType, documentId, content);
        }
        else {
            return false;
        }
    }

    public void updateDocumentDbOwnerType(Document doc, String newOwnerType) {
        if (documentDb != null) {
            String content = documentDb.getDocumentContent(doc.getOwnerType(), doc.getId());
            if (content != null) {
                documentDb.deleteDocument(doc.getOwnerType(), doc.getId());
                documentDb.createDocument(newOwnerType, doc.getId(), content);
            }
        }
    }

    public String getDocumentContent(String ownerType, Long documentId) {
        if (documentDb == null) {
            return null;
        }
        return documentDb.getDocumentContent(ownerType, documentId);
    }
}
