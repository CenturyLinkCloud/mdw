/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.eclipse.swt.widgets.Display;

import com.centurylink.mdw.plugin.MessageConsole.MessageConsoleStream;

public class ProcessConsoleRunner
{
  private Display display;
  private String[] commands;
  private Process process;
  private BufferedReader stdOutReader;
  private BufferedReader stdErrReader;
  private MessageConsoleStream consoleStream;
  
  public ProcessConsoleRunner(Display display, String[] commands)
  {
    this.display = display;
    this.commands = commands;
  }
  
  public void start()
  {
    MessageConsole messageConsole = MessageConsole.findConsole("Server Config", MdwPlugin.getImageDescriptor("icons/ant.gif"), display);
    messageConsole.clearConsole();
    messageConsole.setDefaultShowPref(true);
    consoleStream = messageConsole.newMessageStream();
    
    String cmdline = "";
    for (String cmd : commands)
      cmdline += cmd + " ";
    consoleStream.println(cmdline);
    
    try
    {
      process = Runtime.getRuntime().exec(commands);    
      this.stdOutReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
      this.stdErrReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
      
      new Thread(new Runnable()
      {
        public void run()
        {
          try
          {
            String line = null;
            while ((line = stdOutReader.readLine()) != null)
              write(line);
          }
          catch (IOException ex)
          {
            PluginMessages.log(ex);
          }
        }
      }).start();
      
      new Thread(new Runnable()
      {
        public void run()
        {
          try
          {
            String line = null;
            while ((line = stdErrReader.readLine()) != null)
              write(line);
          }
          catch (IOException ex)
          {
            PluginMessages.log(ex);
          }
        }
      }).start();

      new Thread(new Runnable()
      {
        public void run()
        {
          try
          {
            process.waitFor();
            
            stdOutReader.close();
            stdErrReader.close();
          }
          catch (Exception ex)
          {
            PluginMessages.log(ex);
          }
        }
      });
    }
    catch (Exception ex)
    {
      PluginMessages.log(ex);
    }
    
  }
  
  private void write(final String line)
  {
    display.asyncExec(new Runnable()
    {
      public void run()
      {
        consoleStream.println(line);
      }
    });
  }
}
