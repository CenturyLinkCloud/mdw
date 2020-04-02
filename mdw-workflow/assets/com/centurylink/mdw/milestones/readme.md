# Milestones Package
A common practice in MDW workflows is to modularize by designing processes to invoke subprocesses (see
the [Invoke Subprocess](https://centurylinkcloud.github.io/mdw/docs/help/InvokeSubProcessActivity.html) and
[Invoke Multiple Subprocesses](https://centurylinkcloud.github.io/mdw/docs/help/InvokeMultipleSubprocesses.html) activities.
These subprocesses can go on to invoke sub-subprocesses and so on to create a hierarchy of chained subprocess
calls.  This represents good practice because allows reuse of common functionality contained in the subflows.  In MDWHub
you can drill in to a spawned subflow from the calling process instance.  However, a high degree of modularization
can make it difficult to envision the entire end-to-end picture.  This is one of the problems addressed by milestones.

A process hierarchy can be further complicated by the need to contain housekeeping steps like activities for building
a REST adapter request body, and other steps that are important to the workflow designer but don't have much business
meaning.  Identifying certain key activities as milestones allows MDWHub to represent a higher-level visualization
of the overall workflow.  It also facilitates reporting against these milestones to make sure that workflows are
progressing at a healthy rate.

The images below depict the end-to-end flow for a 
[milestones autotest](https://github.com/CenturyLinkCloud/mdw/tree/master/mdw-workflow/assets/com/centurylink/mdw/tests/milestones).
Even for this relatively simple process hierarchy, you can see how the overall flow on the left can be boiled down by identifying 
milestone activities (on the right).

<p float="left">
<img width="40%" src="https://raw.githubusercontent.com/CenturyLinkCloud/mdw/master/mdw-workflow/assets/com/centurylink/mdw/tests/milestones/e2e.png" alt="e2e">
<img width="13%" style="vertical-align:top" src="https://raw.githubusercontent.com/CenturyLinkCloud/mdw/master/mdw-workflow/assets/com/centurylink/mdw/tests/milestones/milestones.png" alt="milestones">
</p>
<div style="clear:both"></div>

## Dependencies
  - [com.centurylink.mdw.base](https://github.com/CenturyLinkCloud/mdw/blob/master/mdw-workflow/assets/com/centurylink/mdw/base/readme.md)
  - [com.centurylink.mdw.node](https://github.com/CenturyLinkCloud/mdw/blob/master/mdw-workflow/assets/com/centurylink/mdw/node/readme.md)
  - [com.centurylink.mdw.react](https://github.com/CenturyLinkCloud/mdw/blob/master/mdw-workflow/assets/com/centurylink/mdw/react/readme.md)

## Configuring Milestones
To include the milestones feature in your MDW app, first [discover and import](https://centurylinkcloud.github.io/mdw/docs/guides/mdw-studio/#4-discover-and-import-asset-packages)
package `com.centurylink.mdw.milestones` into your project.  

In [MDW Studio](https://centurylinkcloud.github.io/mdw/docs/guides/mdw-studio/) activities are marked as milestones on 
the Configurator Monitoring tab.  A [Monitor](https://centurylinkcloud.github.io/mdw/docs/help/monitoring.html) 
is MDW's way of tracking lifecycle stages for activities, processes, or other workflow elements.

To understand how Milestones are specified, consider this illustration from a subflow in the same milestones
autotest illustrated above.

<img width="80%" src="https://raw.githubusercontent.com/CenturyLinkCloud/mdw/master/mdw-workflow/assets/com/centurylink/mdw/milestones/monitor.png" alt="monitor">

On Spellbinding's Monitoring tab we've enabled the Milestone monitor, and we've also entered a value for options.
Because the activity name ("Spellbinding") may not be business-descriptive, under Options we've entered something else
("Custom Label") which will be displayed in the Milestones view in MDWHub.  If nothing is entered under Options, the
activity name will be displayed for the milestone label.

## Label Expressions
Milestone labels entered under Options may also contain [expressions](https://centurylinkcloud.github.io/mdw/docs/help/bindingExpressions.html) 
that reference runtime values.  For example:
```$xslt
Order ${order.id}\nCompleted
```
Here `order` is a process variable whose `id` property is included in the milestone label.  Notice also that we've added
a line break in the label, and that the newline character is escaped as `\n`.

## Milestone Groups 
If the milestones package is present, in MDWHub milestones can be viewed on the Workflow tab by clicking the Milestones
nav link.  Milestone activities are also indicated when viewing a process instance or definition in MDWHub or MDW Studio.
By default milestones are highlighted with a blue background.  However, in [mdw.yaml](https://centurylinkcloud.github.io/mdw/docs/guides/configuration/#mdwyaml)
you can designate milestone groups like so:
```yaml
milestone:
  groups:  # list of milestone groups
    'Group One':
      color: '#990099'
    'Group Two':
      color: '#ff9900'
      description: 'Describe me'  
```
This way you can color-code milestones according to business categories.  On the Milestones configurator tab under Options,
you specify a milestone group in square brackets after the label: ```Very Important Step[Group One]```.  Groups are 
listed in MDWHub by clicking the Info button on the milestone view. 




