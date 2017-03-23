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
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.centurylink.mdw.plugin.PluginUtil;
import com.centurylink.mdw.xml.DomHelper;

public class DotProjectUpdater {
    private Document doc;
    private IFile dotProjectFile;

    public DotProjectUpdater(IProject project) throws Exception {
        this.dotProjectFile = project.getFile(".project");
        String dotProject = new String(PluginUtil.readFile(dotProjectFile));
        doc = DomHelper.toDomDocument(dotProject);
    }

    public void addNature(String nature) throws Exception {
        Node projectDescription = doc.getFirstChild();
        NodeList projectChildren = projectDescription.getChildNodes();
        for (int i = 0; i < projectChildren.getLength(); i++) {
            Node projectChild = projectChildren.item(i);
            if ("natures".equals(projectChild.getNodeName())) {
                boolean found = false;
                NodeList natureChildren = projectChild.getChildNodes();
                for (int j = 0; j < natureChildren.getLength(); j++) {
                    Node natureChild = natureChildren.item(j);
                    if ("nature".equals(natureChild.getNodeName())
                            && (natureChild.getFirstChild() != null
                                    && natureChild.getFirstChild().getNodeType() == Node.TEXT_NODE)
                            && nature.equals(natureChild.getFirstChild().getNodeValue()))
                        found = true;
                }
                if (!found) {
                    Element natureElement = doc.createElement("nature");
                    Node textNode = doc.createTextNode(nature);
                    natureElement.appendChild(textNode);
                    projectChild.appendChild(natureElement);
                }
            }
        }
    }

    public String toXml() throws TransformerException {
        return DomHelper.toXml(doc);
    }

    public void save(IProgressMonitor monitor) throws TransformerException {
        PluginUtil.writeFile(dotProjectFile, toXml(), monitor);
    }

}
