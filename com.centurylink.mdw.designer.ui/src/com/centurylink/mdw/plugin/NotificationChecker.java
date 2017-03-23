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
package com.centurylink.mdw.plugin;

import java.net.URL;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuDetectEvent;
import org.eclipse.swt.events.MenuDetectListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolTip;
import org.eclipse.swt.widgets.Tray;
import org.eclipse.swt.widgets.TrayItem;
import org.eclipse.ui.IWorkbenchWindow;
import org.json.JSONArray;
import org.json.JSONObject;

import com.centurylink.mdw.common.utilities.HttpHelper;
import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.model.value.user.UserActionVO;
import com.centurylink.mdw.plugin.actions.WebLaunchActions;
import com.centurylink.mdw.plugin.actions.WebLaunchActions.WebApp;
import com.centurylink.mdw.plugin.actions.WebLaunchActions.WebLaunchAction;
import com.centurylink.mdw.plugin.designer.DesignerPerspective;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;

public class NotificationChecker implements Runnable {
    public static final String NOTIF_CHECK_PREFS_KEY = "MdwNoticeCheckPrefsKey";
    public static final String NOTIF_CHECK_INTERVAL_KEY = "MdwNoticeCheckIntervalKey";
    public static final String NOTIF_CHECK_LAST_RESULT_KEY = "MdwNoticeCheckLastResultKey";

    private static final int MAX_NOTICES = 10;

    private Display display;
    private WorkflowProject workflowProject;

    private boolean running;

    public boolean isRunning() {
        return running;
    }

    private Thread backgroundThread;
    private TrayItem trayItem;
    private Menu trayMenu;
    private ToolTip balloonTip;

    private List<String> noticeTypes;

    public List<String> getNoticeTypes() {
        return noticeTypes;
    }

    public void setNoticeTypes(List<String> noticeTypes) {
        this.noticeTypes = noticeTypes;
    }

    private int interval = 10; // minutes

    public int getInterval() {
        return interval;
    }

    public void setInterval(int i) {
        this.interval = i;
    }

    public NotificationChecker(Display display, WorkflowProject project) {
        this.display = display;
        this.workflowProject = project;

        // default lastRun is prev midnight
        Calendar now = Calendar.getInstance();
        now.set(Calendar.HOUR_OF_DAY, 0);
        now.set(Calendar.MINUTE, 0);
        now.set(Calendar.SECOND, 0);
        now.set(Calendar.MILLISECOND, 0);
        lastResult = now.getTime();
    }

    /**
     * Must be called from the UI thread.
     */
    public synchronized void startup() {
        Shell shell = display.getActiveShell();

        Tray tray = display.getSystemTray();
        if (tray == null) {
            MessageDialog.openError(shell, "MDW Notices",
                    "Notifications are not supported in this environment.");
            return;
        }

        trayItem = new TrayItem(tray, SWT.NONE);
        Image trayImage = MdwPlugin.getImageDescriptor("icons/designer.gif").createImage();
        trayItem.setImage(trayImage);
        trayItem.setToolTipText(workflowProject.getLabel() + " Notices");

        // double click
        trayItem.addSelectionListener(new SelectionAdapter() {
            public void widgetDefaultSelected(SelectionEvent e) {
                grabWorkbenchFocus();
            }
        });

        // force check
        trayMenu = new Menu(shell, SWT.POP_UP);
        MenuItem menuItem = new MenuItem(trayMenu, SWT.PUSH);
        menuItem.setText("Check Now");
        menuItem.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event e) {
                doCheck();
            }
        });

        // show prefs
        menuItem = new MenuItem(trayMenu, SWT.PUSH);
        menuItem.setText("Project Preferences");
        menuItem.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event e) {
                IWorkbenchWindow workbenchWindow = grabWorkbenchFocus();
                DesignerPerspective.showPerspectiveAndSelectProjectPreferences(workbenchWindow,
                        workflowProject);
            }
        });

        // exit
        menuItem = new MenuItem(trayMenu, SWT.PUSH);
        menuItem.setText("Exit Notice Check");
        menuItem.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event e) {
                shutdown();
            }
        });

        // tray item menu listener
        trayItem.addMenuDetectListener(new MenuDetectListener() {
            public void menuDetected(MenuDetectEvent e) {
                trayMenu.setVisible(true);
            }
        });

        String lastCheck = workflowProject
                .getPersistentProperty(NotificationChecker.NOTIF_CHECK_LAST_RESULT_KEY);
        if (lastCheck != null) {
            try {
                lastResult = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss").parse(lastCheck);
            }
            catch (ParseException ex) {
                PluginMessages.uiError(ex, "Notice Check", workflowProject);
            }
        }

        if (backgroundThread != null && backgroundThread.isAlive())
            backgroundThread.interrupt();

        backgroundThread = new Thread(this);
        running = true;
        backgroundThread.start();
    }

    private IWorkbenchWindow grabWorkbenchFocus() {
        IWorkbenchWindow workbenchWindow = MdwPlugin.getActiveWorkbenchWindow();
        if (workbenchWindow != null) {
            Shell shell = workbenchWindow.getShell();
            shell.setVisible(true);
            shell.setActive();
            shell.setFocus();
            shell.setMinimized(false);
        }
        return workbenchWindow;
    }

    public synchronized void shutdown() {
        running = false;
        if (trayItem != null) {
            trayItem.setVisible(false);
            if (!trayItem.isDisposed())
                trayItem.dispose();
            trayItem = null;
        }
    }

    public void run() {
        while (running) {
            try {
                Thread.sleep(interval * 60 * 1000);
                doCheck();
            }
            catch (InterruptedException ex) {
                PluginMessages.log("NotificationChecker thread interrupted.");
            }
        }
    }

    private void doCheck() {
        if (running) {
            try {
                String msg = checkNotices();
                if (msg != null) {
                    WebApp webapp = workflowProject.checkRequiredVersion(5, 5) ? WebApp.MdwHub
                            : WebApp.TaskManager;
                    WebLaunchAction launchAction = WebLaunchActions.getLaunchAction(workflowProject,
                            webapp);
                    String urlPath;
                    if (taskInstanceId == null)
                        urlPath = workflowProject.getMyTasksPath();
                    else
                        urlPath = workflowProject.getTaskInstancePath(taskInstanceId, isAssigned);

                    showBalloon(msg, launchAction, urlPath);
                }
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private Date lastResult;

    public Date getLastResult() {
        return lastResult;
    }

    public void setLastResult(Date lastResult) {
        this.lastResult = lastResult;
    }

    private Long taskInstanceId;

    private String checkNotices() {
        taskInstanceId = null;
        String msg = null;
        try {

            String urlStr = workflowProject.getServiceUrl() + "/Services/TaskActions?user="
                    + workflowProject.getUser().getUsername() + "&since="
                    + URLEncoder.encode(StringHelper.dateToString(lastResult), "utf-8") + "&max="
                    + MAX_NOTICES + "&format=json";
            URL url = new URL(urlStr);
            HttpHelper httpHelper = new HttpHelper(url);
            httpHelper.setConnectTimeout(MdwPlugin.getSettings().getHttpConnectTimeout());
            httpHelper.setReadTimeout(MdwPlugin.getSettings().getHttpReadTimeout());
            String response = new String(httpHelper.get());
            JSONArray actions = new JSONArray(response);
            // DesignerDataAccess designerDao = new
            // DesignerDataAccess(workflowProject.getDesignerProxy().getDesignerDataAccess());
            // List<TaskActionVO> actions =
            // designerDao.getUserTaskActions(workflowProject.getDesignerProxy().getPluginDataAccess().getWorkgroupNames(false).toArray(new
            // String[0]), lastResult);
            if (actions.length() > 0) {
                msg = "";
                UserActionVO firstAction = new UserActionVO(actions.getJSONObject(0));
                if (actions.length() == 1) {
                    taskInstanceId = firstAction.getEntityId();
                    msg += messageLine(firstAction);
                }
                else {
                    for (int i = 0; i < actions.length(); i++) {
                        JSONObject action = actions.getJSONObject(i);
                        msg += messageLine(new UserActionVO(action));
                    }
                }
                lastResult = firstAction.getRetrieveDate();
            }
            if (lastResult == null)
                lastResult = new Date(); // fall back to device time
            String lastResultStr = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss").format(lastResult);
            workflowProject.setPersistentProperty(NotificationChecker.NOTIF_CHECK_LAST_RESULT_KEY,
                    lastResultStr);
        }
        catch (Exception ex) {
            PluginMessages.uiError(ex, "Check Notices", workflowProject);
        }

        return msg;
    }

    private String messageLine(UserActionVO action) {
        StringBuffer line = new StringBuffer();
        line.append("Task ");
        if (action.getDescription() != null)
            line.append("\"").append(action.getDescription()).append("\" ");
        line.append(action.getAction().toString());
        if (action.getAction().toString().endsWith("e"))
            line.append("d");
        else
            line.append("ed");
        line.append(" (ID: " + action.getEntityId() + ")");
        return line.toString();
    }

    private boolean isAssigned;

    private SelectionListener tipListener = null;

    private void showBalloon(final String message, final WebLaunchAction launchAction,
            final String urlParams) {
        display.asyncExec(new Runnable() {
            public void run() {
                if (balloonTip != null && tipListener != null)
                    balloonTip.removeSelectionListener(tipListener);

                Shell shell = MdwPlugin.getActiveWorkbenchWindow().getShell();
                balloonTip = new ToolTip(shell, SWT.BALLOON | SWT.ICON_INFORMATION);
                balloonTip.setText(workflowProject.getLabel());
                trayItem.setToolTip(balloonTip);
                balloonTip.setMessage(message);
                balloonTip.setVisible(true);
                balloonTip.addSelectionListener(new SelectionAdapter() {
                    public void widgetSelected(SelectionEvent e) {
                        launchAction.launch(workflowProject, urlParams);
                    }
                });
            }
        });
    }
}
