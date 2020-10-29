package com.centurylink.mdw.cli;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.centurylink.mdw.bpmn.BpmnProcessExporter;
import com.centurylink.mdw.export.ProcessExporter;
import com.centurylink.mdw.html.HtmlProcessExporter;
import com.centurylink.mdw.cli.impls.ActivityAnnotationParser;
import com.centurylink.mdw.image.PngProcessExporter;
import com.centurylink.mdw.model.workflow.ActivityImplementor;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.pdf.PdfProcessExporter;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Paths;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * Exports process into specific format. If --format is specified with html/png,
 * exports into html/png format otherwise, exports into bpmn2 format.
 */
@Parameters(commandNames = "export", commandDescription = "Export process into supported formats", separators = "=")
public class Export extends Setup {
    @Parameter(names = "--process", description = "Process to be exported")
    private String process;
    public String getProcess() {
        return process;
    }
    public void setProcess(String proc) {
        this.process = proc;
    }

    @Parameter(names = "--impls-src", description = "Export old-style impl JSON based on Java source directory")
    private File implsSrc;
    public File getImplsSrc() { return implsSrc; }
    public void setImplsSrc(File implsSrc) { this.implsSrc = implsSrc; }

    @Parameter(names = "--format", description = "Format to be exported (bpmn, png or html)")
    private String format;
    public String getFormat() {
        return format;
    }
    public void setFormat(String format) {
        this.format = format;
    }

    @Parameter(names = "--output", description = "Filename of the exported output")
    private File output;
    public File getOutput() {
        return output;
    }

    Export() { }

    @Override
    public List<Dependency> getDependencies() throws IOException {
        if (implsSrc != null) {
            return super.getDependencies();
        }
        else {
            init();
            return getProcessExporter().getDependencies();
        }
    }

    private String pkgFile;
    private String procName;

    public Export run(ProgressMonitor... monitors) throws IOException {

        if (implsSrc != null) {
            // export impl JSONs from java source at root path
            if (!implsSrc.isDirectory())
                throw new FileNotFoundException("Impls src directory not found: " + implsSrc);
            if (output == null || (!output.isDirectory() && !output.mkdirs()))
                throw new IOException("Bad output directory: " + output);

            List<File> sourceFiles = new ArrayList<>();
            Files.walkFileTree(Paths.get(implsSrc.getPath()),
                    EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, new SimpleFileVisitor<Path>() {
                        @Override
                        public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) {
                            File file = path.toFile();
                            int lastDot = file.getName().lastIndexOf('.');
                            if (lastDot > 0) {
                                String ext = file.getName().substring(lastDot);
                                if (".java".equals(ext) || ".kt".equals(ext)) {
                                    sourceFiles.add(file);
                                }
                            }
                            return FileVisitResult.CONTINUE;
                        }
                    });
            if (sourceFiles.isEmpty()) {
                getOut().println("No java/kt files found under " + implsSrc);
            }
            else {
                for (File sourceFile : sourceFiles) {
                    ActivityAnnotationParser parser = new ActivityAnnotationParser(implsSrc);
                    ActivityImplementor activityImplementor = parser.parse(sourceFile);
                    if (activityImplementor != null) {
                        JSONObject json = activityImplementor.getJson();
                        byte[] exported = json.toString(2).getBytes();
                        String name = sourceFile.getName().substring(0, sourceFile.getName().lastIndexOf('.'));
                        File outputDir = new File(output + "/" + getRelativePath(implsSrc, sourceFile.getParentFile()));
                        if (!outputDir.isDirectory() && !outputDir.mkdirs())
                            throw new IOException("Unable to create output directory: " + outputDir);
                        File outputFile = new File(outputDir + "/" + name + ".impl");
                        Files.write(outputFile.toPath(), exported);
                    }
                }
            }
        }
        else {
            init();
            String content = new String(Files.readAllBytes(Paths.get(pkgFile + procName)));
            Process proc = Process.fromString(content);
            proc.setName(procName.substring(0, procName.length() - 5));

            getOut().println("Exporting " + format + " to " + output);
            ProcessExporter exporter = getProcessExporter();
            if (exporter instanceof HtmlProcessExporter) {
                ((HtmlProcessExporter) exporter).setOutputDir(output.getParentFile());
            }

            byte[] exported = exporter.export(proc);

            if (exported != null) {
                Files.write(output.toPath(), exported);
            }


        }

        return this;
    }

    private void init() throws IOException {
        int index = process.lastIndexOf('/');
        if (index < 1)
            throw new IOException("Invalid process path: " + process);
        String pkg = process.substring(0, index);
        pkgFile = getAssetRoot() + "/" + pkg.replace('.', '/') + "/";
        procName = process.substring(index + 1);

        if (output == null) {
            if (format == null)
                throw new IOException("Either --format or --output must be specified");
            output = new File(pkgFile + procName.substring(0, procName.length() - 5) + "." + format);
        }
        else {
            if (format == null) {
                int lastDot = output.getName().lastIndexOf('.');
                if (lastDot > 0 && lastDot < output.getName().length() - 1) {
                    format = output.getName().substring(lastDot + 1);
                }
                else {
                    throw new IOException("Invalid output: " + output);
                }
            }
            File fileDir = output.getParentFile();
            if (!fileDir.exists() && !fileDir.mkdirs())
                throw new IOException("Unable to create directory: " + fileDir);
        }
    }

    protected ProcessExporter getProcessExporter() throws IOException {
        if ("bpmn2".equals(format) || "bpmn".equals(format))
            return new BpmnProcessExporter();
        else {
            final Setup setup = this;
            if ("html".equals(format))
                return new HtmlProcessExporter(getProject());
            else if ("png".equals(format))
                return new PngProcessExporter(getProject());
            else if ("pdf".equals(format))
                return new PdfProcessExporter(getProject());
        }

        return null;
    }
}
