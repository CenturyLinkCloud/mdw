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
import java.util.HashMap;
import java.util.Map;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;

public class Main {

    private static Map<String,Operation> operations = new HashMap<>();
    private static JCommander.Builder builder = JCommander.newBuilder();

    private static void addOperation(String name, Operation operation) {
        operations.put(name, operation);
        builder.addCommand(name, operation);
    }

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
        }

        Main main = new Main();
        Help help = new Help();
        builder.addObject(main).addCommand(help);
        addOperation("init", new Init());
        addOperation("import", new Import());
        addOperation("update", new Update());
        addOperation("install", new Install());
        addOperation("run", new Run());
        addOperation("stop", new Stop());
        addOperation("version", new Version());
        addOperation("git", new Git());
        addOperation("status", new Status());
        addOperation("test", new Test());
        addOperation("convert", new Convert());
        addOperation("codegen", new Codegen());
        addOperation("vercheck", new Vercheck());
        addOperation("export", new Export());
        addOperation("bpmnimport", new BpmnImport());
        addOperation("encrypt", new Encrypt());
        addOperation("decrypt", new Decrypt());
        addOperation("token", new Token());
        addOperation("paths", new Paths());
        addOperation("dbexport", new DbExport());
        addOperation("dbimport", new DbImport());
        addOperation("hierarchy", new Hierarchy());

        JCommander cmd = builder.build();

        cmd.setProgramName("mdw");

        try {
            cmd.parse(cmdArgs);
            String command = cmd.getParsedCommand();

            if (command == null || command.equals("help")) {
                cmd.usage();
            }
            else {
                Operation op = operations.get(command);

                if (op == null) {
                    cmd.usage();
                }
                else {
                    if (op instanceof Setup) {
                        Setup setup = (Setup) op;
                        String mdwConfig = setup.getMdwConfig();
                        Props.init(mdwConfig == null ? "mdw.yaml" : mdwConfig);
                        if (setup.isDebug())
                            setup.debug();
                        if (!setup.validate())
                            return;
                    }
                    op.run(getMonitor());
                    if (op instanceof Test && !((Test)op).isSuccess()) {
                        System.exit(-1);  // success visible to build script
                    }
                    if (op instanceof Vercheck) {
                        System.exit(((Vercheck)op).getErrorCount());
                    }
                }
            }
        }
        catch (ParameterException ex) {
            System.err.println(ex.getMessage());
            System.err.println("'mdw help' for usage information");
            for (String arg : args) {
                if (arg.equals("--debug")) {
                    ex.printStackTrace();
                }
            }
            System.exit(-1);
        }
    }



    @Parameters(commandNames="help", commandDescription="Syntax Help")
    static class Help { }

    /**
     * Every call with >= 100% progress will print a new line.
     */
    static ProgressMonitor getMonitor() {
        return new ProgressMonitor() {

            @Override
            public void message(String msg) {
                System.out.println(msg + "...");
            }

            @Override
            public void progress(int prog) {
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
            }

        };
    }
}
