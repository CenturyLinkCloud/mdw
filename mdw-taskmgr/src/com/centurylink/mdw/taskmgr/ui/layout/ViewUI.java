/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.layout;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.digester.Digester;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.common.cache.impl.PackageVOCache;
import com.centurylink.mdw.common.cache.impl.RuleSetCache;
import com.centurylink.mdw.common.constant.PropertyNames;
import com.centurylink.mdw.common.utilities.FileHelper;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.common.utilities.property.PropertyManager;
import com.centurylink.mdw.model.value.attribute.RuleSetVO;
import com.centurylink.mdw.model.value.process.PackageVO;
import com.centurylink.mdw.model.value.user.UserActionVO;
import com.centurylink.mdw.model.value.user.UserActionVO.Action;
import com.centurylink.mdw.model.value.user.UserActionVO.Entity;
import com.centurylink.mdw.services.EventManager;
import com.centurylink.mdw.taskmgr.ui.NavScopeActionController;
import com.centurylink.mdw.taskmgr.ui.detail.DetailManager;
import com.centurylink.mdw.taskmgr.ui.filter.FilterManager;
import com.centurylink.mdw.taskmgr.ui.list.ListManager;
import com.centurylink.mdw.taskmgr.ui.tasks.list.TaskListScopeActionController;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.ui.UIException;
import com.centurylink.mdw.web.util.RemoteLocator;

/**
 * Interface class for loading and managing task ui definition.
 */
public class ViewUI
{
  private static StandardLogger logger = LoggerUtil.getStandardLogger();

  protected ViewUI() {}  // access via getInstance()

  private static ViewUI _defaultInstance;
  private static Map<String,ViewUI> _packageInstances = new HashMap<String,ViewUI>();

  private Map<String,UI> _taskViewMap;

  /**
   * Preference order for loading view definition XML:
   * MDWHub:
   *   - Use asset from user custom MDWHub package.
   *       - Load from the asset identified by package property mdw.hub.view.definition
   *       - If none, look for XML assets whose name ends with hub-view.xml or HubView.xml
   *   - Otherwise use global value in MDW properties file:
   *       - Plain filename resolves per rules below for TaskMgr
   *       - Path like MyPack/MyView.xml loads from designated workflow asset
   *   - Otherwise use asset com.centurylink.mdw.hub/mdw-hub-view.xml
   *   - Use the file resolution rules defined in FileHelper.openConfigurationFile()
   *   - Otherwise error
   * MDWTaskManagerWeb:
   *   - If PackageVO is in-scope due to custom webapp access:
   *       - Load from the asset identified by package property mdw.hub.view.definition
   *       - If none, look for XML assets whose name ends with hub-view.xml or HubView.xml
   *   - Otherwise load from filename designated in cfg via MDWFramework.TaskManagerWeb-view.ui.definition.file
   *       - Use the file resolution rules defined in FileHelper.openConfigurationFile()
   *
   * TODO: This needs to be refactored.  It's terribly inefficient, and no app in 5.5 is likely to use the
   * old TaskManager customization mechanism, which adds much confusion.
   */
  public static ViewUI getInstance() throws UIException
  {
    NavScopeActionController navScope = TaskListScopeActionController.getInstance();
    boolean isMdwHub = navScope.isMdwHubRequest() || navScope.isMdwReportsRequest();
    String hubViewDefFromGlobalCfg = PropertyManager.getProperty(PropertyNames.MDW_HUB_VIEW_DEF);

    // if package context, load the package-specific view def
    // see javadocs above for resolution rules
    PackageVO packageVO = null;
    if (isMdwHub)
    {
      packageVO = PackageVOCache.getPackage(PackageVO.MDW_HUB);
      if (packageVO == null)
      {
        // could be initialization scenario -- load from META
        if (hubViewDefFromGlobalCfg == null)
          hubViewDefFromGlobalCfg = "mdw-hub-view.xml";
        else
          throw new UIException("Missing package: '" + PackageVO.MDW_HUB + "'");
      }
      else
      {
        // but override from user-controlled pkg if present
        String userHubPkgName = ApplicationContext.getHubOverridePackage();
        PackageVO userHubPkg = PackageVOCache.getPackage(userHubPkgName);
        if (userHubPkg != null && userHubPkg.getRuleSet("mdw-hub-view.xml") != null)
          packageVO = userHubPkg;
      }
    }
    else
    {
      FacesVariableUtil.getCurrentPackage();
    }
    if (packageVO == null && isMdwHub)
    {
      if (hubViewDefFromGlobalCfg != null)
      {
        if (hubViewDefFromGlobalCfg.indexOf("/") > 0)
          packageVO = PackageVOCache.getPackage(hubViewDefFromGlobalCfg.substring(0, hubViewDefFromGlobalCfg.indexOf("/")));
        // otherwise leave packageVO blank to load from file system
      }
    }
    if (packageVO != null)
    {
      String packageViewDefDocName = isMdwHub ? packageVO.getProperty(PropertyNames.MDW_HUB_VIEW_DEF) : packageVO.getProperty(PropertyNames.TASK_MANAGER_UI_DEF_FILE);
      if (packageViewDefDocName == null && packageVO.getRuleSets() != null)
      {
        // look for CONFIG rulesets matching naming convention
        for (RuleSetVO ruleSetVO : packageVO.getRuleSets())
        {
          if (isMdwHub)
          {
            if (ruleSetVO.getLanguage().equals(RuleSetVO.CONFIG) || ruleSetVO.getLanguage().equals(RuleSetVO.XML)
                && (ruleSetVO.getName().endsWith("HubView.xml") || ruleSetVO.getName().endsWith("hub-view.xml")))
            {
              packageViewDefDocName = ruleSetVO.getName();
              break;
            }
          }
          else
          {
            if (ruleSetVO.getLanguage().equals(RuleSetVO.CONFIG) || ruleSetVO.getLanguage().equals(RuleSetVO.XML)
                && (ruleSetVO.getName().endsWith("TaskView") || ruleSetVO.getName().endsWith("TaskView.xml")))
            {
              packageViewDefDocName = ruleSetVO.getName();
              break;
            }
          }
        }
      }
      if (packageViewDefDocName != null)  // custom view def for package
      {
        RuleSetVO viewDefRuleSet = packageVO.getRuleSet(packageViewDefDocName.indexOf("/") > 0 ? packageViewDefDocName.substring(packageViewDefDocName.indexOf("/")+1) : packageViewDefDocName);
        if (viewDefRuleSet == null)
          throw new UIException("Can't find View Definition document '" + packageViewDefDocName + "' for Package '" + packageVO.getPackageName() + "'");

        ViewUI packageInstance = null;
        RuleSetVO loadedViewDef = RuleSetCache.getRuleSet(viewDefRuleSet.getId());

        if (loadedViewDef != null) {
          String confirmLoaded = (String) loadedViewDef.getCompiledObject();
          if ("loaded".equals(confirmLoaded))  // flag as loaded
            packageInstance = _packageInstances.get(packageVO.getPackageName());
          else
            loadedViewDef.setCompiledObject("loaded");
        }

        if (packageInstance == null)
        {
          // viewUI has not been parsed or rule_set has been reloaded by cache refresh
          packageInstance = new ViewUI();
          _packageInstances.put(packageVO.getPackageName(), packageInstance);
          String msg = "View Definition document '" + packageViewDefDocName + "' for Package '" + packageVO.getPackageName() + "'";
          logger.info("Loading " + msg);
          try
          {
            long before = System.currentTimeMillis();
            packageInstance.load(RuleSetCache.getRuleSet(viewDefRuleSet.getId()).getRuleSet());
            if (logger.isDebugEnabled())
              logger.debug(packageViewDefDocName + " loaded in " + (System.currentTimeMillis() - before) + " ms");
          }
          catch (Exception ex)
          {
            throw new UIException("Failed to load " + msg, ex);
          }
        }
        if (packageInstance.getSkin() != null)
          FacesVariableUtil.setSkin(packageInstance.getSkin());
        return packageInstance;
      }
    }

    if (isMdwHub && hubViewDefFromGlobalCfg == null)
      throw new UIException("Package '" + packageVO.getLabel() + "' must contain a hub-view.xml config asset");

    // return the default instance
    if (_defaultInstance == null)
    {
      _defaultInstance = new ViewUI();
      String viewDefDocName = null;
      if (isMdwHub)
        viewDefDocName = hubViewDefFromGlobalCfg;
      else
      {
        viewDefDocName = PropertyManager.getProperty(PropertyNames.TASK_MANAGER_UI_DEF_FILE);
        if (viewDefDocName == null)
          viewDefDocName = "MDWTaskView.xml";
      }
      try
      {
        logger.info("Loading View Definition from '" + viewDefDocName + "'");
        _defaultInstance.load(readFileFromClasspath(viewDefDocName));
        // set the default instance as a faces application variable
        FacesVariableUtil.setApplicationValue("viewUI", _defaultInstance);
      }
      catch (Exception ex)
      {
        throw new UIException("Failed to load View Definition: " + viewDefDocName, ex);
      }
    }

    if (_defaultInstance.getSkin() != null)
      FacesVariableUtil.setSkin(_defaultInstance.getSkin());
    return _defaultInstance;
  }

  /**
   * Retrieve a ListUI configuration based on its name.
   * @param id - the id of the list
   * @return layout for the specified list
   */
  public ListUI getListUI(String id) throws UIException
  {
    Object o = getInstance()._taskViewMap.get(id);
    if (o == null)
      return null;
    if (!(o instanceof ListUI))
      throw new IllegalStateException("Map value does not match expected type (ListUI id=" + id + "): " + o);
    return (ListUI) o;
  }

  public void addListUI(String id, ListUI listUI) throws UIException
  {
    getInstance()._taskViewMap.put(id, listUI);
  }

  /**
   * Retrieve a DetailUI configuration based on its name.
   * @param id - the id of the detail
   * @return specific for the specified detail
   */
  public DetailUI getDetailUI(String id) throws UIException
  {
    Object o = getInstance()._taskViewMap.get(id);
    if (o == null)
      return null;
    if (!(o instanceof DetailUI))
      throw new IllegalStateException("Map value does not match expected type (DetailUI id=" + id + "): " + o);
    return (DetailUI) o;
  }

  public void addDetailUI(String id, DetailUI detailUI) throws UIException
  {
    getInstance()._taskViewMap.put(id, detailUI);
  }

  /**
   * Retrieve a FilterUI configuration based on its name.
   * @param id - the id of the filter
   * @return layout for the specified filter
   */
  public FilterUI getFilterUI(String id) throws UIException
  {
    Object o = getInstance()._taskViewMap.get(id);
    if (!(o instanceof FilterUI))
      throw new IllegalStateException("Map value does not match expected type (FilterUI id=" + id + "): " + o);
    return (FilterUI) o;
  }

  public void addFilterUI(String id, FilterUI filterUI) throws UIException
  {
    getInstance()._taskViewMap.put(id, filterUI);
  }
  /**
   * Retrieve a ReportsUI configuration based on its name.
   * @param id - the id of the filter
   * @return layout for the specified filter
   */
  public ReportsUI getReportsUI(String id) throws UIException
  {
    Object o = getInstance()._taskViewMap.get(id);
    if (!(o instanceof ReportsUI))
      throw new IllegalStateException("Map value does not match expected type (ReportsUI id=" + id + "): " + o);
    return (ReportsUI) o;
  }

  public void addReportsUI(String id, ReportsUI reportsUI) throws UIException
  {
    getInstance()._taskViewMap.put(id, reportsUI);
  }

  public static void clear()
  {
    logger.info("Task Manager view definition cache cleared");
    _defaultInstance = null;
  }

  /**
   * Invoked only on the default instance (package instances are refreshed with RuleSet cache).
   * For MDWHub view def to be refreshed, it needs to be configured as a workflow asset.
   */
  public void reload() throws UIException
  {
    String viewDefDocName = PropertyManager.getProperty(PropertyNames.TASK_MANAGER_UI_DEF_FILE);
    try
    {
      logger.info("Loading View Definition from '" + viewDefDocName + "'");
      load(readFileFromClasspath(viewDefDocName));
      ListManager.getInstance().invalidate();
      FilterManager.getInstance().invalidate();
      DetailManager.getInstance().invalidate();
      FacesVariableUtil.addMessage("View Definition reloaded from " + viewDefDocName);

      String user = FacesVariableUtil.getCurrentUser().getCuid();
      UserActionVO userAction = new UserActionVO(user, Action.Refresh, Entity.File, new Long(0), viewDefDocName);
      userAction.setSource("Task Manager");
      EventManager eventMgr = RemoteLocator.getEventManager();
      eventMgr.createAuditLog(userAction);
    }
    catch (Exception ex)
    {
      String msg = "Failed to reload View Definition from '" + viewDefDocName + "'";
      logger.severeException(msg, ex);
      throw new UIException(msg, ex);
    }
  }

  private String _skin;
  public String getSkin() { return _skin; }
  public void setSkin(String skin) { this._skin = skin; }

  private boolean _showCommonGroup = true;
  public boolean isShowCommonGroup() { return _showCommonGroup; }
  public void setShowCommonGroup(boolean show) { this._showCommonGroup = show; }

  void load(String viewDefDocument) throws IOException, SAXException, ParserConfigurationException
  {
    Digester d = new Digester();

    _taskViewMap = new HashMap<String,UI>();
    d.push(_taskViewMap);

    // support for lists
    d.addObjectCreate("view/list", ListUI.class);
    d.addCallMethod("view/list", "addListUI", 22);
    d.addCallParam("view/list", 0, "id");
    d.addCallParam("view/list", 1, "name");
    d.addCallParam("view/list", 2, "model");
    d.addCallParam("view/list", 3, "filter");
    d.addCallParam("view/list", 4, "controller");
    d.addCallParam("view/list", 5, "columnMapper");
    d.addCallParam("view/list", 6, "defaultSortColumn");
    d.addCallParam("view/list", 7, "defaultSortDescending");
    d.addCallParam("view/list", 8, "displayRows");
    d.addCallParam("view/list", 9, "paginatedResponse");
    d.addCallParam("view/list", 10, "viewBy");
    d.addCallParam("view/list", 11, "exportable");
    d.addCallParam("view/list", 12, "allRowsLink");
    d.addCallParam("view/list", 13, "showTimings");
    d.addCallParam("view/list", 14, "ajaxEnabled");
    d.addCallParam("view/list", 15, "customButtons");
    d.addCallParam("view/list", 16, "searchable");
    d.addCallParam("view/list", 17, "groupingOptions");
    d.addCallParam("view/list", 18, "defaultGroupBy");
    d.addCallParam("view/list", 19, "pageSizeOptions");
    d.addCallParam("view/list", 20, "columnarFilters");
    d.addCallParam("view/list", 21, "showAllDisplayRows");

    d.addObjectCreate("view/list/column", ItemUI.class);
    d.addSetNext("view/list/column", "addColumn");
    d.addSetProperties("view/list/column");

    // support for filters
    d.addObjectCreate("view/filter/field", FieldUI.class);
    d.addSetNext("view/filter/field", "addField");
    d.addSetProperties("view/filter/field");

    d.addObjectCreate("view/filter", FilterUI.class);
    d.addCallMethod("view/filter", "addFilterUI", 4);
    d.addCallParam("view/filter", 0, "id");
    d.addCallParam("view/filter", 1, "name");
    d.addCallParam("view/filter", 2, "model");
    d.addCallParam("view/filter", 3, "width");

    // support for details
    d.addObjectCreate("view/detail", DetailUI.class);
    d.addCallMethod("view/detail", "addDetailUI", 11);
    d.addCallParam("view/detail", 0, "id");
    d.addCallParam("view/detail", 1, "name");
    d.addCallParam("view/detail", 2, "model");
    d.addCallParam("view/detail", 3, "controller");
    d.addCallParam("view/detail", 4, "labelWidth");
    d.addCallParam("view/detail", 5, "valueWidth");
    d.addCallParam("view/detail", 6, "changedItemStyleClass");
    d.addCallParam("view/detail", 7, "oldValueName");
    d.addCallParam("view/detail", 8, "newValueName");
    d.addCallParam("view/detail", 9, "rolesAllowedToEdit");
    d.addCallParam("view/detail", 10, "layoutColumns");

    d.addObjectCreate("view/detail/row", ItemUI.class);
    d.addSetNext("view/detail/row", "addRow");
    d.addSetProperties("view/detail/row");

    //support for dashboard reports
    d.addObjectCreate("view/reports", ReportsUI.class);
    d.addCallMethod("view/reports", "addReportsUI", 1);
    d.addCallParam("view/reports", 0, "id");

    d.addObjectCreate("view/reports/report", ReportUI.class);
    d.addSetNext("view/reports/report", "addReportUI");
    d.addSetProperties("view/reports/report");



    if (logger.isMdwDebugEnabled())
      logger.mdwDebug("Parsing View Def Document:\n " + viewDefDocument);

    d.parse(new ByteArrayInputStream(viewDefDocument.getBytes()));

    // digester refuses to parse top-level attributes
    SAXParserFactory parserFactory = SAXParserFactory.newInstance();
    parserFactory.setNamespaceAware(true);
    SAXParser parser = parserFactory.newSAXParser();
    parser.parse(new ByteArrayInputStream(viewDefDocument.getBytes()), new DefaultHandler()
    {
      // attributes for workflow project
      public void startElement(String uri, String localName, String qName, Attributes attrs) throws SAXException
      {
        if (localName.equals("view"))
        {
          setSkin(attrs.getValue("skin"));
          FacesVariableUtil.setSkin(getSkin());
          String showCommonGroupAttr = attrs.getValue("showCommonGroup");
          if (showCommonGroupAttr != null)
            setShowCommonGroup(Boolean.parseBoolean(showCommonGroupAttr));
        }
      }
    });
  }

  private static String readFileFromClasspath(String fileName) throws IOException
  {
    ByteArrayOutputStream outStream = new ByteArrayOutputStream();
    InputStream inStream = null;

    try
    {
      inStream = FileHelper.openConfigurationFile(fileName, Thread.currentThread().getContextClassLoader());
      byte[] buffer = new byte[2048];
      while (true)
      {
        int bytesRead = inStream.read(buffer);
        if (bytesRead == -1)
          break;
        outStream.write(buffer, 0, bytesRead);
      }
    }
    finally
    {
      if (inStream != null)
        inStream.close();
    }

    return outStream.toString();
  }

}
