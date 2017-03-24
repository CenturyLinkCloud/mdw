/*
 * Copyright (C) 2017 CenturyLink, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
