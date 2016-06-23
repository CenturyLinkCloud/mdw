// options menu functions
function toggleLineNumbers(menuItemId)
{
  showLineNumbers(toggleMenuItem(menuItemId));
}

function toggleTimeStamps(menuItemId)
{
  showTimeStamps(toggleMenuItem(menuItemId));
}

function showLineNumbers(show)
{
  var lncp = dojo.byId('lineNumbersContentPane');
  if (show)
  {
    lncp.style.display = 'block';
    findTop();
  }
  else
  {
    lncp.style.display = 'none';
  }
  dijit.byId('mainLayoutContainer').resize();
}

function showTimeStamps(show)
{
  showTimeStampsPref = show;
  reloadTree(dijit.byId('filePanelDirectoryTree'));
  reloadTree(dijit.byId('propertyHelperDirectoryTree'));
}

function setFileViewFontSize(value)
{
  fileViewFontSize = value;
  setRadioMenuItem('fontSizeMenu', 'fileViewFontSize', value);
  dojo.byId('fileView').firstChild.style.fontSize = value;
  dojo.byId("lineNumbersContentPane").firstChild.style.fontSize = value;
  calcPixelsPerLine();
  scrollTop();
  dijit.byId('mainLayoutContainer').resize();
}

function setBufferLines(value)
{
  suspendTail();
  bufferLines = value;
  var ajaxRequest = new AjaxRequest('fileView');
  ajaxRequest.setCallback(processAjaxResponse);
  ajaxRequest.invoke(['bufferLines=' + bufferLines], fileViewResponseParams);
  setPreferenceValue('bufferLines', bufferLines);
  resumeTail();
}

function setRefetchThreshold(value)
{
  refetchThreshold = value;
  setPreferenceValue('refetchThreshold', refetchThreshold, true);
}

function setTailInterval(value)
{
  suspendTail();
  tailInterval = value;
  setPreferenceValue('tailInterval', tailInterval, true);
  resumeTail();
}

function setSliderIncrementLines(value)
{
  sliderIncrementLines = value/2;
  setSliderIncrements();
  setPreferenceValue('sliderIncrementLines', value);
  scrollTop();
}

function setSliderIncrements()
{
  var vs = dijit.byId('fileViewVerticalSlider');
  vs.maximum = totalLines - getClientLines();
  if (vs.maximum == 0)
    vs.maximum = 1;
  vs.discreteValues = Math.round((totalLines - getClientLines())/sliderIncrementLines);
  vs.value = vs.maximum * (1 - lineIndex/(totalLines - getClientLines()));
}

function toggleProjectSelection(id)
{
  var checked = dijit.byId(id).checkState;
  setMenuItemChecked(id, !checked);
  dijit.byId(id).checkState = !checked;
  var newPrefVal = '';
  var projMenu = dijit.byId('myProjectsMenu');
  var children = projMenu.getChildren();
  for (var i = 0; i < children.length; i++)
  {
    if (children[i].checkState)
      newPrefVal += children[i].label + ',';
  }
  newPrefVal = newPrefVal.substring(0, newPrefVal.length - 1);
  setPreferenceValue('myProjects', newPrefVal);
  reloadProjectTree(dijit.byId(id).label, !checked);  
}

function toggleMenuItem(menuItemId)
{
  var checked = ('true' == getPreferenceValue(menuItemId));
  setMenuItemChecked(menuItemId, !checked);
  setPreferenceValue(menuItemId, '' + !checked);
  return !checked;
}
function setRadioMenuItem(menuId, name, value)
{
  var menu = dijit.byId(menuId);
  var menuItems = menu.getChildren();
  for (i = 0; i < menuItems.length; i++)
  {
    var menuItem = menuItems[i];
    var checked = menuItem.id.indexOf(value) == menuItem.id.length - value.length;
    setMenuItemChecked(menuItem.id, checked);
  }
  setPreferenceValue(name, value);
}
function setMenuItemChecked(menuItemId, checked)
{
  var menuItem = dijit.byId(menuItemId);
  if (checked)
  {
    dojo.removeClass(menuItem.iconNode, 'menuItemCheckboxUnchecked');
    dojo.addClass(menuItem.iconNode, 'menuItemCheckboxChecked');
  }
  else
  {
    dojo.removeClass(menuItem.iconNode, 'menuItemCheckboxChecked');
    dojo.addClass(menuItem.iconNode, 'menuItemCheckboxUnchecked');
  }
}
function setCloudUser(cloudUser)
{
  var ajaxRequest = new AjaxRequest('configView');
  ajaxRequest.invoke(['cloudUser=' + cloudUser], 'cloudUser');
  setPreferenceValue('cloudUser', cloudUser);  
}
function getPreferenceValue(menuItemId)
{
  var val = dojo.cookie('mdw:' + menuItemId);  // compatibility
  if (val == null)
	  val = dojo.cookie('mdw.' + menuItemId);
}
function setPreferenceValue(menuItemId, value)
{
  dojo.cookie('mdw.' + menuItemId, value, {expires:365});
}

