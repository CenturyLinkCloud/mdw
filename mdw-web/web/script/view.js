function displayFileView()
{
  removeContainerChild(dojo.byId('fileViewLayoutContainer'), dojo.byId('propertyFileEditForm'));
  
  var fileView = dijit.byId('fileView');
  var verticalSliderContainer = dijit.byId('verticalSliderLayoutContainer');
  var lineNumbersPane = dijit.byId('lineNumbersContentPane');
  var horizontalSliderPane = dijit.byId('horizontalSliderContentPane');
  
  dojo.place(horizontalSliderPane.domNode, dojo.byId('fileViewLayoutContainer'), 'first');
  dojo.place(lineNumbersPane.domNode, dojo.byId('fileViewLayoutContainer'), 'first');
  dojo.place(verticalSliderContainer.domNode, dojo.byId('fileViewLayoutContainer'), 'first');
  dojo.place(fileView.domNode, dojo.byId('fileViewLayoutContainer'), 'first');
}

function displayEditBar(saveHandler, cancelHandler)
{
  removeContainerChild(dojo.byId('toolbarParent'), dojo.byId('filePanelToolbar'));
  
  var editbar = dijit.byId('editbar');
  if (!editbar)
  {
    // build the editbar
    editbar = new dijit.Toolbar({id:'editbar',style:'border:none;height:100%;'});
    dojo.place(editbar.domNode, dojo.byId('toolbarParent'), 'first');
    var reactCheckBox = new dijit.form.CheckBox({id:'reactCheckBox',style:'margin-left:15px;margin-right:2px;'});
    reactCheckBox.setChecked(true);
    var globalCheckBox = new dijit.form.CheckBox({id:'globalCheckBox',style:'margin-left:15px;margin-right:2px;'});
    var saveButton = new dijit.form.Button({id:'saveButton',label:'Save',style:'margin:12px;height;margin-left:25px;:16px;border:1px solid #bfbfbf;font-size:8pt;background-color:#e7e7e7;'});
    var cancelButton = new dijit.form.Button({id:'cancelButton',label:'Cancel',style:'margin:12px;margin-left:0px;height:16px;border:1px solid #bfbfbf;font-size:8pt;background-color:#e7e7e7;'});
    if (standAloneMode != true)
    {
      editbar.addChild(reactCheckBox);
      addLabel('editbar', 'reactCheckBox', 'React To Change');
      editbar.addChild(globalCheckBox);
      addLabel('editbar', 'globalCheckBox', 'Apply Change Globally');
    }
    editbar.addChild(saveButton);
    styleButton('saveButton');
    dojo.connect(dojo.byId('saveButton'), 'onclick', saveHandler);
    editbar.addChild(cancelButton);
    styleButton('cancelButton');
    dojo.connect(dojo.byId('cancelButton'), 'onclick', cancelHandler);
  }
  else
  {
    dojo.place(editbar.domNode, dojo.byId('toolbarParent'), 'first'); 
  } 
}

function displayToolBar()
{
  removeContainerChild(dojo.byId('toolbarParent'), dojo.byId('editbar'));
  var toolbar = dijit.byId('filePanelToolbar');
  dojo.place(toolbar.domNode, dojo.byId('toolbarParent'), 'first');
}

function displayBlankBar(saveHandler, cancelHandler)
{
  removeContainerChild(dojo.byId('toolbarParent'), dojo.byId('filePanelToolbar'));
  removeContainerChild(dojo.byId('toolbarParent'), dojo.byId('editbar'));
}

function addLabel(toId, forId, labelText)
{
  var label = document.createElement('label');
  label.setAttribute('id', forId + 'Label');
  label.setAttribute('for', forId);
  label.setAttribute('class', 'generalLabel');
  var text = document.createTextNode(labelText);
  dojo.byId(toId).appendChild(label);
  dojo.byId(forId + 'Label').appendChild(text);
}

function styleButton(buttonId)
{
  var btn = dojo.byId(buttonId);
  btn.style.height = '16px';
  btn.style.fontSize = '8pt';
}