package com.centurylink.mdw.microservice

import com.centurylink.mdw.model.Status
import org.json.JSONObject
import com.centurylink.mdw.microservice.Consolidator
import com.centurylink.mdw.microservice.ServiceSummary

/**
 * Combines the latest updates/invocations (one per microservice).
 * The resulting status code is the worst(highest) of these responses.
 */
class CombiningConsolidator : Consolidator {
    override fun getResponse(serviceSummary: ServiceSummary): Pair<Int,JSONObject> {
        val responses = JSONObject()
        var worstStatus = Status.OK
        for (microservice  in serviceSummary.microservices) {
            val status = latestResponse(microservice)
            if (status != null) {
                responses.put(microservice.microservice, status.json)
                if (status.code > worstStatus.code) {
                    worstStatus = status
                }
            }
        }
        val json = JSONObject()
        json.put("status", worstStatus)
        json.put("responses", responses)
        return Pair(worstStatus.code, json)
    }
}

class SingleWorstConsolidator : Consolidator {
    override fun getResponse(serviceSummary: ServiceSummary): Pair<Int,JSONObject> {
        var worstStatus = Status.OK
        for (microservice  in serviceSummary.microservices) {
            val status = latestResponse(microservice)
            if (status != null) {
                if (status.code > worstStatus.code) {
                    worstStatus = status
                }
            }
        }
        return Pair(worstStatus.code, worstStatus.json)
    }
}

fun latestResponse(microservice: MicroserviceHistory): Status? {
    val lastInvoke = microservice.invocations.maxBy { it.sent }
    val lastUpdate = microservice.updates?.maxBy { it.received }
    return if (lastInvoke != null) {
        if (lastUpdate == null) {
            lastInvoke.status
        } else {
            if (lastInvoke.sent > lastUpdate.received) {
                lastInvoke.status
            } else {
                lastUpdate.status
            }
        }
    } else {
        null
    }
}