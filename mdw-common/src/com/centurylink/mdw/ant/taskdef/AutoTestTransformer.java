/*
 * Copyright (C) 2017 CenturyLink, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
