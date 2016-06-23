/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.prefs;

import java.io.UnsupportedEncodingException;
import java.util.Map;

import javax.el.ELResolver;
import javax.faces.context.FacesContext;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;
import javax.servlet.http.Cookie;

public class PrefsPhaseListener implements PhaseListener
{
  public PhaseId getPhaseId()
  {
    return PhaseId.RESTORE_VIEW;
  }

  public void beforePhase(PhaseEvent event)
  {
    Preferences prefs = getPrefs(event.getFacesContext());
    Map<?,?> cookies = event.getFacesContext().getExternalContext().getRequestCookieMap();

    if (cookies != null)
    {
      // cookie values are retrieved in new format, then if null old format
      // showLineNumbers
      Cookie showLineNumbersCookie = (Cookie) cookies.get("mdw.showLineNumbers");
      if (showLineNumbersCookie == null)
        showLineNumbersCookie = (Cookie) cookies.get("mdw:showLineNumbers");
      if (showLineNumbersCookie != null)
        prefs.setShowLineNumbers(new Boolean(showLineNumbersCookie.getValue()).booleanValue());
      // showTimeStamps
      Cookie showTimeStampsCookie = (Cookie) cookies.get("mdw.showTimeStamps");
      if (showTimeStampsCookie == null)
        showTimeStampsCookie = (Cookie) cookies.get("mdw:showTimeStamps");
      if (showTimeStampsCookie != null)
        prefs.setShowTimeStamps(new Boolean(showTimeStampsCookie.getValue()).booleanValue());
      // fileViewFontSize
      Cookie fileViewFontSizeCookie = (Cookie) cookies.get("mdw.fileViewFontSize");
      if (fileViewFontSizeCookie == null)
        fileViewFontSizeCookie = (Cookie) cookies.get("mdw:fileViewFontSize");
      if (fileViewFontSizeCookie != null)
        prefs.setFileViewFontSize(fileViewFontSizeCookie.getValue());
      // bufferLines
      Cookie bufferLinesCookie = (Cookie) cookies.get("mdw.bufferLines");
      if (bufferLinesCookie == null)
        bufferLinesCookie = (Cookie) cookies.get("mdw:bufferLines");
      if (bufferLinesCookie != null)
        prefs.setBufferLines(Integer.parseInt(bufferLinesCookie.getValue()));
      // refetchThreshold
      Cookie refetchThresholdCookie = (Cookie) cookies.get("mdw.refetchThreshold");
      if (refetchThresholdCookie == null)
        refetchThresholdCookie = (Cookie) cookies.get("mdw:refetchThreshold");
      if (refetchThresholdCookie != null)
        prefs.setRefetchThreshold(Integer.parseInt(refetchThresholdCookie.getValue()));
      // tailInterval
      Cookie tailIntervalCookie = (Cookie) cookies.get("mdw.tailInterval");
      if (tailIntervalCookie == null)
        tailIntervalCookie = (Cookie) cookies.get("mdw:tailInterval");
      if (tailIntervalCookie != null)
        prefs.setTailInterval(Integer.parseInt(tailIntervalCookie.getValue()));
      // sliderIncrementLines
      Cookie sliderIncrementLinesCookie = (Cookie) cookies.get("mdw.sliderIncrementLines");
      if (sliderIncrementLinesCookie == null)
        sliderIncrementLinesCookie = (Cookie) cookies.get("mdw:sliderIncrementLines");
      if (sliderIncrementLinesCookie != null)
        prefs.setSliderIncrementLines(Integer.parseInt(sliderIncrementLinesCookie.getValue()));

      // myProjects
      Cookie myProjectsCookie = (Cookie) cookies.get("mdw.myProjects");
      if (myProjectsCookie == null)
        myProjectsCookie = (Cookie) cookies.get("mdw:myProjects");
      if (myProjectsCookie != null)
      {
        try
        {
          prefs.setMyProjects(java.net.URLDecoder.decode(myProjectsCookie.getValue(), "UTF-8").split(","));
        }
        catch (UnsupportedEncodingException ex)
        {
          throw new RuntimeException(ex);
        }
      }
      Cookie cloudUserCookie = (Cookie) cookies.get("mdw.cloudUser");
      if (cloudUserCookie != null)
        prefs.setCloudUser(cloudUserCookie.getValue());
    }
  }

  public void afterPhase(PhaseEvent event)
  {
  }

  private Preferences getPrefs(FacesContext facesContext)
  {
    ELResolver elResolver = facesContext.getApplication().getELResolver();
    return (Preferences) elResolver.getValue(facesContext.getELContext(), null, "prefs");
  }

}
