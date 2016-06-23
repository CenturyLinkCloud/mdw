function drillDownPopup(report, param)
{
  var msgDialog = $("#drillDownPopup").dialog(
  {
    dialogClass : 'mdw-dialog',
    modal : true,
    title : report,
    buttons :
    {

      Ok : function()
      {

        $(this).dialog("close");

      }

    }

  });
  msgDialog.html("<table><tr><td>Hi" + param + "</td></tr></table>");
  msgDialog.dialog("open");

}
function resizeIframe(obj)
{
  var newheight = obj.contentWindow.document.body.scrollHeight + 30;
  var newWidth = obj.contentWindow.document.body.scrollWidth + 30;
  obj.style.height = newheight + 'px';
  obj.style.width = newWidth + 'px';
}
$(function()
{

  var iFrames = $('iframe');

  function iResize()
  {

    for (var i = 0, j = iFrames.length; i < j; i++)
    {
      iFrames[i].style.height = iFrames[i].contentWindow.document.body.offsetHeight
          + 'px';
    }
  }

  if ($.browser.safari || $.browser.opera)
  {

    iFrames.load(function()
    {
      setTimeout(iResize, 0);
    });

    for (var i = 0, j = iFrames.length; i < j; i++)
    {
      var iSource = iFrames[i].src;
      iFrames[i].src = '';
      iFrames[i].src = iSource;
    }

  } else
  {
    iFrames.load(function()
    {
      this.style.height = this.contentWindow.document.body.offsetHeight + 'px';
    });
  }

});
