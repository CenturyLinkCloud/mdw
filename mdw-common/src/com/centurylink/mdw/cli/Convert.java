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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.json.JSONObject;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.centurylink.mdw.config.OrderedProperties;
import com.centurylink.mdw.config.YamlBuilder;
import com.centurylink.mdw.config.YamlProperties;
import com.centurylink.mdw.dataaccess.AssetRevision;

@Parameters(commandNames="convert", commandDescription="Convert mdw/app property files, or package.json files to yaml", separators="=")
public class Convert extends Setup {

    @Parameter(names="--packages", description="Update package.json files to package.yaml (ignores other options)")
    private boolean packages;
    public boolean isPackages() { return packages; }
    public void setPackages(boolean packages) { this.packages = packages; }

    @Parameter(names="--input", description="Input property file")
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
            convertProperties();
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

}
