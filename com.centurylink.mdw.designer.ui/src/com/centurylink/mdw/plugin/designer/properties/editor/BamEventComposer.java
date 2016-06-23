/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.properties.editor;

import java.util.ArrayList;
import java.util.List;

import noNamespace.DropdownT;
import noNamespace.OptionT;
import noNamespace.PAGELETDocument;
import noNamespace.PageletT;
import noNamespace.TableT;

import org.apache.xmlbeans.XmlException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Text;

import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.designer.DirtyStateListener;
import com.centurylink.mdw.plugin.designer.model.Activity;
import com.centurylink.mdw.plugin.designer.model.WorkflowAsset;
import com.centurylink.mdw.plugin.designer.model.WorkflowPackage;
import com.centurylink.mdw.plugin.designer.model.WorkflowProcess;
import com.centurylink.mdw.plugin.designer.model.Transition;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.plugin.preferences.model.PreferenceConstants;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;
import com.centurylink.mdw.model.value.attribute.AttributeVO;
import com.centurylink.mdw.model.value.event.BamMessageDefinition;
import com.centurylink.mdw.model.value.process.PackageVO;

/**
 * Widget for editing BAM events.
 */
public class BamEventComposer extends Composer
{
  public static final String DEFAULT_BAM_PAGELET = "<PAGELET>\n"
    + "  <TABLE NAME=\"attributes\" LABEL=\"BAM Values:\" VH=\"150\">\n"
    + "    <TEXT NAME=\"name\" LABEL=\"Name\" VW=\"150\"/>\n"
    + "    <TEXT NAME=\"value\" LABEL=\"Value\" />\n"
    + "  </TABLE>\n"
    + "</PAGELET>";

  private static final int LAYOUT_COLS = 5;

  private BamMessageDefinition bamMessage;
  public BamMessageDefinition getBamMessage() { return bamMessage; }
  public void setBamMessage(BamMessageDefinition bamMessage) { this.bamMessage = bamMessage; }

  private Text eventNameText;
  private Text realmText;
  private Text categoryText;
  private Text subCategoryText;
  private Text componentText;
  private Button clearButton;
  private Text dataText;

  private boolean eventDataField;
  private PageletT bamPagelet;

  private AttributesTableContainer attributesTableContainer;
  private DirtyStateListener dataTableDirtyStateListener;

  public BamEventComposer(Composite parent, int style, int width, boolean readOnly)
  {
    super(parent, style, width, readOnly);
    eventDataField = MdwPlugin.getDefault().getPreferenceStore().getBoolean(PreferenceConstants.PREFS_SHOW_BAM_EVENT_DATA_INPUT_FIELD);
    createControls();
  }

  private WorkflowElement element;
  public WorkflowElement getElement() { return element; }
  public void setElement(WorkflowElement element)
  {
    this.element = element;
    WorkflowAsset bamPageletAsset = getElement().getPackage().getAsset(WorkflowProject.BAM_PAGELET);
    if (bamPageletAsset == null)
    {
      // try baseline package
      WorkflowPackage baselinePackage = element.getProject().getPackage(PackageVO.BASELINE_PACKAGE_NAME);
      if (baselinePackage != null)
        bamPageletAsset = baselinePackage.getAsset(WorkflowProject.BAM_PAGELET);
    }

    bamPagelet = null;
    try
    {
      if (bamPageletAsset == null)
      {
        bamPagelet = PAGELETDocument.Factory.parse(BamEventComposer.DEFAULT_BAM_PAGELET).getPAGELET();
      }
      else
      {
        if (!bamPageletAsset.isLoaded())
          bamPageletAsset.load();
        bamPagelet = PAGELETDocument.Factory.parse(bamPageletAsset.getContent()).getPAGELET();
      }
    }
    catch (XmlException ex)
    {
      PluginMessages.uiError(ex, "BAM Pagelet", element.getProject());
    }

    createBamDataTable();
  }

  protected void createControls()
  {
    GridLayout gridLayout = new GridLayout();
    gridLayout.numColumns = LAYOUT_COLS;
    gridLayout.marginWidth = 0;
    gridLayout.marginTop = -5;
    gridLayout.horizontalSpacing = 5;
    setLayout(gridLayout);

    int textWidth = 172;
    int spacerWidth = 10;


    // event name
    createLabel("Event Name");
    eventNameText = createText(textWidth);
    eventNameText.addModifyListener(new ModifyListener()
    {
      public void modifyText(ModifyEvent e)
      {
        if (checkTextChange(eventNameText, bamMessage.getEventName()))
        {
          bamMessage.setEventName(eventNameText.getText().trim());
          fireModify(e);
        }
      }
    });

    createSpacer(spacerWidth);

    // realm
    createLabel("Realm");
    realmText = createText(textWidth);
    realmText.addModifyListener(new ModifyListener()
    {
      public void modifyText(ModifyEvent e)
      {
        if (checkTextChange(realmText, bamMessage.getRealm()))
        {
          bamMessage.setRealm(realmText.getText().trim());
          fireModify(e);
        }
      }
    });

    // category
    createLabel("Category");
    categoryText = createText(textWidth);
    categoryText.addModifyListener(new ModifyListener()
    {
      public void modifyText(ModifyEvent e)
      {
        if (checkTextChange(categoryText, bamMessage.getEventCategory()))
        {
          bamMessage.setEventCategory(categoryText.getText().trim());
          fireModify(e);
        }
      }
    });

    createSpacer(spacerWidth);

    // subcategory
    createLabel("Subcategory");
    subCategoryText = createText(textWidth);
    subCategoryText.addModifyListener(new ModifyListener()
    {
      public void modifyText(ModifyEvent e)
      {
        if (checkTextChange(subCategoryText, bamMessage.getEventSubCategory()))
        {
          bamMessage.setEventSubCategory(subCategoryText.getText().trim());
          fireModify(e);
        }
      }
    });

    // component
    createLabel("Component");
    componentText = createText(textWidth);
    componentText.addModifyListener(new ModifyListener()
    {
      public void modifyText(ModifyEvent e)
      {
        if (checkTextChange(componentText, bamMessage.getComponentId()))
        {
          bamMessage.setComponentId(componentText.getText().trim());
          fireModify(e);
        }
      }
    });

    createSpacer(spacerWidth);

    // clear button
    clearButton = createButton("Clear");
    GridData gd = new GridData(SWT.NONE);
    gd.horizontalSpan = 2;
    gd.horizontalAlignment = SWT.LEFT;
    gd.widthHint = 50;
    gd.horizontalIndent = 200;
    clearButton.setLayoutData(gd);
    clearButton.addSelectionListener(new SelectionAdapter()
    {
      public void widgetSelected(SelectionEvent e)
      {
        bamMessage.clear();
        setInput(bamMessage);
        fireModify(null);
      }
    });

    // data
    if (eventDataField)
    {
      createLabel("Event Data", LAYOUT_COLS);
      dataText = createText(getWidth(), LAYOUT_COLS, true);
      dataText.addModifyListener(new ModifyListener()
      {
        public void modifyText(ModifyEvent e)
        {
          if (checkTextChange(dataText, bamMessage.getEventData()))
          {
            bamMessage.setEventData(dataText.getText().trim());
            fireModify(e);
          }
        }
      });
    }
  }

  private void createBamDataTable()
  {
    if (attributesTableContainer != null)
    {
      if (dataTableDirtyStateListener != null)
        attributesTableContainer.removeDirtyStateListener(dataTableDirtyStateListener);
      attributesTableContainer.dispose();
    }

    // attributes
    TableT bamTable = null;
    if (bamPagelet != null)
    {
      try
      {
        bamTable = getBamPageletTable();
      }
      catch (BamPageletValidationException ex)
      {
        PluginMessages.uiError(ex, "BAM Pagelet");
      }
    }

    String label = "BAM Values:";
    if (bamTable != null && (bamTable.getNAME() != null || bamTable.getLABEL() != null))
      label = bamTable.getLABEL() == null ? bamTable.getNAME() : bamTable.getLABEL();

    //createLabel(label, layoutCols);
    //bamTable.setLABEL(null); // we've just rendered the label

    bamTable.setLABEL(label);
    for (DropdownT dropdown : bamTable.getDROPDOWNList())
    {
      if ("Variables".equalsIgnoreCase(dropdown.getSOURCE()))
      {
        WorkflowProcess processVersion = getProcess();
        if (processVersion != null)
        {
          List<String> varNames = processVersion.getNonDocRefVariableNames();
          // add process variable values
          for (String var : varNames)
          {
            OptionT option = dropdown.addNewOPTION();
            option.setVALUE("#{" + var + "}");
            option.setStringValue(var);
          }
        }
      }
    }

    attributesTableContainer = createAttributesTable(getWidth(), LAYOUT_COLS, bamTable);
    dataTableDirtyStateListener = new DirtyStateListener()
    {
      public void dirtyStateChanged(boolean dirty)
      {
        if (bamMessage != null)
        {
          bamMessage.setAttributes(attributesTableContainer.getAttributes());
          Event event = new Event();
          event.widget = attributesTableContainer.tableEditor.getTable();
          event.data = attributesTableContainer.getAttributes();
          fireModify(new ModifyEvent(event));
        }
      }
    };
    attributesTableContainer.addDirtyStateListener(dataTableDirtyStateListener);

    this.layout(true);
  }

  private boolean checkTextChange(Text text, String value)
  {
    if (bamMessage == null || value == null || value.length() == 0)
      return !text.getText().trim().isEmpty();
    else
      return !text.getText().trim().equals(value);
  }

  @Override
  public void setInput(Object input)
  {
    setBamMessage((BamMessageDefinition)input);
    if (bamMessage == null || bamMessage.getEventName() == null)
      eventNameText.setText("");
    else
      eventNameText.setText(bamMessage.getEventName());

    if (bamMessage == null || bamMessage.getRealm() == null)
      realmText.setText("");
    else
      realmText.setText(bamMessage.getRealm());

    if (bamMessage == null || bamMessage.getEventCategory() == null)
      categoryText.setText("");
    else
      categoryText.setText(bamMessage.getEventCategory());

    if (bamMessage == null || bamMessage.getEventSubCategory() == null)
      subCategoryText.setText("");
    else
      subCategoryText.setText(bamMessage.getEventSubCategory());

    if (bamMessage == null || bamMessage.getComponentId() == null)
      componentText.setText("");
    else
      componentText.setText(bamMessage.getComponentId());

    if (eventDataField)
    {
      if (bamMessage == null || bamMessage.getEventData() == null)
        dataText.setText("");
      else
        dataText.setText(bamMessage.getEventData());
    }

    if (bamMessage == null)
      attributesTableContainer.setAttributes(new ArrayList<AttributeVO>());
    else
      attributesTableContainer.setAttributes(bamMessage.getAttributes());
  }

  @Override
  public void setEditable(boolean editable)
  {
    eventNameText.setEditable(editable);
    realmText.setEditable(editable);
    categoryText.setEditable(editable);
    subCategoryText.setEditable(editable);
    componentText.setEditable(editable);
    clearButton.setEnabled(editable);
    if (eventDataField)
      dataText.setEditable(editable);
    attributesTableContainer.setEditable(editable);
  }

  protected AttributesTableContainer createAttributesTable(int width, int colspan, TableT bamTable)
  {
    return new AttributesTableContainer(this, isReadOnly(), width, colspan, bamTable);
  }

  protected WorkflowProcess getProcess()
  {
    WorkflowProcess process = null;
    if (element instanceof Activity)
      process = ((Activity)element).getProcess();
    else if (element instanceof WorkflowProcess)
      process = (WorkflowProcess) element;
    else if (element instanceof Transition)
      process = ((Transition)element).getProcess();

    return process;
  }

  private TableT getBamPageletTable() throws BamPageletValidationException
  {
    List<TableT> tables = bamPagelet.getTABLEList();
    if (tables == null || tables.size() != 1)
      throw new BamPageletValidationException("Bad BAM Pagelet - Should contain a single TABLE element");
    TableT bamTable = tables.get(0);
    if (AttributesTableContainer.getNameWidget(bamTable) == null)
      throw new BamPageletValidationException("Bad BAM Pagelet - TABLE should contain a subelement with NAME='name'");
    if (AttributesTableContainer.getValueWidget(bamTable) == null)
      throw new BamPageletValidationException("Bad BAM Pagelet - TABLE should contain a subelement with NAME='value'");

    return bamTable;
  }

  class BamPageletValidationException extends Exception
  {
    BamPageletValidationException(String message)
    {
      super(message);
    }
  }
}
