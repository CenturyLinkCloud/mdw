/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.rcp;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.splash.AbstractSplashHandler;

import com.centurylink.mdw.plugin.MdwPlugin;

public class SplashHandler extends AbstractSplashHandler
{
  private final static int LABEL_HORIZONTAL_INDENT = 185;
  private final static int COLUMN_COUNT = 3;

  private Composite loadingComposite;
  private Label loadingMessage;

  private Image iconImage;

  private Shell splashShell;
  public Shell getSplashShell() { return splashShell; }
  public void setSplashShell(Shell splash) { this.splashShell = splash; }

  public void init(final Shell splash)
  {
    replaceShell(splash);
    super.init(getSplashShell());
    configureSplash();
    createUI();
    splash.dispose();
    getSplash().layout(true);
  }

  private void replaceShell(Shell splash)
  {
    Shell newSplash = new Shell(Display.getCurrent(), SWT.NO_TRIM);
    newSplash.setBackgroundImage(splash.getBackgroundImage());
    newSplash.setBounds(splash.getBounds());
    newSplash.setFont(splash.getFont());
    newSplash.setVisible(true);
    setSplashShell(newSplash);
  }

  private void configureSplash()
  {
    FillLayout layout = new FillLayout();
    getSplash().setLayout(layout);
    getSplash().setBackgroundMode(SWT.INHERIT_DEFAULT);
    getSplash().setText("MDW Designer");
    iconImage = MdwPlugin.getImageDescriptor("icons/designer.gif").createImage();
    getSplash().setImage(iconImage);
  }

  private void createUI()
  {
    loadingComposite = new Composite(getSplash(), SWT.BORDER);
    GridLayout layout = new GridLayout(COLUMN_COUNT, false);
    loadingComposite.setLayout(layout);
    Composite spanner = new Composite(loadingComposite, SWT.NONE);
    GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
    data.horizontalSpan = COLUMN_COUNT;
    spanner.setLayoutData(data);

    createMessageSection();
    splashShell.forceActive();
    splashShell.forceFocus();
  }

  private void createMessageSection()
  {
    loadingMessage = new Label(loadingComposite, SWT.NONE);
    loadingMessage.setText("Loading...");
    GridData data = new GridData();
    data.horizontalIndent = LABEL_HORIZONTAL_INDENT;
    data.widthHint = 250;
    data.horizontalSpan = 3;
    data.verticalAlignment = SWT.TOP;
    loadingMessage.setLayoutData(data);
  }
}
