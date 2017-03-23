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
package com.centurylink.mdw.test;

import cucumber.api.cli.Main;

import java.util.Map;

/**
 * Allows running the Cucumber-JVM CLI with differing sets of system properties.
 */
public class CucumberTestMain {

    private String[] args;
    private Map<String,String> sysProps;

    public CucumberTestMain(String[] args, Map<String,String> sysProps) {
        this.args = args;
        this.sysProps = sysProps;
    }

    public byte execute() throws Throwable {
        for (String key : sysProps.keySet())
            System.setProperty(key, sysProps.get(key));

        ClassLoader contextClassLoader = null;
        try {
            contextClassLoader = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(Main.class.getClassLoader());
            return Main.run(args, Main.class.getClassLoader()); // use the Ant classloader
        }
        finally {
            if (contextClassLoader != null)
                Thread.currentThread().setContextClassLoader(contextClassLoader);
        }
    }
}
