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
    fun microservice(block: MicroserviceBuilder.() -> Unit) = add(MicroserviceBuilder().apply(block).build())
}

@MicroserviceDsl
class MicroserviceBuilder {

    var default = Microservice()

    // values initialized via constructor defaults
    var name = default.name
    var url = default.url
    var method = default.method
    var template = default.template
    var enabled = default.enabled
    var count = default.count
    var bindings = default.bindings.toMutableMap()

    var customBindings = mutableMapOf<String,String>()

    fun bindings(block: BindingsMapper.() -> Unit) {
        BindingsMapper(customBindings).apply(block)
    }

    fun build(): Microservice {
        val microservice = Microservice(
                name = name,
                url = url,
                method = method,
                template = template,
                enabled = enabled,
                count = count
        )
        microservice.bindings.putAll(customBindings)
        return microservice
    }
}

@MicroserviceDsl
class BindingsMapper(val bindings: MutableMap<String,String>) {
   infix fun String.to(value: String): Unit {
       bindings.put(this, value);
    }
}