# Milestones
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
Even for this relatively simple process hierarchy, you can see how the overall flow can be boiled down by identifying 
milestone activities.

<img style="float:left;width:40%" src="e2e.png" alt="e2e"></img>
<img style="width:40%" src="milestones.png" alt="milestones"></img>

## Configuring Milestones
 