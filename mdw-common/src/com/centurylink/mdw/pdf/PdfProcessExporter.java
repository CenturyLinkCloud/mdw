package com.centurylink.mdw.pdf;

import com.centurylink.mdw.cli.Dependency;
import com.centurylink.mdw.export.ProcessExporter;
import com.centurylink.mdw.model.project.Project;
import com.centurylink.mdw.model.workflow.Process;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

public class PdfProcessExporter implements ProcessExporter {
    private Project project;

    public PdfProcessExporter(Project project) {
        this.project = project;
    }

    @Override
    public List<Dependency> getDependencies() {
        List<Dependency> dependencies = new ArrayList<>();
        dependencies.add(new Dependency("com/github/librepdf/openpdf/1.3.13/openpdf-1.3.13.jar", 4249976L));
        dependencies.add(new Dependency("org/jetbrains/kotlin/kotlin-stdlib/1.2.61/kotlin-stdlib-1.2.61.jar", 12388L));
        dependencies.add(new Dependency("org/slf4j/slf4j-api/1.7.25/slf4j-api-1.7.25.jar", 41203L));
        dependencies.add(new Dependency("com/vladsch/flexmark/flexmark/0.32.22/flexmark-0.32.22.jar", 386805L));
        dependencies.add(new Dependency("com/vladsch/flexmark/flexmark-util/0.32.22/flexmark-util-0.32.22.jar", 292213L));
        dependencies.add(new Dependency("com/vladsch/flexmark/flexmark-ext-anchorlink/0.32.22/flexmark-ext-anchorlink-0.32.22.jar", 16735L));
        dependencies.add(new Dependency("com/vladsch/flexmark/flexmark-ext-autolink/0.32.22/flexmark-ext-autolink-0.32.22.jar", 6972L));
        dependencies.add(new Dependency("com/vladsch/flexmark/flexmark-ext-superscript/0.32.22/flexmark-ext-superscript-0.32.22.jar", 13343L));
        dependencies.add(new Dependency("com/vladsch/flexmark/flexmark-ext-tables/0.32.22/flexmark-ext-tables-0.32.22.jar", 61489L));
        dependencies.add(new Dependency("com/vladsch/flexmark/flexmark-ext-typographic/0.32.22/flexmark-ext-typographic-0.32.22.jar", 21975L));
        dependencies.add(new Dependency("com/vladsch/flexmark/flexmark-formatter/0.32.22/flexmark-formatter-0.32.22.jar", 21975L));
        dependencies.add(new Dependency("org/nibor/autolink/autolink/0.9.0/autolink-0.9.0.jar", 19890L));
        return dependencies;
    }

    @Override
    public byte[] export(Process process) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            new PdfExportHelper(project).exportProcess(process, out);
            return out.toByteArray();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
