package com.centurylink.mdw.microservice;

import org.json.JSONObject;

import com.centurylink.mdw.model.Status;

import kotlin.Pair;

/**
 * Finds and returns the response with the worst (highest)
 * HTTP status code.
 */
public class WorstCaseConsolidator implements Consolidator {

    @Override
    public Pair<Integer,JSONObject> getResponse(ServiceSummary serviceSummary) {
        Status worstStatus = Status.OK;
        for (String microserviceName : serviceSummary.getMicroservices().keySet()) {
            for (MicroserviceInstance microservice : serviceSummary.getMicroservices(microserviceName)) {
                Status status = microservice.latestStatus();
                if (status.getCode() > worstStatus.getCode())
                    worstStatus = status;
            }
        }
        return new Pair<>(worstStatus.getCode(), worstStatus.getJson());
    }
}
