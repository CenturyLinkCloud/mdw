package com.centurylink.mdw.tests.microservice

import com.centurylink.mdw.microservice.*
import com.centurylink.mdw.test.MockRuntimeContext
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml

class ServicePlanTest {

    val runtimeContext = MockRuntimeContext("ServicePlan Test Activity")

    init {
        runtimeContext.properties["mdw.services.url"] = "http://localhost:8080/mdw"
        runtimeContext.variables["greeting"] = "hello"
        runtimeContext.docRefs["request"] = "DOCUMENT:12345"
    }

    fun defaultBindings(): ServicePlan {
        return servicePlan(runtimeContext) {
            services {
                microservice {
                    name = "admin/createUser"
                    method = "POST"
                    url = "${runtimeContext.props["mdw.services.url"]}/services/Users"
                }
                microservice {
                    name = "admin/createGroup"
                    method = "POST"
                    url = "${runtimeContext.props["mdw.services.url"]}/services/Workgroups"
                }
            }
        }
    }

    fun customBindings(): ServicePlan {
        return servicePlan(runtimeContext) {
            services {
                microservice {
                    name = "admin/createUser"
                    method = "POST"
                    url = "${runtimeContext.props["mdw.services.url"]}/services/Users"
                    bindings {
                        "hello" to "greeting"
                        "requestMapper" to "com.centurylink.mdw.tests.microservice/UserRequestMapper.groovy"
                        "intVar" to 123
                    }
                }
            }
        }
    }

}

fun dumpYaml(servicePlan: ServicePlan) : String {
    val options = DumperOptions()
    options.defaultFlowStyle = DumperOptions.FlowStyle.FLOW
    options.isPrettyFlow = true
    options.indent = 2

    return Yaml(options).dump(servicePlan)
}

fun main(args : Array<String>) {
    val servicePlanTest = ServicePlanTest()
    val servicePlan = servicePlanTest.customBindings()
    println(dumpYaml(servicePlan))
}