package com.centurylink.mdw.microservice

data class ServicePlan(
    val services: MutableList<Microservice>
)

// TODO: dependencies
data class Microservice(
    val name: String,
    val template: String?,
    val enabled: Boolean,
    val count: Int,
    val inMapper: String?,
    val outMapper: String?
)
