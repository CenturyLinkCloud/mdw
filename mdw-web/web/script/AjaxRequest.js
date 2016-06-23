function AjaxRequest(managedBean)
{
  this.managedBean = managedBean;
  this.responseType = 'json';
  this.preventCache = false;
}

AjaxRequest.prototype.setCallback = function(callback)
{
  this.callback = callback;
}

AjaxRequest.prototype.setResponseType = function(responseType)
{
  this.responseType = responseType;
}

AjaxRequest.prototype.setPreventCache = function(preventCache)
{
  this.preventCache = preventCache;
}

AjaxRequest.prototype.setRequestUrl = function(url)
{
  this.requestUrl = url;
}

AjaxRequest.prototype.buildRequestParams = function(params)
{
  if (params == null)
    return '&';
  
  var requestParams = '';
  for (i = 0; i < params.length; i++)
  {
    requestParams += '&' + this.managedBean + '.' + params[i]; 
  }
  return requestParams;
}

AjaxRequest.prototype.buildResponseParams = function(params)
{
  var responseParams = '';
  if (params != null)
  {
    for (i = 0; i < params.length; i++)
    {
      responseParams += this.managedBean + '.' + params[i];
      if (i < params.length - 1)
        responseParams += ',';  
    }
  }
  return responseParams;
}

AjaxRequest.prototype.invoke = function(reqParams, respParams)
{
  var beforeRequest = new Date().getTime();
  var requestUrl = this.requestUrl;
  if (!requestUrl)
    requestUrl = '../faces/' + (this.responseType == 'json' ? 'ajax-request' : 'ajax-xml-request') + '?values=' + this.buildResponseParams(respParams) + this.buildRequestParams(reqParams);
  var deferred = dojo.xhrGet(
  {
    url: requestUrl, 
    handleAs: this.responseType,
    preventCache: this.preventCache,
    timeout: 30000, // milliseconds
    
    // load() will be called on a success response
    load: function(responseObj, ioArgs)
    {
      var afterRequest = new Date().getTime();
      console.log(requestUrl + '-> requestTime: ' + (afterRequest - beforeRequest));
      return responseObj;
    },
    // error() will be called on an error response
    error: function(responseXML, ioArgs)
    {
      console.error(requestUrl + "-> HTTP status code: ", ioArgs.xhr.status);
      return responseObj;
    }
  });
  if (typeof this.callback != "undefined" && this.callback)
    deferred.addCallback(this.callback);
}

AjaxRequest.prototype.invokePost = function(reqParamName, reqContent, respParams)
{
  var contentForm = document.createElement('form');
  contentForm.setAttribute('name', 'contentForm');
  var contentInput = document.createElement('input');
  contentInput.setAttribute('type', 'hidden');
  contentInput.setAttribute('name', this.managedBean + '.' + reqParamName);
  contentInput.setAttribute('value', reqContent);
  contentForm.appendChild(contentInput);
  
  var beforeRequest = new Date().getTime();
  var requestUrl = this.requestUrl;
  if (!requestUrl)
    requestUrl = '../faces/ajax-request?values=' + this.buildResponseParams(respParams);
  var deferred = dojo.xhrPost(
  {
    url: requestUrl,
    form: contentForm,
    handleAs: this.responseType,
    timeout: 30000, // milliseconds
    
    // load() will be called on a success response
    load: function(responseObj, ioArgs)
    {
      var afterRequest = new Date().getTime();
      console.log(requestUrl + '-> requestTime: ' + (afterRequest - beforeRequest));
      return responseObj;
    },
    // error() will be called on an error response
    error: function(responseXML, ioArgs)
    {
      console.error(requestUrl + "-> HTTP status code: ", ioArgs.xhr.status);
      return responseObj;
    }
  });
  if (typeof this.callback != "undefined" && this.callback)
    deferred.addCallback(this.callback);
}

