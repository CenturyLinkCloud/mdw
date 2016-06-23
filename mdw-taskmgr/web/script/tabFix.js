

function fixTabbedPage(pageId)
{
  var tabForm = document.getElementById(pageId + ':_form');
  if (tabForm != null)
  {
    if (tabForm.children[1].children[0].children[0])
      tabForm.children[1].children[0].children[0].children[0].style['display'] = 'none';
    else
      tabForm.children[0].children[0].children[0].children[0].style['display'] = 'none';
  }
}