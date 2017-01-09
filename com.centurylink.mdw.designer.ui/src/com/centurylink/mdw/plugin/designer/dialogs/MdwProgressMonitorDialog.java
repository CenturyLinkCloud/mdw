/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.dialogs;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import com.centurylink.mdw.plugin.MdwPlugin;

public class MdwProgressMonitorDialog extends ProgressMonitorDialog
{
  public MdwProgressMonitorDialog(Shell parent)
  {
    super(parent);
  }

  protected Image getImage()
  {
    Shell shell = getShell();
    final Display display;
    if (shell == null || shell.isDisposed())
    {
      shell = getParentShell();
    }
    if (shell == null || shell.isDisposed())
    {
      display = Display.getCurrent();
      Assert.isNotNull(display, "Dialog should be created in UI thread");
    }
    else
    {
      display = shell.getDisplay();
    }

    final Image[] image = new Image[1];
    display.syncExec(new Runnable()
    {
      public void run()
      {
        image[0] = MdwPlugin.getImageDescriptor("icons/mdw_48.png").createImage();
      }
    });

    return image[0];
  }

  protected Control createMessageArea(Composite composite)
  {
    Image image = getImage();
    if (image != null)
    {
      imageLabel = new Label(composite, SWT.NULL);
      image.setBackground(imageLabel.getBackground());
      imageLabel.setImage(image);
      GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.BEGINNING).applyTo(imageLabel);
    }
    // create message
    if (message != null)
    {
      messageLabel = new Label(composite, getMessageLabelStyle());
      messageLabel.setText(message);
      FontData font = messageLabel.getFont().getFontData()[0];
      font.setStyle(font.getStyle() | SWT.BOLD);
      messageLabel.setFont(new Font(this.getShell().getDisplay(), font));
      GridDataFactory
          .fillDefaults()
          .align(SWT.FILL, SWT.BEGINNING)
          .grab(true, false)
          .hint(convertHorizontalDLUsToPixels(IDialogConstants.MINIMUM_MESSAGE_AREA_WIDTH),
              SWT.DEFAULT).applyTo(messageLabel);
    }
    return composite;
  }
}
