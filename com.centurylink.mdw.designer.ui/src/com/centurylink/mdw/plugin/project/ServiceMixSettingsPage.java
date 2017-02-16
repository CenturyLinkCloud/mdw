/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.project;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.wst.server.core.IRuntime;

import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.project.model.OsgiSettings;
import com.centurylink.mdw.plugin.project.model.ServerSettings;
import com.centurylink.mdw.plugin.project.model.ServerSettings.ContainerType;

public class ServiceMixSettingsPage extends ServerSettingsPage {
    public static final int DEFAULT_HTTP_PORT = 8181;
    public static final int DEFAULT_SSH_PORT = 8101;
    public static final String DEFAULT_USER = "karaf";

    public static final String PAGE_TITLE = "ServiceMix Settings";

    private Text serverSshPortTextField;
    private Text groupIdTextField;
    private Text artifactIdTextField;

    private Button useRootInstanceCheckbox;

    protected Button getUseRootInstanceCheckbox() {
        return useRootInstanceCheckbox;
    }

    private Button mavenRadioButton;
    private Button gradleRadioButton;

    public ServiceMixSettingsPage() {
        setTitle(getPageTitle());
        setDescription("Enter your " + getServerName() + " server information.\n"
                + "This will be written to your environment properties file.");
    }

    public String getPageTitle() {
        return PAGE_TITLE;
    }

    public String getServerName() {
        return "ServiceMix";
    }

    public int getDefaultServerPort() {
        return DEFAULT_HTTP_PORT;
    }

    public int getDefaultServerSshPort() {
        return DEFAULT_SSH_PORT;
    }

    public String getDefaultServerUser() {
        return DEFAULT_USER;
    }

    @Override
    public void initValues() {
        super.initValues();
        ContainerType type = ServerSettings
                .getContainerTypeFromClass(this.getClass().getSimpleName());
        String prevServerCmdPort = MdwPlugin
                .getStringPref(type + "-" + ProjectPersist.MDW_SERVER_CMD_PORT);
        if (prevServerCmdPort.length() > 0) {
            try {
                getServerSettings().setCommandPort(Integer.parseInt(prevServerCmdPort));
            }
            catch (NumberFormatException ex) {
                MdwPlugin.setStringPref(type + "-" + ProjectPersist.MDW_SERVER_CMD_PORT,
                        String.valueOf(getDefaultServerSshPort()));
            }
        }
        else {
            getServerSettings().setCommandPort(getDefaultServerSshPort());
        }
        serverSshPortTextField.setText(String.valueOf(getServerSettings().getCommandPort()));

        OsgiSettings osgiSettings = new OsgiSettings();
        getProject().setOsgiSettings(osgiSettings);
        if (getProject().checkRequiredVersion(5, 5)) {
            gradleRadioButton.setEnabled(true);
            osgiSettings.setGradleBuild(true);
        }
        else {
            gradleRadioButton.setEnabled(false);
        }
        gradleRadioButton.setSelection(osgiSettings.isGradleBuild());
        mavenRadioButton.setSelection(!osgiSettings.isGradleBuild());
        osgiSettings.setGroupId(getProject().getDefaultSourceCodePackage());
        groupIdTextField.setText(osgiSettings.getGroupId());
        osgiSettings.setArtifactId(getProject().getSourceProjectName().toLowerCase());
        artifactIdTextField.setText(osgiSettings.getArtifactId());
    }

    public OsgiSettings getOsgiSettings() {
        if (getProject() == null)
            return null;
        return getProject().getOsgiSettings();
    }

    protected void createServerPortControls(Composite parent, int ncol) {
        Composite composite = new Composite(parent, SWT.LEFT);

        // create the layout for this wizard page
        GridLayout gl = new GridLayout();
        gl.numColumns = 4;
        gl.marginLeft = -5;
        composite.setLayout(gl);
        GridData gd = new GridData();
        gd.verticalAlignment = GridData.FILL;
        gd.horizontalAlignment = GridData.FILL;
        gd.horizontalSpan = ncol;
        composite.setLayoutData(gd);

        Label portLbl = new Label(composite, SWT.NONE);
        portLbl.setText("HTTP:");
        gd = new GridData(GridData.BEGINNING);
        gd.widthHint = 44;
        portLbl.setLayoutData(gd);
        serverPortTextField = new Text(composite, SWT.SINGLE | SWT.BORDER);
        gd = new GridData(GridData.BEGINNING);
        gd.widthHint = 150;
        serverPortTextField.setLayoutData(gd);
        serverPortTextField.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                getServerSettings().setPort(Integer.parseInt(serverPortTextField.getText().trim()));
                handleFieldChanged();
            }
        });

        new Label(composite, SWT.NONE).setText("      SSH:");
        serverSshPortTextField = new Text(composite, SWT.SINGLE | SWT.BORDER);
        gd = new GridData(GridData.BEGINNING);
        gd.widthHint = 150;
        serverSshPortTextField.setLayoutData(gd);
        serverSshPortTextField.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                getServerSettings()
                        .setCommandPort(Integer.parseInt(serverSshPortTextField.getText().trim()));
                handleFieldChanged();
            }
        });
    }

    @Override
    protected String getServerLocationLabel() {
        return "Location";
    }

    @Override
    protected void createServerLocControls(Composite parent, int ncol) {
        super.createServerLocControls(parent, ncol);

        new Label(parent, SWT.NONE);

        useRootInstanceCheckbox = new Button(parent, SWT.CHECK);
        GridData gd = new GridData(GridData.BEGINNING);
        gd.horizontalSpan = ncol;
        gd.horizontalSpan = 3;
        gd.verticalAlignment = GridData.BEGINNING;
        useRootInstanceCheckbox.setLayoutData(gd);
        useRootInstanceCheckbox.setText("Use root container instance");
        useRootInstanceCheckbox.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                boolean useRoot = useRootInstanceCheckbox.getSelection();
                serverLocTextField.setEditable(!useRoot);
                if (useRoot)
                    getServerSettings().setServerLoc(getServerSettings().getHome());
                handleFieldChanged();
            }
        });
    }

    public boolean checkForAppropriateRuntime(IRuntime runtime) {
        String vendor = runtime.getRuntimeType().getVendor();
        String name = runtime.getRuntimeType().getName();
        return vendor.equals("Apache") && name.startsWith("ServiceMix");
    }

    @Override
    protected void createAdditionalControls(Composite parent, int ncol) {
        Group group = new Group(parent, SWT.NONE);
        group.setText("Build Options");
        GridLayout gl = new GridLayout();
        gl.numColumns = 2;
        group.setLayout(gl);
        GridData gd = new GridData(
                GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_FILL);
        gd.horizontalSpan = 4;
        gd.grabExcessHorizontalSpace = true;
        group.setLayoutData(gd);

        mavenRadioButton = new Button(group, SWT.RADIO | SWT.LEFT);
        mavenRadioButton.setText("Maven");
        gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_FILL);
        gd.horizontalSpan = 2;
        gd.grabExcessHorizontalSpace = true;
        mavenRadioButton.setLayoutData(gd);
        mavenRadioButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                getOsgiSettings().setGradleBuild(!mavenRadioButton.getSelection());
                handleFieldChanged();
            }
        });

        gradleRadioButton = new Button(group, SWT.RADIO | SWT.LEFT);
        gradleRadioButton.setText("Gradle (Requires MDW 5.5)");
        gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_FILL);
        gd.horizontalSpan = 2;
        gd.grabExcessHorizontalSpace = true;
        gradleRadioButton.setLayoutData(gd);
        gradleRadioButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                getOsgiSettings().setGradleBuild(gradleRadioButton.getSelection());
                handleFieldChanged();
            }
        });

        new Label(group, SWT.NONE).setText("Group ID:");
        groupIdTextField = new Text(group, SWT.SINGLE | SWT.BORDER);
        gd = new GridData(GridData.BEGINNING);
        gd.widthHint = 200;
        groupIdTextField.setLayoutData(gd);
        groupIdTextField.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                getOsgiSettings().setGroupId(groupIdTextField.getText());
                handleFieldChanged();
            }
        });

        new Label(group, SWT.NONE).setText("Artifact ID:");
        artifactIdTextField = new Text(group, SWT.SINGLE | SWT.BORDER);
        gd = new GridData(GridData.BEGINNING);
        gd.widthHint = 200;
        artifactIdTextField.setLayoutData(gd);
        artifactIdTextField.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                getOsgiSettings().setArtifactId(artifactIdTextField.getText());
                handleFieldChanged();
            }
        });
    }

    protected String serverHomeSpecializedCheck(String serverHome) {
        if (useRootInstanceCheckbox.getSelection()) {
            getServerSettings().setServerLoc(serverHome);
            if (!serverLocTextField.getText().equals(getServerSettings().getServerLoc()))
                serverLocTextField.setText(getServerSettings().getServerLoc());
        }

        String msg = null;
        if (serverHome != null && serverHome.length() != 0
                && !(checkFile(serverHome + "/bin/servicemix.bat")
                        || checkFile(serverHome + "/bin/servicemix.sh")))
            msg = "ServiceMix Home must contain bin/servicemix.bat or bin/servicemix.sh";

        return msg;
    }

    protected String serverLocSpecializedCheck(String serverLoc) {
        String msg = null;
        if (serverLoc != null && serverLoc.length() != 0
                && !(checkFile(serverLoc + "/bin/karaf.bat")
                        || checkFile(serverLoc + "/bin/karaf.sh")))
            msg = "Instance Location must contain bin/karaf.bat or bin/karaf.sh";

        return msg;
    }

    protected void parseServerAdditionalInfo(String serverLoc) {
        String sshPort = readSshPortProp(serverLoc);
        if (sshPort == null)
            sshPort = "8101";
        serverSshPortTextField.setText(sshPort);
    }

    private String readSshPortProp(String location) {
        String sshPort = null;
        File karafShellProps = new File(location + "/etc/org.apache.karaf.shell.cfg");
        if (karafShellProps.exists()) {
            Properties shellProps = new Properties();
            try {
                shellProps.load(new FileInputStream(karafShellProps));
                sshPort = shellProps.getProperty("sshPort");
            }
            catch (Exception ex) {
                PluginMessages.log(ex);
            }
        }
        return sshPort;
    }

    @Override
    public boolean isPageComplete() {
        if (!super.isPageComplete())
            return false;
        return checkInt(getServerSettings().getCommandPort())
                && checkStringNoWhitespace(getOsgiSettings().getGroupId())
                && checkStringNoWhitespace(getOsgiSettings().getArtifactId())
                && checkString(getOsgiSettings().getResourceDir())
                && checkString(getOsgiSettings().getOutputDir());
    }

    @Override
    public IStatus[] getStatuses() {
        IStatus[] is = super.getStatuses();
        if (is != null)
            return is;

        String msg = null;

        if (containsWhitespace(groupIdTextField.getText().trim()))
            msg = "Group ID must not container whitespace characters";
        else if (containsWhitespace(artifactIdTextField.getText().trim()))
            msg = "Artifact ID must not contain whitespace characters";

        if (msg == null)
            return null;

        return new IStatus[] { new Status(IStatus.ERROR, getPluginId(), 0, msg, null) };
    }

}
