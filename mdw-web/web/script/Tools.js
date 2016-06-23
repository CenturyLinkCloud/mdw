setToolsMessage('');

function stackDump()
{
  deselectTreeNode();
  setToolsMessage('Thread dump displayed');
  var tools = new Tools();
  tools.performAction("stackDump");
}

function memoryInfo()
{
  deselectTreeNode();
  setToolsMessage('Memory info displayed');
  var tools = new Tools();
  tools.performAction("memoryInfo");
}

function Tools()
{
}

Tools.prototype.performAction = function(action)
{
  dojo.byId('fileView').innerHTML = '';
  startFileViewProgressBar();

  var ajaxRequest = new AjaxRequest('fileView');
  ajaxRequest.setCallback(
    function(responseObj)
    {
      var newTotalLines = parseInt(responseObj.fileView_lineCount);
      if (bufferLines < newTotalLines)
        bufferLines = newTotalLines;

      processAjaxResponse(responseObj);
      scrollTop();
      dijit.byId('mainLayoutContainer').resize();
      stopFileViewProgressBar();
      return responseObj;
    });
  ajaxRequest.invoke(['action=' + action], fileViewResponseParams);
}


function setToolsMessage(message)
{
  dojo.byId('toolsMessage').innerHTML = message;
}