package com.centurylink.mdw.microservice;

import org.json.JSONObject;

import com.centurylink.mdw.model.Status;

import kotlin.Pair;

public class WorstCaseConsolidator implements Consolidator {

    @Override
    public Pair<Integer,JSONObject> getResponse(ServiceSummary serviceSummary) {
        Status worstStatus = Status.OK;
        for (MicroserviceHistory microservice : serviceSummary.getMicroservices()) {
            Status status = microservice.latestStatus();
            if (status.getCode() > worstStatus.getCode())
                worstStatus = status;
        }
        return new Pair<>(worstStatus.getCode(), worstStatus.getJson());
    }
}
