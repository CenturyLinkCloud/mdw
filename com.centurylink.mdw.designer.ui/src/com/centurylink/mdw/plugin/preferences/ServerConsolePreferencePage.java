/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.preferences;

import java.io.File;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.FontDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;

import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.preferences.model.PreferenceConstants;
import com.centurylink.mdw.plugin.preferences.model.ServerConsoleSettings;
import com.centurylink.mdw.plugin.preferences.model.ServerConsoleSettings.ClientShell;

public class ServerConsolePreferencePage extends PreferencePage implements PreferenceConstants
{
  private Text bufferSizeText;
  private Button fontDialogButton;
  private Button colorDialogButton;
  private FontDialog fontDialog;
  private int bufferSize;
  private FontData fontData;
  private RGB fontRgb;
  private ColorDialog colorDialog;
  private RGB backgroundRgb;
  
  private Button karafClientRadio;
  private Button puttyClientRadio;
  private ClientShell clientShell;
  private Text puttyExeText;
  private String puttyExePath;
  private Button puttyExeBrowse;
  
  public ServerConsolePreferencePage()
  {
    super("Server Console");
  }

  @Override
  protected Control createContents(Composite parent)
  {
    Composite composite = createComposite(parent, 1);    

    Group serverConsoleGroup = new Group(composite, SWT.NONE);
    serverConsoleGroup.setText("Server Runner Console");
    GridLayout gl = new GridLayout();
    gl.numColumns = 2;
    serverConsoleGroup.setLayout(gl);
    GridData gd = new GridData();
    gd.horizontalAlignment = GridData.BEGINNING;
    gd.widthHint = 400;
    serverConsoleGroup.setLayoutData(gd);
    
    createSpacer(serverConsoleGroup, 2, 2);
    createLabel(serverConsoleGroup, "Buffer Size (Chars)", 1);
    bufferSizeText = createTextField(serverConsoleGroup, 150);
    createSpacer(serverConsoleGroup, 2, 5);

    fontDialog = new FontDialog(getShell());
    fontDialog.setText("Select the font to display for Server Console output.");
    fontDialogButton = createButton(serverConsoleGroup, "Output Font...");
    fontDialogButton.addSelectionListener(new SelectionAdapter()
    {
      public void widgetSelected(SelectionEvent e)
      {
        FontData fd = fontDialog.open();
        if (fd != null)
        {
          fontData = fd;
          fontRgb = fontDialog.getRGB();
        }
      }
    });
    
    colorDialog = new ColorDialog(getShell());
    colorDialog.setText("Select the background color for Server Console output.");
    colorDialogButton = createButton(serverConsoleGroup, "Background Color...");
    colorDialogButton.addSelectionListener(new SelectionAdapter()
    {
      public void widgetSelected(SelectionEvent e)
      {
        RGB bgRgb = colorDialog.open();
        if (bgRgb != null)
          backgroundRgb = bgRgb;
      }
    });
    createSpacer(serverConsoleGroup, 2, 2);

    // client shell configuration
    Group clientShellGroup = new Group(composite, SWT.NONE);
    clientShellGroup.setText("Admin Shell Config");
    gl = new GridLayout();
    gl.numColumns = 3;
    clientShellGroup.setLayout(gl);
    gd = new GridData();
    gd.horizontalAlignment = GridData.BEGINNING;
    gd.widthHint = 400;
    gd.verticalIndent = 5;
    clientShellGroup.setLayoutData(gd);

    karafClientRadio = new Button(clientShellGroup, SWT.RADIO | SWT.LEFT);
    karafClientRadio.setText("SSH Client");
    gd = new GridData(GridData.BEGINNING);
    gd.horizontalSpan = 3;
    karafClientRadio.setLayoutData(gd);
    karafClientRadio.addSelectionListener(new SelectionAdapter()
    {
      public void widgetSelected(SelectionEvent e)
      {
        boolean selected = karafClientRadio.getSelection(); 
        if (selected)
        {
          puttyExeText.setText("");
          puttyExePath = null;
          puttyExeText.setEnabled(false);
          puttyExeBrowse.setEnabled(false);
          clientShell = ClientShell.Karaf;
        }
      }
    });

    puttyClientRadio = new Button(clientShellGroup, SWT.RADIO | SWT.LEFT);
    puttyClientRadio.setText("Putty Executable");
    puttyClientRadio.addSelectionListener(new SelectionAdapter()
    {
      public void widgetSelected(SelectionEvent e)
      {
        boolean selected = puttyClientRadio.getSelection(); 
        if (selected)
        {
          puttyExeText.setEnabled(true);
          puttyExeBrowse.setEnabled(true);
          clientShell = ClientShell.Putty;
        }
      }
    });
    
    puttyExeText = new Text(clientShellGroup, SWT.SINGLE | SWT.BORDER);
    gd = new GridData(GridData.BEGINNING | GridData.FILL_HORIZONTAL);
    puttyExeText.setLayoutData(gd);
    
    puttyExeText.addModifyListener(new ModifyListener()
    {
      public void modifyText(ModifyEvent e)
      {
        puttyExePath = puttyExeText.getText().trim();
      }
    });
    
    puttyExeBrowse = new Button(clientShellGroup, SWT.PUSH);
    puttyExeBrowse.setText("Browse...");
    puttyExeBrowse.addSelectionListener(new SelectionAdapter()
    {
      public void widgetSelected(SelectionEvent e)
      {
        FileDialog dlg = new FileDialog(getShell());
        puttyExePath = dlg.open();
        puttyExeText.setText(puttyExePath == null ? "" : puttyExePath);
      }
    });    
    
    initializeValues();

    return new Composite(parent, SWT.NULL);
  }

  @Override
  protected void storeValues()
  {
    // save the settings so they'll reflect in open console
    ServerConsoleSettings settings = MdwPlugin.getSettings().getServerConsoleSettings();
    settings.setBufferSize(bufferSize);
    settings.setFontData(fontData);
    settings.setFontRgb(fontRgb);
    settings.setBackgroundRgb(backgroundRgb);
    settings.setClientShell(clientShell);
    settings.setClientShellExe(puttyExePath == null ? null : new File(puttyExePath));
    
    IPreferenceStore store = getPreferenceStore();
    try
    {
      store.setValue(PREFS_SERVER_CONSOLE_BUFFER_SIZE, new Integer(bufferSizeText.getText()));
    }
    catch (NumberFormatException ex)
    {
      PluginMessages.log(ex);
    }
    store.setValue(PREFS_SERVER_CONSOLE_FONT, fontData.toString());
    store.setValue(PREFS_SERVER_CONSOLE_FONT_RED, fontRgb.red);
    store.setValue(PREFS_SERVER_CONSOLE_FONT_GREEN, fontRgb.green);
    store.setValue(PREFS_SERVER_CONSOLE_FONT_BLUE, fontRgb.blue);    
    store.setValue(PREFS_SERVER_CONSOLE_BG_RED, backgroundRgb.red);
    store.setValue(PREFS_SERVER_CONSOLE_BG_GREEN, backgroundRgb.green);
    store.setValue(PREFS_SERVER_CONSOLE_BG_BLUE, backgroundRgb.blue);
    
    store.setValue(PREFS_SERVER_CLIENT_SHELL, clientShell.toString());
    store.setValue(PREFS_SERVER_CLIENT_SHELL_EXE_PATH, puttyExePath == null ? "" : puttyExePath);
  }

  @Override
  protected void initializeValues()
  {
    IPreferenceStore store = getPreferenceStore();
    bufferSize = store.getInt(PREFS_SERVER_CONSOLE_BUFFER_SIZE);
    bufferSizeText.setText(String.valueOf(bufferSize));
    String font = store.getString(PREFS_SERVER_CONSOLE_FONT);
    fontData = new FontData(font);
    fontDialog.setFontList(new FontData[] { fontData });
    int red = store.getInt(PREFS_SERVER_CONSOLE_FONT_RED);
    int green = store.getInt(PREFS_SERVER_CONSOLE_FONT_GREEN);
    int blue = store.getInt(PREFS_SERVER_CONSOLE_FONT_BLUE);
    fontRgb = new RGB(red, green, blue);
    fontDialog.setRGB(fontRgb);    
    red = store.getInt(PREFS_SERVER_CONSOLE_BG_RED);
    green = store.getInt(PREFS_SERVER_CONSOLE_BG_GREEN);
    blue = store.getInt(PREFS_SERVER_CONSOLE_BG_BLUE);
    backgroundRgb = new RGB(red, green, blue);
    colorDialog.setRGB(backgroundRgb);
    
    String csPref = store.getString(PREFS_SERVER_CLIENT_SHELL);
    if (ClientShell.Putty.toString().equals(csPref))
      clientShell = ClientShell.Putty;
    else
      clientShell = ClientShell.Karaf;
    if (clientShell == ClientShell.Putty)
    {
      puttyClientRadio.setSelection(true);
      puttyExeText.setText(store.getString(PREFS_SERVER_CLIENT_SHELL_EXE_PATH));
    }
    else
    {
      karafClientRadio.setSelection(true);
      puttyExeText.setEnabled(false);
    }
  }

  @Override
  protected void initializeFromDefaults()
  {
    IPreferenceStore store = getPreferenceStore();
    bufferSize = store.getDefaultInt(PREFS_SERVER_CONSOLE_BUFFER_SIZE);
    
    String font = store.getDefaultString(PREFS_SERVER_CONSOLE_FONT);
    fontData = new FontData(font);
    fontDialog.setFontList(new FontData[] { fontData });
    int red = store.getDefaultInt(PREFS_SERVER_CONSOLE_FONT_RED);
    int green = store.getDefaultInt(PREFS_SERVER_CONSOLE_FONT_GREEN);
    int blue = store.getDefaultInt(PREFS_SERVER_CONSOLE_FONT_BLUE);
    fontRgb = new RGB(red, green, blue);
    fontDialog.setRGB(fontRgb);    
    red = store.getDefaultInt(PREFS_SERVER_CONSOLE_BG_RED);
    green = store.getDefaultInt(PREFS_SERVER_CONSOLE_BG_GREEN);
    blue = store.getDefaultInt(PREFS_SERVER_CONSOLE_BG_BLUE);
    backgroundRgb = new RGB(red, green, blue);
    colorDialog.setRGB(backgroundRgb);
    
    String csPref = store.getString(PREFS_SERVER_CLIENT_SHELL);
    if (ClientShell.Putty.toString().equals(csPref))
      clientShell = ClientShell.Putty;
    else
      clientShell = ClientShell.Karaf;
    if (clientShell == ClientShell.Putty)
    {
      puttyClientRadio.setSelection(true);
      puttyExeText.setText(store.getString(PREFS_SERVER_CLIENT_SHELL_EXE_PATH));
    }
    else
    {
      karafClientRadio.setSelection(true);
      puttyExeText.setEnabled(false);
    }    
  }

  @Override
  protected void setDefaultValues()
  {
    ServerConsoleSettings.setDefaultValues();
  }

  @Override
  public boolean validate()
  {
    if (clientShell == ClientShell.Putty)
      return (puttyExePath != null && !puttyExePath.isEmpty());
    else
      return true;
  }
}
