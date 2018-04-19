package com.centurylink.mdw.microservice

@DslMarker
annotation class MicroserviceDsl

fun servicePlan(block: ServicePlanBuilder.() -> Unit): ServicePlan = ServicePlanBuilder().apply(block).build()

@MicroserviceDsl
class ServicePlanBuilder {

    private val services = mutableListOf<Microservice>()

    fun services(block: ServicesHelper.() -> Unit) {
        services.addAll(ServicesHelper().apply(block))
    }

    fun build(): ServicePlan = ServicePlan(services)
}

/*
 * Helper class (receiver for services() lambda)
 */
@MicroserviceDsl
class ServicesHelper: ArrayList<Microservice>() {

    fun microservice(block: MicroserviceBuilder.() -> Unit) {
        add(MicroserviceBuilder().apply(block).build())
    }
}

@MicroserviceDsl
class MicroserviceBuilder {

    // required values are empty vs null or default
    var name: String = ""
    var url: String = ""
    var template: String = "com.centurylink.mdw.microservice/\${DefaultInvoke}.proc"
    var method: String = ""
    var enabled: Boolean = true
    var count: Int = 1
    var requestMapper: String = "com.centurylink.mdw.microservice/IdentityRequestMapper.groovy"
    var responseMapper: String = "com.centurylink.mdw.microservice/IdentityResponseMapper.groovy"

    fun build(): Microservice = Microservice(
            name = name,
            url = url,
            template = template,
            method = method,
            enabled = enabled,
            count = count,
            requestMapper = requestMapper,
            responseMapper = responseMapper
    )

}

