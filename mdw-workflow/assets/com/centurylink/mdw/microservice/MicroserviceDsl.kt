package com.centurylink.mdw.microservice

import com.centurylink.mdw.model.workflow.ActivityRuntimeContext

@DslMarker
annotation class MicroserviceDsl

fun servicePlan(runtimeContext: ActivityRuntimeContext, block: ServicePlanBuilder.() -> Unit): ServicePlan {
    return ServicePlanBuilder(runtimeContext).apply(block).build()
}

@MicroserviceDsl
class ServicePlanBuilder(private val runtimeContext: ActivityRuntimeContext) {
    private val services = mutableListOf<Microservice>()

    fun services(block: ServicesHelper.() -> Unit) {
        services.addAll(ServicesHelper(runtimeContext).apply(block))
    }

    fun build(): ServicePlan = ServicePlan(services)
}

/*
 * Helper class (receiver for services() lambda)
 */
@MicroserviceDsl
class ServicesHelper(private val runtimeContext: ActivityRuntimeContext) : ArrayList<Microservice>() {
    fun microservice(block: MicroserviceBuilder.() -> Unit) {
        add(MicroserviceBuilder(runtimeContext).apply(block).build())
    }
}

@MicroserviceDsl
class MicroserviceBuilder(private val runtimeContext: ActivityRuntimeContext) {

    var default = Microservice(runtimeContext)

    // values initialized via constructor defaults
    var name = default.name
    var url = default.url
    var method = default.method
    var subflow = default.subflow
    var enabled = default.enabled
    var count = default.count
    var bindings = default.bindings.toMutableMap()

    var customBindings = mutableMapOf<String,Any?>()

    fun bindings(block: BindingsMapper.() -> Unit) {
        BindingsMapper(customBindings).apply(block)
    }

    fun build(): Microservice {
        val microservice = Microservice(
                runtimeContext = runtimeContext,
                name = name,
                url = url,
                method = method,
                subflow = subflow,
                enabled = enabled,
                count = count
        )
        microservice.bindings.putAll(customBindings)
        return microservice
    }
}

@MicroserviceDsl
class BindingsMapper(val bindings: MutableMap<String,Any?>) {
   infix fun String.to(value: Any?): Unit {
       bindings.put(this, value);
    }
}