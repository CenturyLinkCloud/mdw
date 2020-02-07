/*
 * Copyright (C) 2020 CenturyLink, Inc.
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

import com.centurylink.mdw.cli.Dependency;
import com.centurylink.mdw.export.ProcessExporter;
import com.centurylink.mdw.model.project.Data;
import com.centurylink.mdw.model.workflow.Process;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class BpmnProcessExporter implements ProcessExporter {

    @Override
    public List<Dependency> getDependencies() {
        List<Dependency> dependencies = new ArrayList<>();
        dependencies.add(new Dependency(Data.GIT_BASE_URL + "/mdw/blob/master/mdw/libs/bpmn-schemas.jar?raw=true", "./bpmn-schemas.jar", 2011745L));
        dependencies.add(new Dependency("org/apache/xmlbeans/xmlbeans/2.4.0/xmlbeans-2.4.0.jar", 2694049L));
        dependencies.add(new Dependency("org/slf4j/slf4j-api/1.7.25/slf4j-api-1.7.25.jar", 41203L));
        return dependencies;
    }

    @Override
    public byte[] export(Process process) throws IOException {
        return new BpmnExportHelper().exportProcess(process).getBytes();
    }
}
