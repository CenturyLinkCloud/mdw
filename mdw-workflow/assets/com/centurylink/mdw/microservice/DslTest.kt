package com.centurylink.mdw.microservice

import com.centurylink.mdw.microservice.servicePlan

var servicePlan = servicePlan {
    services {
        microservice {
            name = "admin/createUser"
            method = "POST"
        }
        microservice {
            name = "admin/createGroup"
            method = "POST"
        }
    }
}