/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.IConsoleView;


public class MessageConsole extends org.eclipse.ui.console.MessageConsole
{
  public static final String STATUS_RUNNING = "";
  public static final String STATUS_TERMINATED = " (Terminated)";
  
  private String coreName;
  public String getCoreName() { return coreName; }
  
  private Display display;
  
  public MessageConsole(String name, ImageDescriptor imageDescriptor, Display display)
  {
    super(name, imageDescriptor);
    this.coreName = name;
    this.display = display;
  }
  
  public void setStatus(final String status)
  {
    display.asyncExec(new Runnable()
    {
      public void run()
      {
        if (status.equals(STATUS_TERMINATED))
          setName(coreName + STATUS_TERMINATED);
        else
          setName(coreName);
      }
    });
  }
  
  public FileConsoleOutputStream newFileConsoleOutputStream(File outputFile) throws IOException
  {
    return new FileConsoleOutputStream(outputFile);
  }
  
  public class FileConsoleOutputStream extends OutputStream
  {
    OutputStream consoleStream;
    FileOutputStream fileStream;
    
    FileConsoleOutputStream(File outputFile) throws IOException
    {
      fileStream = new FileOutputStream(outputFile);
      consoleStream = newMessageStream();
    }

    public void write(byte[] b, int off, int len) throws IOException
    {
      fileStream.write(b, off, len);
      if (isShowPref())
        revealConsole();
      consoleStream.write(b, off, len);
    }

    public void write(byte[] b) throws IOException
    {
      fileStream.write(b);
      if (isShowPref())
        revealConsole();
      consoleStream.write(b);
    }

    public void write(int b) throws IOException
    {
      fileStream.write(b);
      if (isShowPref())
        revealConsole();
      consoleStream.write(b);
    }
    
    public void flush() throws IOException
    {
      fileStream.close();
      consoleStream.flush();
    }

    public void close() throws IOException
    {
      fileStream.close();
      consoleStream.close();
    }
  }

  @Override
  public MessageConsoleStream newMessageStream()
  {
    return new MessageConsoleStream(this);
  }
  
  public class MessageConsoleStream extends org.eclipse.ui.console.MessageConsoleStream
  {
    public MessageConsoleStream(org.eclipse.ui.console.MessageConsole console)
    {
      super(console);
    }

    @Override
    public void println()
    {
      if (isShowPref())
        revealConsole();
      super.println();
    }

    @Override
    public void println(String message)
    {
      if (isShowPref())
        revealConsole();
      super.println(message);
    }
  }
  
  private void revealConsole()
  {
    if (!display.isDisposed())
    {
      display.asyncExec(new Runnable()
      {
        public void run()
        {
          IWorkbenchWindow activeWindow = MdwPlugin.getActiveWorkbenchWindow();
          if (activeWindow != null)
          {
            IWorkbenchPage page = activeWindow.getActivePage();
            try
            {
              IConsoleView view = (IConsoleView) page.showView(IConsoleConstants.ID_CONSOLE_VIEW);
              view.display(findConsole(getName(), getImageDescriptor(), display));
            }
            catch (PartInitException ex)
            {
              PluginMessages.log(ex);
            }
          }
        }      
      });
    }
  }
  
  public static MessageConsole findConsole(String name, ImageDescriptor icon, Display display)
  {
    IConsoleManager conMan = ConsolePlugin.getDefault().getConsoleManager();
    IConsole[] existingConsoles = conMan.getConsoles();
    for (IConsole existingConsole : existingConsoles)
    {
      if (existingConsole.getName().startsWith(name))
        return (MessageConsole) existingConsole;
    }
    
    // no console found, so create a new one
    MessageConsole messageConsole = new MessageConsole(name, icon, display);
    conMan.addConsoles(new IConsole[] { messageConsole });
    return messageConsole;
  }
  
  public boolean isShowPref()
  {
    IPreferenceStore store = MdwPlugin.getDefault().getPreferenceStore();
    return store.getBoolean(MessageConsolePageParticipant.PREFS_KEY + "_" + coreName.replaceAll(" ", ""));
  }
  
  public void setDefaultShowPref(boolean defaultValue)
  {
    IPreferenceStore store = MdwPlugin.getDefault().getPreferenceStore();
    store.setDefault(MessageConsolePageParticipant.PREFS_KEY + "_" + coreName.replaceAll(" ", ""), defaultValue);
  }
  
  private ConsoleRunnableEntity runnableEntity;
  public void setRunnableEntity(ConsoleRunnableEntity runnableEntity) { this.runnableEntity = runnableEntity; }
  
  public boolean isShowTerminate()
  {
    return runnableEntity != null;
  }
  
  public void terminate()
  {
    if (runnableEntity != null)
    {
      runnableEntity.shutdown();
    }
  }
  
  public boolean isRunning()
  {
    if (runnableEntity == null)
      return false;
    else
      return runnableEntity.isRunning();
  }
  
  public interface ConsoleRunnableEntity
  {
    public void shutdown();
    public boolean isRunning();
  }
}
