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

        if (args.length > 1 && args[0].equals("git")) {
            // git pass-through command
            Git.main(args);
            return;
        }

        Help help = new Help();
        Main main = new Main();
        Init init = new Init();
        Import mport = new Import();
        Update update = new Update();
        Install install = new Install();
        Run run = new Run();
        Git git = new Git();
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
            .build();

        cmd.setProgramName("mdw");

        try {
            cmd.parse(args);
            if (!init.isEclipse() && Arrays.asList(args).contains("--eclipse"))
                init.setEclipse(true); // needed to support superfluous setting
            String command = cmd.getParsedCommand();

            if (command == null || command.equals("help")) {
                cmd.usage();
            }
            else {
                version.run();
                if (command.equals("init")) {
                    init.run(getMonitor());
                    new Update(init).run(getMonitor());
                }
                else if (command.equals("import")) {
                    mport.run(getMonitor());
                }
                else if (command.equals("update")) {
                    checkLoc(update.getProjectDir());
                    update.run(getMonitor());
                }
                else if (command.equals("install")) {
                    checkLoc(install.getProjectDir());
                    install.run(getMonitor());
                }
                else if (command.equals("run")) {
                    checkLoc(run.getProjectDir());
                    run.run(getMonitor());
                }
            }
        }
        catch (ParameterException ex) {
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
            System.out.print("\b\b\b\b\b\b\b\b\b");
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
