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
package com.centurylink.mdw.plugin.search;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.ISearchResult;
import org.eclipse.search.ui.ISearchResultListener;

import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;

public class SearchResults implements ISearchResult {
    private List<WorkflowElement> matchingElements;

    public List<WorkflowElement> getMatchingElements() {
        return matchingElements;
    }

    public void addMatchingElement(WorkflowElement element) {
        matchingElements.add(element);
    }

    private SearchQuery searchQuery;

    public SearchResults(SearchQuery searchQuery) {
        this.searchQuery = searchQuery;
        this.matchingElements = new ArrayList<WorkflowElement>();
    }

    public ImageDescriptor getImageDescriptor() {
        return MdwPlugin.getImageDescriptor("icons/" + searchQuery.getIcon());
    }

    public String getLabel() {
        String label = "'" + searchQuery.getPattern() + "' - " + matchingElements.size()
                + " matching element(s) ";
        if (searchQuery.getScopedProjects() != null && !searchQuery.getScopedProjects().isEmpty())
            label += "in project(s): " + searchQuery.getScopedProjectsString();
        else if (searchQuery.getSelectedPackage() != null)
            label += "in package: " + searchQuery.getSelectedPackage().getLabel();
        return label;
    }

    public ISearchQuery getQuery() {
        return searchQuery;
    }

    public String getTooltip() {
        return "Search Results";
    }

    public void addListener(ISearchResultListener l) {
    }

    public void removeListener(ISearchResultListener l) {
    }

}
