/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.model;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;

import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.actions.WebLaunchActions;
import com.centurylink.mdw.plugin.actions.WebLaunchActions.WebApp;
import com.centurylink.mdw.plugin.actions.WebLaunchActions.WebLaunchAction;
import com.centurylink.mdw.plugin.designer.dialogs.MdwInputDialog;
import com.centurylink.mdw.common.constant.TaskAttributeConstant;
import com.centurylink.mdw.model.value.attribute.RuleSetVO;

public class Page extends WorkflowAsset {
    public Page() {
        super();
    }

    public Page(RuleSetVO ruleSetVO, WorkflowPackage packageVersion) {
        super(ruleSetVO, packageVersion);
    }

    public Page(Page cloneFrom) {
        super(cloneFrom);
    }

    @Override
    public String getTitle() {
        if (RuleSetVO.PAGELET.equals(getLanguage()))
            return "Pagelet";
        else
            return "Page";
    }

    @Override
    public String getIcon() {
        return "page.gif";
    }

    @Override
    public String getDefaultExtension() {
        return ".xhtml";
    }

    @Override
    public String getDefaultContent() {
        if (this.getExtension().equals(".xhtml")) {
            return "<?xml version=\"1.0\" encoding=\"ISO-8859-1\" ?>"
                    + "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">";
        }
        else {
            return super.getDefaultContent();
        }
    }

    private static List<String> pageLanguages;

    @Override
    public List<String> getLanguages() {
        if (pageLanguages == null) {
            pageLanguages = new ArrayList<String>();
            pageLanguages.add("Facelet");
            pageLanguages.add("JSP");
            pageLanguages.add("HTML");
            pageLanguages.add("Form");
            pageLanguages.add("XHTML");
            pageLanguages.add("Pagelet");
        }
        return pageLanguages;
    }

    @Override
    public void setLanguage(String language) {
        if ("XHTML".equalsIgnoreCase(language))
            super.setLanguage("FACELET");
        else
            super.setLanguage(language);
    }

    public void run() {
        boolean is55 = getProject().checkRequiredVersion(5, 5);
        String urlPath;
        if (isTaskInstancePage()) {
            MdwInputDialog dlg = new MdwInputDialog(MdwPlugin.getShell(), "Task Instance ID",
                    false);
            if (dlg.open() != Dialog.OK)
                return;

            try {
                Long taskInstanceId = new Long(dlg.getInput());
                urlPath = getProject().getTaskInstancePath(taskInstanceId);
            }
            catch (NumberFormatException ex) {
                MessageDialog.openError(MdwPlugin.getShell(), "Invalid Input",
                        "Invalid task instance ID: " + dlg.getInput());
                return;
            }
        }
        else {
            // TODO: why does new path format not work?
// if (is55)
// urlPath = TaskAttributeConstant.PAGE_PATH + getName();
// else
            urlPath = TaskAttributeConstant.PAGE_COMPATIBILITY_PATH + getName();
        }
        WebApp webapp = is55 ? WebApp.MdwHub : WebApp.TaskManager;
        WebLaunchAction launchAction = WebLaunchActions.getLaunchAction(getProject(), webapp);
        launchAction.launch(getPackage(), urlPath);
    }

    private boolean isTaskInstancePage() {
        // TODO better logic for this
        String n = getName();
        return n.startsWith("baseTask") || n.startsWith("customTaskContent")
                || n.startsWith("taskDetail");
    }
}