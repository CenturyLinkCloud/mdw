/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.properties.editor;

import org.eclipse.epf.richtext.IRichText;
import org.eclipse.epf.richtext.IRichTextToolBar;
import org.eclipse.epf.richtext.RichText;
import org.eclipse.epf.richtext.RichTextCommand;
import org.eclipse.epf.richtext.RichTextToolBar;
import org.eclipse.epf.richtext.actions.AddCodeAction;
import org.eclipse.epf.richtext.actions.AddOrderedListAction;
import org.eclipse.epf.richtext.actions.AddTableAction;
import org.eclipse.epf.richtext.actions.AddUnorderedListAction;
import org.eclipse.epf.richtext.actions.BoldAction;
import org.eclipse.epf.richtext.actions.ClearContentAction;
import org.eclipse.epf.richtext.actions.CopyAction;
import org.eclipse.epf.richtext.actions.CutAction;
import org.eclipse.epf.richtext.actions.FindReplaceAction;
import org.eclipse.epf.richtext.actions.FontNameAction;
import org.eclipse.epf.richtext.actions.FontSizeAction;
import org.eclipse.epf.richtext.actions.FontStyleAction;
import org.eclipse.epf.richtext.actions.IndentAction;
import org.eclipse.epf.richtext.actions.ItalicAction;
import org.eclipse.epf.richtext.actions.OutdentAction;
import org.eclipse.epf.richtext.actions.PasteAction;
import org.eclipse.epf.richtext.actions.RichTextAction;
import org.eclipse.epf.richtext.actions.SubscriptAction;
import org.eclipse.epf.richtext.actions.SuperscriptAction;
import org.eclipse.epf.richtext.actions.TidyActionGroup;
import org.eclipse.epf.richtext.actions.UnderlineAction;
import org.eclipse.jface.action.IAction;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.StatusTextEvent;
import org.eclipse.swt.browser.StatusTextListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;

import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.designer.model.Activity;
import com.centurylink.mdw.plugin.designer.model.EmbeddedSubProcess;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.plugin.designer.model.WorkflowProcess;
import com.qwest.mbeng.MbengNode;

public class RichTextEditor extends PropertyEditor
{
  public static final String TYPE_RICH_TEXT = "RICH_TEXT";
  private static final int STATUS_MODIFIED = 2;
  private static final int STATUS_SELECT_TEXT = 6;

  private RichTextWidget widget;

  public RichTextEditor(WorkflowElement workflowElement)
  {
    super(workflowElement, TYPE_RICH_TEXT);
  }

  public RichTextEditor(WorkflowElement workflowElement, MbengNode mbengNode)
  {
    super(workflowElement, mbengNode);
  }

  @Override
  public void updateWidget(String value)
  {
    widget.setText(value == null ? "" : value);
  }

  public void render(Composite parent)
  {
    widget = createRichText(parent);
  }

  @Override
  public void setEnabled(boolean enabled)
  {
    if (widget != null)
      widget.getControl().setEnabled(enabled);
  }

  @Override
  public void setEditable(boolean editable)
  {
    if (widget != null)
      widget.setEditable(editable);
  }

  @Override
  public void dispose()
  {
    if (widget != null && !widget.isDisposed())
      widget.dispose();
  }

  public void setVisible(boolean visible)
  {
    if (widget != null)
      widget.getControl().setVisible(visible);
  }

  public void setFocus()
  {
    if (widget != null)
      widget.setFocus();
  }

  private RichTextWidget createRichText(Composite parent)
  {
    int style = getStyle() | SWT.V_SCROLL | SWT.WRAP | SWT.BORDER | SWT.FLAT;

    if (isReadOnly())
      style = style | SWT.READ_ONLY;

    WorkflowProcess pv = null;
    if (getElement() instanceof WorkflowProcess)
      pv = (WorkflowProcess) getElement();
    else if (getElement() instanceof Activity)
      pv = ((Activity)getElement()).getProcess();
    else if (getElement() instanceof EmbeddedSubProcess)
      pv = ((EmbeddedSubProcess)getElement()).getProcess();

    if (pv != null)
    {
      IEditorPart edPart = MdwPlugin.getActivePage().findEditor(pv);
      if (edPart != null && edPart.getSite() instanceof IEditorSite)
      {
        final RichTextWidget richText = new RichTextWidget(parent, style, (IEditorSite)edPart.getSite());

        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.horizontalSpan = COLUMNS;
        richText.setLayoutData(gd);

        if (getValue() != null)
          richText.setText(getValue());

        // without this listener, entered text can be lost
        richText.addListener(SWT.Deactivate, new Listener()
        {
          public void handleEvent(Event event)
          {
            saveValue(richText.getText());
          }
        });

        return richText;
      }
    }
    return null;
  }

  private void saveValue(String newVal)
  {
    if (!newVal.isEmpty())
      newVal = "<html>" + newVal + "</html>";
    System.out.println("DEACTIVATE: '" + newVal + "'");
    // setValue(newVal);
    fireValueChanged(newVal);
  }

  public class RichTextWidget extends org.eclipse.epf.richtext.RichTextEditor
  {
    private Color white;
    private Color gray;

    private Browser editor;
    private StatusTextListener statusTextListener;

    @Override
    public void dispose()
    {
      // removeKeyListener(keyListener);
      editor.removeStatusTextListener(statusTextListener);
      super.dispose();
      form.dispose();
    }

    public RichTextWidget(Composite parent, int style, IEditorSite editorSite)
    {
      super(parent, style, editorSite);
      white = new Color(parent.getShell().getDisplay(), 255, 255, 255);
      gray = new Color(parent.getShell().getDisplay(), 212, 208, 200);

      editor = (Browser)getRichTextControl().getControl();

      statusTextListener = new StatusTextListener()
      {
        public void changed(StatusTextEvent event)
        {
          String eventText = event.text;
          int eventTextLength = eventText.length();
          if (eventText.startsWith("$$$") && eventTextLength > 3)
          {

            int endStatusIndex = 4;
            if (eventText.length() > 4 && Character.isDigit(eventText.charAt(endStatusIndex)))
              endStatusIndex++;
            int statusType = Integer.parseInt(eventText.substring(3, endStatusIndex));
            if (statusType == STATUS_MODIFIED)
            {
              saveValue(richText.getText());
            }
            if (statusType == STATUS_SELECT_TEXT)
            {
              if (eventTextLength >= 5)
              {
                RichText richText = (RichText) getRichTextControl();
                // unfortunately getText() here does not reflect the latest typed char,
                // the Deactivate or STATUS_MODIFIED events usually handle this
                saveValue(richText.getText());
              }
            }
          }
        }
      };
      editor.addStatusTextListener(statusTextListener);
    }

    @Override
    public void setEditable(boolean editable)
    {
      super.setEditable(editable);

      fontStyleAction.getCCombo().setEnabled(editable);
      fontNameAction.getCCombo().setEnabled(editable);
      fontSizeAction.getCCombo().setEnabled(editable);

      Color bg = editable ? white : gray;

      fontStyleAction.getCCombo().setBackground(bg);
      fontNameAction.getCCombo().setBackground(bg);
      fontSizeAction.getCCombo().setBackground(bg);
    }

    private FontStyleAction fontStyleAction;
    private FontNameAction fontNameAction;
    private FontSizeAction fontSizeAction;

    @Override
    public void fillToolBar(IRichTextToolBar toolBar)
    {
      toolBar = (RichTextToolBar)toolBar;
      fontStyleAction = new FontStyleAction(this);
      toolBar.addAction(fontStyleAction);

      fontNameAction = new FontNameAction(this);
      toolBar.addAction(fontNameAction);

      fontSizeAction = new FontSizeAction(this);
      toolBar.addAction(fontSizeAction);

      // For some reason adding a button action to the dropdown toolbar is needed
      // or otherwise the CCombos are barely visible due to squashed height.
      ((RichTextToolBar)toolBar).getToolbarMgrCombo().add(new FontColorAction(this));
      ((RichTextToolBar)toolBar).getToolbarMgrCombo().add(new BoldAction(this));
      ((RichTextToolBar)toolBar).getToolbarMgrCombo().add(new ItalicAction(this));
      ((RichTextToolBar)toolBar).getToolbarMgrCombo().add(new UnderlineAction(this));
      ((RichTextToolBar)toolBar).getToolbarMgrCombo().update(true);

      toolBar.addSeparator();
      toolBar.addAction(new CutAction(this));
      toolBar.addAction(new CopyAction(this));
      toolBar.addAction(new PasteAction(this));
      toolBar.addAction(new ClearContentAction(this));
      toolBar.addSeparator();
      toolBar.addAction(new SubscriptAction(this));
      toolBar.addAction(new SuperscriptAction(this));
      toolBar.addSeparator();
      toolBar.addAction(new TidyActionGroup(this));
      toolBar.addSeparator();
      toolBar.addAction(new AddOrderedListAction(this));
      toolBar.addAction(new AddUnorderedListAction(this));
      toolBar.addSeparator();
      toolBar.addAction(new OutdentAction(this));
      toolBar.addAction(new IndentAction(this));
      toolBar.addSeparator();
      toolBar.addAction(new FindReplaceAction(this)
      {
        @Override
        public void execute(IRichText richText)
        {
          richText.getFindReplaceAction().execute(richText);
        }
      });
      toolBar.addSeparator();
      toolBar.addAction(new AddTableAction(this));
      toolBar.addAction(new AddCodeAction(this));
    }

  }

  public class FontColorAction extends RichTextAction
  {

    public FontColorAction(IRichText richText)
    {
      super(richText, IAction.AS_PUSH_BUTTON);
      setImageDescriptor(MdwPlugin.getImageDescriptor("icons/color.gif"));
      setToolTipText("Text Color");
    }

    public void execute(IRichText richText)
    {
      if (richText != null)
      {
        ColorDialog dialog = new ColorDialog(Display.getCurrent().getActiveShell());
        dialog.open();
        RGB rgb = dialog.getRGB();
        if (rgb != null)
        {
          java.awt.Color c = new java.awt.Color(rgb.red, rgb.green, rgb.blue);
          String hex = Integer.toHexString(c.getRGB());
          richText.executeCommand(RichTextCommand.FORGROUND_COLOR, "#" + hex.substring(2, hex.length()));
        }
      }
    }
  }
}
