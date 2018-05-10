# Kotlin Language
Import this package for MDW Kotlin language support. 

## Dynamic Kotlin Assets
Kotlin assets (with .kt extension) can be included in any package and will be compiled on startup (or 
on first access in dev mode).  Compiled output is written to `${props['mdw.temp.dir'}`/kotlin/classes,
and startup compilation is skipped if this destination is considered up-to-date.  If you experience 
problems you suspect are due out-of-date classes, delete the output directory and restart your server.

Implicit access to your Kotlin classes is available in dynamic Java.  Kotlin model objects can also be 
used as variables in workflow processes.  Both of these concepts are illustrated in KotlinExecutor.test 
located in the script tests package:
https://github.com/CenturyLinkCloud/mdw/tree/master/mdw-workflow/assets/com/centurylink/mdw/tests/script

## Script Activity and Evaluator
The Kotlin Script activity is included in this package for full-blown kts support.  Since Kotlin is a strongly and statically typed
language, access to variables is not implicit like in Groovy scripts; instead, use the *variables* map:
```kotlin
var person: Person = variables["jsonablePerson"] as Person
```
Working examples are available in the 
[script tests package](https://github.com/CenturyLinkCloud/mdw/tree/master/mdw-workflow/assets/com/centurylink/mdw/tests/script).

For use in an evaluator activity, select Kotlin in the Language dropdown.
Examples are in KotlinEvaluator.test in the 
[script tests package](https://github.com/CenturyLinkCloud/mdw/tree/master/mdw-workflow/assets/com/centurylink/mdw/tests/script).

## Limitations
Currently Kotlin assets must not refer to dynamic Java asset classes as compilers errors will result.
For such a use case, you'll have to forego dynamic Java and instead compile these Java sources into a jar asset.
Referring to dynamic Kotlin classes from dynamic Java works fine, although Eclipse may complain unless you have 
Kotlin source paths set up. 