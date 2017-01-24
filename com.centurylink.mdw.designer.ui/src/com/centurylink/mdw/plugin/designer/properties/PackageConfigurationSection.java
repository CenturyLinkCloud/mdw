/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.properties;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlOptions;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.IFilter;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.bpm.ApplicationPropertiesDocument.ApplicationProperties;
import com.centurylink.mdw.bpm.PackageDocument;
import com.centurylink.mdw.bpm.ProcessDefinitionDocument;
import com.centurylink.mdw.bpm.PropertyDocument.Property;
import com.centurylink.mdw.bpm.PropertyGroupDocument.PropertyGroup;
import com.centurylink.mdw.common.Compatibility;
import com.centurylink.mdw.designer.DesignerCompatibility;
import com.centurylink.mdw.model.value.attribute.AttributeVO;
import com.centurylink.mdw.model.value.process.PackageVO;
import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.designer.DesignerProxy;
import com.centurylink.mdw.plugin.designer.dialogs.MdwChoiceDialog;
import com.centurylink.mdw.plugin.designer.dialogs.MdwInputDialog;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.plugin.designer.model.WorkflowPackage;
import com.centurylink.mdw.plugin.designer.properties.editor.ColumnSpec;
import com.centurylink.mdw.plugin.designer.properties.editor.PropertyEditor;

public class PackageConfigurationSection extends PropertySection implements IFilter
{
  private WorkflowPackage workflowPackage;
  public WorkflowPackage getPackage() { return workflowPackage; }

  private List<PropertyGroup> propertyGroups;
  private List<ColumnSpec> columnSpecs;
  private List<PropertyRow> propertyRows;

  private TableViewer tableViewer;
  private Table table;

  private boolean dirty;

  private Composite buttonComposite;
  private Button addButton;
  private Button deleteButton;
  private Button newEnvButton;
  private Button deleteEnvButton;
  private Button saveButton;

  public void setSelection(WorkflowElement selection)
  {
    workflowPackage = (WorkflowPackage) selection;

    if (table != null)
      table.dispose();
    if (buttonComposite != null)
      buttonComposite.dispose();

    BusyIndicator.showWhile(getShell().getDisplay(), new Runnable()
    {
      public void run()
      {
        propertyGroups = getPropertyGroups();
        propertyRows = getPropertyRows(propertyGroups);
        columnSpecs = new ArrayList<ColumnSpec>();

        ColumnSpec propNameColSpec = new ColumnSpec(PropertyEditor.TYPE_TEXT, "Property", "propertyName");
        propNameColSpec.width = 200;
        columnSpecs.add(propNameColSpec);

        for (PropertyGroup propGroup : propertyGroups)
        {
          ColumnSpec colSpec = new ColumnSpec(PropertyEditor.TYPE_TEXT, propGroup.getName(), propGroup.getName());
          colSpec.width = 200;
          columnSpecs.add(colSpec);
        }

        Collections.sort(columnSpecs, new Comparator<ColumnSpec>()
        {
          public int compare(ColumnSpec cs1, ColumnSpec cs2)
          {
            if (cs1.label.equals("Property"))
              return -1;
            if (cs2.label.equals("Property"))
              return 1;

            // dev always comes first; prod always comes last
            if (cs1.label.equalsIgnoreCase("dev") || cs2.label.equalsIgnoreCase("prod"))
              return -1;
            if (cs2.label.equalsIgnoreCase("dev") || cs1.label.equalsIgnoreCase("prod"))
              return 1;

            return cs1.label.compareToIgnoreCase(cs2.label);
          }
        });

        Collections.sort(propertyRows);
      }
    });

    table = createTable(composite);
    tableViewer = createTableViewer(table);
    tableViewer.setContentProvider(new PackageConfigContentProvider());
    tableViewer.setLabelProvider(new PackageConfigLabelProvider());
    tableViewer.setInput(propertyRows);

    if ((!workflowPackage.isArchived() || DesignerProxy.isArchiveEditAllowed()) && workflowPackage.getProject().isUserAuthorizedForSystemAdmin())
      createButtons(composite);

    composite.layout(true);
  }

  public void drawWidgets(Composite composite, WorkflowElement selection)
  {
    // widget creation deferred until setSelection()
  }

  private List<PropertyGroup> getPropertyGroups()
  {
    if (workflowPackage.getMetaContent() == null &&  !workflowPackage.isDefaultPackage()) // empty string signifies already loaded
    {
      try
      {
        PackageVO packageVO = workflowPackage.getProject().getDesignerProxy().getDesignerDataAccess().loadPackage(workflowPackage.getId(), false);
        workflowPackage.setMetaContent(packageVO.getMetaContent() == null ? "" : packageVO.getMetaContent());
      }
      catch (Exception ex)
      {
        PluginMessages.uiError(getShell(), ex, "Package Config", workflowPackage.getProject());
      }
    }

    if (workflowPackage.getMetaContent() == null || workflowPackage.getMetaContent().trim().length() == 0)
    {
      return new ArrayList<PropertyGroup>();
    }
    else
    {
      ApplicationProperties appProps = null;
      try
      {
        if (workflowPackage.getMetaContent().trim().startsWith("{"))
        {
          List<PropertyGroup> propGroups = new ArrayList<PropertyGroup>();
          PackageVO metaPkg = new PackageVO(new JSONObject(workflowPackage.getMetaContent()));
          if (metaPkg.getAttributes() != null) {
              Map<String,List<AttributeVO>> groupedAttrs = metaPkg.getAttributesByGroup();
              for (String group : groupedAttrs.keySet()) {
                  PropertyGroup propGroup = PropertyGroup.Factory.newInstance();
                  if (group != null)
                      propGroup.setName(group);
                  for (AttributeVO groupAttr : groupedAttrs.get(group)) {
                      Property prop = propGroup.addNewProperty();
                      prop.setName(groupAttr.getAttributeName());
                      prop.setStringValue(groupAttr.getAttributeValue());
                  }
                  propGroups.add(propGroup);
              }
          }
          return propGroups;
        }
        else if (workflowPackage.getMetaContent().startsWith("<bpm:package") || workflowPackage.getMetaContent().startsWith("<package"))
        {
          PackageDocument pkgDoc = PackageDocument.Factory.parse(workflowPackage.getMetaContent());
          appProps = pkgDoc.getPackage().getApplicationProperties();
        }
        else
        {
          ProcessDefinitionDocument procDefDoc = ProcessDefinitionDocument.Factory.parse(workflowPackage.getMetaContent(), Compatibility.namespaceOptions());
          appProps = procDefDoc.getProcessDefinition().getApplicationProperties();
        }
      }
      catch (Exception ex)
      {
        PluginMessages.uiError(getShell(), ex, "Package Config", workflowPackage.getProject());
      }
      if (appProps != null)
      {
        if (appProps != null && appProps.getPropertyGroupList() != null)
          return appProps.getPropertyGroupList();
      }
      return new ArrayList<PropertyGroup>(); // not found or can't parse
    }
  }

  private void updateModel()
  {
    updatePropertyGroups();
    try
    {
      updatePackageMetaContent();
      dirty = true;
      saveButton.setEnabled(dirty);
    }
    catch (Exception ex)
    {
      PluginMessages.uiError(getShell(), ex, "Update Package Config", workflowPackage.getProject());
    }
  }

  private void updatePropertyGroups()
  {
    List<PropertyGroup> updatedGroups = new ArrayList<PropertyGroup>();

    for (PropertyRow propRow : propertyRows)
    {
      for (String env : propRow.envValues.keySet())
      {
        // get the group
        PropertyGroup propGroup = null;
        for (PropertyGroup group : updatedGroups)
        {
          if (group.getName().equals(env))
          {
            propGroup = group;
            break;
          }
        }
        if (propGroup == null)
        {
          propGroup = PropertyGroup.Factory.newInstance();
          propGroup.setName(env);
          updatedGroups.add(propGroup);
        }

        // set the prop value
        Property property = propGroup.addNewProperty();
        property.setName(propRow.propName.trim());
        property.setStringValue(propRow.envValues.get(env).trim());
      }
    }

    propertyGroups = updatedGroups;
  }

  private void updatePackageMetaContent() throws XmlException, JSONException, IOException
  {
    if (!workflowPackage.getProject().isOldNamespaces())
    {
      if (workflowPackage.getMetaContent() != null && workflowPackage.getMetaContent().trim().startsWith("{"))
      {
        PackageVO metaPkg = new PackageVO(new JSONObject(workflowPackage.getMetaContent()));
        List<AttributeVO> attributes = null;
        if (propertyGroups != null)
        {
          attributes = new ArrayList<AttributeVO>();
          for (PropertyGroup propGroup : propertyGroups)
          {
            String group = propGroup.getName();
            for (Property prop : propGroup.getPropertyList())
            {
              AttributeVO attribute = new AttributeVO(prop.getName(), prop.getStringValue());
              attribute.setAttributeGroup(group);
              attributes.add(attribute);
            }
          }
        }
        metaPkg.setAttributes(attributes);
        workflowPackage.setMetaContent(metaPkg.getJson(false).toString(2));
      }
      else
      {
        PackageDocument pkgDefDoc = null ;
        if (workflowPackage.getMetaContent() == null || workflowPackage.getMetaContent().isEmpty())
        {
          pkgDefDoc = PackageDocument.Factory.newInstance();
        }
        else
        {
          if (workflowPackage.getMetaContent().startsWith("<bpm:package") || workflowPackage.getMetaContent().startsWith("<package"))
          {
            pkgDefDoc = PackageDocument.Factory.parse(workflowPackage.getMetaContent());
          }
          else
          {
            pkgDefDoc = PackageDocument.Factory.newInstance();
          }
        }
        if (pkgDefDoc.getPackage() == null)
          pkgDefDoc.addNewPackage();
        if (pkgDefDoc.getPackage().getApplicationProperties() == null)
          pkgDefDoc.getPackage().addNewApplicationProperties();

        pkgDefDoc.getPackage().getApplicationProperties().setPropertyGroupArray(propertyGroups.toArray(new PropertyGroup[0]));
        String procDefStr = pkgDefDoc.xmlText(new XmlOptions().setSavePrettyPrint().setSavePrettyPrintIndent(2));
        workflowPackage.setMetaContent(procDefStr);
      }
    }
    else
    {
      ProcessDefinitionDocument procDefDoc;
      if (workflowPackage.getMetaContent() == null || workflowPackage.getMetaContent().isEmpty())
        procDefDoc = ProcessDefinitionDocument.Factory.newInstance();
      else
        procDefDoc = ProcessDefinitionDocument.Factory.parse(workflowPackage.getMetaContent(), Compatibility.namespaceOptions());

      if (procDefDoc.getProcessDefinition() == null)
        procDefDoc.addNewProcessDefinition();
      if (procDefDoc.getProcessDefinition().getApplicationProperties() == null)
        procDefDoc.getProcessDefinition().addNewApplicationProperties();

      procDefDoc.getProcessDefinition().getApplicationProperties().setPropertyGroupArray(propertyGroups.toArray(new PropertyGroup[0]));
      String procDefStr = DesignerCompatibility.getInstance().getOldProcessDefinition(procDefDoc);
      workflowPackage.setMetaContent(procDefStr);
    }
  }

  private void saveMetaContent()
  {
    BusyIndicator.showWhile(getShell().getDisplay(), new Runnable()
    {
      public void run()
      {
        try
        {
          if (workflowPackage.getMetaContent() != null && workflowPackage.getMetaContent().trim().startsWith("{"))
            workflowPackage.getPackageVO().setAttributes(workflowPackage.getPackageVO().getMetaAttributes());
          getDesignerProxy().savePackage(workflowPackage);
          dirty = false;
          getDesignerProxy().getCacheRefresh().fireRefresh(false);
        }
        catch (Exception ex)
        {
          PluginMessages.uiError(getShell(), ex, "Save Package", workflowPackage.getProject());
        }
      }
    });

    saveButton.setEnabled(dirty);
  }

  private List<PropertyRow> getPropertyRows(List<PropertyGroup> propGroups)
  {
    List<PropertyRow> rows = new ArrayList<PropertyRow>();
    for (PropertyGroup propGroup : propGroups)
    {
      for (Property prop : propGroup.getPropertyList())
      {
        PropertyRow propRow = null;
        for (PropertyRow row : rows)
        {
          if (row.propName.equals(prop.getName()))
          {
            propRow = row;
            break;
          }
        }
        if (propRow == null)
        {
          propRow = new PropertyRow(prop.getName());
          rows.add(propRow);
        }
        propRow.envValues.put(propGroup.getName().trim(), prop.getStringValue().trim());
      }
    }
    return rows;
  }

  private Table createTable(Composite parent)
  {
    int style = SWT.SINGLE | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.HIDE_SELECTION;

    table = new Table(parent, style);

    GridData gridData = new GridData(GridData.FILL_BOTH);
    table.setLayoutData(gridData);
    table.setLinesVisible(true);
    table.setHeaderVisible(true);

    for (int i = 0; i < columnSpecs.size(); i++)
    {
      ColumnSpec colSpec = columnSpecs.get(i);
      int colStyle = SWT.LEFT | colSpec.style;
      if (colSpec.readOnly)
        colStyle = colStyle | SWT.READ_ONLY;
      TableColumn column = new TableColumn(table, colStyle, i);
      column.setText(colSpec.label);
      column.setWidth(colSpec.width);
      column.setResizable(colSpec.resizable);
    }

    return table;
  }

  private TableViewer createTableViewer(Table table)
  {
    TableViewer tableViewer = new TableViewer(table);
    tableViewer.setUseHashlookup(true);

    String[] columnProps = new String[columnSpecs.size()];
    for (int i = 0; i < columnSpecs.size(); i++)
      columnProps[i] = columnSpecs.get(i).property;

    tableViewer.setColumnProperties(columnProps);

    CellEditor[] cellEditors = new CellEditor[columnSpecs.size()];
    for (int i = 0; i < columnSpecs.size(); i++)
    {
      ColumnSpec colSpec = columnSpecs.get(i);
      CellEditor cellEditor = null;
      if (colSpec.style != 0)
        cellEditor = new TextCellEditor(table, colSpec.style);
      else
        cellEditor = new TextCellEditor(table);
      if (colSpec.listener != null)
        cellEditor.addListener(colSpec.listener);
        cellEditors[i] = cellEditor;

      tableViewer.setCellEditors(cellEditors);
      tableViewer.setCellModifier(new PackageConfigCellModifier());
    }

    return tableViewer;
  }

  private void createButtons(Composite parent)
  {
    buttonComposite = new Composite(parent, SWT.NONE);
    GridLayout gl = new GridLayout();
    gl.numColumns = 1;
    buttonComposite.setLayout(gl);
    GridData gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_FILL);
    buttonComposite.setLayoutData(gd);


    Group propBtnGroup = new Group(buttonComposite, SWT.NONE);
    propBtnGroup.setText("Properties");
    gl = new GridLayout();
    propBtnGroup.setLayout(gl);
    gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_FILL);
    gd.verticalIndent = 15;
    propBtnGroup.setLayoutData(gd);

    // add prop
    addButton = new Button(propBtnGroup, SWT.PUSH | SWT.CENTER);
    addButton.setText("Add");
    GridData gridData = new GridData(GridData.HORIZONTAL_ALIGN_CENTER);
    gridData.widthHint = 60;
    gridData.horizontalIndent = 3;
    addButton.setLayoutData(gridData);
    addButton.addSelectionListener(new SelectionAdapter()
    {
      public void widgetSelected(SelectionEvent e)
      {
        propertyRows.add(new PropertyRow("new.property"));
        if (propertyRows.size() == 1) // first property added
          propertyRows.get(0).envValues.put("dev", "");
        tableViewer.setInput(propertyRows);
        updateModel();
        if (propertyRows.size() == 1) // first property added
          setSelection(workflowPackage);
      }
    });

    // delete prop
    deleteButton = new Button(propBtnGroup, SWT.PUSH | SWT.CENTER);
    deleteButton.setText("Delete");
    gridData = new GridData(GridData.HORIZONTAL_ALIGN_CENTER);
    gridData.widthHint = 60;
    gridData.horizontalIndent = 3;
    deleteButton.setLayoutData(gridData);
    deleteButton.addSelectionListener(new SelectionAdapter()
    {
      public void widgetSelected(SelectionEvent e)
      {
        PropertyRow row = (PropertyRow) ((IStructuredSelection)tableViewer.getSelection()).getFirstElement();
        if (row != null)
          propertyRows.remove(row);
        tableViewer.setInput(propertyRows);
        updateModel();
      }
    });

    Group envBtnGroup = new Group(buttonComposite, SWT.NONE);
    envBtnGroup.setText("Environments");
    gl = new GridLayout();
    envBtnGroup.setLayout(gl);
    gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_FILL);
    gd.verticalIndent = 10;
    envBtnGroup.setLayoutData(gd);

    // add env
    newEnvButton = new Button(envBtnGroup, SWT.PUSH | SWT.CENTER);
    newEnvButton.setText("Add...");
    gridData = new GridData(GridData.HORIZONTAL_ALIGN_CENTER);
    gridData.widthHint = 60;
    gridData.horizontalIndent = 3;
    newEnvButton.setLayoutData(gridData);
    newEnvButton.addSelectionListener(new SelectionAdapter()
    {
      public void widgetSelected(SelectionEvent e)
      {
        MdwInputDialog inputDlg = new MdwInputDialog(getShell(), "Environment Name", false);
        inputDlg.setTitle("New Environment");
        if (inputDlg.open() == Dialog.OK)
        {
          if (propertyRows.isEmpty())
            propertyRows.add(new PropertyRow("new.property"));
          propertyRows.get(0).envValues.put(inputDlg.getInput(), "");
          tableViewer.setInput(propertyRows);
          updateModel();
          setSelection(workflowPackage);
        }
      }
    });

    // delete env
    deleteEnvButton = new Button(envBtnGroup, SWT.PUSH | SWT.CENTER);
    deleteEnvButton.setText("Delete...");
    gridData = new GridData(GridData.HORIZONTAL_ALIGN_CENTER);
    gridData.widthHint = 60;
    gridData.horizontalIndent = 3;
    deleteEnvButton.setLayoutData(gridData);
    deleteEnvButton.addSelectionListener(new SelectionAdapter()
    {
      public void widgetSelected(SelectionEvent e)
      {
        List<String> envs = new ArrayList<String>();
        for (PropertyRow propRow : propertyRows)
        {
          for (String env : propRow.envValues.keySet())
          {
            if (!envs.contains(env))
              envs.add(env);
          }
        }
        MdwChoiceDialog choiceDlg = new MdwChoiceDialog(getShell(), "Environment to Remove", envs.toArray(new String[0]));
        choiceDlg.setTitle("Remove Environment");
        int res = choiceDlg.open();
        if (res != MdwChoiceDialog.CANCEL)
        {
          String choice = envs.get(res);
          for (PropertyRow propRow : propertyRows)
          {
            propRow.envValues.remove(choice);
          }
          updateModel();
          setSelection(workflowPackage);
        }
      }
    });

    // save
    saveButton = new Button(buttonComposite, SWT.PUSH | SWT.CENTER);
    saveButton.setText("Save");
    gridData = new GridData(GridData.HORIZONTAL_ALIGN_CENTER);
    gridData.widthHint = 60;
    gridData.verticalIndent = 15;
    saveButton.setLayoutData(gridData);
    saveButton.setEnabled(dirty);
    saveButton.addSelectionListener(new SelectionAdapter()
    {
      public void widgetSelected(SelectionEvent e)
      {
        saveMetaContent();
      }
    });
  }

  public void aboutToBeHidden()
  {
    if (dirty)
      MessageDialog.openWarning(getShell(), "Package Configuration", "Remember to save any changes.");
  }

  public boolean select(Object toTest)
  {
    if (toTest == null || !(toTest instanceof WorkflowPackage))
      return false;

    return ((WorkflowPackage)toTest).getProject().checkRequiredVersion(5, 1);
  }

  class PackageConfigContentProvider implements IStructuredContentProvider
  {
    @SuppressWarnings("unchecked")
    public Object[] getElements(Object inputElement)
    {
      List<PropertyRow> rows = (List<PropertyRow>) inputElement;
      return rows.toArray(new PropertyRow[0]);
    }

    public void dispose()
    {
    }

    public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
    {
    }
  }

  private static ImageDescriptor imageDescriptor = MdwPlugin.getImageDescriptor("icons/properties.gif");
  private static Image image = imageDescriptor.createImage();

  class PackageConfigLabelProvider extends LabelProvider implements ITableLabelProvider
  {
    public Image getColumnImage(Object element, int columnIndex)
    {
      if (columnIndex == 0)
        return image;
      else
        return null;
    }

    public String getColumnText(Object element, int columnIndex)
    {
      PropertyRow row = (PropertyRow) element;
      if (columnIndex == 0)
        return " " + row.propName;
      else
        return row.envValues.get(columnSpecs.get(columnIndex).property);
    }
  }

  class PackageConfigCellModifier implements ICellModifier
  {
    public boolean canModify(Object element, String property)
    {
      return true;
    }

    public Object getValue(Object element, String colName)
    {
      PropertyRow row = (PropertyRow) element;
      if (colName.equals("propertyName"))
        return row.propName;
      else
      {
        String val = row.envValues.get(colName);
        return val == null ? "" : val;
      }
    }

    public void modify(Object element, String colName, Object value)
    {
      TableItem item = (TableItem) element;
      PropertyRow row = (PropertyRow) item.getData();
      String stringVal = value == null ? null : value.toString();
      if (colName.equals("propertyName"))
        row.propName = stringVal;
      else
        row.envValues.put(colName, stringVal);

      // save back to the table
      tableViewer.setInput(propertyRows);
      updateModel();
    }
  }

  class PropertyRow implements Comparable<PropertyRow>
  {
    private String propName;
    private Map<String,String> envValues = new LinkedHashMap<String,String>();

    public PropertyRow(String propName)
    {
      this.propName = propName;
    }

    public int compareTo(PropertyRow other)
    {
      return this.propName.compareTo(other.propName);
    }
  }
}