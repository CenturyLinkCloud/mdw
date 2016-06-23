/**
 * Copyright (c) 2015 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.artis;

import java.io.Serializable;
import java.util.HashMap;

public class ArtisRecordCache implements Serializable {

    private String requestRecord;
    private HashMap<String, String> calloutRecords;
    private ArtisProcessVariables artisProcessVariables;

    public ArtisRecordCache() {
        calloutRecords = new HashMap<String, String>();
        this.artisProcessVariables = new ArtisProcessVariables();
    }

    public void setRequestRecord(String requestRecord) {
        this.requestRecord = requestRecord;
    }

    public String getRequestRecord() {
        return this.requestRecord;
    }

    public void addCalloutRecord(String key, String calloutRecord) {
        calloutRecords.put(key, calloutRecord);
    }

    public String getCalloutRecord(String key) {
        return this.calloutRecords.get(key);
    }

    public String useCalloutRecord(String key) {
        String response = getCalloutRecord(key);
        calloutRecords.remove(key);
        return response;
    }

    public boolean hasCalloutRecord(String key) {
        return this.calloutRecords.containsKey(key);
    }

    public void setArtisProcessVariables(ArtisProcessVariables artisProcessVariables) {
        this.artisProcessVariables = artisProcessVariables;
    }

    public ArtisProcessVariables getArtisProcessVariables() {
        return this.artisProcessVariables;
    }

    /*
     * For some reason MDW was throwing an error stating I needed to implement
     * an Equals method, so here it is...
     */
    @Override
    public boolean equals(Object object) {
        boolean response = false;

        if (object instanceof ArtisRecordCache) {
            if (this.requestRecord.equals(((ArtisRecordCache) object).getRequestRecord()))
                response = true;
        }

        return response;
    }

}
