/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.codegen.meta;

import java.util.ArrayList;
import java.util.List;

public class AdapterActivity extends Activity
{
  public static final String ADAPTER_TYPE_GENERAL = "General Adapter";
  public static final String ADAPTER_TYPE_JMS = "JMS-Based Service";
  public static final String ADAPTER_TYPE_BUS = "TIBCO Bus Service";
  public static final String ADAPTER_TYPE_WEB_SERVICE = "SOAP Web Service";
  public static final String ADAPTER_TYPE_RESTFUL = "RESTful Web Service";

  private static List<String> adapterTypeNames = new ArrayList<String>();
  static
  {
    adapterTypeNames.add(ADAPTER_TYPE_GENERAL);
    adapterTypeNames.add(ADAPTER_TYPE_JMS);
    adapterTypeNames.add(ADAPTER_TYPE_BUS);
    adapterTypeNames.add(ADAPTER_TYPE_WEB_SERVICE);
    adapterTypeNames.add(ADAPTER_TYPE_RESTFUL);
  }

  public static List<String> getAdapterTypeNames()
  {
    return adapterTypeNames;
  }

  private String adapterType;
  public String getAdapterType() { return adapterType; }
  public void setAdapterType(String type) { this.adapterType = type; }

  private boolean mdwWebService;
  public boolean isMdwWebService() { return mdwWebService; }
  public void setMdwWebService(boolean mdwWebService) { this.mdwWebService = mdwWebService; }

  private String httpMethod;
  public String getHttpMethod() { return httpMethod; }
  public void setHttpMethod(String httpMethod) { this.httpMethod = httpMethod; }

  private String busServiceTomId;
  public String getBusServiceTomId() { return busServiceTomId; }
  public void setBusServiceTomId(String tomId) { this.busServiceTomId = tomId; }

  private boolean synchronousJms;
  public boolean isSynchronousJms() { return synchronousJms; }
  public void setSynchronousJms(boolean synchronousJms) { this.synchronousJms = synchronousJms; }

  public String toString()
  {
    return super.toString()
      + "adapterType: " + getAdapterType() + "\n"
      + "baseClass: " + getBaseClass() + "\n"
      + "isMdwWebService: " + isMdwWebService() + "\n"
      + "httpMethod: " + getHttpMethod() + "\n"
      + "isSynchronousJms: " + isSynchronousJms() + "\n";
  }
}
