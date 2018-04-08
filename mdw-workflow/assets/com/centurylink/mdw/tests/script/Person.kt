package com.centurylink.mdw.tests.script
  
import com.centurylink.mdw.model.Jsonable
import org.json.JSONObject
  
class Person(json: JSONObject) : Jsonable {
    init {
        bind(json)
    }
  
    var firstName: String? = null
    var lastName: String? = null
} 