searchIndex = -1;

function search(forward)
{
  if (tailMode)
    tail(false);
  
  var searchObj = new Search();
  searchObj.forward = forward;
  searchObj.ignoreCase = !dojo.byId('matchCaseCheckBox').checked;
  searchObj.regExp = dojo.byId('regExpCheckBox').checked;
  searchObj.find(dojo.byId('searchTextBox').value);
}

function Search()
{
  this.forward = true;
  this.ignoreCase = true;
  this.regExp = false;
}

Search.prototype.find = function(toFind)
{
  setSearchMessage('');
  
  var searchExpr = this.ignoreCase ? toFind.toLowerCase() : toFind;
  if (searchExpr.length == 0)
  {
    this.clearResults();
    return;
  }
  
  if (searchIndex == 0)
    searchIndex = this.getSearchIndex();

  var charIndex = this.searchLocal(xmlEscape(searchExpr), true);
  if (charIndex == -1)
  {
    this.searchRemote(searchExpr);  
  }
}

Search.prototype.searchLocal = function(searchExpr)  
{
  var searchBuf = this.ignoreCase ? getBuffer().toLowerCase() : getBuffer();
  var charIndex = -1;

  if (this.forward)
  {
    if (this.regExp)
    {
      var matches = searchExpr = searchBuf.match(eval('/' + searchExpr.replace(/\//g, ".") + '/'))
      if (!matches)
      {
        this.notFound();
        return;
      }
      searchExpr = matches[0];
    }     
    charIndex = searchBuf.indexOf(searchExpr, searchIndex);
  }
  else
  {
    if (this.regExp)
    {
      var matches = searchBuf.substring(0, searchIndex - 1).match(eval('/' + searchExpr + '/g'));
      if (!matches)
      {
        this.notFound();
        return;
      }
      searchExpr = matches[matches.length-1];
    }    
    charIndex = searchBuf.substring(0, searchIndex - 1).lastIndexOf(searchExpr);
  }
  
  if (charIndex != -1)
  {
    this.highlight(charIndex, searchExpr.length);
    this.scrollIntoView(charIndex, searchExpr.length);
    searchIndex = charIndex + 1;
  }
  return charIndex;
}

Search.prototype.searchRemote = function(searchExpr)
{
  var ajaxRequest = new AjaxRequest('fileView');
  ajaxRequest.setCallback(
    dojo.hitch(this,
      function(responseObj)
      {
        lineIndex = parseInt(responseObj.fileView_lineIndex);
        if (lineIndex != -1)
        {
          var searchWrapped = responseObj.fileView_searchWrapped;
          if (searchWrapped == 'true')
            setSearchMessage('Search has wrapped.');
            
          // update the buffer with the response
          var newTotalLines = parseInt(responseObj.fileView_lineCount);
          if (newTotalLines != totalLines)
          {
            totalLines = newTotalLines;
            setSliderIncrements();
          }
          bufferFirstLine = getBufferFirstLine();
          bufferLastLine = getBufferLastLine();
          setBuffer(responseObj.fileView_searchView); 
          fillLineNumbers();
          findTop();          
          // if searching forward, scroll so that match comes into view near client bottom
          var clientLines = getClientLines();
          if (this.forward && lineIndex + clientLines < totalLines && lineIndex - clientLines > 0)
            scrollLine(lineIndex - clientLines + 2);
          searchIndex = this.getSearchIndex();
          this.searchLocal(xmlEscape(searchExpr));
          dijit.byId('mainLayoutContainer').resize();        
        }
        else
        {
          this.notFound();
        }
        return responseObj;
      }));
    var searchStart = this.forward ? (bufferLastLine + 1) : (bufferFirstLine - 1);
    var reqParams = ['lineIndex=' + searchStart, 'filePath=' + filepath, 'searchExpression=' + escape(searchExpr), 'forwardSearch=' + this.forward, 'ignoreCaseSearch=' + this.ignoreCase, 'regularExpressionSearch=' + this.regExp];
    var respParams = ['searchView', 'lineIndex', 'lineCount', 'searchWrapped'];    
    ajaxRequest.invoke(reqParams, respParams);
}

Search.prototype.notFound = function()
{
  this.clearResults();
  setSearchMessage('Search expression not found.');
}

Search.prototype.clearResults = function()
{
  setBuffer(getBuffer()); // clean the buffer
  resetSearchIndex();
}

Search.prototype.highlight = function(start, length)
{
  var buf = getBuffer();
  setBuffer(buf.substring(0, start) + "<span class='highlighted'>" + buf.substr(start, length) + "</span>" + buf.substring(start + length));
}

// does not change buffer contents
// start is char index relative to buffer start
Search.prototype.scrollIntoView = function(start, length)
{
  var fileView = dojo.byId('fileView'); 
  var buf = getBuffer();
  var charLineIndex = -1;  // index of line relative to buffer start
  var lineCharIndex = 0;  // index of first char within line
  var charCount = 0;
  while (charCount < start + 1 && charLineIndex < bufferLines - 1)
  {
    var charsInLine = buf.indexOf('\n', charCount) - charCount + 1;  // incl newline
    lineCharIndex = start - charCount;
    charCount += charsInLine;
    charLineIndex++;
  }
  
  var clientLines = getClientLines();
  if (charLineIndex > fileView.scrollTop/pxPerLine + clientLines - 2)
    scrollLine(charLineIndex + bufferFirstLine - clientLines + 3);
  else if (charLineIndex < fileView.scrollTop/pxPerLine)
    scrollLine(charLineIndex + bufferFirstLine);
  
  var clientChars = getClientChars();
  if (lineCharIndex + length > clientChars + fileView.firstChild.scrollLeft/pxPerChar)
    scrollChar(lineCharIndex + length + 3 - clientChars + fileView.firstChild.scrollLeft/pxPerChar);
  else if (lineCharIndex < fileView.firstChild.scrollLeft/pxPerChar)
    scrollChar(lineCharIndex - 6);
    
  dojo.byId('lineInfo').innerHTML = (charLineIndex + bufferFirstLine + 1) + " / " + totalLines;
}

Search.prototype.getSearchIndex = function()
{
  // count the characters up till the current line
  var lineCharIndex = -1;
  var buf = getBuffer();
  var lineCount = 0;
  var currentLine = lineIndex - bufferFirstLine;
 
  while (lineCount < currentLine)
  {
    lineCharIndex = buf.indexOf('\n', lineCharIndex + 1);
    lineCount++
  }
  if (this.forward)
  {
    return lineCharIndex;
  }
  else
  {
    var lineRetIdx = buf.indexOf('\n', lineCharIndex);
    if (lineRetIdx == -1)
      return buf.length - 1;
    else
      return lineCharIndex + lineRetIdx;
  }
}

function xmlEscape(toEsc)
{
  return toEsc.replace(/&/, '&amp;').replace(/</, '&lt;').replace(/>/, '&gt;');
}

function resetSearchIndex()
{
  searchIndex = 0;
}

function setSearchMessage(message)
{
  dojo.byId('searchMessage').innerHTML = message;
}
