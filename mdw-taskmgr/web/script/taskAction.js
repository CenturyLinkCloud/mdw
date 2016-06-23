// task action javascript

// these functions are for the new taskActionMenu component
function fixSubMenus()
{
  var assignGrp = findDiv('mdwAssignMenuGroup_menu');
  if (assignGrp != null && assignGrp.parentNode != null)
  {
    var assignListNode = findMenuListNode(assignGrp);
    if (assignListNode != null)
    {
      assignListNode.style.maxHeight = getDocHeight() - getVertOffset(assignGrp.parentNode) - 10 + 'px';
      assignListNode.style.overflow = 'auto';
      if (isIe7())
      {
        assignGrp.style.position = 'relative';
        assignListNode.style.position = 'relative';
        assignListNode.style.overflowX = 'hidden';
      }
    }
  }
  var forwardGrp = findDiv('mdwForwardMenuGroup_menu');
  if (forwardGrp != null && forwardGrp.parentNode != null)
  {
    var forwardListNode = findMenuListNode(forwardGrp);
    if (forwardListNode != null)
    {
      forwardListNode.style.maxHeight = getDocHeight() - getVertOffset(forwardGrp.parentNode) - 10 + 'px';
      forwardListNode.style.overflow = 'auto';
      forwardListNode.style.width = forwardGrp.style.width;
      if (isIe7())
      {
        forwardGrp.style.position = 'relative';
        forwardListNode.style.position = 'relative';
        forwardListNode.style.overflowX = 'hidden';
      }
    }
  }
}

function findMenuListNode(group)
{
  if (group.hasChildNodes())
  {
    for (var i = 0; i < group.childNodes.length; i++)
    {
      var node = group.childNodes[i];
      if (node.className == 'rich-menu-list-bg')
        return node;
    }
  }
  return null;
}

function getDocHeight()
{
  var body = document.body;
  var html = document.documentElement;

  return Math.max(body.scrollHeight, body.offsetHeight, 
                  html.clientHeight, html.scrollHeight, html.offsetHeight);
}

function getVertOffset(el)
{
  if (isIe7())
    return el.offsetTop - el.scrollTop;
  
  var y = 0;
  while(el && !isNaN(el.offsetTop))
  {
    y += el.offsetTop - el.scrollTop;
    el = el.offsetParent;
  }
  return y;
}

function findDiv(suffix)
{
  var allDivs = document.getElementsByTagName("div");
  for (i = 0; i < allDivs.length; i++)
  {
    var element = allDivs[i];
    if (element.id && element.id != null && element.id.endsWith(suffix))
      return element;
  }
  return null;
}

function isIe7()
{
  if (navigator.appVersion.indexOf("MSIE") == -1)
    return false;
  if (!document.documentMode)
    return true;
  if (document.documentMode < 8)
    return true;
  return parseFloat(navigator.appVersion.split("MSIE")[1]) < 8;
}

// the functions below are for the old taskAction component
function taskCommentOnClick(commentInput)
{
  if (commentInput.value == '[Enter Comment]')
  {
    commentInput.value = '';
  }
}

function showHideUserSelect(selObj, userMenuId)
{
  userSelect = null;
  for (i = 0; i < document.forms.length; i++)
  {
    elements = document.forms[i].elements;
    for (j = 0; j < elements.length; j++)
    {
      if (elements[j].name.indexOf(userMenuId) >= 0)
      {
        userSelect = elements[j];
      }
    }
  }
    
  if (userSelect != null)
  {
    if (selObj.options[selObj.selectedIndex].value == 'Assign')
    {
      userSelect.style['width'] = '';
      userSelect.style['visibility'] = 'visible';
    }
    else
    {
      userSelect.style['width'] = '0';
      userSelect.style['visibility'] = 'hidden';
    }
  }  
}

function showHideDestination(selObj, destinationMenuId)
{
  destinationSelect = null;
  for (i = 0; i < document.forms.length; i++)
  {
    elements = document.forms[i].elements;
    for (j = 0; j < elements.length; j++)
    {
      if (elements[j].name.indexOf(destinationMenuId) >= 0)
      {
        destinationSelect = elements[j];
      }
    }
  }
    
  if (destinationSelect != null)
  {
    if (selObj.options[selObj.selectedIndex].value == 'Forward')
    {
      destinationSelect.style['width'] = '';
      destinationSelect.style['visibility'] = 'visible';
    }
    else
    {
      destinationSelect.style['width'] = '0';
      destinationSelect.style['visibility'] = 'hidden';
    }
  }  
}

function showHideComment(selObj, taskCancelId)
{
  taskCancel = null;
  for (i = 0; i < document.forms.length; i++)
  {
    elements = document.forms[i].elements;
    for (j = 0; j < elements.length; j++)
    {
      if (elements[j].name.indexOf(taskCancelId) >= 0)
      {
        taskCancel = elements[j];
      }
    }
  }

  if (taskCancel != null)
  {
    if (selObj.options[selObj.selectedIndex].value == 'Cancel')
    {
      taskCancel.style['width'] = '300px';
      taskCancel.style['visibility'] = 'visible';
    }
    else
    {
      taskCancel.style['width'] = '0';
      taskCancel.style['visibility'] = 'hidden';
    }
  }  

}

function showCommentDialog(taskAction, actionItem)
{
  var hid = document.getElementById('filteredListForm:submitCommentsHiddenAction');
  if (hid == null)
    hid = document.getElementById('mainDetailForm:actionForm:submitCommentsHiddenAction');
  hid.value = taskAction;

  var hidIt = document.getElementById('filteredListForm:submitCommentsHiddenItem');
  if (hidIt == null)
    hidIt = document.getElementById('mainDetailForm:actionForm:submitCommentsHiddenItem');
  hidIt.value = actionItem;
  
  var dlgObj = document.getElementById('filteredListForm:commentDlg');
  if (dlgObj == null)
    dlgObj = document.getElementById('mainDetailForm:actionForm:commentDlg');
  
  dlgObj.component.show();
}

function submitComments()
{
  var com = document.getElementById('filteredListForm:submitCommentsHiddenComment');
  if (com == null)
    com = document.getElementById('mainDetailForm:actionForm:submitCommentsHiddenComment');
  
  var comSrc = document.getElementById('filteredListForm:taskActionComments');
  if (comSrc == null)
    comSrc = document.getElementById('mainDetailForm:actionForm:taskActionComments');
  
  com.value = comSrc.value;
  
  var btn = document.getElementById('filteredListForm:submitCommentsHiddenButton');
  if (btn == null)
    btn = document.getElementById('mainDetailForm:actionForm:submitCommentsHiddenButton');
  
  btn.click();
}
