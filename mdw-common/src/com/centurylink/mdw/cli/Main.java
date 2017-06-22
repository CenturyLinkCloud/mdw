/**
 * Copyright (c) 2017 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.cli;

import java.io.IOException;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;

public class Main {

    public static void main(String[] args) throws IOException {

        Main main = new Main();
        Init init = new Init();
        Update update = new Update();

        JCommander cmd = JCommander.newBuilder()
            .addObject(main)
            .addCommand("help", new Help())
            .addCommand("init", init)
            .addCommand("update", update)
            .build();

        cmd.setProgramName("mdw");

        try {
            cmd.parse(args);
            String command = cmd.getParsedCommand();
            if (command == null || command.equals("help")) {
                cmd.usage();
            }
            else if (command.equals("version")) {
                System.out.println();
            }
            else if (command.equals("init")) {
                init.run();
            }
            else if (command.equals("update")) {
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
    static class Help {
    }
}
