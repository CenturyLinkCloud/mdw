/*
 * Copyright (C) 2018 CenturyLink, Inc.
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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.centurylink.mdw.config.OrderedProperties;
import com.centurylink.mdw.config.YamlBuilder;
import com.centurylink.mdw.config.YamlProperties;

@Parameters(commandNames="convert", commandDescription="Convert mdw or application property files to yaml", separators="=")
public class Convert extends Setup {

    @Parameter(names="--input", description="Input property file")
    private File input;
    public File getInput() {
        return input;
    }

    @Parameter(names="--map", description="Optional compatibility mapping file")
    private File map;
    public File getMap() {
        return map;
    }

    @Override
    public Convert run(ProgressMonitor... progressMonitors) throws IOException {

        InputStream mapIn;
        if (map == null) {
            mapIn = getClass().getClassLoader().getResourceAsStream("META-INF/mdw/configurations.map");
        }
        else {
            System.out.println("Mapping from " + map.getAbsolutePath());
            mapIn = new FileInputStream(map);
        }
        Properties mapProps = new Properties();
        mapProps.load(mapIn);

        if (input == null) {
            input = new File(getConfigRoot() + "/mdw.properties");
        }
        System.out.println("Loading properties from " + input.getAbsolutePath());
        Properties inputProps = new OrderedProperties();
        inputProps.load(new FileInputStream(input));

        String prefix = input.getName().substring(0, input.getName().lastIndexOf('.'));
        try {
            YamlBuilder yamlBuilder = YamlProperties.translate(prefix, inputProps, mapProps);
            File out = new File(getConfigRoot() + "/" + prefix + ".yaml");
            System.out.println("Writing output config: " + out.getAbsolutePath());
            Files.write(Paths.get(out.getPath()), yamlBuilder.toString().getBytes());
        }
        catch (ReflectiveOperationException ex) {
            throw new IOException(ex.getMessage(), ex);
        }

        return this;
    }

}
