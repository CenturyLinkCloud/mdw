package com.centurylink.mdw.cli;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import com.beust.jcommander.Parameters;

@Parameters(commandNames="version", commandDescription="MDW CLI Version")
public class Version implements Operation {

    public Version run(ProgressMonitor... progressMonitors) throws IOException {

        String name = getClass().getSimpleName() + ".class";
        String path = getClass().getResource(name).toString();
        if (path.startsWith("jar")) {
            String mf = path.substring(0, path.lastIndexOf("!") + 1) + "/META-INF/MANIFEST.MF";
            Manifest manifest = new Manifest(new URL(mf).openStream());
            Attributes attr = manifest.getMainAttributes();
            getOut().println("MDW CLI " + attr.getValue("MDW-Version") + " (" + attr.getValue("MDW-Build") + ")");
        }
        return this;
    }

    /**
     * This returns the CLIENT app MDW version (as opposed to the CLI version
     * returned by run());
     * TODO: Read mdw version from plugin.xml if present, and compare
     * with gradle.properties.
     * TODO: Support mdw version in pom.xml as alternative to gradle.properties.
     */
    public String getMdwVersion(File projectDir) throws IOException {
        File gradleProps = new File(projectDir + "/gradle.properties");
        if (!gradleProps.exists())
            throw new IOException("Missing: " + gradleProps.getAbsolutePath());
        Properties props = new Properties();
        props.load(new FileInputStream(gradleProps));
        return props.getProperty("mdwVersion");
    }
}
