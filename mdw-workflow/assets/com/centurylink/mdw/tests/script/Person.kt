package com.centurylink.mdw.tests.script
  
import com.centurylink.mdw.model.Jsonable
import org.json.JSONObject
  
class Person() : Jsonable {
	
	constructor(json: JSONObject) : this() {
		bind(json)
	}
  
    var firstName: String? = null
    var lastName: String? = null
	
    fun getName(): String {
		return firstName + " " + lastName
	}	
} 