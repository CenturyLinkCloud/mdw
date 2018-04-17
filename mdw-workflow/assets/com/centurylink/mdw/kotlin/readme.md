# Kotlin Language
Import this package for MDW Kotlin language support. 

## Dynamic Kotlin Assets
Kotlin assets (with .kt extension) can be included in any package and will be compiled on startup (or 
on first access in dev mode).  Implicit access to your Kotlin classes is available in dynamic Java.
Kotlin model objects can also be used as variables.  Both of these concepts are illustrated in
KotlinExecutor.test in the script tests package:
https://github.com/CenturyLinkCloud/mdw/tree/master/mdw-workflow/assets/com/centurylink/mdw/tests/script

## Script Activity and Evaluator
For use in an evaluator activity, select Kotlin in the Language dropdown.  Examples are in KotlinEvaluator.test
in the [script tests package](https://github.com/CenturyLinkCloud/mdw/tree/master/mdw-workflow/assets/com/centurylink/mdw/tests/script).
The Kotlin Script activity is included in this package for full-blown kts support.  Since Kotlin is a strongly and statically typed
language, access to variables is not implicit; instead, use the *variables* map:
```kotlin
var person: Person = variables["jsonablePerson"] as Person
```
Working examples are available in the 
[script tests package](https://github.com/CenturyLinkCloud/mdw/tree/master/mdw-workflow/assets/com/centurylink/mdw/tests/script).