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
package com.centurylink.mdw.html;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.centurylink.mdw.cli.Dependency;
import com.centurylink.mdw.export.ProcessExporter;
import com.centurylink.mdw.model.Project;
import com.centurylink.mdw.model.workflow.Process;

public class HtmlProcessExporter implements ProcessExporter {

    private Project project;
    public HtmlProcessExporter(Project project) {
        this.project = project;
    }

    private File outputDir;
    public void setOutputDir(File outputDir) {
        this.outputDir = outputDir;
    }

    @Override
    public byte[] export(Process process) throws IOException {
        return new HtmlExportHelper(project).exportProcess(process, outputDir).getBytes();
    }

    @Override
    public List<Dependency> getDependencies() {
        List<Dependency> dependencies = new ArrayList<>();
        dependencies.add(new Dependency("http://repo.maven.apache.org/maven2",
                "org/jetbrains/kotlin/kotlin-stdlib/1.2.61/kotlin-stdlib-1.2.61.jar", 12388L));
        dependencies.add(new Dependency("http://repo.maven.apache.org/maven2",
                "org/slf4j/slf4j-api/1.7.25/slf4j-api-1.7.25.jar", 41203L));
        dependencies.add(new Dependency("http://repo.maven.apache.org/maven2",
                "com/vladsch/flexmark/flexmark/0.32.22/flexmark-0.32.22.jar", 386805L));
        dependencies.add(new Dependency("http://repo.maven.apache.org/maven2",
                "com/vladsch/flexmark/flexmark-util/0.32.22/flexmark-util-0.32.22.jar", 292213L));
        dependencies.add(new Dependency("http://repo.maven.apache.org/maven2",
                "com/vladsch/flexmark/flexmark-ext-anchorlink/0.32.22/flexmark-ext-anchorlink-0.32.22.jar",
                16735L));
        dependencies.add(new Dependency("http://repo.maven.apache.org/maven2",
                "com/vladsch/flexmark/flexmark-ext-autolink/0.32.22/flexmark-ext-autolink-0.32.22.jar",
                6972L));
        dependencies.add(new Dependency("http://repo.maven.apache.org/maven2",
                "com/vladsch/flexmark/flexmark-ext-superscript/0.32.22/flexmark-ext-superscript-0.32.22.jar",
                13343L));
        dependencies.add(new Dependency("http://repo.maven.apache.org/maven2",
                "com/vladsch/flexmark/flexmark-ext-tables/0.32.22/flexmark-ext-tables-0.32.22.jar",
                61489L));
        dependencies.add(new Dependency("http://repo.maven.apache.org/maven2",
                "com/vladsch/flexmark/flexmark-ext-typographic/0.32.22/flexmark-ext-typographic-0.32.22.jar",
                21975L));
        dependencies.add(new Dependency("http://repo.maven.apache.org/maven2",
                "com/vladsch/flexmark/flexmark-formatter/0.32.22/flexmark-formatter-0.32.22.jar",
                21975L));
        dependencies.add(new Dependency("http://repo.maven.apache.org/maven2",
                "org/nibor/autolink/autolink/0.9.0/autolink-0.9.0.jar",
                19890L));

        return dependencies;
    }

}
