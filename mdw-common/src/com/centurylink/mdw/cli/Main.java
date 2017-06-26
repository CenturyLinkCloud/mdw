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

import java.io.IOException;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;

public class Main {

    public static void main(String[] args) throws IOException {

        Help help = new Help();
        Main main = new Main();
        Init init = new Init();
        Update update = new Update();
        Install install = new Install();
        Run run = new Run();
        Version version = new Version();

        JCommander cmd = JCommander.newBuilder()
            .addObject(main)
            .addCommand("help", help)
            .addCommand("init", init)
            .addCommand("update", update)
            .addCommand("install", install)
            .addCommand("run", run)
            .addCommand("version", version)
            .build();

        cmd.setProgramName("mdw");

        try {
            cmd.parse(args);
            String command = cmd.getParsedCommand();
            if (command == null || command.equals("help")) {
                cmd.usage();
            }
            else {
                version.run();
                if (command.equals("init")) {
                    init.run();
                    new Update(init).run();
                }
                else if (command.equals("update")) {
                    update.run();
                }
                else if (command.equals("install")) {
                    install.run();
                }
            }
        }
        catch (ParameterException ex) {
            System.err.println(ex.getMessage());
            cmd.usage();
        }
        catch (CliException ex) {
            System.err.println(ex.getMessage());
        }
    }

    @Parameters(commandNames="help", commandDescription="Syntax Help")
    static class Help { }
}
