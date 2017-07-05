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
package com.centurylink.mdw.cli;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.converters.CommaParameterSplitter;

public abstract class Setup {

    protected static List<String> defaultBasePackages = new ArrayList<>();
    static {
        defaultBasePackages.add("com.centurylink.mdw.base");
        defaultBasePackages.add("com.centurylink.mdw.db");
        defaultBasePackages.add("com.centurylink.mdw.testing");
    }

    File projectDir;
    public File getProjectDir() { return projectDir; }

    @Parameter(names="--mdw-version", description="MDW Version")
    private String mdwVersion;
    public String getMdwVersion() { return mdwVersion; }
    public void setMdwVersion(String version) { this.mdwVersion = version; }

    @Parameter(names="--discovery-url", description="Asset Discovery URL")
    private String discoveryUrl = "https://mdw.useast.appfog.ctl.io/mdw";
    public String getDiscoveryUrl() { return discoveryUrl; }
    public void setDiscoveryUrl(String url) { this.discoveryUrl = url; }

    @Parameter(names="--base-asset-packages", description="MDW Base Asset Packages (comma-separated)",
            splitter=CommaParameterSplitter.class)
    private List<String> baseAssetPackages;
    public List<String> getBaseAssetPackages() { return baseAssetPackages; }
    public void setBaseAssetPackages(List<String> packages) { this.baseAssetPackages = packages; }

    /**
     * Checks for any existing packages.  If none present, adds the defaults.
     */
    protected void initBaseAssetPackages() throws IOException {
        baseAssetPackages = new ArrayList<>();
        String assetLoc = getProperty("mdw.asset.location");
        File assetDir = new File(projectDir + "/" + assetLoc);
        addBasePackages(assetDir, assetDir);
        if (baseAssetPackages.isEmpty())
            baseAssetPackages = defaultBasePackages;
    }

    private void addBasePackages(File assetDir, File dir) throws IOException {
        if (!dir.exists() && !dir.mkdirs())
            throw new IOException("Cannot create directory: " + dir.getAbsolutePath());
        if (!dir.isDirectory())
            throw new IOException("Expected directory: " + dir.getAbsolutePath());

        if (new File(dir + "/.mdw/package.json").isFile()) {
            String pkg = dir.getAbsolutePath().substring(assetDir.getAbsolutePath().length() - 1).replace('/', '.').replace('\\', '.');
            if (pkg.startsWith("com.centurylink.mdw."))
                baseAssetPackages.add(pkg);
        }
        for (File child : dir.listFiles()) {
            if (child.isDirectory())
                addBasePackages(assetDir, child);
        }
    }

    protected Setup() {
        projectDir = new File(".");
    }

    /**
     * Copies param values
     */
    public Setup(Setup cloneFrom) {
        projectDir = cloneFrom.getProjectDir();
        mdwVersion = cloneFrom.getMdwVersion();
        discoveryUrl = cloneFrom.getDiscoveryUrl();
        baseAssetPackages = cloneFrom.getBaseAssetPackages();
    }

    static final Pattern SUBST_PATTERN = Pattern.compile("\\{\\{(.*?)}}");

    protected void subst(File dir) throws IOException {
        for (File child : dir.listFiles()) {
            if (child.isDirectory()) {
                subst(child);
            }
            else {
                String contents = new String(Files.readAllBytes(Paths.get(child.getPath())));
                StringBuilder newContents = new StringBuilder(contents.length());
                int index = 0;
                Matcher matcher = SUBST_PATTERN.matcher(contents);
                while (matcher.find()) {
                    String match = matcher.group();
                    newContents.append(contents.substring(index, matcher.start()));
                    Object value = getValue(match.substring(2, match.length() - 2));
                    if (value == null)
                        value = match;
                    newContents.append(value == null ? "" : value);
                    index = matcher.end();
                }
                newContents.append(contents.substring(index));
                if (newContents.toString().equals(contents)) {
                    System.out.println("   " + child);
                }
                else {
                    Files.write(Paths.get(child.getPath()), newContents.toString().getBytes());
                    System.out.println("   " + child + " *");
                }
            }
        }
    }

    public Object getValue(String name) {
        name = toCamel(name);
        Field field = null;
        try {
            field = Init.class.getDeclaredField(name);
        }
        catch (NoSuchFieldException ex) {
            try {
                field = Setup.class.getDeclaredField(name);
            }
            catch (NoSuchFieldException ex2) {
                // no subst
            }
        }
        if (field != null) {
            try {
                field.setAccessible(true);
                return field.get(this);
            }
            catch (IllegalAccessException ex) {
                throw new IllegalArgumentException(ex.getMessage(), ex);
            }
        }
        return null;
    }

    public String toCamel(String hyphenated) {
        StringBuilder nameBuilder = new StringBuilder(hyphenated.length());
        boolean capNextChar = false;
        for (char c : hyphenated.toCharArray()) {
            if (c == '-') {
                capNextChar = true;
                continue;
            }
            if (capNextChar) {
                nameBuilder.append(Character.toUpperCase(c));
            }
            else {
                nameBuilder.append(c);
            }
            capNextChar = false;
        }
        return nameBuilder.toString();
    }

    protected Properties getProperties() throws IOException {
        File propFile = new File(projectDir + "/config/mdw.properties");
        if (!propFile.exists())
            throw new IOException("Missing: " + propFile.getAbsolutePath());
        Properties props = new Properties();
        props.load(new FileInputStream(propFile));
        return props;
    }

    /**
     * Reloads every time.  Use {@link #getProperties()} for multiple.
     */
    protected String getProperty(String name) throws IOException {
        String prop = getProperties().getProperty(name);
        if (prop == null)
            throw new IOException("Missing in config/mdw.properties: " + name);
        return prop;
    }
}
