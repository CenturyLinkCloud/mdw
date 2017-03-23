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
package com.centurylink.mdw.plugin.designer.properties;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.views.properties.tabbed.ITabDescriptor;
import org.eclipse.ui.views.properties.tabbed.ITabDescriptorProvider;
import org.eclipse.ui.views.properties.tabbed.ITabbedPropertySheetPageContributor;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.designer.model.AttributeHolder;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;

public class TabDescriptorProvider implements ITabDescriptorProvider {
    public static final String CONTRIBUTOR_ID = "mdw.tabbedprops.contributor";

    private ITabbedPropertySheetPageContributor contributor;
    private MyTabbedPropertyRegistry registry;

    @Override
    public ITabDescriptor[] getTabDescriptors(IWorkbenchPart part, ISelection selection) {
        if (contributor == null) {
            contributor = new ITabbedPropertySheetPageContributor() {
                public String getContributorId() {
                    return CONTRIBUTOR_ID;
                }
            };
        }
        if (registry == null) {
            registry = new MyTabbedPropertyRegistry(CONTRIBUTOR_ID);
        }

        ITabDescriptor[] result = registry.getAllTabDescriptors();

        if (selection instanceof WorkflowElement && selection instanceof AttributeHolder) {
            List<PageletTab> elementTabs = getPageletTabs((WorkflowElement) selection);
            if (elementTabs != null) {
                for (PageletTab elementTab : elementTabs) {
                    boolean add = true;
                    for (int i = 0; i < result.length; i++) {
                        if (result[i].getId().equals(elementTab.getId())) {
                            add = false;
                            break;
                        }
                    }
                    if (add)
                        result = addToArray(elementTab, result);
                }
            }
        }

        return result;
    }

    private ITabDescriptor[] addToArray(ITabDescriptor descriptor, ITabDescriptor[] array) {
        ITabDescriptor[] result = new ITabDescriptor[array.length + 1];
        for (int i = 0; i < array.length; i++)
            result[i] = array[i];
        result[array.length] = descriptor;
        return result;
    }

    protected List<PageletTab> getPageletTabs(WorkflowElement element) {
        List<PageletTab> tabs = null;
        WorkflowProject project = element.getProject();
        if (project.checkRequiredVersion(5, 5)) {
            String elementType = element.getTitle();
            List<PageletTab> allProjTabs = project.getPageletTabs();
            if (allProjTabs != null) {
                for (PageletTab pageletTab : allProjTabs) {
                    InputStream inStream = new ByteArrayInputStream(pageletTab.getXml().getBytes());
                    SAXParserFactory parserFactory = SAXParserFactory.newInstance();
                    try {
                        SAXParser parser = parserFactory.newSAXParser();
                        final List<String> types = new ArrayList<String>();
                        parser.parse(inStream, new DefaultHandler() {
                            public void startElement(String uri, String localName, String qName,
                                    Attributes attrs) throws SAXException {
                                if (qName.equalsIgnoreCase("PAGELET")) {
                                    String appliesTo = attrs.getValue("APPLIES_TO");
                                    if (appliesTo != null) {
                                        for (String type : appliesTo.split(","))
                                            types.add(type);
                                    }
                                }
                            }
                        });
                        if (types.contains(elementType)) {
                            if (tabs == null)
                                tabs = new ArrayList<PageletTab>();
                            tabs.add(pageletTab);
                        }
                    }
                    catch (Exception ex) {
                        PluginMessages.log(ex); // don't prevent processing
                                                // other pagelets
                    }
                }
            }
        }

        return tabs;
    }

    /**
     * The only purpose for this class is to use the default tabbed properties
     * registry getAllTabDescriptors() method for retrieving tabs based on
     * plugin.xml.
     */
    @SuppressWarnings({ "restriction", "rawtypes", "unchecked" })
    class MyTabbedPropertyRegistry
            extends org.eclipse.ui.internal.views.properties.tabbed.view.TabbedPropertyRegistry {
        protected MyTabbedPropertyRegistry(String id) {
            super(id);
        }

        public ITabDescriptor[] getAllTabDescriptors() {
            return super.getAllTabDescriptors();
        }

        @Override
        protected List readTabDescriptors() {
            List result = new ArrayList();
            IConfigurationElement[] extensions = getConfigurationElements("propertyTabs");
            for (int i = 0; i < extensions.length; i++) {
                IConfigurationElement extension = extensions[i];
                IConfigurationElement[] tabs = extension.getChildren("propertyTab");
                for (int j = 0; j < tabs.length; j++) {
                    IConfigurationElement tab = tabs[j];
                    PropertyTab descriptor = new PropertyTab(tab);
                    if (getIndex(propertyCategories.toArray(), descriptor.getCategory()) == -1)
                        PluginMessages.log("Tab Error - tab descriptor has unknown category:"
                                + descriptor.getCategory());
                    else
                        result.add(descriptor);
                }
            }
            return result;
        }

        private int getIndex(Object[] array, Object target) {
            for (int i = 0; i < array.length; i++) {
                if (array[i].equals(target)) {
                    return i;
                }
            }
            return -1; // should never happen
        }
    }
}
