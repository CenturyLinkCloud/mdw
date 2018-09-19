/*
 * Copyright (C) 2018 CenturyLink, Inc.
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
package com.centurylink.mdw.bpmn;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.centurylink.mdw.cli.Dependency;
import com.centurylink.mdw.export.ProcessExporter;
import com.centurylink.mdw.model.workflow.Process;

public class BpmnProcessExporter implements ProcessExporter {

    @Override
    public byte[] export(Process process) throws IOException {
        return new BpmnExportHelper().exportProcess(process).getBytes();
    }

    @Override
    public List<Dependency> getDependencies() {
        List<Dependency> dependencies = new ArrayList<>();
        dependencies.add(new Dependency(
                "https://github.com/CenturyLinkCloud/mdw/blob/master/mdw/libs/bpmn-schemas.jar?raw=true",
                "./bpmn-schemas.jar", 2011745L));
        dependencies.add(new Dependency("http://repo.maven.apache.org/maven2",
                "org/apache/xmlbeans/xmlbeans/2.4.0/xmlbeans-2.4.0.jar", 2694049L));
        return dependencies;
    }

}
