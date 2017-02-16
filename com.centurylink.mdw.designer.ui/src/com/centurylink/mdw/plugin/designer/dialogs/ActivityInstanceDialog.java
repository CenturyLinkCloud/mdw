/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.dialogs;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.centurylink.mdw.plugin.designer.DesignerProxy;
import com.centurylink.mdw.plugin.designer.model.Activity;
import com.centurylink.mdw.model.data.work.WorkStatuses;
import com.centurylink.mdw.model.value.event.ExternalMessageVO;
import com.centurylink.mdw.model.value.user.UserRoleVO;
import com.centurylink.mdw.model.value.work.ActivityInstanceVO;

public class ActivityInstanceDialog extends TrayDialog {
    public enum Mode {
        VIEW, RETRY, SKIP
    }

    private Activity activity;

    public Activity getActivity() {
        return activity;
    }

    private ActivityInstanceVO activityInstanceVO;

    public ActivityInstanceVO getActivityInstanceVO() {
        return activityInstanceVO;
    }

    private Text completionCodeText;
    private Text requestMessageText;
    private Text responseMessageText;
    private Button retryButton;
    private Button skipButton;

    private String completionCode;

    public String getCompletionCode() {
        return completionCode;
    }

    private String statusMessage;
    private Mode mode;

    public ActivityInstanceDialog(Shell shell, Activity activity,
            ActivityInstanceVO activityInstanceVO, Mode mode) {
        super(shell);
        this.activity = activity;
        this.activityInstanceVO = activityInstanceVO;
        this.mode = mode;
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite composite = (Composite) super.createDialogArea(parent);
        GridLayout layout = new GridLayout();
        layout.numColumns = 3;
        layout.verticalSpacing = 1;
        composite.setLayout(layout);
        composite.getShell().setText("Activity Instance");

        // activity name
        Label nameLabel = new Label(composite, SWT.NONE);
        nameLabel.setFont(new Font(nameLabel.getDisplay(), new FontData("Tahoma", 8, SWT.BOLD)));
        GridData gd = new GridData(SWT.LEFT);
        gd.horizontalSpan = 3;
        nameLabel.setLayoutData(gd);
        nameLabel.setText(activity.getName());

        // high-level info
        new Label(composite, SWT.NONE).setText("Instance ID: " + activityInstanceVO.getId());

        Label spacer = new Label(composite, SWT.NONE);
        gd = new GridData(SWT.LEFT);
        gd.widthHint = 50;
        spacer.setLayoutData(gd);
        new Label(composite, SWT.NONE).setText("Start: " + activityInstanceVO.getStartDate());
        new Label(composite, SWT.NONE).setText("Status: "
                + WorkStatuses.getWorkStatuses().get(activityInstanceVO.getStatusCode()));
        spacer = new Label(composite, SWT.NONE);
        spacer.setLayoutData(gd);
        new Label(composite, SWT.NONE).setText("End: " + activityInstanceVO.getEndDate());

        if (!mode.equals(Mode.RETRY)) {
            if (!mode.equals(Mode.SKIP)) {
                if (activity.isAdapter() || activity.isStart() || activity.isEventWait()) {
                    ExternalMessageVO messageVO = getDesignerProxy().getExternalMessage(activity,
                            activityInstanceVO);

                    if (!activity.isEventWait()) {
                        spacer = new Label(composite, SWT.NONE);
                        gd = new GridData(SWT.LEFT);
                        gd.horizontalSpan = 3;
                        gd.heightHint = 5;
                        spacer.setLayoutData(gd);

                        // request
                        Label label = new Label(composite, SWT.NONE);
                        gd = new GridData(SWT.LEFT);
                        gd.horizontalSpan = 3;
                        label.setLayoutData(gd);
                        label.setText(activity.isAdapter() ? "Adapter Request:"
                                : "External Event Request:");
                        requestMessageText = new Text(composite, SWT.BORDER | SWT.READ_ONLY
                                | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
                        gd = new GridData(SWT.LEFT);
                        gd.horizontalSpan = 3;
                        gd.widthHint = 500;
                        gd.heightHint = 180;
                        requestMessageText.setLayoutData(gd);
                        if (messageVO != null && messageVO.getRequest() != null)
                            requestMessageText.setText(messageVO.getRequest());
                    }

                    spacer = new Label(composite, SWT.NONE);
                    gd = new GridData(SWT.LEFT);
                    gd.horizontalSpan = 3;
                    gd.heightHint = 5;
                    spacer.setLayoutData(gd);

                    // response
                    Label label = new Label(composite, SWT.NONE);
                    gd = new GridData(SWT.LEFT);
                    gd.horizontalSpan = 3;
                    label.setLayoutData(gd);
                    label.setText(activity.isAdapter() ? "Adapter Response:"
                            : activity.isEmpty() ? "Event Message:" : "Response Message:");
                    responseMessageText = new Text(composite,
                            SWT.BORDER | SWT.READ_ONLY | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
                    gd = new GridData(SWT.LEFT);
                    gd.horizontalSpan = 3;
                    gd.widthHint = 500;
                    gd.heightHint = 180;
                    responseMessageText.setLayoutData(gd);
                    if (messageVO != null && messageVO.getResponse() != null)
                        responseMessageText.setText(messageVO.getResponse());
                }
            }

            spacer = new Label(composite, SWT.NONE);
            gd = new GridData(SWT.LEFT);
            gd.horizontalSpan = 3;
            gd.heightHint = 5;
            spacer.setLayoutData(gd);

            // completion code / status message
            Label label = new Label(composite, SWT.NONE);
            gd = new GridData(SWT.LEFT);
            gd.horizontalSpan = 3;
            label.setLayoutData(gd);
            label.setText(mode.equals(Mode.VIEW) ? "Completion Code / Status Message:"
                    : "Completion Code");
            int style = SWT.BORDER;
            if (mode.equals(Mode.VIEW))
                style = style | SWT.READ_ONLY | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL;
            completionCodeText = new Text(composite, style);

            gd = new GridData(SWT.LEFT);
            gd.horizontalSpan = 3;
            gd.widthHint = 500;
            if (mode.equals(Mode.VIEW))
                gd.heightHint = 100;
            completionCodeText.setLayoutData(gd);
            if (mode.equals(Mode.VIEW) && activityInstanceVO.getStatusMessage() != null)
                completionCodeText.setText(activityInstanceVO.getStatusMessage());
            if (mode.equals(Mode.SKIP)) {
                completionCodeText.addModifyListener(new ModifyListener() {
                    public void modifyText(ModifyEvent e) {
                        String compCode = completionCodeText.getText().trim();
                        completionCode = compCode.length() == 0 ? null : compCode;
                    }
                });
            }
        }

        return composite;
    }

    protected void createButtonsForButtonBar(Composite parent) {
        if (mode.equals(Mode.VIEW)) {
            createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
            return;
        }

        if (mode.equals(Mode.RETRY)) {
            retryButton = createButton(parent, -1, "Retry", false);
            retryButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    BusyIndicator.showWhile(getShell().getDisplay(), new Runnable() {
                        public void run() {
                            statusMessage = activity.getProject().getDesignerProxy()
                                    .retryActivityInstance(activity, activityInstanceVO);
                        }
                    });

                    if (statusMessage == null || statusMessage.trim().length() == 0)
                        MessageDialog.openInformation(getShell(), "Retry Activity",
                                "Activity instance: " + activityInstanceVO.getId() + " retried");
                    else
                        MessageDialog.openError(getShell(), "Retry Activity", statusMessage);

                    close();
                }
            });
            if (!activity.getProcess().isUserAuthorized(UserRoleVO.PROCESS_EXECUTION))
                retryButton.setEnabled(false);
        }

        if (mode.equals(Mode.SKIP)) {
            skipButton = createButton(parent, -1, "Proceed", false);
            skipButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    BusyIndicator.showWhile(getShell().getDisplay(), new Runnable() {
                        public void run() {
                            statusMessage = activity.getProject().getDesignerProxy()
                                    .skipActivityInstance(activity, activityInstanceVO,
                                            completionCode);
                        }
                    });

                    if (statusMessage == null || statusMessage.trim().length() == 0)
                        MessageDialog.openInformation(getShell(), "Skip Activity",
                                "Activity instance: " + activityInstanceVO.getId()
                                        + " skipped with completion code: " + completionCode + ".");
                    else
                        MessageDialog.openError(getShell(), "Skip Activity", statusMessage);

                    close();
                }
            });
            if (!activity.getProcess().isUserAuthorized(UserRoleVO.PROCESS_EXECUTION))
                skipButton.setEnabled(false);
        }

        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, true);

    }

    private DesignerProxy getDesignerProxy() {
        return activity.getProject().getDesignerProxy();
    }
}
