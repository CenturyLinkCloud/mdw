package com.centurylink.mdw.microservice

import com.centurylink.mdw.model.workflow.ActivityRuntimeContext
import com.centurylink.mdw.test.MockRuntimeContext

data class ServicePlan(
    var services: MutableList<Microservice> = mutableListOf()
)

/**
 * Required values come first in constructor.
 * Binding values do not need to be escaped when overridden since runtimeContext is available.
 * TODO: dependencies, etc
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
        var bindings: MutableMap<String,Any?> = mutableMapOf(
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
)
