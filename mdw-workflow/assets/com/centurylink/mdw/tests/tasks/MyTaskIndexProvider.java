/**
 * Copyright (c) 2015 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.tests.tasks;

import java.util.HashMap;
import java.util.Map;

import com.centurylink.mdw.annotations.RegisteredService;
import com.centurylink.mdw.model.value.task.TaskRuntimeContext;
import com.centurylink.mdw.observer.task.TaskIndexProvider;

@RegisteredService(TaskIndexProvider.class)
public class MyTaskIndexProvider implements TaskIndexProvider {

    public Map<String,String> collect(TaskRuntimeContext runtimeContext) {

        Map<String,String> indexes = new HashMap<String,String>();
        Map<String,Object> variables = runtimeContext.getVariables();
        TaskJaxb taskJaxb = (TaskJaxb) variables.get("jaxbVar");
        indexes.put("MyAttributeTwo", taskJaxb.getAttributeTwo());
        indexes.put("MyElementOne", taskJaxb.getElementOne());
        indexes.put("MyInt", variables.get("intVar").toString());
        indexes.put("MyDate", variables.get("dateVar").toString());
        indexes.put("MyString", variables.get("stringVar").toString());

        return indexes;
    }
}
