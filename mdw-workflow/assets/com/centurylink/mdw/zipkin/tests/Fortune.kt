package com.centurylink.mdw.zipkin.tests

import com.centurylink.mdw.model.Jsonable
import org.json.JSONObject

class Fortune() : Jsonable {

    var name: String? = null
    var fortune: String? = null

    constructor(json: JSONObject) : this() {
        bind(json)
    }

    constructor(name: String, fortune: String) : this() {
        this.name = name
        this.fortune = fortune
    }
}