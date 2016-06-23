// render a customized dropdown list that conforms to our size requirements
function dropdown(selObj, sequenceId)
{
  expDropdown = findDocElement(sequenceId + ':expandableSelectDropdown');
  expDropdown.style['left'] = '25px';
  expDropdown.style['visibility'] = 'visible';
  expDropdown.focus();
}

// collapse our customized dropdown
function collapse(selObj, sequenceId)
{
  selObj.style['visibility'] = 'hidden';
  selectItem(selObj, sequenceId);
}

// handle the selection of an option in dropdown
function selectItem(selObj, sequenceId)
{
  cSel = findDocElement(sequenceId + ':expandableSelect');
  if (selObj.selectedIndex >= 0)
  {
    cSel[0].value = selObj.options[selObj.selectedIndex].value;
    cSel[0].text = selObj.options[selObj.selectedIndex].text;
    cSel.selectedIndex = 0;
    cSel.focus();
  }
}

// provide handling of keystrokes to ensure 508 compliance
function key(evt, sequenceId)
{
  // handle ie and ff
  var kc;
  evt = evt ? evt : window.event;

  if (evt.keyCode)
    kc = evt.keyCode;
  else if
    (evt.which) kc = evt.which;

    dropdownSize = 10;
    cDrop = findDocElement(sequenceId + ':expandableSelectDropdown');

    if (kc == 38) // up arrow
    {
      if (cDrop.selectedIndex > 0)
        cDrop.selectedIndex -= 1;
    }
    else if (kc == 40) // down arrow
    {
      if (cDrop.selectedIndex < cDrop.options.length - 1)
        cDrop.selectedIndex += 1;
    }
    else if (kc == 36) // home
    {
      cDrop.selectedIndex = 0;
    }
    else if (kc == 35) // end
    {
      cDrop.selectedIndex = cDrop.length - 1;
    }
    else if (kc == 33) // page up
    {
      if (cDrop.selectedIndex - dropdownSize < 0)
        cDrop.selectedIndex = 0;
      else
        cDrop.selectedIndex -= dropdownSize;
    }
    else if (kc == 34) // page down
    {
      if (cDrop.selectedIndex + dropdownSize > cDrop.length - 1)
        cDrop.selectedIndex = cDrop.length - 1;
      else
        cDrop.selectedIndex += dropdownSize;
    }
  selectItem(cDrop, sequenceId);
}

function dropdownKey(evt, sequenceId)
{
  // handle ie and ff
  var kc;
  evt = evt ? evt : window.event;

  if (evt.keyCode)
    kc = evt.keyCode;
  else if
    (evt.which) kc = evt.which;

  if (kc == 13 || kc == 27)  // enter or esc
  {
    collapse(findDocElement(sequenceId + ':expandableSelectDropdown'), sequenceId);
  }
}

function findDocElement(name)
{
  forms = document.forms;
  for (i = 0; i < forms.length; i++)
  {
    elements = forms[i].elements;
    for (j = 0; j < elements.length; j++)
    {
      if (elements[j].name.indexOf(name) >= 0)
      {
        return elements[j];
      }
    }
  }
  return null;
}