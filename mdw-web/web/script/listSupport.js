// list-related javascript (for list.xhtml)

function selectAll(theForm, checkboxChecked)
{
  for (var i = 0; i < theForm.elements.length; i++)
  {
    if (theForm.elements[i].type == 'checkbox')
    {
      if (checkboxChecked)
      {
        theForm.elements[i].checked = true;
      }
      else
      {
        theForm.elements[i].checked = false;
      }
    }
  }
  if (checkboxChecked)
  {
    return true;
  }
  else
  {
    return false;
  }
}

function listItemLoading(item, imgSrc)
{
  if (imgSrc == null)
    imgSrc = "../images/loading_sm.gif";
  
  item.style.border = "none";
  item.style.outline = "none";

  if (item.firstChild && item.firstChild.src)
  {
    var w = item.firstChild.width;
    var h = item.firstChild.height;
    item.firstChild.style.width = w + "px";
    item.firstChild.style.height = h + "px";
    item.firstChild.src = imgSrc;
  }
  else
  {
    if (item.tagName == "DIV")
    {
      item.innerHTML = '<img class="mdw_listButton" alt="Loading" src="' + imgSrc + '">';
    }
    else if (item.tagName == "INPUT")
    {
      item.style.backgroundImage = "url('../images/loading_sm.gif')";
    }    
    else
    {
      if (item.firstChild && item.firstChild.innerHTML == "All")
      {
        item.innerHTML = '<img class="mdw_listImage" style="width:12px;height:12px;" alt="Loading" src="' + imgSrc + '">';
      }
      else
      {
        item.innerHTML = '<img class="mdw_listImage" alt="Loading" src="' + imgSrc + '">';        
      }
    }
    if (item.tagName == "INPUT")
    {
      setTimeout('updateBackground("' + item.id + '");', 25);
    }
    else
    {
      item.firstChild.id = item.id + "_img";
      setTimeout('updateImage("' + item.firstChild.id + '");', 25);
    }
  }
}

function updateImage(imgId)
{
  var img = document.getElementById(imgId);
  img.src = img.src;  
}

function updateBackground(inputId)
{
  var inp = document.getElementById(inputId);
  inp.style.backgroundImage = inp.style.backgroundImage;
}

function expandListItem(elem)
{
  elem.parentNode.style.verticalAlign = 'top';
  var prefix = elem.id.substring(0, elem.id.length - 'expand'.length - 1);
  var divs = findExpandedContentDivs(prefix);
  for (var i = 0; i < divs.length; i++)
  {
    divs[i].style.display = 'block';
  }
  elem.style.display = 'none';
  document.getElementById(prefix + ':collapse').style.display = 'inline';
}

function collapseListItem(elem)
{
  elem.parentNode.style.verticalAlign = 'middle';
  var prefix = elem.id.substring(0, elem.id.length - 'collapse'.length - 1);
  var divs = findExpandedContentDivs(prefix);
  for (var i = 0; i < divs.length; i++)
  {
    divs[i].style.display = 'none';
  }
  elem.style.display = 'none';
  document.getElementById(prefix + ':expand').style.display = 'inline';
}

function findExpandedContentDivs(prefix)
{
  var found = new Array();
  var divs = document.getElementsByTagName("DIV");
  var start = prefix.substring(0, prefix.lastIndexOf(':'));
  start = start.substring(0, start.lastIndexOf(':'));
  var end = 'expandedContent';
  for (var i = 0; i < divs.length; i++)
  {
    var divId = divs[i].id;
    if (divId.indexOf(start) == 0 && divId.indexOf(end, divId.length - end.length) !== -1)
      found.push(divs[i]);
  }
    
  return found;
}
function displayListPrefsPopup()
{
  RichFaces.$('listPrefsModalPanel').show();
} 
function hideListPrefsPopup()
{
  RichFaces.$('listPrefsModalPanel').hide();
}

function showFilterOption(show, elem)
{
  var imgs = elem.getElementsByTagName("img");
  if (imgs != null)
  {
    for (var i = 0; i < imgs.length; i++)
    {
      if (imgs[i].src != null && endsWith(imgs[i].src, 'images/filter.gif'))
      {
        imgs[i].style.visibility = (show ? 'visible' : 'hidden');
        return;
      }
    }
  }
}

function showFilterInput(elem)
{
  var inputId = elem.id.substring(0, elem.id.indexOf('filterBtn')) + 'filterInput';
  var input = document.getElementById(inputId);
  if (input != null)
  {
    input.style.display = 'inline';
    input.style.opacity = '1';
    if (focus)
    {
      if (endsWith(input.firstElementChild.id, 'dateRangeFilterHeader'))
        input.firstElementChild.firstElementChild.firstElementChild.firstElementChild.firstElementChild.focus();
      else
        input.firstElementChild.focus();
    }
  }
}

function hideFilterInput(elem)
{
  // make invisible immediately
  elem.parentElement.style.opacity = '0';
  // delay removing so that onclick can trigger submit
  setTimeout(function()
    {
      elem.parentElement.style.display = 'none';
    }, 500);
}

function handleDateRangeInputHide(elem)
{
  var prefix = elem.id.substring(0, elem.id.lastIndexOf(':'));
  setTimeout(function()
    {
      // console.log('document.activeElement.id: ' + document.activeElement.id);
      var doHide = true;
      if (document.activeElement && document.activeElement != null && document.activeElement.id)
      {
        var activeId = document.activeElement.id;
        if (activeId && activeId != null)
        {
          var lastColon = activeId.lastIndexOf(':');
          doHide = lastColon < 0 || prefix !== activeId.substring(0, lastColon); 
        }
      }
      if (doHide)
        hideFilterInput(elem.parentElement.parentElement.parentElement.parentElement);
    }, 100);
}

function showSearchInput(elem)
{
  var searchInputId = elem.id.substring(0, elem.id.indexOf('listSearchButton')) + 'listSearchInput';
  var searchInput = document.getElementById(searchInputId);
  if (searchInput != null)
  {
    searchInput.parentElement.style.visibility = 'visible';
    searchInput.focus();
  }
  var applyBtnId = elem.id.substring(0, elem.id.indexOf('listSearchButton')) + 'listSearchApplyButton';
  var applyBtn = document.getElementById(applyBtnId);
  if (applyBtn != null)
    applyBtn.parentElement.style.visibility = 'visible';
}

function submitOnEnter(evt, form)
{
  if (evt.keyCode == 13)
  {
    if (evt.preventDefault)
      evt.preventDefault();
    form.submit();
  }
}
function endsWith(str, suffix)
{
  return str.indexOf(suffix, str.length - suffix.length) !== -1;
}