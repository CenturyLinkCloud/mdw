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
package com.centurylink.mdw.plugin.project.model;

import javax.xml.transform.TransformerException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.centurylink.mdw.plugin.PluginUtil;
import com.centurylink.mdw.xml.DomHelper;

public class DotClasspathUpdater {
    private Document doc;
    private IFile dotClasspathFile;

    public DotClasspathUpdater(IProject project) throws Exception {
        this.dotClasspathFile = project.getFile(".classpath");
        String dotClasspath = new String(PluginUtil.readFile(dotClasspathFile));
        doc = DomHelper.toDomDocument(dotClasspath);
    }

    public void addContainerClasspathEntry(String path) throws Exception {
        Node classpath = doc.getFirstChild();
        boolean found = false;
        NodeList classpathChildren = classpath.getChildNodes();
        for (int i = 0; i < classpathChildren.getLength(); i++) {
            Node classpathChild = classpathChildren.item(i);
            if ("classpathentry".equals(classpathChild.getNodeName())) {
                NamedNodeMap attrs = classpathChild.getAttributes();
                Node pathNode = attrs.getNamedItem("path");
                if (pathNode != null && path.equals(pathNode.getNodeValue()))
                    found = true;
            }
        }
        if (!found) {
            Element classpathElement = doc.createElement("classpathentry");
            classpathElement.setAttribute("exported", "true");
            classpathElement.setAttribute("kind", "con");
            classpathElement.setAttribute("path", path);
            classpath.appendChild(classpathElement);
        }
    }

    public String toXml() throws TransformerException {
        return DomHelper.toXml(doc);
    }

    public void save(IProgressMonitor monitor) throws TransformerException {
        PluginUtil.writeFile(dotClasspathFile, toXml(), monitor);
    }
}
