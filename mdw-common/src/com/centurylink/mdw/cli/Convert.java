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
package com.centurylink.mdw.cli;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.centurylink.mdw.config.OrderedProperties;
import com.centurylink.mdw.config.YamlBuilder;
import com.centurylink.mdw.config.YamlProperties;
import com.centurylink.mdw.dataaccess.AssetRevision;
import com.centurylink.mdw.model.asset.Pagelet;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@Parameters(commandNames="convert", commandDescription="Convert mdw/app property files, or package.json files to yaml", separators="=")
public class Convert extends Setup {

    @Parameter(names="--packages", description="Update package.json files to package.yaml (ignores other options)")
    private boolean packages;
    public boolean isPackages() { return packages; }
    public void setPackages(boolean packages) { this.packages = packages; }

    @Parameter(names="--input", description="Input property file or impl file")
    private File input;
    public File getInput() {
        return input;
    }

    @Parameter(names="--map", description="Optional compatibility mapping file")
    private File map;
    public File getMap() {
        return map;
    }

    @Parameter(names="--prefix", description="Optional common prop prefix")
    private String prefix;
    public String getPrefix() {
        return prefix;
    }

    @Parameter(names="--language", description="Output language for impls")
    private String language;
    public String getLanguage() {
        return prefix;
    }

    Convert() {

    }

    public Convert(File input) {
        this.input = input;
    }

    @Override
    public Convert run(ProgressMonitor... progressMonitors) throws IOException {

        if (isPackages()) {
            convertPackages();
        }
        else {
            if (input != null && input.getName().endsWith(".impl")) {
                convertImplementor();
            }
            else {
                convertProperties();
            }
        }

        return this;
    }

    protected void convertPackages() throws IOException {

        System.out.println("Processing packages:");
        Map<String,File> packageDirs = getAssetPackageDirs();
        for (String packageName : packageDirs.keySet()) {
            System.out.println("  " + packageName);
            File packageDir = packageDirs.get(packageName);
            File metaDir = new File(packageDir + "/" + META_DIR);
            File yamlFile = new File(metaDir + "/package.yaml");
            File jsonFile = new File(metaDir + "/package.json");
            if (yamlFile.exists()) {
                if (jsonFile.exists()) {
                    System.out.println("    Removing redundant file: " + jsonFile);
                    new Delete(jsonFile).run();
                }
                else {
                    System.out.println("    Ignoring existing: " + yamlFile);
                }
            }
            else {
                System.out.println("    Converting: " + jsonFile);
                JSONObject json = new JSONObject(new String(Files.readAllBytes(Paths.get(jsonFile.getPath()))));
                Map<String,String> vals = new HashMap<>();
                vals.put("name", json.getString("name"));
                int version = AssetRevision.parsePackageVersion(json.getString("version"));
                vals.put("version", AssetRevision.formatPackageVersion(++version));
                String schemaVer = json.optString("schemaVer");
                if (schemaVer.isEmpty() || schemaVer.startsWith("5") || schemaVer.equals("6.0")) {
                    schemaVer = "6.1";
                }
                vals.put("schemaVersion", schemaVer);
                YamlBuilder yamlBuilder = new YamlBuilder();
                yamlBuilder.append(vals);
                System.out.println("    Writing: " + yamlFile);
                Files.write(Paths.get(yamlFile.getPath()), yamlBuilder.toString().getBytes());
                System.out.println("    Deleting: " + jsonFile);
                new Delete(jsonFile).run();
            }
        }
    }

    protected void convertProperties() throws IOException {
        InputStream mapIn;
        if (map == null) {
            mapIn = getClass().getClassLoader().getResourceAsStream("META-INF/mdw/configurations.map");
        }
        else {
            System.out.println("Mapping from " + map.getAbsolutePath());
            mapIn = new FileInputStream(map);
        }
        Properties mapProps = new Properties();
        mapProps.load(mapIn);

        if (input == null) {
            input = new File(getConfigRoot() + "/mdw.properties");
        }
        System.out.println("Loading properties from " + input.getAbsolutePath());
        Properties inputProps = new OrderedProperties();
        inputProps.load(new FileInputStream(input));

        String baseName = input.getName().substring(0, input.getName().lastIndexOf('.'));
        if (prefix == null && baseName.equals("mdw"))
            prefix = "mdw";

        try {
            YamlBuilder yamlBuilder = YamlProperties.translate(prefix, inputProps, mapProps);
            File out = new File(getConfigRoot() + "/" + baseName + ".yaml");
            System.out.println("Writing output config: " + out.getAbsolutePath());
            Files.write(Paths.get(out.getPath()), yamlBuilder.toString().getBytes());
        }
        catch (ReflectiveOperationException ex) {
            throw new IOException(ex.getMessage(), ex);
        }
    }

    protected void convertImplementor() throws IOException {
        JSONObject implJson = new JSONObject(new String(Files.readAllBytes(input.toPath())));
        String implClass = implJson.getString("implementorClass");
        String outPath = getAssetRoot() + "/" + implClass.replace(".", "/");
        String suffix = "kotlin".equals(language) || "kt".equals(language) ? "kt" : "java";
        File outFile = new File(outPath += "." + suffix);
        if (outFile.isFile()) {
            throw new IOException("Asset file already exists: " + outFile);
        }
        if (!outFile.getParentFile().isDirectory() && !outFile.getParentFile().mkdirs()) {
            throw new IOException("Asset path cannot be created: " + outFile);
        }

        System.out.println("Creating: " + outFile);

        String label = implJson.getString("label");
        String imports = "import com.centurylink.mdw.annotations.Activity" + (suffix.equals("kt") ? "" : ";") + "\n";
        String annotations = "@Activity(value=\"" + label + "\"";

        String category = "com.centurylink.mdw.activity.types.GeneralActivity";
        if (implJson.has("category")) {
            category = implJson.getString("category");
            imports += "import " + category + (suffix.equals("kt") ? "" : ";") + "\n";
        }
        String categoryClass = category.substring(category.lastIndexOf('.') + 1);
        annotations += ", category=" + categoryClass + (suffix.equals("kt") ?  "::class" : ".class");

        String icon = "shape:activity";
        if (implJson.has("icon"))
            icon = implJson.getString("icon");
        annotations += ", icon=\"" + icon + "\"";

        String pageletXml = implJson.has("pagelet") ? implJson.getString("pagelet") : null;
        if (pageletXml != null) {
            try {
                JSONObject pagelet = new Pagelet(pageletXml).getJson();
                System.out.println("pagelet for formatted pasting: " + pagelet.toString(2));
                annotations += ",\n                pagelet=" + JSONObject.quote(pagelet.toString());
            }
            catch (Exception ex) {
                throw new IOException(ex);
            }
        }

        annotations += ")\n";

        downloadTemplates();
        File templateFile = new File(getTemplateDir() + "/assets/code/activity/general_" + suffix);
        String template = new String(Files.readAllBytes(templateFile.toPath()));
        Map<String,Object> values = new HashMap<>();
        values.put("packageName", implClass.substring(0, implClass.lastIndexOf(".")));
        values.put("className", implClass.substring(implClass.lastIndexOf(".") + 1));
        values.put("annotationsImports", imports);
        values.put("annotations", annotations);
        String source = substitute(template, values);
        Files.write(outFile.toPath(), source.getBytes(), StandardOpenOption.CREATE_NEW);
    }

}
