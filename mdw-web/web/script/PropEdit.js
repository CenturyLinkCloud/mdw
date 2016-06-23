propEditMode = false;
editPropObj = null;

function editProp(propItem)
{
  editPropObj = new PropEdit(propItem);
  editPropObj.setEditMode(propItem.editable);
}

function PropEdit(propItem)
{
  this.propItem = propItem;
}
PropEdit.prototype.setEditMode = function(isEditMode)
{
  propEditMode = isEditMode;
  if (propEditMode)
  {
    if (!(window.fvvsResize == undefined))
    {
      dojo.disconnect(fvvsResize);
    }
    displayEditBar(this.save, this.cancel);
    this.displayPropEdit();
  }
  else
  {
    displayBlankBar();
    this.displayPropEdit();
  }
}
PropEdit.prototype.save = function()
{
  dojo.byId('reactToChange').value = dojo.byId('reactCheckBox').checked;
  dojo.byId('applyChangeGlobally').value = dojo.byId('globalCheckBox').checked;
  dojo.byId('savePropBtn').click();
}
PropEdit.prototype.cancel = function()
{
  editPropObj.displayPropEdit();
}
PropEdit.prototype.displayPropEdit = function()
{
  dojo.xhrGet(
  {
    preventCache: true,
    url: '../property/propEdit.jsf?proppath=' + this.propItem.path, 
    handleAs: "text",
    timeout: 5000, // milliseconds
    
    // load() will be called on a success response
    load: function(response, ioArgs)
    {
      var fileViewContainer = dojo.byId('fileViewLayoutContainer');
      removeContainerChild(fileViewContainer, dojo.byId('fileView'));
      removeContainerChild(fileViewContainer, dojo.byId('verticalSliderLayoutContainer'));
      removeContainerChild(fileViewContainer, dojo.byId('lineNumbersContentPane'));
      removeContainerChild(fileViewContainer, dojo.byId('horizontalSliderContentPane'));
      fileViewContainer.innerHTML = response;
      return response;
    },
    // error() will be called on an error response
    error: function(response, ioArgs)
    {
      console.error("HTTP status code: ", ioArgs.xhr.status);
      return response;
    }
  });
}