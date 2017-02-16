/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
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
