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

        // TODO: user reflection to instantiate appropriate command
        Help help = new Help();
        Main main = new Main();
        Init init = new Init();
        Import mport = new Import();
        Update update = new Update();
        Install install = new Install();
        Run run = new Run();
        Stop stop = new Stop();
        Git git = new Git();
        Test test = new Test();
        Status status = new Status();
        Version version = new Version();
        Convert convert = new Convert();
        Codegen codegen = new Codegen();
        Vercheck vercheck = new Vercheck();
        Export export = new Export();
        BpmnImport bpmnimport = new BpmnImport();
        Encrypt encrypt = new Encrypt();
        Decrypt decrypt = new Decrypt();
        Token token = new Token();

        JCommander cmd = JCommander.newBuilder()
            .addObject(main)
            .addCommand("help", help)
            .addCommand("init", init)
            .addCommand("import", mport)
            .addCommand("update", update)
            .addCommand("install", install)
            .addCommand("run", run)
            .addCommand("stop", stop)
            .addCommand("version", version)
            .addCommand("git", git)
            .addCommand("status", status)
            .addCommand("test", test)
            .addCommand("convert", convert)
            .addCommand("codegen", codegen)
            .addCommand("vercheck", vercheck)
            .addCommand("export", export)
            .addCommand("bpmnimport", bpmnimport)
            .addCommand("encrypt", encrypt)
            .addCommand("decrypt", decrypt)
            .addCommand("token", token)
            .build();

        cmd.setProgramName("mdw");

        try {
            cmd.parse(cmdArgs);
            String command = cmd.getParsedCommand();

            if (command == null || command.equals("help")) {
                cmd.usage();
            }
            else {
                Operation op = null;
                if (command.equals("init")) {
                    op = init;
                }
                else if (command.equals("import")) {
                    op = mport;
                }
                else if (command.equals("update")) {
                    op = update;
                }
                else if (command.equals("install")) {
                    op = install;
                }
                else if (command.equals("run")) {
                    op = run;
                }
                else if (command.equals("stop")) {
                    op = stop;
                }
                else if (command.equals("status")) {
                    op = status;
                }
                else if (command.equals("test")) {
                    op = test;
                }
                else if (command.equals("version")) {
                    op = version;
                }
                else if (command.equals("convert")) {
                    op = convert;
                }
                else if (command.equals("codegen")) {
                    op = codegen;
                }
                else if (command.equals("vercheck")) {
                    op = vercheck;
                }
                else if (command.equals("export")) {
                    op = export;
                }
                else if (command.equals("bpmnimport")) {
                    op = bpmnimport;
                }
                else if (command.equals("encrypt")) {
                    op = encrypt;
                }
                else if (command.equals("decrypt")) {
                    op = decrypt;
                }
                else if (command.equals("token")) {
                    op = token;
                }

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
