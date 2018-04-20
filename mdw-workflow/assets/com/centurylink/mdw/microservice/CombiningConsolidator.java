package com.centurylink.mdw.microservice;

import org.json.JSONObject;

import com.centurylink.mdw.model.Status;

import kotlin.Pair;

public class CombiningConsolidator implements Consolidator {

    @Override
    public Pair<Integer,JSONObject> getResponse(ServiceSummary serviceSummary) {
        JSONObject responses = new JSONObject();
        Status worstStatus = Status.OK;
        for (MicroserviceHistory microservice : serviceSummary.getMicroservices()) {
            Status status = microservice.latestStatus();
            responses.put(microservice.getJsonName(), status.getJson());
            if (status.getCode() > worstStatus.getCode())
                worstStatus = status;
        }
        JSONObject json = new JSONObject();
        json.put("status", worstStatus.getJson());
        json.put("responses", responses);
        return new Pair<>(worstStatus.getCode(), json);
    }
}
