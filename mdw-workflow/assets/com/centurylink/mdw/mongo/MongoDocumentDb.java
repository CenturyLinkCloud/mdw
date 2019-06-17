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
package com.centurylink.mdw.mongo;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Projections.excludeId;
import static com.mongodb.client.model.Projections.fields;
import static com.mongodb.client.model.Projections.include;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.mongodb.*;
import org.bson.Document;
import org.bson.json.JsonWriterSettings;

import com.centurylink.mdw.dataaccess.db.DocumentDb;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.timer.CodeTimer;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;

public class MongoDocumentDb implements DocumentDb {

    private static final String MDW_PREFIX = "MDW_";

    // Optional URI specified options (for replica or shard sets)
    protected static final String MAX_POOL_SIZE = "maxPoolSize";
    protected static final String READ_CONCERN = "readConcernLevel";
    protected static final String WRITE_CONCERN = "w";
    protected static final String READ_PREFERENCE = "readPreference";


    private static String dbName;
    public String getDbName() { return dbName; }
    public void setDbName(String name) { this.dbName = name; }

    private String dbHost;
    public String getDbHost() { return dbHost; }
    public void setDbHost(String host) { this.dbHost = host; }

    private int dbPort;
    public int getDbPort() { return dbPort; }
    public void setDbPort(int port) { this.dbPort = port; }

    private int maxConnections;
    public int getMaxConnections() { return maxConnections; }
    public void setMaxConnections(int maxConns) { this.maxConnections = maxConns; }

    private static MongoClient mongoClient;
    private static Map<String,Boolean> collectionDocIdIndexed = new ConcurrentHashMap<>();

    public static MongoDatabase getMongoDb() {

        return getMongoDb(dbName);   // Default database within Mongo instance
    }

    public static MongoDatabase getMongoDb(String databaseName) {
        return mongoClient == null ? null : mongoClient.getDatabase(databaseName);
    }

    @Override
    public boolean isEnabled() {
        return dbHost != null;
    }

    @Override
    public void initializeDbClient() {
        if (mongoClient == null) {
            MongoClientOptions.Builder options = MongoClientOptions.builder();

            if (dbHost.startsWith("mongodb") && dbHost.contains("://")) {
                boolean needMaxConnections = true;
                boolean needReadConcern = true;
                boolean needWriteConcern = true;
                boolean needReadPreference = true;

                int index = dbHost.indexOf("?");
                if (index > 0 && dbHost.length() > index+2) { // Means URI has options specified
                    for (String entry : dbHost.substring(index+1).split("[&;]")) {
                        String[] option = entry.split("=");
                        if (option.length == 2 && option[1] != null) {
                            switch (option[0]) {
                                case MAX_POOL_SIZE:
                                    needMaxConnections = false;
                                    break;
                                case READ_CONCERN:
                                    needReadConcern = false;
                                    break;
                                case WRITE_CONCERN:
                                    needWriteConcern = false;
                                    break;
                                case READ_PREFERENCE:
                                    needReadPreference = false;
                                    break;
                                default:
                                    break;
                            }
                        }
                    }
                }
                // Apply default option needed for replica sets whenever not specified in URI
                if (needMaxConnections)
                    options = options.connectionsPerHost(maxConnections);
                if (needReadConcern)
                    options = options.readConcern(ReadConcern.MAJORITY);
                if (needWriteConcern)
                    options = options.writeConcern(WriteConcern.MAJORITY);
                if (needReadPreference)
                    options = options.readPreference(ReadPreference.primary());

                mongoClient = new MongoClient(new MongoClientURI(dbHost, options));
                dbPort = 0;
            }
            else {
                mongoClient = new MongoClient(new ServerAddress(dbHost, dbPort), options.connectionsPerHost(maxConnections).writeConcern(WriteConcern.ACKNOWLEDGED).build());
            }

            if (MongoDocumentDb.getMongoDb() != null)
                for (String name : MongoDocumentDb.getMongoDb().listCollectionNames()) {
                    if (name.startsWith(MDW_PREFIX)) {
                        boolean needsIndex = true;
                        for (Document doc : MongoDocumentDb.getMongoDb().getCollection(name).listIndexes()) {
                            if ("document_id_1".equals(doc.getString("name"))) {
                                needsIndex = false;
                                collectionDocIdIndexed.putIfAbsent(name, true);
                            }
                        }
                        if (needsIndex)
                            createMongoDocIdIndex(name);
                    }
                }
            LoggerUtil.getStandardLogger().info(mongoClient.getMongoClientOptions().toString());
        }
    }

    private String getCollectionName(String ownerType) {
        return MDW_PREFIX + ownerType;
    }

    @Override
    public String getDocumentContent(String ownerType, Long documentId) {
        CodeTimer timer = new CodeTimer("Load from documentDb", true);
        try {
            MongoCollection<Document> mongoCollection = getMongoDb().getCollection(getCollectionName(ownerType));
            org.bson.Document mongoQuery = new org.bson.Document("document_id", documentId);
            org.bson.Document c = mongoCollection.find(mongoQuery).limit(1).projection(fields(include("CONTENT","isJSON"), excludeId())).first();
            if (c == null) {
                return null;
            }
            else if (c.getBoolean("isJSON", false)) {
                return decodeMongoDoc(c.get("CONTENT", org.bson.Document.class)).toJson(JsonWriterSettings.builder().indent(true).build());
            }
            else {
                return c.getString("CONTENT");
            }
        }
        finally {
            timer.stopAndLogTiming(null);
        }
    }

    @Override
    public void createDocument(String ownerType, Long documentId, String content) {
        String collectionName = getCollectionName(ownerType);
        MongoCollection<org.bson.Document> collection = getMongoDb().getCollection(collectionName);
        org.bson.Document myDoc = null;
        if (content != null && content.startsWith("{")) {
            try {
                // Parse JSON to create BSON CONTENT Document
                org.bson.Document myJsonDoc = org.bson.Document.parse(content);
                if (!myJsonDoc.isEmpty()) {
                    if (content.contains(".") || content.contains("$"))
                        myJsonDoc = encodeMongoDoc(myJsonDoc);
                    // Plus append _id and isJSON:true field
                    myDoc = new org.bson.Document("CONTENT", myJsonDoc).append("document_id", documentId).append("isJSON", true);
                }
            }
            catch (Throwable ex) { myDoc=null; }  // Assume not JSON then
        }
        if (myDoc == null) {
            // Create BSON document with Raw content if it wasn't JSON, plus append _id and isJSON:false
            myDoc = new org.bson.Document("CONTENT", content).append("document_id", documentId).append("isJSON", false);
        }

        collection.insertOne(myDoc);
        if (!checkForDocIdIndex(collectionName))
            createMongoDocIdIndex(collectionName);
    }


    @Override
    public boolean updateDocument(String ownerType, Long documentId, String content) {
        MongoCollection<org.bson.Document> collection = getMongoDb().getCollection(getCollectionName(ownerType));
        org.bson.Document myDoc = null;
        if (content != null && content.startsWith("{")) {
            try {
                // Parse JSON to create BSON CONTENT Document
                org.bson.Document myJsonDoc = org.bson.Document.parse(content);
                if (!myJsonDoc.isEmpty()) {
                    if (content.contains(".") || content.contains("$"))
                        myJsonDoc = encodeMongoDoc(myJsonDoc);
                    // Plus append isJSON:true field
                    myDoc = new org.bson.Document("CONTENT", myJsonDoc).append("document_id", documentId).append("isJSON", true);
                }
            }
            catch (Throwable ex) {myDoc=null;}  // Assume not JSON then
        }
        if (myDoc == null)   // Create BSON document with Raw content if it wasn't JSON plus append isJSON:false
            myDoc = new org.bson.Document("CONTENT", content).append("document_id", documentId).append("isJSON", false);
        if (collection.findOneAndReplace(eq("document_id", documentId), myDoc) != null)
            return true;
        else {  // Must have been null content initially and being updated now to non-null
            collection.insertOne(myDoc);
            return true;
        }
    }

    @Override
    public boolean deleteDocument(String ownerType, Long documentId) {
        MongoCollection<org.bson.Document> collection = getMongoDb().getCollection(getCollectionName(ownerType));
        return collection.findOneAndDelete(eq("document_id", documentId)) != null;
    }

    public static void createMongoDocIdIndex(String collectionName) {
        try {
            IndexOptions indexOptions = new IndexOptions().unique(true).background(true).name("document_id_1");
            MongoCollection<org.bson.Document> collection = MongoDocumentDb.getMongoDb().getCollection(collectionName);
            String indexName = collection.createIndex(Indexes.ascending("document_id"), indexOptions);
            LoggerUtil.getStandardLogger().mdwDebug("Created Index : " + indexName + " on collection : " + collectionName);
            collectionDocIdIndexed.putIfAbsent(collectionName, true);
        }
        catch (Exception e) {
            LoggerUtil.getStandardLogger().info("Failed to create index for 'document_id' on " + collectionName + " collection", e);
        }
    }

    /**
     * MongoDB doesn't allow keys to have dots (.) or to start with $.  This method encodes such keys if found
     * and returns a new BSON document
     */
    @SuppressWarnings("unchecked")
    public static org.bson.Document encodeMongoDoc(org.bson.Document doc) {
        org.bson.Document newDoc = new org.bson.Document();
        for (String key : doc.keySet()) {
            Object value = doc.get(key);
            if (value instanceof org.bson.Document)
                value = encodeMongoDoc(doc.get(key, org.bson.Document.class));
            else if (value instanceof List<?>) {
                for (int i=0; i < ((List<?>)value).size(); i++) {
                    Object obj = ((List<?>)value).get(i);
                    if (obj instanceof org.bson.Document)
                        ((List<org.bson.Document>)value).set(i, encodeMongoDoc((org.bson.Document)obj));
                }
            }

            String newKey = key;
            if (key.startsWith("$"))
                newKey = "\\uff04" + key.substring(1);
            if (key.contains(".")) {
                newKey = newKey.replace(".", "\\uff0e");
            }
            newDoc.put(newKey, value);
        }
        return newDoc;
    }

    /**
     * MongoDB doesn't allow keys to have dots (.) or to start with $.  This method decodes such keys back to dots and $ if found
     * and returns a new BSON document
     */
    @SuppressWarnings("unchecked")
    public static org.bson.Document decodeMongoDoc(org.bson.Document doc) {
        org.bson.Document newDoc = new org.bson.Document();
        for (String key : doc.keySet()) {
            Object value = doc.get(key);
            if (value instanceof org.bson.Document)
                value = decodeMongoDoc(doc.get(key, org.bson.Document.class));
            else if (value instanceof List<?>) {
                for (int i=0; i < ((List<?>)value).size(); i++) {
                    Object obj = ((List<?>)value).get(i);
                    if (obj instanceof org.bson.Document)
                        ((List<org.bson.Document>)value).set(i, decodeMongoDoc((org.bson.Document)obj));
                }
            }

            String newKey = key;
            if (key.startsWith("\\uff04"))
                newKey = "$" + key.substring(6);
            if (key.contains("\\uff0e")) {
                newKey = newKey.replace("\\uff0e", ".");
            }
            newDoc.put(newKey, value);
        }
        return newDoc;
    }

    public static boolean checkForDocIdIndex(String collectionName) {
        Boolean value = collectionDocIdIndexed.get(collectionName);
        return value == null ? false : value;
    }

    public String toString() {
        return (dbPort == 0 ? dbHost : dbHost + ":" + dbPort) + "/" + dbName;
    }


}
