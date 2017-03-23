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
package com.centurylink.mdw.yaml;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.representer.Representer;

import com.centurylink.mdw.util.file.FileHelper;

@SuppressWarnings("rawtypes")
public class YamlLoader {

    private Object top;
    public Object getTop() { return top; }

    public YamlLoader(File file) throws IOException {
        this(new String(FileHelper.read(file)));
    }

    public YamlLoader(String yamlStr) throws IOException {
        Yaml yaml = new Yaml();
        top = yaml.load(yamlStr);
    }

    public Map getMap(String name, Object source) {
        Object obj = name.isEmpty() ? source : ((Map)source).get(name);
        if (obj == null)
            return null;
        if (!(obj instanceof Map))
            throw new YAMLException("Object: " + name + " is not Map");
        return (Map) obj;
    }

    public Map getMap(String name, Object source, String path) {
        Object obj = name.isEmpty() ? source : ((Map)source).get(name);
        if (obj == null)
            return null;
        if (!(obj instanceof Map))
            throw new YAMLException("Object: " + path + "/" + name + " is not Map");
        return (Map) obj;
    }

    public Map getRequiredMap(String name, Object source, String path) {
        Object obj = name.isEmpty() ? source : ((Map)source).get(name);
        if (obj == null)
            missingYaml(name, path);
        if (!(obj instanceof Map))
            throw new YAMLException("Object: " + path + "/" + name + " is not Map");
        return (Map) obj;
    }

    public List getList(String name, Map source) {
        Object obj = name.isEmpty() ? source : ((Map)source).get(name);
        if (obj == null)
            return null;
        if (!(obj instanceof List))
            throw new YAMLException("Object: " + name + " is not List");
        return (List) obj;
    }

    public List getList(String name, Map source, String path) {
        Object obj = name.isEmpty() ? source : ((Map)source).get(name);
        if (obj == null)
            return null;
        if (!(obj instanceof List))
            throw new YAMLException("Object: " + path + "/" + name + " is not List");
        return (List) obj;
    }

    public List getRequiredList(String name, Map source, String path) {
        Object obj = name.isEmpty() ? source : ((Map)source).get(name);
        if (obj == null)
            missingYaml(name, path);
        if (!(obj instanceof List))
            throw new YAMLException("Object: " + path + "/" + name + " is not List");
        return (List) obj;
    }

    public String get(String name, Map source) {
        Object val = source.get(name);
        if (val == null)
            return null;
        return val.toString();
    }

    public String getRequired(String name, Map source, String path) {
        String val = get(name, source);
        if (val == null)
            missingYaml(name, path);
        return val;
    }

    public void missingYaml(String name, String path) {
        String msg = "Missing required element: ";
        if (!path.isEmpty())
            msg += path + "/";
        if (!name.isEmpty())
            msg += name;
        throw new YAMLException(msg);
    }

    public String toString() {
        return new Yaml(new Representer(), getDumperOptions()).dump(top);
    }

    protected DumperOptions getDumperOptions() {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.FLOW);
        options.setPrettyFlow(true);
        options.setIndent(2);
        return options;
    }

}
