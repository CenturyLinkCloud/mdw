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
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

@Parameters(commandNames="init", commandDescription="Initialize an MDW project", separators="=")
public class Init {

    public Init(String project) {
        this.project = project;
    }

    Init() {
        // cli use only
    }

    @Parameter(description="<project>", required=true)
    private String project;

    @Parameter(names="--mdw-version", description="MDW Version")
    private String mdwVersion;
    public String getMdwVersion() { return mdwVersion; }
    public void setMdwVersion(String version) { this.mdwVersion = version; }

    @Parameter(names="--discovery-url", description="Asset Discovery URL")
    private String discoveryUrl = "https://mdw.useast.appfog.ctl.io/mdw";
    public String getDiscoveryUrl() { return discoveryUrl; }
    public void setDiscoveryUrl(String url) { this.discoveryUrl = url; }

    @Parameter(names="--releases-url", description="MDW Releases Maven Repo URL")
    private String releasesUrl = "http://repo.maven.apache.org/maven2";
    public String getReleasesUrl() { return releasesUrl; }
    public void setReleasesUrl(String url) { this.releasesUrl = url; }

    @Parameter(names="--snapshots", description="Whether to include snapshot builds")
    private boolean snapshots;
    public boolean isSnapshots() { return snapshots; }
    public void setSnapshots(boolean snapshots) { this.snapshots = snapshots; }

    @Parameter(names="--user", description="Dev user")
    private String user = System.getProperty("user.name");
    public String getUser() { return user; }
    public void setUser(String user) { this.user = user; }

    private File projectDir;
    public File getProjectDir() { return projectDir; }

    public void run() throws IOException {
        System.out.println("initializing " + project + "...");
        projectDir = new File(project);
        if (projectDir.exists()) {
            if (!projectDir.isDirectory() || projectDir.list().length > 0)
                throw new CliException(projectDir + " already exists and is not an empty directory");
        }
        else {
            if (!projectDir.mkdirs())
                throw new IOException("Unable to create destination: " + projectDir);
        }

        if (!releasesUrl.endsWith("/"))
            releasesUrl += "/";

        if (mdwVersion == null) {
            // find latest non-snapshot
            URL url = new URL(releasesUrl + "com/centurylink/mdw/mdw-templates/");
            Crawl crawl = new Crawl(url, snapshots);
            crawl.run();
            if (crawl.getReleases().size() == 0)
                throw new IOException("Unable to locate MDW releases: " + url);
            mdwVersion = crawl.getReleases().get(crawl.getReleases().size() - 1);
        }

        String templatesUrl = releasesUrl + "com/centurylink/mdw/mdw-templates/" + mdwVersion
                + "/mdw-templates-" + mdwVersion + ".zip";
        System.out.println(" - retrieving templates: " + templatesUrl);
        File tempZip = File.createTempFile("mdw", ".zip", null);
        new Download(new URL(templatesUrl), tempZip).run();
        new Unzip(tempZip, projectDir).run();
        System.out.println(" - wrote: ");
        subst(projectDir);
    }

    static final Pattern SUBST_PATTERN = Pattern.compile("\\{\\{(.*)}}");

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
                    Object value = match;
                    try {
                        String name = toCamel(match.substring(2, match.length() - 2));
                        Field field = Init.class.getDeclaredField(name);
                        field.setAccessible(true);
                        value = field.get(this);
                    }
                    catch (NoSuchFieldException ex) {
                        // no subst
                    }
                    catch (IllegalAccessException ex) {
                        throw new IOException(ex.getMessage(), ex);
                    }
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

    private static String toCamel(String hyphenated) {
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

}
