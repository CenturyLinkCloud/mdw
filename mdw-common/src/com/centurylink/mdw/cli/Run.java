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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.converters.IParameterSplitter;

@Parameters(commandNames="run", commandDescription="Run MDW", separators="=")
public class Run {

    protected static List<String> defaultVmArgs = new ArrayList<>();
    static {
        defaultVmArgs.add("-Dmdw.runtime.env=dev");
        defaultVmArgs.add("-Dmdw.config.location=config");
    }

    private File projectDir;
    public File getProjectDir() { return projectDir; }

    Run() {
        // cli use only
        this.projectDir = new File(".");
    }
    public Run(File projectDir) {
        this.projectDir = projectDir;
    }

    @Parameter(names="--binaries-url", description="MDW Binaries")
    private String binariesUrl = "https://github.com/CenturyLinkCloud/mdw/releases";
    public String getBinariesUrl() { return binariesUrl; }
    public void setBinariesUrl(String url) { this.binariesUrl = url; }

    @Parameter(names="--vm-args", description="Java VM Arguments (enclose in quotes)",
            splitter=SpaceParameterSplitter.class)
    private List<String> vmArgs = defaultVmArgs;
    public List<String> getVmArgs() { return vmArgs; }
    public void setVmArgs(List<String> args) { this.vmArgs = args; }

    public void run() throws IOException {
        List<String> cmdLine = new ArrayList<>();
        cmdLine.add(getJava());
        cmdLine.add("-jar");
        cmdLine.addAll(getVmArgs());
        cmdLine.add(getMdwJar());
        ProcessBuilder builder = new ProcessBuilder(cmdLine);
        builder.redirectErrorStream(true);
        System.out.println("Starting process:");
        for (String cmd : cmdLine)
            System.out.print(cmd + " ");
        System.out.println("\n");
        Process process = builder.start();
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                try {
                    String port = "8080";
                    String ctx = "mdw";
                    for (String arg : getVmArgs()) {
                        if (arg.startsWith("-Dmdw.server.port=") || arg.equals("-Dserver.port=")) {
                            port = arg.substring(arg.indexOf('=') + 1);
                        }
                        else if (arg.startsWith("-Dmdw.server.contextPath=") || arg.equals("-Dserver.contextPath=")) {
                            port = arg.substring(arg.indexOf('=') + 1);
                            break;
                        }
                    }
                    new Download(new URL("http://localhost:" + port + "/" + ctx + "/Services/System/exit")).read();
                    process.waitFor();
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                    process.destroy();
                }
            }
        });
        new Thread(new Runnable() {
            public void run() {
                try (BufferedReader out = new BufferedReader(new InputStreamReader(process.getInputStream()));) {
                    out.lines().forEach(line -> {
                        System.out.println(line);
                    });
                }
                catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }).start();

        try {
            process.waitFor();
        }
        catch (InterruptedException ex) {
            process.destroy();
        }
    }

    protected String getJava() {
        return System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
    }

    protected String getMdwJar() throws IOException {
        String mdwVersion = new Version().getMdwVersion(getProjectDir());
        return "bin" + File.separator + "mdw-" + mdwVersion + ".jar";
    }

    static class SpaceParameterSplitter implements IParameterSplitter {
        @Override
        public List<String> split(String value) {
            return Arrays.asList(value.split(" "));
        }
    }
}
