/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.designer.testing;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetAddress;

import com.centurylink.mdw.designer.utils.RestfulServer;
import com.centurylink.mdw.soccom.SoccomException;
import com.centurylink.mdw.soccom.SoccomServer;

public class StubServer extends SoccomServer
{
  private RestfulServer restfulServer;
  private Stubber stubber;

  private static StubServer singleton = null;

  private int port;
  private boolean running;
  private boolean oldNamespaces;

  public static boolean isRunning()
  {
    return singleton != null;
  }

  public static void start(RestfulServer restfulServer, int port, Stubber stubber, boolean oldNamespaces) throws IOException
  {
    if (singleton == null)
    {
      singleton = new StubServer(restfulServer, port, stubber, oldNamespaces);
      singleton.start(true);
    }
    else
    {
      throw new IOException("Stub server already running");
    }
  }

  public static void stop()
  {
    if (singleton != null)
    {
      singleton.shutdown();
      singleton = null;
    }
  }

  private StubServer(RestfulServer restfulServer, int port, Stubber stubber, boolean oldNamespaces) throws IOException
  {
    super(String.valueOf(port), (PrintStream) null);
    this.port = port;
    this.stubber = stubber;
    super.max_threads = 1;
    this.restfulServer = restfulServer;
    this.oldNamespaces = oldNamespaces;
  }

  public void start()
  {
    start(true);
  }

  @Override
  public void start(boolean useNewThread)
  {
    if (!running)
    {
      try
      {
        notifyServer(true);
        running = true;
      }
      catch (IOException ex)
      {
        throw new IllegalStateException(ex.getMessage(), ex);
      }
      catch (Exception e)
      {
        e.printStackTrace();
      }
      super.start(useNewThread);
    }
  }

  @Override
  public void shutdown()
  {
    if (running)
    {
      try
      {
        notifyServer(false);
        running = false;
      }
      catch (Exception e)
      {
        e.printStackTrace();
      }
      super.shutdown();
    }
  }

  private void notifyServer(boolean on) throws Exception
  {
    // TODO use a pref setting to enable IPs?
    InetAddress ownIP = InetAddress.getLocalHost();
    restfulServer.stubServer(ownIP.getHostAddress(), port, on, oldNamespaces);
  }

  public interface Stubber
  {
    String processMessage(String masterRequestId, String request);
  }

  protected void request_proc(String threadId, String msgid, byte[] msg, int msgsize, OutputStream out) throws IOException, SoccomException
  {
    String request = new String(msg, 0, msgsize);
    String[] parsed = request.split("~", 3);
    String response = stubber.processMessage(parsed[1], parsed[2]);
    if (response != null)
      putresp("main", out, msgid, response);
  }

}
