/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
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
