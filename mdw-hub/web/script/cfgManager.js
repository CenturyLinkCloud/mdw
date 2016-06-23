mdwMajorVersion = -1;
mdwMinorVersion = -1;
mdwBuildNumber = -1;

function initConfigManager()
{
  var tabContainer = dijit.byId('configViewTabContainer');
  var overviewTab = dijit.byId('configOverviewTab');
  var manageTab = dijit.byId('configManageTab');
  var defineTab = dijit.byId('configDefineTab');
  
  dojo.connect(tabContainer, 'selectChild', function()
    {
      if (this.selectedChildWidget.id == 'configManageTab')
      {
        dojo.byId('manageWarningDiv').style.display = 'none';
        dojo.byId('interfacesAccordionContainer').style.display = 'none';

        if (isAtLeastMdw4212())
        {
          createInterfacesAccordion();
        }
        else
        {
          dojo.byId('manageWarningDiv').innerHTML = 'Feature will be available when this workflow application upgrades to MDW 5.5.';
          dojo.byId('manageWarningDiv').style.display = 'block';
        }
      }
    });
  
  var accordion = dijit.byId('interfacesAccordion');
  dojo.connect(accordion, 'selectChild', function()
    {
      accordion.domNode.style.borderBottom = '1px solid #bfbfbf';
    });
}

dojo.addOnLoad(initConfigManager);

var cmProgressBarTimeout;

function displayWorkflowApp(appName)
{  
  showApplicationLevelTabs();

  dojo.byId('applicationInfo').innerHTML = appName;

  showApplicationLevelSections(false);
  showEnvironmentLevelSections(false);
  startProgressBar('cmProgressBarPane');
  
  // retrieve details and urls
  var detailsRequest = new AjaxRequest('configView');
  detailsRequest.setResponseType('json');
  detailsRequest.setCallback(
    function(responseObj)
    {
      var appDetails = dojo.byId('appDetails');
      appDetails.innerHTML = responseObj.configView_appDetails;
      var appLinksUl = dojo.byId('applicationUrlsList');
      appLinksUl.innerHTML = responseObj.configView_appLinks;
      
      stopProgressBar('cmProgressBarPane');
      showEnvironmentLevelSections(false);
      showApplicationLevelSections(true);
            
      return responseObj;
    });
  detailsRequest.invoke(['workflowApp=' + appName], ['appDetails', 'appLinks']);

}

function displayWorkflowEnvironment(appName, environment)
{
  showEnvironmentLevelTabs();

  dojo.byId('environmentInfo').innerHTML = appName + ' ' + environment;
  dojo.byId('environmentInfo_manage').innerHTML = appName + ' ' + environment;
  
  showApplicationLevelSections(false);
  showEnvironmentLevelSections(false);
  startProgressBar('cmProgressBarPane');
  
  // retrieve build info (also sets workflowApp and environment so run first)
  var buildInfoRequest = new AjaxRequest('configView');
  buildInfoRequest.setResponseType('text');
  buildInfoRequest.setCallback(
    function(responseObj)
    {
      dojo.byId('buildInfo').innerHTML = responseObj;
      dojo.byId('buildInfo_manage').innerHTML = responseObj;
      
      // retrieve urls
      var urlsRequest = new AjaxRequest('configView');
      urlsRequest.setResponseType('json');
      urlsRequest.setCallback(
        function(responseObj)
        {
          mdwMajorVersion = responseObj.configView_mdwMajorVersion;
          mdwMinorVersion = responseObj.configView_mdwMinorVersion;
          mdwBuildNumber = responseObj.configView_mdwBuildNumber;

          var mdwHubLink = dojo.byId('mdwHubLink');
          var mdwHubUrl = responseObj.configView_mdwHubUrl;
          dojo.byId('mdwHubUrl').style.display = (mdwHubUrl == null || mdwHubUrl == '') ? 'none' : '';
          if (mdwHubUrl != null && mdwHubUrl != '')
          {
            mdwHubLink.href = responseObj.configView_mdwHubUrl;
            mdwHubLink.innerHTML = responseObj.configView_mdwHubUrl;
          }
          
          var mdwWebLink = dojo.byId('mdwWebLink');
          var mdwWebUrl = responseObj.configView_mdwWebUrl;
          dojo.byId('mdwWebUrl').style.display = (mdwWebUrl == null || mdwWebUrl == '') ? 'none' : '';
          if (mdwWebUrl != null && mdwWebUrl != '')
          {
            mdwWebLink.href = responseObj.configView_mdwWebUrl;
            mdwWebLink.innerHTML = responseObj.configView_mdwWebUrl;
          }
          
          var designerRcpLink = dojo.byId('designerRcpLink');
          designerRcpLink.href = responseObj.configView_designerRcpUrl;
          designerRcpLink.innerHTML = responseObj.configView_designerRcpLabel;
          
          var taskManagerLink = dojo.byId('taskManagerLink');
          var taskManagerUrl = responseObj.configView_taskManagerUrl;
          dojo.byId('taskManagerUrl').style.display = (taskManagerUrl == null || taskManagerUrl == '') ? 'none' : '';
          if (taskManagerUrl != null && taskManagerUrl != '')
          {
            taskManagerLink.href = responseObj.configView_taskManagerUrl;
            taskManagerLink.innerHTML = responseObj.configView_taskManagerUrl;
          }
          
          var reportsLink = dojo.byId('reportsLink');
          var reportsUrl = responseObj.configView_reportsUrl;
          dojo.byId('reportsUrl').style.display = (reportsUrl == null || reportsUrl == '') ? 'none' : '';
          if (reportsUrl != null && reportsUrl != '')
          {
            reportsLink.href = responseObj.configView_reportsUrl;
            reportsLink.innerHTML = responseObj.configView_reportsUrl;
          }
          
          var consoleLink = dojo.byId('consoleLink');
          consoleLink.href = responseObj.configView_consoleUrl;
          consoleLink.innerHTML = responseObj.configView_consoleUrl;
          return responseObj;
        });
      urlsRequest.invoke(null, ['mdwHubUrl', 'mdwWebUrl', 'designerRcpUrl', 'designerRcpLabel', 'taskManagerUrl', 'reportsUrl', 'consoleUrl', 'mdwMajorVersion', 'mdwMinorVersion', 'mdwBuildNumber', 'container']);

      // retrieve system info
      var sysInfoRequest = new AjaxRequest('configView');
      sysInfoRequest.setResponseType('text');
      sysInfoRequest.setCallback(
        function(responseObj)
        {
          dojo.byId('sysInfoPane').innerHTML = responseObj;
          stopProgressBar('cmProgressBarPane');
          showApplicationLevelSections(false);
          showEnvironmentLevelSections(true);
          return responseObj;
        });
      sysInfoRequest.invoke(null, ['sysInfo']);

      return responseObj;
    });
  buildInfoRequest.invoke(['workflowApp=' + appName, 'environment=' + environment], ['buildInfo']);        
}

function showApplicationLevelSections(show)
{
  var displayStyle = show ? 'block' : 'none';
  dojo.byId('applicationInfo').style.display = displayStyle;
  dojo.byId('appDetails').style.display = displayStyle;
  dojo.byId('applicationUrls').style.display = displayStyle;
  dojo.byId('footerPane').style.display = displayStyle;
}

function showEnvironmentLevelSections(show)
{
  var displayStyle = show ? 'block' : 'none';
  dojo.byId('environmentInfo').style.display = displayStyle;
  dojo.byId('buildInfo').style.display = displayStyle;
  dojo.byId('environmentUrls').style.display = displayStyle;
  dojo.byId('sysInfoLabel').style.display = displayStyle;
  dojo.byId('sysInfoPane').style.display = displayStyle;
  dojo.byId('footerPane').style.display = displayStyle;
}

function showApplicationLevelTabs()
{
  var tabContainer = dijit.byId('configViewTabContainer');
  var defineTab = dijit.byId('configDefineTab');
  var overviewTab = dijit.byId('configOverviewTab');
  var manageTab = dijit.byId('configManageTab');
  
  var children = tabContainer.getChildren();
  var defineTabPresent = false;
  for (i = 0; i < children.length; i++)
  {
    if (children[i] == manageTab)
      tabContainer.removeChild(manageTab);
    else if (children[i] == defineTab)
      defineTabPresent = true;
  }
  if (!defineTabPresent)
    tabContainer.addChild(defineTab);
  
  tabContainer.selectChild(overviewTab);
}

function showEnvironmentLevelTabs()
{
  var tabContainer = dijit.byId('configViewTabContainer');
  var defineTab = dijit.byId('configDefineTab');
  var manageTab = dijit.byId('configManageTab');
  var overviewTab = dijit.byId('configOverviewTab');
  
  var children = tabContainer.getChildren();
  var manageTabPresent = false;
  for (i = 0; i < children.length; i++)
  {
    if (children[i] == defineTab)
      tabContainer.removeChild(defineTab);
    else if (children[i] == manageTab)
      manageTabPresent = true;
  }
  if (!manageTabPresent)
    tabContainer.addChild(manageTab);
  
  tabContainer.selectChild(overviewTab);
}

function startProgressBar(id)
{
  if (dojo.byId(id))
    cmProgressBarTimeout = setTimeout("dojo.byId('" +id + "').style.display = 'block'", 250);
}
function stopProgressBar(id)
{
  if (dojo.byId(id))
  {
    clearTimeout(cmProgressBarTimeout);
    dojo.byId(id).style.display = 'none';
  }
}

function createInterfacesAccordion()
{
  var container = dojo.byId('interfacesAccordionContainer');

  if (container != null)
  {
    var accordion = dijit.byId('interfacesAccordion');
    //accordion.resize();
    container.style.display = 'none';
    var children = accordion.getChildren();
    if (children.length == 0)
    {
      var accordionPane0 = new dijit.layout.AccordionPane({id:'pane0', title:'Pane 0', selected:true, style:'display:none;height:0px;'});
      accordionPane0.isContainer = false;
      accordion.addChild(accordionPane0);
    }

    for (i = 0; i < children.length; i++)
    {
      if (i > 0)
      {
        accordion.removeChild(children[i]);
        children[i].destroyRecursive();
      }
    }
    
    // retrieve the interfaces
    dojo.byId('environmentInfo_manage').style.display = 'none';
    dojo.byId('buildInfo_manage').style.display = 'none';
    dojo.byId('importExportButtons').style.display = 'none';
    dojo.byId('manageDiv').style.display = 'none';
    startProgressBar('cmProgressBarPane_manage');
    var interfacesRequest = new AjaxRequest('configManager');
    interfacesRequest.setResponseType('json');
    interfacesRequest.setCallback(
      function(responseObj)
      {
        var interfacesJson = responseObj.configManager_interfacesJson;
        var interfaces = eval(interfacesJson);
        for (i = 0; i < interfaces.length; i++)
        {
          var name = interfaces[i].name;
          var nameSubst = name.replace(/ /g, '_');
          var id = nameSubst + "_Pane";
          var title = name + '&nbsp;&nbsp;  (' + interfaces[i].protocol + ' : ' + interfaces[i].direction + ')';
          var accordionPane = new dijit.layout.AccordionPane({id:id, title:title, selected:false, href:'../configManager/interface.jsf?name=' + escape(name) + '&id=' + escape(nameSubst)});
          accordion.addChild(accordionPane);
        }
        stopProgressBar('cmProgressBarPane_manage');
        
        dojo.byId('environmentInfo_manage').style.display = 'block';
        dojo.byId('buildInfo_manage').style.display = 'block';
        dojo.byId('manageDiv').style.display = 'block';
        dojo.byId('importExportButtons').style.display = 'block';
        dojo.byId('interfacesAccordionContainer').style.display = 'block';

        accordion.domNode.style.height = (23 * accordion.getChildren().length + 410) + 'px';
        accordion.resize();
        
        return responseObj;
      });
    interfacesRequest.invoke(null, ['interfacesJson']);    
    
  }
}

function downloadRcpZip()
{
  window.location = 'http://qshare/sites/MDW/Releases/Designer%20RCP.html';
}

function enableFormElement(element, enabled)
{
  element.disabled = !enabled;
  if (enabled)
  {
    element.style.backgroundColor = 'white';
  }
  else
  {
    element.style.backgroundColor = '#dcdcdc';
  }
}

function isAtLeastMdw4212()
{
  return mdwMajorVersion > 4 || (mdwMajorVersion == 4 && mdwMinorVersion > 2)
    || (mdwMajorVersion == 4 && mdwMinorVersion == 2 && mdwBuildNumber >= 12);
}
