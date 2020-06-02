package com.centurylink.mdw.tests.code;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.model.workflow.ActivityRuntimeContext;
import com.centurylink.mdw.workflow.activity.DefaultActivityImpl;
import com.centurylink.mdw.annotations.Activity;

@Activity(value="Java Invader", icon="com.centurylink.mdw.tests.code/invader.png",
    pagelet="com.centurylink.mdw.tests.code/JavaInvader.pagelet")
public class JavaInvader extends DefaultActivityImpl {

    @Override
    public Object execute(ActivityRuntimeContext runtimeContext) throws ActivityException {
        setValue("updated", "updated from java");
        setValue("invader", getAttribute("Invader"));
        return getAttribute("Planet");
    }
}
