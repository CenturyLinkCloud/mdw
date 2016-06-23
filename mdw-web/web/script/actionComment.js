// list action javascript
function showCommentDialog(taskAction, actionItem)
{
  var hid = document.getElementById('filteredListForm:submitCommentsHiddenAction');
  if (hid == null)
    hid = document.getElementById('mainDetailForm:actionForm:submitCommentsHiddenAction');
  hid.value = taskAction;

  var hidIt = document.getElementById('filteredListForm:submitCommentsHiddenItem');
  if (hidIt == null)
    hidIt = document.getElementById('mainDetailForm:actionForm:submitCommentsHiddenItem');
  hidIt.value = actionItem;
  
  var dlgObj = document.getElementById('filteredListForm:commentDlg');
  if (dlgObj == null)
    dlgObj = document.getElementById('mainDetailForm:actionForm:commentDlg');
  RichFaces.$(dlgObj.id).show();
}

function submitComments()
{
  var com = document.getElementById('filteredListForm:submitCommentsHiddenComment');
  if (com == null)
    com = document.getElementById('mainDetailForm:actionForm:submitCommentsHiddenComment');
  
  var comSrc = document.getElementById('filteredListForm:taskActionComments');
  if (comSrc == null)
    comSrc = document.getElementById('mainDetailForm:actionForm:taskActionComments');
  
  com.value = comSrc.value;
  
  var btn = document.getElementById('filteredListForm:submitCommentsHiddenButton');
  if (btn == null)
    btn = document.getElementById('mainDetailForm:actionForm:submitCommentsHiddenButton');
  
  btn.click();
}
