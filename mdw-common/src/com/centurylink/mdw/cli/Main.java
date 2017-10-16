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
import java.util.Arrays;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;

public class Main {

    public static void main(String[] args) throws IOException {

        // this trimming is necessary on linux
        String[] cmdArgs = new String[args.length];
        for (int i = 0; i < args.length; i++)
            cmdArgs[i] = args[i].trim();

        if (cmdArgs.length > 1) {
            if (cmdArgs[0].equals("git")) {
                // git pass-through command
                Git.main(cmdArgs);
                return;
            }
            else if (cmdArgs[0].equals("archive")) {
                Archive.main(cmdArgs);
                return;
            }
        }

        Help help = new Help();
        Main main = new Main();
        Init init = new Init();
        Import mport = new Import();
        Update update = new Update();
        Install install = new Install();
        Run run = new Run();
        Git git = new Git();
        Archive asset = new Archive(false);
        Status status = new Status();
        Version version = new Version();

        JCommander cmd = JCommander.newBuilder()
            .addObject(main)
            .addCommand("help", help)
            .addCommand("init", init)
            .addCommand("import", mport)
            .addCommand("update", update)
            .addCommand("install", install)
            .addCommand("run", run)
            .addCommand("version", version)
            .addCommand("git", git)
            .addCommand("status", status)
            .addCommand("asset", asset)
            .build();

        cmd.setProgramName("mdw");

        try {
            cmd.parse(cmdArgs);
            if (!init.isEclipse() && Arrays.asList(cmdArgs).contains("--eclipse"))
                init.setEclipse(true); // needed to support superfluous setting
            String command = cmd.getParsedCommand();

            if (command == null || command.equals("help")) {
                cmd.usage();
            }
            else {
                version.run();
                Operation op = null;
                if (command.equals("init")) {
                    op = init;
                }
                else if (command.equals("import")) {
                    op = mport;
                }
                else if (command.equals("update")) {
                    checkLoc(update.getProjectDir());
                    op = update;
                }
                else if (command.equals("install")) {
                    if (install.getWebappsDir() == null)
                        checkLoc(install.getProjectDir());
                    op = install;
                }
                else if (command.equals("run")) {
                    checkLoc(run.getProjectDir());
                    op = run;
                }
                else if (command.equals("status")) {
                    checkLoc(run.getProjectDir());
                    op = status;
                }

                if (op == null) {
                    cmd.usage();
                }
                else {
                    if (op instanceof Setup) {
                        Setup setup = (Setup) op;
                        if (setup.isDebug())
                            setup.debug();
                        if (!setup.validate())
                            return;
                    }
                    op.run(getMonitor());
                }
            }
        }
        catch (ParameterException ex) {
            ex.printStackTrace();
            System.err.println(ex.getMessage());
            System.err.println("'mdw help' for usage information");
        }
    }

    @Parameters(commandNames="help", commandDescription="Syntax Help")
    static class Help { }

    /**
     * Every call with >= 100% progress will print a new line.
     */
    static ProgressMonitor getMonitor() {
        return prog -> {
            if ("\\".equals(System.getProperty("file.separator"))) {
                System.out.print("\b\b\b\b\b\b\b\b\b");
            }
            else {
                System.out.print("\r         \r");
            }
            if (prog >= 100)
                System.out.println(" --> Done");
            else if (prog <= 0) // don't report zero progress since it may indicate unknown
                System.out.print(" ... ");
            else
                System.out.printf(" --> %3d%%", prog);
        };
    }

    static void checkLoc(File projectDir) throws ParameterException {
        File props = new File(projectDir + "/config/mdw.properties");
        if (!props.isFile()) {
            throw new ParameterException("Invalid project: " + projectDir.getAbsolutePath());
        }
    }
}
