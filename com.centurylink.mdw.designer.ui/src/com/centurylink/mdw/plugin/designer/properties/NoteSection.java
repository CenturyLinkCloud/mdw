/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.properties;

import org.eclipse.jface.viewers.IFilter;
import org.eclipse.swt.widgets.Composite;

import com.centurylink.mdw.plugin.designer.model.Note;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.plugin.designer.properties.editor.PropertyEditor;
import com.centurylink.mdw.plugin.designer.properties.editor.ValueChangeListener;

public class NoteSection extends PropertySection implements IFilter
{
  private Note note;
  public Note getNote() { return note; }

  private PropertyEditor contentEditor;

  public void setSelection(WorkflowElement selection)
  {
    note = (Note) selection;

    contentEditor.setElement(note);
    contentEditor.setValue(note.getText());
    contentEditor.setEditable(!note.isReadOnly());
  }

  public void drawWidgets(Composite composite, WorkflowElement selection)
  {
    note = (Note) selection;

    // content text area
    contentEditor = new PropertyEditor(note, PropertyEditor.TYPE_TEXT);
    contentEditor.setLabel("Note");
    contentEditor.setWidth(475);
    contentEditor.setHeight(100);
    contentEditor.setMultiLine(true);
    contentEditor.addValueChangeListener(new ValueChangeListener()
    {
      public void propertyValueChanged(Object newValue)
      {
        note.setText((String)newValue);
        note.adjustSize();
      }
    });
    contentEditor.render(composite);

  }

  public boolean select(Object toTest)
  {
    return (toTest instanceof Note);
  }
}