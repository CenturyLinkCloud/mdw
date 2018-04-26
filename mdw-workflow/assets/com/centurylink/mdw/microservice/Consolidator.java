package com.centurylink.mdw.microservice;

import org.json.JSONObject;

import kotlin.Pair;

/**
 * Consolidates responses into a combined code with response json.
 */
@FunctionalInterface
public interface Consolidator {
    public Pair<Integer,JSONObject> getResponse(ServiceSummary serviceSummary);
}