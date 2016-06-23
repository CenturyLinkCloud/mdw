function hubLoading()
{
  if (navigator.userAgent.indexOf('MSIE') >= 0 || navigator.userAgent.indexOf('Trident') >= 0)
    showHubLoadingIe();
  else
    showHubLoading();
}

function hubLoaded()
{
  if (navigator.userAgent.indexOf('MSIE') >= 0 || navigator.userAgent.indexOf('Trident') >= 0)
    hideHubLoadingIe();
  else
    hideHubLoading();
}

function showHubLoading()
{
  document.getElementById('hub_logo').style.display = 'none';
  document.getElementById('hub_loading').style.display = 'inline';
}

function hideHubLoading()
{
  document.getElementById('hub_loading').style.display = 'none';
  document.getElementById('hub_logo').style.display = 'inline';
}

function showHubLoadingIe()
{
  var logo = document.getElementById('hub_logo');
  logo.src = logo.src.substring(0, logo.src.lastIndexOf('/')) + '/hub_loading.gif';
}

function hideHubLoadingIe()
{
  var logo = document.getElementById('hub_logo');
  logo.src = logo.src.substring(0, logo.src.lastIndexOf('/')) + '/hub_logo.png';
}