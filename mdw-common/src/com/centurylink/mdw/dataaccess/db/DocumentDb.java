package com.centurylink.mdw.dataaccess.db;

public interface DocumentDb {

    boolean isEnabled();

    void initializeDbClient();

    String getDocumentContent(String ownerType, Long documentId);

    void createDocument(String ownerType, Long documentId, String content);

    /**
     * Find and update existing document and return true if exists.
     */
    boolean updateDocument(String ownerType, Long documentId, String content);

    boolean deleteDocument(String ownerType, Long documentId);
}
