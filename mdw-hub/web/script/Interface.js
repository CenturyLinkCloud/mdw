function applyInterfaceValues(ifaceName)
{
  var iface = new Interface(ifaceName);
  iface.applyValues();
}

function Interface(name)
{
  this.name = name;
  this.substName = this.name.replace(/ /g, '_');
  this.endPoint = dojo.byId('endPointTextBox_' + this.substName).value;
  if (dojo.byId('topicTextBox_' + this.substName))
    this.topic = dojo.byId('topicTextBox_' + this.substName).value;
  this.logRequest = dojo.byId('logRequestCheckbox_' + this.substName).checked;
  this.logResponse = dojo.byId('logResponseCheckbox_' + this.substName).checked;
  if (dojo.byId('validateRequestCheckbox_' + this.substName))
    this.validateRequest = dojo.byId('validateRequestCheckbox_' + this.substName).checked;
  if (dojo.byId('validateResponseCheckbox_' + this.substName))
    this.validateResponse = dojo.byId('validateResponseCheckbox_' + this.substName).checked;
  this.stubMode = dojo.byId('stubModeCheckbox_' + this.substName).checked;
  if (this.stubMode)
    this.stubbedXml = dojo.byId('stubbedXmlTextArea_' + this.substName).value;
}

Interface.prototype.applyValues = function()
{
  var progressBarId = 'ifaceProgressBarPane_' + this.substName;
  var statusId = 'ifaceStatus_' + this.substName;
  dojo.byId(statusId).style.display = 'none';
  startProgressBar(progressBarId);
  var json = JSON.stringify(this);
  
  var applyRequest = new AjaxRequest('configManager');
  applyRequest.setCallback(
    function(responseObj)
    {
      var status = responseObj.configManager_status;
      stopProgressBar(progressBarId);
      dojo.byId(statusId).innerHTML = status;
      dojo.byId(statusId).style.display = 'block';
      return responseObj;
    });
  applyRequest.invokePost('interfaceInstances["' + this.name + '"]', json, ['status']);    
}
