/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.jsf.view;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import javax.faces.context.FacesContext;
import javax.faces.view.facelets.FaceletException;

import com.centurylink.mdw.common.Compatibility;
import com.centurylink.mdw.common.Compatibility.SubstitutionResult;
import com.centurylink.mdw.common.cache.impl.PackageVOCache;
import com.centurylink.mdw.common.cache.impl.RuleSetCache;
import com.centurylink.mdw.common.cache.impl.TemplateCache;
import com.centurylink.mdw.common.constant.FormConstants;
import com.centurylink.mdw.common.constant.TaskAttributeConstant;
import com.centurylink.mdw.common.email.Template;
import com.centurylink.mdw.common.utilities.ExpressionUtil;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.model.value.attribute.AssetVersionSpec;
import com.centurylink.mdw.model.value.attribute.RuleSetVO;
import com.centurylink.mdw.model.value.process.PackageVO;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.ui.MDWBase;
import com.centurylink.mdw.web.ui.UIException;

public class MDWPageContent
{
  public static final String FORM = "form.xhtml";
  public static final String FORM_VIEW = "/form.jsf";
  public static final String FORM_NAME = "formName";
  public static final String TEMPLATE = "template.xhtml";
  public static final String TEMPLATE_NAME = "templateName";
  public static final String PAGE = "page.xhtml";
  public static final String PAGE_VIEW = "/page.jsf";
  public static final String PAGE_NAME = "pageName";
  public static final String NOTICE = "notice.xhtml";
  public static final String NOTICE_VIEW = "/notice.jsf";
  public static final String NOTICE_TEMPLATE_NAME = "noticeTemplateName";
  public static final String NOTICE_TEMPLATE_VERSION = "noticeTemplateVersion";
  public static final String START_VIEW = "/start.jsf";
  public static final String START = "start.xhtml";

  private static StandardLogger logger = LoggerUtil.getStandardLogger();

  private String path;
  private Map<String,String> params;

  public static boolean isInternalView(String viewId)
  {
    return viewId.equals(FORM_VIEW) || viewId.endsWith(PAGE_VIEW) || viewId.equals(START_VIEW) || viewId.equals(NOTICE_VIEW) || isInternalUri(viewId);
  }

  public static boolean isInternalUri(String uri)
  {
    return uri.endsWith(FORM) || uri.endsWith(PAGE) || uri.endsWith(TEMPLATE) || uri.endsWith(NOTICE) || uri.endsWith(START);
  }

  public MDWPageContent(String path)
  {
    params = new HashMap<String,String>();

    int q = path.indexOf('?');
    if (q > 0)
    {
      // facelet templates referenced from custom webapp pages can have parameters on the path
      this.path = path.substring(0, q);
      String query = path.substring(q + 1);
      for (String pair : query.split("&"))
      {
        int eq = pair.indexOf("=");
        params.put(pair.substring(0, eq), pair.substring(eq + 1));
      }
    }
    else
    {
      this.path = path;
    }

    params.putAll(FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap());
  }

  public String getKey()
  {
    if (path.endsWith(PAGE))
      return path + ":" + getPageName() + (getPageVersion() == null ? "" : ":" + getPageVersion());
    else if (path.endsWith(FORM)) {
      return path + ":" + getFormName();
    } else if (path.endsWith(TEMPLATE))
      return path + ":" + params.get(TEMPLATE_NAME);
    else if (path.endsWith(NOTICE))
      return path + ":" + params.get(NOTICE_TEMPLATE_NAME)+ (params.get(NOTICE_TEMPLATE_VERSION) == null ? "" : ":" + params.get(NOTICE_TEMPLATE_VERSION));
    else
      return path;
  }

  public String getContent() throws UIException
  {
    if (path.endsWith(PAGE))
    {
      String pageName = getPageName();
      String assetVersion = null;
      RuleSetVO facelet = null;

      if (pageName == null)
        throw new UIException("Missing parameter: " + PAGE_NAME);

      assetVersion = getPageVersion();

      if (assetVersion != null)
        facelet = RuleSetCache.getRuleSet(new AssetVersionSpec(pageName, assetVersion));

      if (facelet == null)
        facelet = RuleSetCache.getRuleSet(pageName, RuleSetVO.FACELET);
      if (facelet != null)
      {
        String content = facelet.getRuleSet();
        try
        {
          if (Compatibility.hasPageSubstitutions())
            content = doCompatibilityPageSubstitutions(facelet.getLabel(), content);
          return content;
        }
        catch (Exception ex)
        {
          logger.severeException(ex.getMessage(), ex);
          throw new UIException(ex.getMessage(), ex);
        }
      }
      else
      {
        Object mdwObj = FacesVariableUtil.getValue("mdw");
        if (mdwObj instanceof MDWBase)
        {
          MDWBase mdw = (MDWBase) mdwObj;
          String refresh_string = params.get(FormConstants.URLARG_REFRESH);
          boolean refresh = refresh_string!=null && refresh_string.equalsIgnoreCase("true") || mdw.isToRefresh();
          if (refresh)
            mdw.clearCache();
          try
          {
            return mdw.getFacelet(pageName);
          }
          catch (Exception ex)
          {
            logger.severeException(ex.getMessage(), ex);
            throw new UIException(ex.getMessage(), ex);
          }
        }
        else
        {
          throw new UIException("Not supported in MDWHub");
        }
      }
    }
    else if (path.endsWith(START))
    {
      String customStartPage = (String) FacesVariableUtil.getValue("customStartPage");
      String customStartPageVersion = (String) FacesVariableUtil.getValue("customStartPageVersion");
      RuleSetVO facelet = null;
      if (customStartPage != null)
      {
        if (customStartPageVersion != null)
        {
          facelet = RuleSetCache.getRuleSet(new AssetVersionSpec(customStartPage, customStartPageVersion));
        }
        if (facelet == null)
          facelet = RuleSetCache.getRuleSet(customStartPage, RuleSetVO.FACELET);
        if (facelet != null)
        {
          String content = facelet.getRuleSet();
          try
          {
            if (Compatibility.hasPageSubstitutions())
              content = doCompatibilityPageSubstitutions(facelet.getLabel(), content);
            return content;
          }
          catch (Exception ex)
          {
            logger.severeException(ex.getMessage(), ex);
            throw new UIException(ex.getMessage(), ex);
          }
        }
      }
      else
      {
        //TODO : read content of default process/start.jsf content to avoid parsing exceptions
        return "go_processStart";
      }
    }
    else if (path.endsWith(FORM))
    {
      // the following doesn't work in MDWHub
      MDWBase mdw = (MDWBase) FacesVariableUtil.getValue("mdw");
      String formName = mdw.getFormName();
      if (formName == null)
        throw new UIException("Missing parameter: " + FORM_NAME);
      try
      {
        return mdw.getFacelet(formName);
      }
      catch (Exception ex)
      {
        throw new UIException(ex.getMessage(), ex);
      }
    }
    else if (path.endsWith(TEMPLATE))
    {
      String templateName = params.get(TEMPLATE_NAME);
      RuleSetVO template = RuleSetCache.getRuleSet(templateName, RuleSetVO.FACELET);
      if (template != null)
      {
        String content = template.getRuleSet();
        try
        {
          if (Compatibility.hasPageSubstitutions())
            content = doCompatibilityPageSubstitutions(template.getLabel(), content);
          return content;
        }
        catch (Exception ex)
        {
          logger.severeException(ex.getMessage(), ex);
          throw new UIException(ex.getMessage(), ex);
        }
      }
    }
    else if (path.endsWith(NOTICE))
    {
      String noticeTemplate = params.get(NOTICE_TEMPLATE_NAME);
      String noticeTemplateVersion = params.get(NOTICE_TEMPLATE_VERSION);
      if (noticeTemplate != null)
      {
        try
        {
          Template template = null;
          if (noticeTemplateVersion != null)
            template = TemplateCache.getTemplate(new AssetVersionSpec(noticeTemplate, noticeTemplateVersion));
          if (template == null)
            template = TemplateCache.getTemplate(noticeTemplate);
          return ExpressionUtil.substitute(template.getContent(), FacesContext.getCurrentInstance().getExternalContext().getSessionMap(), new HashMap<String,String>(), false);
        }
        catch (Exception ex)
        {
          throw new FaceletException(ex.getMessage(), ex);
        }
      }
    }
    else
    {
      String rulesetPath = path;
      if (rulesetPath.startsWith("/resources/"))
        rulesetPath = path.substring(11);
      if (rulesetPath.startsWith("/"))
        rulesetPath = path.substring(1);
      RuleSetVO general = RuleSetCache.getRuleSet(rulesetPath);
      if (general != null)
        return general.getRuleSet();
    }

    return null;
  }

  private String getPageName()
  {
    String pageName = params.get(PAGE_NAME);

    if (pageName == null && params.containsKey("AJAXREQUEST"))
    {
      // can still be null with richfaces ajax validators
      // try to infer the page name from the referrer request header
      String referer = FacesContext.getCurrentInstance().getExternalContext().getRequestHeaderMap().get("Referer");
      if (referer != null)
      {
        int pageIdx = referer.indexOf(PAGE_NAME + "=");
        if (pageIdx != -1)
        {
          int ampIdx = referer.indexOf('&', pageIdx + 9);
          if (ampIdx != -1)
            pageName = referer.substring(pageIdx + 9, ampIdx);
          else
            pageName = referer.substring(pageIdx + 9);
        }
      }
    }
    if (pageName == null)
    {
      // in custom navigation scenario, pageName is stored in the session
      if (FacesVariableUtil.getValue(PAGE_NAME) != null)
        pageName = (String) FacesVariableUtil.getValue(PAGE_NAME);
    }

    try
    {
      if (pageName != null)
        pageName = URLDecoder.decode(pageName, "UTF-8");
    }
    catch (UnsupportedEncodingException ex)
    {
      throw new RuntimeException(ex.getMessage(), ex);
    }
    PackageVO packageVO = FacesVariableUtil.getCurrentPackage();
    if (pageName == null || packageVO == null || pageName.indexOf('/') > 0 /*already qualified*/)
      return pageName;
    else
      return packageVO.getPackageName() + "/" + pageName;  // TODO not sure whether this is hit anymore
  }

  /**
   * @return
   */
  private String getPageVersion()
  {
     return (String) FacesVariableUtil.getValue(TaskAttributeConstant.CUSTOM_PAGE_ASSET_VERSION);
  }

  private String getFormName()
  {
	MDWBase mdw = (MDWBase) FacesVariableUtil.getValue("mdw");
    return mdw.getFormName();
  }

  public static String getCustomPage(String path, String params)
  {
    return getCustomPage(null, path, params);
  }

  /**
   * Returns the custom version of a page if one exists for the runtime package.
   */
  public static String getCustomPage(PackageVO basePackage, String path, String params)
  {
    String customPage = null;
    PackageVO packageVO = FacesVariableUtil.getCurrentPackage();

    int lastSlash = path.lastIndexOf('/') + 1;
    if (packageVO == null && basePackage != null)
    {
      // adjust package for special MDWHub convention
      packageVO = basePackage;
      if (lastSlash > 0)
      {
        String packagePath = path.substring(0, lastSlash - 1);
        packagePath = packagePath.replace('/', '.');  // qualified package path
        packageVO = PackageVOCache.getPackage(packageVO.getName() + "." + packagePath);
      }
    }

    if (packageVO != null)
    {
      String resourceName = path.substring(lastSlash);
      if (params != null && params.length() > 0)
      {
        StringTokenizer st = new StringTokenizer(params, "?&=");
        String name = null;
        while (st.hasMoreTokens())
        {
          String next = st.nextToken();
          if (name != null && name.equals("pageName"))
            resourceName = next;
          name = (name == null) ? next : null;
        }
      }
      int dotIdx = resourceName.lastIndexOf('.');
      if (dotIdx > 0)
        resourceName = resourceName.substring(0, dotIdx);
      if (packageVO.getRuleSets() != null)
      {
        for (int i = 0; i < packageVO.getRuleSets().size() && customPage == null; i++)
        {
          RuleSetVO ruleSet = packageVO.getRuleSets().get(i);
          if (ruleSet.getName().equals(resourceName))
            customPage = packageVO.getPackageName() + "/" + resourceName;
          else if (ruleSet.getName().equals(resourceName + ".xhtml"))
            customPage = packageVO.getPackageName() + "/" + resourceName + ".xhtml";
        }
      }
    }
    return customPage;
  }

  protected static String doCompatibilityPageSubstitutions(String label, String in) throws IOException
  {
    SubstitutionResult substitutionResult = Compatibility.getInstance().performPageSubstitutions(in);
    if (!substitutionResult.isEmpty()) {
        logger.warn("Compatibility substitutions applied for Custom Page asset " + label + " (details logged at debug level).");
        if (logger.isDebugEnabled())
            logger.debug("Compatibility substitutions for " + label + ":\n" + substitutionResult.getDetails());
        if (logger.isMdwDebugEnabled())
            logger.mdwDebug("Substitution output for " + label + ":\n" + substitutionResult.getOutput());
        return substitutionResult.getOutput();
    }
    return in;
  }

}
