import com.centurylink.mdw.microservice.servicePlan

var isGroup = false

val plan = servicePlan {
    services {
        microservice {
            name = "user"
            template = "com.centurylink.mdw.tests.microservice/GetUser v0.2"
        }
        microservice {
            name = "group"
            enabled = isGroup
        }
    }
}