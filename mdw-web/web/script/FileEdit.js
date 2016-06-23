filepath = null;
editMode = false;

function edit(isEditable)
{
  var editObj = new FileEdit(filepath);
  editObj.setEditMode(isEditable);
}

function FileEdit(filepath)
{
  this.filepath = filepath;
}
FileEdit.prototype.setEditMode = function(isEditMode)
{
  editMode = isEditMode;
  if (editMode)
  {
    if (!(window.fvvsResize == undefined))
    {
      dojo.disconnect(fvvsResize);
    }
    this.displayFileEdit();
    displayEditBar(this.save, this.cancel);
  }
  else
  {
    displayToolBar();
    displayFileView();
  }
}
FileEdit.prototype.save = function()
{
  if (dojo.byId('reactCheckBox'))
    dojo.byId('reactToChange').value = dojo.byId('reactCheckBox').checked;
  if (dojo.byId('globalCheckBox'))
    dojo.byId('applyChangeGlobally').value = dojo.byId('globalCheckBox').checked;
  dojo.byId('saveFileBtn').click();
}
FileEdit.prototype.cancel = function()
{
  edit(false);
}
FileEdit.prototype.displayFileEdit = function()
{
  dojo.xhrGet(
  {
    preventCache: true,
    url: 'fileEdit.jsf?filepath=' + filepath, 
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
      // size the textarea
      var textarea = dojo.byId('propertyFileTextarea');
      textarea.rows = (fileViewContainer.clientHeight - 10) / 16;
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

function enableEdit(enabled)
{
  var editButton = dijit.byId('editButton');
  if (dojo.byId('editButton'))
  {
    editButton.setDisabled(!enabled);
    editButton.focusNode.disabled = false;
    dojo.byId('editButton').title = enabled ? 'Edit' : 'Not editable';
  }
}