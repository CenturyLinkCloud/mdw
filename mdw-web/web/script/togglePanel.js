var currentlyExpanded = null;
var inProgress = false;
var somethingOpenedId = null;
var nothingOpenedId = null;
var clientIdPrefix = null;

function collapsePanel(panelId)
{
  currentlyExpanded = null;
  if (!inProgress) 
  {
    document.getElementById(somethingOpenedId).style.display = 'none';
    document.getElementById(nothingOpenedId).style.display = 'inline';
  }
}

function expandPanel(event, panelId)
{
  if (currentlyExpanded != null)
  {
    var elem = findPanelElement(currentlyExpanded);
    inProgress = true;
    SimpleTogglePanelManager.toggleOnClient(event, elem.id);
    inProgress = false;
  }
  document.getElementById(nothingOpenedId).style.display = 'none';
  document.getElementById(somethingOpenedId).style.display = 'inline';
  if (currentlyExpanded == null && !inProgress)
  {
    var elem = findPanelElement(panelId);
    inProgress = true;
    SimpleTogglePanelManager.toggleOnClient(event, elem.id);
    inProgress = false;
  }
  currentlyExpanded = panelId;
}

function findPanelElement(id)
{
  return document.getElementById(clientIdPrefix + id)
}
