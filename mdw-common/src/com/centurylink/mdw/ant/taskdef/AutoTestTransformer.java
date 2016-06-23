/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.ant.taskdef;

import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.optional.junit.AggregateTransformer;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.resources.StringResource;

public class AutoTestTransformer extends AggregateTransformer {

    public AutoTestTransformer(Task task) {
        super(task);
    }

    private String xsl;
    public String getXsl() { return xsl; }
    public void setXsl(String xsl) { this.xsl = xsl; }

    protected Resource getStylesheet() {

        if (xsl != null) {
            StringResource stylesheet = new StringResource();
            stylesheet.setValue(xsl);
            return stylesheet;
        }
        else {
            return super.getStylesheet();
        }
    }
}
