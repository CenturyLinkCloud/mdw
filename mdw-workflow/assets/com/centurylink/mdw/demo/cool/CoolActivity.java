package com.centurylink.mdw.demo.cool;

import com.centurylink.mdw.activity.ActivityException;
import com.centurylink.mdw.model.workflow.ActivityRuntimeContext;
import com.centurylink.mdw.util.log.StandardLogger.LogLevel;
import com.centurylink.mdw.util.timer.Tracked;
import com.centurylink.mdw.workflow.activity.DefaultActivityImpl;
import com.centurylink.mdwdemo.DemoHelper;

@Tracked(LogLevel.TRACE)
public class CoolActivity extends DefaultActivityImpl {

    @Override
    public Object execute(ActivityRuntimeContext runtimeContext) throws ActivityException {
        runtimeContext.logInfo("Degree of coolness: " + runtimeContext.getAttribute("coolLevel"));
        runtimeContext.logInfo("Accessing helper...");
        DemoHelper helper = new DemoHelper();
        String something = helper.doSomething();
        setVariableValue("something", something);
        runtimeContext.logInfo("helper.doSomething(): " + something);
        String greeting = helper.hello((String)getVariableValue("name"));
        setVariableValue("greeting", greeting);
        
        runtimeContext.logInfo("Updating input doc...");
        CoolDoc doc = (CoolDoc) runtimeContext.getVariables().get("doc");
        if (doc != null) {
            doc.setCoolRequired("iamcooler");
            doc.setOptionalAttr("coolness_optional");
            setVariableValue("doc", doc);
        }
        return null;
    }
}