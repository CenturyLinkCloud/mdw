/*
 * Copyright (C) 2018 CenturyLink, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
                documentDb.createDocument(doc.getOwnerType(), doc.getDocumentId(), content);
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
            String content = documentDb.getDocumentContent(doc.getOwnerType(), doc.getDocumentId());
            if (content != null) {
                documentDb.deleteDocument(doc.getOwnerType(), doc.getDocumentId());
                documentDb.createDocument(newOwnerType, doc.getDocumentId(), content);
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
