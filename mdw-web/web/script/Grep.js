function grep()
{
  if (editMode)
    edit(false);
  enableRefresh(false);
  enableDownload(false);
  var directory = dojo.byId('grepDirectory').innerHTML;
  var grepObj = new Grep(directory);
  var searchPattern = dojo.byId('patternTextBox').value;
  var filePattern = dojo.byId('filesTextBox').value;
  grepObj.find(searchPattern, filePattern); 
}

function showFile(path, foundLineIdx)
{
  selectDirTreeNodeByPath(path);
  enableRefresh(true);
  enableDownload(typeof message !== "undefined" && !message.item.maskable);
  
  var ajaxRequest = new AjaxRequest('fileView');
  ajaxRequest.setCallback(
    dojo.hitch(this,
      function(responseObj)
      {
        lineIndex = parseInt(responseObj.fileView_lineIndex);
        
        // update the buffer with the response
        var newTotalLines = parseInt(responseObj.fileView_lineCount);
        if (newTotalLines != totalLines)
        {
          totalLines = newTotalLines;
          setSliderIncrements();
        }
        bufferFirstLine = getBufferFirstLine();
        bufferLastLine = getBufferLastLine();
        setBuffer(responseObj.fileView_view); 
        fillLineNumbers();
        findTop();
        
        var searchObj = new Search();
        searchObj.regExp = true;
        searchObj.ignoreCase = false;
        searchIndex = searchObj.getSearchIndex();
        var searchPattern = dojo.byId('patternTextBox').value;
        searchObj.searchLocal(xmlEscape(searchPattern));
        dojo.byId('searchTextBox').value = searchPattern;
        dojo.byId('fileInfo').innerHTML = path + '<br/>' + responseObj.fileView_fileInfo;
        dijit.byId('mainLayoutContainer').resize();        
        return responseObj;
      }));
    var reqParams = ['filePath=' + path, 'lineIndex=' + foundLineIdx, 'binary=false', 'maskable=true'];
    var respParams = ['view', 'lineIndex', 'lineCount', 'fileInfo'];    
    ajaxRequest.invoke(reqParams, respParams);
}

function Grep(directory)
{
  this.directory = directory;
}

Grep.prototype.find = function(searchPattern, filePattern)
{
  var ajaxRequest = new AjaxRequest('fileGrep');
  ajaxRequest.setCallback(
    dojo.hitch(this,
      function(responseObj)
      {
        lineIndex = 0;
        // update the buffer with the response
        var newTotalLines = parseInt(responseObj.fileGrep_matchCount);
        if (newTotalLines != totalLines)
        {
          totalLines = newTotalLines;
          setSliderIncrements();
        }
        bufferFirstLine = 0;
        bufferLastLine = totalLines;
        setBuffer(responseObj.fileGrep_htmlView); 
        fillLineNumbers();
        findTop();
        scrollChar(0);
        dijit.byId('mainLayoutContainer').resize();
        document.body.style.cursor = 'default';        
        return responseObj;
      }));
      
  var reqParams = ['directory=' + this.directory, 'filePattern=' + escape(filePattern), 'searchPattern=' + escape(searchPattern), 'clickHandler=showFile'];
  var respParams = ['htmlView', 'matchCount'];
  document.body.style.cursor = 'wait'; 
  ajaxRequest.invoke(reqParams, respParams);
};

function enableGrep(enabled)
{
  var grepButton = dijit.byId('grepButton');
  grepButton.setDisabled(!enabled);
  grepButton.focusNode.disabled = false;
  dojo.byId('grepButton').title = enabled ? '' : 'Choose a directory';
}