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
package com.centurylink.mdw.services.asset;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.stream.Collectors;

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.cli.Props;
import com.centurylink.mdw.cli.Setup;
import com.centurylink.mdw.html.HtmlExportHelper;
import com.centurylink.mdw.model.project.Data;
import com.centurylink.mdw.model.project.Project;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.html.FlexmarkInstances;
import com.centurylink.mdw.html.HtmlProcessExporter;
import com.centurylink.mdw.model.asset.AssetInfo;
import com.centurylink.mdw.model.system.MdwVersion;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.SystemServices;
import com.centurylink.mdw.util.file.FileHelper;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import com.vladsch.flexmark.ast.Node;
import com.vladsch.flexmark.parser.Parser;

/**
 * TODO: cached output based on file timestamp
 */
public class HtmlRenderer implements Renderer {
    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    public static final String STYLES = "styles";
    public static final String HTML_BODY = "html-body";

    @SuppressWarnings("squid:S1845")
    private static String styles = null;

    static String getStyles() throws IOException {
        if (styles == null) {
            InputStream is = FileHelper.readFile("css/styles.css", HtmlRenderer.class.getClassLoader());
            if (is != null) {
                try (BufferedReader buffer = new BufferedReader(new InputStreamReader(is))) {
                    styles = buffer.lines().collect(Collectors.joining("\n"));
                }
            }
        }
        return styles;
    }

    private AssetInfo asset;

    public HtmlRenderer(AssetInfo asset) {
        this.asset = asset;
    }

    public byte[] render(Map<String, String> options) throws RenderingException {
        Path filePath = Paths.get(asset.getFile().getPath());
        try {
            if (asset.getExtension().equals("html")) {
                return Files.readAllBytes(filePath);
            } else if (asset.getExtension().equals("md")) {

                Parser parser = FlexmarkInstances.getParser(null);
                com.vladsch.flexmark.html.HtmlRenderer renderer = FlexmarkInstances.getRenderer(null);

                StringBuilder html = new StringBuilder();
                boolean withStyles = "true".equalsIgnoreCase(options.get(STYLES));
                if (withStyles) {
                    html.append("<html>\n<head>\n<style>\n");
                    html.append(getStyles());
                    html.append("\n</style>\n</head>\n<body>");
                }
                Node document = parser.parse(new String(Files.readAllBytes(filePath)));
                html.append(renderer.render(document));
                if (withStyles) {
                    html.append("\n<body>\n</html>");
                }
                return html.toString().getBytes();
            } else if (asset.getExtension().equals("proc")) {
                String pkgPath = ApplicationContext.getAssetRoot().toPath().relativize(asset.getFile().getParentFile().toPath()).normalize().toString();
                String pkg = pkgPath.replace('/', '.');
                String assetPath = pkg + "/" + asset.getName();
                logger.debug("Exporting process: " + assetPath + " to PDF");
                File procFile = asset.getFile();
                try {
                    SystemServices systemServices = ServiceLocator.getSystemServices();
                    File exportDir = new File(ApplicationContext.getTempDirectory() + "/export/" + pkgPath);
                    if (!exportDir.isDirectory() && !exportDir.mkdirs())
                        throw new IOException("Unable to create export directory: " + exportDir);
                    File exportFile = new File(exportDir + "/" + asset.getRootName() + ".html");
                    String cliCommand = "export --process=" + assetPath + " --format=html --output=" + exportFile;
                    logger.debug("CLI command: '" + cliCommand + "'");
                    String output = systemServices.runCliCommand(cliCommand);
                    if (!exportFile.exists())
                        throw new FileNotFoundException(exportFile.toString() + " -- CLI output:\n" + output);
                    return Files.readAllBytes(exportFile.toPath());
                }
                catch (Exception ex) {
                    throw new RenderingException(ServiceException.INTERNAL_ERROR, ex.getMessage(), ex);
                }
            } else {
                throw new RenderingException(ServiceException.NOT_IMPLEMENTED, "Cannot convert " + asset.getExtension() + " to HTML");
            }
        } catch (IOException ex) {
            throw new RenderingException(ServiceException.INTERNAL_ERROR, "Error reading: " + filePath, ex);
        }
    }

    @Override
    public String getFileName() {
        return asset.getRootName() + ".html";
    }
}
