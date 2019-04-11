package com.centurylink.mdw.microservice

import com.centurylink.mdw.model.workflow.ActivityRuntimeContext
import com.centurylink.mdw.test.MockRuntimeContext
import org.json.JSONArray
import org.json.JSONObject

data class ServicePlan(
        var services: MutableList<Microservice> = mutableListOf()
)

/**
 * Required values come first in constructor.
 * Binding values do not need to be escaped when overridden since runtimeContext is available.
 * TODO: dependencies(complete), etc
 */
data class Microservice(
        private val runtimeContext: ActivityRuntimeContext = MockRuntimeContext("dummy"),
        var name: String = "",
        var url: String = "",
        var method: String = "",
        var subflow: String = "com.centurylink.mdw.microservice/\${StandardInvoke}.proc",
        var enabled: Boolean? = true,
        var count: Int = 1,
        var synchronous: Boolean? = true,
        var dependencies: String = "",
        var bindings: MutableMap<String, Any?> = mutableMapOf(
                "microservice" to name,
                "serviceUrl" to url,
                "serviceMethod" to method,
                "synchronous" to synchronous,
                "dependencies" to dependencies,
                "request" to runtimeContext.docRefs["request"],
                "requestHeaders" to runtimeContext.docRefs["requestHeaders"],
                "serviceSummary" to runtimeContext.docRefs["serviceSummary"],
                "requestMapper" to "com.centurylink.mdw.microservice/IdentityRequestMapper.groovy",
                "responseMapper" to "com.centurylink.mdw.microservice/IdentityResponseMapper.groovy"
        )
) {
    constructor(
            runtimeContext: ActivityRuntimeContext,
            name: String,
            url: String,
            method: String,
            subflow: String,
            enabled: Boolean?,
            count: Int,
            synchronous: Boolean?,
            bindings: MutableMap<String, Any?>
    ) : this(
            runtimeContext,
            name,
            url,
            method,
            subflow,
            enabled,
            count,
            synchronous,
            "",
            bindings
    )
}

class Microservices(runtimeContext: ActivityRuntimeContext) : ArrayList<Microservice>() {

    init {
        val rows = JSONArray(runtimeContext.attributes["microservices"])
        for (i in 0 until rows.length()) {
            val row = rows.getJSONArray(i)
            val def = JSONObject(row.getString(ROWS.indexOf("definition")))
            val envs = def.getJSONArray("environments")
            var baseUrl = envs.getJSONObject(0).getString("baseEndpoint")
            if (baseUrl.endsWith("/")) baseUrl = baseUrl.substring(0, baseUrl.length - 1)

            val microservice = Microservice(runtimeContext,
                    name = def.getString("teamName") + "/" + def.getString("name"),
                    url = baseUrl + row.getString(ROWS.indexOf("path")),
                    method = row.getString(ROWS.indexOf("method")),
                    enabled = "true" == row.getString(ROWS.indexOf("enabled")),
                    count = row.getString(ROWS.indexOf("count")).toInt()
            )

            microservice.bindings["requestMapper"] = row.getString(ROWS.indexOf("requestMapper"))
            microservice.bindings["responseMapper"] = row.getString(ROWS.indexOf("responseMapper"))

            add(microservice)
        }
    }

    companion object {
        val ROWS = arrayOf(
                "enabled",
                "definition",
                "path",
                "method",
                "count",
                "requestMapper",
                "responseMapper",
                "dependencies"
        )
    }
}
