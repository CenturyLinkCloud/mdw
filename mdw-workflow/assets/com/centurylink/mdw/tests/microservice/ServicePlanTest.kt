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
    }

    fun defaultBindings(): ServicePlan {
        return servicePlan {
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
        return servicePlan {
            services {
                microservice {
                    name = "admin/createUser"
                    method = "POST"
                    url = "${runtimeContext.props["mdw.services.url"]}/services/Users"
                    bindings {
                        "greeting" to "hello"
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
