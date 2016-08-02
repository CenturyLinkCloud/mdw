/**
 * Copyright (c) 2015 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.launch;

import java.util.Date;

import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.IStreamListener;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStreamMonitor;

import com.centurylink.mdw.designer.testing.TestCase;
import com.centurylink.mdw.plugin.PluginMessages;

/**
 * This is for standalone (non-MDW) tests.
 * For MDW test cases, see GherkinTestCaseLaunch.
 * TODO: Currently this does not add any value.
 */
public class CucumberLaunchListener implements IDebugEventSetListener
{
  private ILaunchConfiguration launchConfig;
  private boolean running;
  private int exitCode;

  private Date start;
  public Date getStart() { return start; }

  private Date end;
  public Date getEnd() { return end; }

  private String status;
  public String getStatus() { return status; }

  public CucumberLaunchListener(ILaunchConfiguration launchConfig)
  {
    this.launchConfig = launchConfig;
  }

  public void handleDebugEvents(DebugEvent[] events)
  {
    for (DebugEvent event : events)
    {
      if (event.getSource() instanceof IProcess)
      {
        IProcess process = (IProcess) event.getSource();
        if (event.getKind() == DebugEvent.CREATE)
        {
          process.getStreamsProxy().getOutputStreamMonitor().addListener(new IStreamListener()
          {
            public void streamAppended(String text, IStreamMonitor monitor)
            {
              System.out.print(text);
              if (!running)
              {
                running = true;
                start = new Date();
              }
            }
          });
          process.getStreamsProxy().getErrorStreamMonitor().addListener(new IStreamListener()
          {
            public void streamAppended(String text, IStreamMonitor monitor)
            {
              System.out.print(text);
            }
          });
        }
        else if (event.getKind() == DebugEvent.TERMINATE)
        {
          if (process.getLaunch().getLaunchConfiguration().equals(launchConfig) && process.isTerminated() && true)
          {
            end = new Date();
            try
            {
              exitCode = process.getExitValue();
              if (exitCode == 0)
              {
                status = TestCase.STATUS_PASS;
              }
              else
              {
                status = TestCase.STATUS_FAIL;
              }
            }
            catch (DebugException ex)
            {
              PluginMessages.log(ex);
              status = TestCase.STATUS_ERROR;
            }
          }
        }
      }
    }
  }
}
