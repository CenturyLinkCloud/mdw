package com.centurylink.mdw.microservice

data class ServicePlan(
    var services: MutableList<Microservice> = mutableListOf()
)

/**
 * Required values come first in constructor.
 * TODO: dependencies, etc
 */
data class Microservice(
    var name: String = "",
    var url: String = "",
    var method: String = "",
    var template: String = "com.centurylink.mdw.microservice/\${DefaultInvoke}.proc",
    var enabled: Boolean? = true,
    var count: Int = 1,
    var requestMapper: String = "com.centurylink.mdw.microservice/IdentityRequestMapper.groovy",
    var responseMapper: String = "com.centurylink.mdw.microservice/IdentityResponseMapper.groovy"
)
