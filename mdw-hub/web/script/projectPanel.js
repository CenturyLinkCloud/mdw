filterConnect = null;
function initProjectPanel()
{
  if (filterConnect)
    dojo.disconnect(filterConnect);
  filterConnect = dojo.connect(dijit.byId('cmProjectTree'), '_onLoadAllItems', filterTreeItems);
  if (dojo.isIE)
    dojo.connect(document, 'reload', ieReload());
  buildMyProjectsMenu();
}
dojo.addOnLoad(initProjectPanel);

function ieReload()
{
  var tree = dijit.byId('cmProjectTree');
  buildMyProjectsMenu();
  filterTreeItems(tree);
}

function getProjectTreeIconClass(item)
{
  if (item)
  {
    if (item.type == 'workflowApp')
      return 'treeItemProject';
    else
      return 'treeItemEnv';
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

function cmProjectTreeSelect(message)
{
  if (message.event == 'execute' && message.item)
  {
    var name = message.item.name;
    var id = message.item.id;
    var workflowApp = '';    
    dojo.byId('configViewTabContainer').style.visibility = 'visible';
    if (message.item.type == 'workflowApp')
    {
      // workflow app selected
      workflowApp = message.item.name;
      displayWorkflowApp(workflowApp);
    }
    else if (message.item.type == 'environment')
    {
      // environment selected      
      workflowApp = message.item.workflowApp;
      var environment = message.item.name;
      displayWorkflowEnvironment(workflowApp, environment);    
    }
    else
    {
      dojo.byId('configViewTabContainer').style.visibility = 'hidden';
    }
  }
}

function filterTreeItems(tree)
{
  var children = tree.getChildren();
  var lastChild = null;
  for (var i = 0; i < children.length; i++)
  {
    var item = children[i].item;
    if (item.type == 'workflowApp')
    {
      if (!(item.selected == true || item.selected == 'true'))
      {
        tree.removeChild(children[i]);
        children[i].destroyRecursive();
      }
      else
      {
        lastChild = children[i];
      }
    }
  }
  if (lastChild)
    dojo.toggleClass(lastChild.domNode, "dijitTreeIsLast", true);
}

function buildMyProjectsMenu()
{
  var projRequest = new AjaxRequest('');
  projRequest.setResponseType('json');
  projRequest.setPreventCache(true);
  projRequest.setRequestUrl('../configManager/projectData.jsf');
  projRequest.setCallback(
    function(responseObj)
    {
      var projJson = responseObj;
      var projects = projJson.items;
      for (var i = 0; i < projects.length; i++)
      {
        var name = projects[i].name;
        var id = name.replace(/ /g, '_');
        var projMenu = dijit.byId('myProjectsMenu');
        var menuItem = new dijit.MenuItem({id:id, label:name, checkState:projects[i].selected});
        setMenuItemChecked(id, projects[i].selected);
        dojo.connect(menuItem, "onClick", function(){toggleProjectSelection(this.id);});
        projMenu.addChild(menuItem);
      }      
      return responseObj;
    });
  projRequest.invoke();
}

function reloadProjectTree(toggledProject, newVal)
{
  var tree = dijit.byId('cmProjectTree');

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
  var oldState = new Array();
  var oldChildren = tree.getChildren();
  for (var i = 0; i < oldChildren.length; i++)
    oldState[oldChildren[i].item.name] = oldChildren[i].item.selected;
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
    document.createElement(treeId));
 
  if (filterConnect)
    dojo.disconnect(filterConnect);
  filterConnect = dojo.connect(newTree, '_onLoadAllItems', filterTreeItems);
  if (dojo.isIE)
  {
    var children = newTree.getChildren();
    for (var j = 0; j < children.length; j++)
    {
      if (children[j].item.name == toggledProject)
        children[j].item.selected = newVal;
      else if (oldState[children[j].item.name])
        children[j].item.selected = oldState[children[j].item.name];
    }
    filterTreeItems(newTree);
  }
  dojo.byId(containerID).appendChild(newTree.domNode);
} 
