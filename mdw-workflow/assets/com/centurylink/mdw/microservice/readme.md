# Microservices
Import this package for MDW microservice orchestration support. 

## Microservice Orchestration Activity
Use this to execute your orchestration service plan:
http://centurylinkcloud.github.io/mdw/docs/help/MicroserviceOrchestrator.html

## Dependencies Wait/Notify
(TODO)

## Response Consolidation
(TODO)

## Script Activity
The Kotlin Script activity is included in this package for full-blown kts support.  Since Kotlin is a strongly and statically typed
language, access to variables is not implicit like in Groovy scripts; instead, use the *variables* map:
```kotlin
var person: Person = variables["jsonablePerson"] as Person
```
Kotlin script activities work the same way as other [MDW script activities](http://centurylinkcloud.github.io/mdw/docs/help/scriptActivity.html)
Working examples are available in the 
[script tests package](https://github.com/CenturyLinkCloud/mdw/tree/master/mdw-workflow/assets/com/centurylink/mdw/tests/script).

## Script Evaluator
For use in an evaluator activity, select Kotlin in the Language dropdown.
Examples are in KotlinEvaluator.test in the 
[script tests package](https://github.com/CenturyLinkCloud/mdw/tree/master/mdw-workflow/assets/com/centurylink/mdw/tests/script).

## Limitations
Currently Kotlin assets must not refer to dynamic Java asset classes as compiler errors will result.
For such a use case, you'll have to forego dynamic Java and instead compile these Java sources into a jar asset.
Referring to dynamic Kotlin classes from dynamic Java works fine, although Eclipse may complain unless you have 
Kotlin source paths set up.  