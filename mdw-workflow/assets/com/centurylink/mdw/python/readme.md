# Python Package
Import this package for MDW Python language support. 

## Python Script Activity
This package adds the Python Script activity to MDW's Toolbox.  Like script activities for other languages,
once dragged onto the process design canvas, its source can be edited by clicking the Edit link on the 
Configurator Design tab (or by right-clicking on the activity and selecting Open Python).

## Variables Access
All process variables are implicitly available to read or assign by their name.  For example, PythonTest.proc
in the [script autotests package](https://github.com/CenturyLinkCloud/mdw/tree/master/mdw-workflow/assets/com/centurylink/mdw/tests/script).
has a string variable named 'message' which is assigned a value in the A3 Python script activity like so:
```python
message = 'Hello'
```
Even variables with Java types can be accessed similarly.  In the same test process activity 'person', which is a
[Jsonable](https://centurylinkcloud.github.io/mdw/docs/javadoc/com/centurylink/mdw/model/Jsonable.html) is accessed as follows:
```python
name = person.getName()
```

## Runtime Context
As with other script activities, the [ActivityRuntimeContext](https://centurylinkcloud.github.io/mdw/docs/javadoc/com/centurylink/mdw/model/workflow/ActivityRuntimeContext.html)
is accessible as `runtimeContext`:
```python
runtimeContext.logDebug('Executing ' + runtimeContext.activity.name)
```

## Return Value
Although Python scripts don't ordinarily have a return value, an MDW Python activity can return a value which
acts as the activity result code:
```python
return 'onward'
```

## Python Modules
You can create Python modules in MDW as assets with extension '.py'.  You can import from these modules for
reuse in your script activities.  Here's an example from the 'Use Module' activity in PythonTest.proc that
imports functions from pymodule.py via the package-qualified module name:  
```python
from com.centurylink.mdw.tests.script.pymodule import square, greet
# ...
message = greet(name, message)
``` 

## Dependencies
  - [com.centurylink.mdw.base](https://github.com/CenturyLinkCloud/mdw/blob/master/mdw-workflow/assets/com/centurylink/mdw/base/readme.md)
  

