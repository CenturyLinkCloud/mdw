/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.editors;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.wst.xml.core.internal.contentmodel.CMElementDeclaration;
import org.eclipse.wst.xml.core.internal.contentmodel.modelquery.ModelQuery;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMAttr;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMDocument;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMElement;
import org.eclipse.wst.xml.ui.internal.XMLUIMessages;
import org.eclipse.wst.xml.ui.internal.actions.EditAttributeAction;
import org.eclipse.wst.xml.ui.internal.actions.NodeAction;
import org.eclipse.wst.xml.ui.internal.contentoutline.XMLNodeActionManager;
import org.eclipse.wst.xml.ui.internal.dialogs.EditAttributeDialog;
import org.eclipse.wst.xml.ui.internal.tabletree.TreeExtension;
import org.eclipse.wst.xml.ui.internal.tabletree.XMLEditorMessages;
import org.eclipse.wst.xml.ui.internal.tabletree.XMLTableTreeContentProvider;
import org.eclipse.wst.xml.ui.internal.tabletree.XMLTableTreeViewer;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.centurylink.jface.viewers.TreeComboCellEditor;
import com.centurylink.jface.viewers.TreeComboCellEditor.SelectionModifier;
import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.designer.model.ViewLayout;
import com.centurylink.swt.widgets.CTreeComboItem;

@SuppressWarnings("restriction")
public class ViewLayoutTableTreeViewer extends XMLTableTreeViewer
{
  private ViewLayout viewLayout;

  ImageDescriptor listImageDescriptor = MdwPlugin.getImageDescriptor("icons/list.gif");
  Image listImage = listImageDescriptor.createImage();
  ImageDescriptor filterImageDescriptor = MdwPlugin.getImageDescriptor("icons/uiFilter.gif");
  Image filterImage = filterImageDescriptor.createImage();
  ImageDescriptor detailImageDescriptor = MdwPlugin.getImageDescriptor("icons/detail.gif");
  Image detailImage = detailImageDescriptor.createImage();
  ImageDescriptor chartImageDescriptor = MdwPlugin.getImageDescriptor("icons/report.gif");
  Image chartImage = chartImageDescriptor.createImage();
  ImageDescriptor columnImageDescriptor = MdwPlugin.getImageDescriptor("icons/column.gif");
  Image columnImage = columnImageDescriptor.createImage();
  ImageDescriptor fieldImageDescriptor = MdwPlugin.getImageDescriptor("icons/field.gif");
  Image fieldImage = fieldImageDescriptor.createImage();

  public ViewLayoutTableTreeViewer(Composite parent, ViewLayout model)
  {
    super(parent);

    this.viewLayout = model;
    setLabelProvider(new XMLTableTreeContentProvider()
    {
      public String getText(Object object)
      {
        if (object instanceof Node)
        {
          Node node = (Node) object;
          if (node.getNodeType() == Node.ELEMENT_NODE)
          {
            Element element = (Element) node;
            if (("list".equals(element.getNodeName()) || "filter".equals(element.getNodeName()) || "detail".equals(element.getNodeName()) || "chart".equals(element.getNodeName()))
                && (element.getAttribute("id") != null && element.getAttribute("id").trim().length() > 0))
            {
              return element.getAttribute("id");
            }
            else if (("column".equals(element.getNodeName()) || "field".equals(element.getNodeName()) || "row".equals(element.getNodeName()))
                && (element.getAttribute("attribute") != null && element.getAttribute("attribute").trim().length() > 0))
            {
              return super.getText(object) + " (" + element.getAttribute("attribute") + ")";
            }
          }
        }
        return super.getText(object);
      }

      @Override
      public Image getImage(Object object)
      {
        if (object instanceof Node)
        {
          Node node = (Node) object;
          if (node.getNodeType() == Node.ELEMENT_NODE)
          {
            Element element = (Element) node;
            if ("list".equals(element.getNodeName()))
              return listImage;
            else if ("filter".equals(element.getNodeName()))
              return filterImage;
            else if ("detail".equals(element.getNodeName()))
              return detailImage;
            else if ("chart".equals(element.getNodeName()))
              return chartImage;
            else if ("column".equals(element.getNodeName()))
              return columnImage;
            else if ("field".equals(element.getNodeName()))
              return fieldImage;
            else if ("row".equals(element.getNodeName()))
              return fieldImage;
          }
        }
        return super.getImage(object);
      }
    });

    setCellModifier(new ViewLayoutCellModifier());
  }

  @Override
  protected void createContextMenu()
  {
    MenuManager contextMenu = new MenuManager("#PopUp");
    contextMenu.add(new Separator("additions"));
    contextMenu.setRemoveAllWhenShown(true);
    contextMenu.addMenuListener(new ViewLayoutNodeActionMenuListener());
    Menu menu = contextMenu.createContextMenu(getControl());
    getControl().setMenu(menu);
  }

  class ViewLayoutNodeActionMenuListener implements IMenuListener
  {
    public void menuAboutToShow(IMenuManager menuManager)
    {
      IDOMDocument idom = (IDOMDocument) getInput();
      XMLNodeActionManager nodeActionManager = new XMLNodeActionManager(idom.getModel(), ViewLayoutTableTreeViewer.this)
      {
        public void beginNodeAction(NodeAction action)
        {
          super.beginNodeAction(action);
        }
        public void endNodeAction(NodeAction action)
        {
          super.endNodeAction(action);
        }
        @SuppressWarnings("rawtypes")
        public void contributeActions(IMenuManager menu, List selection)
        {
          int ic = ModelQuery.INCLUDE_CHILD_NODES;
          int vc = ModelQuery.VALIDITY_NONE;

          if (selection.size() == 1 && selection.get(0) instanceof IDOMElement)
          {
            IDOMElement idomElem = (IDOMElement) selection.get(0);

            CMElementDeclaration ed = modelQuery.getCMElementDeclaration(idomElem);
            IMenuManager addAttributeMenu = new MyMenuManager(XMLUIMessages._UI_MENU_ADD_ATTRIBUTE);
            IMenuManager addChildMenu = new MyMenuManager(XMLUIMessages._UI_MENU_ADD_CHILD);

            if ("view".equals(idomElem.getNodeName()))
            {
              menu.add(addChildMenu);
              List modelQueryActionList = new ArrayList();
              modelQuery.getInsertActions(idomElem, ed, -1, ic, vc, modelQueryActionList);
              addActionHelper(addChildMenu, modelQueryActionList);
              contributeUnconstrainedAddElementAction(addChildMenu, idomElem, ed, -1);
            }
            else if ("list".equals(idomElem.getNodeName())
                  || "filter".equals(idomElem.getNodeName())
                  || "detail".equals(idomElem.getNodeName())
                  || "chart".equals(idomElem.getNodeName()))
            {
              contributeDeleteActions(menu, getSelectedNodes(selection, true), ic, vc);
              menu.add(addAttributeMenu);
              menu.add(addChildMenu);
              if (ed != null)
              {
                // attribute actions
                List modelQueryActionList = new ArrayList();
                modelQuery.getInsertActions(idomElem, ed, -1, ModelQuery.INCLUDE_ATTRIBUTES, vc, modelQueryActionList);
                addActionHelper(addAttributeMenu, modelQueryActionList);
                // child node actions
                modelQueryActionList = new ArrayList();
                modelQuery.getInsertActions(idomElem, ed, -1, ic, vc, modelQueryActionList);
                addActionHelper(addChildMenu, modelQueryActionList);
              }
              contributeUnconstrainedAttributeActions(addAttributeMenu, idomElem, ed);
            }
            else if ("column".equals(idomElem.getNodeName())
                  || "field".equals(idomElem.getNodeName())
                  || "row".equals(idomElem.getNodeName()))
            {
              contributeDeleteActions(menu, getSelectedNodes(selection, true), ic, vc);
              menu.add(addAttributeMenu);
              if (ed != null)
              {
                // attribute actions
                List modelQueryActionList = new ArrayList();
                modelQuery.getInsertActions(idomElem, ed, -1, ModelQuery.INCLUDE_ATTRIBUTES, vc, modelQueryActionList);
                addActionHelper(addAttributeMenu, modelQueryActionList);
              }
              contributeUnconstrainedAttributeActions(addAttributeMenu, idomElem, ed);
            }
            else
            {
              super.contributeActions(menu, selection);
            }
            return;
          }
          else if (selection.size() == 1 && selection.get(0) instanceof IDOMAttr)
          {
            IDOMAttr idomAttr = (IDOMAttr) selection.get(0);
            Element ownerElem = idomAttr.getOwnerElement();

            contributeDeleteActions(menu, getSelectedNodes(selection, true), ic, vc);

            Action editAttributeAction = new EditAttributeAction(this, ownerElem, idomAttr, XMLUIMessages._UI_MENU_EDIT_ATTRIBUTE, XMLUIMessages._UI_MENU_EDIT_ATTRIBUTE_TITLE)
            {
              public void run()
              {
                Shell shell = MdwPlugin.getShell();
                if (validateEdit(manager.getModel(), shell))
                {
                  manager.beginNodeAction(this);
                  EditAttributeDialog dialog = new ViewLayoutEditAttributeDialog(shell, viewLayout, ownerElement, attr);
                  dialog.create();
                  dialog.getShell().setText(title);
                  dialog.setBlockOnOpen(true);
                  dialog.open();

                  if (dialog.getReturnCode() == Window.OK)
                  {
                    if (attr != null)
                    {
                      ownerElement.removeAttributeNode(attr);
                    }
                    Document document = ownerElement.getOwnerDocument();
                    Attr newAttribute = document.createAttribute(dialog.getAttributeName());
                    newAttribute.setValue(dialog.getAttributeValue());
                    ownerElement.setAttributeNode(newAttribute);
                    manager.setViewerSelection(newAttribute);
                  }
                  manager.endNodeAction(this);
                }
              }
            };
            contributeAction(menu, editAttributeAction);
          }
          else
          {
            // default behavior
            super.contributeActions(menu, selection);
          }
        }
      };

      nodeActionManager.fillContextMenu(menuManager, getSelection());
    }
  }

  class ViewLayoutCellModifier implements ICellModifier, TreeExtension.ICellEditorProvider
  {
    public boolean canModify(Object element, String property)
    {
      boolean result = false;
      if (element instanceof Node)
      {
        Node node = (Node) element;
        if (property == XMLEditorMessages.XMLTreeExtension_1)
        {
          result = treeContentHelper.isEditable(node);
          if (result)
          {
            // set up the cell editor based on the element
            CellEditor[] editors = getCellEditors();
            if (editors.length > 0)
            {
              if (editors[1] != null)
                editors[1].dispose();
              editors[1] = getCellEditor(element, 1);
            }
          }
        }
      }
      return result;
    }

    public Object getValue(Object object, String property)
    {
      String result = null;
      if (object instanceof Node)
      {
        Node node = (Node) object;
        result = treeContentHelper.getNodeValue(node);
      }
      return (result != null) ? result : "";
    }

    public void modify(Object element, String property, Object value)
    {
      Item item = (Item) element;
      String oldValue = treeContentHelper.getNodeValue((Node) item.getData());
      String newValue = value.toString();
      if ((newValue != null) && !newValue.equals(oldValue))
      {
        Node node = (Node) item.getData();
        if (node.getNodeType() == Node.ATTRIBUTE_NODE && value instanceof CTreeComboItem)
        {
          // dropdown choice of attribute value
          value = ((CTreeComboItem)value).getText();
        }
        //System.out.println("value: " + value);
        treeContentHelper.setNodeValue(node, value.toString(), getControl().getShell());
      }
    }

    public CellEditor getCellEditor(Object o, int col)
    {
      if (o instanceof Node && ((Node)o).getNodeType() == Node.ATTRIBUTE_NODE)
      {
        Attr attr = (Attr) o;
        String[] attrOptions = viewLayout.getAttributeOptions(attr);
        if (attrOptions != null)
        {
          TreeComboCellEditor treeComboCellEditor = new TreeComboCellEditor(getTree());
          treeComboCellEditor.setSelectionModifier(new SelectionModifier()
          {
            public String modify(CTreeComboItem selection)
            {
              if (hasVariablesParent(selection))
                return "$" + selection.getText();
              else
                return selection.getText();
            }
          });
          for (String option : attrOptions)
          {
            CTreeComboItem item = treeComboCellEditor.addItem(option);
            if (option.equals(ViewLayout.VARIABLES_ATTRIBUTE_OPTION))
            {
              Map<String,List<String>> vars = viewLayout.getVariables(attr, option);
              if (vars != null)
              {
                for (String task : vars.keySet())
                {
                  CTreeComboItem taskItem = new CTreeComboItem(item);
                  taskItem.setText(task);
                  for (String var : vars.get(task))
                  {
                    CTreeComboItem varItem = new CTreeComboItem(taskItem);
                    varItem.setText(var);
                  }
                }
              }
            }
          }

          return treeComboCellEditor;
        }
      }

      // default behavior
      IPropertyDescriptor pd = propertyDescriptorFactory.createPropertyDescriptor(o);
      return pd != null ? pd.createPropertyEditor(getTree()) : null;
    }

    private boolean hasVariablesParent(CTreeComboItem item)
    {
      CTreeComboItem parentItem = item.getParentItem();
      if (parentItem == null)
        return false;
        else if (parentItem.getText().equals(ViewLayout.VARIABLES_ATTRIBUTE_OPTION))
      return true;

      return hasVariablesParent(parentItem);
    }
  }
}
