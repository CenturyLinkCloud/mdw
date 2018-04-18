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

    var name: String = ""
    var template: String? = null
    var enabled: Boolean = true
    var count: Int = 1
    var inMapper: String? = null
    var outMapper: String? = null

    fun build(): Microservice = Microservice(
            name,
            template,
            enabled,
            count,
            inMapper,
            outMapper
    )

}

