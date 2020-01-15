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

import com.beust.jcommander.Parameter;
import com.centurylink.mdw.config.YamlProperties;
import com.centurylink.mdw.model.Yamlable;
import com.centurylink.mdw.model.project.Data;
import com.centurylink.mdw.model.system.MdwVersion;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.util.file.Packages;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("unused")
public abstract class Setup implements Operation {

    protected static final String META_DIR = ".mdw";
    protected static final String VERSIONS = "versions";
    protected static final String MDW_COMMON_PATH = "/com/centurylink/mdw/mdw-templates/";
    public static final String MAVEN_CENTRAL_URL = "https://repo.maven.apache.org/maven2";
    public static final String SONATYPE_URL = "https://oss.sonatype.org/service/local/artifact/maven";

    Setup() {
    }

    protected Setup(File projectDir) {
        this.projectDir = projectDir;
    }

    private PrintStream out = System.out;
    public PrintStream getOut() { return out; }
    public void setOut(PrintStream out) { this.out = out; }

    private PrintStream err = System.err;
    public PrintStream getErr() { return err; }
    public void setErr(PrintStream err) { this.err = err; }

    @Parameter(names="--project-dir", description="Project directory (default = ./)")
    protected File projectDir;
    public File getProjectDir() {
        return projectDir == null ? new File(".") : projectDir;
    }

    @Parameter(names="--debug", description="Display CLI debug information")
    private boolean debug;
    public boolean isDebug() { return debug; }
    public void setDebug(boolean debug) { this.debug = debug; }

    @Parameter(names="--discovery-url", description="Asset Discovery URL")
    private String discoveryUrl = MAVEN_CENTRAL_URL;
    public String getDiscoveryUrl() { return discoveryUrl; }

    public void setDiscoveryUrl(String url) {
        this.discoveryUrl = url;
        if (Props.DISCOVERY_URL != null)
            Props.DISCOVERY_URL.specified = true;
    }

    @Parameter(names="--releases-url", description="MDW releases Maven repo URL")
    private String releasesUrl = MAVEN_CENTRAL_URL;
    public String getReleasesUrl() {
        return releasesUrl.endsWith("/") ? releasesUrl.substring(0, releasesUrl.length() - 1) : releasesUrl;
    }
    public void setReleasesUrl(String url) {
        this.releasesUrl = url;
        Props.Gradle.MAVEN_REPO_URL.specified = true;
    }

    @Parameter(names="--services-url", description="MDW service base URL")
    private String servicesUrl = "http://localhost:8080/mdw/services";
    public String getServicesUrl() {
        return servicesUrl.endsWith("/") ? servicesUrl.substring(0, servicesUrl.length() - 1) : servicesUrl;
    }
    public void setServicesUrl(String url) {
        this.servicesUrl = url;
        Props.SERVICES_URL.specified = true;
    }

    @Parameter(names="--base-asset-packages", description="MDW Base Asset Packages (comma-separated)")
    private List<String> baseAssetPackages;
    public List<String> getBaseAssetPackages() { return baseAssetPackages; }
    public void setBaseAssetPackages(List<String> packages) { this.baseAssetPackages = packages; }

    // Git Parameters used by Import (otherwise only for templates)
    @Parameter(names="--git-remote-url", description="Git repository URL")
    private String gitRemoteUrl = "https://github.com/CenturyLinkCloud/mdw-demo.git";
    public String getGitRemoteUrl() { return gitRemoteUrl; }
    public void setGitRemoteUrl(String url) {
        this.gitRemoteUrl = url;
        Props.Git.REMOTE_URL.specified = true;
    }

    @Parameter(names="--git-branch", description="Git branch")
    private String gitBranch = "master";
    public String getGitBranch() { return gitBranch; }
    public void setGitBranch(String branch) {
        this.gitBranch = branch;
        Props.Git.BRANCH.specified = true;
    }

    @Parameter(names="--git-user", description="Git user")
    private String gitUser = "anonymous";
    public String getGitUser() { return gitUser; }
    public void setGitUser(String user) {
        this.gitUser = user;
        Props.Git.USER.specified = true;
    }

    @Parameter(names="--git-password", description="Git password")
    private String gitPassword;
    String getGitPassword() { return this.gitPassword; }
    public void setGitPassword(String password) {
        this.gitPassword = password;
        Props.Git.PASSWORD.specified = true;
    }

    @Parameter(names="--database-url", description="JDBC URL (without credentials)")
    private String databaseUrl = "jdbc:mariadb://localhost:3308/mdw";
    public String getDatabaseUrl() { return databaseUrl; }
    public void setDatabaseUrl(String url) {
        this.databaseUrl = url;
        Props.Db.URL.specified = true;
    }

    @Parameter(names="--database-user", description="DB User")
    private String databaseUser = "mdw";
    public String getDatabaseUser() { return databaseUser; }
    public void setDatabaseUser(String user) {
        this.databaseUser = user;
        Props.Db.USER.specified = true;
    }

    @SuppressWarnings("squid:S2068")
    @Parameter(names="--database-password", description="DB Password")
    private String databasePassword = "mdw";
    public String getDatabasePassword() { return databasePassword; }
    public void setDatabasePassword(String password) {
        this.databasePassword = password;
        Props.Db.PASSWORD.specified = true;
    }

    private String databaseDriver = "org.mariadb.jdbc.Driver";
    public String getDatabaseDriver() {
        String driver = DbInfo.getDatabaseDriver(getDatabaseUrl());
        return driver == null ? databaseDriver : driver;
    }
    public void setDatabaseDriver(String driver) { this.databaseDriver = driver; }

    @Parameter(names="--source-group", description="Source code group name")
    protected String sourceGroup;
    public String getSourceGroup() {
        return sourceGroup == null ? "com.example." + getProjectDir().getName() : sourceGroup;
    }
    public void setSourceGroup(String sourceGroup) {
        this.sourceGroup = sourceGroup;
        Props.Gradle.SOURCE_GROUP.specified = true;
    }

    @Parameter(names="--template-dir", description="Template Directory")
    protected String templateDir;
    public void setTemplateDir(String templateDir) {
        this.templateDir = templateDir;
    }

    @Parameter(names="--snapshots-url", description="MDW snapshot releases Sonatype repo URL")
    private String snapshotsUrl = "https://oss.sonatype.org/content/repositories/snapshots";
    public String getSnapshotsUrl() {
        return snapshotsUrl.endsWith("/") ? snapshotsUrl.substring(0, snapshotsUrl.length() - 1) : snapshotsUrl;
    }
    public void setSnapshotsUrl(String url) {
        this.snapshotsUrl = url;
        Props.Gradle.SONATYPE_REPO_URL.specified = true;
    }

    private YamlProperties projectYaml;
    public YamlProperties getProjectYaml() throws IOException {
        if (projectYaml == null) {
            File yamlFile = new File(getProjectDir() + "/project.yaml");
            if (yamlFile.isFile())
                projectYaml = new YamlProperties(yamlFile);
        }
        return projectYaml;
    }

    private String mdwVersion;
    /**
     * Reads from project.yaml
     */
    public String getMdwVersion() throws IOException {
        if (mdwVersion == null) {
            YamlProperties yaml = getProjectYaml();
            if (yaml != null) {
                mdwVersion = yaml.getString(Props.ProjectYaml.MDW_VERSION);
            }
        }
        return mdwVersion;
    }
    public void setMdwVersion(String version) { this.mdwVersion = version; }

    /**
     * Reads from project.yaml
     */
    public boolean isSnapshots() throws IOException {
        String version = getMdwVersion();
        return version != null && version.endsWith("-SNAPSHOT");
    }

    @Parameter(names="--config-loc", description="Config location (default is 'config')")
    protected String configLoc;
    public String getConfigLoc() throws IOException {
        if (configLoc == null) {
            YamlProperties yaml = getProjectYaml();
            if (yaml != null) {
                configLoc = yaml.getString(Props.ProjectYaml.CONFIG_LOC);
            }
            if (configLoc == null) {
                configLoc = projectDir == null ? "config" : projectDir + "/config";
            }
        }
        return configLoc;
    }
    public void setConfigLoc(String configLoc) { this.configLoc = configLoc; }

    /**
     * Asset loc is taken from project.yaml, falling back to mdw.yaml, and finally "assets"
     */
    @Parameter(names="--asset-loc", description="Asset location (default is 'assets')")
    protected String assetLoc;
    public String getAssetLoc() throws IOException {
        if (assetLoc == null) {
            YamlProperties yaml = getProjectYaml();
            if (yaml != null) {
                assetLoc = yaml.getString(Props.ProjectYaml.ASSET_LOC);
            }
            if (assetLoc == null) {
                assetLoc = new Props(this).get(Props.ASSET_LOC, false);
            }
            if (assetLoc == null) {
                assetLoc = projectDir == null ? "assets" : projectDir + "/assets";
            }
        }
        return assetLoc;
    }
    public void setAssetLoc(String assetLoc) {
        this.assetLoc = assetLoc;
        if (Props.ASSET_LOC != null)
            Props.ASSET_LOC.specified = true;
    }

    /**
     * Crawls to find the latest stable version.
     */
    public String findMdwVersion(boolean snapshots) throws IOException {
        URL url = new URL(getReleasesUrl() + MDW_COMMON_PATH);
        if (snapshots)
            url = new URL(getSnapshotsUrl() + MDW_COMMON_PATH);
        Crawl crawl = new Crawl(url, snapshots);
        crawl.run();
        if (crawl.getReleases().size() == 0)
            throw new IOException("Unable to locate MDW releases: " + url);
        return crawl.getReleases().get(crawl.getReleases().size() - 1);
    }

    /**
     * Checks for any existing packages.  If none present, adds the defaults.
     */
    protected void initBaseAssetPackages() throws IOException {
        baseAssetPackages = new ArrayList<>();
        addBasePackages(getAssetRoot(), getAssetRoot());
        if (baseAssetPackages.isEmpty())
            baseAssetPackages = Packages.DEFAULT_BASE_PACKAGES;
    }

    private void addBasePackages(File assetDir, File dir) throws IOException {
        if (!dir.exists() && !dir.mkdirs())
            throw new IOException("Cannot create directory: " + dir.getAbsolutePath());
        if (!dir.isDirectory())
            throw new IOException("Expected directory: " + dir.getAbsolutePath());

        if (new File(dir + "/.mdw/package.json").isFile() || new File(dir + "/.mdw/package.yaml").isFile()) {
            String pkg = getAssetPath(dir).replace('/', '.');
            if (Packages.isMdwPackage(pkg))
                baseAssetPackages.add(pkg);
        }
        for (File child : dir.listFiles()) {
            if (child.isDirectory())
                addBasePackages(assetDir, child);
        }
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
                    newContents.append(value);
                    index = matcher.end();
                }
                newContents.append(contents.substring(index));
                if (newContents.toString().equals(contents)) {
                    getOut().println("   " + child);
                }
                else {
                    Files.write(Paths.get(child.getPath()), newContents.toString().getBytes());
                    getOut().println("   " + child + " *");
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

    public File getConfigRoot() {
        if (configLoc != null)
            return new File(configLoc);
        return new File(getProjectDir() + "/config");
    }

    /**
     * Returns 'to' file or dir path relative to 'from' dir.
     * Result always uses forward slashes and has no trailing slash.
     */
    public String getRelativePath(File from, File to) {
        Path fromPath = Paths.get(from.getPath()).normalize().toAbsolutePath();
        Path toPath = Paths.get(to.getPath()).normalize().toAbsolutePath();
        return fromPath.relativize(toPath).toString().replace('\\', '/');
    }

    public String getAssetPath(File file) throws IOException {
        return getRelativePath(getAssetRoot(), file);
    }

    public String getPackageName(String assetPath) {
        int lastSlash = assetPath.lastIndexOf('/');
        return lastSlash > 0 ? assetPath.substring(0, lastSlash).replace('/', '.') : null;
    }
    public String getAssetName(String assetPath) {
        int lastSlash = assetPath.lastIndexOf('/');
        return lastSlash < assetPath.length() ? assetPath.substring(lastSlash + 1) : assetPath;
    }
    public File getAssetFile(String assetPath) throws IOException {
        String packageName = getPackageName(assetPath);
        if (packageName == null)
            return null;
        return new File(getAssetRoot() + "/" + packageName.replace('.', '/') + "/" + getAssetName(assetPath));
    }

    public File getAssetRoot() throws IOException {
        return new File(getAssetLoc());
    }

    private File gitRoot;
    public File getGitRoot() throws IOException {
        if (gitRoot != null)
            return gitRoot; // was set programmatically
        Props props = new Props(this);
        String gitLocalPath = props.get("mdw.git.local.path");
        if (gitLocalPath != null) {
            File gitRoot = new File(gitLocalPath);
            if (gitRoot.isAbsolute() || getProjectDir() == null)
                return gitRoot;
            else
                return new File(getProjectDir() + "/" + gitLocalPath);
        }
        return getProjectDir();
    }
    public void setGitRoot(File gitRoot) {
        this.gitRoot = gitRoot;
    }


    public String getGitPath(File file) throws IOException {
        return getRelativePath(getGitRoot(), file);
    }

    public boolean gitExists() throws IOException {
        return new File(getGitRoot() + "/.git").isDirectory();
    }

    public String getMdwConfig() {
        String mdwConfig = null;
        File mdwYaml = new File(getConfigRoot() + "/mdw.yaml");
        if (mdwYaml.isFile()) {
            mdwConfig = mdwYaml.getName();
        }
        else {
            File mdwProps = new File(getConfigRoot() + "/mdw.properties");
            if (mdwProps.isFile()) {
                mdwConfig = mdwProps.getName();
            }
        }
        return mdwConfig;
    }

    public File getMdwHome() throws IOException {
        String mdwHome = System.getenv("MDW_HOME");
        if (mdwHome == null)
            mdwHome = System.getProperty("mdw.home");
        if (mdwHome == null)
            throw new IOException("Missing environment variable: MDW_HOME");
        File mdwDir = new File(mdwHome);
        if (!mdwDir.isDirectory())
            throw new IOException("MDW_HOME is not a directory: " + mdwDir.getAbsolutePath());
        return mdwDir;
    }

    public String getTemplatesUrl() throws IOException {
        String mdwVer = getMdwVersion();
        String templates = "mdw-templates-" + mdwVer + ".zip";
        String templatesUrl;
        String templatesLoc = System.getProperty("mdw.templates.url");
        if (templatesLoc != null) {
            // allows loading templates from a directory for testing
            templatesUrl = templatesLoc;
        }
        else {
            if (isSnapshots()) {
                templatesUrl = SONATYPE_URL
                        + "/redirect?r=snapshots&g=com.centurylink.mdw&a=mdw-templates&v=LATEST&p=zip";
            }
            else {
                templatesUrl = getReleasesUrl() + MDW_COMMON_PATH
                        + mdwVer + "/" + templates;
            }
        }
        return templatesUrl;
    }

    /**
     * Override for extended validation (always calling super.validate()).
     */
    public boolean validate() throws IOException {
        // check config
        if (needsConfig() && getMdwConfig() == null) {
            throw new IOException("Error: Missing config (mdw.yaml or mdw.properties)");
        }
        return true;
    }

    protected boolean needsConfig() {
        return true;
    }

    /**
     * Override for extended debug info (always calling super.debug()).
     */
    public void debug() throws IOException {
        status();
        getOut().println("CLI Props:");
        Props props = new Props(this);
        for (Prop prop : Props.ALL_PROPS) {
            getOut().println("  " + prop);
            getOut().println("    = " + props.get(prop, false));
        }
    }

    public void status() throws IOException {
        getOut().println("Project Dir:\n " + getProjectDir().getAbsolutePath());
        getOut().println("Config Root:\n " + getConfigRoot());
        getOut().println("Asset Root:\n  " + getAssetRoot());
        getOut().println("Git Root:\n  " + getGitRoot());
        getOut().println("Java:\n" + getJava());
    }

    public String getJava() {
        return System.getProperty("java.vm.name") + " " + System.getProperty("java.vm.version") +
                " (" + System.getProperty("java.home") + ")";
    }

    /**
     * Creates a package if it doesn't exist.  Only works from 6.1.
     */
    public void mkPackage(String name) throws IOException {
        File metaDir = new File(getAssetLoc() + "/" + name.replace('.', '/') + "/.mdw");
        if (!metaDir.exists() && !metaDir.mkdirs())
            throw new IOException("Cannot create directory: " + metaDir.getAbsolutePath());
        File pkgFile = new File(metaDir + "/package.yaml");
        if (!pkgFile.exists()) {
            JSONObject pkgJson = new JSONObject();
            pkgJson.put("name", name);
            pkgJson.put("version", "1.0.01");
            pkgJson.put("schemaVersion", "6.1");
            Files.write(Paths.get(pkgFile.getPath()), pkgJson.toString(2).getBytes());
        }
    }

    public void createAsset(String assetPath, byte[] content) throws IOException {
        int lastSlash = assetPath.lastIndexOf("/");
        if (lastSlash < 0 || lastSlash > assetPath.length() - 2)
            throw new IOException("Invalid asset path: " + assetPath);
        mkPackage(getPackageName(assetPath));
        Path filePath = Paths.get(getAssetFile(assetPath).getPath());
        Files.write(filePath, content);
    }

    protected File getTemplateDir() throws IOException {
        if (templateDir != null)
            return new File(templateDir);
        else {
            return new File(getMdwHome() + "/lib/" + getMdwVersion());
        }
    }

    /**
     * Downloads asset templates for codegen, etc.
     */
    protected void downloadTemplates(ProgressMonitor... monitors) throws IOException {
        File templateDir = getTemplateDir();
        if (!templateDir.exists()) {
            if (!templateDir.mkdirs())
                throw new IOException("Unable to create directory: " + templateDir.getAbsolutePath());
            String templatesUrl = getTemplatesUrl();
            getOut().println("Retrieving templates: " + templatesUrl);
            File tempZip = Files.createTempFile("mdw-templates-", ".zip").toFile();
            new Download(new URL(templatesUrl), tempZip).run(monitors);
            File tempDir = Files.createTempDirectory("mdw-templates-").toFile();
            new Unzip(tempZip, tempDir, false).run();
            File codegenDir = new File(templateDir + "/codegen");
            new Copy(new File(tempDir + "/codegen"), codegenDir, true).run();
            File assetsDir = new File(templateDir + "/assets");
            new Copy(new File(tempDir + "/assets"), assetsDir, true).run();
        }
    }

    public File getTempDir() throws IOException {
        String propVal = new Props(this).get(Props.TEMP_DIR, false);
        if (propVal == null)
            return null;
        return new File(propVal);
    }

    private Packages assetPackageDirs;
    protected Packages getAssetPackageDirs() throws IOException {
        if (assetPackageDirs == null) {
            assetPackageDirs = new Packages(getAssetRoot());
        }
        return assetPackageDirs;
    }

    public Map<String,List<File>> findAllAssets(String ext) throws IOException {
        Map<String,List<File>> matchingAssets = new HashMap<>();
        for (String pkg : getAssetPackageDirs().getPackageNames()) {
            for (File assetFile : getAssetPackageDirs().getAssetFiles(pkg)) {
                if (assetFile.getName().endsWith("." + ext)) {
                    List<File> pkgAssets = matchingAssets.get(pkg);
                    if (pkgAssets == null) {
                        pkgAssets = new ArrayList<>();
                        matchingAssets.put(pkg, pkgAssets);
                    }
                    pkgAssets.add(assetFile);
                }
            }
        }
        return matchingAssets;
    }

    protected List<File> getAssetFiles(String packageName) throws IOException {
        return getAssetPackageDirs().getAssetFiles(packageName);
    }

    protected String getBaseUrl() throws MalformedURLException {
        URL serviceUrl = new URL(getServicesUrl() + "/..");
        return serviceUrl.getProtocol() + "://" + serviceUrl.getHost() + ":" + serviceUrl.getPort() + "/" + serviceUrl.getPath();
    }

    protected void updateBuildFile() throws IOException {
        final Pattern repositoryPattern = Pattern.compile("maven\\s*\\{\\s*url\\s+\\w+\\s*}");
        String contents = new String(
                Files.readAllBytes(Paths.get(getProjectDir() + "/build.gradle")));
        StringBuilder newContents = new StringBuilder(contents.length());
        Matcher matcher = repositoryPattern.matcher(contents);
        if (matcher.find()) {
            newContents.append(contents.substring(0, matcher.end())).append("\n\t")
                    .append("maven { url snapshotsUrl }");
            newContents.append(contents.substring(matcher.end()));
            Files.write(Paths.get(getProjectDir() + "/build.gradle"),
                    newContents.toString().getBytes());
        }
    }

    protected Map<String,Properties> getVersionProps(Map<String,File> packageDirs) throws IOException {
        Map<String,Properties> versionProps = new HashMap<>();
        for (String pkg : packageDirs.keySet()) {
            File packageDir = packageDirs.get(pkg);
            Properties props = new Properties();
            props.load(new FileInputStream(packageDir + "/" + META_DIR + "/versions"));
            versionProps.put(pkg, props);
        }
        return versionProps;
    }

    static long getAssetId(String assetPath, int assetVersion) throws IOException {
        String logicalPath = assetPath + " v" + formatVersion(assetVersion);
        String blob = "blob " + logicalPath.length() + "\0" + logicalPath;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] bytes = md.digest(blob.getBytes());
            String h = byteArrayToHexString(bytes).substring(0, 7);
            return Long.parseLong(h, 16);
        }
        catch (NoSuchAlgorithmException ex) {
            throw new IOException(ex.getMessage(), ex);
        }
    }

    static String formatVersion(int version) {
        if (version == 0)
            return "0";
        else
            return version/1000 + "." + version%1000;
    }

    private static String byteArrayToHexString(byte[] b) {
        String result = "";
        for (byte value : b)
            result += Integer.toString((value & 0xff) + 0x100, 16).substring(1);
        return result;
    }

    /**
     * Simple substitution mechanism.  Missing values substituted with empty string.
     */
    public static String substitute(String input, Map<String,Object> values) {
        StringBuilder output = new StringBuilder(input.length());
        int index = 0;
        Matcher matcher = SUBST_PATTERN.matcher(input);
        while (matcher.find()) {
            String match = matcher.group();
            output.append(input.substring(index, matcher.start()));
            Object value = values.get(match.substring(2, match.length() - 2));
            output.append(value == null ? "" : String.valueOf(value));
            index = matcher.end();
        }
        output.append(input.substring(index));
        return output.toString();
    }

    protected boolean isCommandLine() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        StackTraceElement start = stackTrace[stackTrace.length - 1];
        return start.getClassName().equals(Main.class.getName()) && start.getMethodName().equals("main");
    }

    private Project project;
    public Project getProject() {
        if (project == null) {
            project = new Project();
        }
        return project;
    }

    Process loadProcess(String pkg, File procFile, boolean deep) throws IOException {
        Properties versionProps = getPackageVersions().get(pkg);
        Process process;
        if (deep) {
            String contents = new String(Files.readAllBytes(procFile.toPath()));
            if (contents.startsWith("{")) {
                process = Process.fromString(contents);
            }
            else {
                process = new Process(Yamlable.fromString(contents));
            }
        }
        else {
            process = new Process();
        }
        int lastDot = procFile.getName().lastIndexOf('.');
        String assetPath = pkg + "/" + procFile.getName();
        process.setName(procFile.getName().substring(0, lastDot));
        process.setPackageName(pkg);
        String verProp = versionProps == null ? null : versionProps.getProperty(procFile.getName());
        process.setVersion(verProp == null ? 0 : Integer.parseInt(verProp.split(" ")[0]));
        process.setId(getAssetId(assetPath, process.getVersion()));
        return process;
    }

    private Map<String,Properties> packageVersions;
    private Map<String,Properties> getPackageVersions() throws IOException {
        if (packageVersions == null)
            packageVersions = getVersionProps(getAssetPackageDirs());
        return packageVersions;
    }

    public class Project implements com.centurylink.mdw.model.project.Project {
        public File getAssetRoot() throws IOException {
            return Setup.this.getAssetRoot();
        }
        public String getHubRootUrl() throws IOException {
            return new Props(Setup.this).get(Props.HUB_URL);
        }
        public MdwVersion getMdwVersion() throws IOException {
            return new MdwVersion(Setup.this.getMdwVersion());
        }
        private Data data;
        public Data getData() {
            if (data == null)
                data = new Data(this);
            return data;
        }

        @Override
        public String readData(String name) throws IOException {
            YamlProperties projectYaml = getProjectYaml();
            return projectYaml == null ? null : projectYaml.getString(name);
        }

        @Override
        public List<String> readDataList(String name) throws IOException {
            YamlProperties projectYaml = getProjectYaml();
            return projectYaml == null ? null : projectYaml.getList(name);
        }

        @Override
        public SortedMap<String,String> readDataMap(String name) throws IOException {
            YamlProperties projectYaml = getProjectYaml();
            if (projectYaml == null)
                return null;
            Map<String,String> map = projectYaml.getMap(name);
            if (map == null)
                return null;
            TreeMap<String,String> sortedMap = new TreeMap<>();
            sortedMap.putAll(map);
            return sortedMap;
        }
    }
}
