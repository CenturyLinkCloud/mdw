previousTreeSelectionNode = null;
fpProgressBarTimeout = null;
showTimeStampsPref = false;

function initDirPanel()
{
  dirData = dojo.byId('filePanelDirectoryData');
  if (dirData)
    showTimeStampsPref = !!dirData.getAttribute('showTimeStamps');
  
  var propGroupTree = dijit.byId('propertyGroupTree');
  dojo.connect(propGroupTree, '_onLoadAllItems', function()
    {
      var propGroupTreeCookie = dojo.cookie(propGroupTree.cookieName);
      if (!propGroupTreeCookie)
      {
        propGroupTree.collapse();
      }
    });
}
dojo.addOnLoad(initDirPanel);

function getTreeLabel(item)
{
  if (item)
  {
    if (item.type == 'file')
    {
      return item.name + (showTimeStampsPref ? ' - (' + item.timestamp + ')' : '');
    }
    else if (item.type == 'propGroup' && item.name == "")
    {
      return "/";
    }
    else
    {
      return item.name;
    }
  }
  return null;
}

function getTreeIconClass(item)
{
  if (item)
  {
    if (item.type == 'directory')
      return 'treeItemDir';
    else if (item.type == 'file')
      return 'treeItemFile';
    else if (item.type == 'propGroup')
      return 'treeItemGroup';
    else if (item.type == 'propItem')
      return 'treeItemProp';
  }
  return null;
}

function dirTreeNodeSelect(message)
{
  if (message.event == 'execute')
  {
    if (previousTreeSelectionNode)
      previousTreeSelectionNode.labelNode.className = '';
    if (message.item)
    {
      message.node.labelNode.className = 'treeLabelSelected';      
      previousTreeSelectionNode = message.node;
    }
  }
}

function deselectTreeNode()
{
  if (previousTreeSelectionNode)
    previousTreeSelectionNode.labelNode.className = '';
  enableRefresh(false);
  enableDownload(false);  
}

function selectDirTreeNodeByPath(path)
{
  filepath = path;
  var filePanelDirTree = dijit.byId('filePanelDirectoryTree');
  filePanelDirTree.expand();
  var fpStartNode = filePanelDirTree._navToRootOrFirstNode();
  var node = findNode(filePanelDirTree, fpStartNode, path);
  
  if (!node)
  {
    var propsDirTree = dijit.byId('propertyHelperDirectoryTree');
    propsDirTree.expand();
    var propsStartNode = propsDirTree._navToRootOrFirstNode();
    node = findNode(propsDirTree, propsStartNode, path);  
  }
  
  if (previousTreeSelectionNode)
    previousTreeSelectionNode.labelNode.className = '';
  node.labelNode.className = 'treeLabelSelected';
  previousTreeSelectionNode = node;
}

function findNode(tree, node, path)
{
  while (node && (!node.item || node.item.path != path))
  {
    tree._expandNode(node);
    node = navToNextNode(tree, node);
  }
  return node;
}
  
function navToNextNode(tree, node)
{
  var returnNode;
  // get the first child
  if (node.isExpandable && node.isExpanded && node.hasChildren())
  {
    returnNode = node.getChildren()[0];     
  }
  else
  {
    // find a parent node with a sibling
    while (node && node.isTreeNode)
    {
      returnNode = node.getNextSibling();
      if (returnNode)
      {
        tree._collapseNode(node);
        break;
      }
      tree._collapseNode(node);
      node = node.getParent();
    }
  }
  return returnNode;
}

function filePanelDirTreeSelect(message)
{
  if (message.event == 'execute' && message.item)
  {
    displayToolBar();
    displayFileView();
    
    if (editMode)
      edit(false);
    var path = escape(message.item.path);
    suspendTail();
    filepath = path;
    enableGrep(true);
    enableEdit(message.item.editable);
    var binary = message.item.binary;
    var maskable = message.item.maskable;
    var ajaxRequest = new AjaxRequest('fileView');
    if (message.item.type == 'file')
    {
      // file selected
      enableRefresh(true);
      enableDownload(!message.item.maskable);      
      dojo.byId('fileView').innerHTML = '';
      startFileViewProgressBar();
      ajaxRequest.setCallback(
        function(responseObj)
        {
          processAjaxResponse(responseObj);
          dojo.byId('fileInfo').innerHTML = path + '<br/>' + responseObj.fileView_fileInfo;
          dojo.byId('grepDirectory').innerHTML = path.substring(0, path.lastIndexOf('/'));
          dijit.byId('mainLayoutContainer').resize();
          if (!resumeTail())
          {
            scrollTop();
            setScrollLeft(0, true);
          }
          stopFileViewProgressBar();
          return responseObj;
        });
      ajaxRequest.invoke(['filePath=' + path, 'lineIndex=0', 'bufferLines=' + bufferLines, 'binary=' + binary, 'maskable=' + maskable], fileViewResponseParams);
    }
    else
    {
      // dir selected
      enableRefresh(false);
      enableDownload(false);      
      dojo.byId('fileView').innerHTML = '';
      startFileViewProgressBar();
      ajaxRequest.setCallback(
        function(responseObj)
        {
          processAjaxResponse(responseObj);
          dojo.byId('fileInfo').innerHTML = path + '<br/>' + responseObj.fileView_fileInfo;
          dojo.byId('grepDirectory').innerHTML = path;
          dijit.byId('mainLayoutContainer').resize();
          scrollTop();
          setSliderIncrements();
          stopFileViewProgressBar();
          return responseObj;
        });
      ajaxRequest.invoke(['filePath=' + path], fileViewResponseParams);
    }
    
    setSearchMessage('');
    setToolsMessage('');
  }
}

function filePanelPropertySelect(message)
{
  if (message.event == 'execute' && message.item)
  {
    suspendTail();
    if (editMode)
      edit(false);
    enableEdit(false);
    enableGrep(false);
    lineIndex = 0;
    resetSearchIndex();
    
    dojo.byId('fileInfo').innerHTML = '<br/><br/>';
    dojo.byId('grepDirectory').innerHTML = '';
    
    if (message.item.type == 'propItem')
    {
      editProp(message.item);
    }
    else if (message.item.type == 'propGroup')
    {
      var ajaxRequest = new AjaxRequest('propertyGroupView');
      displayToolBar();
      displayFileView();
      dojo.byId('fileView').innerHTML = '';
      startFileViewProgressBar();
      ajaxRequest.setCallback(
        function(responseObj)
        {
          buffer = responseObj.propertyGroupView_groupInfo;
          dojo.byId('fileView').innerHTML = "<pre class='fileView' style='font-size:" + fileViewFontSize + ";'>\n" + buffer + "\n</pre>";
          totalLines = parseInt(responseObj.propertyGroupView_infoLines);
          dijit.byId('mainLayoutContainer').resize();
          var sl = dijit.byId('fileViewVerticalSlider');
          if (sl)
          {
            sl.progressBar.style[sl._progressPixelSize] = (100) + "%";
            sl.remainingBar.style[sl._progressPixelSize] = (0) + "%";
          }
          dojo.byId('lineInfo').innerHTML = (totalLines == 0 ? '0' : '1') + ' / ' + totalLines;
          setSliderIncrements();
          stopFileViewProgressBar();
          return responseObj;
        });
      ajaxRequest.invoke(['group=' + message.item.name], ['groupInfo','infoLines']);
    }
  }
}

function reloadTree(tree)
{  
  // rebuild store
  var storeurl = tree.store._jsonFileUrl;
  var newStore = new dojo.data.ItemFileReadStore({url: storeurl});
 
  // destroy and rebuild tree
  var treeLabel = tree.label;
  var treeId = tree.id;
  var containerID = tree.domNode.parentNode.id;
  var treeClass = tree['class'];
  var treeQuery = tree['query'];
  var treeLabelAttr = tree['labelAttr'];
  var treeGetLabel = tree['getLabel'];
  var customIconMethod = tree.getIconClass;
  tree.destroyRecursive(true);
 
  var newTree = new dijit.Tree(
    {
      'store':        newStore,
      'label':        treeLabel,
      'id':           treeId,
      'class':        treeClass,
      'query':        treeQuery,
      'labelAttr':    treeLabelAttr,
      'getLabel':     treeGetLabel,
      'getIconClass': customIconMethod
    },
    document.createElement(treeId) );
 
  dojo.byId(containerID).appendChild(newTree.domNode);
} 

function startFileViewProgressBar()
{
  fpProgressBarTimeout = setTimeout("dojo.byId('fpProgressBarPane').style.display = 'block'", 250);
}
function stopFileViewProgressBar()
{
  clearTimeout(fpProgressBarTimeout);
  dojo.byId('fpProgressBarPane').style.display = 'none';
}