/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.server;

import org.eclipse.core.commands.operations.IUndoableOperation;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.ManagedForm;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.wst.server.ui.editor.ServerEditorSection;

public class ServiceMixMdwBundleSection extends ServerEditorSection
{
  protected ManagedForm managedForm;

  private Button refreshOutputDirectoryCheckbox;

  @Override
  public void createSection(Composite parent)
  {
    super.createSection(parent);

    FormToolkit toolkit = new FormToolkit(getShell().getDisplay());

    Section section = toolkit.createSection(parent, ExpandableComposite.TWISTIE | ExpandableComposite.TITLE_BAR | Section.DESCRIPTION | ExpandableComposite.FOCUS_TITLE | ExpandableComposite.EXPANDED);
    section.setText("MDW Bundle");
    section.setDescription("Settings for MDW bundle deployment");
    section.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_FILL));

    Composite composite = toolkit.createComposite(section);
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

    // refresh output dir
    if (server != null)
    {
      refreshOutputDirectoryCheckbox = toolkit.createButton(composite, "Refresh output directory before publish", SWT.CHECK);
      GridData data = new GridData(GridData.FILL_HORIZONTAL);
      data.horizontalSpan = 2;
      refreshOutputDirectoryCheckbox.setLayoutData(data);
      boolean sel = server.getAttribute(ServiceMixServerBehavior.REFRESH_OUTPUT_DIR_BEFORE_PUBLISH, "true").equalsIgnoreCase("true");
      refreshOutputDirectoryCheckbox.setSelection(sel);

      refreshOutputDirectoryCheckbox.addSelectionListener(new SelectionAdapter()
      {
        public void widgetSelected(SelectionEvent e)
        {
          boolean sel = refreshOutputDirectoryCheckbox.getSelection();
          IUndoableOperation cmd = new ServerAttributeSetterCommand(server, ServiceMixServerBehavior.REFRESH_OUTPUT_DIR_BEFORE_PUBLISH, sel, true);
          execute(cmd);
        }
      });
    }
  }
}

