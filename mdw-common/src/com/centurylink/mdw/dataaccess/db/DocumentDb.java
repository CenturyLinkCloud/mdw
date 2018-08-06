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

    void deleteDocument(String ownerType, Long documentId);
}
