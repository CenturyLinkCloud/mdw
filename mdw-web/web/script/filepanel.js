function initFilePanel()
{
  scrollInProgress = false;
  fileViewResponseParams = ['view', 'lineIndex', 'lineCount', 'fileInfo'];
 
  displayFileView();
  calcPixelsPerLine();
  
  var fileView = dojo.byId('fileView');
  totalLines = parseInt(fileView.getAttribute('totalLines'));
  lineIndex = parseInt(fileView.getAttribute('lineIndex'));

  // preferences
  bufferLines = parseInt(fileView.getAttribute('bufferLines'));
  refetchThreshold = parseInt(fileView.getAttribute('refetchThreshold'));
  tailInterval = parseInt(fileView.getAttribute('tailInterval'));
  fileViewFontSize = fileView.getAttribute('fileViewFontSize');
  sliderIncrementLines = parseInt(fileView.getAttribute('sliderIncrementLines'))/2;
  setSliderIncrements();
  
  standAloneMode = fileView.getAttribute('standAloneMode') == 'true';
  enableStackDump(fileView.getAttribute('systemAdminUser') == 'true');
  enableMemoryInfo(fileView.getAttribute('systemAdminUser') == 'true');

  bufferFirstLine = getBufferFirstLine();
  bufferLastLine = getBufferLastLine();
    
  fillLineNumbers();
  
  dojo.byId('tailModeCheckBox').checked = false;
  
  connectEventHandlers();  
  
  scrollTop();  
  
  if (dojo.isIE)
  {
    fixLayoutForIE();
  }
  
  dojo.byId('fileInfo').innerHTML = '';
  enableRefresh(false);
  enableDownload(false);  
  enableGrep(false);
  enableEdit(false);
  dijit.byId('mainLayoutContainer').resize();  
}

dojo.addOnLoad(initFilePanel);

function connectEventHandlers()
{
  var fileView = dojo.byId('fileView');
  // mouse wheel
  if (dojo.isFF)
    dojo.connect(fileView, 'DOMMouseScroll', fileViewMouseWheeled);
  else
    dojo.connect(fileView, 'onmousewheel', fileViewMouseWheeled);
  // keyboard
  dojo.connect(fileView, 'onkeypress', fileViewKeyPressed);  // TODO: doesn't work in Chrome  
  
  // slider mousemove
  dojo.connect(dojo.byId('verticalSliderContentPane'), 'onmousemove', verticalSliderMouseOver);
  // scrollBottom image
  dojo.connect(dojo.byId('scrollBottomImage'), 'onmousemove', scrollBottomMouseOver);
  // fileView resize
  if (dojo.isIE)
  {
    fvvsResize = dojo.connect(fileView, 'onresize', fileViewVerticalSliderResize);
    fileViewVerticalSliderResize();
  }
  // search textbox keypress
  dojo.connect(dojo.byId('searchTextBox'), 'onkeypress', searchTextBoxKeyPressed);
}

function scrollVertical(units, bottom)
{
  // calculate new line index
  lineIndex = Math.round((units/dijit.byId('fileViewVerticalSlider').maximum) * (totalLines - getClientLines()));
  if (lineIndex <= 0)
    lineIndex = 0;
  else if (lineIndex > totalLines - 1)
    lineIndex = totalLines - 1;
  
  // check whether we're approaching buffer refetch threshold
  if ( (bufferFirstLine > 0 && lineIndex - refetchThreshold < bufferFirstLine) 
         || (bufferLastLine < totalLines - 1 && lineIndex + refetchThreshold > bufferLastLine) )
  {
    if (!scrollInProgress)
    {
      scrollInProgress = true;
      var ajaxRequest = new AjaxRequest('fileView');
      ajaxRequest.setCallback(
        function(responseObj)
        {
        
          var pendingLineIndex = lineIndex;        
          processAjaxResponse(responseObj);

          scrollInProgress = false;  // allow scrolling to resume
          var vs = dijit.byId("fileViewVerticalSlider");
          if (pendingLineIndex != lineIndex)
          {
            // catch up to asynchronous user scrolling
            lineIndex = pendingLineIndex;
            scrollVertical((vs.maximum * lineIndex) / (totalLines - getClientLines()));            
            //scrollVertical(vs.maximum - vs.getValue());
            findTop();
          }
          if (bottom)
            dojo.byId('lineInfo').innerHTML = totalLines + " / " + totalLines;
                      
          return responseObj;
        });      
      ajaxRequest.invoke(['lineIndex=' + lineIndex], fileViewResponseParams);
    }
    
    // if not completely out of buffer
    if (!((bufferFirstLine > 0 && lineIndex < bufferFirstLine)
          || (bufferLastLine < totalLines - 1 && lineIndex > bufferLastLine)))
    {
      findTop();  // keep scrolling during asynchronous retrieve
    }
  }
  else
  {
    findTop();
  }
  resetSearchIndex();  
}

function scrollBottom()
{
  scrollVertical(dijit.byId('fileViewVerticalSlider').maximum, true);
  dijit.byId('fileViewVerticalSlider').setValue(0);
  dojo.byId('lineInfo').innerHTML = totalLines + " / " + totalLines;
}

function scrollTop()
{
  scrollVertical(0);
  dijit.byId('fileViewVerticalSlider').setValue(dijit.byId('fileViewVerticalSlider').maximum);
  dojo.byId('lineInfo').innerHTML = (totalLines == 0 ? '0' : '1') + ' / ' + totalLines;
}

function scrollHorizontal(percent)
{
  var fileView = dojo.byId('fileView');
  var pre = fileView.firstChild;
  if (dojo.isIE == 6)
    setScrollLeft((percent/dijit.byId("fileViewHorizontalSlider").maximum) * (fileView.scrollWidth - fileView.clientWidth + 20));
  else
    setScrollLeft((percent/dijit.byId("fileViewHorizontalSlider").maximum) * (pre.scrollWidth - pre.clientWidth + 20));
}

function findTop()
{
  var fileView = dojo.byId('fileView');
  var scrollPos = (lineIndex - bufferFirstLine) * pxPerLine + 4;
  // don't scroll past the top of the last page
  if (scrollPos > fileView.scrollHeight - fileView.clientHeight)
  {
    scrollPos = fileView.scrollHeight - fileView.clientHeight;
  }
  fileView.scrollTop = scrollPos;
  dojo.byId('lineNumbersContentPane').scrollTop = scrollPos;
  dojo.byId('lineInfo').innerHTML = (totalLines == 0 ? 0 : lineIndex + 1) + " / " + totalLines;
  resetSearchIndex();
}

function processAjaxResponse(responseObj)
{
  lineIndex = parseInt(responseObj.fileView_lineIndex);

  var newTotalLines = parseInt(responseObj.fileView_lineCount);
  if (newTotalLines != totalLines)
  {
    totalLines = newTotalLines;
    setSliderIncrements();
  }
  
  // since we just fetched, recalculate buffer span based on line index
  bufferFirstLine = getBufferFirstLine();
  bufferLastLine = getBufferLastLine();
  setBuffer(responseObj.fileView_view);
  fillLineNumbers();
  findTop();
  scrollChar(0);

  return responseObj;   
}

function setBuffer(buffer)
{
  dojo.byId('fileView').innerHTML = "<pre class='fileView' style='font-size:" + fileViewFontSize + ";'>\n" + buffer + "\n</pre>";
}
function getBuffer()
{
  return dojo.byId('fileView').firstChild.innerHTML.replace(/<span.*?>/i, '').replace(/<\/span>/i, '');
}

// same logic as FileView method (assumes buffer span is in sync with lineIndex)
function getBufferFirstLine()
{
  var firstLine = lineIndex - bufferLines/2;
  if (lineIndex + bufferLines/2 > totalLines - 1)
    firstLine = totalLines - bufferLines - 1;  
  if (firstLine < 0)
    firstLine = 0;
  
  return firstLine;
}
// same logic as FileView method (assumes buffer span is in sync with lineIndex)
function getBufferLastLine()
{
  var lastLine = getBufferFirstLine() + bufferLines;
  if (lastLine > totalLines - 1)
    lastLine = totalLines - 1;
  if (lastLine < 0)
    lastLine = 0;
  
  return lastLine;
}

function fillLineNumbers()
{
  var lineNumbers = new StringBuffer();
  if (totalLines != 0)
  {
    var paddedLen = ('' + totalLines).length;
    for (i = bufferFirstLine + 1; i <= bufferLastLine + 1; i++)
    {
      // pad numbers to ensure constant width
      var lineNo = '' + i;
      for (j = lineNo.length; j < paddedLen; j++)
        lineNo += ' ';
      lineNumbers.append(lineNo).append("\n");
    }
  }

  dojo.byId("lineNumbersContentPane").innerHTML = "<pre class='lineNumbers' style='font-size:" + fileViewFontSize + ";'>" 
      + lineNumbers.toString() + "\n\n<br style='height:24px;'/></pre>";
}

// assumes index falls within buffer
function scrollLine(index)
{
  lineIndex = index;
  findTop();
  var vs = dijit.byId('fileViewVerticalSlider');
  var fraction = lineIndex / (totalLines - getClientLines());
  if (fraction > 1)
    fraction = 1;
  vs.progressBar.style[vs._progressPixelSize] = (1-fraction)*100 + "%";
  vs.remainingBar.style[vs._progressPixelSize] = (fraction)*100 + "%";
  vs.value = (totalLines - getClientLines()) - lineIndex;
}

function scrollChar(charIndex)
{
  setScrollLeft(charIndex*pxPerChar, true);
}

function setScrollLeft(pixels, adjustSlider)
{
  var fileView = dojo.byId('fileView');
  var pre = fileView.firstChild;
  
  pre.scrollLeft = pixels; 
  if (dojo.isIE == 6)
    fileView.scrollLeft = pixels;
  
  if (adjustSlider)
  {
    var hs = dijit.byId('fileViewHorizontalSlider');
    var fraction = pre.scrollLeft/(pre.scrollWidth - pre.clientWidth + 20);
    if (dojo.isIE == 6)
      fraction = fileView.scrollLeft/(fileView.scrollWidth - fileView.clientWidth);
    if (fraction > 1)
      fraction = 1;
    hs.progressBar.style[hs._progressPixelSize] = fraction*100 + "%";
    hs.remainingBar.style[hs._progressPixelSize] = (1-fraction)*100 + "%";
  } 
}

function fileViewMouseWheeled(event)
{
  dijit.byId('fileViewVerticalSlider')._mouseWheeled(event);
}

function fileViewKeyPressed(event)
{
  if (editMode)
    return;
  
  // HOME and END keys are backward
  if (event.keyCode == dojo.keys.HOME)
  {
    dijit.byId("fileViewVerticalSlider").setValue(dijit.byId(fileViewVerticalSlider).maximum);
    return;
  }
  else if (event.keyCode == dojo.keys.END)
  {
    dijit.byId("fileViewVerticalSlider").setValue(0);
    return;
  }
  else if (event.keyCode == dojo.keys.PAGE_DOWN)
  {
    var vs = dijit.byId('fileViewVerticalSlider');
    // one page worth of slider increments
    var newVal = vs.getValue() - 2*getClientLines()/sliderIncrementLines;
    if (newVal < 0)
      newVal = 0;
    vs.setValue(newVal);
  }
  else if (event.keyCode == dojo.keys.PAGE_UP)
  {
    var vs = dijit.byId('fileViewVerticalSlider');
    // one page worth of slider increments
    var newVal = vs.getValue() + 2*getClientLines()/sliderIncrementLines;
    if (newVal > vs.maximum)
      newVal = vs.maximum;
    vs.setValue(newVal);
  } 
  else if (event.keyCode != dojo.keys.RIGHT_ARROW && event.keyCode != dojo.keys.LEFT_ARROW)
  {
    dijit.byId('fileViewVerticalSlider')._onKeyPress(event);
  }
}

function verticalSliderMouseOver(event)
{
  // emulate dojo slider algorithm for predicting line number
  var vs = dijit.byId('fileViewVerticalSlider');
  var abspos = dojo.coords(vs.sliderBarContainer, true);
  var pixelValue = abspos[vs._pixelCount] - (event[vs._mousePixelCoord] - abspos[vs._startingPixelCoord]);
  var maxPixels = abspos[vs._pixelCount];  
  var count = vs.discreteValues - 1;
  var pixelsPerValue = maxPixels / count;
  var wholeIncrements = Math.round(pixelValue / pixelsPerValue);
  var value = (vs.maximum - vs.minimum) * wholeIncrements/count + vs.minimum;
  var units = vs.maximum - value;
  var lineNo = Math.round((units/vs.maximum) * (totalLines - getClientLines())) + 1;
  if (lineNo < 1 || lineNo > totalLines)
    lineNo = '';
  dojo.byId('verticalSliderContentPane').title = lineNo;      
}

function scrollBottomMouseOver()
{
  dojo.byId('scrollBottomImage').title = totalLines;
}

function fileViewVerticalSliderResize(event)
{
  var vs = dojo.byId('fileViewVerticalSlider');
  vs.style.display = 'none';
  vs.style.height = (dojo.byId('fileView').clientHeight - 50) + 'px';
  vs.style.display = 'block';
}

function searchTextBoxKeyPressed(event)
{
  // ENTER key performs forward search
  if (event.keyCode == dojo.keys.ENTER)
  {
    search(true);
    return;
  }
}

function filePanelDirTreeSelectX(message)
{
  if (message.event == 'execute' && message.item)
  {
    var path = escape(message.item.path);
    suspendTail();
    filepath = path;
    enableGrep(true);
    enableEdit(message.item.editable);
    var ajaxRequest = new AjaxRequest('fileView');
    if (message.item.type == 'file')
    {
      // file selected
      enableRefresh(true);
      enableDownload(!message.item.maskable);      
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
          return responseObj;
        });
      
      ajaxRequest.invoke(['filePath=' + path, 'lineIndex=0', 'bufferLines=' + bufferLines], fileViewResponseParams);
    }
    else
    {
      // dir selected
      enableRefresh(false);
      enableDownload(false);      
      ajaxRequest.setCallback(
        function(responseObj)
        {
          processAjaxResponse(responseObj);
          dojo.byId('fileInfo').innerHTML = path + '<br/>' + responseObj.fileView_fileInfo;
          dojo.byId('grepDirectory').innerHTML = path;
          dijit.byId('mainLayoutContainer').resize();
          return responseObj;
        });
      ajaxRequest.invoke(['filePath=' + path], fileViewResponseParams);
    }
  }
  setSearchMessage('');
  setToolsMessage('');
}

function enableRefresh(enabled)
{
  var refreshButton = dijit.byId('refreshButton');
  refreshButton.setDisabled(!enabled);
  refreshButton.focusNode.disabled = false;
  dojo.byId('refreshButton').title = enabled ? 'Refresh' : 'Select a file first';
}

function refresh()
{
  var ajaxRequest = new AjaxRequest('fileView');
  ajaxRequest.setCallback(
    function(responseObj)
    {
      processAjaxResponse(responseObj);
      scrollTop();
      return responseObj;
    });
  ajaxRequest.invoke(['bufferLines=' + bufferLines], fileViewResponseParams);
}

function enableDownload(enabled)
{
  var downloadButton = dijit.byId('downloadButton');
  downloadButton.setDisabled(!enabled);
  downloadButton.focusNode.disabled = false;
  dojo.byId('downloadButton').title = enabled ? 'Download' : 'Select a downloadable file first';
}

function download()
{
  if (filepath && filepath != null)
    location.href = '../download?filepath=' + filepath;
}

// returns the number of lines in client height 
function getClientLines()
{
  return Math.round(dojo.byId('fileView').clientHeight/pxPerLine);
}

// returns the number of characters in client width
function getClientChars()
{
  if (dojo.isIE == 6)
    return Math.round(dojo.byId('fileView').clientWidth/pxPerChar);
  else
    return Math.round(dojo.byId('fileView').firstChild.clientWidth/pxPerChar);
}

function calcPixelsPerLine()
{
  var fileView = dojo.byId('fileView');
  pxPerLine = 16; // default for 10pt
  pxPerChar = 8;
  if (fileView.firstChild.style.fontSize == '8pt')
  {
    pxPerLine = 14;
    pxPerChar = 7;
  }
  else if (fileView.firstChild.style.fontSize == '9pt')
  {
    pxPerLine = 15;
    pxPerChar = 7;
  }
  else if (fileView.firstChild.style.fontSize == '12pt')
  {
    pxPerLine = 18;
    pxPerChar = 10;
  }
}

function fixLayoutForIE()
{
  dojo.byId('optionsDropDown').style.marginTop = '2px';
  dojo.byId('searchLabel').style.top = "-2px";
  dojo.byId('searchTextBox').size = '20';
  dojo.byId('forwardSearchButton').style.height = '16px';
  dojo.byId('forwardSearchButton').style.width = '60px';
  dojo.byId('backwardSearchButton').style.height = '16px';
  dojo.byId('backwardSearchButton').style.width = '70px';
  dojo.byId('refreshButton').style.height = '20px';
  dojo.byId('downloadButton').style.height = '20px';
  dojo.byId('editButton').style.height = '20px';
  dojo.byId('stackDumpButton').style.height = '20px';
  dojo.byId('memoryInfoButton').style.height = '20px';
  dojo.byId('grepButton').style.height = '16px';
}

function enableStackDump(enabled)
{
  var stackDumpButton = dijit.byId('stackDumpButton');
  stackDumpButton.setDisabled(!enabled);
  stackDumpButton.focusNode.disabled = false;
  dojo.byId('stackDumpButton').title = enabled ? 'Thread Dump' : 'Not permitted';
}

function enableMemoryInfo(enabled)
{
  var memoryInfoButton = dijit.byId('memoryInfoButton');
  memoryInfoButton.setDisabled(!enabled);
  memoryInfoButton.focusNode.disabled = false;
  dojo.byId('memoryInfoButton').title = enabled ? 'Memory Info' : 'Not permitted';
}
function removeContainerChild(parent, child)
{
  if (!(child == undefined))
    parent.removeChild(child);
}