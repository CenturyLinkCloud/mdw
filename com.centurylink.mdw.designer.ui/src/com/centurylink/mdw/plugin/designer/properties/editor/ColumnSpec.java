/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.properties.editor;

import java.text.DateFormat;

import org.eclipse.jface.viewers.ICellEditorListener;

/**
 * Column specification for tables.
 */
public class ColumnSpec {
    public static final int DEFAULT_COL_WIDTH = 300;
    public static final int DEFAULT_ROW_HEIGHT = -1;

    public String label;
    public String type;
    public String property;
    public int width = DEFAULT_COL_WIDTH;
    public int height = DEFAULT_ROW_HEIGHT;
    public boolean readOnly;
    public boolean resizable = true;
    public String[] options;
    public String source;
    public ICellEditorListener listener;
    public int style;
    public String defaultValue;
    public String[] assetTypes; // for wf-asset tree-combo
    public boolean hidden;

    public Decoder decoder;
    public DateFormat dateFormat;

    public ColumnSpec(String type, String label, String property) {
        this.label = label;
        this.type = type;
        this.property = property;
    }

    public interface Decoder {
        public String decode(Long code);
    }
}
