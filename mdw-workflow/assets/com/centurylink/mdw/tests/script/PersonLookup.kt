package com.centurylink.mdw.tests.script

import org.json.JSONObject

class PersonLookup {
    companion object {
        @JvmStatic fun find(lastName: String): Person {
            val result = when (lastName) {
                "Torvalds" -> Person(JSONObject("{\"firstName\": \"Linus\", \"lastName\": \"Torvalds\"}"))
                else -> Person(JSONObject("{\"firstName\": \"Try\", \"lastName\": \"Google\"}"))
            }
            return result
        }
    }
}