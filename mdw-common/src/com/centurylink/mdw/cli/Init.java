/**
 * Copyright (c) 2017 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.cli;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.centurylink.mdw.util.file.FileHelper;
import com.centurylink.mdw.util.file.ZipHelper;
import com.google.common.io.Files;

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
    private String mdwVersion = "6.0.04-SNAPSHOT";

    @Parameter(names="--discovery-url", description="Asset Discovery URL")
    private String discoveryUrl = "https://mdw.useast.appfog.ctl.io/mdw";

    @Parameter(names="--releases-url", description="MDW Releases Maven Repo URL")
    private String releasesUrl = "http://repo.maven.apache.org/maven2";

    public void run() throws IOException {
        System.out.println("initializing " + project + "...");
        File projectDir = new File(project);
        if (projectDir.exists()) {
            if (!FileHelper.isEmpty(projectDir))
                throw new CliException(projectDir + " already exists and is not an empty directory");
        }
        else {
            if (!projectDir.mkdirs())
                throw new IOException("Unable to create destination: " + projectDir);
        }

        String url = releasesUrl;
        if (!url.endsWith("/"))
            url += "/";
        url += "com/centurylink/mdw/mdw-templates/" + mdwVersion + "/mdw-templates-" + mdwVersion + ".zip";
        System.out.println(" - retrieving templates: " + url);
        ZipHelper.unzip(new URL(url), projectDir);
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
                String contents = new String(FileHelper.read(child));
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
                    Files.write(newContents.toString().getBytes(), child);
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
