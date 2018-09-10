package com.centurylink.mdw.tests.cloud;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.model.workflow.ActivityRuntimeContext;
import com.centurylink.mdw.workflow.activity.DefaultActivityImpl;
import com.centurylink.mdw.annotations.Activity;

@Activity(value="Java Invader", icon="com.centurylink.mdw.tests.cloud/invader.png",
    pagelet="{\n" +
            "  \"widgets\": [\n" +
            "    { \"name\": \"Planet\", \"type\": \"dropdown\", \"options\": [\"Jupiter\",\"Mars\",\"Neptune\",\"Venus\"] },\n" +
            "    { \"name\": \"Invader\", \"type\": \"text\"}\n" +
            "  ]\n" +
            "}\n")
public class JavaInvader extends DefaultActivityImpl {

    @Override
    public Object execute(ActivityRuntimeContext runtimeContext) throws ActivityException {
        setValue("updated", "updated from java");
        setValue("invader", getAttribute("Invader"));
        return getAttribute("Planet");
    }
}
