package com.centurylink.mdw.microservice;

import org.json.JSONObject;

import com.centurylink.mdw.model.Status;

import kotlin.Pair;

/**
 * Create a payload with an array of StatusResponse Jsonable.
 * Overall status code/message are taken from the worst (highest)
 * HTTP status code among individual responses.
 */
public class CombiningConsolidator implements Consolidator {

    @Override
    public Pair<Integer,JSONObject> getResponse(ServiceSummary serviceSummary) {
        JSONObject responses = new JSONObject();
        Status worstStatus = Status.OK;
        for (String microserviceName : serviceSummary.getMicroservices().keySet()) {
            for (MicroserviceInstance microservice : serviceSummary.getMicroservices(microserviceName).getInstances()) {
                Status status = microservice.latestStatus();
                responses.put(microservice.getJsonName(), status.getJson());
                if (status.getCode() > worstStatus.getCode())
                    worstStatus = status;
            }
        }
        JSONObject json = new JSONObject();
        json.put("status", worstStatus.getJson());
        json.put("responses", responses);
        return new Pair<>(worstStatus.getCode(), json);
    }
}
