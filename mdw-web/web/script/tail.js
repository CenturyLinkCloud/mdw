tailMode = false;
previousTailMode = false;
tailTimer = null;
  
function tail(toTail)
{
  tailMode = toTail;
  dijit.byId('tailModeCheckBox').setChecked(tailMode);
  if (tailMode)
  {
    scrollBottom();
    var ajaxRequest = new AjaxRequest('fileView');
    ajaxRequest.setCallback(
      function(responseObj)
      {
        processAjaxResponse(responseObj);
        return responseObj;
      });
    tailTimer = setInterval(
    function()
    {
      if (tailCount >= 600/tailInterval)  // ten minutes
      {
        tail(false);
      }
      else
      {
        ajaxRequest.invoke(['tailMode=true'], fileViewResponseParams);
        tailCount++;
      }
    }, tailInterval*1000);
  }
  else
  {
    tailCount = 0;
    if (tailTimer)
    {
      clearInterval(tailTimer);
    }
    var ajaxRequest = new AjaxRequest('fileView');
    ajaxRequest.invoke(['tailMode=false'], fileViewResponseParams);
  }
}

function suspendTail()
{
  if (dijit.byId('tailModeCheckBox'))  // applicable?
  {
    previousTailMode = tailMode;
    tail(false);
  }
}

function resumeTail()
{
  if (dijit.byId('tailModeCheckBox'))  // applicable?
  {
    tail(previousTailMode);
    return previousTailMode;
  }
}



  