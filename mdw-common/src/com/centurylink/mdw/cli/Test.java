/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.cli;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

@Parameters(commandNames="test", commandDescription="test command line options")
public class Test {

    @Parameter(names = "args", variableArity = true)
    public List<String> args = new ArrayList<>();

    public void run() throws IOException {
        System.out.println("ARGS:");
        if (args != null) {
            for (String arg : args) {
                System.out.println("  " + arg);
            }
        }


    }

    public static void main(String[] args) throws IOException {
        Test test = new Test();
        JCommander j = new JCommander();
        j.addCommand("test", test);
        j.parse(new String[]{"test", "args", "arg1", "arg2"});
        test.run();
    }
}
