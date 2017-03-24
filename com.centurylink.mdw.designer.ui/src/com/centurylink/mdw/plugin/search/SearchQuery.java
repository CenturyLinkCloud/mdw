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

import java.util.List;

import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.ISearchResult;
import org.eclipse.swt.widgets.Shell;

import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.designer.model.WorkflowPackage;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;

public abstract class SearchQuery implements ISearchQuery {
    public enum SearchType {
        ENTITY_BY_NAME, ENTITY_BY_ID, CONTAINING_TEXT, CONTAINING_ENTITY, INVOKING_ENTITY, INSTANCE_BY_ID, INSTANCE_BY_MRI, INSTANCE_BY_ENTITY_ID
    }

    public abstract String getIcon();

    public abstract void handleOpen(WorkflowElement workflowElement);

    private List<WorkflowProject> scopedProjects;

    public List<WorkflowProject> getScopedProjects() {
        return scopedProjects;
    }

    public String getScopedProjectsString() {
        if (scopedProjects == null)
            return null;
        String ret = "";
        for (int i = 0; i < scopedProjects.size(); i++) {
            ret += scopedProjects.get(i).getName();
            if (i < scopedProjects.size() - 1)
                ret += ", ";
        }
        return ret;
    }

    private WorkflowPackage selectedPackage;

    public WorkflowPackage getSelectedPackage() {
        return selectedPackage;
    }

    public void setSelectedPackage(WorkflowPackage selPkg) {
        this.selectedPackage = selPkg;
    }

    private SearchType searchType;

    public SearchType getSearchType() {
        return searchType;
    }

    private String pattern;

    public String getPattern() {
        return pattern;
    }

    private boolean caseSensitive;

    public boolean isCaseSensitive() {
        return caseSensitive;
    }

    private SearchResults searchResults;

    public SearchResults getSearchResults() {
        return searchResults;
    }

    private Shell shell;

    public Shell getShell() {
        return shell;
    }

    public boolean isInstanceQuery() {
        return searchType == SearchType.INSTANCE_BY_ID
                || searchType == SearchType.INSTANCE_BY_ENTITY_ID
                || searchType == SearchType.INSTANCE_BY_MRI;
    }

    public SearchQuery(List<WorkflowProject> scopedProjects, SearchType searchType,
            String searchPattern, boolean caseSensitive, Shell shell) {
        this.scopedProjects = scopedProjects;
        this.searchType = searchType;
        this.pattern = caseSensitive ? searchPattern : searchPattern.toLowerCase();
        if (!pattern.equals("*"))
            pattern = pattern.replaceAll("\\*", ""); // ignore *s since we match
                                                     // pattern anywhere in name
        this.caseSensitive = caseSensitive;
        searchResults = new SearchResults(this);
        this.shell = shell;
    }

    public boolean canRerun() {
        return true;
    }

    public boolean canRunInBackground() {
        return true;
    }

    public String getLabel() {
        return "Process Search";
    }

    public ISearchResult getSearchResult() {
        return searchResults;
    }

    public void showError(final Exception ex, final String title, final WorkflowProject project) {
        PluginMessages.log(ex);
        shell.getDisplay().asyncExec(new Runnable() {
            public void run() {
                PluginMessages.uiError(shell, ex, title, project);
            }
        });
    }

    public void showError(final String msg, final String title, final WorkflowProject project) {
        PluginMessages.log("MDW Search Error: " + msg);
        shell.getDisplay().asyncExec(new Runnable() {
            public void run() {
                PluginMessages.uiError(shell, msg, title, project);
            }
        });
    }

}
