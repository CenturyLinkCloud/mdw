package com.centurylink.mdw.microservice

data class ServicePlan(
    val services: MutableList<Microservice>
)

// TODO: dependencies
data class Microservice(
    val name: String,
    val url: String,
    val template: String,
    val method: String,
    val enabled: Boolean,
    val count: Int,
    val requestMapper: String,
    val responseMapper: String
)
