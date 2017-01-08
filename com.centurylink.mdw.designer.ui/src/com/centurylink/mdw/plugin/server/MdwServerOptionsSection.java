/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.server;

import org.eclipse.core.commands.operations.IUndoableOperation;
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
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.wst.server.ui.editor.ServerEditorSection;

import com.centurylink.mdw.plugin.PluginMessages;

public abstract class MdwServerOptionsSection extends ServerEditorSection implements MdwServerConstants
{
  private Text javaOptionsTextField;
  private Button debugCheckbox;
  private Group debugGroup;
  private Label debugPortLabel;
  private Text debugPortTextField;
  private Button suspendCheckbox;
  protected Composite composite;

  @Override
  public void createSection(Composite parent)
  {
    super.createSection(parent);

    FormToolkit toolkit = new FormToolkit(getShell().getDisplay());

    Section section = toolkit.createSection(parent, ExpandableComposite.TWISTIE | ExpandableComposite.TITLE_BAR | Section.DESCRIPTION | ExpandableComposite.FOCUS_TITLE | ExpandableComposite.EXPANDED);
    section.setText("MDW Server Options");
    section.setDescription(getDescription());
    section.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_FILL));

    composite = toolkit.createComposite(section);
    GridLayout layout = new GridLayout();
    layout.numColumns = 2;
    layout.marginHeight = 5;
    layout.marginWidth = 10;
    layout.verticalSpacing = 5;
    layout.horizontalSpacing = 15;
    composite.setLayout(layout);
    composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_FILL));
    toolkit.paintBordersFor(composite);
    section.setClient(composite);

    Label javaOptionsLabel = new Label(composite, SWT.NONE);
    GridData gd = new GridData(GridData.BEGINNING);
    gd.horizontalSpan = 4;
    javaOptionsLabel.setLayoutData(gd);
    javaOptionsLabel.setText("Java Options:");

    javaOptionsTextField = new Text(composite, SWT.MULTI | SWT.WRAP | SWT.V_SCROLL | SWT.BORDER);
    gd = new GridData(GridData.FILL_HORIZONTAL);
    gd.horizontalSpan = 4;
    gd.widthHint = 300;
    gd.heightHint = 48;
    javaOptionsTextField.setLayoutData(gd);
    String javaOpts = server.getAttribute(JAVA_OPTIONS, getDefaultJavaOptions());
    javaOptionsTextField.setText(javaOpts);
    javaOptionsTextField.addModifyListener(new ModifyListener()
    {
      public void modifyText(ModifyEvent e)
      {
        String opts = javaOptionsTextField.getText().trim();
        IUndoableOperation cmd = new ServerAttributeSetterCommand(server, JAVA_OPTIONS, opts, getDefaultJavaOptions());
        execute(cmd);
      }
    });

    // debug options
    debugCheckbox = new Button(composite, SWT.CHECK);
    gd = new GridData(GridData.BEGINNING);
    gd.horizontalSpan = 3;
    gd.verticalIndent = 5;
    debugCheckbox.setLayoutData(gd);
    debugCheckbox.setText(getDebugModeLabel());
    boolean debug = server.getAttribute(DEBUG_MODE, getDefaultDebugMode());
    debugCheckbox.setSelection(debug);
    debugCheckbox.addSelectionListener(new SelectionAdapter()
    {
      public void widgetSelected(SelectionEvent e)
      {
        boolean debug = debugCheckbox.getSelection();
        enableDebugControls(debug);
        IUndoableOperation cmd = new ServerAttributeSetterCommand(server, DEBUG_MODE, debug, getDefaultDebugMode());
        execute(cmd);
      }
    });

    debugGroup = new Group(composite, SWT.NONE);
    GridLayout gl = new GridLayout();
    gl.numColumns = 3;
    debugGroup.setLayout(gl);
    debugGroup.setText("Debug Settings");
    gd = new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL);
    gd.horizontalSpan = 4;
    debugGroup.setLayoutData(gd);

    debugPortLabel = new Label(debugGroup, SWT.NONE);
    debugPortLabel.setText("Debug Port:");

    debugPortTextField = new Text(debugGroup, SWT.SINGLE | SWT.BORDER);
    gd = new GridData(GridData.BEGINNING);
    gd.verticalIndent = 5;
    gd.widthHint = 50;
    debugPortTextField.setLayoutData(gd);
    int port = server.getAttribute(DEBUG_PORT, DEFAULT_DEBUG_PORT);
    debugPortTextField.setText(Integer.toString(port));
    debugPortTextField.addModifyListener(new ModifyListener()
    {
      public void modifyText(ModifyEvent e)
      {
        String port = debugPortTextField.getText().trim();
        try
        {
          IUndoableOperation cmd = new ServerAttributeSetterCommand(server, DEBUG_PORT, Integer.parseInt(port), DEFAULT_DEBUG_PORT);
          execute(cmd);
        }
        catch (NumberFormatException ex)
        {
          PluginMessages.log(ex);
        }
      }
    });

    suspendCheckbox =  new Button(debugGroup, SWT.CHECK);
    gd = new GridData(GridData.BEGINNING);
    gd.horizontalIndent = 20;
    gd.verticalIndent = 5;
    suspendCheckbox.setLayoutData(gd);
    suspendCheckbox.setText("Suspend on Startup");
    boolean suspend = server.getAttribute(DEBUG_SUSPEND, DEFAULT_DEBUG_SUSPEND);
    suspendCheckbox.setSelection(suspend);
    suspendCheckbox.addSelectionListener(new SelectionAdapter()
    {
      public void widgetSelected(SelectionEvent e)
      {
        boolean suspend = suspendCheckbox.getSelection();
        IUndoableOperation cmd = new ServerAttributeSetterCommand(server, DEBUG_SUSPEND, suspend, DEFAULT_DEBUG_SUSPEND);
        execute(cmd);
      }
    });

    // initial enablement
    enableDebugControls(debug);
  }

  protected String getDebugModeLabel()
  {
    return "Run in Debug Mode";
  }

  protected boolean getDefaultDebugMode()
  {
    return true;
  }

  protected abstract String getDescription();
  protected abstract String getDefaultJavaOptions();

  private void enableDebugControls(boolean enabled)
  {
    debugGroup.setEnabled(enabled);
    debugPortLabel.setEnabled(enabled);
    debugPortTextField.setEnabled(enabled);
    suspendCheckbox.setEnabled(enabled);
  }

}

